package com.ayanabyte.one.tdt

import android.media.tv.TvContentRating
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.media.tv.TvTrackInfo
import android.media.tv.TvView
import android.util.Log
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.ayanabyte.one.hbbtv.HbbTvBridge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.material3.Text
import com.ayanabyte.one.BuildConfig
import com.ayanabyte.one.R
import com.ayanabyte.one.ui.FocusButton
import kotlinx.coroutines.delay

private enum class Reception(val message: Int? = null, val retry: Boolean = false) {
    Tuning(R.string.tdt_tuning), Video, Radio,
    Signal(R.string.tdt_signal, true), Disconnected(R.string.tdt_disconnected, true),
    Busy(R.string.tdt_busy, true), Blocked(R.string.tdt_blocked),
    Denied(R.string.tdt_playback_permission, true), Failed(R.string.tdt_playback_error, true),
    Timeout(R.string.tdt_timeout, true)
}

@Composable
internal fun TerrestrialPlayer(channel: TerrestrialChannel, changeChannel: (Int) -> Unit) {
    val focus = remember { FocusRequester() }
    var attempt by remember(channel.id) { mutableIntStateOf(0) }
    var bannerRequest by remember(channel.id) { mutableIntStateOf(0) }
    var bannerVisible by remember(channel.id) { mutableStateOf(true) }
    var reception by remember(channel.id, attempt) { mutableStateOf(Reception.Tuning) }
    val context = LocalContext.current
    val hbbtv = remember(context, channel.id, attempt) { HbbTvBridge(context) }
    var hbbtvStatus by remember(hbbtv) { mutableStateOf("Inicializando HbbTV…") }
    if (BuildConfig.FLAVOR == "tclHbbtv" && !channel.radio) {
        LaunchedEffect(hbbtv) {
            while (true) {
                hbbtvStatus = hbbtv.diagnostics()
                delay(900)
            }
        }
    }
    val tvView = hbbtv.tvView
    fun showPlaybackControls() {
        bannerVisible = true
        bannerRequest++
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(channel.id, bannerRequest) {
        bannerVisible = true
        delay(8000)
        bannerVisible = false
    }
    LaunchedEffect(channel.id, attempt, reception) {
        if (reception == Reception.Tuning) {
            delay(12_000)
            reception = Reception.Timeout
        }
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black).focusRequester(focus)
            .onPreviewKeyEvent { event ->
                val native = event.nativeKeyEvent
                if (hbbtv.handleKey(native)) return@onPreviewKeyEvent true
                val pressed = native.action == KeyEvent.ACTION_DOWN && native.repeatCount == 0
                when (native.keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                        if (pressed) changeChannel(-1)
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_UP -> {
                        if (pressed) changeChannel(1)
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_LEFT -> false
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        if (pressed) {
                            when {
                                reception.retry -> attempt++
                                !bannerVisible -> showPlaybackControls()
                                else -> bannerRequest++
                            }
                        }
                        true
                    }
                    else -> false
                }
            }.focusable()
    ) {
        TunerView(channel, attempt, hbbtv) { reception = it }
        if (BuildConfig.FLAVOR == "tclHbbtv" && !channel.radio) {
            Column(Modifier.align(Alignment.TopEnd).padding(20.dp)
                .background(Color(0xDD000000)).padding(12.dp)) {
                Text("O! DIAG 3 · ERROR TCL DETALLADO", color = Color(0xFF4DDCF5), fontSize = 15.sp)
                Text(hbbtvStatus, color = Color.White, fontSize = 13.sp)
            }
        }
        if (bannerVisible || reception != Reception.Video) {
            val name = channel.name ?: stringResource(R.string.tdt_unnamed)
            val label = channel.number?.let { stringResource(R.string.tdt_channel_label, it, name) } ?: name
            Column(Modifier.align(Alignment.BottomStart).padding(40.dp)
                .background(Color(0xE6000000)).padding(horizontal = 20.dp, vertical = 14.dp)) {
                Text(label, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                val message = reception.message ?: if (reception == Reception.Radio) R.string.tdt_radio else null
                message?.let { Text(stringResource(it), color = Color(0xFFB0B0B0), fontSize = 18.sp) }
                if (reception.retry) {
                    Text(stringResource(R.string.tdt_player_retry), color = Color(0xFF4DDCF5), fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun TunerView(channel: TerrestrialChannel, attempt: Int, hbbtv: HbbTvBridge, onReception: (Reception) -> Unit) {
    val context = LocalContext.current
    val tvView = hbbtv.tvView
    AndroidView(factory = { hbbtv.view }, modifier = Modifier.fillMaxSize())
    DisposableEffect(hbbtv) { onDispose { hbbtv.release() } }
    LifecycleResumeEffect(hbbtv, channel.id, attempt) {
        var active = true
        var audioSelected = false
        var contentBlocked = false
        fun report(inputId: String, reception: Reception) {
            if (active && inputId == channel.inputId && (!contentBlocked || reception == Reception.Blocked)) {
                onReception(reception)
            }
        }

        onReception(Reception.Tuning)
        tvView.setCallback(object : TvView.TvInputCallback() {
            override fun onVideoAvailable(inputId: String) {
                Log.i(TdtLog, "Video available for ${channel.id} (${channel.number} ${channel.name})")
                if (!channel.radio) {
                    report(inputId, Reception.Video)
                    hbbtv.update(channel.inputId, channel.id, true, attempt.toLong(), true)
                }
            }

            override fun onVideoUnavailable(inputId: String, reason: Int) {
                Log.i(TdtLog, "Video unavailable for ${channel.id}, reason=$reason")
                hbbtv.update(channel.inputId, channel.id, false, attempt.toLong(), false)
                val reception = when (reason) {
                    TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING,
                    TvInputManager.VIDEO_UNAVAILABLE_REASON_BUFFERING -> Reception.Tuning
                    TvInputManager.VIDEO_UNAVAILABLE_REASON_WEAK_SIGNAL -> Reception.Signal
                    TvInputManager.VIDEO_UNAVAILABLE_REASON_NOT_CONNECTED -> Reception.Disconnected
                    TvInputManager.VIDEO_UNAVAILABLE_REASON_INSUFFICIENT_RESOURCE -> Reception.Busy
                    TvInputManager.VIDEO_UNAVAILABLE_REASON_AUDIO_ONLY -> Reception.Radio
                    TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN -> when {
                        channel.radio && audioSelected -> Reception.Radio
                        channel.radio -> Reception.Tuning
                        else -> Reception.Failed
                    }
                    else -> Reception.Failed
                }
                report(inputId, reception)
            }

            override fun onTrackSelected(inputId: String, type: Int, trackId: String?) {
                if (active && inputId == channel.inputId && type == TvTrackInfo.TYPE_AUDIO) {
                    audioSelected = trackId != null
                    if (audioSelected) {
                        Log.i(TdtLog, "Audio track selected for ${channel.id}")
                        if (channel.radio) report(inputId, Reception.Radio)
                    }
                }
            }

            override fun onConnectionFailed(inputId: String) {
                Log.w(TdtLog, "Connection failed for $inputId")
                report(inputId, Reception.Failed)
            }

            override fun onDisconnected(inputId: String) {
                contentBlocked = false
                report(inputId, Reception.Disconnected)
            }

            override fun onContentBlocked(inputId: String, rating: TvContentRating) {
                if (!active || inputId != channel.inputId) return
                contentBlocked = true
                report(inputId, Reception.Blocked)
            }

            override fun onContentAllowed(inputId: String) {
                if (!active || inputId != channel.inputId || !contentBlocked) return
                contentBlocked = false
                report(inputId, if (channel.radio && audioSelected) Reception.Radio else Reception.Tuning)
            }
        })

        val manager = context.getSystemService(TvInputManager::class.java)
        try {
            if (manager?.getTvInputInfo(channel.inputId) == null ||
                manager.getInputState(channel.inputId) == TvInputManager.INPUT_STATE_DISCONNECTED
            ) {
                onReception(Reception.Disconnected)
            } else {
                hbbtv.update(channel.inputId, channel.id, false, attempt.toLong(), !channel.radio)
                tvView.tune(channel.inputId, TvContract.buildChannelUri(channel.id))
                tvView.setStreamVolume(1f)
                Log.i(TdtLog, "Tune requested for ${channel.id} (${channel.number} ${channel.name})")
            }
        } catch (error: SecurityException) {
            Log.w(TdtLog, "TvView tune denied for ${channel.id}", error)
            onReception(Reception.Denied)
        } catch (error: RuntimeException) {
            Log.w(TdtLog, "TvView tune failed for ${channel.id}", error)
            onReception(Reception.Failed)
        }

        onPauseOrDispose {
            active = false
            tvView.setCallback(null)
            tvView.reset()
            hbbtv.suspend()
            Log.i(TdtLog, "Session reset for ${channel.id}")
        }
    }
}
