#!/usr/bin/env python3
"""Fail CI if tracked text files introduce identifiers from retired applications.

The tokens are stored as hex so this guard is not itself a source of the
identifiers it rejects. The application's one supported namespace is
com.ayanabyte.one. The native TCL service namespace is not a retired app.
"""
from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

RETIRED = tuple(
    bytes.fromhex(encoded).decode("ascii")
    for encoded in (
        "6368616e6e656c706c7573",
        "6368616e6e656c2b",
        "737461796f6e",
        "6f776e7476",
        "74762e6f776e2e",
    )
)

def compact(text: str) -> str:
    return re.sub(r"[\s_-]", "", text.casefold())


def main() -> int:
    filenames = subprocess.check_output(["git", "ls-files", "-z"]).decode("utf-8").split("\0")
    violations: list[str] = []
    for filename in filter(None, filenames):
        path = Path(filename)
        if not path.is_file():
            continue
        if any(name in compact(filename) for name in RETIRED):
            violations.append(f"{filename}: ruta heredada")
        try:
            content = path.read_text(encoding="utf-8")
        except (UnicodeError, OSError):
            continue  # Bundled Gradle binaries and other non-text resources
        if "\0" in content:
            continue
        for line_no, line in enumerate(content.splitlines(), 1):
            if any(name in compact(line) for name in RETIRED):
                violations.append(f"{filename}:{line_no}: identificador heredado")
    if violations:
        print("La identidad de O! no está limpia:")
        print("\n".join(violations))
        return 1
    print("Identidad O! verificada: sin nombres heredados en archivos versionados.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
