package com.outguess;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ReactModule(name = OutguessModule.NAME)
public class OutguessModule extends ReactContextBaseJavaModule {
    public static final String NAME = "OutguessModule";
    private static final String TAG = "OutguessModule";
    
    // Thread pool for background processing
    private final ExecutorService executorService;
    
    // Load native library
    static {
        try {
            System.loadLibrary("outguess");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load native library", e);
        }
    }

    public OutguessModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.executorService = Executors.newCachedThreadPool();
    }

    @Override
    @NonNull
    public String getName() {
        return NAME;
    }

    @ReactMethod
    public void embedMessage(
        String imagePath,
        String message,
        String outputPath,
        ReadableMap options,
        Promise promise
    ) {
        executorService.execute(() -> {
            try {
                // Validate inputs
                if (!validateImagePath(imagePath)) {
                    promise.reject("INVALID_IMAGE_PATH", "Invalid image path: " + imagePath);
                    return;
                }
                
                if (message == null || message.isEmpty()) {
                    promise.reject("INVALID_MESSAGE", "Message cannot be empty");
                    return;
                }
                
                // Extract options
                String password = options.hasKey("password") ? options.getString("password") : null;
                int compressionResistance = options.hasKey("compressionResistance") ? 
                    options.getInt("compressionResistance") : 5;
                int quality = options.hasKey("quality") ? options.getInt("quality") : 85;
                boolean verbose = options.hasKey("verbose") ? options.getBoolean("verbose") : false;
                int maxMessageSize = options.hasKey("maxMessageSize") ? 
                    options.getInt("maxMessageSize") : 65536;
                
                // Check message size
                if (message.length() > maxMessageSize) {
                    promise.reject("MESSAGE_TOO_LARGE", 
                        "Message size exceeds maximum allowed size of " + maxMessageSize + " bytes");
                    return;
                }
                
                // Call native method
                long startTime = System.currentTimeMillis();
                EmbedResult result = nativeEmbedMessage(
                    imagePath, 
                    message, 
                    outputPath, 
                    password,
                    compressionResistance,
                    quality,
                    verbose
                );
                long processingTime = System.currentTimeMillis() - startTime;
                
                if (result.success) {
                    WritableMap response = Arguments.createMap();
                    response.putString("outputPath", result.outputPath);
                    response.putInt("messageSize", result.messageSize);
                    response.putDouble("processingTime", processingTime);
                    response.putBoolean("success", true);
                    
                    // Add metadata if available
                    if (result.originalSize > 0) {
                        WritableMap metadata = Arguments.createMap();
                        metadata.putInt("originalSize", result.originalSize);
                        metadata.putInt("outputSize", result.outputSize);
                        metadata.putDouble("compressionRatio", result.compressionRatio);
                        response.putMap("metadata", metadata);
                    }
                    
                    promise.resolve(response);
                } else {
                    promise.reject("EMBED_FAILED", result.errorMessage);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error embedding message", e);
                promise.reject("EMBED_ERROR", e.getMessage());
            }
        });
    }

    @ReactMethod
    public void extractMessage(String imagePath, ReadableMap options, Promise promise) {
        executorService.execute(() -> {
            try {
                if (!validateImagePath(imagePath)) {
                    promise.reject("INVALID_IMAGE_PATH", "Invalid image path: " + imagePath);
                    return;
                }
                
                String password = options.hasKey("password") ? options.getString("password") : null;
                boolean verbose = options.hasKey("verbose") ? options.getBoolean("verbose") : false;
                
                long startTime = System.currentTimeMillis();
                ExtractResult result = nativeExtractMessage(imagePath, password, verbose);
                long processingTime = System.currentTimeMillis() - startTime;
                
                if (result.success) {
                    WritableMap response = Arguments.createMap();
                    response.putString("message", result.message);
                    response.putInt("messageSize", result.messageSize);
                    response.putDouble("processingTime", processingTime);
                    response.putBoolean("success", true);
                    response.putBoolean("verified", result.verified);
                    promise.resolve(response);
                } else {
                    promise.reject("EXTRACT_FAILED", result.errorMessage);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error extracting message", e);
                promise.reject("EXTRACT_ERROR", e.getMessage());
            }
        });
    }

    @ReactMethod
    public void hasHiddenData(String imagePath, Promise promise) {
        executorService.execute(() -> {
            try {
                if (!validateImagePath(imagePath)) {
                    promise.reject("INVALID_IMAGE_PATH", "Invalid image path: " + imagePath);
                    return;
                }
                
                boolean hasData = nativeHasHiddenData(imagePath);
                promise.resolve(hasData);
                
            } catch (Exception e) {
                Log.e(TAG, "Error checking hidden data", e);
                promise.reject("CHECK_ERROR", e.getMessage());
            }
        });
    }

    @ReactMethod
    public void getMaxMessageSize(String imagePath, ReadableMap options, Promise promise) {
        executorService.execute(() -> {
            try {
                if (!validateImagePath(imagePath)) {
                    promise.reject("INVALID_IMAGE_PATH", "Invalid image path: " + imagePath);
                    return;
                }
                
                int compressionResistance = options.hasKey("compressionResistance") ? 
                    options.getInt("compressionResistance") : 5;
                int quality = options.hasKey("quality") ? options.getInt("quality") : 85;
                
                int maxSize = nativeGetMaxMessageSize(imagePath, compressionResistance, quality);
                promise.resolve(maxSize);
                
            } catch (Exception e) {
                Log.e(TAG, "Error getting max message size", e);
                promise.reject("MAX_SIZE_ERROR", e.getMessage());
            }
        });
    }

    @ReactMethod
    public void testCompressionResistance(
        String imagePath, 
        int compressionQuality, 
        @Nullable String password, 
        Promise promise
    ) {
        executorService.execute(() -> {
            try {
                if (!validateImagePath(imagePath)) {
                    promise.reject("INVALID_IMAGE_PATH", "Invalid image path: " + imagePath);
                    return;
                }
                
                if (compressionQuality < 1 || compressionQuality > 100) {
                    promise.reject("INVALID_QUALITY", "Compression quality must be between 1 and 100");
                    return;
                }
                
                boolean survives = nativeTestCompressionResistance(imagePath, compressionQuality, password);
                promise.resolve(survives);
                
            } catch (Exception e) {
                Log.e(TAG, "Error testing compression resistance", e);
                promise.reject("TEST_ERROR", e.getMessage());
            }
        });
    }

    @ReactMethod
    public void getVersion(Promise promise) {
        try {
            String version = nativeGetVersion();
            promise.resolve(version);
        } catch (Exception e) {
            promise.resolve("1.0.0"); // Fallback version
        }
    }

    private boolean validateImagePath(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return false;
        }
        
        File file = new File(imagePath);
        if (!file.exists() || !file.isFile()) {
            return false;
        }
        
        String lowerPath = imagePath.toLowerCase();
        return lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg");
    }

    // Native method declarations
    private native EmbedResult nativeEmbedMessage(
        String imagePath,
        String message,
        String outputPath,
        String password,
        int compressionResistance,
        int quality,
        boolean verbose
    );

    private native ExtractResult nativeExtractMessage(
        String imagePath,
        String password,
        boolean verbose
    );

    private native boolean nativeHasHiddenData(String imagePath);

    private native int nativeGetMaxMessageSize(
        String imagePath,
        int compressionResistance,
        int quality
    );

    private native boolean nativeTestCompressionResistance(
        String imagePath,
        int compressionQuality,
        String password
    );

    private native String nativeGetVersion();

    // Result classes for native methods
    public static class EmbedResult {
        public boolean success;
        public String outputPath;
        public int messageSize;
        public int originalSize;
        public int outputSize;
        public double compressionRatio;
        public String errorMessage;
    }

    public static class ExtractResult {
        public boolean success;
        public String message;
        public int messageSize;
        public boolean verified;
        public String errorMessage;
    }
}