package co.yangdong;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.NonNull;

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
import com.luck.picture.lib.compress.CompressionPredicate;
import com.luck.picture.lib.compress.Luban;
import com.luck.picture.lib.compress.OnCompressListener;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.engine.CompressEngine;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.listener.OnCallbackListener;
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

    private ReactContext reactContext;

    public PhotoPickerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    @NonNull
    public String getName() {
        return NAME;
    }


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
                .isOriginalImageControl(true) // 启用原图
                .forResult(new OnResultCallbackListener<LocalMedia>() {
                    @Override
                    public void onResult(List<LocalMedia> result) {
                        try {
                            WritableArray list = Arguments.createArray();
                            for (LocalMedia media : result) {
                                WritableMap data = Arguments.createMap();
                                String path = media.isCompressed() ?  media.getCompressPath() : media.isCut() ? media.getCutPath() : media.getRealPath();
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
                                    File coverFile = getImageCover(file.getPath(), width, height);
                                    data.putInt("width", width);
                                    data.putInt("height", height);
                                    data.putString("coverFileName", coverFile.getName());
                                    data.putString("coverPath", coverFile.getPath());
                                    data.putString("coverUri", Uri.fromFile(coverFile).toString());
                                    data.putString("coverMime", getMimeType(coverFile));
                                    data.putDouble("coverSize", coverFile.length());
                                }

                                if (PictureMimeType.isHasVideo(media.getMimeType())) {
                                    File coverFile = getVideoCover(file.getPath());
                                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                                    retriever.setDataSource(file.getPath());
                                    int width = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                                    int height = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                                    data.putInt("width", width);
                                    data.putInt("height", height);
                                    data.putString("coverFileName", coverFile.getName());
                                    data.putString("coverPath", coverFile.getPath());
                                    data.putString("coverUri", Uri.fromFile(coverFile).toString());
                                    data.putString("coverMime", getMimeType(coverFile));
                                    data.putDouble("coverSize", coverFile.length());
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


    @ReactMethod
    public void clean() {
        Activity activity = getCurrentActivity();
        if (activity != null) {
            PictureCacheManager.deleteAllCacheDirFile(activity);
        }
    }

    private String getBase64StringFromFilePath(String absoluteFilePath) {
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(new File(absoluteFilePath));
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
