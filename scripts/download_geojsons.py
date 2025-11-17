#!/usr/bin/env python3
"""Download all .geojson and .geojsonseq assets from the latest GitHub release.

Usage:
  python3 scripts/download_geojsons.py --repo krizleebear/osm-polygons --dest data/geojsons

You can set `GITHUB_TOKEN` to increase API rate limits and to allow private-asset downloads.
"""
import argparse
import json
import os
import sys
import time
from urllib.request import Request, urlopen


def get_latest_release(repo, token=None):
    url = f"https://api.github.com/repos/{repo}/releases/latest"
    headers = {"Accept": "application/vnd.github.v3+json", "User-Agent": "download-geojsons-script"}
    if token:
        headers["Authorization"] = f"token {token}"
    req = Request(url, headers=headers)
    with urlopen(req) as resp:
        return json.load(resp)


def download_url(url, dest, token=None, chunk_size=8192):
    headers = {"User-Agent": "download-geojsons-script"}
    if token:
        headers["Authorization"] = f"token {token}"
    req = Request(url, headers=headers)
    with urlopen(req) as resp:
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        with open(dest, "wb") as out:
            while True:
                chunk = resp.read(chunk_size)
                if not chunk:
                    break
                out.write(chunk)


def main():
    p = argparse.ArgumentParser(description="Download .geojson and .geojsonseq assets from latest GitHub release")
    p.add_argument("--repo", default="krizleebear/osm-polygons", help="GitHub repo in owner/repo form")
    p.add_argument("--dest", default="data/geojsons", help="Destination directory")
    p.add_argument("--token", default=os.environ.get("GITHUB_TOKEN"), help="GitHub token (or set GITHUB_TOKEN env)")
    p.add_argument("--sleep", type=float, default=0.0, help="Seconds to wait between downloads")
    p.add_argument("--exts", default=",.geojson,.geojsonseq", help="Comma-separated extensions to consider (leading dots)")
    p.add_argument("--match", default=None, help="Case-insensitive substring (or comma-separated list) to match in asset filenames, e.g. 'germany' or 'germany,bavaria'")
    args = p.parse_args()

    try:
        release = get_latest_release(args.repo, args.token)
    except Exception as e:
        print(f"Failed to fetch release info: {e}")
        sys.exit(2)

    assets = release.get("assets", [])
    exts = [e.strip().lower() for e in args.exts.split(",") if e.strip()]
    matches = None
    if args.match:
        matches = [m.strip().lower() for m in args.match.split(",") if m.strip()]

    def is_match(name):
        nl = (name or "").lower()
        # extension match
        if not any(nl.endswith(ext) for ext in exts):
            return False
        # if no substring filters provided, accept by extension only
        if not matches:
            return True
        # accept if any provided substring appears in the filename
        return any(m in nl for m in matches)

    geo_assets = [a for a in assets if is_match(a.get("name", ""))]

    if not geo_assets:
        print("No matching geojson assets found in latest release.")
        sys.exit(0)

    print(f"Found {len(geo_assets)} matching asset(s). Downloading to: {args.dest}")

    for i, a in enumerate(geo_assets, start=1):
        name = a.get("name")
        url = a.get("browser_download_url")
        if not (name and url):
            print(f"Skipping malformed asset: {a}")
            continue
        dest_path = os.path.join(args.dest, name)
        print(f"[{i}/{len(geo_assets)}] Downloading {name} -> {dest_path}")
        try:
            download_url(url, dest_path, token=args.token)
        except Exception as e:
            print(f"Failed to download {name}: {e}")
            continue
        if args.sleep > 0:
            time.sleep(args.sleep)

    print("Done.")


if __name__ == "__main__":
    main()
