#!/usr/bin/env python3
"""
Deploy built mod jar to Exaroton server via REST API.
Atomic replacement: removes old version of mod jar before/after uploading new jar.

Requires EXAROTON_API_TOKEN and EXAROTON_SERVER_ID in .env or environment.
"""

from __future__ import annotations

import json
import os
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parent.parent
ENV_PATH = ROOT_DIR / ".env"
BUILD_LIBS_DIR = ROOT_DIR / "build" / "libs"
GRADLE_PROPERTIES_PATH = ROOT_DIR / "gradle.properties"


def load_env() -> dict[str, str]:
    env_vars: dict[str, str] = dict(os.environ)
    if ENV_PATH.exists():
        for line in ENV_PATH.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            k, v = line.split("=", 1)
            env_vars[k.strip()] = v.strip().strip("'\"")
    return env_vars


def get_base_name() -> str:
    if GRADLE_PROPERTIES_PATH.exists():
        content = GRADLE_PROPERTIES_PATH.read_text(encoding="utf-8")
        match = re.search(r"^archives_base_name\s*=\s*(.+)$", content, re.MULTILINE)
        if match:
            return match.group(1).strip()
    return "exservermod"


def find_latest_jar(base_name: str) -> Path:
    if not BUILD_LIBS_DIR.exists():
        raise FileNotFoundError(f"Build directory not found: {BUILD_LIBS_DIR}. Run 'task build' first.")

    candidates = [
        f
        for f in BUILD_LIBS_DIR.glob("*.jar")
        if not f.name.endswith("-sources.jar") and not f.name.endswith("-dev.jar")
    ]

    if not candidates:
        raise FileNotFoundError(f"No mod jar found in {BUILD_LIBS_DIR}. Run 'task build' first.")

    # Match base name if possible
    matching = [f for f in candidates if f.name.startswith(base_name)]
    if matching:
        return sorted(matching, key=lambda f: f.stat().st_mtime, reverse=True)[0]
    return sorted(candidates, key=lambda f: f.stat().st_mtime, reverse=True)[0]


def api_request(
    server_id: str,
    token: str,
    endpoint: str,
    method: str = "GET",
    data: bytes | None = None,
    content_type: str | None = None,
) -> dict | None:
    url = f"https://api.exaroton.com/v1/servers/{server_id}{endpoint}"
    headers = {
        "Authorization": f"Bearer {token}",
        "User-Agent": "ExServerMod-Deployer/1.0",
    }
    if content_type:
        headers["Content-Type"] = content_type

    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            body = resp.read()
            if body and resp.headers.get_content_type() == "application/json":
                return json.loads(body.decode("utf-8"))
            return None
    except urllib.error.HTTPError as err:
        err_body = err.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"Exaroton API error ({err.code} {err.reason}) on {method} {url}: {err_body}") from err


def deploy() -> None:
    env = load_env()
    token = env.get("EXAROTON_API_TOKEN")
    server_id = env.get("EXAROTON_SERVER_ID")
    mods_dir = env.get("EXAROTON_MODS_DIR", "mods").strip("/")
    auto_restart = env.get("EXAROTON_AUTO_RESTART", "false").lower() in ("true", "1", "yes")

    if not token or token == "your_exaroton_api_token_here":
        print("ERROR: EXAROTON_API_TOKEN is missing or not set in .env", file=sys.stderr)
        print("Please configure .env from .env.example before deploying.", file=sys.stderr)
        sys.exit(1)

    if not server_id or server_id == "your_server_id_here":
        print("ERROR: EXAROTON_SERVER_ID is missing or not set in .env", file=sys.stderr)
        print("Please configure .env from .env.example before deploying.", file=sys.stderr)
        sys.exit(1)

    base_name = get_base_name()
    local_jar = find_latest_jar(base_name)
    print(f"[TARGET] Artifact: {local_jar.name} ({local_jar.stat().st_size} bytes)")

    # 1. Inspect existing files in mods directory
    print(f"[REMOTE] Checking '{mods_dir}/' on Exaroton server ({server_id})...")
    remote_files = api_request(server_id, token, f"/files/data/{mods_dir}")

    old_jars_to_delete: list[str] = []
    if remote_files and remote_files.get("success"):
        file_list = remote_files.get("data", [])
        if isinstance(file_list, list):
            for item in file_list:
                item_name = item.get("name", "")
                item_path = item.get("path", f"{mods_dir}/{item_name}")
                # Look for previous versions of this mod
                if item_name.startswith(f"{base_name}-") and item_name.endswith(".jar"):
                    if item_name != local_jar.name:
                        old_jars_to_delete.append(item_path)

    # 2. Delete outdated versions to prevent jar duplicates
    for old_path in old_jars_to_delete:
        print(f"[DELETE] Outdated jar: {old_path}...")
        api_request(server_id, token, f"/files/data/{old_path}", method="DELETE")

    # 3. Upload new version
    target_remote_path = f"{mods_dir}/{local_jar.name}"
    print(f"[UPLOAD] Uploading {local_jar.name} to {target_remote_path}...")
    jar_bytes = local_jar.read_bytes()
    api_request(
        server_id,
        token,
        f"/files/data/{target_remote_path}",
        method="PUT",
        data=jar_bytes,
        content_type="application/octet-stream",
    )
    print("[SUCCESS] Upload complete.")

    # 4. Optional restart
    if auto_restart:
        print("[RESTART] Restarting Exaroton server...")
        api_request(server_id, token, "/restart", method="POST")
        print("[SUCCESS] Server restart command sent.")
    else:
        print("[INFO] Server restart skipped (EXAROTON_AUTO_RESTART=false).")


if __name__ == "__main__":
    deploy()
