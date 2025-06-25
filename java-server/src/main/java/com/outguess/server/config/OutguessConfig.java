package com.outguess.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Outguess配置类
 */
@Configuration
@ConfigurationProperties(prefix = "outguess")
public class OutguessConfig {
    
    private int maxFileSize = 10 * 1024 * 1024; // 10MB
    private int maxMessageSize = 1024 * 1024; // 1MB
    private String tempDir = System.getProperty("java.io.tmpdir");
    private boolean enableVerboseLogging = false;
    private int maxConcurrentRequests = 10;
    
    // Getters and Setters
    public int getMaxFileSize() {
        return maxFileSize;
    }
    
    public void setMaxFileSize(int maxFileSize) {
        this.maxFileSize = maxFileSize;
    }
    
    public int getMaxMessageSize() {
        return maxMessageSize;
    }
    
    public void setMaxMessageSize(int maxMessageSize) {
        this.maxMessageSize = maxMessageSize;
    }
    
    public String getTempDir() {
        return tempDir;
    }
    
    public void setTempDir(String tempDir) {
        this.tempDir = tempDir;
    }
    
    public boolean isEnableVerboseLogging() {
        return enableVerboseLogging;
    }
    
    public void setEnableVerboseLogging(boolean enableVerboseLogging) {
        this.enableVerboseLogging = enableVerboseLogging;
    }
    
    public int getMaxConcurrentRequests() {
        return maxConcurrentRequests;
    }
    
    public void setMaxConcurrentRequests(int maxConcurrentRequests) {
        this.maxConcurrentRequests = maxConcurrentRequests;
    }
}