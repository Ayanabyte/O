package com.ayanabyte.one.hbbtv;

import android.content.Context;
import android.media.tv.TvView;
import android.view.KeyEvent;
import android.view.View;

/** Stable Android TV engine: no vendor HbbTV and no external app handoff. */
public final class HbbTvBridge {
    public final TvView tvView;
    public final View view;
    public HbbTvBridge(Context context) {
        tvView = new TvView(context);
        tvView.setFocusable(false);
        tvView.setFocusableInTouchMode(false);
        tvView.setZOrderMediaOverlay(true);
        view = tvView;
    }
    public void update(String inputId, long id, boolean ready, long generation, boolean active) { }
    public boolean handleKey(KeyEvent event) { return false; }
    public void suspend() { }
    public void release() { }
}
