import { NativeModules } from 'react-native';

type PhotoPickerType = {
  multiply(a: number, b: number): Promise<number>;
};

const { PhotoPicker } = NativeModules;

export default PhotoPicker as PhotoPickerType;
