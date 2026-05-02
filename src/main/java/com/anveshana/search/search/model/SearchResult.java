package com.anveshana.search.search.model;

public class SearchResult {

    private final int docId;
    private final String path;
    private final double score;
    private final String content;
    private String snippet;

    public SearchResult(int docId, String path, double score, String content) {
        this.docId = docId;
        this.path = path;
        this.score = score;
        this.content = content;
    }

    public int getDocId()      { return docId; }
    public String getPath()    { return path; }
    public double getScore()   { return score; }
    public String getContent() { return content; }

    public String getSnippet()             { return snippet; }
    public void setSnippet(String snippet) { this.snippet = snippet; }
}
