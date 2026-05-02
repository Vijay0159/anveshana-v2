package com.anveshana.search.search.model;

public class Document {

    private final int docId;
    private final String path;
    private final String content;

    public Document(int docId, String path, String content) {
        this.docId = docId;
        this.path = path;
        this.content = content;
    }

    public int getDocId()    { return docId; }
    public String getPath()  { return path; }
    public String getContent(){ return content; }
}
