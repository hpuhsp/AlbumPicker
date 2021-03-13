package com.luck.picture.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;

import java.io.File;
import java.util.Random;

/**
 * @Description:
 * @Author: Hsp
 * @Email: 1101121039@qq.com
 * @CreateTime: 2021/3/12 11:47
 * @UpdateRemark: 更新说明：
 */
public class FileUtils {
    /**
     * SD卡读写权限
     */
    private static final String EXTERNAL_STORAGE_PERMISSION =
            "android.permission.WRITE_EXTERNAL_STORAGE";
    /**
     * 默认压缩文件缓存目录
     */
    private static final String MEDIA_CACHE_DIR = "CompressCache";

    public static String getCompressFilePath(Context context) {
        File appCacheDir = null;
        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()
                && hasExternalStoragePermission(context)
        ) {
            appCacheDir = new File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DCIM).getAbsolutePath(),
                    MEDIA_CACHE_DIR
            );
        }
        if (appCacheDir == null || !appCacheDir.exists() && !appCacheDir.mkdirs()) {
            appCacheDir = context.getCacheDir();
        }
        String path = appCacheDir.getPath();
        return null == path ? "" : path;
    }

    /**
     * 获取照片存储的路径
     *
     * @return
     */
    public static String getCompressVideoFile(Context context) {
        return new File(getCompressFilePath(context), "VID" + System.currentTimeMillis() + "_" + new Random().nextInt() + ".mp4").getAbsolutePath();
    }

    /**
     * 判断权限
     */
    private static boolean hasExternalStoragePermission(Context context) {
        int perm = context.checkCallingOrSelfPermission(EXTERNAL_STORAGE_PERMISSION);
        return perm == PackageManager.PERMISSION_GRANTED;
    }
}
