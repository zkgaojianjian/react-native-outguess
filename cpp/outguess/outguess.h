#ifndef OUTGUESS_H
#define OUTGUESS_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>
#include <stdbool.h>

// Error codes
typedef enum {
    OUTGUESS_SUCCESS = 0,
    OUTGUESS_ERROR_INVALID_INPUT = -1,
    OUTGUESS_ERROR_FILE_NOT_FOUND = -2,
    OUTGUESS_ERROR_INVALID_JPEG = -3,
    OUTGUESS_ERROR_MESSAGE_TOO_LARGE = -4,
    OUTGUESS_ERROR_COMPRESSION_FAILED = -5,
    OUTGUESS_ERROR_EXTRACTION_FAILED = -6,
    OUTGUESS_ERROR_MEMORY_ALLOCATION = -7,
    OUTGUESS_ERROR_CRYPTO_FAILED = -8
} outguess_error_t;

// Configuration structure
typedef struct {
    const char* password;           // Optional password for encryption
    int compression_resistance;     // 1-10, higher = more resistant
    int quality;                   // JPEG quality 1-100
    bool verbose;                  // Enable verbose logging
    int max_message_size;          // Maximum message size in bytes
} outguess_config_t;

// Result structures
typedef struct {
    char* output_path;
    int message_size;
    int original_size;
    int output_size;
    double compression_ratio;
    outguess_error_t error_code;
    char* error_message;
} outguess_embed_result_t;

typedef struct {
    char* message;
    int message_size;
    bool verified;
    outguess_error_t error_code;
    char* error_message;
} outguess_extract_result_t;

// Core functions
outguess_embed_result_t* outguess_embed_message(
    const char* image_path,
    const char* message,
    const char* output_path,
    const outguess_config_t* config
);

outguess_extract_result_t* outguess_extract_message(
    const char* image_path,
    const outguess_config_t* config
);

bool outguess_has_hidden_data(const char* image_path);

int outguess_get_max_message_size(
    const char* image_path,
    int compression_resistance,
    int quality
);

bool outguess_test_compression_resistance(
    const char* image_path,
    int compression_quality,
    const char* password
);

const char* outguess_get_version(void);

// Utility functions
void outguess_free_embed_result(outguess_embed_result_t* result);
void outguess_free_extract_result(outguess_extract_result_t* result);
const char* outguess_error_string(outguess_error_t error);

// Advanced functions
typedef struct {
    int dct_coefficients_used;
    int total_dct_coefficients;
    double capacity_utilization;
    int redundancy_level;
} outguess_stats_t;

outguess_stats_t* outguess_get_embedding_stats(const char* image_path);
void outguess_free_stats(outguess_stats_t* stats);

// Callback for progress reporting
typedef void (*outguess_progress_callback_t)(int progress, void* user_data);

outguess_embed_result_t* outguess_embed_message_with_progress(
    const char* image_path,
    const char* message,
    const char* output_path,
    const outguess_config_t* config,
    outguess_progress_callback_t callback,
    void* user_data
);

#ifdef __cplusplus
}
#endif

#endif // OUTGUESS_H