module.exports = {
  dependencies: {
    'react-native-outguess': {
      platforms: {
        android: {
          sourceDir: '../android',
          packageImportPath: 'import com.outguess.OutguessModule;',
        },
        ios: {
          podspecPath: '../react-native-outguess.podspec',
        },
      },
    },
  },
};