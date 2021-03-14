package com.hsp.mms

import android.content.Context
import android.graphics.Rect
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ScreenUtils
import com.blankj.utilcode.util.SizeUtils
import com.blankj.utilcode.util.TimeUtils
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.luck.picture.config.PictureMimeType
import com.luck.picture.entity.LocalMedia

/**
 * @Description: 自定义九宫格显示控件
 * @Author:   Hsp
 * @Email:    1101121039@qq.com
 * @CreateTime:     2020/11/8 15:54
 * @UpdateRemark:   更新说明：
 */
class NinePhotoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    RecyclerView(context, attrs, defStyleAttr) {

    /**
     * 最多可选数目
     */
    private var maxSelectCount: Int = 9

    /**
     * 横向排列列数
     */
    private var gridColumn: Int = 4

    /**
     * 是否显示添加项
     */
    private var showPlus: Boolean = false

    /**
     * 右上角边距
     */
    private var edgeMargin: Int = 18

    /**
     * 空白间距
     */
    private var whiteSpacing: Int = 24
    private var edgePadding: Int = 0

    private var itemHeight: Int = 0
    private lateinit var mAdapter: PictureAdapter

    init {
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.EquipmentNinePhotoView)
        maxSelectCount =
            typedArray.getInt(R.styleable.EquipmentNinePhotoView_np_max_select, 9)
        gridColumn =
            typedArray.getInt(R.styleable.EquipmentNinePhotoView_np_columns, 4)
        showPlus =
            typedArray.getBoolean(R.styleable.EquipmentNinePhotoView_np_show_plus, false)
        edgeMargin = typedArray.getDimensionPixelSize(
            R.styleable.EquipmentNinePhotoView_np_item_margin,
            SizeUtils.dp2px(6f)
        )
        whiteSpacing = typedArray.getDimensionPixelSize(
            R.styleable.EquipmentNinePhotoView_np_white_spacing,
            SizeUtils.dp2px(8f)
        )
        edgePadding = typedArray.getDimensionPixelSize(
            R.styleable.EquipmentNinePhotoView_np_edge_spacing,
            SizeUtils.dp2px(15f)
        )
        initRecyclerView()
    }

    private fun initRecyclerView() {
        // Item高度
        itemHeight =
            (ScreenUtils.getScreenWidth() - edgePadding * 2 - gridColumn * whiteSpacing) / gridColumn
        this.setPadding(edgePadding, 0, edgePadding, 0)
        this.clipToPadding = false
        this.overScrollMode = OVER_SCROLL_NEVER

        this.layoutManager = GridLayoutManager(context, gridColumn)
        this.addItemDecoration(NineGridDivider(whiteSpacing / 2))
        mAdapter = PictureAdapter()
        this.adapter = mAdapter
    }

    fun setList(list: List<LocalMedia>) {
        mAdapter.setList(list)
    }

    fun addList(list: List<LocalMedia>) {
        mAdapter.addData(list)
    }

    companion object {
        private const val NORMAL_ITEM_TYPE = 166
        private const val PLUS_ITEM_TYPE = 167
    }

    inner class PictureAdapter() :
        RecyclerView.Adapter<BaseViewHolder>() {
        private val data = ArrayList<LocalMedia>()

        override fun getItemViewType(position: Int): Int {
            return if (showPlus && position == data.size) {
                PLUS_ITEM_TYPE
            } else {
                NORMAL_ITEM_TYPE
            }
        }

        override fun getItemCount(): Int {
            return if (showPlus) {
                if (data.size < maxSelectCount) data.size + 1 else data.size
            } else {
                data.size
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
            return BaseViewHolder(
                LayoutInflater.from(context)
                    .inflate(R.layout.item_nine_photo_layout, null)
            )
        }

        override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
            val imageView = holder.getView<ImageView>(R.id.iv_image)
            val tvDuration = holder.getView<TextView>(R.id.tv_duration)

            val marginParam = imageView.layoutParams as ConstraintLayout.LayoutParams
            if (showPlus) { // 编辑状态
                marginParam.topMargin = edgeMargin
                marginParam.marginEnd = edgeMargin
                holder.setGone(R.id.iv_delete, isPlusPosition(position))
                holder.getView<View>(R.id.iv_delete).setOnClickListener {
                    removeAt(position)
                    mOnClickItemListener?.onItemClickDelete(position)
                }
            } else {
                holder.setGone(R.id.iv_delete, true)
            }
            marginParam.height = itemHeight
            imageView.layoutParams = marginParam

            if (holder.itemViewType == PLUS_ITEM_TYPE) { // 添加图例
                imageView.setImageResource(R.mipmap.img_add_picture)
            } else { // 普通图片
                if (position >= data.size) {
                    return
                }
                val localMedia = data[position]
                if (localMedia == null
                    || TextUtils.isEmpty(localMedia.path)
                ) {
                    return
                }
                val chooseModel: Int = localMedia.chooseModel
                val path: String = if (localMedia.isCut && !localMedia.isCompressed) {
                    // 裁剪过
                    localMedia.cutPath
                } else if (localMedia.isCompressed || localMedia.isCut && localMedia.isCompressed) {
                    // 压缩过,或者裁剪同时压缩过,以最终压缩过图片为准
                    localMedia.compressPath
                } else {
                    // 原图
                    localMedia.path
                }
                val isHttp = PictureMimeType.isHasHttp(path)
                if (isHttp) {
                    holder.setGone(R.id.tv_duration, true)
                    holder.setGone(
                        R.id.iv_mark_play,
                        !PictureMimeType.isHasVideo(localMedia.mimeType)
                    )
                } else {
                    holder.setGone(R.id.iv_mark_play, true)
                    val duration: Long = localMedia.duration
                    holder.setGone(
                        R.id.tv_duration,
                        !PictureMimeType.isHasVideo(localMedia.mimeType)
                    )
                    if (chooseModel == PictureMimeType.ofAudio()) {
                        holder.setGone(R.id.tv_duration, false)
                        tvDuration
                            .setCompoundDrawablesRelativeWithIntrinsicBounds(
                                R.drawable.picture_icon_audio,
                                0,
                                0,
                                0
                            )
                    } else {
                        tvDuration
                            .setCompoundDrawablesRelativeWithIntrinsicBounds(
                                R.drawable.picture_icon_video,
                                0,
                                0,
                                0
                            )
                    }
                    tvDuration.text = TimeUtils.millis2String(duration, "mm:ss")
                }

                if (chooseModel == PictureMimeType.ofAudio()) {
                    imageView.setImageResource(R.drawable.picture_audio_placeholder)
                } else {
                    Glide.with(context)
                        .load(path)
                        .into(imageView)
                }
            }

            imageView.setOnClickListener {
                if (isPlusPosition(position)) {
                    mOnClickItemListener?.onItemClickAddFromAlbum()
                } else {
                    mOnClickItemListener?.onItemClickPreview(position, getData())
                }
            }
        }

        /**
         * set 图片、视频数据
         */
        fun setList(list: List<LocalMedia>) {
            data.clear()
            data.addAll(list)
            notifyDataSetChanged()
        }

        /**
         * add 图片、视频数据
         */
        fun addData(list: List<LocalMedia>) {
            data.addAll(list)
            notifyDataSetChanged()
        }

        /**
         * 删除操作
         */
        private fun removeAt(position: Int) {
            if (position < data.size) {
                data.removeAt(position)
                notifyDataSetChanged()
            }
        }

        /**
         *获取所有数据
         */
        fun getData(): List<LocalMedia> {
            return data
        }
    }

    /**
     * 设置间距
     */
    inner class NineGridDivider(private val space: Int) :
        RecyclerView.ItemDecoration() {

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            outRect.left = space
            outRect.right = space
            outRect.top = space
            outRect.bottom = space
        }
    }

    /**
     * 判断是否是加号位置
     */
    fun isPlusPosition(position: Int): Boolean {
        return position == getData().size
    }

    /**
     * 设置最大选择数目
     */
    fun setMaxSelectCount(maxSelectCount: Int) {
        this.maxSelectCount = maxSelectCount
    }

    /**
     * 是否显示编辑状态下 添加选项
     */
    fun showPlus(show: Boolean) {
        this.showPlus = show
    }

    fun getData(): List<LocalMedia> {
        return mAdapter.getData() ?: ArrayList()
    }

    private var mOnClickItemListener: OnClickItemListener? = null

    fun addOnClickItemListener(listener: OnClickItemListener) {
        this.mOnClickItemListener = listener
    }

    interface OnClickItemListener {
        fun onItemClickPreview(position: Int, list: List<LocalMedia>)
        fun onItemClickAddFromAlbum()
        fun onItemClickDelete(position: Int)
    }
}


