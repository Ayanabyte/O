#!/usr/bin/env python3
"""Prepare an isolated ORB reference test; never modifies the O! main app.

ORB source: https://github.com/OpenRedButtonProject/orb
License: Apache-2.0 (upstream); test files in this directory are original.
"""
from pathlib import Path
import shutil
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: prepare.py <checked-out-orb-directory>")
orb = Path(sys.argv[1]).resolve()
here = Path(__file__).resolve().parent

def patch(relative: str, old: str, new: str) -> None:
    path = orb / relative
    original = path.read_text(encoding="utf-8")
    if original.count(old) != 1:
        raise RuntimeError(f"Unexpected ORB upstream contents: {relative} / {old!r}")
    path.write_text(original.replace(old, new, 1), encoding="utf-8")

# ORB's historical Android Gradle build still uses JCenter.
gradle = orb / "android/build.gradle"
gradle_source = gradle.read_text(encoding="utf-8")
if gradle_source.count("jcenter()") != 2:
    raise RuntimeError("Expected exactly two JCenter repositories in ORB upstream build")
gradle.write_text(gradle_source.replace("jcenter()", "mavenCentral()"), encoding="utf-8")

# Current ORB main uses MediaSwitcher in JNI but does not yet add its
# source/include directory to the historical Android CMake target.
patch(
    "android/orblibrary/CMakeLists.txt",
    "   ../../components/network_services/media_synchroniser\n",
    "   ../../components/network_services/media_synchroniser\n"
    "   ../../components/network_services/media_switcher\n",
)
patch(
    "android/orblibrary/CMakeLists.txt",
    '    "../../components/network_services/app2app/app2app_remote_service.cpp"',
    '    "../../components/network_services/app2app/app2app_remote_service.cpp"\n'
    '    "../../components/network_services/media_switcher/media_switcher.cpp"',
)

# OrbCronet.java (added upstream) imports org.chromium.net but the legacy Android
# Gradle module omits its Maven dependencies. Pin compatible published binaries.
patch(
    "android/orblibrary/build.gradle",
    "    implementation 'com.squareup.okhttp3:okhttp:3.14.9'",
    "    implementation 'com.squareup.okhttp3:okhttp:3.14.9'\n"
    "    implementation 'org.chromium.net:cronet-api:119.6045.31'\n"
    "    implementation 'org.chromium.net:cronet-embedded:119.6045.31'",
)

# AndroidX Core from transitive dependencies uses android:attr/lStar (API 31).
# The historical modules target compileSdk 28/31; AndroidX now requires API 33.
for module in ("orblibrary", "orbpolyfill"):
    patch(
        f"android/{module}/build.gradle",
        "    compileSdkVersion 28",
        "    compileSdkVersion 33",
    )

patch(
    "android/mock203app/build.gradle",
    "    compileSdkVersion 31",
    "    compileSdkVersion 33",
)

# Current ORB adds required decoder-capabilities callbacks that its old 2.0.3
# mock application did not implement. The isolated sample advertises no decoder
# until we can query real playback capabilities on the test hardware.
callback_file = orb / "android/mock203app/src/main/java/org/orbtv/mock203app/MockOrbSessionCallback.java"
callback_src = callback_file.read_text(encoding="utf-8")
callback_at = "    @Override\n    public BridgeTypes.SystemInformation getSystemInformation() {"
if callback_src.count(callback_at) != 1:
    raise RuntimeError("Missing expected ORB mock callback insertion point")
callback_src = callback_src.replace(
    callback_at,
    "    @Override\n"
    "    public java.util.List<String> getBroadbandCapabilities(int decoderIndex) {\n"
    "        return null; // No real broadband decoder in mock harness\n"
    "    }\n\n"
    "    @Override\n"
    "    public java.util.List<String> getBroadcastCapabilities(int decoderIndex) {\n"
    "        return new java.util.ArrayList<>(); // Mock broadcast decoder not wired\n"
    "    }\n\n" + callback_at,
    1,
)
callback_file.write_text(callback_src, encoding="utf-8")

# Make the upstream mock tester a visible Android TV app and keep its package
# org.orbtv.mock203app, entirely separate from com.ayanabyte.one.
patch(
    "android/mock203app/src/main/AndroidManifest.xml",
    '<category android:name="android.intent.category.LAUNCHER" />',
    '<category android:name="android.intent.category.LAUNCHER" />\n'
    '                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />',
)
patch(
    "android/mock203app/src/main/AndroidManifest.xml",
    'android:label="@string/app_name"',
    'android:label="O! HbbTV ORB LAB"',
)

callback = "android/mock203app/src/main/java/org/orbtv/mock203app/MockOrbSessionCallback.java"
patch(callback, 'extras.getString("testsuite_manifest", "")',
      'extras.getString("testsuite_manifest", "o-smoke/manifest.json")')
patch(callback, 'mManifest = "";', 'mManifest = "o-smoke/manifest.json";')
patch(callback, 'mSession = session;\n        mTestSuiteRunner.run();',
      'mSession = session;\n        Log.i("O_ORB_POC", "SESSION_READY");\n'
      '        mTestSuiteRunner.run();')

target = orb / "tests/o-smoke"
target.mkdir(parents=True, exist_ok=True)
for name in ("manifest.json", "scenario.json", "index.html"):
    shutil.copy2(here / name, target / name)


# Visible on-screen boot diagnostics: a plain grey background was observed on
# TCL Android TV, so the test must show progress before ORB renders any HTML.
# Keep the native overlay visible and behind no WebView; do not mask failures.
main = "android/mock203app/src/main/java/org/orbtv/mock203app/MainActivity.java"
patch(main,
    "    private IOrbSession mTvBrowserSession = null;",
    "    private IOrbSession mTvBrowserSession = null;\n"
    "    private android.widget.TextView mLabStatus;\n")
patch(main,
    "        setContentView(R.layout.activity_main);\n        Bundle extras = getIntent().getExtras();",
    "        setContentView(R.layout.activity_main);\n"
    "        FrameLayout frameLayout = findViewById(R.id.frameLayout);\n"
    "        mLabStatus = new android.widget.TextView(this);\n"
    "        mLabStatus.setTextColor(android.graphics.Color.WHITE);\n"
    "        mLabStatus.setTextSize(18);\n"
    "        mLabStatus.setPadding(22, 14, 22, 14);\n"
    "        mLabStatus.setBackgroundColor(0xDD111111);\n"
    "        mLabStatus.setClickable(false);\n"
    "        mLabStatus.setFocusable(false);\n"
    "        FrameLayout.LayoutParams labParams = new FrameLayout.LayoutParams(\n"
    "                FrameLayout.LayoutParams.WRAP_CONTENT,\n"
    "                FrameLayout.LayoutParams.WRAP_CONTENT,\n"
    "                android.view.Gravity.TOP | android.view.Gravity.LEFT);\n"
    "        labParams.setMargins(24, 24, 24, 24);\n"
    "        frameLayout.addView(mLabStatus, labParams);\n"
    "        setLabStatus(\"01 Iniciando laboratorio...\");\n"
    "        Bundle extras = getIntent().getExtras();")
patch(main,
    "        FrameLayout frameLayout = findViewById(R.id.frameLayout);\n        mTvBrowserSession = OrbSessionFactory.createSession(",
    "        setLabStatus(\"03 Inicializando motor ORB...\");\n"
    "        mTvBrowserSession = OrbSessionFactory.createSession(")
patch(main,
    "            e.printStackTrace();\n            Log.d(\"orb_automation_msg\", \"finished\");",
    "            Log.e(\"O_ORB_POC\", \"ERROR preparando tests\", e);\n"
    "            setLabStatus(\"ERROR preparando pruebas: \" + e.getClass().getSimpleName() + \": \" + e.getMessage());\n"
    "            Log.d(\"orb_automation_msg\", \"finished\");")
patch(main,
    "        frameLayout.addView(mTvBrowserSession.getView());",
    "        frameLayout.addView(mTvBrowserSession.getView(), 0);")
patch(main,
    "    @Override\n    protected void onDestroy() {",
    "    public void setLabStatus(final String status) {\n"
    "        Log.i(\"O_ORB_POC\", status);\n"
    "        runOnUiThread(() -> {\n"
    "            if (mLabStatus != null) mLabStatus.setText(\"O! HbbTV ORB LAB\\n\" + status);\n"
    "        });\n"
    "    }\n\n"
    "    @Override\n    protected void onDestroy() {")

callback = "android/mock203app/src/main/java/org/orbtv/mock203app/MockOrbSessionCallback.java"
patch(callback,
    "mServer = new MockHttpServer(mContext);",
    "mServer = new MockHttpServer(mMainActivity);\n"
    "        mMainActivity.setLabStatus(\"02 Servidor HTTP local preparado\");")
patch(callback,
    "                        mTestSuiteScenario = scenario;\n                        setDsmccData(dsmccData);",
    "                        mTestSuiteScenario = scenario;\n"
    "                        mMainActivity.setLabStatus(\"05 Escenario local cargado; simulando sintonización\");\n"
    "                        setDsmccData(dsmccData);")
patch(callback,
    "        mSession = session;\n        Log.i(\"O_ORB_POC\", \"SESSION_READY\");\n        mTestSuiteRunner.run();",
    "        mSession = session;\n"
    "        Log.i(\"O_ORB_POC\", \"SESSION_READY\");\n"
    "        mMainActivity.setLabStatus(\"04 SESSION_READY: ORB iniciado\");\n"
    "        mTestSuiteRunner.run();")
patch(callback,
    "                mSession.processAitSection(506, channel.sid, mTestSuiteScenario.getCurrentChannelAit());",
    "                mMainActivity.setLabStatus(\"06 Enviando AIT simulada a ORB...\");\n"
    "                mSession.processAitSection(506, channel.sid, mTestSuiteScenario.getCurrentChannelAit());")

# HbbTV expects the application/xhtml MIME type: the original mock server
# mapped .html to text/html, which can prevent special HbbTV document handling.
http = "android/mock203app/src/main/java/org/orbtv/mock203app/MockHttpServer.java"
patch(http,
    "                Log.d(TAG, \"Get path: \" + path);\n                okio.Buffer body = getAsset(path);",
    "                Log.d(TAG, \"Get path: \" + path);\n"
    "                if (path != null && path.startsWith(\"/o-smoke/ready\")) {\n"
    "                    Log.i(\"O_ORB_POC\", \"DOCUMENT_LOADED_FROM_WEB\");\n"
    "                    if (mContext instanceof MainActivity) {\n"
    "                        ((MainActivity) mContext).setLabStatus(\"08 HbbTV ejecutado: JavaScript OK\");\n"
    "                    }\n"
    "                    return new MockResponse().setResponseCode(204);\n"
    "                }\n"
    "                if (path != null && path.startsWith(\"/o-smoke/index.html\")) {\n"
    "                    Log.i(\"O_ORB_POC\", \"HBBTV_DOCUMENT_REQUESTED\");\n"
    "                    if (mContext instanceof MainActivity) {\n"
    "                        ((MainActivity) mContext).setLabStatus(\"07 Página HbbTV solicitada por ORB\");\n"
    "                    }\n"
    "                }\n"
    "                okio.Buffer body = getAsset(path);")
patch(http,
    "    private String getMimeTypeFromUrl(String url) {\n        String type = \"*/*\";",
    "    private String getMimeTypeFromUrl(String url) {\n"
    "        if (url.startsWith(\"/o-smoke/\") && url.endsWith(\".html\")) {\n"
    "            return \"application/vnd.hbbtv.xhtml+xml; charset=UTF-8\";\n"
    "        }\n"
    "        String type = \"*/*\";")


# The first TCL test reached JavaScript but showed only grey. The second stage
# isolates actual WebView visibility/paint rather than claiming HbbTV is usable.
# Let the window's normal hardware-accelerated renderer compose the WebView.
orb_session = orb / "android/orblibrary/src/main/java/org/orbtv/orblibrary/OrbSession.java"
orb_src = orb_session.read_text(encoding="utf-8")
old_layer = "mBrowserView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);"
if orb_src.count(old_layer) != 2:
    raise RuntimeError("Expected two software WebView layer declarations")
orb_session.write_text(
    orb_src.replace(old_layer, "mBrowserView.setLayerType(View.LAYER_TYPE_NONE, null);"),
    encoding="utf-8",
)

browser = "android/orblibrary/src/main/java/org/orbtv/orblibrary/BrowserView.java"
patch(
    browser,
    "        setBackgroundColor(Color.TRANSPARENT);",
    "        setBackgroundColor(Color.BLACK);",
)
patch(
    browser,
    "            @Override\n            public void onPageCommitVisible(WebView view, String url) {",
    "            @Override\n"
    "            public void onPageFinished(WebView view, String url) {\n"
    "                super.onPageFinished(view, url);\n"
    "                Log.i(\"O_ORB_POC\", \"WEBVIEW_PAGE_FINISHED url=\" + url);\n"
    "                // Some stock Android TV WebViews do not invoke onPageCommitVisible\n"
    "                // when the WebView began life INVISIBLE. Unblock first paint.\n"
    "                if (url != null && !url.startsWith(\"about:blank\") && !mVisibilityOverride) {\n"
    "                    Log.i(\"O_ORB_POC\", \"VISIBILITY_FALLBACK_ON_PAGE_FINISHED\");\n"
    "                    mVisibilityOverride = true;\n"
    "                    setHiddenFlag(mHiddenMask);\n"
    "                }\n"
    "            }\n\n"
    "            @Override\n"
    "            public void onPageCommitVisible(WebView view, String url) {\n"
    "                Log.i(\"O_ORB_POC\", \"WEBVIEW_PAGE_COMMIT_VISIBLE url=\" + url);",
)

# Native 09 status confirms whether the actual ORB WebView became visible,
# separate from the onload/JS 08 status; no screenshots, permissions or uploads.
patch(
    main,
    "    @Override\n    protected void onDestroy() {",
    "    public void inspectBrowserVisibility() {\n"
    "        runOnUiThread(() -> new android.os.Handler(android.os.Looper.getMainLooper())\n"
    "            .postDelayed(() -> {\n"
    "                FrameLayout frame = findViewById(R.id.frameLayout);\n"
    "                android.view.View root = frame.getChildAt(0);\n"
    "                android.view.View browser = root;\n"
    "                if (root instanceof android.view.ViewGroup) {\n"
    "                    browser = ((android.view.ViewGroup) root).getChildAt(0);\n"
    "                }\n"
    "                if (browser != null) {\n"
    "                    setLabStatus(\"09 WebView \" + (browser.getVisibility() == 0 ? \"VISIBLE\" : \"OCULTO\")\n"
    "                        + \", shown=\" + browser.isShown() + \", \"\n"
    "                        + browser.getWidth() + \"x\" + browser.getHeight());\n"
    "                } else {\n"
    "                    setLabStatus(\"09 WebView no encontrado\");\n"
    "                }\n"
    "            }, 1600));\n"
    "    }\n\n"
    "    @Override\n    protected void onDestroy() {",
)
patch(
    http,
    "                        ((MainActivity) mContext).setLabStatus(\"08 HbbTV ejecutado: JavaScript OK\");",
    "                        ((MainActivity) mContext).setLabStatus(\"08 HbbTV ejecutado: JavaScript OK\");\n"
    "                        ((MainActivity) mContext).inspectBrowserVisibility();",
)


# Keep the ORB visibility mask authoritative. The local HbbTV document calls
# Application.show(), as a broadcast-related application must, instead of
# bypassing the engine's visibility state.
patch(
    main,
    "        FrameLayout frameLayout = findViewById(R.id.frameLayout);\n"
    "        mLabStatus = new android.widget.TextView(this);",
    "        FrameLayout frameLayout = findViewById(R.id.frameLayout);\n"
    "        frameLayout.setBackgroundColor(0xFF000000); // Black fallback for lab\n"
    "        mLabStatus = new android.widget.TextView(this);",
)

print("ORB smoke test prepared; main O! application is not involved.")
