require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))
folly_compiler_flags = '-DFOLLY_NO_CONFIG -DFOLLY_MOBILE=1 -DFOLLY_USE_LIBCPP=1 -Wno-comma -Wno-shorten-64-to-32'

Pod::Spec.new do |s|
  s.name         = "react-native-outguess"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  s.platforms    = { :ios => "11.0" }
  s.source       = { :git => "https://github.com/yourusername/react-native-outguess.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,m,mm,swift}", "cpp/**/*.{h,hpp,cpp,c,cc}"
  s.requires_arc = true

  # Dependencies
  s.dependency "React-Core"

  # Use install_modules_dependencies helper to install the dependencies if React Native version >=0.71.0
  if respond_to?(:install_modules_dependencies, true)
    install_modules_dependencies(s)
  else
    s.dependency "React-cxxreact"
    s.dependency "React-callinvoker"
    s.dependency "ReactCommon/turbomodule/core"
  end

  # C++ compilation settings
  s.pod_target_xcconfig = {
    "CLANG_CXX_LANGUAGE_STANDARD" => "c++17",
    "HEADER_SEARCH_PATHS" => "\"$(PODS_ROOT)/boost\" \"$(PODS_ROOT)/RCT-Folly\" \"$(PODS_ROOT)/DoubleConversion\"",
    "OTHER_CPLUSPLUSFLAGS" => folly_compiler_flags
  }

  s.compiler_flags = folly_compiler_flags + ' -DRCT_NEW_ARCH_ENABLED=1'
  s.user_target_xcconfig = { "HEADER_SEARCH_PATHS" => "\"$(PODS_ROOT)/boost\"" }

  # Enable modular headers
  s.module_map = "ios/react-native-outguess.modulemap"
end