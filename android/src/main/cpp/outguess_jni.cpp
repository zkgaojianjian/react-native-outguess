#include <jni.h>
#include <android/log.h>
#include <string>
#include <memory>
#include "outguess.h"

#define LOG_TAG "OutguessJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Helper function to convert Java string to C string
std::string jstring_to_string(JNIEnv* env, jstring jstr) {
    if (!jstr) return "";
    
    const char* chars = env->GetStringUTFChars(jstr, nullptr);
    std::string result(chars);
    env->ReleaseStringUTFChars(jstr, chars);
    return result;
}

// Helper function to create Java string from C string
jstring string_to_jstring(JNIEnv* env, const std::string& str) {
    return env->NewStringUTF(str.c_str());
}

extern "C" {

JNIEXPORT jobject JNICALL
Java_com_outguess_OutguessModule_nativeEmbedMessage(
    JNIEnv* env,
    jobject /* this */,
    jstring imagePath,
    jstring message,
    jstring outputPath,
    jstring password,
    jint compressionResistance,
    jint quality,
    jboolean verbose
) {
    LOGI("Starting message embedding");
    
    // Convert Java strings to C++ strings
    std::string image_path = jstring_to_string(env, imagePath);
    std::string msg = jstring_to_string(env, message);
    std::string output_path = jstring_to_string(env, outputPath);
    std::string pwd = password ? jstring_to_string(env, password) : "";
    
    // Create configuration
    outguess_config_t config = {};
    config.password = pwd.empty() ? nullptr : pwd.c_str();
    config.compression_resistance = compressionResistance;
    config.quality = quality;
    config.verbose = verbose;
    config.max_message_size = 1000000; // 1MB default
    
    // Call native function
    outguess_embed_result_t* result = outguess_embed_message(
        image_path.c_str(),
        msg.c_str(),
        output_path.c_str(),
        &config
    );
    
    // Find the EmbedResult class
    jclass embedResultClass = env->FindClass("com/outguess/OutguessModule$EmbedResult");
    if (!embedResultClass) {
        LOGE("Failed to find EmbedResult class");
        if (result) outguess_free_embed_result(result);
        return nullptr;
    }
    
    // Create new instance
    jmethodID constructor = env->GetMethodID(embedResultClass, "<init>", "()V");
    jobject embedResult = env->NewObject(embedResultClass, constructor);
    
    // Set fields
    jfieldID successField = env->GetFieldID(embedResultClass, "success", "Z");
    jfieldID outputPathField = env->GetFieldID(embedResultClass, "outputPath", "Ljava/lang/String;");
    jfieldID messageSizeField = env->GetFieldID(embedResultClass, "messageSize", "I");
    jfieldID originalSizeField = env->GetFieldID(embedResultClass, "originalSize", "I");
    jfieldID outputSizeField = env->GetFieldID(embedResultClass, "outputSize", "I");
    jfieldID compressionRatioField = env->GetFieldID(embedResultClass, "compressionRatio", "D");
    jfieldID errorMessageField = env->GetFieldID(embedResultClass, "errorMessage", "Ljava/lang/String;");
    
    if (result && result->error_code == OUTGUESS_SUCCESS) {
        env->SetBooleanField(embedResult, successField, JNI_TRUE);
        env->SetObjectField(embedResult, outputPathField, string_to_jstring(env, result->output_path));
        env->SetIntField(embedResult, messageSizeField, result->message_size);
        env->SetIntField(embedResult, originalSizeField, result->original_size);
        env->SetIntField(embedResult, outputSizeField, result->output_size);
        env->SetDoubleField(embedResult, compressionRatioField, result->compression_ratio);
        
        LOGI("Message embedding successful");
    } else {
        env->SetBooleanField(embedResult, successField, JNI_FALSE);
        std::string error_msg = result && result->error_message ? 
            result->error_message : "Unknown error occurred";
        env->SetObjectField(embedResult, errorMessageField, string_to_jstring(env, error_msg));
        
        LOGE("Message embedding failed: %s", error_msg.c_str());
    }
    
    // Cleanup
    if (result) outguess_free_embed_result(result);
    
    return embedResult;
}

JNIEXPORT jobject JNICALL
Java_com_outguess_OutguessModule_nativeExtractMessage(
    JNIEnv* env,
    jobject /* this */,
    jstring imagePath,
    jstring password,
    jboolean verbose
) {
    LOGI("Starting message extraction");
    
    // Convert Java strings to C++ strings
    std::string image_path = jstring_to_string(env, imagePath);
    std::string pwd = password ? jstring_to_string(env, password) : "";
    
    // Create configuration
    outguess_config_t config = {};
    config.password = pwd.empty() ? nullptr : pwd.c_str();
    config.verbose = verbose;
    
    // Call native function
    outguess_extract_result_t* result = outguess_extract_message(
        image_path.c_str(),
        &config
    );
    
    // Find the ExtractResult class
    jclass extractResultClass = env->FindClass("com/outguess/OutguessModule$ExtractResult");
    if (!extractResultClass) {
        LOGE("Failed to find ExtractResult class");
        if (result) outguess_free_extract_result(result);
        return nullptr;
    }
    
    // Create new instance
    jmethodID constructor = env->GetMethodID(extractResultClass, "<init>", "()V");
    jobject extractResult = env->NewObject(extractResultClass, constructor);
    
    // Set fields
    jfieldID successField = env->GetFieldID(extractResultClass, "success", "Z");
    jfieldID messageField = env->GetFieldID(extractResultClass, "message", "Ljava/lang/String;");
    jfieldID messageSizeField = env->GetFieldID(extractResultClass, "messageSize", "I");
    jfieldID verifiedField = env->GetFieldID(extractResultClass, "verified", "Z");
    jfieldID errorMessageField = env->GetFieldID(extractResultClass, "errorMessage", "Ljava/lang/String;");
    
    if (result && result->error_code == OUTGUESS_SUCCESS) {
        env->SetBooleanField(extractResult, successField, JNI_TRUE);
        env->SetObjectField(extractResult, messageField, string_to_jstring(env, result->message));
        env->SetIntField(extractResult, messageSizeField, result->message_size);
        env->SetBooleanField(extractResult, verifiedField, result->verified ? JNI_TRUE : JNI_FALSE);
        
        LOGI("Message extraction successful, size: %d", result->message_size);
    } else {
        env->SetBooleanField(extractResult, successField, JNI_FALSE);
        std::string error_msg = result && result->error_message ? 
            result->error_message : "Unknown error occurred";
        env->SetObjectField(extractResult, errorMessageField, string_to_jstring(env, error_msg));
        
        LOGE("Message extraction failed: %s", error_msg.c_str());
    }
    
    // Cleanup
    if (result) outguess_free_extract_result(result);
    
    return extractResult;
}

JNIEXPORT jboolean JNICALL
Java_com_outguess_OutguessModule_nativeHasHiddenData(
    JNIEnv* env,
    jobject /* this */,
    jstring imagePath
) {
    std::string image_path = jstring_to_string(env, imagePath);
    bool has_data = outguess_has_hidden_data(image_path.c_str());
    
    LOGI("Hidden data check for %s: %s", image_path.c_str(), has_data ? "true" : "false");
    
    return has_data ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_com_outguess_OutguessModule_nativeGetMaxMessageSize(
    JNIEnv* env,
    jobject /* this */,
    jstring imagePath,
    jint compressionResistance,
    jint quality
) {
    std::string image_path = jstring_to_string(env, imagePath);
    int max_size = outguess_get_max_message_size(
        image_path.c_str(),
        compressionResistance,
        quality
    );
    
    LOGI("Max message size for %s: %d bytes", image_path.c_str(), max_size);
    
    return max_size;
}

JNIEXPORT jboolean JNICALL
Java_com_outguess_OutguessModule_nativeTestCompressionResistance(
    JNIEnv* env,
    jobject /* this */,
    jstring imagePath,
    jint compressionQuality,
    jstring password
) {
    std::string image_path = jstring_to_string(env, imagePath);
    std::string pwd = password ? jstring_to_string(env, password) : "";
    
    bool survives = outguess_test_compression_resistance(
        image_path.c_str(),
        compressionQuality,
        pwd.empty() ? nullptr : pwd.c_str()
    );
    
    LOGI("Compression resistance test at quality %d: %s", 
         compressionQuality, survives ? "PASS" : "FAIL");
    
    return survives ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_outguess_OutguessModule_nativeGetVersion(
    JNIEnv* env,
    jobject /* this */
) {
    const char* version = outguess_get_version();
    return string_to_jstring(env, version);
}

} // extern "C"