import React, { useState, useEffect } from 'react';
import {
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  View,
  TouchableOpacity,
  TextInput,
  Alert,
  Image,
  ActivityIndicator,
  Platform,
} from 'react-native';
import { OutguessManager } from 'react-native-outguess';
import DocumentPicker from 'react-native-document-picker';
import RNFS from 'react-native-fs';
import Share from 'react-native-share';

const App = () => {
  const [selectedImage, setSelectedImage] = useState<string | null>(null);
  const [message, setMessage] = useState('');
  const [password, setPassword] = useState('');
  const [extractedMessage, setExtractedMessage] = useState('');
  const [isProcessing, setIsProcessing] = useState(false);
  const [maxMessageSize, setMaxMessageSize] = useState(0);
  const [libraryVersion, setLibraryVersion] = useState('');

  useEffect(() => {
    // Get library version on startup
    OutguessManager.getVersion().then(setLibraryVersion);
  }, []);

  const selectImage = async () => {
    try {
      const result = await DocumentPicker.pickSingle({
        type: [DocumentPicker.types.images],
      });
      
      if (result.uri.toLowerCase().includes('.jpg') || result.uri.toLowerCase().includes('.jpeg')) {
        setSelectedImage(result.uri);
        
        // Get max message size for this image
        const maxSize = await OutguessManager.getMaxMessageSize(result.uri);
        setMaxMessageSize(maxSize);
      } else {
        Alert.alert('Error', 'Please select a JPEG image file');
      }
    } catch (err) {
      if (!DocumentPicker.isCancel(err)) {
        Alert.alert('Error', 'Failed to select image');
      }
    }
  };

  const embedMessage = async () => {
    if (!selectedImage || !message) {
      Alert.alert('Error', 'Please select an image and enter a message');
      return;
    }

    if (message.length > maxMessageSize) {
      Alert.alert('Error', `Message too long. Maximum size: ${maxMessageSize} characters`);
      return;
    }

    setIsProcessing(true);
    
    try {
      const outputPath = `${RNFS.DocumentDirectoryPath}/outguess_output_${Date.now()}.jpg`;
      
      const result = await OutguessManager.embedMessage(
        selectedImage,
        message,
        outputPath,
        {
          password: password || undefined,
          compressionResistance: 7,
          quality: 90,
          verbose: true,
        }
      );

      Alert.alert(
        'Success',
        `Message embedded successfully!\nProcessing time: ${result.processingTime}ms\nOutput size: ${result.metadata?.outputSize} bytes`,
        [
          {
            text: 'Share Image',
            onPress: () => shareImage(outputPath),
          },
          { text: 'OK' },
        ]
      );
      
      // Update selected image to the output
      setSelectedImage(outputPath);
      
    } catch (error: any) {
      Alert.alert('Error', `Failed to embed message: ${error.message}`);
    } finally {
      setIsProcessing(false);
    }
  };

  const extractMessage = async () => {
    if (!selectedImage) {
      Alert.alert('Error', 'Please select an image');
      return;
    }

    setIsProcessing(true);
    
    try {
      const result = await OutguessManager.extractMessage(selectedImage, {
        password: password || undefined,
        verbose: true,
      });

      setExtractedMessage(result.message);
      
      Alert.alert(
        'Message Extracted',
        `Processing time: ${result.processingTime}ms\nMessage size: ${result.messageSize} bytes\nVerified: ${result.verified ? 'Yes' : 'No'}`
      );
      
    } catch (error: any) {
      Alert.alert('Error', `Failed to extract message: ${error.message}`);
      setExtractedMessage('');
    } finally {
      setIsProcessing(false);
    }
  };

  const checkHiddenData = async () => {
    if (!selectedImage) {
      Alert.alert('Error', 'Please select an image');
      return;
    }

    try {
      const hasData = await OutguessManager.hasHiddenData(selectedImage);
      Alert.alert(
        'Hidden Data Check',
        hasData ? 'This image appears to contain hidden data' : 'No hidden data detected'
      );
    } catch (error: any) {
      Alert.alert('Error', `Failed to check for hidden data: ${error.message}`);
    }
  };

  const testCompression = async () => {
    if (!selectedImage) {
      Alert.alert('Error', 'Please select an image with embedded data');
      return;
    }

    try {
      const survives70 = await OutguessManager.testCompressionResistance(selectedImage, 70, password || undefined);
      const survives50 = await OutguessManager.testCompressionResistance(selectedImage, 50, password || undefined);
      const survives30 = await OutguessManager.testCompressionResistance(selectedImage, 30, password || undefined);
      
      Alert.alert(
        'Compression Resistance Test',
        `Quality 70%: ${survives70 ? 'PASS' : 'FAIL'}\n` +
        `Quality 50%: ${survives50 ? 'PASS' : 'FAIL'}\n` +
        `Quality 30%: ${survives30 ? 'PASS' : 'FAIL'}`
      );
    } catch (error: any) {
      Alert.alert('Error', `Failed to test compression resistance: ${error.message}`);
    }
  };

  const shareImage = async (imagePath: string) => {
    try {
      await Share.open({
        url: `file://${imagePath}`,
        type: 'image/jpeg',
      });
    } catch (error) {
      console.log('Share cancelled');
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" backgroundColor="#f8f9fa" />
      <ScrollView contentInsetAdjustmentBehavior="automatic" style={styles.scrollView}>
        
        {/* Header */}
        <View style={styles.header}>
          <Text style={styles.title}>React Native Outguess</Text>
          <Text style={styles.subtitle}>JPEG Steganography Demo</Text>
          <Text style={styles.version}>Library Version: {libraryVersion}</Text>
        </View>

        {/* Image Selection */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>1. Select JPEG Image</Text>
          <TouchableOpacity style={styles.button} onPress={selectImage}>
            <Text style={styles.buttonText}>Choose Image</Text>
          </TouchableOpacity>
          
          {selectedImage && (
            <View style={styles.imageContainer}>
              <Image source={{ uri: selectedImage }} style={styles.previewImage} />
              <Text style={styles.imageInfo}>
                Max message size: {maxMessageSize} characters
              </Text>
            </View>
          )}
        </View>

        {/* Message Input */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>2. Message to Embed</Text>
          <TextInput
            style={styles.textInput}
            placeholder="Enter your secret message..."
            value={message}
            onChangeText={setMessage}
            multiline
            numberOfLines={4}
            maxLength={maxMessageSize}
          />
          <Text style={styles.characterCount}>
            {message.length} / {maxMessageSize} characters
          </Text>
        </View>

        {/* Password Input */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>3. Password (Optional)</Text>
          <TextInput
            style={styles.textInput}
            placeholder="Enter password for encryption..."
            value={password}
            onChangeText={setPassword}
            secureTextEntry
          />
        </View>

        {/* Action Buttons */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>4. Actions</Text>
          
          <TouchableOpacity 
            style={[styles.button, styles.primaryButton]} 
            onPress={embedMessage}
            disabled={isProcessing || !selectedImage || !message}
          >
            {isProcessing ? (
              <ActivityIndicator color="#fff" />
            ) : (
              <Text style={styles.buttonText}>Embed Message</Text>
            )}
          </TouchableOpacity>

          <TouchableOpacity 
            style={[styles.button, styles.secondaryButton]} 
            onPress={extractMessage}
            disabled={isProcessing || !selectedImage}
          >
            <Text style={[styles.buttonText, styles.secondaryButtonText]}>Extract Message</Text>
          </TouchableOpacity>

          <View style={styles.buttonRow}>
            <TouchableOpacity 
              style={[styles.button, styles.smallButton]} 
              onPress={checkHiddenData}
              disabled={isProcessing || !selectedImage}
            >
              <Text style={[styles.buttonText, styles.smallButtonText]}>Check Hidden Data</Text>
            </TouchableOpacity>

            <TouchableOpacity 
              style={[styles.button, styles.smallButton]} 
              onPress={testCompression}
              disabled={isProcessing || !selectedImage}
            >
              <Text style={[styles.buttonText, styles.smallButtonText]}>Test Compression</Text>
            </TouchableOpacity>
          </View>
        </View>

        {/* Extracted Message Display */}
        {extractedMessage ? (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Extracted Message</Text>
            <View style={styles.extractedMessageContainer}>
              <Text style={styles.extractedMessage}>{extractedMessage}</Text>
            </View>
          </View>
        ) : null}

        {/* Usage Instructions */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>How to Use</Text>
          <Text style={styles.instructions}>
            1. Select a JPEG image from your device{'\n'}
            2. Enter the message you want to hide{'\n'}
            3. Optionally set a password for encryption{'\n'}
            4. Tap "Embed Message" to hide the message{'\n'}
            5. Use "Extract Message" to retrieve hidden messages{'\n'}
            6. Test compression resistance to verify robustness
          </Text>
        </View>

        {/* Features */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Features</Text>
          <Text style={styles.features}>
            ✓ High compression resistance{'\n'}
            ✓ Password-based encryption{'\n'}
            ✓ Optimized for speed{'\n'}
            ✓ Cross-platform (iOS & Android){'\n'}
            ✓ Large message capacity{'\n'}
            ✓ Robust against JPEG recompression
          </Text>
        </View>

      </ScrollView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8f9fa',
  },
  scrollView: {
    flex: 1,
  },
  header: {
    padding: 20,
    alignItems: 'center',
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e9ecef',
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#212529',
    marginBottom: 4,
  },
  subtitle: {
    fontSize: 16,
    color: '#6c757d',
    marginBottom: 8,
  },
  version: {
    fontSize: 12,
    color: '#adb5bd',
  },
  section: {
    margin: 16,
    padding: 16,
    backgroundColor: '#fff',
    borderRadius: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#212529',
    marginBottom: 12,
  },
  button: {
    backgroundColor: '#007bff',
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 8,
    alignItems: 'center',
    marginBottom: 8,
  },
  primaryButton: {
    backgroundColor: '#28a745',
  },
  secondaryButton: {
    backgroundColor: '#fff',
    borderWidth: 2,
    borderColor: '#007bff',
  },
  smallButton: {
    flex: 1,
    marginHorizontal: 4,
    backgroundColor: '#6c757d',
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  secondaryButtonText: {
    color: '#007bff',
  },
  smallButtonText: {
    fontSize: 14,
  },
  buttonRow: {
    flexDirection: 'row',
    marginTop: 8,
  },
  textInput: {
    borderWidth: 1,
    borderColor: '#ced4da',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    backgroundColor: '#fff',
    textAlignVertical: 'top',
  },
  characterCount: {
    textAlign: 'right',
    color: '#6c757d',
    fontSize: 12,
    marginTop: 4,
  },
  imageContainer: {
    marginTop: 12,
    alignItems: 'center',
  },
  previewImage: {
    width: 200,
    height: 150,
    borderRadius: 8,
    marginBottom: 8,
  },
  imageInfo: {
    fontSize: 14,
    color: '#6c757d',
  },
  extractedMessageContainer: {
    backgroundColor: '#f8f9fa',
    padding: 12,
    borderRadius: 8,
    borderLeftWidth: 4,
    borderLeftColor: '#28a745',
  },
  extractedMessage: {
    fontSize: 16,
    color: '#212529',
    lineHeight: 24,
  },
  instructions: {
    fontSize: 14,
    color: '#6c757d',
    lineHeight: 20,
  },
  features: {
    fontSize: 14,
    color: '#28a745',
    lineHeight: 20,
  },
});

export default App;