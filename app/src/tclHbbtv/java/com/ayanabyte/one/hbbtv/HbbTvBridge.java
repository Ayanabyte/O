package com.ayanabyte.one.hbbtv;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.media.tv.TvView;
import android.view.KeyEvent;
import android.view.View;
import tv.own.owntv.features.hbbtv.HbbTvPlayerView;

/** TCL-only experiment: embed the real vendor Tbrowser over the EXISTING tuner. */
public final class HbbTvBridge {
    private final HbbTvPlayerView player;
    private final Activity activity;
    private final Context context;
    public final TvView tvView;
    public final View view;

    public HbbTvBridge(Context context) {
        this.context = context;
        player = new HbbTvPlayerView(context);
        activity = activityOf(context);
        tvView = player.getTvView();
        tvView.setFocusable(false);
        tvView.setFocusableInTouchMode(false);
        view = player;
    }

    public void update(String inputId, long id, boolean ready, long generation, boolean active) {
        player.updateHbbTv(inputId, id, ready, generation, active);
    }
    public boolean handleKey(KeyEvent event) {
        return activity != null && HbbTvPlayerView.dispatch(activity, event);
    }
    public String diagnostics() {
        return player.oneDiagnostic() +
            "\nnetStatePermission=" +
            (context.checkSelfPermission("android.permission.ACCESS_NETWORK_STATE")
                == PackageManager.PERMISSION_GRANTED) +
            " internetPermission=" +
            (context.checkSelfPermission("android.permission.INTERNET")
                == PackageManager.PERMISSION_GRANTED);
    }
    public void suspend() { player.updateHbbTv(null, -1L, false, -1L, false); }
    public void release() { player.release(); }

    private static Activity activityOf(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return (Activity) context;
            Context base = ((ContextWrapper) context).getBaseContext();
            if (base == context) break;
            context = base;
        }
        return null;
    }
}
