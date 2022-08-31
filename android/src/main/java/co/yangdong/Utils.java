package co.yangdong;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.facebook.react.bridge.ReadableMap;
import com.luck.picture.lib.config.PictureSelectionConfig;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.view.OverlayView;

import java.util.Objects;

public class Utils {
    public static UCrop.Options getCropOptions(ReadableMap options) {
        UCrop.Options cropOptions = new UCrop.Options();
        if (options.hasKey("maxResultSize")) {
            ReadableMap maxResultSize = options.getMap("maxResultSize");
            cropOptions.withMaxResultSize(maxResultSize.getInt("width"), maxResultSize.getInt("height"));
        } else if (options.hasKey("aspectRatio")) {
            ReadableMap aspectRatio = options.getMap("aspectRatio");
            cropOptions.withAspectRatio((float) aspectRatio.getDouble("x"), (float) aspectRatio.getDouble("y"));
        } else {
            cropOptions.useSourceImageAspectRatio();
        }

        boolean customCropRatio = false;
        if (options.hasKey("customCropRatio")) {
            customCropRatio = options.getBoolean("customCropRatio");
        }
        cropOptions.setFreeStyleCropEnabled(customCropRatio);
        cropOptions.setFreestyleCropMode(customCropRatio ? OverlayView.FREESTYLE_CROP_MODE_ENABLE : OverlayView.FREESTYLE_CROP_MODE_DISABLE);// 裁剪框拖动模式

        if (options.hasKey("compressionFormat")) {
            String compressionFormat = options.getString("compressionFormat");
            cropOptions.setCompressionFormat(
                    "PNG".equals(compressionFormat) ? Bitmap.CompressFormat.PNG
                    : "WEBP".equals(compressionFormat) ? Bitmap.CompressFormat.WEBP
                    : Bitmap.CompressFormat.JPEG);
        }

        if (options.hasKey("compressionQuality")) {
            cropOptions.setCompressionQuality(options.getInt("compressionQuality"));
        }

        if (options.hasKey("requestedOrientation")) {
            cropOptions.setRequestedOrientation(options.getInt("requestedOrientation"));
        }

        if (options.hasKey("dimmedLayerColor")) {
            cropOptions.setDimmedLayerColor(Color.parseColor(options.getString("dimmedLayerColor")));
        }

        if (options.hasKey("dimmedLayerBorderColor")) {
            cropOptions.setDimmedLayerBorderColor(Color.parseColor(options.getString("dimmedLayerBorderColor")));
        }

        if (options.hasKey("circleStrokeWidth")) {
            cropOptions.setCircleStrokeWidth(options.getInt("circleStrokeWidth"));
        }

        if (options.hasKey("isCircle")) {
            cropOptions.setCircleDimmedLayer(options.getBoolean("isCircle")); // 是否开启圆形裁剪
        }

        if (options.hasKey("showCropFrame")) {
            cropOptions.setShowCropFrame(options.getBoolean("showCropFrame")); // 是否显示裁剪框
        }

        if (options.hasKey("cropFrameColor")) {
            cropOptions.setCropFrameColor(Color.parseColor(options.getString("cropFrameColor")));
        }

        if (options.hasKey("cropFrameStrokeWidth")) {
            cropOptions.setCropFrameStrokeWidth(options.getInt("cropFrameStrokeWidth"));
        }

        if (options.hasKey("showCropGrid")) {
            cropOptions.setShowCropGrid(options.getBoolean("showCropGrid")); // 是否显示裁剪框网格
        }

        if (options.hasKey("dragFrameEnabled")) {
            cropOptions.setDragFrameEnabled(options.getBoolean("dragFrameEnabled"));
        }

        if (options.hasKey("cropGridRowCount")) {
            cropOptions.setCropGridRowCount(options.getInt("cropGridRowCount"));
        }

        if (options.hasKey("scaleEnabled")) {
            cropOptions.setScaleEnabled(options.getBoolean("scaleEnabled"));
        }

        if (options.hasKey("rotateEnabled")) {
            cropOptions.setRotateEnabled(options.getBoolean("rotateEnabled")); // 是否开启旋转
        }

        if (options.hasKey("cropGridColumnCount")) {
            cropOptions.setCropGridColumnCount(options.getInt("cropGridColumnCount"));
        }

        if (options.hasKey("cropGridColor")) {
            cropOptions.setCropGridColor(Color.parseColor(options.getString("cropGridColor")));
        }

        if (options.hasKey("cropGridStrokeWidth")) {
            cropOptions.setCropGridStrokeWidth(options.getInt("cropGridStrokeWidth"));
        }

        if (options.hasKey("toolbarColor")) {
            cropOptions.setToolbarColor(Color.parseColor(options.getString("toolbarColor")));
        }

        if (options.hasKey("isOpenWhiteStatusBar")) {
            cropOptions.isOpenWhiteStatusBar(options.getBoolean("isOpenWhiteStatusBar"));
        }

        if (options.hasKey("statusBarColor")) {
            cropOptions.setStatusBarColor(Color.parseColor(options.getString("statusBarColor")));
        }

        if (options.hasKey("activeWidgetColor")) {
            cropOptions.setActiveWidgetColor(Color.parseColor(options.getString("activeWidgetColor")));
        }

        if (options.hasKey("activeControlsWidgetColor")) {
            cropOptions.setActiveControlsWidgetColor(Color.parseColor(options.getString("activeControlsWidgetColor")));
        }

        if (options.hasKey("toolbarWidgetColor")) {
            cropOptions.setToolbarWidgetColor(Color.parseColor(options.getString("toolbarWidgetColor")));
        }

        if (options.hasKey("toolbarTitle")) {
            cropOptions.setToolbarTitle(options.getString("toolbarTitle"));
        }

        if (options.hasKey("logoColor")) {
            cropOptions.setLogoColor(Color.parseColor(options.getString("logoColor")));
        }

        if (options.hasKey("editorImage")) {
            cropOptions.setEditorImage(options.getBoolean("editorImage"));
        }

        if (options.hasKey("hideBottomControls")) {
            cropOptions.setHideBottomControls(options.getBoolean("hideBottomControls")); // 是否隐藏底部工具栏
        }

        if (options.hasKey("cropDragSmoothToCenter")) {
            cropOptions.setCropDragSmoothToCenter(options.getBoolean("cropDragSmoothToCenter"));
        }

        if (options.hasKey("navBarColor")) {
            cropOptions.setNavBarColor(Color.parseColor(options.getString("navBarColor")));
        }

        if (options.hasKey("rootViewBackgroundColor")) {
            cropOptions.setRootViewBackgroundColor(Color.parseColor(options.getString("rootViewBackgroundColor")));
        }

        cropOptions.setCropExitAnimation(PictureSelectionConfig.windowAnimationStyle.activityCropExitAnimation);

        return cropOptions;
    }
}
