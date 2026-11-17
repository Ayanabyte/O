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
import androidx.compose.runtime.Composable
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
import com.ayanabyte.one.R
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
    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(channel.id, bannerRequest) {
        bannerVisible = true
        delay(2500)
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
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        if (pressed) {
                            if (reception.retry) attempt++ else bannerRequest++
                        }
                        true
                    }
                    else -> false
                }
            }.focusable()
    ) {
        key(channel.id, attempt) {
            TunerView(channel) { reception = it }
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
private fun TunerView(channel: TerrestrialChannel, onReception: (Reception) -> Unit) {
    val context = LocalContext.current
    val tvView = remember(context) {
        TvView(context).apply {
            isFocusable = false
            isFocusableInTouchMode = false
        }
    }
    AndroidView(factory = { tvView }, modifier = Modifier.fillMaxSize())
    LifecycleResumeEffect(tvView) {
        var active = true
        fun report(inputId: String, reception: Reception) {
            if (active && inputId == channel.inputId) onReception(reception)
        }

        onReception(Reception.Tuning)
        tvView.setCallback(object : TvView.TvInputCallback() {
            override fun onVideoAvailable(inputId: String) {
                Log.i(TdtLog, "Video available for ${channel.id} (${channel.number} ${channel.name})")
                report(inputId, Reception.Video)
            }

            override fun onVideoUnavailable(inputId: String, reason: Int) {
                Log.i(TdtLog, "Video unavailable for ${channel.id}, reason=$reason")
                val reception = when (reason) {
                    TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING,
                    TvInputManager.VIDEO_UNAVAILABLE_REASON_BUFFERING -> Reception.Tuning
                    TvInputManager.VIDEO_UNAVAILABLE_REASON_WEAK_SIGNAL -> Reception.Signal
                    TvInputManager.VIDEO_UNAVAILABLE_REASON_NOT_CONNECTED -> Reception.Disconnected
                    TvInputManager.VIDEO_UNAVAILABLE_REASON_INSUFFICIENT_RESOURCE -> Reception.Busy
                    TvInputManager.VIDEO_UNAVAILABLE_REASON_AUDIO_ONLY -> Reception.Radio
                    else -> Reception.Failed
                }
                report(inputId, reception)
            }

            override fun onTrackSelected(inputId: String, type: Int, trackId: String?) {
                if (active && inputId == channel.inputId && type == TvTrackInfo.TYPE_AUDIO && trackId != null) {
                    Log.i(TdtLog, "Audio track selected for ${channel.id}")
                }
            }

            override fun onConnectionFailed(inputId: String) {
                Log.w(TdtLog, "Connection failed for $inputId")
                report(inputId, Reception.Failed)
            }

            override fun onDisconnected(inputId: String) = report(inputId, Reception.Disconnected)

            override fun onContentBlocked(inputId: String, rating: TvContentRating) =
                report(inputId, Reception.Blocked)
        })

        val manager = context.getSystemService(TvInputManager::class.java)
        try {
            if (manager?.getTvInputInfo(channel.inputId) == null ||
                manager.getInputState(channel.inputId) == TvInputManager.INPUT_STATE_DISCONNECTED
            ) {
                onReception(Reception.Disconnected)
            } else {
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
            Log.i(TdtLog, "Session reset for ${channel.id}")
        }
    }
}
