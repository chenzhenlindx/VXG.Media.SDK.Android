package com.hxct;


import android.content.Context;
import android.util.AttributeSet;

/**
 * Created by conlin on 2017/9/25.
 */

public class MediaPlayer extends veg.mediaplayer.sdk.MediaPlayer {
    public MediaPlayer(Context context, boolean is_window) {
        super(context, is_window);
    }

    public MediaPlayer(Context context) {
        super(context);
    }

    public MediaPlayer(Context context, AttributeSet attr) {
        super(context, attr);
    }

    @Override
    protected void AddLogoView() {
    }
    @Override
    protected void RemoveLogoView() {
    }
}
