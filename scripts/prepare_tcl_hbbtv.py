#!/usr/bin/env python3
"""Rebuild the historical, embedded TCL HbbTV bridge in O!'s opt-in source set.

Input is the pinned public Channelplus v5.3.14 checkout. No vendor APK is
launched: this reconstructs its same-process Tbrowser + HbbtvServer integration
over O!'s existing TvView. The stock O! flavor is not modified.
"""
from __future__ import annotations

import hashlib
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LEGACY = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else None
if LEGACY is None or not (LEGACY / "tools/hbbtv/fix_5312_direct_activity.py").exists():
    raise SystemExit("Usage: python scripts/prepare_tcl_hbbtv.py <Channelplus-v5.3.14-checkout>")
archive = LEGACY / "ci/channelplus-5.3.7-overlay.zip"
expected = "fea649552eb21f7a5df958a20dbb1cf6915b186b701a346aca498be434b8173b"
if hashlib.sha256(archive.read_bytes()).hexdigest() != expected:
    raise SystemExit("Historical overlay SHA-256 mismatch; abort")
with zipfile.ZipFile(archive) as z:
    z.extractall(LEGACY)

SRC = LEGACY / "owntv_tdt/app"
r = SRC / "src/main/java/tv/own/channelplus/lovestvprobe/HbbTvRuntimeService.java"
def replace(original: str, replacement: str, label: str) -> None:
    text = r.read_text(encoding="utf-8")
    if text.count(original) != 1:
        raise RuntimeError("Expected exactly one historical match: " + label)
    r.write_text(text.replace(original, replacement), encoding="utf-8")

def run_old(name: str) -> None:
    subprocess.run([sys.executable, "tools/hbbtv/" + name], cwd=LEGACY, check=True)

# Repair the old remote and its startup watchdog before mounting the Activity browser.
run_old("fix_539_key_routing.py")
run_old("fix_539_reliability.py")

# TCL native HbbTV used a transparent, visible Activity-owned root during init.
replace("private Object engine,browser,initListener;",
        "private Object engine,browser,initListener,eventNotify,eventListener;", "event refs")
replace("root=new FrameLayout(windowContext); root.setBackgroundColor(Color.TRANSPARENT); root.setAlpha(0f); root.setFocusable(false);",
        "root=new FrameLayout(windowContext); root.setBackgroundColor(Color.TRANSPARENT); root.setAlpha(1f); root.setVisibility(View.VISIBLE); root.setFocusable(false); root.setClipChildren(false); root.setClipToPadding(false);",
        "visible Activity root")
replace("        initializing=true;\n        main.removeCallbacks(timeout);",
        "        initializing=true;\n        visibility();\n        main.removeCallbacks(timeout);",
        "native browser init")
replace("ExperimentalAuthorization.begin(this,authorized->main.post(()->{",
        "ExperimentalAuthorization.begin(windowContext!=null?windowContext:this,authorized->main.post(()->{",
        "authorization Activity context")
replace("                prepareKeys();\n                browser=singleton(\"com.tcl.tbrowser.Tbrowser\");",
        "                prepareKeys();\n                registerBroadcastEvents();\n                browser=singleton(\"com.tcl.tbrowser.Tbrowser\");",
        "broadcaster events")
replace("    private void prepareKeys() throws ReflectiveOperationException {",
        """    private void registerBroadcastEvents() {
        try {
            Class<?> type=Class.forName("com.tcl.hbbtv.HbbtvEventNotify");
            Class<?> listener=Class.forName("com.tcl.hbbtv.HbbtvEventListener");
            eventNotify=type.getMethod("getInstance",Context.class).invoke(null,windowContext);
            eventListener=Proxy.newProxyInstance(listener.getClassLoader(),new Class<?>[]{listener},
                (proxy,method,args)->{
                    if(method.getDeclaringClass()==Object.class) return objectMethod(proxy,method,args);
                    Log.i(TAG,"TCL broadcaster event="+method.getName()+" channel="+channelId);
                    return defaultValue(method.getReturnType());
                });
            type.getMethod("registerHbbtvEventListener",listener).invoke(eventNotify,eventListener);
        } catch(Exception | LinkageError error) {
            Log.w(TAG,"Broadcaster event listener unavailable",error);
        }
    }
    private void prepareKeys() throws ReflectiveOperationException {""",
        "event listener")
replace("private void visibility() { if(root!=null) root.setAlpha(running && ready && !hidden ? 1f : 0f); }",
        "private void visibility() { if(root!=null) { boolean show=ready && !hidden && (initializing || running); root.setAlpha(show ? 1f : 0f); if(show) root.bringToFront(); } }",
        "transparent root visibility")
replace("            visibility(); reply(STARTED,new Bundle()); inspect.run();",
        "            visibility(); Log.i(TAG,\"Native HbbTV autostart completed without pressing any key; awaiting broadcaster LOVEStv side hints\"); reply(STARTED,new Bundle()); inspect.run();",
        "startup report")

# Recreate the documented 5.3.12/13/14 integration, in the original order.
run_old("fix_5312_direct_activity.py")
run_old("fix_5313_playback_controls.py")
run_old("fix_5314_seek_live.py")

target = ROOT / "app/src/tclHbbtv"
copy_source = [
    "src/main/java/tv/own/owntv/features/hbbtv/HbbTvActivityHost.java",
    "src/main/java/tv/own/owntv/features/hbbtv/HbbTvInputGate.java",
    "src/main/java/tv/own/owntv/features/hbbtv/HbbTvProtocol.java",
    "src/main/java/tv/own/owntv/features/hbbtv/HbbTvPlayerView.java",
    "src/main/java/tv/own/channelplus/lovestvprobe/HbbTvRuntimeService.java",
]
for path in copy_source:
    src = SRC / path
    dst = target / "java" / path.split("java/", 1)[1]
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(src, dst)
for name in ("libsectionprobe.so", "libuaprofile.so"):
    src = SRC / "src/main/jniLibs/armeabi-v7a" / name
    dst = target / "jniLibs/armeabi-v7a" / name
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(src, dst)
jar = ROOT / "app/libs/lovestv-runtime.jar"
jar.parent.mkdir(parents=True, exist_ok=True)
shutil.copyfile(SRC / "libs/lovestv-runtime.jar", jar)
# The historical JAR was compiled for Channel+'s old appId. It rejects O!
# before invoking the manufacturer's real authorization. Adapt only this
# guard in our own legacy client; TCL's systemAuthorized remains unchanged.
old_id = b"tv.own.owntv.tdt2"
new_id = b"com.ayanabyte.one.hbbtvdiag6"
class_path = "tv/own/channelplus/lovestvprobe/ExperimentalAuthorization.class"
guard = len(old_id).to_bytes(2, "big") + old_id
replacement = len(new_id).to_bytes(2, "big") + new_id
patched = jar.with_suffix(".patched.jar")
with zipfile.ZipFile(jar) as zin, zipfile.ZipFile(patched, "w") as zout:
    seen_guard = False
    for entry in zin.infolist():
        data = zin.read(entry.filename)
        if entry.filename == class_path:
            assert data.count(guard) == 1, "Historical package guard changed"
            data = data.replace(guard, replacement)
            seen_guard = True
        zout.writestr(entry, data)
assert seen_guard, "Historical registration class missing from JAR"
patched.replace(jar)
with zipfile.ZipFile(jar) as check:
    patched_class = check.read(class_path)
    assert patched_class.count(new_id) == 1 and old_id not in patched_class
print("Adapted historical app-id check, without bypassing OEM authorization")
asset = target / "assets/lovestv_playback_145.js"
asset.parent.mkdir(parents=True, exist_ok=True)
shutil.copyfile(SRC / "src/hbbtv32/assets/lovestv_playback_145.js", asset)

# Preserve the vendor failure in the IPC reply rather than dropping it.
# The original implementation sent an empty Bundle and hid the actual cause.
service = SRC / "src/main/java/tv/own/channelplus/lovestvprobe/HbbTvRuntimeService.java"
service_source = service.read_text(encoding="utf-8")
old_reply = 'Log.e(TAG,reason,error); reply(FAILED,new Bundle()); cleanup();'
assert service_source.count(old_reply) == 1, "Unexpected runtime failure handler"
service_source = service_source.replace(old_reply, """
        Log.e(TAG,reason,error);
        String detail=reason;
        if(reason.contains("registration failed")) {
            try {
                String report=Report.read(windowContext!=null?windowContext:this);
                if(report!=null && !report.trim().isEmpty()) {
                    String last=report.trim();
                    int split=last.lastIndexOf('\\n');
                    if(split>=0) last=last.substring(split+1);
                    detail+=" | "+last;
                }
            } catch(Exception ignored) { }
        }
        if(error!=null) {
            Throwable cause=error;
            for(int depth=0; depth<8 && cause.getCause()!=null && cause.getCause()!=cause; depth++) {
                cause=cause.getCause();
            }
            detail+=" | "+cause.getClass().getName()+": "+String.valueOf(cause.getMessage());
        }
        if(detail.length()>450) detail=detail.substring(0,450);
        Bundle info=new Bundle(); info.putString("oneFailure",detail);
        reply(FAILED,info); cleanup();""")
# The working 1.45 probe mounted Tbrowser's root inside Activity content.
# The previous O! bridge mounted it in decorView, a different view hierarchy.
# Keep the existing TvView and content; mount only the transparent browser root.
old_parent = 'activityParent=(ViewGroup)playerActivity.getWindow().getDecorView();'
new_parent = 'activityParent=(ViewGroup)playerActivity.findViewById(android.R.id.content);'
if service_source.count(old_parent) != 1:
    raise RuntimeError("Unexpected TCL browser parent hierarchy")
service_source = service_source.replace(old_parent,new_parent)
# The physical Activity is required, not a Service/window context.
attach_line = 'activityParent.addView(root,new ViewGroup.LayoutParams(-1,-1));'
if service_source.count(attach_line)!=1:
    raise RuntimeError("Cannot verify browser attachment")
service_source=service_source.replace(attach_line,
    attach_line + '''
            Log.i(TAG,"TCL root parent="+activityParent.getClass().getName()
                +" windowToken="+(root.getWindowToken()!=null)
                +" size="+root.getWidth()+"x"+root.getHeight());''')

# Track the *real* runtime stages across Messenger IPC.
def replace_runtime_one(before, after, label):
    global service_source
    if service_source.count(before) != 1:
        raise RuntimeError("Cannot insert HbbTV stage: " + label)
    service_source = service_source.replace(before, after)

replace_runtime_one(
    "    private void initialize() {",
    """    private void oneStage(String text) {
        Log.i(TAG, "O! TCL stage: " + text);
        Bundle data=new Bundle(); data.putString("oneStage",text);
        reply(96,data);
    }
    private void initialize() {""",
    "stage sender"
)
replace_runtime_one(
    'Log.i(TAG,"Autostart: registering the 1.45 native runtime (no colour key)");',
    'Log.i(TAG,"Autostart: registering the 1.45 native runtime (no colour key)"); oneStage("1: registration requested");',
    "registration requested"
)
replace_runtime_one(
    'if(!authorized) { fail("Existing 1.45 runtime registration failed",null); return; }',
    'if(!authorized) { fail("Existing 1.45 runtime registration failed",null); return; } oneStage("2: registration passed");',
    "registration approved"
)
replace_runtime_one(
    'VendorBrowserAccess.prepare(windowContext);',
    'VendorBrowserAccess.prepare(windowContext); oneStage("3: vendor browser access prepared");',
    "vendor preparation"
)
replace_runtime_one(
    'Object initialized=call(engine,"init");',
    'oneStage("4: HbbtvServer.init requested"); Object initialized=call(engine,"init");',
    "engine init"
)
replace_runtime_one(
    'if(ok) browserReady(); else fail("Tbrowser onSuccess=false",null);',
    'if(ok) { oneStage("7: Tbrowser callback succeeded"); browserReady(); } else fail("Tbrowser onSuccess=false",null);',
    "browser callback"
)
replace_runtime_one(
    '.invoke(browser,windowContext,root,false);',
    '.invoke(browser,windowContext,root,false); oneStage("6: Tbrowser.init invoked; waiting for callback");',
    "browser initialization"
)
replace_runtime_one(
    'initListener=Proxy.newProxyInstance(listener.getClassLoader(),new Class<?>[]{listener},(proxy,method,args)->{',
    '''initListener=Proxy.newProxyInstance(listener.getClassLoader(),new Class<?>[]{listener},(proxy,method,args)->{
                    if(method.getDeclaringClass()!=Object.class) {
                        String callback=method.getName();
                        String info="Tbrowser callback="+callback
                            +" argc="+(args==null ? 0 : args.length);
                        Log.i(TAG,info);
                        main.post(()->oneStage("callback: "+callback));
                    }''',
    "browser callback trace"
)
replace_runtime_one(
    'browser.getClass().getMethod("setTbrowserInitEventListener",listener).invoke(browser,initListener);',
    'browser.getClass().getMethod("setTbrowserInitEventListener",listener).invoke(browser,initListener); oneStage("5: callback listener installed");',
    "browser callback registration"
)
replace_runtime_one(
    'browser.getClass().getMethod("initTbrowser",Context.class,ViewGroup.class,boolean.class)',
    'oneStage("5a: calling native browser, Activity="+(windowContext instanceof android.app.Activity)+" token="+(root.getWindowToken()!=null)); browser.getClass().getMethod("initTbrowser",Context.class,ViewGroup.class,boolean.class)',
    "browser Activity and window token"
)

replace_runtime_one(
    'Object result=call(engine,"start");',
    'oneStage("8: HbbtvServer.start requested"); Object result=call(engine,"start");',
    "engine start"
)
replace_runtime_one(
    'reply(STARTED,new Bundle()); inspect.run();',
    'oneStage("9: engine started; awaiting broadcaster application"); reply(STARTED,new Bundle()); inspect.run();',
    "startup complete"
)

service.write_text(service_source,encoding="utf-8")
# CRITICAL: copy_source already copied this file to O!.
# Copy the modified runtime AGAIN, so O! actually sends oneFailure
# instead of the old FAILED message with an empty Bundle.
embedded_runtime = target / "java/tv/own/channelplus/lovestvprobe/HbbTvRuntimeService.java"
shutil.copyfile(service, embedded_runtime)
assert 'info.putString("oneFailure",detail)' in embedded_runtime.read_text(encoding="utf-8")
assert 'reply(FAILED,new Bundle())' not in embedded_runtime.read_text(encoding="utf-8")

# Keep a traceable source, but do not put the old product UI into O!.
runtime = target / "java/tv/own/channelplus/lovestvprobe/HbbTvRuntimeService.java"
view = target / "java/tv/own/owntv/features/hbbtv/HbbTvPlayerView.java"

# Display a truthful startup trace inside the experimental player, avoiding
# guessing why LOVEStv's broadcaster hint isn't rendered.
source = view.read_text(encoding="utf-8")
old_failed = 'case FAILED: fail("Runtime initialization failed; see preceding ChannelplusHbb log",null); break;'
assert source.count(old_failed) == 1, "Unexpected host-side failure handler"
source = source.replace(old_failed, 'case FAILED: fail(data.getString("oneFailure","TCL HbbTV failed without details"),null); break;')
anchor = "    public TvView getTvView() { return tvView; }"
assert source.count(anchor) == 1
source = source.replace(anchor, """
    private String oneLastFailure = "none";
    public String oneDiagnostic() {
        return "TCL input=" + (input==null ? "(none)" : input)
            + "\\nvideo=" + ready + " enabled=" + enabled
            + " 32bit=" + (!Process.is64Bit())
            + " jar=" + new File("/product/framework/com.tcl.hbbtv.jar").isFile()
            + "\\noverlay=" + (overlay!=null) + " surface=" + surfaceReady
            + " bound=" + (connection!=null) + " messenger=" + (remote!=null)
            + "\\nattached=" + attached + " started=" + started
            + " failed=" + failed + " error=" + oneLastFailure;
    }
""" + anchor)
needle = 'private void fail(String reason,Throwable error) {'
assert source.count(needle) == 1
source = source.replace(needle, needle + ' oneLastFailure=reason;')
view.write_text(source, encoding="utf-8")

# Listen for separate, non-terminal initialization messages from TCL runtime.
host = view.read_text(encoding="utf-8")
if host.count('private String oneLastFailure = "none";') != 1:
    raise RuntimeError("DIAG 5 requires existing on-screen diagnostic")
host = host.replace('private String oneLastFailure = "none";',
    'private String oneLastFailure = "none", oneStage = "waiting for runtime stage";')
if host.count('case FAILED: fail(data.getString("oneFailure","TCL HbbTV failed without details"),null); break;') != 1:
    raise RuntimeError("Host message switch changed")
host = host.replace('case FAILED: fail(data.getString("oneFailure","TCL HbbTV failed without details"),null); break;',
    'case FAILED: fail(data.getString("oneFailure","TCL HbbTV failed without details"),null); break;\n            case 96: oneStage=data.getString("oneStage","runtime event"); break;')
needle = ' + " failed=" + failed + " error=" + oneLastFailure;'
if host.count(needle) != 1:
    raise RuntimeError("Host diagnostics anchor changed")
host = host.replace(needle, ' + " failed=" + failed + " error=" + oneLastFailure\n            + "\\nlastStage=" + oneStage;')
view.write_text(host,encoding="utf-8")

assert "oneFailure" in runtime.read_text(), "Error details were not packaged into O!"
assert "activityParent.addView(root" in runtime.read_text()
assert "installPlaybackControls" in runtime.read_text()
assert "HbbTvActivityHost.attach(activity)" in view.read_text()
print("Prepared TCL-only embedded HbbTV variant; original stable O! unaffected")
