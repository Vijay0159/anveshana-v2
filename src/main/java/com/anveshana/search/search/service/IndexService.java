package com.anveshana.search.search.service;

import com.anveshana.search.search.engine.InvertedIndex;
import com.anveshana.search.search.engine.ScoringStrategy;
import com.anveshana.search.search.engine.Posting;
import com.anveshana.search.search.model.Document;
import com.anveshana.search.search.model.NodeInfo;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class IndexService {

    @Value("${anveshana.root.path}")
    private String rootPath;

    private final InvertedIndex invertedIndex = new InvertedIndex();
    private final List<Document> documents = new ArrayList<>();
    private final AtomicInteger docIdCounter = new AtomicInteger(1);

    /**
     * Term-frequency scoring strategy.
     * Declared here so SearchService and controllers share the same instance.
     */
    private final ScoringStrategy scoringStrategy = (postings, totalDocs) -> {
        double score = 0;
        for (Posting p : postings) {
            score += p.getFrequency();
        }
        return score;
    };

    @PostConstruct
    public void init() {
        buildIndex(rootPath);
    }

    /**
     * (Re-)builds the index from the given root path.
     * Clears any previously indexed data before starting.
     */
    public void buildIndex(String path) {
        documents.clear();
        docIdCounter.set(1);
        invertedIndex.clear();

        File root = new File(path);
        if (!root.exists() || !root.isDirectory()) {
            throw new IllegalArgumentException("Invalid directory: " + path);
        }

        NodeInfo rootNode = buildTree(root);
        collectDocuments(rootNode);
        indexDocuments();
    }

    // -------------------------------------------------------------------------
    // Accessors used by SearchService and controllers
    // -------------------------------------------------------------------------

    public InvertedIndex getInvertedIndex() {
        return invertedIndex;
    }

    public List<Document> getDocuments() {
        return Collections.unmodifiableList(documents);
    }

    public ScoringStrategy getScoringStrategy() {
        return scoringStrategy;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private NodeInfo buildTree(File file) {
        NodeInfo node = new NodeInfo(file.getAbsolutePath(), file.isFile());

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    node.addChild(buildTree(child));
                }
            }
        }
        return node;
    }

    private void collectDocuments(NodeInfo node) {
        if (node.isFile()) {
            try {
                String content = Files.readString(new File(node.getPath()).toPath());
                documents.add(new Document(docIdCounter.getAndIncrement(), node.getPath(), content));
            } catch (IOException e) {
                System.err.println("Failed to read file: " + node.getPath());
            }
            return;
        }

        for (NodeInfo child : node.getChildren()) {
            collectDocuments(child);
        }
    }

    private void indexDocuments() {
        for (Document document : documents) {
            invertedIndex.addDocument(document);
        }
    }
}
