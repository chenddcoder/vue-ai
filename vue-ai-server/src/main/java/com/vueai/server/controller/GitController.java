package com.vueai.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/git")
@CrossOrigin
public class GitController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 生成提交hash
    private String generateCommitHash() {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
            String random = UUID.randomUUID().toString();
            String input = timestamp + random;
            
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 8);
        } catch (Exception e) {
            return UUID.randomUUID().toString().substring(0, 8);
        }
    }

    // 获取项目提交历史
    @GetMapping("/commits/{projectId}")
    public Map<String, Object> getCommits(
            @PathVariable Integer projectId,
            @RequestParam(defaultValue = "main") String branch,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        
        Map<String, Object> result = new HashMap<>();
        try {
            // 验证项目存在
            List<Map<String, Object>> projects = jdbcTemplate.queryForList(
                "SELECT * FROM magic_sys_project WHERE id = ?", projectId);
            if (projects.isEmpty()) {
                result.put("code", 404);
                result.put("message", "Project not found");
                return result;
            }

            // 获取提交总数
            Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM magic_sys_project_commit WHERE project_id = ? AND branch = ?",
                Integer.class, projectId, branch);

            // 获取提交列表
            int offset = (page - 1) * pageSize;
            List<Map<String, Object>> commits = jdbcTemplate.queryForList(
                "SELECT c.*, u.avatar as author_avatar " +
                "FROM magic_sys_project_commit c " +
                "LEFT JOIN magic_sys_user u ON c.author_id = u.id " +
                "WHERE c.project_id = ? AND c.branch = ? " +
                "ORDER BY c.create_time DESC " +
                "LIMIT ? OFFSET ?",
                projectId, branch, pageSize, offset);

            result.put("code", 200);
            result.put("data", commits);
            result.put("total", total);
            result.put("page", page);
            result.put("pageSize", pageSize);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // 创建提交
    @PostMapping("/commit")
    public Map<String, Object> createCommit(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            Integer projectId = (Integer) body.get("projectId");
            Integer authorId = (Integer) body.get("authorId");
            String message = (String) body.get("message");
            String content = (String) body.get("content");
            String branch = (String) body.getOrDefault("branch", "main");

            if (projectId == null || message == null || content == null) {
                result.put("code", 400);
                result.put("message", "Missing required fields");
                return result;
            }

            // 获取作者信息
            String authorName = "Unknown";
            if (authorId != null) {
                List<Map<String, Object>> users = jdbcTemplate.queryForList(
                    "SELECT username FROM magic_sys_user WHERE id = ?", authorId);
                if (!users.isEmpty()) {
                    authorName = (String) users.get(0).get("username");
                }
            }

            // 获取父提交hash
            String parentHash = null;
            List<Map<String, Object>> lastCommit = jdbcTemplate.queryForList(
                "SELECT commit_hash FROM magic_sys_project_commit " +
                "WHERE project_id = ? AND branch = ? " +
                "ORDER BY create_time DESC LIMIT 1",
                projectId, branch);
            if (!lastCommit.isEmpty()) {
                parentHash = (String) lastCommit.get(0).get("commit_hash");
            }

            // 生成新的commit hash
            String commitHash = generateCommitHash();

            // 插入提交记录
            jdbcTemplate.update(
                "INSERT INTO magic_sys_project_commit " +
                "(project_id, commit_hash, parent_hash, author_id, author_name, message, content, branch) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                projectId, commitHash, parentHash, authorId, authorName, message, content, branch);

            // 更新分支当前提交
            jdbcTemplate.update(
                "UPDATE magic_sys_project_branch SET current_commit_hash = ?, update_time = CURRENT_TIMESTAMP " +
                "WHERE project_id = ? AND name = ?",
                commitHash, projectId, branch);

            result.put("code", 200);
            result.put("message", "Commit created successfully");
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("commitHash", commitHash);
            result.put("data", data);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // 获取提交详情
    @GetMapping("/commit/{commitHash}")
    public Map<String, Object> getCommitDetail(@PathVariable String commitHash) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> commits = jdbcTemplate.queryForList(
                "SELECT c.*, u.avatar as author_avatar " +
                "FROM magic_sys_project_commit c " +
                "LEFT JOIN magic_sys_user u ON c.author_id = u.id " +
                "WHERE c.commit_hash = ?",
                commitHash);

            if (commits.isEmpty()) {
                result.put("code", 404);
                result.put("message", "Commit not found");
                return result;
            }

            result.put("code", 200);
            result.put("data", commits.get(0));
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // 获取分支列表
    @GetMapping("/branches/{projectId}")
    public Map<String, Object> getBranches(@PathVariable Integer projectId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> branches = jdbcTemplate.queryForList(
                "SELECT b.*, c.message as last_commit_message, c.create_time as last_commit_time " +
                "FROM magic_sys_project_branch b " +
                "LEFT JOIN magic_sys_project_commit c ON b.current_commit_hash = c.commit_hash " +
                "WHERE b.project_id = ? " +
                "ORDER BY b.is_default DESC, b.update_time DESC",
                projectId);

            result.put("code", 200);
            result.put("data", branches);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // 创建分支
    @PostMapping("/branch")
    public Map<String, Object> createBranch(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            Integer projectId = (Integer) body.get("projectId");
            String branchName = (String) body.get("name");
            String fromCommit = (String) body.get("fromCommit");

            if (projectId == null || branchName == null) {
                result.put("code", 400);
                result.put("message", "Missing required fields");
                return result;
            }

            // 检查分支是否已存在
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM magic_sys_project_branch WHERE project_id = ? AND name = ?",
                Integer.class, projectId, branchName);

            if (count != null && count > 0) {
                result.put("code", 400);
                result.put("message", "Branch already exists");
                return result;
            }

            // 如果没有指定fromCommit，使用当前分支的最新提交
            if (fromCommit == null) {
                List<Map<String, Object>> lastCommit = jdbcTemplate.queryForList(
                    "SELECT commit_hash FROM magic_sys_project_commit " +
                    "WHERE project_id = ? ORDER BY create_time DESC LIMIT 1",
                    projectId);
                if (!lastCommit.isEmpty()) {
                    fromCommit = (String) lastCommit.get(0).get("commit_hash");
                }
            }

            // 创建分支
            jdbcTemplate.update(
                "INSERT INTO magic_sys_project_branch (project_id, name, current_commit_hash) VALUES (?, ?, ?)",
                projectId, branchName, fromCommit);

            result.put("code", 200);
            result.put("message", "Branch created successfully");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // 删除分支
    @DeleteMapping("/branch/{projectId}/{branchName}")
    public Map<String, Object> deleteBranch(
            @PathVariable Integer projectId,
            @PathVariable String branchName) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 不能删除默认分支
            Integer isDefault = jdbcTemplate.queryForObject(
                "SELECT is_default FROM magic_sys_project_branch WHERE project_id = ? AND name = ?",
                Integer.class, projectId, branchName);

            if (isDefault != null && isDefault == 1) {
                result.put("code", 400);
                result.put("message", "Cannot delete default branch");
                return result;
            }

            jdbcTemplate.update(
                "DELETE FROM magic_sys_project_branch WHERE project_id = ? AND name = ?",
                projectId, branchName);

            result.put("code", 200);
            result.put("message", "Branch deleted successfully");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // 切换到指定提交（检出）
    @PostMapping("/checkout")
    public Map<String, Object> checkout(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            Integer projectId = (Integer) body.get("projectId");
            String commitHash = (String) body.get("commitHash");

            if (projectId == null || commitHash == null) {
                result.put("code", 400);
                result.put("message", "Missing required fields");
                return result;
            }

            // 获取提交内容
            List<Map<String, Object>> commits = jdbcTemplate.queryForList(
                "SELECT content FROM magic_sys_project_commit WHERE commit_hash = ? AND project_id = ?",
                commitHash, projectId);

            if (commits.isEmpty()) {
                result.put("code", 404);
                result.put("message", "Commit not found");
                return result;
            }

            String content = (String) commits.get(0).get("content");

            // 更新项目当前内容
            jdbcTemplate.update(
                "UPDATE magic_sys_project SET content = ? WHERE id = ?",
                content, projectId);

            result.put("code", 200);
            result.put("message", "Checkout successful");
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("content", content);
            result.put("data", data);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // 对比两个提交
    @PostMapping("/diff")
    public Map<String, Object> diff(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String fromHash = (String) body.get("fromHash");
            String toHash = (String) body.get("toHash");

            if (fromHash == null || toHash == null) {
                result.put("code", 400);
                result.put("message", "Missing required fields");
                return result;
            }

            // 获取两个提交的内容
            List<Map<String, Object>> fromCommit = jdbcTemplate.queryForList(
                "SELECT content, message, author_name, create_time FROM magic_sys_project_commit WHERE commit_hash = ?",
                fromHash);

            List<Map<String, Object>> toCommit = jdbcTemplate.queryForList(
                "SELECT content, message, author_name, create_time FROM magic_sys_project_commit WHERE commit_hash = ?",
                toHash);

            if (fromCommit.isEmpty() || toCommit.isEmpty()) {
                result.put("code", 404);
                result.put("message", "Commit not found");
                return result;
            }

            result.put("code", 200);
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("from", fromCommit.get(0));
            data.put("to", toCommit.get(0));
            result.put("data", data);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // 初始化项目Git（创建默认main分支和初始提交）
    @PostMapping("/init/{projectId}")
    public Map<String, Object> initGit(@PathVariable Integer projectId) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 检查项目是否存在
            List<Map<String, Object>> projects = jdbcTemplate.queryForList(
                "SELECT * FROM magic_sys_project WHERE id = ?", projectId);
            if (projects.isEmpty()) {
                result.put("code", 404);
                result.put("message", "Project not found");
                return result;
            }

            // 检查是否已初始化
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM magic_sys_project_branch WHERE project_id = ?",
                Integer.class, projectId);

            if (count != null && count > 0) {
                result.put("code", 400);
                result.put("message", "Git already initialized");
                return result;
            }

            // 获取项目内容
            String content = (String) projects.get(0).get("content");
            String projectName = (String) projects.get(0).get("name");
            Integer ownerId = (Integer) projects.get(0).get("owner_id");

            // 获取作者信息
            String authorName = "System";
            if (ownerId != null) {
                List<Map<String, Object>> users = jdbcTemplate.queryForList(
                    "SELECT username FROM magic_sys_user WHERE id = ?", ownerId);
                if (!users.isEmpty()) {
                    authorName = (String) users.get(0).get("username");
                }
            }

            // 创建初始提交
            String commitHash = generateCommitHash();
            jdbcTemplate.update(
                "INSERT INTO magic_sys_project_commit " +
                "(project_id, commit_hash, author_id, author_name, message, content, branch) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                projectId, commitHash, ownerId, authorName, "Initial commit", content, "main");

            // 创建main分支
            jdbcTemplate.update(
                "INSERT INTO magic_sys_project_branch (project_id, name, current_commit_hash, is_default) " +
                "VALUES (?, ?, ?, 1)",
                projectId, "main", commitHash);

            result.put("code", 200);
            result.put("message", "Git initialized successfully");
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("commitHash", commitHash);
            result.put("data", data);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // 获取远程仓库列表
    @GetMapping("/remotes/{projectId}")
    public Map<String, Object> getRemotes(@PathVariable Integer projectId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> remotes = jdbcTemplate.queryForList(
                "SELECT * FROM magic_sys_project_remote WHERE project_id = ? ORDER BY is_default DESC, name",
                projectId);

            result.put("code", 200);
            result.put("data", remotes);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // 添加远程仓库
    @PostMapping("/remote")
    public Map<String, Object> addRemote(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            Integer projectId = (Integer) body.get("projectId");
            String name = (String) body.get("name");
            String url = (String) body.get("url");
            Boolean isDefault = (Boolean) body.getOrDefault("isDefault", false);

            if (projectId == null || name == null || url == null) {
                result.put("code", 400);
                result.put("message", "Missing required fields");
                return result;
            }

            // 如果设为默认，先取消其他默认
            if (isDefault) {
                jdbcTemplate.update(
                    "UPDATE magic_sys_project_remote SET is_default = 0 WHERE project_id = ?",
                    projectId);
            }

            jdbcTemplate.update(
                "INSERT INTO magic_sys_project_remote (project_id, name, url, is_default) VALUES (?, ?, ?, ?)",
                projectId, name, url, isDefault ? 1 : 0);

            result.put("code", 200);
            result.put("message", "Remote added successfully");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // 删除远程仓库
    @DeleteMapping("/remote/{projectId}/{remoteName}")
    public Map<String, Object> deleteRemote(
            @PathVariable Integer projectId,
            @PathVariable String remoteName) {
        Map<String, Object> result = new HashMap<>();
        try {
            jdbcTemplate.update(
                "DELETE FROM magic_sys_project_remote WHERE project_id = ? AND name = ?",
                projectId, remoteName);

            result.put("code", 200);
            result.put("message", "Remote deleted successfully");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // 模拟拉取（实际项目中需要调用git命令）
    @PostMapping("/pull")
    public Map<String, Object> pull(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            Integer projectId = (Integer) body.get("projectId");
            String remoteName = (String) body.getOrDefault("remoteName", "origin");

            // 获取远程仓库信息
            List<Map<String, Object>> remotes = jdbcTemplate.queryForList(
                "SELECT * FROM magic_sys_project_remote WHERE project_id = ? AND name = ?",
                projectId, remoteName);

            if (remotes.isEmpty()) {
                result.put("code", 404);
                result.put("message", "Remote not found: " + remoteName);
                return result;
            }

            // 获取最新的远程提交（模拟）
            List<Map<String, Object>> remoteCommits = jdbcTemplate.queryForList(
                "SELECT * FROM magic_sys_project_commit WHERE project_id = ? ORDER BY create_time DESC LIMIT 1",
                projectId);

            // 模拟拉取成功
            result.put("code", 200);
            result.put("message", "Pull from " + remoteName + " successful");
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("filesUpdated", 0);
            data.put("commitsPulled", remoteCommits.size());
            data.put("message", "从 " + remoteName + " 拉取成功");
            result.put("data", data);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // 模拟推送（实际项目中需要调用git命令）
    @PostMapping("/push")
    public Map<String, Object> push(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            Integer projectId = (Integer) body.get("projectId");
            String remoteName = (String) body.getOrDefault("remoteName", "origin");
            String branchName = (String) body.getOrDefault("branch", "main");

            // 获取远程仓库信息
            List<Map<String, Object>> remotes = jdbcTemplate.queryForList(
                "SELECT * FROM magic_sys_project_remote WHERE project_id = ? AND name = ?",
                projectId, remoteName);

            if (remotes.isEmpty()) {
                result.put("code", 404);
                result.put("message", "Remote not found: " + remoteName);
                return result;
            }

            // 获取当前分支的最新提交
            List<Map<String, Object>> commits = jdbcTemplate.queryForList(
                "SELECT * FROM magic_sys_project_commit WHERE project_id = ? AND branch = ? ORDER BY create_time DESC LIMIT 1",
                projectId, branchName);

            // 模拟推送成功
            result.put("code", 200);
            result.put("message", "Push to " + remoteName + " successful");
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("commitsPushed", commits.size());
            data.put("branch", branchName);
            data.put("remote", remoteName);
            data.put("message", "推送到 " + remoteName + "/" + branchName + " 成功");
            result.put("data", data);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // 获取Git状态
    @GetMapping("/status/{projectId}")
    public Map<String, Object> getStatus(@PathVariable Integer projectId) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 获取分支数
            Integer branchCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM magic_sys_project_branch WHERE project_id = ?",
                Integer.class, projectId);

            // 获取提交数
            Integer commitCount = jdbcTemplate.queryForList(
                "SELECT * FROM magic_sys_project_commit WHERE project_id = ?",
                projectId).size();

            // 获取远程仓库数
            Integer remoteCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM magic_sys_project_remote WHERE project_id = ?",
                Integer.class, projectId);

            // 获取当前分支
            List<Map<String, Object>> currentBranch = jdbcTemplate.queryForList(
                "SELECT b.name FROM magic_sys_project_branch b WHERE b.project_id = ? AND b.is_default = 1",
                projectId);
            String currentBranchName = currentBranch.isEmpty() ? "main" : (String) currentBranch.get(0).get("name");

            // 获取未推送的提交数（最近10条）
            List<Map<String, Object>> recentCommits = jdbcTemplate.queryForList(
                "SELECT commit_hash, message, create_time FROM magic_sys_project_commit " +
                "WHERE project_id = ? AND branch = ? ORDER BY create_time DESC LIMIT 10",
                projectId, currentBranchName);

            result.put("code", 200);
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("branchCount", branchCount != null ? branchCount : 0);
            data.put("commitCount", commitCount);
            data.put("remoteCount", remoteCount != null ? remoteCount : 0);
            data.put("currentBranch", currentBranchName);
            data.put("recentCommits", recentCommits);
            data.put("hasChanges", recentCommits.size() > 0);
            data.put("isGitInitialized", commitCount > 0);
            result.put("data", data);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
