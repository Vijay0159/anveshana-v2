package com.anveshana.search.search.model;

import java.util.ArrayList;
import java.util.List;

public class NodeInfo {

    private final String path;
    private final boolean isFile;
    private final List<NodeInfo> children = new ArrayList<>();

    public NodeInfo(String path, boolean isFile) {
        this.path = path;
        this.isFile = isFile;
    }

    public void addChild(NodeInfo child) { children.add(child); }

    public String getPath()           { return path; }
    public boolean isFile()           { return isFile; }
    public List<NodeInfo> getChildren(){ return children; }
}
