export interface OutguessOptions {
  /**
   * Password for encryption/decryption (optional)
   */
  password?: string;
  
  /**
   * Compression resistance level (1-10, higher = more resistant)
   * @default 5
   */
  compressionResistance?: CompressionLevel;
  
  /**
   * Quality level for JPEG processing (1-100)
   * @default 85
   */
  quality?: QualityLevel;
  
  /**
   * Enable verbose logging
   * @default false
   */
  verbose?: boolean;
  
  /**
   * Maximum message size in bytes
   * @default 65536
   */
  maxMessageSize?: number;
}

export type CompressionLevel = 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10;
export type QualityLevel = number; // 1-100

export interface EmbedResult {
  /**
   * Path to the output image with embedded message
   */
  outputPath: string;
  
  /**
   * Size of the embedded message in bytes
   */
  messageSize: number;
  
  /**
   * Processing time in milliseconds
   */
  processingTime: number;
  
  /**
   * Success status
   */
  success: boolean;
  
  /**
   * Additional metadata
   */
  metadata?: {
    originalSize: number;
    outputSize: number;
    compressionRatio: number;
  };
}

export interface ExtractResult {
  /**
   * Extracted message content
   */
  message: string;
  
  /**
   * Size of the extracted message in bytes
   */
  messageSize: number;
  
  /**
   * Processing time in milliseconds
   */
  processingTime: number;
  
  /**
   * Success status
   */
  success: boolean;
  
  /**
   * Verification status
   */
  verified: boolean;
}

export interface OutguessError {
  code: string;
  message: string;
  details?: any;
}

export type OutguessEventType = 'progress' | 'error' | 'complete';

export interface OutguessEvent {
  type: OutguessEventType;
  progress?: number; // 0-100
  error?: OutguessError;
  result?: EmbedResult | ExtractResult;
}