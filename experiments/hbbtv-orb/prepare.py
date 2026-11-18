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

print("ORB smoke test prepared; main O! application is not involved.")
