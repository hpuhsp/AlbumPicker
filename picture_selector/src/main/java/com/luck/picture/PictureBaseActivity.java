package com.luck.picture;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.hw.videoprocessor.VideoProcessor;
import com.hw.videoprocessor.util.VideoProgressListener;
import com.luck.picture.app.PictureAppMaster;
import com.luck.picture.compress.Luban;
import com.luck.picture.compress.OnCompressListener;
import com.luck.picture.config.PictureConfig;
import com.luck.picture.config.PictureMimeType;
import com.luck.picture.config.PictureSelectionConfig;
import com.luck.picture.dialog.PictureCustomDialog;
import com.luck.picture.dialog.PictureLoadingDialog;
import com.luck.picture.dialog.VideoWorkProgressFragment;
import com.luck.picture.engine.PictureSelectorEngine;
import com.luck.picture.entity.LocalMedia;
import com.luck.picture.entity.LocalMediaFolder;
import com.luck.picture.immersive.ImmersiveManage;
import com.luck.picture.immersive.NavBarUtils;
import com.luck.picture.language.PictureLanguageUtils;
import com.luck.picture.lib.R;
import com.luck.picture.model.LocalMediaPageLoader;
import com.luck.picture.permissions.PermissionChecker;
import com.luck.picture.thread.PictureThreadUtils;
import com.luck.picture.tools.AndroidQTransformUtils;
import com.luck.picture.tools.AttrsUtils;
import com.luck.picture.tools.MediaUtils;
import com.luck.picture.tools.PictureFileUtils;
import com.luck.picture.tools.SdkVersionUtils;
import com.luck.picture.tools.StringUtils;
import com.luck.picture.tools.ToastUtils;
import com.luck.picture.tools.VoiceUtils;
import com.luck.picture.utils.FileUtils;
import com.yalantis.ucrop.view.widget.HorizontalProgressWheelView;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


/**
 * @author：luck
 * @data：2018/3/28 下午1:00
 * @describe: BaseActivity
 */
public abstract class PictureBaseActivity extends AppCompatActivity {
    protected PictureSelectionConfig config;
    protected boolean openWhiteStatusBar, numComplete;
    protected int colorPrimary, colorPrimaryDark;
    protected PictureLoadingDialog mLoadingDialog;
    protected List<LocalMedia> selectionMedias;
    protected Handler mHandler;
    protected View container;
    private VideoWorkProgressFragment mWorkLoadingProgress;
    /**
     * if there more
     */
    protected boolean isHasMore = true;
    /**
     * page
     */
    protected int mPage = 1;
    /**
     * is onSaveInstanceState
     */
    protected boolean isOnSaveInstanceState;

    /**
     * Whether to use immersion, subclasses copy the method to determine whether to use immersion
     *
     * @return
     */
    @Override
    public boolean isImmersive() {
        return true;
    }

    /**
     * Whether to change the screen direction
     *
     * @return
     */
    public boolean isRequestedOrientation() {
        return true;
    }


    public void immersive() {
        ImmersiveManage.immersiveAboveAPI23(this
                , colorPrimaryDark
                , colorPrimary
                , openWhiteStatusBar);
    }


    /**
     * get Layout Resources Id
     *
     * @return
     */
    public abstract int getResourceId();

    /**
     * init Views
     */
    protected void initWidgets() {

    }

    /**
     * init PictureSelector Style
     */
    protected void initPictureSelectorStyle() {

    }

    /**
     * Set CompleteText
     */
    protected void initCompleteText(int startCount) {

    }

    /**
     * Set CompleteText
     */
    protected void initCompleteText(List<LocalMedia> list) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        config = PictureSelectionConfig.getInstance();
        PictureLanguageUtils.setAppLanguage(getContext(), config.language);
        if (!config.camera) {
            setTheme(config.themeStyleId == 0 ? R.style.picture_default_style : config.themeStyleId);
        }
        super.onCreate(savedInstanceState);
        newCreateEngine();
        newCreateResultCallbackListener();
        if (isRequestedOrientation()) {
            setNewRequestedOrientation();
        }
        mHandler = new Handler(Looper.getMainLooper());
        initConfig();
        if (isImmersive()) {
            immersive();
        }
        if (PictureSelectionConfig.uiStyle != null) {
            if (PictureSelectionConfig.uiStyle.picture_navBarColor != 0) {
                NavBarUtils.setNavBarColor(this, PictureSelectionConfig.uiStyle.picture_navBarColor);
            }
        } else if (PictureSelectionConfig.style != null) {
            if (PictureSelectionConfig.style.pictureNavBarColor != 0) {
                NavBarUtils.setNavBarColor(this, PictureSelectionConfig.style.pictureNavBarColor);
            }
        }
        int layoutResID = getResourceId();
        if (layoutResID != 0) {
            setContentView(layoutResID);
        }
        initWidgets();
        initPictureSelectorStyle();
        isOnSaveInstanceState = false;
    }

    /**
     * Get the image loading engine again, provided that the user implements the IApp interface in the Application
     */
    private void newCreateEngine() {
        if (PictureSelectionConfig.imageEngine == null) {
            PictureSelectorEngine baseEngine = PictureAppMaster.getInstance().getPictureSelectorEngine();
            if (baseEngine != null) PictureSelectionConfig.imageEngine = baseEngine.createEngine();
        }
    }

    /**
     * Retrieve the result callback listener, provided that the user implements the IApp interface in the Application
     */
    private void newCreateResultCallbackListener() {
        if (config.isCallbackMode) {
            if (PictureSelectionConfig.listener == null) {
                PictureSelectorEngine baseEngine = PictureAppMaster.getInstance().getPictureSelectorEngine();
                if (baseEngine != null) {
                    PictureSelectionConfig.listener = baseEngine.getResultCallbackListener();
                }
            }
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        if (config == null) {
            super.attachBaseContext(newBase);
        } else {
            super.attachBaseContext(PictureContextWrapper.wrap(newBase, config.language));
        }
    }


    /**
     * setNewRequestedOrientation
     */
    protected void setNewRequestedOrientation() {
        if (config != null && !config.camera) {
            setRequestedOrientation(config.requestedOrientation);
        }
    }

    /**
     * get Context
     *
     * @return this
     */
    protected Context getContext() {
        return this;
    }

    /**
     * init Config
     */
    private void initConfig() {
        selectionMedias = config.selectionMedias == null ? new ArrayList<>() : config.selectionMedias;
        if (PictureSelectionConfig.uiStyle != null) {
            openWhiteStatusBar = PictureSelectionConfig.uiStyle.picture_statusBarChangeTextColor;
            if (PictureSelectionConfig.uiStyle.picture_top_titleBarBackgroundColor != 0) {
                colorPrimary = PictureSelectionConfig.uiStyle.picture_top_titleBarBackgroundColor;
            }
            if (PictureSelectionConfig.uiStyle.picture_statusBarBackgroundColor != 0) {
                colorPrimaryDark = PictureSelectionConfig.uiStyle.picture_statusBarBackgroundColor;
            }
            numComplete = PictureSelectionConfig.uiStyle.picture_switchSelectTotalStyle;

            config.checkNumMode = PictureSelectionConfig.uiStyle.picture_switchSelectNumberStyle;

        } else if (PictureSelectionConfig.style != null) {
            openWhiteStatusBar = PictureSelectionConfig.style.isChangeStatusBarFontColor;
            if (PictureSelectionConfig.style.pictureTitleBarBackgroundColor != 0) {
                colorPrimary = PictureSelectionConfig.style.pictureTitleBarBackgroundColor;
            }
            if (PictureSelectionConfig.style.pictureStatusBarColor != 0) {
                colorPrimaryDark = PictureSelectionConfig.style.pictureStatusBarColor;
            }
            numComplete = PictureSelectionConfig.style.isOpenCompletedNumStyle;
            config.checkNumMode = PictureSelectionConfig.style.isOpenCheckNumStyle;
        } else {
            openWhiteStatusBar = config.isChangeStatusBarFontColor;
            if (!openWhiteStatusBar) {
                openWhiteStatusBar = AttrsUtils.getTypeValueBoolean(this, R.attr.picture_statusFontColor);
            }

            numComplete = config.isOpenStyleNumComplete;
            if (!numComplete) {
                numComplete = AttrsUtils.getTypeValueBoolean(this, R.attr.picture_style_numComplete);
            }

            config.checkNumMode = config.isOpenStyleCheckNumMode;
            if (!config.checkNumMode) {
                config.checkNumMode = AttrsUtils.getTypeValueBoolean(this, R.attr.picture_style_checkNumMode);
            }

            if (config.titleBarBackgroundColor != 0) {
                colorPrimary = config.titleBarBackgroundColor;
            } else {
                colorPrimary = AttrsUtils.getTypeValueColor(this, R.attr.colorPrimary);
            }

            if (config.pictureStatusBarColor != 0) {
                colorPrimaryDark = config.pictureStatusBarColor;
            } else {
                colorPrimaryDark = AttrsUtils.getTypeValueColor(this, R.attr.colorPrimaryDark);
            }
        }

        if (config.openClickSound) {
            VoiceUtils.getInstance().init(getContext());
        }
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        isOnSaveInstanceState = true;
        outState.putParcelable(PictureConfig.EXTRA_CONFIG, config);
    }

    /**
     * loading dialog
     */
    protected void showPleaseDialog() {
        try {
            if (!isFinishing()) {
                if (mLoadingDialog == null) {
                    mLoadingDialog = new PictureLoadingDialog(getContext());
                }
                if (mLoadingDialog.isShowing()) {
                    mLoadingDialog.dismiss();
                }
                mLoadingDialog.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * dismiss dialog
     */
    protected void dismissDialog() {
        if (!isFinishing()) {
            try {
                if (mLoadingDialog != null
                        && mLoadingDialog.isShowing()) {
                    mLoadingDialog.dismiss();
                }
            } catch (Exception e) {
                mLoadingDialog = null;
                e.printStackTrace();
            }
        }
    }


    /**
     * compressImage
     */
    protected void compressImage(final List<LocalMedia> result) {
        showPleaseDialog();
        compressMediaFiles(result);
    }

    /**
     * Compress Video And Images
     * 说明：考虑到不过度引入其他库（如:RxJava、Kotlin Flow）的原则，此处以JDK原生内容实现。
     * 由于Android Api版本25才支持CompletableFuture.runAsync()等方式，当前用CompletionService类实现多
     * 线程处理。
     *
     * @param result
     */
    protected void compressMediaFiles(List<LocalMedia> result) {
        showPleaseDialog();
        String cachePath = "";
        if (null == config.compressSavePath || config.compressSavePath.isEmpty()) {
            cachePath = FileUtils.getCompressFilePath(this);
        } else {
            cachePath = config.compressSavePath;
        }
        List<LocalMedia> imageList = new ArrayList<>();
        List<LocalMedia> videoList = new ArrayList<>();
        for (LocalMedia media : result) {
            if (PictureMimeType.isHasImage(media.getMimeType())) {
                imageList.add(media);
            } else if (PictureMimeType.isHasVideo(media.getMimeType())) {
                videoList.add(media);
            }
        }
        int size = 0;
        if (config.isCompress && !config.isCheckOriginalImage) {
            size += imageList.size();
        }
        if (config.isVideoCompress) {
            size += videoList.size();
        }
        int compressSize = size;
        final int[] taskSize = {0};
        try {
            CompletionService<String> completionService = new ExecutorCompletionService<String>(PictureThreadUtils.getIoPool());
            if (config.synOrAsy) {
                // 同步实现,先压缩图片后压缩视频。
                if (!imageList.isEmpty() && config.isCompress && !config.isCheckOriginalImage) {
                    completionService.submit(new ImageCompressTask(imageList, cachePath, new OnCompressImageCallBack() {
                        @Override
                        public void compressFailed(List<LocalMedia> images) {
                            taskSize[0] += images.size();
                            if (taskSize[0] == compressSize) {
                                backForResult(result);
                            } else {
                                dismissDialog();
                                // 压缩视频
                                compressVideoFiles(completionService, result, videoList, taskSize, compressSize);
                            }
                        }

                        @Override
                        public void compressSuccess(List<LocalMedia> images, List<File> files) {
                            taskSize[0] += images.size();
                            checkImagesCompressResult(images, files);
                            if (taskSize[0] == compressSize) {
                                backForResult(result);
                            } else {
                                dismissDialog();
                                compressVideoFiles(completionService, result, videoList, taskSize, compressSize);
                            }
                        }
                    }), "默认图片压缩返回结果");
                } else if (config.isVideoCompress && !videoList.isEmpty()) {
                    dismissDialog();
                    compressVideoFiles(completionService, result, videoList, taskSize, compressSize);
                }

            } else {
                // 异步实现，图片、视频资源并行压缩【推荐使用】
                // 图片压缩
                if (!imageList.isEmpty() && config.isCompress && !config.isCheckOriginalImage) {
                    Luban.with(this)
                            .loadMediaData(imageList)
                            .ignoreBy(config.minimumCompressSize)
                            .isCamera(config.camera)
                            .setCompressQuality(config.compressQuality)
                            .setTargetDir(cachePath)
                            .setFocusAlpha(config.focusAlpha)
                            .setNewCompressFileName(config.renameCompressFileName)
                            .setCompressListener(new OnCompressListener() {
                                @Override
                                public void onStart() {
                                }

                                @Override
                                public void onSuccess(List<LocalMedia> images) {
                                    dismissDialog();
                                    taskSize[0] += images.size();
                                    if (taskSize[0] == compressSize) {
                                        backForResult(result);
                                    }
                                }

                                @Override
                                public void onError(Throwable e) {
                                    dismissDialog();
                                    taskSize[0] += imageList.size();
                                    if (taskSize[0] == compressSize) {
                                        backForResult(result);
                                    }
                                }
                            }).launch();
                }
                // 视频压缩
                if (config.isVideoCompress && !videoList.isEmpty()) {
                    compressVideoFiles(completionService, result, videoList, taskSize, compressSize);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            backForResult(result);
        }
    }

    /**
     * 返回结果
     *
     * @param result
     */
    private void backForResult(List<LocalMedia> result) {
        if (mWorkLoadingProgress != null && mWorkLoadingProgress.isAdded()) {
            mWorkLoadingProgress.dismiss();
        }
        for (LocalMedia media : result) {
            System.out.println("CompressPath：------------------------>" + media.getCompressPath());
        }
        onResult(result);
    }

    /**
     * 视频上传进度条
     */
    private void initWorkLoadingProgress() {
        if (mWorkLoadingProgress == null) {
            mWorkLoadingProgress = VideoWorkProgressFragment.newInstance(getString(R.string.video_in_processing));
        }
        mWorkLoadingProgress.setProgress(0);
        mWorkLoadingProgress.show(getSupportFragmentManager(), "progress_dialog");

    }

    /**
     * 压缩视频文件
     *
     * @param result
     * @param videoList
     * @param taskSize
     */
    private void compressVideoFiles(CompletionService<String> completionService, List<LocalMedia> result, List<LocalMedia> videoList, int[] taskSize, int compressSize) {
        initWorkLoadingProgress();
        final int size = videoList.size();
        final float unit = (float) 100 / size;
        Map<Integer, Float> concurrentHashMap = new ConcurrentHashMap<Integer, Float>();
        for (int i = 0; i < videoList.size(); i++) {
            final int j = i;
            concurrentHashMap.put(j, 0f);
            VideoProcessor.Processor processor = VideoProcessor.processor(this);
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(this, Uri.parse(videoList.get(i).getPath()));
            videoList.get(i).setCompressPath(FileUtils.getCompressVideoFile(this));
            retriever.setDataSource(this, Uri.parse(videoList.get(i).getPath()));
            completionService.submit(new VideoCompressTask(processor, videoList.get(i), retriever, new OnCompressVideoCallBack() {
                @Override
                public void onCompressSuccess(LocalMedia localMedia) {
                    taskSize[0]++;
                    if (taskSize[0] == compressSize) {
                        backForResult(result);
                    }
                }

                @Override
                public void onCompressFailed() {
                    ToastUtils.s(PictureBaseActivity.this, "视频压缩失败!");
                    taskSize[0]++;
                    if (taskSize[0] == compressSize) {
                        backForResult(result);
                    }
                }

                @Override
                public void onCompleteProgress(float progress) {
                    concurrentHashMap.put(j, unit * progress);
                    calculateTaskProgress(concurrentHashMap, size);
                }
            }), "默认视频压缩返回结果");
        }
    }

    /**
     * 计算视频压缩进度
     *
     * @param concurrentHashMap
     * @param size
     */
    private void calculateTaskProgress(Map<Integer, Float> concurrentHashMap, int size) {
        System.out.println("------------------currentThread------------>" + Thread.currentThread().getName());
        float completeProgress = 0;
        for (int a = 0; a < size; a++) {
            completeProgress += concurrentHashMap.get(a);
        }
        if (null != mWorkLoadingProgress && mWorkLoadingProgress.isAdded()) {
            mWorkLoadingProgress.setProgress((int) completeProgress);
        }
    }

    /**
     * 检查图片压缩结果，兼容Android Q版本
     *
     * @param images
     * @param files
     */
    private void checkImagesCompressResult(List<LocalMedia> images, List<File> files) {
        if (images == null || files == null) {
            exit();
            return;
        }
        boolean isAndroidQ = SdkVersionUtils.checkedAndroid_Q();
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            if (file == null) {
                continue;
            }
            String path = file.getAbsolutePath();
            LocalMedia image = images.get(i);
            boolean http = PictureMimeType.isHasHttp(path);
            boolean flag = !TextUtils.isEmpty(path) && http;
            boolean isHasVideo = PictureMimeType.isHasVideo(image.getMimeType());
            image.setCompressed(!isHasVideo && !flag);
            image.setCompressPath(isHasVideo || flag ? null : path);
            if (isAndroidQ) {
                image.setAndroidQToPath(image.getCompressPath());
            }
        }
    }

    /**
     * 视频压缩
     */
    private class VideoCompressTask extends PictureThreadUtils.SimpleTask<LocalMedia> {
        private final VideoProcessor.Processor processor;
        private final LocalMedia localMedia;
        private final MediaMetadataRetriever retriever;
        private final OnCompressVideoCallBack mCallBack;

        public VideoCompressTask(VideoProcessor.Processor processor, LocalMedia localMedia, MediaMetadataRetriever retriever, OnCompressVideoCallBack callBack) {
            this.processor = processor;
            this.localMedia = localMedia;
            this.retriever = retriever;
            this.mCallBack = callBack;
        }

        @Override
        public LocalMedia doInBackground() throws Throwable {
            int originWidth = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            int originHeight = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            int bitrate = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE));
            String filePath = "";
            if (SdkVersionUtils.checkedAndroid_Q()) {
                filePath = localMedia.getRealPath();
            } else {
                filePath = localMedia.getPath();
            }
            if (PictureMimeType.isContent(filePath)) {
                filePath = PictureFileUtils.getPath(PictureBaseActivity.this, Uri.parse(localMedia.getPath()));
            }

            processor
                    .input(filePath)
                    .output(localMedia.getCompressPath())
                    .outWidth(originWidth / 3)
                    .outHeight(originHeight / 3)
                    .speed(2f)
                    .bitrate(bitrate / 3)
                    .progressListener(new VideoProgressListener() {
                        @Override
                        public void onProgress(float progress) {
                            mCallBack.onCompleteProgress(progress);
                        }
                    })
                    .process();
            return localMedia;
        }

        @Override
        public void onSuccess(LocalMedia result) {
            mCallBack.onCompressSuccess(result);
        }

        @Override
        public void onFail(Throwable t) {
            super.onFail(t);
            mCallBack.onCompressFailed();
        }
    }

    interface OnCompressVideoCallBack {
        void onCompressSuccess(LocalMedia localMedia);

        void onCompressFailed();

        void onCompleteProgress(float progress);
    }

    /**
     * 图片压缩
     *
     * @param
     */
    private class ImageCompressTask extends PictureThreadUtils.SimpleTask<List<File>> {
        private List<LocalMedia> list;
        private String cachePath;
        private OnCompressImageCallBack mCallBack;

        public ImageCompressTask(List<LocalMedia> list, String path, OnCompressImageCallBack callBack) {
            this.list = list;
            this.cachePath = path;
            this.mCallBack = callBack;
        }

        @Override
        public List<File> doInBackground() throws Throwable {
            // 同步方式
            return Luban.with(getContext())
                    .loadMediaData(list)
                    .isCamera(config.camera)
                    .setTargetDir(cachePath)
                    .setCompressQuality(config.compressQuality)
                    .setFocusAlpha(config.focusAlpha)
                    .setNewCompressFileName(config.renameCompressFileName)
                    .ignoreBy(config.minimumCompressSize).get();
        }

        @Override
        public void onSuccess(List<File> files) {
            if (files != null && files.size() > 0 && files.size() == list.size()) {
                mCallBack.compressSuccess(list, files);
            } else {
                mCallBack.compressFailed(list);
            }
        }
    }

    interface OnCompressImageCallBack {
        void compressFailed(List<LocalMedia> list);

        void compressSuccess(List<LocalMedia> images, List<File> files);
    }

    /**
     * compress
     * 默认开启线程，采用异步方式
     *
     * @param result
     */
    private void compressToLuban(List<LocalMedia> result) {
        if (config.synOrAsy) {
            PictureThreadUtils.executeByIo(new PictureThreadUtils.SimpleTask<List<File>>() {

                @Override
                public List<File> doInBackground() throws Exception {
                    return Luban.with(getContext())
                            .loadMediaData(result)
                            .isCamera(config.camera)
                            .setTargetDir(config.compressSavePath)
                            .setCompressQuality(config.compressQuality)
                            .setFocusAlpha(config.focusAlpha)
                            .setNewCompressFileName(config.renameCompressFileName)
                            .ignoreBy(config.minimumCompressSize).get();
                }

                @Override
                public void onSuccess(List<File> files) {
                    if (files != null && files.size() > 0 && files.size() == result.size()) {
                        handleCompressCallBack(result, files);
                    } else {
                        onResult(result);
                    }
                }
            });
        } else {
            Luban.with(this)
                    .loadMediaData(result)
                    .ignoreBy(config.minimumCompressSize)
                    .isCamera(config.camera)
                    .setCompressQuality(config.compressQuality)
                    .setTargetDir(config.compressSavePath)
                    .setFocusAlpha(config.focusAlpha)
                    .setNewCompressFileName(config.renameCompressFileName)
                    .setCompressListener(new OnCompressListener() {
                        @Override
                        public void onStart() {
                        }

                        @Override
                        public void onSuccess(List<LocalMedia> list) {
                            onResult(list);
                        }

                        @Override
                        public void onError(Throwable e) {
                            onResult(result);
                        }
                    }).launch();
        }
    }

    /**
     * handleCompressCallBack
     *
     * @param images
     * @param files
     */
    private void handleCompressCallBack(List<LocalMedia> images, List<File> files) {
        if (images == null || files == null) {
            exit();
            return;
        }
        boolean isAndroidQ = SdkVersionUtils.checkedAndroid_Q();
        int size = images.size();
        if (files.size() == size) {
            for (int i = 0, j = size; i < j; i++) {
                File file = files.get(i);
                if (file == null) {
                    continue;
                }
                String path = file.getAbsolutePath();
                LocalMedia image = images.get(i);
                boolean http = PictureMimeType.isHasHttp(path);
                boolean flag = !TextUtils.isEmpty(path) && http;
                boolean isHasVideo = PictureMimeType.isHasVideo(image.getMimeType());
                image.setCompressed(!isHasVideo && !flag);
                image.setCompressPath(isHasVideo || flag ? null : path);
                if (isAndroidQ) {
                    image.setAndroidQToPath(image.getCompressPath());
                }
            }
        }
        onResult(images);
    }


    /**
     * compress or callback
     *
     * @param result
     */
    protected void handlerResult(List<LocalMedia> result) {
        if ((config.isCompress
                && !config.isCheckOriginalImage) || config.isVideoCompress) {
            compressMediaFiles(result);
        } else {
            onResult(result);
        }
    }

    /**
     * If you don't have any albums, first create a camera film folder to come out
     *
     * @param folders
     */
    protected void createNewFolder(List<LocalMediaFolder> folders) {
        if (folders.size() == 0) {
            // 没有相册 先创建一个最近相册出来
            LocalMediaFolder newFolder = new LocalMediaFolder();
            String folderName = config.chooseMode == PictureMimeType.ofAudio() ?
                    getString(R.string.picture_all_audio) : getString(R.string.picture_camera_roll);
            newFolder.setName(folderName);
            newFolder.setFirstImagePath("");
            newFolder.setCameraFolder(true);
            newFolder.setBucketId(-1);
            newFolder.setChecked(true);
            folders.add(newFolder);
        }
    }

    /**
     * Insert the image into the camera folder
     *
     * @param path
     * @param imageFolders
     * @return
     */
    protected LocalMediaFolder getImageFolder(String path, String realPath, List<LocalMediaFolder> imageFolders) {
        File imageFile = new File(PictureMimeType.isContent(path) ? realPath : path);
        File folderFile = imageFile.getParentFile();
        for (LocalMediaFolder folder : imageFolders) {
            if (folderFile != null && folder.getName().equals(folderFile.getName())) {
                return folder;
            }
        }
        LocalMediaFolder newFolder = new LocalMediaFolder();
        newFolder.setName(folderFile != null ? folderFile.getName() : "");
        newFolder.setFirstImagePath(path);
        imageFolders.add(newFolder);
        return newFolder;
    }

    /**
     * return image result
     *
     * @param mediaFiles
     */
    protected void onResult(List<LocalMedia> mediaFiles) {
        boolean isAndroidQ = SdkVersionUtils.checkedAndroid_Q();
        if (isAndroidQ && config.isAndroidQTransform) {
            showPleaseDialog();
            onResultToAndroidAsy(mediaFiles);
        } else {
            dismissDialog();
            if (config.camera
                    && config.selectionMode == PictureConfig.MULTIPLE
                    && selectionMedias != null) {
                mediaFiles.addAll(mediaFiles.size() > 0 ? mediaFiles.size() - 1 : 0, selectionMedias);
            }
            if (config.isCheckOriginalImage) {
                int size = mediaFiles.size();
                for (int i = 0; i < size; i++) {
                    LocalMedia media = mediaFiles.get(i);
                    media.setOriginal(true);
                    media.setOriginalPath(media.getPath());
                }
            }
            if (PictureSelectionConfig.listener != null) {
                PictureSelectionConfig.listener.onResult(mediaFiles);
            } else {
                Intent intent = PictureSelector.putIntentResult(mediaFiles);
                setResult(RESULT_OK, intent);
            }
            exit();
        }
    }

    /**
     * Android Q
     *
     * @param images
     */
    private void onResultToAndroidAsy(List<LocalMedia> images) {
        PictureThreadUtils.executeByIo(new PictureThreadUtils.SimpleTask<List<LocalMedia>>() {
            @Override
            public List<LocalMedia> doInBackground() {
                int size = images.size();
                for (int i = 0; i < size; i++) {
                    LocalMedia media = images.get(i);
                    if (media == null || TextUtils.isEmpty(media.getPath())) {
                        continue;
                    }
                    boolean isCopyAndroidQToPath = !media.isCut()
                            && !media.isCompressed()
                            && TextUtils.isEmpty(media.getAndroidQToPath());
                    if (isCopyAndroidQToPath && PictureMimeType.isContent(media.getPath())) {
                        if (!PictureMimeType.isHasHttp(media.getPath())) {
                            String AndroidQToPath = AndroidQTransformUtils.copyPathToAndroidQ(getContext(),
                                    media.getPath(), media.getWidth(), media.getHeight(), media.getMimeType(), config.cameraFileName);
                            media.setAndroidQToPath(AndroidQToPath);
                        }
                    } else if (media.isCut() && media.isCompressed()) {
                        media.setAndroidQToPath(media.getCompressPath());
                    }
                    if (config.isCheckOriginalImage) {
                        media.setOriginal(true);
                        media.setOriginalPath(media.getAndroidQToPath());
                    }
                }
                return images;
            }

            @Override
            public void onSuccess(List<LocalMedia> images) {
                dismissDialog();
                if (images != null) {
                    if (config.camera
                            && config.selectionMode == PictureConfig.MULTIPLE
                            && selectionMedias != null) {
                        images.addAll(images.size() > 0 ? images.size() - 1 : 0, selectionMedias);
                    }
                    if (PictureSelectionConfig.listener != null) {
                        PictureSelectionConfig.listener.onResult(images);
                    } else {
                        Intent intent = PictureSelector.putIntentResult(images);
                        setResult(RESULT_OK, intent);
                    }
                    exit();
                }
            }
        });
    }

    /**
     * Close Activity
     */
    protected void exit() {
        finish();
        if (config.camera) {
            overridePendingTransition(0, R.anim.picture_anim_fade_out);
            if (getContext() instanceof PictureSelectorCameraEmptyActivity
                    || getContext() instanceof PictureCustomCameraActivity) {
                releaseResultListener();
            }
        } else {
            overridePendingTransition(0,
                    PictureSelectionConfig.windowAnimationStyle.activityExitAnimation);
            if (getContext() instanceof PictureSelectorActivity) {
                releaseResultListener();
                if (config.openClickSound) {
                    VoiceUtils.getInstance().releaseSoundPool();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mLoadingDialog != null) {
            mLoadingDialog.dismiss();
            mLoadingDialog = null;
        }
        super.onDestroy();
    }


    /**
     * get audio path
     *
     * @param data
     */
    protected String getAudioPath(Intent data) {
        if (data != null && config.chooseMode == PictureMimeType.ofAudio()) {
            try {
                Uri uri = data.getData();
                if (uri != null) {
                    return Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT ? uri.getPath() : MediaUtils.getAudioFilePathFromUri(getContext(), uri);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "";
    }


    /**
     * start to camera、preview、crop
     */
    protected void startOpenCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            Uri imageUri;
            String cameraFileName = null;
            int chooseMode = config.chooseMode == PictureConfig.TYPE_ALL ? PictureConfig.TYPE_IMAGE : config.chooseMode;
            if (!TextUtils.isEmpty(config.cameraFileName)) {
                boolean isSuffixOfImage = PictureMimeType.isSuffixOfImage(config.cameraFileName);
                config.cameraFileName = !isSuffixOfImage ? StringUtils.renameSuffix(config.cameraFileName, PictureMimeType.JPEG) : config.cameraFileName;
                cameraFileName = config.camera ? config.cameraFileName : StringUtils.rename(config.cameraFileName);
            }
            if (SdkVersionUtils.checkedAndroid_Q()) {
                if (TextUtils.isEmpty(config.outPutCameraPath)) {
                    imageUri = MediaUtils.createImageUri(this, config.cameraFileName, config.suffixType);
                } else {
                    File cameraFile = PictureFileUtils.createCameraFile(this,
                            chooseMode, cameraFileName, config.suffixType, config.outPutCameraPath);
                    config.cameraPath = cameraFile.getAbsolutePath();
                    imageUri = PictureFileUtils.parUri(this, cameraFile);
                }
                if (imageUri != null) {
                    config.cameraPath = imageUri.toString();
                }
            } else {
                File cameraFile = PictureFileUtils.createCameraFile(this, chooseMode, cameraFileName, config.suffixType, config.outPutCameraPath);
                config.cameraPath = cameraFile.getAbsolutePath();
                imageUri = PictureFileUtils.parUri(this, cameraFile);
            }
            if (imageUri == null) {
                ToastUtils.s(getContext(), "open is camera error，the uri is empty ");
                if (config.camera) {
                    exit();
                }
                return;
            }
            config.cameraMimeType = PictureMimeType.ofImage();
            if (config.isCameraAroundState) {
                cameraIntent.putExtra(PictureConfig.CAMERA_FACING, PictureConfig.CAMERA_BEFORE);
            }
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(cameraIntent, PictureConfig.REQUEST_CAMERA);
        }
    }


    /**
     * start to camera、video
     */
    protected void startOpenCameraVideo() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            Uri videoUri;
            String cameraFileName = null;
            int chooseMode = config.chooseMode == PictureConfig.TYPE_ALL ? PictureConfig.TYPE_VIDEO : config.chooseMode;
            if (!TextUtils.isEmpty(config.cameraFileName)) {
                boolean isSuffixOfImage = PictureMimeType.isSuffixOfImage(config.cameraFileName);
                config.cameraFileName = isSuffixOfImage ? StringUtils.renameSuffix(config.cameraFileName, PictureMimeType.MP4) : config.cameraFileName;
                cameraFileName = config.camera ? config.cameraFileName : StringUtils.rename(config.cameraFileName);
            }
            if (SdkVersionUtils.checkedAndroid_Q()) {
                if (TextUtils.isEmpty(config.outPutCameraPath)) {
                    videoUri = MediaUtils.createVideoUri(this, config.cameraFileName, config.suffixType);
                } else {
                    File cameraFile = PictureFileUtils.createCameraFile(this, chooseMode, cameraFileName, config.suffixType, config.outPutCameraPath);
                    config.cameraPath = cameraFile.getAbsolutePath();
                    videoUri = PictureFileUtils.parUri(this, cameraFile);
                }
                if (videoUri != null) {
                    config.cameraPath = videoUri.toString();
                }
            } else {
                File cameraFile = PictureFileUtils.createCameraFile(this, chooseMode, cameraFileName, config.suffixType, config.outPutCameraPath);
                config.cameraPath = cameraFile.getAbsolutePath();
                videoUri = PictureFileUtils.parUri(this, cameraFile);
            }
            if (videoUri == null) {
                ToastUtils.s(getContext(), "open is camera error，the uri is empty ");
                if (config.camera) {
                    exit();
                }
                return;
            }
            config.cameraMimeType = PictureMimeType.ofVideo();
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
            if (config.isCameraAroundState) {
                cameraIntent.putExtra(PictureConfig.CAMERA_FACING, PictureConfig.CAMERA_BEFORE);
            }
            cameraIntent.putExtra(PictureConfig.EXTRA_QUICK_CAPTURE, config.isQuickCapture);
            cameraIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, config.recordVideoSecond);
            cameraIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, config.videoQuality);
            startActivityForResult(cameraIntent, PictureConfig.REQUEST_CAMERA);
        }
    }

    /**
     * start to camera audio
     */
    public void startOpenCameraAudio() {
        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)) {
            Intent cameraIntent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                config.cameraMimeType = PictureMimeType.ofAudio();
                startActivityForResult(cameraIntent, PictureConfig.REQUEST_CAMERA);
            }
        } else {
            PermissionChecker.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, PictureConfig.APPLY_AUDIO_PERMISSIONS_CODE);
        }
    }

    /**
     * Release listener
     */
    private void releaseResultListener() {
        if (config != null) {
            PictureSelectionConfig.destroy();
            LocalMediaPageLoader.setInstanceNull();
            PictureThreadUtils.cancel(PictureThreadUtils.getIoPool());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PictureConfig.APPLY_AUDIO_PERMISSIONS_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent cameraIntent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
                if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(cameraIntent, PictureConfig.REQUEST_CAMERA);
                }
            } else {
                ToastUtils.s(getContext(), getString(R.string.picture_audio));
            }
        }
    }

    /**
     * showPermissionsDialog
     *
     * @param isCamera
     * @param errorMsg
     */
    protected void showPermissionsDialog(boolean isCamera, String errorMsg) {

    }

    /**
     * Dialog
     *
     * @param content
     */
    protected void showPromptDialog(String content) {
        if (!isFinishing()) {
            PictureCustomDialog dialog = new PictureCustomDialog(getContext(), R.layout.picture_prompt_dialog);
            TextView btnOk = dialog.findViewById(R.id.btnOk);
            TextView tvContent = dialog.findViewById(R.id.tv_content);
            tvContent.setText(content);
            btnOk.setOnClickListener(v -> {
                if (!isFinishing()) {
                    dialog.dismiss();
                }
            });
            dialog.show();
        }
    }


    /**
     * sort
     *
     * @param imageFolders
     */
    protected void sortFolder(List<LocalMediaFolder> imageFolders) {
        Collections.sort(imageFolders, (lhs, rhs) -> {
            if (lhs.getData() == null || rhs.getData() == null) {
                return 0;
            }
            int lSize = lhs.getImageNum();
            int rSize = rhs.getImageNum();
            return Integer.compare(rSize, lSize);
        });
    }
}
