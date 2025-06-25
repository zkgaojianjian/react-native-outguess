package com.outguess.server.service;

import com.outguess.server.config.OutguessConfig;
import com.outguess.server.model.OutguessRequest;
import com.outguess.server.model.OutguessResponse;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Outguess服务层
 */
@Service
public class OutguessService {
    
    private static final Logger logger = LoggerFactory.getLogger(OutguessService.class);
    
    @Autowired
    private OutguessDecoder decoder;
    
    @Autowired
    private OutguessConfig config;
    
    private final Executor executor = Executors.newFixedThreadPool(10);
    
    /**
     * 异步解码消息
     */
    public CompletableFuture<OutguessResponse> decodeMessageAsync(OutguessRequest request) {
        return CompletableFuture.supplyAsync(() -> decodeMessage(request), executor);
    }
    
    /**
     * 同步解码消息
     */
    public OutguessResponse decodeMessage(OutguessRequest request) {
        try {
            // 验证请求
            String validationError = validateRequest(request);
            if (validationError != null) {
                return OutguessResponse.error("INVALID_REQUEST", validationError);
            }
            
            // 解码Base64图像数据
            byte[] jpegData;
            try {
                jpegData = Base64.decodeBase64(request.getImageData());
            } catch (Exception e) {
                return OutguessResponse.error("INVALID_IMAGE_DATA", "无效的Base64图像数据");
            }
            
            // 检查文件大小
            if (jpegData.length > config.getMaxFileSize()) {
                return OutguessResponse.error("FILE_TOO_LARGE", 
                    "文件大小超过限制: " + config.getMaxFileSize() + " bytes");
            }
            
            // 执行解码
            OutguessDecoder.DecodeResult result = decoder.extractMessage(
                jpegData, 
                request.getPassword(), 
                request.isVerbose()
            );
            
            if (result.isSuccess()) {
                OutguessResponse response = OutguessResponse.success(
                    result.getMessage(),
                    result.getMessageSize(),
                    result.getProcessingTime(),
                    result.isVerified()
                );
                
                // 添加元数据
                OutguessResponse.ResponseMetadata metadata = new OutguessResponse.ResponseMetadata();
                response.setMetadata(metadata);
                
                return response;
            } else {
                return OutguessResponse.error("DECODE_FAILED", result.getErrorMessage());
            }
            
        } catch (Exception e) {
            logger.error("解码过程中发生未预期的错误", e);
            return OutguessResponse.error("INTERNAL_ERROR", "服务器内部错误");
        }
    }
    
    /**
     * 检查图像是否包含隐藏数据
     */
    public OutguessResponse checkHiddenData(OutguessRequest request) {
        try {
            // 验证请求
            if (request.getImageData() == null || request.getImageData().isEmpty()) {
                return OutguessResponse.error("INVALID_REQUEST", "图像数据不能为空");
            }
            
            // 解码Base64图像数据
            byte[] jpegData;
            try {
                jpegData = Base64.decodeBase64(request.getImageData());
            } catch (Exception e) {
                return OutguessResponse.error("INVALID_IMAGE_DATA", "无效的Base64图像数据");
            }
            
            // 检查隐藏数据
            boolean hasHiddenData = decoder.hasHiddenData(jpegData);
            
            OutguessResponse response = new OutguessResponse(true);
            response.setMessage(hasHiddenData ? "检测到隐藏数据" : "未检测到隐藏数据");
            response.setVerified(hasHiddenData);
            
            return response;
            
        } catch (Exception e) {
            logger.error("检查隐藏数据时发生错误", e);
            return OutguessResponse.error("CHECK_ERROR", "检查失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证请求参数
     */
    private String validateRequest(OutguessRequest request) {
        if (request.getImageData() == null || request.getImageData().isEmpty()) {
            return "图像数据不能为空";
        }
        
        if (request.getPassword() != null && request.getPassword().length() > 100) {
            return "密码长度不能超过100字符";
        }
        
        // 检查Base64格式
        if (!Base64.isBase64(request.getImageData())) {
            return "图像数据必须是有效的Base64格式";
        }
        
        return null;
    }
}