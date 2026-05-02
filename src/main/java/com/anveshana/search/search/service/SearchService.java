package com.anveshana.search.search.service;

import com.anveshana.search.search.engine.InvertedIndex;
import com.anveshana.search.search.engine.Posting;
import com.anveshana.search.search.engine.TextTokenizer;
import com.anveshana.search.search.model.Document;
import com.anveshana.search.search.model.SearchResult;
import com.anveshana.search.search.util.HtmlHighlighter;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SearchService {

    private final IndexService indexService;

    public SearchService(IndexService indexService) {
        this.indexService = indexService;
    }

    /**
     * Search for a query and return ranked results.
     * Multi-word queries perform AND matching across all terms.
     */
    public List<SearchResult> search(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        List<String> queryTerms = new ArrayList<>(TextTokenizer.tokenize(query));
        if (queryTerms.isEmpty()) {
            return Collections.emptyList();
        }

        InvertedIndex invertedIndex = indexService.getInvertedIndex();
        List<Document> documents = indexService.getDocuments();

        Set<Integer> matchingDocIds = getDocumentsContainingAllTerms(queryTerms, invertedIndex);
        List<SearchResult> results = new ArrayList<>();

        for (int docId : matchingDocIds) {
            List<Posting> combinedPostings = new ArrayList<>();
            for (String term : queryTerms) {
                Map<Integer, List<Posting>> postings = invertedIndex.getPostings(term);
                combinedPostings.addAll(postings.getOrDefault(docId, List.of()));
            }

            double score = indexService.getScoringStrategy().score(combinedPostings, documents.size());

            Document doc = documents.stream()
                    .filter(d -> d.getDocId() == docId)
                    .findFirst()
                    .orElse(null);

            if (doc != null) {
                String highlightedContent = HtmlHighlighter.highlight(doc.getContent(), queryTerms);
                results.add(new SearchResult(docId, doc.getPath(), score, highlightedContent));
            }
        }

        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return results;
    }

    /**
     * Returns the set of document IDs that contain ALL query terms (AND semantics).
     */
    private Set<Integer> getDocumentsContainingAllTerms(List<String> queryTerms,
                                                         InvertedIndex invertedIndex) {
        Set<Integer> result = invertedIndex.search(queryTerms.get(0));

        for (int i = 1; i < queryTerms.size(); i++) {
            Set<Integer> termDocs = invertedIndex.search(queryTerms.get(i));
            result.retainAll(termDocs);
            if (result.isEmpty()) return Collections.emptySet();
        }

        return result;
    }
}
