package com.example.aitemplate.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SkillSourceImportService {

    private static final Logger log = LoggerFactory.getLogger(SkillSourceImportService.class);
    private static final Pattern SLUG_PATTERN = Pattern.compile("^([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+)(?:@([A-Za-z0-9_.-]+))?$");

    private final SkillRegistry skillRegistry;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public SkillSourceImportService(SkillRegistry skillRegistry, ObjectMapper objectMapper) {
        this.skillRegistry = skillRegistry;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public SkillRegistry.SkillImportResult importFromSource(String source) {
        log.info("[Import] Starting skill import from source: {}", source);
        String normalized = normalizeSource(source);
        RepoRef repo = parseRepoRef(normalized);
        log.info("[Import] Resolved target: owner={}, repo={}, skillHint={}", repo.owner(), repo.repo(), repo.skillHint());

        List<String> errors = new ArrayList<>();
        List<String> importedNames = new ArrayList<>();
        int imported = 0;
        try {
            String defaultBranch = fetchDefaultBranch(repo.owner(), repo.repo());
            log.info("[Import] Repository default branch: {}", defaultBranch);

            // Fetch the full tree once — used for both SKILL.md and .py discovery
            List<String> allPaths = fetchAllBlobPaths(repo.owner(), repo.repo(), defaultBranch);
            List<String> skillPaths = filterSkillMdPaths(allPaths, repo.skillHint());

            if (skillPaths.isEmpty()) {
                log.warn("[Import] No SKILL.md files found in {}/{}", repo.owner(), repo.repo());
                errors.add("No SKILL.md found in source: " + source);
                return new SkillRegistry.SkillImportResult(0, errors, List.of());
            }
            log.info("[Import] Found {} SKILL.md file(s) to process: {}", skillPaths.size(), skillPaths);

            for (String path : skillPaths) {
                log.debug("[Import] Fetching file: {}", path);
                try {
                    String fileContent = fetchRawFile(repo.owner(), repo.repo(), defaultBranch, path);
                    SkillDoc skillDoc = parseSkillDoc(repo, path, fileContent);
                    log.info("[Import] Parsed skill: {}@{} (contentLength={})",
                            skillDoc.skillName(), skillDoc.version(), skillDoc.content().length());
                    skillRegistry.upsertDynamic(skillDoc.skillName(), skillDoc.version(), skillDoc.content());
                    importedNames.add(skillDoc.skillName() + "@" + skillDoc.version());
                    imported++;
                    log.info("[Import] Saved skill {}/{}: {}@{}",
                            imported, skillPaths.size(), skillDoc.skillName(), skillDoc.version());

                    // Also download script/data assets from the same directory tree
                    downloadSkillAssets(repo, defaultBranch, path, allPaths, skillDoc.skillName(), skillDoc.version(), errors);
                }
                catch (Exception ex) {
                    log.warn("[Import] Failed to import {}: {}", path, ex.getMessage());
                    errors.add("Failed to import " + path + ": " + ex.getMessage());
                }
            }
        }
        catch (Exception ex) {
            log.error("[Import] Import source failed for '{}': {}", source, ex.getMessage(), ex);
            errors.add("Import source failed: " + ex.getMessage());
        }

        log.info("[Import] Completed: imported={}, errors={}", imported, errors.size());
        return new SkillRegistry.SkillImportResult(imported, errors, importedNames);
    }

    /**
     * Download all files under the same directory tree as SKILL.md (except SKILL.md itself)
     * and persist them byte-for-byte into the runtime skill directory.
     *
     * This keeps script + data dependencies together and avoids corrupting binary assets.
     */
    private void downloadSkillAssets(
            RepoRef repo,
            String branch,
            String skillMdPath,
            List<String> allPaths,
            String skillName,
            String version,
            List<String> errors) {
        String skillDir = skillMdPath.contains("/")
                ? skillMdPath.substring(0, skillMdPath.lastIndexOf('/') + 1)
                : "";
        List<String> directAssetPaths = allPaths.stream()
                .filter(p -> p.startsWith(skillDir) && !p.equals(skillMdPath))
                .toList();
        if (directAssetPaths.isEmpty()) {
            log.debug("[Import] No script/data assets found alongside {}", skillMdPath);
            return;
        }

        Map<String, String> copyPlan = new LinkedHashMap<>();
        for (String assetPath : directAssetPaths) {
            String rel = assetPath.substring(skillDir.length());
            String redirect = tryReadRelativeRedirect(repo, branch, assetPath);
            if (redirect == null) {
                copyPlan.putIfAbsent(rel, assetPath);
                continue;
            }

            List<String> redirectedSources = resolveRedirectedBlobPaths(allPaths, assetPath, redirect);
            if (redirectedSources.isEmpty()) {
                log.warn("[Import] Redirect file '{}' points to '{}' but no files were found in repo tree", assetPath, redirect);
                continue;
            }

            for (String src : redirectedSources) {
                String tail = src.substring(resolveRedirectBase(assetPath, redirect).length());
                if (tail.startsWith("/")) {
                    tail = tail.substring(1);
                }
                String redirectedRel = rel + (tail.isBlank() ? "" : "/" + tail);
                copyPlan.putIfAbsent(redirectedRel, src);
            }
            log.info("[Import] Resolved redirect asset {} -> {} ({} file(s))", assetPath, redirect, redirectedSources.size());
        }

        if (copyPlan.isEmpty()) {
            log.warn("[Import] Found assets for {}@{} but no downloadable files after resolving redirects", skillName, version);
            return;
        }
        log.info("[Import] Found {} asset file(s) for {}@{} after redirect resolution", copyPlan.size(), skillName, version);

        Path skillVersionDir = skillRegistry.skillVersionDir(skillName, version);
        for (Map.Entry<String, String> entry : copyPlan.entrySet()) {
            String relativePath = entry.getKey();
            String sourcePath = entry.getValue();
            try {
                byte[] content = fetchRawFileBytes(repo.owner(), repo.repo(), branch, sourcePath);
                Path targetFile = skillVersionDir.resolve(relativePath.replace('/', java.io.File.separatorChar));
                ensureParentDirectory(targetFile.getParent());
                Files.write(targetFile, content);
                log.info("[Import] Saved asset: {} -> {} ({} bytes)", sourcePath, targetFile, content.length);
            }
            catch (Exception ex) {
                log.warn("[Import] Failed to download asset {}: {}", sourcePath, ex.getMessage());
                errors.add("Failed to download asset " + sourcePath + ": " + ex.getMessage());
            }
        }
    }

    private void ensureParentDirectory(Path dir) throws IOException {
        if (dir == null) {
            return;
        }
        Path parent = dir.getParent();
        if (parent != null) {
            ensureParentDirectory(parent);
        }
        if (Files.exists(dir) && !Files.isDirectory(dir)) {
            Files.delete(dir);
        }
        Files.createDirectories(dir);
    }

    private String tryReadRelativeRedirect(RepoRef repo, String branch, String assetPath) {
        try {
            String content = fetchRawFile(repo.owner(), repo.repo(), branch, assetPath).trim();
            if (content.isBlank()) {
                return null;
            }
            // Some skill repos use tiny text files as path redirects:
            // scripts -> ../../../src/xxx/scripts
            if (content.contains("\n") || content.contains("\r")) {
                return null;
            }
            if (content.startsWith("../") || content.startsWith("./")) {
                return content.replace('\\', '/');
            }
            return null;
        }
        catch (Exception ex) {
            return null;
        }
    }

    private List<String> resolveRedirectedBlobPaths(List<String> allPaths, String redirectFilePath, String redirect) {
        String redirectBase = resolveRedirectBase(redirectFilePath, redirect);
        String prefix = redirectBase + "/";
        List<String> nested = allPaths.stream()
                .filter(p -> p.startsWith(prefix))
                .toList();
        if (!nested.isEmpty()) {
            return nested;
        }
        return allPaths.contains(redirectBase) ? List.of(redirectBase) : List.of();
    }

    private String resolveRedirectBase(String redirectFilePath, String redirect) {
        String parent = redirectFilePath.contains("/")
                ? redirectFilePath.substring(0, redirectFilePath.lastIndexOf('/'))
                : "";
        Path resolved = Paths.get(parent).resolve(redirect).normalize();
        return resolved.toString().replace('\\', '/');
    }

    private String normalizeSource(String source) {
        if (source == null) {
            return "";
        }
        return source.trim();
    }

    private RepoRef parseRepoRef(String source) {
        String candidate = source;
        if (source.startsWith("http://") || source.startsWith("https://")) {
            URI uri = URI.create(source);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            String[] segs = uri.getPath() == null ? new String[0] : uri.getPath().replaceAll("^/+", "").split("/");
            if (segs.length < 2) {
                throw new IllegalArgumentException("Invalid source URL path, expected owner/repo");
            }
            if (host.contains("skills.sh")) {
                String skillHint = segs.length >= 3 ? segs[2] : null;
                return new RepoRef(segs[0], segs[1], skillHint);
            }
            if (host.contains("github.com")) {
                return new RepoRef(segs[0], segs[1], null);
            }
            throw new IllegalArgumentException("Unsupported host: " + host);
        }

        Matcher matcher = SLUG_PATTERN.matcher(candidate);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Source must be owner/repo or owner/repo@skill");
        }
        return new RepoRef(matcher.group(1), matcher.group(2), matcher.group(3));
    }

    private String fetchDefaultBranch(String owner, String repo) throws IOException, InterruptedException {
        String url = "https://api.github.com/repos/" + encode(owner) + "/" + encode(repo);
        log.debug("[Import] GET {}", url);
        JsonNode node = getJson(url);
        JsonNode branch = node.get("default_branch");
        if (branch == null || branch.asText().isBlank()) {
            return "main";
        }
        return branch.asText();
    }

    /** Fetch all blob paths from the repo tree. */
    private List<String> fetchAllBlobPaths(String owner, String repo, String branch)
            throws IOException, InterruptedException {
        String url = "https://api.github.com/repos/" + encode(owner) + "/" + encode(repo)
                + "/git/trees/" + encode(branch) + "?recursive=1";
        log.debug("[Import] GET tree: {}", url);
        JsonNode root = getJson(url);
        JsonNode tree = root.get("tree");
        if (tree == null || !tree.isArray()) {
            return List.of();
        }
        List<String> paths = new ArrayList<>();
        for (JsonNode entry : tree) {
            if ("blob".equals(entry.path("type").asText())) {
                paths.add(entry.path("path").asText(""));
            }
        }
        return paths;
    }

    private List<String> filterSkillMdPaths(List<String> allPaths, String skillHint) {
        return allPaths.stream()
                .filter(p -> p.endsWith("SKILL.md"))
                .filter(p -> {
                    if (skillHint == null || skillHint.isBlank()) {
                        return true;
                    }
                    String lower = p.toLowerCase(Locale.ROOT);
                    String hint = skillHint.toLowerCase(Locale.ROOT);
                    return lower.contains("/" + hint + "/") || lower.contains(hint);
                })
                .toList();
    }

    private String fetchRawFile(String owner, String repo, String branch, String path)
            throws IOException, InterruptedException {
        byte[] bytes = fetchRawFileBytes(owner, repo, branch, path);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private byte[] fetchRawFileBytes(String owner, String repo, String branch, String path)
            throws IOException, InterruptedException {
        String url = "https://raw.githubusercontent.com/" + encode(owner) + "/" + encode(repo)
                + "/" + encode(branch) + "/" + path;
        log.debug("[Import] GET raw: {}", url);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "*/*")
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() >= 400) {
            throw new IOException("raw fetch failed: " + response.statusCode());
        }
        return response.body();
    }

    private SkillDoc parseSkillDoc(RepoRef repo, String path, String markdown) {
        String content = markdown == null ? "" : markdown;
        String skillName = repo.owner() + "/" + repo.repo() + "/" + deriveSkillName(path);
        String version = "1.0.0";

        if (content.startsWith("---\n")) {
            int end = content.indexOf("\n---\n", 4);
            if (end > 0) {
                String frontMatter = content.substring(4, end);
                content = content.substring(end + 5);
                String parsedName = extractFrontMatter(frontMatter, "name");
                String parsedVersion = extractFrontMatter(frontMatter, "version");
                if (parsedName != null && !parsedName.isBlank()) {
                    log.debug("[Import] Front-matter override name: {} -> {}", skillName, parsedName.trim());
                    skillName = parsedName.trim();
                }
                if (parsedVersion != null && !parsedVersion.isBlank()) {
                    version = parsedVersion.trim();
                }
            }
        }
        return new SkillDoc(skillName, version, content.trim());
    }

    private String extractFrontMatter(String frontMatter, String key) {
        Pattern pattern = Pattern.compile("^" + Pattern.quote(key) + "\\s*:\\s*(.+)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(frontMatter);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).replaceAll("^\"|\"$", "").trim();
    }

    private String deriveSkillName(String path) {
        String normalized = path.replace('\\', '/');
        if (normalized.endsWith("/SKILL.md")) {
            normalized = normalized.substring(0, normalized.length() - "/SKILL.md".length());
        }
        normalized = normalized.replace('/', '-');
        if (normalized.isBlank()) {
            return "skill";
        }
        return normalized;
    }

    private JsonNode getJson(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "ai-template-importer")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 400) {
            throw new IOException("GitHub API failed: " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record RepoRef(String owner, String repo, String skillHint) {
    }

    private record SkillDoc(String skillName, String version, String content) {
    }
}
