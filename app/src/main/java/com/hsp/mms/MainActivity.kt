package com.hsp.mms

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.luck.picture.PictureSelector
import com.luck.picture.config.PictureConfig
import com.luck.picture.config.PictureMimeType
import com.luck.picture.entity.LocalMedia
import com.luck.picture.utils.FileUtils.getCompressFilePath
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), NinePhotoView.OnClickItemListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        photo_view.addOnClickItemListener(this)
        photo_view.showPlus(true)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                PictureConfig.CHOOSE_REQUEST -> {
                    val selectList = PictureSelector.obtainMultipleResult(data)
                    photo_view.setList(selectList)
                }
            }
        }
    }

    override fun onItemClickPreview(position: Int, list: List<LocalMedia>) {
        PictureHelper.getInstance().previewMedia(this, position, list)
    }

    override fun onItemClickAddFromAlbum() {
        PictureHelper.getInstance().openAlbum(this, photo_view.getData())
    }

    override fun onItemClickDelete(position: Int) {
        System.out.println("--------------删除操作----------------->")
    }

    /**
     * FragmentActivity  开启相册
     */
    private fun openAlbum(activity: FragmentActivity, list: List<LocalMedia>?) {
        PictureSelector.create(activity)
            .openGallery(PictureMimeType.ofAll())
            .isWithVideoImage(true)
            .isAndroidQTransform(true) // 必须添加，适配Android Q版本
            .theme(R.style.picture_default_style)
            .imageEngine(GlideEngine.createGlideEngine())
            .isCamera(true)
            .synOrAsy(true)
            .maxSelectNum(9)
            .maxVideoSelectNum(2)
            .isCompress(false)
            .isVideoCompress(true)
            .compressSavePath(getCompressFilePath(activity)) // 图片压缩路径,包括视频
            .imageSpanCount(4)
            .selectionData(list ?: ArrayList()) // 是否传入已选图片
            .videoMaxSecond(16) // 限定选择10s的视频
            .videoMinSecond(5)
            .videoQuality(5)
            .isQuickCapture(false)
            .recordVideoSecond(15)
            .forResult(PictureConfig.CHOOSE_REQUEST)
    }
}