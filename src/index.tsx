import { NativeModules } from 'react-native';

export enum PickerType {
  Photo = 0, // 只显示图片
  Video = 1, // 只显示视频
  PhotoAndVideo = 2, // 图片和视频一起显示
}

export interface ExportFile {
  path: string;
  uri: string;
  fileName: string;
  width: number;
  height: number;
  size: number;
  duration: number;
  mime: string;
  data: string;
  coverUri: string;
  coverPath: string;
  coverMime: string;
  coverFileName: string;
  coverSize: number;
  isVideo: boolean;
}

export interface PhotoPickerOptions {
  // picker类型
  type?: PickerType;

  // 最大选择数
  maxNum?: number;

  // 视频最大选择数
  videoMaxNum?: number;

  // 是否打开相机功能
  openCamera?: boolean;

  // 是否开启查看GIF图片功能
  lookGifPhoto?: boolean;

  // 图片和视频是否能够同时选择
  selectTogether?: boolean;

  // 能选择的最大文件大小 为0不过滤 默认50M
  maxFileSize?: number;

  /**
   * 视频能选择的最大秒数  -  默认 60s
   * 当视频超过能选的最大时长，如果视频可以编辑那么在列表选择的时候会自动跳转视频裁剪界面
   */
  videoMaximumSelectDuration?: number;

  // 视频能选择的最小秒数  -  默认 0秒 - 不限制
  videoMinimumSelectDuration?: number;

  // 相机视频录制最大秒数  -  默认60s
  videoMaximumDuration?: number;

  // 相机视频录制最小秒数  -  默认3s
  videoMinimumDuration?: number;

  // 是否可以编辑照片
  photoCanEdit?: boolean;

  // 是否为单选模式 默认 NO  HXPhotoView 不支持
  singleSelected?: boolean;

  // 单选模式下选择图片时是否直接跳转到编辑界面
  singleJumpEdit?: boolean;

  /**
   * 是否压缩图片
   */
  isCompress?: boolean;

  /**
   * 小于多少kb的图片不用压缩
   */
  minimumCompressSize?: number;

  /**
   * 压缩质量
   */
  compressQuality?: number;

  // 裁剪
  // 是否圆形裁剪框
  isRoundCliping?: boolean;

  // 裁剪框宽度比
  cropWidthRatio?: number;
  // 裁剪框高度比
  cropHeightRatio?: number;

  // 是否可以拖拽裁剪框，自定义裁剪比例
  customCropRatio?: boolean;

  // 图片是否包含base64编码信息
  includeBase64?: boolean;

  /**
   * 仅支持 ANDROID
   */
  // 视频录制质量 0 = 极低 1 = 高清
  videoQuality?: number;
  // 可显示的视频类型
  mimeTypeConditions?: (
    | 'image/png'
    | 'image/jpeg'
    | 'image/jpg'
    | 'image/bmp'
    | 'image/gif'
    | 'image/webp'
  )[];
  /***
   * 仅支持IOS
   */
  // 是否开启查看LivePhoto功能
  lookLivePhoto?: boolean;

  /**
   *  删除临时的照片/视频 -
   * 注:相机拍摄的照片并没有保存到系统相册 或 是本地图片
   * 如果当这样的照片都没有被选中时会清空这些照片 有一张选中了就不会删..
   */
  deleteTemporaryPhoto?: boolean;

  // 拍摄的 照片/视频 是否保存到系统相册  默认NO
  saveSystemAlbum?: boolean;

  // 是否可以编辑视频
  videoCanEdit?: boolean;

  isCover?: boolean;
}

const { PhotoPickerModule } = NativeModules;

const PhotoPciker = {
  openPicker: (options: PhotoPickerOptions): Promise<ExportFile[]> => {
    const defaultOptions: PhotoPickerOptions = {
      type: 0,
      maxNum: 9,
      videoMaxNum: 1,
      openCamera: true,
      lookGifPhoto: true,
      lookLivePhoto: true,
      selectTogether: false,
      maxFileSize: 1024 * 1024 * 50,
      videoMaximumSelectDuration: 60,
      videoMinimumSelectDuration: 0,
      videoMaximumDuration: 60,
      videoMinimumDuration: 3,
      deleteTemporaryPhoto: true,
      saveSystemAlbum: false,

      isCompress: true,
      minimumCompressSize: 200,
      compressQuality: 30,

      videoQuality: 0,
      videoCanEdit: false,
      photoCanEdit: false,
      singleSelected: false,
      singleJumpEdit: false,

      isRoundCliping: false,

      cropWidthRatio: 0,
      cropHeightRatio: 0,

      customCropRatio: false,

      includeBase64: false,

      mimeTypeConditions: [],
      isCover: true,
    };

    return PhotoPickerModule.openPicker({ ...defaultOptions, ...options });
  },
  clean: () => PhotoPickerModule.clean(),
};

export default PhotoPciker;
