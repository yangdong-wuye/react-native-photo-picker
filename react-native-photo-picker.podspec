require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-photo-picker"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  s.platforms    = { :ios => "10.0" }
  s.source       = { :git => "https://github.com/yangdong-wuye/react-native-photo-picker.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,m,mm}"

  s.dependency "HXPhotoPicker", "~> 3.2.3"
  s.dependency "HXPhotoPicker/SDWebImage", "~> 3.2.3"
  s.dependency "HXPhotoPicker/YYWebImage", "~> 3.2.3"
   
  s.dependency "React-Core"
end
