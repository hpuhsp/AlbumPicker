package com.luck.picture.dialog;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.luck.picture.R;
import com.luck.picture.widget.NumberProgressBar;

/**
 * @Description:
 * @Author: Hsp
 * @Email: 1101121039@qq.com
 * @CreateTime: 2021/3/13 18:01
 * @UpdateRemark:
 */
public class VideoWorkProgressFragment extends DialogFragment {
    private static final String KEY_TITLE = "key_title";
    private View mContentView;
    private ImageView mIvStop;
    private NumberProgressBar mPbLoading;
    private TextView mTvTips;
    private int mProgress;
    private View.OnClickListener mListener;
    private boolean mCanCancel = false;


    public static VideoWorkProgressFragment newInstance(String title) {
        VideoWorkProgressFragment fragment = new VideoWorkProgressFragment();
        Bundle bundle = new Bundle();
        bundle.putString(KEY_TITLE, title);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(R.style.ConfirmDialogStyle, R.style.DialogFragmentStyle);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.layout_joiner_progress, null);
        mTvTips = mContentView.findViewById(R.id.joiner_tv_msg);
        Bundle bundle = getArguments();
        if (bundle != null) {
            String msg = bundle.getString(KEY_TITLE);
            if (!TextUtils.isEmpty(msg)) {
                mTvTips.setText(msg);
            }
        }
        mIvStop = mContentView.findViewById(R.id.joiner_iv_stop);
        mPbLoading = mContentView.findViewById(R.id.joiner_pb_loading);
        mPbLoading.setMax(100);
        mPbLoading.setProgress(mProgress);
        mIvStop.setOnClickListener(mListener);
        if (mCanCancel) {
            mIvStop.setVisibility(View.VISIBLE);
        } else {
            mIvStop.setVisibility(View.INVISIBLE);
        }
        return mContentView;
    }


    /**
     * 设置停止按钮的监听
     *
     * @param listener
     */
    public void setOnClickStopListener(View.OnClickListener listener) {
        mListener = listener;
        if (mIvStop != null) {
            mIvStop.setOnClickListener(listener);
        }
    }

    /**
     * 设置进度条
     *
     * @param progress
     */
    public void setProgress(int progress) {
        if (mPbLoading == null) {
            mProgress = progress;
            return;
        }
        mPbLoading.setProgress(progress);
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        //此处不使用用.show(...)的方式加载dialogfragment，避免IllegalStateException
        try {
            if (isAdded()) {
                dismiss();
            }
            manager.beginTransaction().add(this, tag).commitAllowingStateLoss();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                manager.beginTransaction().remove(this).add(this, tag).commitAllowingStateLoss();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    @Override
    public void dismiss() {
        // 和show对应
        if (getFragmentManager() != null && isAdded()) {
            getFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
        }
        if (mPbLoading != null) {
            mPbLoading.setProgress(0);
        }
    }

    public void setCanCancel(boolean canCancel) {
        mCanCancel = canCancel;
        if (mIvStop == null) {
        } else {
            if (canCancel) {
                mIvStop.setVisibility(View.VISIBLE);
            } else {
                mIvStop.setVisibility(View.INVISIBLE);
            }
        }
    }
}
