package com.wuye;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.luck.picture.lib.engine.CacheResourcesEngine;

import java.io.File;
import java.util.concurrent.ExecutionException;

public class GlideCacheEngine implements CacheResourcesEngine {

    @Override
    public String onCachePath(Context context, String url) {
        File cacheFile = null;
        try {
            cacheFile = Glide.with(context).downloadOnly().load(url).submit().get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return cacheFile != null ? cacheFile.getAbsolutePath() : "";
    }

    private GlideCacheEngine() {
    }

    private static GlideCacheEngine instance;

    public static GlideCacheEngine createCacheEngine() {
        if (null == instance) {
            synchronized (GlideCacheEngine.class) {
                if (null == instance) {
                    instance = new GlideCacheEngine();
                }
            }
        }
        return instance;
    }
}
