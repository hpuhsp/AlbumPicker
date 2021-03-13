package com.hsp.mms

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.SyncStateContract
import android.util.Log
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
        btn_open.setOnClickListener {
            openAlbum(this, ArrayList())
        }
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
    }

    override fun onItemClickAddFromAlbum() {
        openAlbum(this, photo_view.getData())
    }

    override fun onItemClickDelete(position: Int) {
    }
}