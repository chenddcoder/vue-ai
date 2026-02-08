package com.vueai.server.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

public class ProjectContextAnalyzer {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String analyzeStructure(Object contentObj) {
        try {
            if (contentObj instanceof Map) {
                // Handle flat map (path -> content)
                Map<String, String> flatFiles = (Map<String, String>) contentObj;
                return buildTreeFromFlatMap(flatFiles);
            }
            
            List<Map<String, Object>> files;
            if (contentObj instanceof String) {
                // Try to parse as Map first (flat structure)
                try {
                     Map<String, String> map = objectMapper.readValue((String) contentObj, new TypeReference<Map<String, String>>(){});
                     return buildTreeFromFlatMap(map);
                } catch (Exception e) {
                     // Fallback to List (tree structure)
                     files = objectMapper.readValue((String) contentObj, List.class);
                }
            } else {
                files = (List<Map<String, Object>>) contentObj;
            }
            StringBuilder sb = new StringBuilder();
            buildTree(files, "", sb);
            return sb.toString();
        } catch (Exception e) {
            return "Error analyzing project structure: " + e.getMessage();
        }
    }

    private static String buildTreeFromFlatMap(Map<String, String> flatFiles) {
        // Convert flat paths to tree structure for visualization
        Set<String> paths = new TreeSet<>(flatFiles.keySet());
        StringBuilder sb = new StringBuilder();
        // A simple implementation: just list files hierarchically
        // Or better: build a temporary tree node structure
        
        // Let's implement a simple indentation based on path depth
        for (String path : paths) {
            String[] parts = path.split("/");
            for (int i = 0; i < parts.length - 1; i++) {
                // We assume folders are implicit in flat map
            }
        }
        
        // Re-implementing a proper tree builder from paths
        TreeNode root = new TreeNode("root", "folder");
        for (String path : flatFiles.keySet()) {
            String[] parts = path.split("/");
            TreeNode current = root;
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                boolean isFile = (i == parts.length - 1);
                TreeNode child = current.findChild(part);
                if (child == null) {
                    child = new TreeNode(part, isFile ? "file" : "folder");
                    current.addChild(child);
                }
                current = child;
            }
        }
        
        buildTreeFromNode(root.children, "", sb);
        return sb.toString();
    }
    
    private static void buildTreeFromNode(List<TreeNode> nodes, String prefix, StringBuilder sb) {
        for (int i = 0; i < nodes.size(); i++) {
            TreeNode node = nodes.get(i);
            boolean isLast = (i == nodes.size() - 1);
            
            sb.append(prefix);
            sb.append(isLast ? "└── " : "├── ");
            sb.append(node.name);
            if ("folder".equals(node.type)) {
                sb.append("/");
            }
            sb.append("\n");
            
            if ("folder".equals(node.type)) {
                buildTreeFromNode(node.children, prefix + (isLast ? "    " : "│   "), sb);
            }
        }
    }
    
    static class TreeNode {
        String name;
        String type;
        List<TreeNode> children = new ArrayList<>();
        
        TreeNode(String name, String type) {
            this.name = name;
            this.type = type;
        }
        
        void addChild(TreeNode child) {
            children.add(child);
        }
        
        TreeNode findChild(String name) {
            for (TreeNode child : children) {
                if (child.name.equals(name)) return child;
            }
            return null;
        }
    }

    private static void buildTree(List<Map<String, Object>> nodes, String prefix, StringBuilder sb) {
        if (nodes == null) return;
        
        for (int i = 0; i < nodes.size(); i++) {
            Map<String, Object> node = nodes.get(i);
            String name = (String) node.get("name");
            String type = (String) node.get("type");
            boolean isLast = (i == nodes.size() - 1);
            
            sb.append(prefix);
            sb.append(isLast ? "└── " : "├── ");
            sb.append(name);
            if ("folder".equals(type) || "directory".equals(type)) {
                sb.append("/");
            }
            sb.append("\n");
            
            if ("folder".equals(type) || "directory".equals(type)) {
                List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
                buildTree(children, prefix + (isLast ? "    " : "│   "), sb);
            }
        }
    }
    
    public static String getDependencies(Object contentObj) {
        return findFileContent(contentObj, "package.json");
    }

    public static String findFileContent(Object contentObj, String targetFileName) {
        try {
            List<Map<String, Object>> files;
            if (contentObj instanceof String) {
                files = objectMapper.readValue((String) contentObj, List.class);
            } else {
                files = (List<Map<String, Object>>) contentObj;
            }
            return findFile(files, targetFileName);
        } catch (Exception e) {
            return null;
        }
    }
    
    private static String findFile(List<Map<String, Object>> nodes, String targetName) {
        if (nodes == null) return null;
        
        for (Map<String, Object> node : nodes) {
            String name = (String) node.get("name");
            String type = (String) node.get("type");
            
            if (targetName.equals(name) && "file".equals(type)) {
                return (String) node.get("content");
            }
            
            if ("folder".equals(type) || "directory".equals(type)) {
                List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
                String result = findFile(children, targetName);
                if (result != null) return result;
            }
        }
        return null;
    }
}
