package com.anveshana.search.search.engine;

public class Posting {

    private final int docId;
    private int frequency;

    public Posting(int docId) {
        this.docId = docId;
        this.frequency = 1;
    }

    public void increment() { frequency++; }

    public int getDocId()    { return docId; }
    public int getFrequency(){ return frequency; }
}
