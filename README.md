# react-native-photo-picker

react native 照片选择库
android使用 PictureSelector(#https://github.com/LuckSiege/PictureSelector)
ios使用 HXPhotoPicker(https://github.com/SilenceLove/HXPhotoPicker)

## Installation

```sh
npm install react-native-photo-picker
```

## Usage

```js
import PhotoPicker from "react-native-photo-picker";

// ...
const result = await PhotoPicker.openPicker({ maxNum: 3 });
```

## Android

### 权限
AndroidManifest.xml添加
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

### 混淆
proguard-rules.pro添加
```xml
#PictureSelector 2.0
-keep class com.luck.picture.lib.** { *; }
#Ucrop
-dontwarn com.yalantis.ucrop**
-keep class com.yalantis.ucrop** { *; }
-keep interface com.yalantis.ucrop** { *; }
```

## IOS

### 安装
```sh
pod install
```

### 权限
info.plist添加
```
	<key>NSCameraUsageDescription</key>
	<string>是否允许此App使用你的相机进行拍照？</string>
	<key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
	<string>是否允许此App访问您的地址位置，以获取数据</string>
	<key>NSLocationAlwaysUsageDescription</key>
	<string>是否允许此App访问您的地址位置，以获取数据</string>
	<key>NSLocationWhenInUseUsageDescription</key>
	<string>是否允许此App访问您的地址位置，以获取数据</string>
	<key>NSMicrophoneUsageDescription</key>
	<string>是否允许此App使用你的麦克风进行录像？</string>
	<key>NSPhotoLibraryUsageDescription</key>
	<string>请允许访问相册以选取照片</string>
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT
