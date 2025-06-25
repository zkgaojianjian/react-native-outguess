package com.outguess.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.outguess.server.controller.OutguessController;
import com.outguess.server.model.OutguessRequest;
import com.outguess.server.model.OutguessResponse;
import com.outguess.server.service.OutguessService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OutguessController.class)
public class OutguessControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private OutguessService outguessService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    public void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/outguess/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Outguess服务运行正常"));
    }
    
    @Test
    public void testDecodeMessage() throws Exception {
        // 准备测试数据
        String testImageData = Base64.getEncoder().encodeToString("test image data".getBytes());
        OutguessRequest request = new OutguessRequest();
        request.setImageData(testImageData);
        request.setPassword("testpass");
        
        OutguessResponse mockResponse = OutguessResponse.success("测试消息", 8, 100L, true);
        when(outguessService.decodeMessage(any(OutguessRequest.class))).thenReturn(mockResponse);
        
        mockMvc.perform(post("/api/outguess/decode")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("测试消息"))
                .andExpect(jsonPath("$.messageSize").value(8))
                .andExpect(jsonPath("$.verified").value(true));
    }
    
    @Test
    public void testDecodeMessageWithInvalidData() throws Exception {
        OutguessRequest request = new OutguessRequest();
        request.setImageData(""); // 空数据
        
        mockMvc.perform(post("/api/outguess/decode")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    public void testCheckHiddenData() throws Exception {
        String testImageData = Base64.getEncoder().encodeToString("test image data".getBytes());
        OutguessRequest request = new OutguessRequest();
        request.setImageData(testImageData);
        
        OutguessResponse mockResponse = new OutguessResponse(true);
        mockResponse.setMessage("检测到隐藏数据");
        mockResponse.setVerified(true);
        
        when(outguessService.checkHiddenData(any(OutguessRequest.class))).thenReturn(mockResponse);
        
        mockMvc.perform(post("/api/outguess/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("检测到隐藏数据"))
                .andExpect(jsonPath("$.verified").value(true));
    }
}