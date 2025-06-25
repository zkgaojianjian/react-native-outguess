package com.outguess.server.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Outguess解码响应模型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OutguessResponse {
    
    private boolean success;
    private String message;
    private Integer messageSize;
    private Long processingTime;
    private Boolean verified;
    private String errorCode;
    private String errorMessage;
    private ResponseMetadata metadata;
    
    // Constructors
    public OutguessResponse() {}
    
    public OutguessResponse(boolean success) {
        this.success = success;
    }
    
    public static OutguessResponse success(String message, int messageSize, long processingTime, boolean verified) {
        OutguessResponse response = new OutguessResponse(true);
        response.setMessage(message);
        response.setMessageSize(messageSize);
        response.setProcessingTime(processingTime);
        response.setVerified(verified);
        return response;
    }
    
    public static OutguessResponse error(String errorCode, String errorMessage) {
        OutguessResponse response = new OutguessResponse(false);
        response.setErrorCode(errorCode);
        response.setErrorMessage(errorMessage);
        return response;
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Integer getMessageSize() {
        return messageSize;
    }
    
    public void setMessageSize(Integer messageSize) {
        this.messageSize = messageSize;
    }
    
    public Long getProcessingTime() {
        return processingTime;
    }
    
    public void setProcessingTime(Long processingTime) {
        this.processingTime = processingTime;
    }
    
    public Boolean getVerified() {
        return verified;
    }
    
    public void setVerified(Boolean verified) {
        this.verified = verified;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public ResponseMetadata getMetadata() {
        return metadata;
    }
    
    public void setMetadata(ResponseMetadata metadata) {
        this.metadata = metadata;
    }
    
    /**
     * 响应元数据
     */
    public static class ResponseMetadata {
        private String version = "1.0.0";
        private long timestamp = System.currentTimeMillis();
        private String serverInfo = "Outguess Java Server";
        
        // Getters and Setters
        public String getVersion() {
            return version;
        }
        
        public void setVersion(String version) {
            this.version = version;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
        
        public String getServerInfo() {
            return serverInfo;
        }
        
        public void setServerInfo(String serverInfo) {
            this.serverInfo = serverInfo;
        }
    }
}