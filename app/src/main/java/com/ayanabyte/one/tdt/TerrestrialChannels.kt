package com.ayanabyte.one.tdt

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.tv.TvContract
import android.media.tv.TvContract.Channels
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.provider.BaseColumns
import android.util.Log
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal const val TvListingsPermission = "android.permission.READ_TV_LISTINGS"
internal const val TdtLog = "OneTdt"

internal data class TerrestrialChannel(
    val id: Long,
    val inputId: String,
    val number: String?,
    val name: String?,
    val radio: Boolean
)

internal sealed interface ChannelCatalog {
    data object Loading : ChannelCatalog
    data object NeedsPermission : ChannelCatalog
    data object NoTuner : ChannelCatalog
    data object Unavailable : ChannelCatalog
    data class Available(val channels: List<TerrestrialChannel>) : ChannelCatalog
}

private val terrestrialTypes = setOf(
    Channels.TYPE_DVB_T, Channels.TYPE_DVB_T2, Channels.TYPE_ATSC_T, Channels.TYPE_ATSC3_T,
    Channels.TYPE_ISDB_T, Channels.TYPE_ISDB_TB, Channels.TYPE_DTMB, Channels.TYPE_1SEG
)

private val channelColumns = arrayOf(
    BaseColumns._ID, Channels.COLUMN_TYPE, Channels.COLUMN_SERVICE_TYPE,
    Channels.COLUMN_DISPLAY_NUMBER, Channels.COLUMN_DISPLAY_NAME, Channels.COLUMN_BROWSABLE
)

private const val TclTvInput = "com.tcl.tvinput"
private const val TclRadio = "EN_TCL_RADIO"
private const val TclAnalog = "EN_TCL_ATV"

internal suspend fun readTerrestrialChannels(context: Context): ChannelCatalog =
    withContext(Dispatchers.IO) {
        try {
            val manager = context.getSystemService(TvInputManager::class.java)
            val inputs = manager?.tvInputList.orEmpty().filter { input ->
                input.type == TvInputInfo.TYPE_TUNER && !input.isPassthroughInput
            }
            if (inputs.isEmpty()) return@withContext ChannelCatalog.NoTuner

            // TYPE_TUNER also includes software inputs. The broadcast type identifies TDT rows.
            val channels = buildList {
                for (input in inputs) {
                    val isTcl = input.serviceInfo.packageName == TclTvInput
                    val uri = TvContract.buildChannelsUriForInput(input.id)
                    val rows = context.contentResolver.query(uri, channelColumns, null, null, null)
                        ?: return@withContext ChannelCatalog.Unavailable
                    rows.use { cursor ->
                        while (cursor.moveToNext()) {
                            if (cursor.getString(1) !in terrestrialTypes || cursor.getInt(5) != 1) continue
                            val service = cursor.getString(2)
                            // This TCL provider labels analog rows as DVB-T and uses its own service types.
                            if (service == Channels.SERVICE_TYPE_OTHER || (isTcl && service == TclAnalog)) continue
                            add(TerrestrialChannel(
                                id = cursor.getLong(0),
                                inputId = input.id,
                                number = cursor.getString(3)?.takeIf(String::isNotBlank),
                                name = cursor.getString(4)?.takeIf(String::isNotBlank),
                                radio = service == Channels.SERVICE_TYPE_AUDIO || (isTcl && service == TclRadio)
                            ))
                        }
                    }
                }
            }.sortedWith(compareBy<TerrestrialChannel> { it.radio }
                .thenBy { it.number?.takeWhile(Char::isDigit)?.toIntOrNull() ?: Int.MAX_VALUE }
                .thenBy { it.number.orEmpty() }
                .thenBy { it.name.orEmpty() })

            Log.i(TdtLog, "Catalog: ${channels.count { !it.radio }} TV, ${channels.count { it.radio }} radio, " +
                "inputs=${channels.map { it.inputId }.distinct()}")
            ChannelCatalog.Available(channels)
        } catch (error: SecurityException) {
            Log.w(TdtLog, "TvContract query denied", error)
            ChannelCatalog.NeedsPermission
        } catch (error: RuntimeException) {
            Log.w(TdtLog, "TvContract catalog unavailable", error)
            ChannelCatalog.Unavailable
        }
    }

internal suspend fun readChannelLogo(context: Context, channelId: Long): Bitmap? =
    withContext(Dispatchers.IO) {
        val uri = TvContract.buildChannelLogoUri(channelId)
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }
