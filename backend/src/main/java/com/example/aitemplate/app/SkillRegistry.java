package com.example.aitemplate.app;

import com.example.aitemplate.core.skill.SkillProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SkillRegistry {

    private static final Logger log = LoggerFactory.getLogger(SkillRegistry.class);

    private final Map<String, SkillProvider> builtinProviders;
    private final Map<String, SkillProvider> dynamicProviders;
    private final Path localSkillRoot;
    private final ObjectMapper objectMapper;

    public SkillRegistry(
            List<SkillProvider> providers,
            @Value("${app.skills.local-dir:skills/runtime}") String localSkillDir,
            ObjectMapper objectMapper) {
        this.builtinProviders = new ConcurrentHashMap<>();
        this.dynamicProviders = new ConcurrentHashMap<>();
        this.objectMapper = objectMapper;
        this.localSkillRoot = Paths.get(localSkillDir).toAbsolutePath().normalize();
        for (SkillProvider provider : providers) {
            this.builtinProviders.put(key(provider.skillName(), provider.version()), provider);
        }
        log.info("[Skill] Registered {} builtin skill(s): {}", builtinProviders.size(),
                builtinProviders.values().stream().map(p -> p.skillName() + "@" + p.version()).toList());
        loadDynamicProvidersFromDisk();
    }

    public List<SkillProvider> list() {
        return listEntries().stream().map(SkillEntry::provider).toList();
    }

    public Path localSkillRoot() {
        return localSkillRoot;
    }

    public List<SkillEntry> listEntries() {
        List<SkillEntry> entries = new ArrayList<>();
        builtinProviders.values().stream()
                .sorted((a, b) -> key(a.skillName(), a.version()).compareToIgnoreCase(key(b.skillName(), b.version())))
                .forEach(provider -> entries.add(new SkillEntry(provider, "builtin", false)));
        dynamicProviders.values().stream()
                .sorted((a, b) -> key(a.skillName(), a.version()).compareToIgnoreCase(key(b.skillName(), b.version())))
                .forEach(provider -> entries.add(new SkillEntry(provider, "dynamic", true)));
        return entries;
    }

    public List<SkillProvider> resolve(List<String> skillRefs) {
        if (skillRefs == null || skillRefs.isEmpty()) {
            return List.of();
        }
        List<SkillProvider> resolved = skillRefs.stream()
                .map(this::resolveOne)
                .filter(provider -> provider != null)
                .toList();
        log.debug("[Skill] Resolved {}/{} skill refs: {}",
                resolved.size(), skillRefs.size(),
                resolved.stream().map(p -> p.skillName() + "@" + p.version()).toList());
        return resolved;
    }

    public SkillProvider upsertDynamic(String skillName, String version, String content) {
        String safeName = skillName == null ? "" : skillName.trim();
        String safeVersion = version == null || version.isBlank() ? "1.0.0" : version.trim();
        if (safeName.isBlank()) {
            throw new IllegalArgumentException("skillName is required");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content is required");
        }
        SkillProvider provider = new DynamicSkillProvider(safeName, safeVersion, content);
        dynamicProviders.put(key(safeName, safeVersion), provider);
        persistDynamicSkill(provider);
        log.info("[Skill] Saved dynamic skill: {}@{}, contentLength={}", safeName, safeVersion, content.length());
        return provider;
    }

    public boolean deleteDynamic(String skillName, String version) {
        if (skillName == null || skillName.isBlank()) {
            return false;
        }
        if (version == null || version.isBlank()) {
            String prefix = skillName.trim() + "@";
            boolean removed = false;
            for (String k : List.copyOf(dynamicProviders.keySet())) {
                if (k.startsWith(prefix)) {
                    SkillProvider provider = dynamicProviders.remove(k);
                    if (provider != null) {
                        deleteDynamicSkillFiles(provider.skillName(), provider.version());
                        log.info("[Skill] Deleted dynamic skill: {}@{}", provider.skillName(), provider.version());
                    }
                    removed = true;
                }
            }
            return removed;
        }
        SkillProvider removed = dynamicProviders.remove(key(skillName.trim(), version.trim()));
        if (removed != null) {
            deleteDynamicSkillFiles(removed.skillName(), removed.version());
            log.info("[Skill] Deleted dynamic skill: {}@{}", removed.skillName(), removed.version());
            return true;
        }
        return false;
    }

    private SkillProvider resolveOne(String skillRef) {
        if (skillRef == null || skillRef.isBlank()) {
            return null;
        }
        String ref = skillRef.trim();
        int atIndex = ref.indexOf('@');
        if (atIndex > 0 && atIndex < ref.length() - 1) {
            String name = ref.substring(0, atIndex);
            String version = ref.substring(atIndex + 1);
            SkillProvider dynamic = dynamicProviders.get(key(name, version));
            if (dynamic != null) {
                return dynamic;
            }
            return builtinProviders.get(key(name, version));
        }

        SkillProvider latestDynamic = latestByName(dynamicProviders, ref);
        if (latestDynamic != null) {
            return latestDynamic;
        }
        return latestByName(builtinProviders, ref);
    }

    private SkillProvider latestByName(Map<String, SkillProvider> map, String name) {
        return map.values().stream()
                .filter(provider -> provider.skillName().equals(name))
                .max((a, b) -> compareVersion(a.version(), b.version()))
                .orElse(null);
    }

    private int compareVersion(String left, String right) {
        return normalizeVersion(left).compareTo(normalizeVersion(right));
    }

    private String normalizeVersion(String version) {
        if (version == null) {
            return "0";
        }
        return version.toLowerCase(Locale.ROOT).replace("v", "");
    }

    private String key(String name, String version) {
        return name + "@" + version;
    }

    public Optional<Path> findPythonSkillScript(String skillName, String version) {
        if (skillName == null || skillName.isBlank()) {
            return Optional.empty();
        }
        String name = skillName.trim();

        // 1. Check skills/runtime/{name}/{version}/ — standard dynamic skill location
        Path dir = resolveSkillVersionDir(name, version);
        if (dir != null) {
            // Check run.py / skill.py directly in the version dir
            Path runPy = dir.resolve("run.py");
            if (Files.exists(runPy)) {
                return Optional.of(runPy);
            }
            Path skillPy = dir.resolve("skill.py");
            if (Files.exists(skillPy)) {
                return Optional.of(skillPy);
            }
            // Check scripts/ subdirectory (e.g. scripts/search.py)
            Optional<Path> inScripts = findFirstPyIn(dir.resolve("scripts"));
            if (inScripts.isPresent()) {
                log.debug("[Skill] Found Python script for {} at {}", name, inScripts.get());
                return inScripts;
            }
        }

        // 2. Check skills/{name}/ and skills/{name}/scripts/ — for skills installed outside runtime/
        Path skillsBase = localSkillRoot.getParent();
        if (skillsBase != null) {
            Path nameDir = skillsBase;
            for (String seg : name.split("/")) {
                nameDir = nameDir.resolve(sanitizePathSegment(seg));
            }
            Optional<Path> inScripts = findFirstPyIn(nameDir.resolve("scripts"));
            if (inScripts.isPresent()) {
                log.debug("[Skill] Found Python script (outside runtime) for {} at {}", name, inScripts.get());
                return inScripts;
            }
            Path runPy = nameDir.resolve("run.py");
            if (Files.exists(runPy)) {
                return Optional.of(runPy);
            }
        }

        return Optional.empty();
    }

    private Optional<Path> findFirstPyIn(Path dir) {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return Optional.empty();
        }
        try (var stream = Files.walk(dir)) {
            return stream.filter(p -> p.toString().endsWith(".py")).findFirst();
        }
        catch (IOException ex) {
            return Optional.empty();
        }
    }

    /** Expose the resolved absolute path for a skill version directory (for script persistence). */
    public Path skillVersionDir(String skillName, String version) {
        return localSkillDir(skillName, version);
    }

    private Path resolveSkillVersionDir(String skillName, String version) {
        if (version != null && !version.isBlank()) {
            Path specific = localSkillDir(skillName, version.trim());
            return Files.exists(specific) ? specific : null;
        }
        Path nameDir = localSkillNameDir(skillName);
        if (!Files.exists(nameDir) || !Files.isDirectory(nameDir)) {
            return null;
        }
        try (var stream = Files.list(nameDir)) {
            return stream.filter(Files::isDirectory)
                    .max((a, b) -> compareVersion(a.getFileName().toString(), b.getFileName().toString()))
                    .orElse(null);
        }
        catch (IOException ex) {
            return null;
        }
    }

    private void loadDynamicProvidersFromDisk() {
        try {
            if (!Files.exists(localSkillRoot)) {
                Files.createDirectories(localSkillRoot);
                log.debug("[Skill] Created skills directory: {}", localSkillRoot);
                return;
            }
            try (var stream = Files.walk(localSkillRoot)) {
                stream.filter(path -> path.getFileName().toString().equals("skill.meta.json"))
                        .forEach(this::loadOneSkillMetaFile);
            }
            log.info("[Skill] Loaded {} dynamic skill(s) from disk: {}", dynamicProviders.size(), localSkillRoot);
        }
        catch (IOException ex) {
            throw new IllegalStateException("Failed to initialize local skills directory: " + localSkillRoot, ex);
        }
    }

    private void loadOneSkillMetaFile(Path metaPath) {
        try {
            JsonSkillMeta meta = objectMapper.readValue(Files.readString(metaPath, StandardCharsets.UTF_8), JsonSkillMeta.class);
            Path contentPath = metaPath.getParent().resolve("SKILL.md");
            if (!Files.exists(contentPath)) {
                log.warn("[Skill] Missing SKILL.md for meta at {}, skipping", metaPath);
                return;
            }
            String content = Files.readString(contentPath, StandardCharsets.UTF_8);
            SkillProvider provider = new DynamicSkillProvider(meta.skillName(), meta.version(), content);
            dynamicProviders.put(key(provider.skillName(), provider.version()), provider);
            log.debug("[Skill] Loaded from disk: {}@{}", meta.skillName(), meta.version());
        }
        catch (Exception ex) {
            log.warn("[Skill] Failed to load skill from {}: {}", metaPath, ex.getMessage());
        }
    }

    private void persistDynamicSkill(SkillProvider provider) {
        try {
            Path skillDir = localSkillDir(provider.skillName(), provider.version());
            Files.createDirectories(skillDir);
            Path contentPath = skillDir.resolve("SKILL.md");
            Path metaPath = skillDir.resolve("skill.meta.json");
            Files.writeString(contentPath, provider.content(), StandardCharsets.UTF_8);

            ObjectNode meta = objectMapper.createObjectNode();
            meta.put("skillName", provider.skillName());
            meta.put("version", provider.version());
            Files.writeString(metaPath, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(meta), StandardCharsets.UTF_8);
        }
        catch (IOException ex) {
            throw new IllegalStateException("Failed to persist skill locally: " + provider.skillName(), ex);
        }
    }

    private void deleteDynamicSkillFiles(String skillName, String version) {
        Path skillDir = localSkillDir(skillName, version);
        try {
            if (!Files.exists(skillDir)) {
                return;
            }
            try (var stream = Files.walk(skillDir)) {
                stream.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            }
                            catch (IOException ignore) {
                                // best effort
                            }
                        });
            }
        }
        catch (IOException ignore) {
            // best effort
        }
    }

    private Path localSkillDir(String skillName, String version) {
        return localSkillNameDir(skillName).resolve(sanitizePathSegment(version)).normalize();
    }

    private Path localSkillNameDir(String skillName) {
        Path path = localSkillRoot;
        for (String segment : skillName.split("/")) {
            path = path.resolve(sanitizePathSegment(segment));
        }
        return path.normalize();
    }

    private String sanitizePathSegment(String value) {
        String sanitized = value == null ? "" : value.replaceAll("[^A-Za-z0-9._-]", "_");
        if (sanitized.isBlank()) {
            return "_";
        }
        return sanitized;
    }

    public record SkillEntry(SkillProvider provider, String source, boolean editable) {
    }

    public record SkillImportResult(int imported, List<String> errors, List<String> skillNames) {
    }

    private record DynamicSkillProvider(String skillName, String version, String content) implements SkillProvider {
    }

    private record JsonSkillMeta(String skillName, String version) {
    }
}
