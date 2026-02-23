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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class SkillSourceImportService {

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
        String normalized = normalizeSource(source);
        RepoRef repo = parseRepoRef(normalized);

        List<String> errors = new ArrayList<>();
        int imported = 0;
        try {
            String defaultBranch = fetchDefaultBranch(repo.owner(), repo.repo());
            List<String> skillPaths = fetchSkillPaths(repo.owner(), repo.repo(), defaultBranch, repo.skillHint());
            if (skillPaths.isEmpty()) {
                errors.add("No SKILL.md found in source: " + source);
                return new SkillRegistry.SkillImportResult(0, errors);
            }

            for (String path : skillPaths) {
                try {
                    String fileContent = fetchRawFile(repo.owner(), repo.repo(), defaultBranch, path);
                    SkillDoc skillDoc = parseSkillDoc(repo, path, fileContent);
                    skillRegistry.upsertDynamic(skillDoc.skillName(), skillDoc.version(), skillDoc.content());
                    imported++;
                }
                catch (Exception ex) {
                    errors.add("Failed to import " + path + ": " + ex.getMessage());
                }
            }
        }
        catch (Exception ex) {
            errors.add("Import source failed: " + ex.getMessage());
        }

        return new SkillRegistry.SkillImportResult(imported, errors);
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
        JsonNode node = getJson(url);
        JsonNode branch = node.get("default_branch");
        if (branch == null || branch.asText().isBlank()) {
            return "main";
        }
        return branch.asText();
    }

    private List<String> fetchSkillPaths(String owner, String repo, String branch, String skillHint)
            throws IOException, InterruptedException {
        String url = "https://api.github.com/repos/" + encode(owner) + "/" + encode(repo)
                + "/git/trees/" + encode(branch) + "?recursive=1";
        JsonNode root = getJson(url);
        JsonNode tree = root.get("tree");
        if (tree == null || !tree.isArray()) {
            return List.of();
        }
        List<String> paths = new ArrayList<>();
        for (JsonNode entry : tree) {
            if (!"blob".equals(entry.path("type").asText())) {
                continue;
            }
            String path = entry.path("path").asText("");
            if (!path.endsWith("SKILL.md")) {
                continue;
            }
            if (skillHint != null && !skillHint.isBlank()) {
                String lower = path.toLowerCase(Locale.ROOT);
                String hint = skillHint.toLowerCase(Locale.ROOT);
                if (!lower.contains("/" + hint + "/") && !lower.contains(hint)) {
                    continue;
                }
            }
            paths.add(path);
        }
        return paths;
    }

    private String fetchRawFile(String owner, String repo, String branch, String path)
            throws IOException, InterruptedException {
        String url = "https://raw.githubusercontent.com/" + encode(owner) + "/" + encode(repo)
                + "/" + encode(branch) + "/" + path;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "text/plain")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
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
