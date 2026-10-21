#!/usr/bin/env python3
"""
Semantic version bumper for gradle.properties.
Usage:
    python3 scripts/bump_version.py get
    python3 scripts/bump_version.py patch
    python3 scripts/bump_version.py minor
    python3 scripts/bump_version.py major
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

PROPERTIES_PATH = Path(__file__).resolve().parent.parent / "gradle.properties"
VERSION_REGEX = re.compile(r"^(mod_version\s*=\s*)([0-9]+(?:\.[0-9]+)*)(.*)$", re.MULTILINE)


def read_version_line() -> tuple[str, str, int, list[int]]:
    if not PROPERTIES_PATH.exists():
        raise FileNotFoundError(f"Missing {PROPERTIES_PATH}")

    content = PROPERTIES_PATH.read_text(encoding="utf-8")
    match = VERSION_REGEX.search(content)
    if not match:
        raise ValueError(f"Could not find 'mod_version' key in {PROPERTIES_PATH}")

    current_ver = match.group(2)
    parts = [int(p) for p in current_ver.split(".")]
    return content, current_ver, match.start(), parts


def bump(parts: list[int], action: str) -> list[int]:
    # Ensure at least 2 components (major.minor)
    while len(parts) < 2:
        parts.append(0)

    if action == "major":
        parts[0] += 1
        for i in range(1, len(parts)):
            parts[i] = 0
    elif action == "minor":
        parts[1] += 1
        for i in range(2, len(parts)):
            parts[i] = 0
    elif action == "patch":
        if len(parts) == 2:
            parts.append(1)
        else:
            parts[2] += 1
            for i in range(3, len(parts)):
                parts[i] = 0
    else:
        raise ValueError(f"Unknown bump action: {action}")

    return parts


def main() -> None:
    action = sys.argv[1] if len(sys.argv) > 1 else "get"
    content, current_ver, _, parts = read_version_line()

    if action == "get":
        print(current_ver)
        return

    new_parts = bump(list(parts), action)
    new_ver = ".".join(str(p) for p in new_parts)

    new_content = VERSION_REGEX.sub(rf"\g<1>{new_ver}\g<3>", content)
    PROPERTIES_PATH.write_text(new_content, encoding="utf-8")
    print(f"Bumped version: {current_ver} -> {new_ver}")


if __name__ == "__main__":
    main()
