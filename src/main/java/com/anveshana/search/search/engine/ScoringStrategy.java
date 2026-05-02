package com.anveshana.search.search.engine;

import java.util.List;

public interface ScoringStrategy {

    /**
     * Calculate a relevance score for a document based on its postings.
     *
     * @param postings  List of postings for a term in a document
     * @param totalDocs Total number of documents in the corpus
     * @return relevance score
     */
    double score(List<Posting> postings, int totalDocs);
}
