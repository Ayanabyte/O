#!/usr/bin/env python3
"""Apply the independent TvView test to a local OpenRedButtonProject/orb checkout.

Changes ONLY android/mock203app, never app/src/... or com.ayanabyte.one.
Usage: python3 apply_to_orb.py /path/to/orb
"""
import argparse
from pathlib import Path
import shutil

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('orb_root', type=Path, help='Root of the local ORB checkout')
args = parser.parse_args()

root = args.orb_root.expanduser().resolve()
kit = Path(__file__).resolve().parent
src = kit / 'TdtTvViewTestActivity.java'
dst = root / 'android/mock203app/src/main/java/org/orbtv/mock203app/TdtTvViewTestActivity.java'
manifest = root / 'android/mock203app/src/main/AndroidManifest.xml'
main_activity = root / 'android/mock203app/src/main/java/org/orbtv/mock203app/MainActivity.java'

if not main_activity.is_file() or not manifest.is_file():
    parser.error('Not an ORB checkout containing android/mock203app; no files changed.')
if dst.exists() and dst.read_bytes() != src.read_bytes():
    parser.error(f'{dst} has local edits. Save/merge those changes manually; no files changed.')
text = manifest.read_text(encoding='utf-8')
if 'TdtTvViewTestActivity' not in text:
    if '</application>' not in text:
        parser.error('Cannot find </application> in manifest; no files changed.')
    fragment = (kit / 'AndroidManifest.fragment.xml').read_text(encoding='utf-8')
    fragment = fragment.split('-->', 1)[-1].strip()
    text = text.replace('</application>', '    ' + fragment + '\n    </application>', 1)
    manifest.write_text(text, encoding='utf-8')
    print('Registered activity in:', manifest)
else:
    print('Activity already registered in:', manifest)
shutil.copy2(src, dst)
print('Installed Java activity:', dst)
print('No original ORB MainActivity, TCL system package, or O! application code modified.')
