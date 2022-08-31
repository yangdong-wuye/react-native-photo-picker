package co.yangdong;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.config.PictureSelectionConfig;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.listener.OnResultCallbackListener;
import com.luck.picture.lib.manager.PictureCacheManager;
import com.luck.picture.lib.style.PictureSelectorUIStyle;
import com.luck.picture.lib.style.PictureWindowAnimationStyle;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.view.OverlayView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.List;
import java.util.UUID;

public class PhotoPickerModule extends ReactContextBaseJavaModule {
    public static final String NAME = "PhotoPickerModule";

    private final ReactContext reactContext;

    public PhotoPickerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;

    }

    @Override
    @NonNull
    public String getName() {
        return NAME;
    }


    @SuppressWarnings("unused")
    @ReactMethod
    public void openPicker(ReadableMap options, Promise promise) {
        int type = options.getInt("type");
        int maxNum = options.getInt("maxNum");
        int videoMaxNum = options.getInt("videoMaxNum");
        boolean openCamera = options.getBoolean("openCamera");
        boolean lookGifPhoto = options.getBoolean("lookGifPhoto");
        boolean selectTogether = options.getBoolean("selectTogether");
        int maxFileSize = options.getInt("maxFileSize");
        int videoMaximumDuration = options.getInt("videoMaximumDuration");
        int videoMinimumDuration = options.getInt("videoMinimumDuration");
        int videoMaximumSelectDuration = options.getInt("videoMaximumSelectDuration"); // 视频最大选择时间
        int videoMinimumSelectDuration = options.getInt("videoMinimumSelectDuration"); // 视频最小选择时间
        int videoQuality = options.getInt("videoQuality"); // 视频录制质量
        boolean isCompress = options.getBoolean("isCompress"); // 是否压缩
        int minimumCompressSize = options.getInt("minimumCompressSize"); // 小于多少kb的图片不压缩
        int compressQuality = options.getInt("compressQuality"); // 图片压缩质量
        boolean videoCanEdit = options.getBoolean("videoCanEdit"); // 视频是否可以编辑（TODO）
        boolean photoCanEdit = options.getBoolean("photoCanEdit"); // 照片是否可以编辑
        boolean singleSelected = options.getBoolean("singleSelected");
        boolean singleJumpEdit = options.getBoolean("singleJumpEdit");
        boolean isRoundCliping = options.getBoolean("isRoundCliping");
        int cropWidthRatio = options.getInt("cropWidthRatio");
        int cropHeightRatio = options.getInt("cropHeightRatio");
        boolean customCropRatio = options.getBoolean("customCropRatio");
        boolean includeBase64 = options.getBoolean("includeBase64");
        boolean isCover = options.getBoolean("isCover");
        ReadableArray mimeTypeConditions = options.getArray("mimeTypeConditions");

        UCrop.Options cropOptions = new UCrop.Options();
        if (cropWidthRatio <= 0 || cropHeightRatio <= 0) {
            cropOptions.useSourceImageAspectRatio();
        } else {
            cropOptions.withAspectRatio(cropWidthRatio, cropHeightRatio);
        }
        cropOptions.setCircleDimmedLayer(isRoundCliping); // 是否开启圆形裁剪
        cropOptions.isMultipleSkipCrop(true); // 多图裁剪时是否支持跳过，默认支持
        cropOptions.setHideBottomControls(false); // 是否显示底部工具栏
        cropOptions.setShowCropFrame(true); // 是否显示裁剪框
        cropOptions.setShowCropGrid(true); // 是否显示裁剪框网格
        cropOptions.setFreeStyleCropEnabled(customCropRatio);
        cropOptions.setFreestyleCropMode(customCropRatio ? OverlayView.FREESTYLE_CROP_MODE_ENABLE : OverlayView.FREESTYLE_CROP_MODE_DISABLE);// 裁剪框拖动模式
        cropOptions.setCropDragSmoothToCenter(true); // 裁剪框拖动时图片自动跟随居中

        int[] pictureMimeType = {PictureMimeType.ofImage(), PictureMimeType.ofVideo(), PictureMimeType.ofAll()};
        PictureSelector.create(this.getCurrentActivity())
                .openGallery(pictureMimeType[type])
                .queryMimeTypeConditions(mimeTypeConditions.toArrayList().toArray(new String[]{}))
                .setPictureUIStyle(PictureSelectorUIStyle.ofNewStyle())
                .maxSelectNum(maxNum) // 最大图片选择数量
                .minSelectNum(0) // 最小选择数量
                .maxVideoSelectNum(videoMaxNum) // 视频最大选择数量，如果没有单独设置的需求则可以不设置，同用maxSelectNum字段
                .minVideoSelectNum(0) // 视频最小选择数量，如果没有单独设置的需求则可以不设置，同用minSelectNum字段
                .isCamera(openCamera) // 是否显示拍照按钮
                .isGif(lookGifPhoto) // 是否显示gif
                .isWithVideoImage(selectTogether) // 图片和视频是否可以同选,只在ofAll模式下有效
                .filterMinFileSize(0) // 过滤最小的文件
                .filterMaxFileSize(maxFileSize) // 过滤最大的文件
                .recordVideoSecond(videoMaximumDuration) // 录制视频秒数 默认60s
                .recordVideoMinSecond(videoMinimumDuration) // 最低录制秒数
                .videoMaxSecond(videoMaximumSelectDuration) // 查询多少秒以内的视频
                .videoMinSecond(videoMinimumSelectDuration) // 查询多少秒以外的视频
                .videoQuality(videoQuality)
                .isCompress(isCompress) // 是否压缩
                .compressQuality(compressQuality) // 图片压缩质量
                .minimumCompressSize(minimumCompressSize) // 小于多少kb的图片不压缩
                .selectionMode(singleSelected ? PictureConfig.SINGLE : PictureConfig.MULTIPLE) // 多选 or 单选
                .isSingleDirectReturn(singleJumpEdit) // 单选模式下是否直接返回，PictureConfig.SINGLE模式下有效
                .isPreviewImage(true) // 是否可预览图片
                .isPreviewVideo(true) // 是否可预览视频
                .imageSpanCount(4) // 每行显示个数
                .isReturnEmpty(false) // 未选择数据时点击按钮是否可以返回
                .isEnablePreviewAudio(false) // 是否可播放音频
                .isOpenClickSound(false) // 是否开启点击声音
                .isMaxSelectEnabledMask(true) //选择条件达到阀时列表是否启用蒙层效果
                .imageEngine(GlideEngine.createGlideEngine()) // 外部传入图片加载引擎，必传项
                .isUseCustomCamera(true)
                .setPictureWindowAnimationStyle(new PictureWindowAnimationStyle()) // 自定义相册启动退出动画
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) // 设置相册Activity方向，不设置默认使用系统
                .isEnableCrop(photoCanEdit) // 是否开启裁剪功能
                .rotateEnabled(false) // 禁止旋转
                .basicUCropConfig(cropOptions) // 配置裁剪
                .forResult(new OnResultCallbackListener<LocalMedia>() {
                    @Override
                    public void onResult(List<LocalMedia> result) {
                        try {
                            WritableArray list = Arguments.createArray();
                            for (LocalMedia media : result) {
                                WritableMap data = Arguments.createMap();
                                String path = media.isCompressed() ? media.getCompressPath() : media.isCut() ? media.getCutPath() : media.getRealPath();
                                boolean isOriginal = !media.isCompressed() && !media.isCut();
                                File file = new File(path);
                                Uri uri = Uri.fromFile(file);

                                String mime = isOriginal ? media.getMimeType() : getMimeType(file);
                                data.putString("path", file.getPath());
                                data.putString("uri", uri.toString());
                                data.putString("fileName", file.getName());
                                data.putDouble("size", file.length());
                                data.putDouble("duration", media.getDuration());
                                data.putString("mime", mime);
                                data.putBoolean("isVideo", PictureMimeType.isHasVideo(media.getMimeType()));

                                if (PictureMimeType.isHasImage(media.getMimeType())) {
                                    Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
                                    int width = bitmap.getWidth();
                                    int height = bitmap.getHeight();
                                    data.putInt("width", width);
                                    data.putInt("height", height);
                                    if (isCover) {
                                        File coverFile = getImageCover(file.getPath(), width, height);
                                        data.putString("coverFileName", coverFile.getName());
                                        data.putString("coverPath", coverFile.getPath());
                                        data.putString("coverUri", Uri.fromFile(coverFile).toString());
                                        data.putString("coverMime", getMimeType(coverFile));
                                        data.putDouble("coverSize", coverFile.length());
                                    }
                                }

                                if (PictureMimeType.isHasVideo(media.getMimeType())) {
                                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                                    retriever.setDataSource(file.getPath());
                                    int width = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                                    int height = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                                    data.putInt("width", width);
                                    data.putInt("height", height);
                                    if (isCover) {
                                        File coverFile = getVideoCover(file.getPath());
                                        data.putString("coverFileName", coverFile.getName());
                                        data.putString("coverPath", coverFile.getPath());
                                        data.putString("coverUri", Uri.fromFile(coverFile).toString());
                                        data.putString("coverMime", getMimeType(coverFile));
                                        data.putDouble("coverSize", coverFile.length());
                                    }

                                }

                                if (includeBase64) {
                                    data.putString("data", getBase64StringFromFilePath(path));
                                }

                                list.pushMap(data);
                            }

                            promise.resolve(list);
                        } catch (Exception ex) {
                            promise.reject(ex);
                        }

                    }

                    @Override
                    public void onCancel() {
                        promise.reject("cancel", new Exception("cancel"));
                    }
                });
    }

    @SuppressWarnings("unused")
    @ReactMethod
    public void openGallery(final ReadableMap options, final Promise promise) {
        Activity currentActivity = this.getCurrentActivity();
        if (currentActivity != null) {
            int type = options.getInt("type");
            int maxNum = options.getInt("maxNum");
            int videoMaxNum = options.getInt("videoMaxNum");
            boolean openCamera = options.getBoolean("openCamera");
            boolean lookGifPhoto = options.getBoolean("lookGifPhoto");
            boolean selectTogether = options.getBoolean("selectTogether");
            int maxFileSize = options.getInt("maxFileSize");
            int videoMaximumDuration = options.getInt("videoMaximumDuration");
            int videoMinimumDuration = options.getInt("videoMinimumDuration");
            int videoMaximumSelectDuration = options.getInt("videoMaximumSelectDuration"); // 视频最大选择时间
            int videoMinimumSelectDuration = options.getInt("videoMinimumSelectDuration"); // 视频最小选择时间
            int videoQuality = options.getInt("videoQuality"); // 视频录制质量
            boolean singleSelected = options.getBoolean("singleSelected");
            boolean singleDirectReturn = options.getBoolean("singleDirectReturn");
            boolean isOriginalImageControl = options.getBoolean("isOriginalImageControl"); // 是否开启原图
            boolean isCompress = options.getBoolean("isCompress"); // 是否压缩
            int compressQuality = options.getInt("compressQuality"); // 压缩质量
            boolean isEnableCrop = options.getBoolean("isEnableCrop"); // 是否开启裁剪
            ReadableArray mimeTypeConditions = options.getArray("mimeTypeConditions");

            int[] pictureMimeType = {PictureMimeType.ofImage(), PictureMimeType.ofVideo(), PictureMimeType.ofAll()};
            PictureSelector.create(currentActivity)
                    .openGallery(pictureMimeType[type])
                    .queryMimeTypeConditions(mimeTypeConditions.toArrayList().toArray(new String[]{}))
                    .setPictureUIStyle(PictureSelectorUIStyle.ofNewStyle())
                    .maxSelectNum(maxNum) // 最大图片选择数量
                    .minSelectNum(0) // 最小选择数量
                    .maxVideoSelectNum(videoMaxNum) // 视频最大选择数量，如果没有单独设置的需求则可以不设置，同用maxSelectNum字段
                    .minVideoSelectNum(0) // 视频最小选择数量，如果没有单独设置的需求则可以不设置，同用minSelectNum字段
                    .isCamera(openCamera) // 是否显示拍照按钮
                    .isGif(lookGifPhoto) // 是否显示gif
                    .isWithVideoImage(selectTogether) // 图片和视频是否可以同选,只在ofAll模式下有效
                    .filterMinFileSize(0) // 过滤最小的文件
                    .filterMaxFileSize(maxFileSize) // 过滤最大的文件
                    .recordVideoSecond(videoMaximumDuration) // 录制视频秒数 默认60s
                    .recordVideoMinSecond(videoMinimumDuration) // 最低录制秒数
                    .videoMaxSecond(videoMaximumSelectDuration) // 查询多少秒以内的视频
                    .videoMinSecond(videoMinimumSelectDuration) // 查询多少秒以外的视频
                    .videoQuality(videoQuality)
                    .selectionMode(singleSelected ? PictureConfig.SINGLE : PictureConfig.MULTIPLE) // 多选 or 单选
                    .isSingleDirectReturn(singleDirectReturn) // 单选模式下是否直接返回，PictureConfig.SINGLE模式下有效
                    .isOriginalImageControl(isOriginalImageControl) // 是否开启原图
                    .isCompress(isCompress) // 是否压缩
                    .compressQuality(compressQuality)
                    .isEnableCrop(isEnableCrop) // 是否开启裁剪功能
                    .isPreviewImage(true) // 是否可预览图片
                    .isPreviewVideo(true) // 是否可预览视频
                    .imageSpanCount(4) // 每行显示个数
                    .isReturnEmpty(false) // 未选择数据时点击按钮是否可以返回
                    .isEnablePreviewAudio(false) // 是否可播放音频
                    .isOpenClickSound(false) // 是否开启点击声音
                    .isMaxSelectEnabledMask(true) //选择条件达到阀时列表是否启用蒙层效果
                    .imageEngine(GlideEngine.createGlideEngine()) // 外部传入图片加载引擎，必传项
                    .isUseCustomCamera(true) // 是否使用自定义相机
                    .setPictureWindowAnimationStyle(new PictureWindowAnimationStyle()) // 自定义相册启动退出动画
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) // 设置相册Activity方向，不设置默认使用系统
                    .forResult(new OnResultCallbackListener<LocalMedia>() {
                        @Override
                        public void onResult(List<LocalMedia> result) {
                            try {
                                WritableArray list = Arguments.createArray();
                                for (LocalMedia media : result) {
                                    WritableMap data = Arguments.createMap();
                                    String path = media.getRealPath();
                                    File file = new File(path);
                                    Uri uri = Uri.fromFile(file);

                                    data.putString("path", file.getPath());
                                    data.putString("uri", uri.toString());
                                    data.putString("fileName", file.getName());
                                    data.putDouble("size", file.length());
                                    data.putDouble("width", media.getWidth());
                                    data.putDouble("height", media.getHeight());
                                    data.putDouble("duration", media.getDuration());
                                    data.putString("mime", media.getMimeType());
                                    list.pushMap(data);
                                }

                                promise.resolve(list);
                            } catch (Exception ex) {
                                promise.reject(ex);
                            }

                        }

                        @Override
                        public void onCancel() {
                            promise.reject("cancel", new Exception("cancel"));
                        }
                    });
        }
    }

    @SuppressWarnings("unused")
    @ReactMethod
    public void openCrop(final String path, final ReadableMap options, final Promise promise) {
        Activity currentActivity = this.getCurrentActivity();
        if (currentActivity != null) {
            ActivityEventListener listener = new ActivityEventListener() {
                @Override
                public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
                    if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
                        final Uri resultUri = UCrop.getOutput(data);
                        WritableMap result = Arguments.createMap();
                        result.putString("uri", resultUri.toString());
                        result.putString("path", resultUri.getPath());
                        promise.resolve(result);
                    } else if (resultCode == UCrop.RESULT_ERROR) {
                        final Throwable cropError = UCrop.getError(data);
                        promise.reject(cropError);
                    }
                }

                @Override
                public void onNewIntent(Intent intent) {

                }
            };
            reactContext.removeActivityEventListener(listener);
            reactContext.addActivityEventListener(listener);

            try {
                File file = new File(path);
                String fileName = file.getName();
                File newFile = new File(reactContext.getApplicationContext().getExternalCacheDir(), "IMG_CROP_" + fileName);
                UCrop builder = UCrop.of(Uri.fromFile(file), Uri.fromFile(newFile));
                builder.withOptions(Utils.getCropOptions(options));
                builder.startAnimationActivity(currentActivity, PictureSelectionConfig.windowAnimationStyle.activityCropEnterAnimation);
            } catch (Exception ex) {
                ex.printStackTrace();
                promise.reject(ex);
            }
        }
    }

    @SuppressWarnings("unused")
    @ReactMethod
    public void clean() {
        Activity activity = getCurrentActivity();
        if (activity != null) {
            PictureCacheManager.deleteAllCacheDirFile(activity);
        }
    }

    @SuppressWarnings("unused")
    @ReactMethod
    public void getFileInfo(final String path, Promise promise) {
        try {
            WritableMap data = Arguments.createMap();
            File file = new File(path);
            String mime = getMimeType(file);
            int width = 0;
            int height = 0;
            long duration = 0;
            if (PictureMimeType.isHasImage(mime)) {
                Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
                width = bitmap.getWidth();
                height = bitmap.getHeight();
            }

            if (PictureMimeType.isHasVideo(mime) || PictureMimeType.isHasAudio(mime)) {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(file.getPath());
                width = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                height = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                duration = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            }

            data.putString("path", path);
            data.putString("uri", Uri.fromFile(file).toString());
            data.putString("fileName", file.getName());
            data.putDouble("size", file.length());
            data.putInt("width", width);
            data.putInt("height", height);
            data.putDouble("duration", duration);
            data.putString("mime", mime);
            promise.resolve(data);
        } catch (Exception ex) {
            ex.printStackTrace();
            promise.reject(ex);
        }
    }

    private String getBase64StringFromFilePath(String absoluteFilePath) {
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(absoluteFilePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        byte[] bytes;
        byte[] buffer = new byte[8192];
        int bytesRead;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        bytes = output.toByteArray();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    private File getImageCover(String imagePath, int width, int height) throws IOException {
        double ratio = (double) width / (double) height;
        int thumbWidth = Math.min(width, 375);
        int thumbHeight = Double.valueOf(thumbWidth / ratio).intValue();
        Bitmap thumbBitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(imagePath), thumbWidth, thumbHeight);
        final String uuid = "thumb-" + UUID.randomUUID().toString();
        final String localThumb = reactContext.getExternalCacheDir().getAbsolutePath() + "/" + uuid + ".png";
        final File file = new File(localThumb);
        FileOutputStream outStream = new FileOutputStream(file);
        thumbBitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
        outStream.close();

        return file;
    }

    private File getVideoCover(String videoPath) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(videoPath);
        Bitmap bitmap = retriever.getFrameAtTime();
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        double ratio = (double) width / (double) height;
        int thumbWidth = Math.min(width, 375);
        int thumbHeight = Double.valueOf(thumbWidth / ratio).intValue();
        Bitmap thumbBitmap = ThumbnailUtils.extractThumbnail(bitmap, thumbWidth, thumbHeight);
        final String uuid = "thumb-" + UUID.randomUUID().toString();
        final String localThumb = reactContext.getExternalCacheDir().getAbsolutePath() + "/" + uuid + ".png";
        final File file = new File(localThumb);
        FileOutputStream outStream = new FileOutputStream(file);
        thumbBitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
        outStream.close();
        retriever.release();

        return file;
    }

    private String getMimeType(File file) {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        return fileNameMap.getContentTypeFor(file.getName());
    }
}
