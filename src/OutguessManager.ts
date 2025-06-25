import { NativeModules, Platform } from 'react-native';
import type {
  OutguessOptions,
  EmbedResult,
  ExtractResult,
  OutguessError,
  OutguessEvent,
  OutguessEventType,
} from './types';

const LINKING_ERROR =
  `The package 'react-native-outguess' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'cd ios && pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const OutguessModule = NativeModules.OutguessModule
  ? NativeModules.OutguessModule
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export class OutguessManager {
  private static eventListeners: Map<string, (event: OutguessEvent) => void> = new Map();

  /**
   * Embed a message into a JPEG image using Outguess algorithm
   * @param imagePath Path to the input JPEG image
   * @param message Message to embed
   * @param outputPath Path for the output image
   * @param options Embedding options
   * @returns Promise<EmbedResult>
   */
  static async embedMessage(
    imagePath: string,
    message: string,
    outputPath: string,
    options: OutguessOptions = {}
  ): Promise<EmbedResult> {
    try {
      const startTime = Date.now();
      
      // Validate inputs
      this.validateInputs(imagePath, message, options);
      
      const result = await OutguessModule.embedMessage(
        imagePath,
        message,
        outputPath,
        {
          password: options.password || null,
          compressionResistance: options.compressionResistance || 5,
          quality: options.quality || 85,
          verbose: options.verbose || false,
          maxMessageSize: options.maxMessageSize || 65536,
        }
      );

      const processingTime = Date.now() - startTime;
      
      return {
        ...result,
        processingTime,
        success: true,
      };
    } catch (error) {
      throw this.handleError(error);
    }
  }

  /**
   * Extract a message from a JPEG image using Outguess algorithm
   * @param imagePath Path to the JPEG image with embedded message
   * @param options Extraction options
   * @returns Promise<ExtractResult>
   */
  static async extractMessage(
    imagePath: string,
    options: OutguessOptions = {}
  ): Promise<ExtractResult> {
    try {
      const startTime = Date.now();
      
      // Validate inputs
      this.validateImagePath(imagePath);
      
      const result = await OutguessModule.extractMessage(imagePath, {
        password: options.password || null,
        verbose: options.verbose || false,
      });

      const processingTime = Date.now() - startTime;
      
      return {
        ...result,
        processingTime,
        success: true,
        verified: result.verified || false,
      };
    } catch (error) {
      throw this.handleError(error);
    }
  }

  /**
   * Check if an image contains hidden data
   * @param imagePath Path to the JPEG image
   * @returns Promise<boolean>
   */
  static async hasHiddenData(imagePath: string): Promise<boolean> {
    try {
      this.validateImagePath(imagePath);
      return await OutguessModule.hasHiddenData(imagePath);
    } catch (error) {
      throw this.handleError(error);
    }
  }

  /**
   * Get the maximum message size that can be embedded in an image
   * @param imagePath Path to the JPEG image
   * @param options Options for calculation
   * @returns Promise<number> Maximum message size in bytes
   */
  static async getMaxMessageSize(
    imagePath: string,
    options: OutguessOptions = {}
  ): Promise<number> {
    try {
      this.validateImagePath(imagePath);
      return await OutguessModule.getMaxMessageSize(imagePath, {
        compressionResistance: options.compressionResistance || 5,
        quality: options.quality || 85,
      });
    } catch (error) {
      throw this.handleError(error);
    }
  }

  /**
   * Test compression resistance of an embedded message
   * @param imagePath Path to the image with embedded message
   * @param compressionQuality JPEG quality for compression test (1-100)
   * @param password Password for extraction (optional)
   * @returns Promise<boolean> True if message survives compression
   */
  static async testCompressionResistance(
    imagePath: string,
    compressionQuality: number,
    password?: string
  ): Promise<boolean> {
    try {
      this.validateImagePath(imagePath);
      if (compressionQuality < 1 || compressionQuality > 100) {
        throw new Error('Compression quality must be between 1 and 100');
      }
      
      return await OutguessModule.testCompressionResistance(
        imagePath,
        compressionQuality,
        password || null
      );
    } catch (error) {
      throw this.handleError(error);
    }
  }

  /**
   * Add event listener for processing events
   * @param eventType Type of event to listen for
   * @param listener Event listener function
   * @returns Cleanup function
   */
  static addEventListener(
    eventType: OutguessEventType,
    listener: (event: OutguessEvent) => void
  ): () => void {
    const key = `${eventType}_${Date.now()}_${Math.random()}`;
    this.eventListeners.set(key, listener);
    
    return () => {
      this.eventListeners.delete(key);
    };
  }

  /**
   * Remove all event listeners
   */
  static removeAllListeners(): void {
    this.eventListeners.clear();
  }

  /**
   * Get library version
   * @returns Promise<string>
   */
  static async getVersion(): Promise<string> {
    try {
      return await OutguessModule.getVersion();
    } catch (error) {
      return '1.0.0'; // Fallback version
    }
  }

  // Private helper methods
  private static validateInputs(imagePath: string, message: string, options: OutguessOptions): void {
    this.validateImagePath(imagePath);
    
    if (!message || typeof message !== 'string') {
      throw new Error('Message must be a non-empty string');
    }
    
    if (options.maxMessageSize && message.length > options.maxMessageSize) {
      throw new Error(`Message size exceeds maximum allowed size of ${options.maxMessageSize} bytes`);
    }
    
    if (options.compressionResistance && (options.compressionResistance < 1 || options.compressionResistance > 10)) {
      throw new Error('Compression resistance must be between 1 and 10');
    }
    
    if (options.quality && (options.quality < 1 || options.quality > 100)) {
      throw new Error('Quality must be between 1 and 100');
    }
  }

  private static validateImagePath(imagePath: string): void {
    if (!imagePath || typeof imagePath !== 'string') {
      throw new Error('Image path must be a non-empty string');
    }
    
    if (!imagePath.toLowerCase().endsWith('.jpg') && !imagePath.toLowerCase().endsWith('.jpeg')) {
      throw new Error('Only JPEG images are supported');
    }
  }

  private static handleError(error: any): OutguessError {
    if (error.code && error.message) {
      return error as OutguessError;
    }
    
    return {
      code: 'UNKNOWN_ERROR',
      message: error.message || 'An unknown error occurred',
      details: error,
    };
  }

  private static notifyListeners(event: OutguessEvent): void {
    this.eventListeners.forEach(listener => {
      try {
        listener(event);
      } catch (error) {
        console.warn('Error in OutguessManager event listener:', error);
      }
    });
  }
}