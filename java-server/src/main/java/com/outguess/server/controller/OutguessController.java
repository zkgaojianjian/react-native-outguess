package com.outguess.server.controller;

import com.outguess.server.model.OutguessRequest;
import com.outguess.server.model.OutguessResponse;
import com.outguess.server.service.OutguessService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * Outguess REST API控制器
 */
@RestController
@RequestMapping("/api/outguess")
@CrossOrigin(origins = "*")
public class OutguessController {
    
    private static final Logger logger = LoggerFactory.getLogger(OutguessController.class);
    
    @Autowired
    private OutguessService outguessService;
    
    /**
     * 解码消息 - JSON格式
     */
    @PostMapping(value = "/decode", 
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OutguessResponse> decodeMessage(@Valid @RequestBody OutguessRequest request) {
        logger.info("收到解码请求，文件名: {}", request.getFilename());
        
        OutguessResponse response = outguessService.decodeMessage(request);
        
        if (response.isSuccess()) {
            logger.info("解码成功，消息长度: {} bytes", response.getMessageSize());
            return ResponseEntity.ok(response);
        } else {
            logger.warn("解码失败: {}", response.getErrorMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 异步解码消息
     */
    @PostMapping(value = "/decode/async",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public CompletableFuture<ResponseEntity<OutguessResponse>> decodeMessageAsync(
            @Valid @RequestBody OutguessRequest request) {
        
        logger.info("收到异步解码请求，文件名: {}", request.getFilename());
        
        return outguessService.decodeMessageAsync(request)
                .thenApply(response -> {
                    if (response.isSuccess()) {
                        logger.info("异步解码成功，消息长度: {} bytes", response.getMessageSize());
                        return ResponseEntity.ok(response);
                    } else {
                        logger.warn("异步解码失败: {}", response.getErrorMessage());
                        return ResponseEntity.badRequest().body(response);
                    }
                });
    }
    
    /**
     * 解码消息 - 文件上传格式
     */
    @PostMapping(value = "/decode/upload",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OutguessResponse> decodeMessageFromFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "verbose", defaultValue = "false") boolean verbose) {
        
        try {
            logger.info("收到文件上传解码请求，文件名: {}, 大小: {} bytes", 
                       file.getOriginalFilename(), file.getSize());
            
            // 验证文件类型
            if (!isJpegFile(file)) {
                return ResponseEntity.badRequest().body(
                    OutguessResponse.error("INVALID_FILE_TYPE", "只支持JPEG图像文件"));
            }
            
            // 转换为Base64
            byte[] fileBytes = file.getBytes();
            String base64Data = Base64.getEncoder().encodeToString(fileBytes);
            
            // 创建请求对象
            OutguessRequest request = new OutguessRequest();
            request.setImageData(base64Data);
            request.setPassword(password);
            request.setVerbose(verbose);
            request.setFilename(file.getOriginalFilename());
            
            // 执行解码
            OutguessResponse response = outguessService.decodeMessage(request);
            
            if (response.isSuccess()) {
                logger.info("文件上传解码成功，消息长度: {} bytes", response.getMessageSize());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("文件上传解码失败: {}", response.getErrorMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            logger.error("处理文件上传时发生错误", e);
            return ResponseEntity.internalServerError().body(
                OutguessResponse.error("UPLOAD_ERROR", "文件处理失败: " + e.getMessage()));
        }
    }
    
    /**
     * 检查隐藏数据
     */
    @PostMapping(value = "/check",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OutguessResponse> checkHiddenData(@Valid @RequestBody OutguessRequest request) {
        logger.info("收到隐藏数据检查请求");
        
        OutguessResponse response = outguessService.checkHiddenData(request);
        
        if (response.isSuccess()) {
            logger.info("隐藏数据检查完成: {}", response.getMessage());
            return ResponseEntity.ok(response);
        } else {
            logger.warn("隐藏数据检查失败: {}", response.getErrorMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<OutguessResponse> healthCheck() {
        OutguessResponse response = new OutguessResponse(true);
        response.setMessage("Outguess服务运行正常");
        
        OutguessResponse.ResponseMetadata metadata = new OutguessResponse.ResponseMetadata();
        response.setMetadata(metadata);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取服务信息
     */
    @GetMapping("/info")
    public ResponseEntity<OutguessResponse> getServiceInfo() {
        OutguessResponse response = new OutguessResponse(true);
        response.setMessage("Outguess Java服务端解码器");
        
        OutguessResponse.ResponseMetadata metadata = new OutguessResponse.ResponseMetadata();
        metadata.setServerInfo("Outguess Java Server v1.0.0");
        response.setMetadata(metadata);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 验证是否为JPEG文件
     */
    private boolean isJpegFile(MultipartFile file) {
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        
        return (contentType != null && contentType.startsWith("image/jpeg")) ||
               (filename != null && (filename.toLowerCase().endsWith(".jpg") || 
                                   filename.toLowerCase().endsWith(".jpeg")));
    }
}