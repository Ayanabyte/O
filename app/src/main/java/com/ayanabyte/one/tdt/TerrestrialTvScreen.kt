package com.ayanabyte.one.tdt

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.ayanabyte.one.R
import com.ayanabyte.one.ui.FocusButton
import kotlinx.coroutines.flow.first

private val Sky = Color(0xFF4DDCF5)
private val Grey = Color(0xFFB0B0B0)

@Composable
internal fun TerrestrialTvScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    var permitted by remember { mutableStateOf(context.canReadListings()) }
    var permissionAsked by rememberSaveable { mutableStateOf(false) }
    var usePermissionSettings by rememberSaveable { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var lastChannelId by rememberSaveable { mutableStateOf<Long?>(null) }
    var playingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permitted = granted
        usePermissionSettings = !granted &&
            activity?.shouldShowRequestPermissionRationale(TvListingsPermission) == false
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permitted = context.canReadListings()
    }
    LaunchedEffect(Unit) {
        if (!permitted && !permissionAsked) {
            permissionAsked = true
            requestPermission.launch(TvListingsPermission)
        }
    }
    val catalog by produceState<ChannelCatalog>(ChannelCatalog.Loading, permitted, refresh) {
        value = if (permitted) {
            value = ChannelCatalog.Loading
            readTerrestrialChannels(context.applicationContext)
        } else {
            ChannelCatalog.NeedsPermission
        }
    }

    BackHandler {
        if (playingId != null) playingId = null else onBack()
    }

    when (val result = catalog) {
        is ChannelCatalog.Available -> {
            val channels = result.channels
            val current = channels.indexOfFirst { it.id == playingId }
            if (current >= 0) {
                val channel = channels[current]
                TerrestrialPlayer(channel) { step ->
                    channels.getOrNull(current + step)?.takeIf { it.radio == channel.radio }?.let {
                        playingId = it.id
                        lastChannelId = it.id
                    }
                }
            } else if (channels.isNotEmpty()) {
                ChannelList(channels, lastChannelId) {
                    lastChannelId = it.id
                    playingId = it.id
                }
            } else {
                CatalogMessage(R.string.tdt_no_channels, R.string.tdt_retry) { refresh++ }
            }
        }
        ChannelCatalog.Loading -> CatalogMessage(R.string.tdt_loading)
        ChannelCatalog.NeedsPermission -> when {
            permitted -> CatalogMessage(R.string.tdt_permission_restricted, R.string.tdt_retry) { refresh++ }
            usePermissionSettings -> CatalogMessage(R.string.tdt_permission, R.string.tdt_permission_settings) {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                })
            }
            else -> CatalogMessage(R.string.tdt_permission, R.string.tdt_allow) {
                permissionAsked = true
                requestPermission.launch(TvListingsPermission)
            }
        }
        ChannelCatalog.NoTuner -> CatalogMessage(R.string.tdt_no_tuner, R.string.tdt_retry) { refresh++ }
        ChannelCatalog.Unavailable -> CatalogMessage(R.string.tdt_catalog_error, R.string.tdt_retry) { refresh++ }
    }
}

private fun Context.canReadListings(): Boolean =
    checkSelfPermission(TvListingsPermission) == PackageManager.PERMISSION_GRANTED

@Composable
private fun CatalogMessage(message: Int, action: Int? = null, onAction: () -> Unit = {}) {
    val focus = remember { FocusRequester() }
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(horizontal = 54.dp, vertical = 34.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(stringResource(R.string.tdt_title), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
        Text(stringResource(message), color = Grey, fontSize = 20.sp)
        if (action != null) {
            FocusButton(stringResource(action), Modifier.focusRequester(focus), onAction)
            LaunchedEffect(action) { focus.requestFocus() }
        }
    }
}

@Composable
private fun ChannelList(
    channels: List<TerrestrialChannel>,
    lastChannelId: Long?,
    onSelect: (TerrestrialChannel) -> Unit
) {
    val restoreIndex = channels.indexOfFirst { it.id == lastChannelId }.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = restoreIndex)
    val restoreFocus = remember { FocusRequester() }

    Column(Modifier.fillMaxSize().background(Color.Black).padding(horizontal = 54.dp, vertical = 28.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.tdt_title), Modifier.weight(1f), color = Color.White, fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold)
            val radioCount = channels.count { it.radio }
            Text(stringResource(R.string.tdt_count, channels.size - radioCount, radioCount), color = Grey, fontSize = 16.sp)
        }
        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(channels, key = { _, channel -> channel.id }) { index, channel ->
                if (index == 0 || channels[index - 1].radio != channel.radio) {
                    Text(
                        stringResource(if (channel.radio) R.string.tdt_radio else R.string.tdt_television),
                        Modifier.padding(top = 8.dp, bottom = 8.dp), color = Grey, fontSize = 17.sp
                    )
                }
                val row = if (index == restoreIndex) Modifier.focusRequester(restoreFocus) else Modifier
                ChannelRow(channel, row) { onSelect(channel) }
            }
        }
    }
    LaunchedEffect(restoreIndex) {
        listState.scrollToItem(restoreIndex)
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.any { it.index == restoreIndex } }.first { it }
        restoreFocus.requestFocus()
    }
}

@Composable
private fun ChannelRow(channel: TerrestrialChannel, modifier: Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    val logo by produceState<Bitmap?>(null, channel.id) { value = readChannelLogo(context, channel.id) }
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }.clip(shape)
            .background(if (focused) Color(0xFF1B3038) else Color(0xFF151515))
            .border(2.dp, if (focused) Sky else Color.Transparent, shape)
            .clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(channel.number.orEmpty(), Modifier.width(52.dp), color = if (focused) Sky else Grey, fontSize = 18.sp)
        logo?.let { Image(it.asImageBitmap(), contentDescription = null, modifier = Modifier.size(38.dp, 28.dp)) }
        Text(channel.name ?: stringResource(R.string.tdt_unnamed), color = Color.White, fontSize = 20.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
