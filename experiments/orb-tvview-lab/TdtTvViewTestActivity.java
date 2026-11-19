/*
 * Independent TCL DVB-T / Android TV Input Framework smoke test for ORB mock203app.
 * No system permissions, hidden APIs, TCL internals or HbbTV engines are used.
 */
package org.orbtv.mock203app;

import android.app.Activity;
import android.graphics.Color;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvView;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Starts a separate TV Input Framework session, independent of ORB's browser.
 * Launch with: adb shell am start -n org.orbtv.mock203app/.TdtTvViewTestActivity
 */
public final class TdtTvViewTestActivity extends Activity {
    private static final String TAG = "O_TDT_TVVIEW";
    private static final String INPUT_ID =
            "com.tcl.tvinput/.TDTVInputService/HW1413742848";
    private static final Uri CHANNEL_URI =
            Uri.parse("content://android.media.tv/channel/10032330");

    private TvView tvView;
    private TextView statusView;
    private boolean resumed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        tvView = new TvView(this);
        root.addView(tvView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        statusView = new TextView(this);
        statusView.setTextColor(Color.WHITE);
        statusView.setBackgroundColor(Color.argb(210, 0, 0, 0));
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        int pad = (int) (getResources().getDisplayMetrics().density * 18);
        statusView.setPadding(pad, pad, pad, pad);
        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP);
        root.addView(statusView, statusParams);
        setContentView(root);

        tvView.setCallback(new TvView.TvInputCallback() {
            @Override
            public void onConnectionFailed(String inputId) {
                report("onConnectionFailed input=" + inputId);
            }

            @Override
            public void onDisconnected(String inputId) {
                report("onDisconnected input=" + inputId);
            }

            @Override
            public void onVideoAvailable(String inputId) {
                report("onVideoAvailable input=" + inputId + " (VIDEO DISPONIBLE)");
            }

            @Override
            public void onVideoUnavailable(String inputId, int reason) {
                report("onVideoUnavailable input=" + inputId + " reason=" + reason);
            }

            @Override
            public void onTuned(String inputId, Uri channelUri) {
                report("onTuned input=" + inputId + " uri=" + channelUri);
            }

            @Override
            public void onChannelRetuned(String inputId, Uri channelUri) {
                report("onChannelRetuned input=" + inputId + " uri=" + channelUri);
            }
        });
        report("Pantalla de prueba TIF creada; todavía sin sintonizar");
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        tvView.post(this::tuneLa1);
    }

    @Override
    protected void onPause() {
        resumed = false;
        // Release our own TvView session. Never touch com.tcl.tv or the tuner settings.
        tvView.reset();
        Log.i(TAG, "onPause: TvView.reset() - session released");
        super.onPause();
    }

    private void tuneLa1() {
        if (!resumed || isFinishing()) {
            return;
        }
        report("Solicitando La 1 HD mediante TvView.tune()...");
        try {
            TvInputManager manager = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
            if (manager == null) {
                report("ERROR: TvInputManager no disponible");
                return;
            }
            TvInputInfo input = manager.getTvInputInfo(INPUT_ID);
            if (input == null) {
                report("ERROR: no se encuentra el TvInputInfo de TCL: " + INPUT_ID);
                return;
            }
            Log.i(TAG, "TvInputInfo found: " + input.getId() + ", state="
                    + manager.getInputState(INPUT_ID));
            tvView.tune(INPUT_ID, CHANNEL_URI);
            Log.i(TAG, "tune() returned; waiting for asynchronous TvInputCallback");
        } catch (SecurityException e) {
            report("SecurityException: TCL/Android bloquea el acceso TIF: " + e);
        } catch (RuntimeException e) {
            report("ERROR TvView.tune: " + e);
        }
    }

    private void report(final String message) {
        Log.i(TAG, message);
        runOnUiThread(() -> {
            if (statusView != null) {
                statusView.setText("O! LAB · PRUEBA TDT TCL\n" + message
                        + "\nLa 1 HD · Canal ID 10032330"
                        + "\nOK = reintentar · AZUL = verificar tecla · ATRÁS = salir");
            }
        });
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_PROG_BLUE) {
                report("KEYCODE_PROG_BLUE (186) recibido; HbbTV no se inicia en esta prueba");
                return true;
            }
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER
                    || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                tvView.reset();
                tvView.post(this::tuneLa1);
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
