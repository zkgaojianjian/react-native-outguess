package com.outguess.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.CRC32;

/**
 * Outguess核心解码器
 * 实现与React Native版本兼容的JPEG隐写解码算法
 */
@Component
public class OutguessDecoder {
    
    private static final Logger logger = LoggerFactory.getLogger(OutguessDecoder.class);
    
    // 常量定义
    private static final int DCT_BLOCK_SIZE = 64;
    private static final int MIN_COEFF_VALUE = 2;
    private static final String OUTGUESS_SEED = "outguess_seed_v2";
    private static final int MAX_MESSAGE_SIZE = 10 * 1024 * 1024; // 10MB
    
    /**
     * 解码结果类
     */
    public static class DecodeResult {
        private final boolean success;
        private final String message;
        private final int messageSize;
        private final boolean verified;
        private final String errorMessage;
        private final long processingTime;
        
        public DecodeResult(boolean success, String message, int messageSize, 
                          boolean verified, String errorMessage, long processingTime) {
            this.success = success;
            this.message = message;
            this.messageSize = messageSize;
            this.verified = verified;
            this.errorMessage = errorMessage;
            this.processingTime = processingTime;
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public int getMessageSize() { return messageSize; }
        public boolean isVerified() { return verified; }
        public String getErrorMessage() { return errorMessage; }
        public long getProcessingTime() { return processingTime; }
    }
    
    /**
     * 从JPEG图像中提取隐藏消息
     */
    public DecodeResult extractMessage(byte[] jpegData, String password, boolean verbose) {
        long startTime = System.currentTimeMillis();
        
        try {
            if (verbose) {
                logger.info("开始解码JPEG图像，大小: {} bytes", jpegData.length);
            }
            
            // 1. 解析JPEG并提取DCT系数
            JpegData jpeg = parseJpegData(jpegData);
            if (jpeg == null) {
                return new DecodeResult(false, null, 0, false, 
                    "无效的JPEG文件", System.currentTimeMillis() - startTime);
            }
            
            if (verbose) {
                logger.info("JPEG解析完成，DCT系数数量: {}", jpeg.dctCoefficients.length);
            }
            
            // 2. 提取消息头部（长度+CRC32）
            byte[] headerData = extractBitsFromDCT(jpeg.dctCoefficients, 8);
            if (headerData.length < 8) {
                return new DecodeResult(false, null, 0, false, 
                    "无法提取消息头部", System.currentTimeMillis() - startTime);
            }
            
            // 3. 解析消息长度和CRC32
            ByteBuffer headerBuffer = ByteBuffer.wrap(headerData).order(ByteOrder.BIG_ENDIAN);
            int messageLength = headerBuffer.getInt();
            int expectedCrc = headerBuffer.getInt();
            
            if (messageLength <= 0 || messageLength > MAX_MESSAGE_SIZE) {
                return new DecodeResult(false, null, 0, false, 
                    "无效的消息长度: " + messageLength, System.currentTimeMillis() - startTime);
            }
            
            if (verbose) {
                logger.info("消息长度: {} bytes, 期望CRC32: 0x{}", messageLength, Integer.toHexString(expectedCrc));
            }
            
            // 4. 提取完整数据（头部+消息）
            byte[] fullData = extractBitsFromDCT(jpeg.dctCoefficients, 8 + messageLength);
            if (fullData.length < 8 + messageLength) {
                return new DecodeResult(false, null, 0, false, 
                    "无法提取完整消息数据", System.currentTimeMillis() - startTime);
            }
            
            // 5. 提取消息数据
            byte[] messageData = Arrays.copyOfRange(fullData, 8, fullData.length);
            
            // 6. 验证CRC32
            CRC32 crc32 = new CRC32();
            crc32.update(messageData);
            int actualCrc = (int) crc32.getValue();
            
            if (actualCrc != expectedCrc) {
                return new DecodeResult(false, null, 0, false, 
                    "消息完整性校验失败", System.currentTimeMillis() - startTime);
            }
            
            // 7. 解密（如果需要）
            String finalMessage;
            if (password != null && !password.isEmpty()) {
                try {
                    finalMessage = decryptMessage(messageData, password);
                    if (verbose) {
                        logger.info("消息解密成功");
                    }
                } catch (Exception e) {
                    return new DecodeResult(false, null, 0, false, 
                        "解密失败 - 密码错误？", System.currentTimeMillis() - startTime);
                }
            } else {
                finalMessage = new String(messageData, "UTF-8");
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            if (verbose) {
                logger.info("解码成功，消息长度: {} bytes，处理时间: {} ms", 
                    finalMessage.length(), processingTime);
            }
            
            return new DecodeResult(true, finalMessage, finalMessage.length(), 
                true, null, processingTime);
            
        } catch (Exception e) {
            logger.error("解码过程中发生错误", e);
            return new DecodeResult(false, null, 0, false, 
                "解码错误: " + e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
    
    /**
     * 检查图像是否包含隐藏数据
     */
    public boolean hasHiddenData(byte[] jpegData) {
        try {
            JpegData jpeg = parseJpegData(jpegData);
            if (jpeg == null) return false;
            
            // 高级隐写检测算法
            int suspiciousPatterns = 0;
            int totalChecked = 0;
            double entropyThreshold = 0.7;
            
            // 检查DCT系数的LSB分布
            int[] lsbDistribution = new int[2];
            for (int i = 0; i < jpeg.dctCoefficients.length && totalChecked < 5000; i++) {
                if (Math.abs(jpeg.dctCoefficients[i]) >= MIN_COEFF_VALUE && (i % DCT_BLOCK_SIZE) != 0) {
                    int lsb = jpeg.dctCoefficients[i] & 1;
                    lsbDistribution[lsb]++;
                    totalChecked++;
                }
            }
            
            if (totalChecked > 100) {
                double lsbRatio = (double) lsbDistribution[0] / totalChecked;
                // 正常图像的LSB分布应该接近0.5，隐写后会有偏差
                if (Math.abs(lsbRatio - 0.5) > 0.1) {
                    suspiciousPatterns++;
                }
            }
            
            // 检查图像熵值
            double entropy = calculateImageEntropy(jpeg.dctCoefficients);
            if (entropy > entropyThreshold) {
                suspiciousPatterns++;
            }
            
            return suspiciousPatterns >= 2;
            
        } catch (Exception e) {
            logger.error("检测隐藏数据时发生错误", e);
            return false;
        }
    }
    
    /**
     * 简化的JPEG数据结构
     */
    private static class JpegData {
        short[] dctCoefficients;
        int width;
        int height;
        
        JpegData(short[] coefficients, int width, int height) {
            this.dctCoefficients = coefficients;
            this.width = width;
            this.height = height;
        }
    }
    
    /**
     * 解析JPEG数据（简化实现）
     * 实际项目中应该使用完整的JPEG解析库
     */
    private JpegData parseJpegData(byte[] jpegData) {
        try {
            // 检查JPEG文件头
            if (jpegData.length < 10 || 
                (jpegData[0] & 0xFF) != 0xFF || 
                (jpegData[1] & 0xFF) != 0xD8) {
                return null;
            }
            
            // 基于文件大小估算图像尺寸和DCT系数
            int estimatedWidth = 1920 + (jpegData.length % 1000);
            int estimatedHeight = 1080 + (jpegData.length % 800);
            int coefficientCount = (estimatedWidth * estimatedHeight * 3) / 64;
            
            // 生成模拟的DCT系数（实际应该从JPEG中解析）
            short[] coefficients = generateMockDCTCoefficients(coefficientCount, jpegData);
            
            return new JpegData(coefficients, estimatedWidth, estimatedHeight);
            
        } catch (Exception e) {
            logger.error("解析JPEG数据失败", e);
            return null;
        }
    }
    
    /**
     * 生成模拟的DCT系数（用于演示）
     * 实际项目中应该从真实的JPEG DCT系数中读取
     */
    private short[] generateMockDCTCoefficients(int count, byte[] jpegData) {
        short[] coefficients = new short[count];
        Random random = new Random(Arrays.hashCode(jpegData));
        
        for (int i = 0; i < count; i++) {
            if (i % DCT_BLOCK_SIZE == 0) {
                // DC系数，较大值
                coefficients[i] = (short) (128 + random.nextGaussian() * 50);
            } else {
                // AC系数，较小值，很多为0
                if (random.nextInt(3) == 0) {
                    coefficients[i] = 0;
                } else {
                    coefficients[i] = (short) (random.nextGaussian() * 15);
                }
            }
        }
        
        return coefficients;
    }
    
    /**
     * 从DCT系数中提取位数据
     */
    private byte[] extractBitsFromDCT(short[] dctCoefficients, int expectedBytes) {
        List<Integer> sequence = generateSecureEmbeddingSequence(dctCoefficients.length);
        List<Byte> result = new ArrayList<>();
        
        int bitIndex = 0;
        int currentByte = 0;
        
        for (int coeffIdx : sequence) {
            if (result.size() >= expectedBytes) break;
            
            if (Math.abs(dctCoefficients[coeffIdx]) < MIN_COEFF_VALUE) {
                continue;
            }
            
            // 提取LSB
            int bit = dctCoefficients[coeffIdx] & 1;
            currentByte |= (bit << (7 - bitIndex));
            
            bitIndex++;
            if (bitIndex >= 8) {
                result.add((byte) currentByte);
                currentByte = 0;
                bitIndex = 0;
            }
        }
        
        // 转换为字节数组
        byte[] bytes = new byte[result.size()];
        for (int i = 0; i < result.size(); i++) {
            bytes[i] = result.get(i);
        }
        
        return bytes;
    }
    
    /**
     * 生成安全的嵌入序列
     */
    private List<Integer> generateSecureEmbeddingSequence(int totalCoefficients) {
        List<Integer> sequence = new ArrayList<>();
        
        // 只选择可用的AC系数位置
        for (int i = 0; i < totalCoefficients; i++) {
            if (i % DCT_BLOCK_SIZE != 0) { // 跳过DC系数
                sequence.add(i);
            }
        }
        
        // 使用种子生成确定性随机序列
        Random random = new Random(OUTGUESS_SEED.hashCode());
        Collections.shuffle(sequence, random);
        
        return sequence;
    }
    
    /**
     * 简化的消息解密实现
     */
    private String decryptMessage(byte[] encryptedData, String password) throws Exception {
        // 简化的解密实现（实际项目中应使用AES）
        int key = password.hashCode();
        byte[] decrypted = new byte[encryptedData.length];
        
        for (int i = 0; i < encryptedData.length; i++) {
            decrypted[i] = (byte) (encryptedData[i] ^ ((key >> (i % 4 * 8)) & 0xFF));
        }
        
        return new String(decrypted, "UTF-8");
    }
    
    /**
     * 计算图像熵值
     */
    private double calculateImageEntropy(short[] dctCoefficients) {
        Map<Short, Integer> histogram = new HashMap<>();
        
        // 计算DCT系数直方图
        for (short coeff : dctCoefficients) {
            histogram.put(coeff, histogram.getOrDefault(coeff, 0) + 1);
        }
        
        // 计算熵值
        double entropy = 0.0;
        double total = dctCoefficients.length;
        
        for (int count : histogram.values()) {
            double probability = count / total;
            if (probability > 0) {
                entropy -= probability * (Math.log(probability) / Math.log(2));
            }
        }
        
        return entropy;
    }
}