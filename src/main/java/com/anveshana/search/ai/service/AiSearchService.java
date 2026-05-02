package com.anveshana.search.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

@Service
public class AiSearchService {

    @Value("${anveshana.root.path}")
    private String rootPath;

    @Value("${groq.api.key}")
    private String groqApiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    // tracks which files were AI-generated this session (for cleanup on logout)
    private final Set<String> sessionGeneratedFiles = new HashSet<>();

    /**
     * Returns AI-generated content for the query.
     * Checks cache first — only calls Anthropic API if no cached file exists.
     */
    public AiSearchResult search(String query) throws IOException, InterruptedException {
        Path cacheDir = getCacheDir();
        Path cacheFile = cacheDir.resolve(sanitizeFilename(query) + ".txt");

        boolean fromCache = false;

        if (Files.exists(cacheFile)) {
            String content = Files.readString(cacheFile);
            fromCache = true;
            return new AiSearchResult(content, cacheFile.toString(), fromCache);
        }

        // not cached — call Groq
        String content = callGroq(query);

        // write to cache
        Files.writeString(cacheFile, content);
        sessionGeneratedFiles.add(cacheFile.toAbsolutePath().toString());

        return new AiSearchResult(content, cacheFile.toString(), fromCache);
    }

    /**
     * Deletes all AI-generated files created during this session.
     * Called on logout.
     */
    public void clearSessionFiles() {
        for (String filePath : sessionGeneratedFiles) {
            try {
                Files.deleteIfExists(Path.of(filePath));
            } catch (IOException e) {
                System.err.println("Could not delete AI cache file: " + filePath);
            }
        }
        sessionGeneratedFiles.clear();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String callGroq(String query) throws IOException, InterruptedException {
        String prompt = """
                A user searched for: "%s"
                
                Please provide a clear, accurate, and informative response about this topic.
                Write it as a well-structured plain text document (no markdown, no bullet symbols).
                Include relevant facts, context, and useful details a person would want to know.
                Keep it between 200-400 words.
                """.formatted(query);

        String requestBody = objectMapper.writeValueAsString(new java.util.LinkedHashMap<>() {{
            put("model", "llama-3.3-70b-versatile");
            put("max_tokens", 1024);
            put("messages", new Object[]{
                    new java.util.LinkedHashMap<>() {{
                        put("role", "user");
                        put("content", prompt);
                    }}
            });
        }});

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + groqApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Groq API error: " + response.statusCode()
                    + " — " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        return root.path("choices").get(0).path("message").path("content").asText();
    }

    private Path getCacheDir() throws IOException {
        Path cacheDir = Path.of(rootPath, "ai-cache");
        if (!Files.exists(cacheDir)) {
            Files.createDirectories(cacheDir);
        }
        return cacheDir;
    }

    /**
     * Converts a query into a safe filename.
     * e.g. "What is machine learning?" -> "what_is_machine_learning"
     */
    private String sanitizeFilename(String query) {
        return query.toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "")
                .substring(0, Math.min(query.length(), 80));
    }

    // -------------------------------------------------------------------------
    // Result wrapper
    // -------------------------------------------------------------------------

    public static class AiSearchResult {
        private final String content;
        private final String filePath;
        private final boolean fromCache;

        public AiSearchResult(String content, String filePath, boolean fromCache) {
            this.content = content;
            this.filePath = filePath;
            this.fromCache = fromCache;
        }

        public String getContent()   { return content; }
        public String getFilePath()  { return filePath; }
        public boolean isFromCache() { return fromCache; }
    }
}
