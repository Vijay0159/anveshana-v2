package com.anveshana.search.search.engine;

import com.anveshana.search.search.model.Document;

import java.util.*;

public class InvertedIndex {

    // Term → List of Postings (docId + frequency)
    private final Map<String, List<Posting>> index = new HashMap<>();

    /**
     * Adds a document to the inverted index.
     * Tokens are normalized to lowercase and frequencies are tracked per document.
     */
    public void addDocument(Document document) {
        List<String> tokens = TextTokenizer.tokenize(document.getContent());

        for (String token : tokens) {
            List<Posting> postings = index.computeIfAbsent(token, k -> new ArrayList<>());

            Optional<Posting> existing = postings.stream()
                    .filter(p -> p.getDocId() == document.getDocId())
                    .findFirst();

            if (existing.isPresent()) {
                existing.get().increment();
            } else {
                postings.add(new Posting(document.getDocId()));
            }
        }
    }

    public Set<Integer> search(String term) {
        if (term == null || term.isBlank()) {
            return Collections.emptySet();
        }

        List<Posting> postings = index.get(term.toLowerCase());
        if (postings == null) return Collections.emptySet();

        Set<Integer> docIds = new HashSet<>();
        for (Posting p : postings) {
            docIds.add(p.getDocId());
        }
        return docIds;
    }

    public Map<Integer, List<Posting>> getPostings(String term) {
        Map<Integer, List<Posting>> result = new HashMap<>();

        if (term == null || term.isBlank()) {
            return result;
        }

        List<Posting> postings = index.get(term.toLowerCase());
        if (postings != null) {
            for (Posting p : postings) {
                result.put(p.getDocId(), List.of(p));
            }
        }

        return result;
    }

    public void clear() {
        index.clear();
    }

    public void printIndex() {
        index.forEach((term, postings) -> {
            System.out.print(term + " -> [");
            for (int i = 0; i < postings.size(); i++) {
                Posting p = postings.get(i);
                System.out.print("docId: " + p.getDocId() + ", freq: " + p.getFrequency());
                if (i < postings.size() - 1) System.out.print("; ");
            }
            System.out.println("]");
        });
    }
}
