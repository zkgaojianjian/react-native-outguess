package com.outguess.server.exception;

import com.outguess.server.model.OutguessResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<OutguessResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        logger.warn("参数验证失败: {}", errors);
        
        return ResponseEntity.badRequest().body(
            OutguessResponse.error("VALIDATION_ERROR", "参数验证失败: " + errors.toString()));
    }
    
    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<OutguessResponse> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        logger.warn("文件上传大小超限: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
            OutguessResponse.error("FILE_TOO_LARGE", "上传文件大小超过限制"));
    }
    
    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<OutguessResponse> handleGenericException(Exception ex) {
        logger.error("发生未处理的异常", ex);
        
        return ResponseEntity.internalServerError().body(
            OutguessResponse.error("INTERNAL_ERROR", "服务器内部错误"));
    }
    
    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<OutguessResponse> handleRuntimeException(RuntimeException ex) {
        logger.error("发生运行时异常", ex);
        
        return ResponseEntity.internalServerError().body(
            OutguessResponse.error("RUNTIME_ERROR", "运行时错误: " + ex.getMessage()));
    }
}