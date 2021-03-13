package com.luck.picture.listener;


/**
 * @author：luck
 * @date：2020-01-15 14:38
 * @describe：Custom video playback callback
 */
public interface OnVideoSelectedPlayCallback<T> {
    /**
     * Play the video
     *
     * @param data
     */
    void startPlayVideo(T data);
}
