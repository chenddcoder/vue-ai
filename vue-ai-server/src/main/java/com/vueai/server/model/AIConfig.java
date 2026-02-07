package com.vueai.server.model;

public class AIConfig {
    private Integer id;
    private Integer userId;
    private String providerId;
    private String modelId;
    private String config; // JSON string
    private Boolean isActive;
    private String createTime;
    private String updateTime;

    public AIConfig() {}

    public AIConfig(Integer userId, String providerId, String modelId, String config, Boolean isActive) {
        this.userId = userId;
        this.providerId = providerId;
        this.modelId = modelId;
        this.config = config;
        this.isActive = isActive;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "AIConfig{" +
                "id=" + id +
                ", userId=" + userId +
                ", providerId='" + providerId + '\'' +
                ", modelId='" + modelId + '\'' +
                ", config='" + config + '\'' +
                ", isActive=" + isActive +
                ", createTime='" + createTime + '\'' +
                ", updateTime='" + updateTime + '\'' +
                '}';
    }
}