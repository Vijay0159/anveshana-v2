package com.anveshana.search.ai.service;

import com.anveshana.search.ai.entity.AiDocument;
import com.anveshana.search.ai.repository.AiDocumentRepository;
import com.anveshana.search.auth.entity.User;
import com.anveshana.search.search.engine.InvertedIndex;
import com.anveshana.search.search.engine.Posting;
import com.anveshana.search.search.engine.TextTokenizer;
import com.anveshana.search.search.model.Document;
import com.anveshana.search.search.util.HtmlHighlighter;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class AiDocumentService {

    private final AiDocumentRepository aiDocumentRepository;

    // In-memory inverted index over saved AI documents (all users combined)
    // docId here = AiDocument.id (Long cast to int for reuse of existing engine)
    private final InvertedIndex invertedIndex = new InvertedIndex();

    // Maps docId (int) → AiDocument for fast lookup after search
    private final Map<Integer, AiDocument> docIdMap = new ConcurrentHashMap<>();

    public AiDocumentService(AiDocumentRepository aiDocumentRepository) {
        this.aiDocumentRepository = aiDocumentRepository;
    }

    /**
     * On startup, load all saved AI docs from DB and build the inverted index.
     */
    @PostConstruct
    public void init() {
        rebuildIndex();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Save an AI result to DB for the given user.
     * If the user already saved a result for this exact query, it is skipped.
     */
    @Transactional
    public AiDocument save(User user, String query, String content) {
        if (aiDocumentRepository.existsByUserAndQuery(user, query)) {
            return aiDocumentRepository.findByUserAndQuery(user, query).orElseThrow();
        }

        String title = toTitle(query);
        AiDocument doc = new AiDocument(user, query, title, content);
        AiDocument saved = aiDocumentRepository.save(doc);

        // Add to in-memory index immediately — no restart needed
        addToIndex(saved);

        return saved;
    }

    /**
     * Delete a saved AI document by id, then rebuild the index.
     */
    @Transactional
    public void delete(Long id, User user) {
        aiDocumentRepository.findById(id).ifPresent(doc -> {
            if (doc.getUser().getId().equals(user.getId())) {
                aiDocumentRepository.delete(doc);
                rebuildIndex(); // rebuild since deletion needs full re-index
            }
        });
    }

    /**
     * Returns all saved AI documents for a user (for the library page).
     */
    public List<AiDocument> getAllForUser(User user) {
        return aiDocumentRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Check if the user already saved a result for this query.
     */
    public boolean isSaved(User user, String query) {
        return aiDocumentRepository.existsByUserAndQuery(user, query);
    }

    /**
     * Search saved AI documents for a user using the inverted index.
     * Returns ranked results containing only this user's documents.
     */
    public List<SavedSearchResult> search(String query, User user) {
        if (query == null || query.isBlank()) return Collections.emptyList();

        List<String> queryTerms = new ArrayList<>(TextTokenizer.tokenize(query));
        if (queryTerms.isEmpty()) return Collections.emptyList();

        // AND match across all terms
        Set<Integer> matchingDocIds = getDocumentsContainingAllTerms(queryTerms);

        List<SavedSearchResult> results = new ArrayList<>();

        for (int docId : matchingDocIds) {
            AiDocument aiDoc = docIdMap.get(docId);
            if (aiDoc == null) continue;

            // Only include this user's documents
            if (!aiDoc.getUser().getId().equals(user.getId())) continue;

            // Score: sum of term frequencies across query terms
            double score = 0;
            for (String term : queryTerms) {
                Map<Integer, List<Posting>> postings = invertedIndex.getPostings(term);
                List<Posting> ps = postings.getOrDefault(docId, List.of());
                for (Posting p : ps) score += p.getFrequency();
            }

            // Generate snippet with highlights
            String snippet = generateSnippet(aiDoc.getContent(), query, queryTerms);

            results.add(new SavedSearchResult(aiDoc, score, snippet));
        }

        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return results;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void rebuildIndex() {
        invertedIndex.clear();
        docIdMap.clear();

        List<AiDocument> all = aiDocumentRepository.findAll();
        for (AiDocument doc : all) {
            addToIndex(doc);
        }
    }

    private void addToIndex(AiDocument aiDoc) {
        int docId = aiDoc.getId().intValue();
        docIdMap.put(docId, aiDoc);

        // Reuse the existing Document model and InvertedIndex engine
        Document engineDoc = new Document(docId, aiDoc.getTitle(), aiDoc.getContent());
        invertedIndex.addDocument(engineDoc);
    }

    private Set<Integer> getDocumentsContainingAllTerms(List<String> queryTerms) {
        Set<Integer> result = new HashSet<>(invertedIndex.search(queryTerms.get(0)));
        for (int i = 1; i < queryTerms.size(); i++) {
            result.retainAll(invertedIndex.search(queryTerms.get(i)));
            if (result.isEmpty()) return Collections.emptySet();
        }
        return result;
    }

    private String generateSnippet(String content, String query, List<String> terms) {
        String lower = content.toLowerCase();
        String firstTerm = terms.stream().findFirst().orElse(null);
        int startIdx = 0;
        if (firstTerm != null) {
            int idx = lower.indexOf(firstTerm.toLowerCase());
            if (idx >= 0) startIdx = Math.max(0, idx - 80);
        }
        int endIdx = Math.min(content.length(), startIdx + 260);
        String snippet = content.substring(startIdx, endIdx).trim();
        return HtmlHighlighter.highlight(snippet, terms);
    }

    /**
     * Converts a query string into a readable title.
     * e.g. "what is machine learning" → "What Is Machine Learning"
     */
    private String toTitle(String query) {
        return Arrays.stream(query.trim().split("\\s+"))
                .map(w -> w.isEmpty() ? w :
                        Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    // -------------------------------------------------------------------------
    // Result wrapper
    // -------------------------------------------------------------------------

    public static class SavedSearchResult {
        private final AiDocument document;
        private final double score;
        private final String snippet;

        public SavedSearchResult(AiDocument document, double score, String snippet) {
            this.document = document;
            this.score = score;
            this.snippet = snippet;
        }

        public AiDocument getDocument() { return document; }
        public double getScore()        { return score; }
        public String getSnippet()      { return snippet; }
    }
}
