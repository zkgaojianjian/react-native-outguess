import Foundation
import React

@objc(OutguessModule)
class OutguessModule: NSObject {
    
    private let processingQueue = DispatchQueue(label: "com.outguess.processing", qos: .userInitiated)
    
    @objc
    func embedMessage(
        _ imagePath: String,
        message: String,
        outputPath: String,
        options: [String: Any],
        resolver resolve: @escaping RCTPromiseResolveBlock,
        rejecter reject: @escaping RCTPromiseRejectBlock
    ) {
        processingQueue.async {
            do {
                // Validate inputs
                guard self.validateImagePath(imagePath) else {
                    reject("INVALID_IMAGE_PATH", "Invalid image path: \(imagePath)", nil)
                    return
                }
                
                guard !message.isEmpty else {
                    reject("INVALID_MESSAGE", "Message cannot be empty", nil)
                    return
                }
                
                // Extract options
                let password = options["password"] as? String
                let compressionResistance = options["compressionResistance"] as? Int ?? 5
                let quality = options["quality"] as? Int ?? 85
                let verbose = options["verbose"] as? Bool ?? false
                let maxMessageSize = options["maxMessageSize"] as? Int ?? 65536
                
                // Check message size
                guard message.count <= maxMessageSize else {
                    reject("MESSAGE_TOO_LARGE", 
                           "Message size exceeds maximum allowed size of \(maxMessageSize) bytes", nil)
                    return
                }
                
                // Call native implementation
                let startTime = CFAbsoluteTimeGetCurrent()
                let result = try self.nativeEmbedMessage(
                    imagePath: imagePath,
                    message: message,
                    outputPath: outputPath,
                    password: password,
                    compressionResistance: compressionResistance,
                    quality: quality,
                    verbose: verbose
                )
                let processingTime = (CFAbsoluteTimeGetCurrent() - startTime) * 1000
                
                var response: [String: Any] = [
                    "outputPath": result.outputPath,
                    "messageSize": result.messageSize,
                    "processingTime": processingTime,
                    "success": true
                ]
                
                // Add metadata if available
                if result.originalSize > 0 {
                    response["metadata"] = [
                        "originalSize": result.originalSize,
                        "outputSize": result.outputSize,
                        "compressionRatio": result.compressionRatio
                    ]
                }
                
                resolve(response)
                
            } catch let error as OutguessError {
                reject(error.code, error.message, nil)
            } catch {
                reject("EMBED_ERROR", error.localizedDescription, nil)
            }
        }
    }
    
    @objc
    func extractMessage(
        _ imagePath: String,
        options: [String: Any],
        resolver resolve: @escaping RCTPromiseResolveBlock,
        rejecter reject: @escaping RCTPromiseRejectBlock
    ) {
        processingQueue.async {
            do {
                guard self.validateImagePath(imagePath) else {
                    reject("INVALID_IMAGE_PATH", "Invalid image path: \(imagePath)", nil)
                    return
                }
                
                let password = options["password"] as? String
                let verbose = options["verbose"] as? Bool ?? false
                
                let startTime = CFAbsoluteTimeGetCurrent()
                let result = try self.nativeExtractMessage(
                    imagePath: imagePath,
                    password: password,
                    verbose: verbose
                )
                let processingTime = (CFAbsoluteTimeGetCurrent() - startTime) * 1000
                
                let response: [String: Any] = [
                    "message": result.message,
                    "messageSize": result.messageSize,
                    "processingTime": processingTime,
                    "success": true,
                    "verified": result.verified
                ]
                
                resolve(response)
                
            } catch let error as OutguessError {
                reject(error.code, error.message, nil)
            } catch {
                reject("EXTRACT_ERROR", error.localizedDescription, nil)
            }
        }
    }
    
    @objc
    func hasHiddenData(
        _ imagePath: String,
        resolver resolve: @escaping RCTPromiseResolveBlock,
        rejecter reject: @escaping RCTPromiseRejectBlock
    ) {
        processingQueue.async {
            do {
                guard self.validateImagePath(imagePath) else {
                    reject("INVALID_IMAGE_PATH", "Invalid image path: \(imagePath)", nil)
                    return
                }
                
                let hasData = try self.nativeHasHiddenData(imagePath: imagePath)
                resolve(hasData)
                
            } catch {
                reject("CHECK_ERROR", error.localizedDescription, nil)
            }
        }
    }
    
    @objc
    func getMaxMessageSize(
        _ imagePath: String,
        options: [String: Any],
        resolver resolve: @escaping RCTPromiseResolveBlock,
        rejecter reject: @escaping RCTPromiseRejectBlock
    ) {
        processingQueue.async {
            do {
                guard self.validateImagePath(imagePath) else {
                    reject("INVALID_IMAGE_PATH", "Invalid image path: \(imagePath)", nil)
                    return
                }
                
                let compressionResistance = options["compressionResistance"] as? Int ?? 5
                let quality = options["quality"] as? Int ?? 85
                
                let maxSize = try self.nativeGetMaxMessageSize(
                    imagePath: imagePath,
                    compressionResistance: compressionResistance,
                    quality: quality
                )
                
                resolve(maxSize)
                
            } catch {
                reject("MAX_SIZE_ERROR", error.localizedDescription, nil)
            }
        }
    }
    
    @objc
    func testCompressionResistance(
        _ imagePath: String,
        compressionQuality: Int,
        password: String?,
        resolver resolve: @escaping RCTPromiseResolveBlock,
        rejecter reject: @escaping RCTPromiseRejectBlock
    ) {
        processingQueue.async {
            do {
                guard self.validateImagePath(imagePath) else {
                    reject("INVALID_IMAGE_PATH", "Invalid image path: \(imagePath)", nil)
                    return
                }
                
                guard compressionQuality >= 1 && compressionQuality <= 100 else {
                    reject("INVALID_QUALITY", "Compression quality must be between 1 and 100", nil)
                    return
                }
                
                let survives = try self.nativeTestCompressionResistance(
                    imagePath: imagePath,
                    compressionQuality: compressionQuality,
                    password: password
                )
                
                resolve(survives)
                
            } catch {
                reject("TEST_ERROR", error.localizedDescription, nil)
            }
        }
    }
    
    @objc
    func getVersion(
        _ resolve: @escaping RCTPromiseResolveBlock,
        rejecter reject: @escaping RCTPromiseRejectBlock
    ) {
        do {
            let version = try nativeGetVersion()
            resolve(version)
        } catch {
            resolve("1.0.0") // Fallback version
        }
    }
    
    // MARK: - Private Methods
    
    private func validateImagePath(_ imagePath: String) -> Bool {
        guard !imagePath.isEmpty else { return false }
        
        let fileManager = FileManager.default
        guard fileManager.fileExists(atPath: imagePath) else { return false }
        
        let lowercasePath = imagePath.lowercased()
        return lowercasePath.hasSuffix(".jpg") || lowercasePath.hasSuffix(".jpeg")
    }
    
    // MARK: - Native Implementation Stubs
    // These would be implemented in C++ and linked via bridging
    
    private func nativeEmbedMessage(
        imagePath: String,
        message: String,
        outputPath: String,
        password: String?,
        compressionResistance: Int,
        quality: Int,
        verbose: Bool
    ) throws -> EmbedResult {
        // This would call the actual C++ implementation
        // For now, return a mock result
        return EmbedResult(
            outputPath: outputPath,
            messageSize: message.count,
            originalSize: 1024000,
            outputSize: 1024500,
            compressionRatio: 1.0005
        )
    }
    
    private func nativeExtractMessage(
        imagePath: String,
        password: String?,
        verbose: Bool
    ) throws -> ExtractResult {
        // This would call the actual C++ implementation
        // For now, return a mock result
        return ExtractResult(
            message: "Mock extracted message",
            messageSize: 23,
            verified: true
        )
    }
    
    private func nativeHasHiddenData(imagePath: String) throws -> Bool {
        // This would call the actual C++ implementation
        return false
    }
    
    private func nativeGetMaxMessageSize(
        imagePath: String,
        compressionResistance: Int,
        quality: Int
    ) throws -> Int {
        // This would call the actual C++ implementation
        return 65536
    }
    
    private func nativeTestCompressionResistance(
        imagePath: String,
        compressionQuality: Int,
        password: String?
    ) throws -> Bool {
        // This would call the actual C++ implementation
        return true
    }
    
    private func nativeGetVersion() throws -> String {
        return "1.0.0"
    }
}

// MARK: - Result Types

struct EmbedResult {
    let outputPath: String
    let messageSize: Int
    let originalSize: Int
    let outputSize: Int
    let compressionRatio: Double
}

struct ExtractResult {
    let message: String
    let messageSize: Int
    let verified: Bool
}

// MARK: - Error Types

enum OutguessError: Error {
    case invalidInput(String)
    case processingFailed(String)
    case compressionFailed(String)
    case extractionFailed(String)
    
    var code: String {
        switch self {
        case .invalidInput: return "INVALID_INPUT"
        case .processingFailed: return "PROCESSING_FAILED"
        case .compressionFailed: return "COMPRESSION_FAILED"
        case .extractionFailed: return "EXTRACTION_FAILED"
        }
    }
    
    var message: String {
        switch self {
        case .invalidInput(let msg): return msg
        case .processingFailed(let msg): return msg
        case .compressionFailed(let msg): return msg
        case .extractionFailed(let msg): return msg
        }
    }
}