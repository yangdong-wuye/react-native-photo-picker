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
        int videoMaximumSelectDuration = options.getInt("videoMaximumSelectDuration"); // ????????????????????????
        int videoMinimumSelectDuration = options.getInt("videoMinimumSelectDuration"); // ????????????????????????
        int videoQuality = options.getInt("videoQuality"); // ??????????????????
        boolean isCompress = options.getBoolean("isCompress"); // ????????????
        int minimumCompressSize = options.getInt("minimumCompressSize"); // ????????????kb??????????????????
        int compressQuality = options.getInt("compressQuality"); // ??????????????????
        boolean videoCanEdit = options.getBoolean("videoCanEdit"); // ???????????????????????????TODO???
        boolean photoCanEdit = options.getBoolean("photoCanEdit"); // ????????????????????????
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
        cropOptions.setCircleDimmedLayer(isRoundCliping); // ????????????????????????
        cropOptions.isMultipleSkipCrop(true); // ????????????????????????????????????????????????
        cropOptions.setHideBottomControls(false); // ???????????????????????????
        cropOptions.setShowCropFrame(true); // ?????????????????????
        cropOptions.setShowCropGrid(true); // ???????????????????????????
        cropOptions.setFreeStyleCropEnabled(customCropRatio);
        cropOptions.setFreestyleCropMode(customCropRatio ? OverlayView.FREESTYLE_CROP_MODE_ENABLE : OverlayView.FREESTYLE_CROP_MODE_DISABLE);// ?????????????????????
        cropOptions.setCropDragSmoothToCenter(true); // ??????????????????????????????????????????

        int[] pictureMimeType = {PictureMimeType.ofImage(), PictureMimeType.ofVideo(), PictureMimeType.ofAll()};
        PictureSelector.create(this.getCurrentActivity())
                .openGallery(pictureMimeType[type])
                .queryMimeTypeConditions(mimeTypeConditions.toArrayList().toArray(new String[]{}))
                .setPictureUIStyle(PictureSelectorUIStyle.ofNewStyle())
                .maxSelectNum(maxNum) // ????????????????????????
                .minSelectNum(0) // ??????????????????
                .maxVideoSelectNum(videoMaxNum) // ???????????????????????????????????????????????????????????????????????????????????????maxSelectNum??????
                .minVideoSelectNum(0) // ???????????????????????????????????????????????????????????????????????????????????????minSelectNum??????
                .isCamera(openCamera) // ????????????????????????
                .isGif(lookGifPhoto) // ????????????gif
                .isWithVideoImage(selectTogether) // ?????????????????????????????????,??????ofAll???????????????
                .filterMinFileSize(0) // ?????????????????????
                .filterMaxFileSize(maxFileSize) // ?????????????????????
                .recordVideoSecond(videoMaximumDuration) // ?????????????????? ??????60s
                .recordVideoMinSecond(videoMinimumDuration) // ??????????????????
                .videoMaxSecond(videoMaximumSelectDuration) // ??????????????????????????????
                .videoMinSecond(videoMinimumSelectDuration) // ??????????????????????????????
                .videoQuality(videoQuality)
                .isCompress(isCompress) // ????????????
                .compressQuality(compressQuality) // ??????????????????
                .minimumCompressSize(minimumCompressSize) // ????????????kb??????????????????
                .selectionMode(singleSelected ? PictureConfig.SINGLE : PictureConfig.MULTIPLE) // ?????? or ??????
                .isSingleDirectReturn(singleJumpEdit) // ????????????????????????????????????PictureConfig.SINGLE???????????????
                .isPreviewImage(true) // ?????????????????????
                .isPreviewVideo(true) // ?????????????????????
                .imageSpanCount(4) // ??????????????????
                .isReturnEmpty(false) // ????????????????????????????????????????????????
                .isEnablePreviewAudio(false) // ?????????????????????
                .isOpenClickSound(false) // ????????????????????????
                .isMaxSelectEnabledMask(true) //??????????????????????????????????????????????????????
                .imageEngine(GlideEngine.createGlideEngine()) // ??????????????????????????????????????????
                .isUseCustomCamera(true)
                .setPictureWindowAnimationStyle(new PictureWindowAnimationStyle()) // ?????????????????????????????????
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) // ????????????Activity????????????????????????????????????
                .isEnableCrop(photoCanEdit) // ????????????????????????
                .rotateEnabled(false) // ????????????
                .basicUCropConfig(cropOptions) // ????????????
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
