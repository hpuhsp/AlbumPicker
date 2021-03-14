package com.hsp.mms

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.luck.picture.PictureSelector
import com.luck.picture.config.PictureConfig
import com.luck.picture.config.PictureMimeType
import com.luck.picture.entity.LocalMedia
import com.luck.picture.utils.FileUtils

/**
 * @Description:
 * @Author:   Hsp
 * @Email:    1101121039@qq.com
 * @CreateTime:     2020/11/8 10:33
 * @UpdateRemark:   更新说明：
 */
class PictureHelper {

    companion object :
        SingletonHolderNoneArg<PictureHelper>(::PictureHelper)

    /**
     * FragmentActivity  开启相册
     */
    fun openAlbum(activity: FragmentActivity, list: List<LocalMedia>?) {
        PictureSelector.create(activity)
            .openGallery(PictureMimeType.ofAll())
//            .selectionMode(PictureConfig.SINGLE) // 单选模式
//            .isSingleDirectReturn(true)
            .isWithVideoImage(true)
            .isAndroidQTransform(true) // 必须添加，适配Android Q版本
            .theme(R.style.picture_default_style)
            .imageEngine(GlideEngine.createGlideEngine())
            .isCamera(true)
            .maxSelectNum(9)
            .maxVideoSelectNum(2)
            .isCompress(true)
            .isVideoCompress(true)
            .compressSavePath(FileUtils.getCompressFilePath(activity)) // 图片压缩路径，不包括视频
            .imageSpanCount(4)
            .selectionData(list ?: ArrayList()) // 是否传入已选图片
            .videoMaxSecond(16) // 限定选择10s的视频
            .videoMinSecond(5)
            .videoQuality(1)
            .isQuickCapture(false)
            .recordVideoSecond(15)
            .forResult(PictureConfig.CHOOSE_REQUEST)
    }

    /**
     * Fragment 开启相册
     */
    fun openAlbum(fragment: Fragment, list: List<LocalMedia>?) {
        PictureSelector.create(fragment)
            .openGallery(PictureMimeType.ofAll())
            .isWithVideoImage(true)
            .isAndroidQTransform(true) // 必须添加，适配Android Q版本
            .theme(R.style.picture_default_style)
            .imageEngine(GlideEngine.createGlideEngine())
            .isCamera(true)
            .maxSelectNum(9)
            .maxVideoSelectNum(2)
            .isCompress(true)
            .compressSavePath(fragment.context?.let { FileUtils.getCompressFilePath(it) }) // 图片压缩路径
            .imageSpanCount(4)
            .selectionData(list ?: ArrayList()) // 是否传入已选图片
            .videoMaxSecond(16) // 限定选择16s的视频
            .videoMinSecond(5)
            .isQuickCapture(false)
            .recordVideoSecond(15)
            .forResult(PictureConfig.CHOOSE_REQUEST)
    }


    /**
     * 预览图片/视频
     */
    fun previewMedia(
        activity: FragmentActivity,
        position: Int,
        selectList: List<LocalMedia>
    ) {
        if (selectList.isEmpty()) {
            return
        }
        val localMedia = selectList[position]
        when (PictureMimeType.getMimeType(localMedia.mimeType)) {
            PictureConfig.TYPE_VIDEO -> {
                // 预览视频
                PictureSelector.create(activity)
                    .externalPictureVideo(localMedia.path)
            }

            else -> {
                PictureSelector.create(activity)
                    .themeStyle(R.style.picture_default_style)
                    .isNotPreviewDownload(true)
                    .imageEngine(GlideEngine.createGlideEngine())
                    .openExternalPreview(position, selectList)
            }
        }
    }

    /**
     * 预览图片/视频
     */
    fun previewMedia(fragment: Fragment, position: Int, selectList: List<LocalMedia>) {
        if (selectList.isEmpty()) {
            return
        }
        val localMedia = selectList[position]
        when (PictureMimeType.getMimeType(localMedia.mimeType)) {
            PictureConfig.TYPE_VIDEO -> {
                // 预览视频
                PictureSelector.create(fragment)
                    .externalPictureVideo(localMedia.path)
            }

            else -> {
                PictureSelector.create(fragment)
                    .themeStyle(R.style.picture_default_style)
                    .isNotPreviewDownload(true)
                    .imageEngine(GlideEngine.createGlideEngine())
                    .openExternalPreview(position, selectList)
            }
        }
    }

    /**
     *判断是否是视频未见
     */
    fun isVideoFile(url: String): Boolean {
        val split = url.split(".")
        if (split.size == 2 && !split[1].isNullOrEmpty()) {
            return arrayOf(
                "avi",
                "flv",
                "mpg",
                "mpeg",
                "mpe",
                "m1v",
                "m2v",
                "mpv2",
                "mp2v",
                "dat",
                "ts",
                "tp",
                "tpr",
                "pva",
                "pss",
                "mp4",
                "m4v",
                "m4p",
                "m4b",
                "3gp",
                "3gpp",
                "3g2",
                "3gp2",
                "ogg",
                "mov",
                "qt",
                "amr",
                "rm",
                "ram",
                "rmvb",
                "rpm"
            ).contains(split[1])
        }
        return false
    }
}