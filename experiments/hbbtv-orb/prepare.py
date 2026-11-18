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
