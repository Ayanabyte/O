package com.ayanabyte.one.tdt

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import java.lang.reflect.Proxy

/** A small adapter around the public TCL TV SDK shipped on compatible televisions. */
internal class TclInteractiveRuntime(
    private val activity: Activity,
    private val browserHost: ViewGroup,
    private val onStatus: (String?, Boolean) -> Unit
) {
    private var engine: Any? = null
    private var browser: Any? = null
    private var browserReady = false
    private var running = false
    private val keyValues = mutableMapOf<String, Int>()

    fun initialize() {
        if (engine != null || browser != null) return
        try {
            val engineClass = activity.classLoader.loadClass("com.tcl.hbbtv.HbbtvServer")
            val browserClass = activity.classLoader.loadClass("com.tcl.tbrowser.Tbrowser")
            engine = engineClass.getMethod("getInstance", Context::class.java).invoke(null, activity)
            val result = engine!!.javaClass.getMethod("init").invoke(engine) as? Int
            Log.i(TAG, "TCL HbbTV init result=$result")
            prepareKeyMap()

            browser = browserClass.getMethod("getInstance", Context::class.java).invoke(null, activity)
            val listenerClass = activity.classLoader.loadClass("com.tcl.tbrowser.TbrowserInitEventListener")
            val listener = Proxy.newProxyInstance(listenerClass.classLoader, arrayOf(listenerClass)) { _, method, args ->
                when (method.name) {
                    "onSuccess" -> activity.runOnUiThread {
                        browserReady = true
                        if (args?.firstOrNull() != true) fail("El navegador interactivo de TCL no pudo iniciarse")
                    }
                    "onFailure" -> activity.runOnUiThread { fail("TCL no pudo preparar el navegador interactivo") }
                }
                null
            }
            browser!!.javaClass.getMethod("setTbrowserInitEventListener", listenerClass).invoke(browser, listener)
            browser!!.javaClass.getMethod("initTbrowser", Context::class.java, ViewGroup::class.java, Boolean::class.javaPrimitiveType)
                .invoke(browser, activity, browserHost, false)
            onStatus("Preparando el servicio interactivo…", false)
        } catch (error: Throwable) {
            Log.e(TAG, "TCL interactive SDK initialization failed", error)
            fail("LOVEStv no está disponible en este televisor: ${error.cause?.message ?: error.message}")
        }
    }

    fun activate(): Boolean {
        if (!browserReady) {
            initialize()
            onStatus("El servicio interactivo se está preparando. Vuelve a pulsar Azul en unos segundos.", false)
            return false
        }
        if (!running) startEngine()
        if (!running) return false
        return dispatchKey("BLUE")
    }

    fun dispatchAndroidKey(androidKeyCode: Int): Boolean {
        val name = when (androidKeyCode) {
            19 -> "UP"
            20 -> "DOWN"
            21 -> "LEFT"
            22 -> "RIGHT"
            23, 66 -> "OK"
            4 -> "BACK"
            186 -> "BLUE"
            else -> return false
        }
        return dispatchKey(name)
    }

    fun isRunning(): Boolean = running

    fun stopInteractive() {
        runCatching { browser?.javaClass?.getMethod("destoryPage")?.invoke(browser) }
        runCatching { engine?.javaClass?.getMethod("closePortal")?.invoke(engine) }
        runCatching { engine?.javaClass?.getMethod("stopAsync")?.invoke(engine) }
        running = false
        onStatus(null, false)
    }

    fun close() {
        runCatching { browser?.javaClass?.getMethod("setVisible", String::class.java, Boolean::class.javaPrimitiveType)?.invoke(browser, "hbbtv", false) }
        runCatching { browser?.javaClass?.getMethod("destoryPage")?.invoke(browser) }
        runCatching { engine?.javaClass?.getMethod("closePortal")?.invoke(engine) }
        runCatching { engine?.javaClass?.getMethod("stopAsync")?.invoke(engine) }
        runCatching { browser?.javaClass?.getMethod("onPause")?.invoke(browser) }
        engine = null
        browser = null
        browserReady = false
        running = false
        keyValues.clear()
    }

    private fun startEngine() {
        if (running) return
        try {
            val result = engine?.javaClass?.getMethod("start")?.invoke(engine) as? Int
            // TCL's wrapper return value is not a reliable indicator of portal rendering.
            running = true
            Log.i(TAG, "TCL HbbTV start result=$result")
            onStatus(null, true)
        } catch (error: Throwable) {
            Log.e(TAG, "TCL HbbTV start failed", error)
            fail("No se pudo iniciar el servicio interactivo: ${error.cause?.message ?: error.message}")
        }
    }

    private fun dispatchKey(name: String): Boolean {
        if (!running) return false
        val value = keyValues[name] ?: return false
        return try {
            engine?.javaClass?.getMethod("handleKey", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                ?.invoke(engine, value, 0) as? Boolean ?: false
        } catch (error: Throwable) {
            Log.w(TAG, "TCL rejected key $name", error)
            false
        }
    }

    private fun prepareKeyMap() {
        val mapClass = activity.classLoader.loadClass("com.tcl.tvmanager.vo.TCLKeyMap\$EnTCLKeyMap")
        listOf("UP", "DOWN", "LEFT", "RIGHT", "OK", "BACK", "BLUE").forEach { name ->
            runCatching {
                val entry = mapClass.getField("EN_TCL_KEY_$name").get(null)
                keyValues[name] = (mapClass.getMethod("getValue").invoke(entry) as Number).toInt()
            }.onFailure { Log.w(TAG, "TCL key mapping unavailable: $name", it) }
        }
    }

    private fun fail(message: String) {
        Log.w(TAG, message)
        onStatus(message, false)
    }

    private companion object { const val TAG = "O-TdtInteractive" }
}

internal class TerrestrialSurface(context: Context) : FrameLayout(context) {
    val tvView = android.media.tv.TvView(context).apply {
        isFocusable = false
        isFocusableInTouchMode = false
        setZOrderMediaOverlay(true)
    }

    init {
        addView(tvView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }
}
