package com.outguess.server.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Outguess解码请求模型
 */
public class OutguessRequest {
    
    @NotNull(message = "图像数据不能为空")
    private String imageData; // Base64编码的图像数据
    
    @Size(max = 100, message = "密码长度不能超过100字符")
    private String password;
    
    private boolean verbose = false;
    
    private String filename;
    
    // Constructors
    public OutguessRequest() {}
    
    public OutguessRequest(String imageData, String password) {
        this.imageData = imageData;
        this.password = password;
    }
    
    // Getters and Setters
    public String getImageData() {
        return imageData;
    }
    
    public void setImageData(String imageData) {
        this.imageData = imageData;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public boolean isVerbose() {
        return verbose;
    }
    
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
    public String getFilename() {
        return filename;
    }
    
    public void setFilename(String filename) {
        this.filename = filename;
    }
}