"""Offline regression check using the yt-dlp bundled in the Android dependency."""

import json
import os
from pathlib import Path
import re
import sys
import tempfile
import zipfile


root = Path(__file__).resolve().parents[1]
source = (root / "app/src/main/java/com/indirx/app/SupportedPlatforms.kt").read_text(encoding="utf-8")
match = re.search(r'const val extractors = ("(?:[^"\\]|\\.)*")', source)
if not match:
    raise RuntimeError("Extractor configuration not found")
patterns = json.loads(match[1]).split(",")
cache = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle"))
aars = list((cache / "caches/modules-2/files-2.1/io.github.junkfood02.youtubedl-android/library/0.18.1").glob("*/*.aar"))
if len(aars) != 1:
    raise RuntimeError(f"Expected one resolved Android library AAR; got {len(aars)}")

with tempfile.TemporaryDirectory() as temporary:
    engine = Path(temporary) / "ytdlp.zip"
    with zipfile.ZipFile(aars[0]) as aar:
        engine.write_bytes(aar.read("res/raw/ytdlp"))
    sys.path.insert(0, str(engine))
    from yt_dlp import YoutubeDL

    # No extract_info/download calls: this checks real extractor registration offline.
    with YoutubeDL({"allowed_extractors": patterns, "quiet": True}) as downloader:
        loaded = set(downloader._ies)
        required = {"Youtube", "YoutubeTab", "Instagram"}
        if missing := required - loaded:
            raise AssertionError(f"Required extractors missing: {sorted(missing)}")
        if loaded.intersection({"TikTok", "TikTokVM", "Generic", "Facebook", "Twitter", "Vimeo", "Dailymotion", "Douyin"}):
            raise AssertionError("An unsupported platform extractor was enabled")
        extractors = list(downloader._ies.values())
        if any(not ie.IE_NAME.lower().startswith(("youtube", "instagram")) for ie in extractors):
            raise AssertionError("An extractor outside YouTube/Instagram was enabled")
        for url in (
            "https://www.tiktok.com/@name/video/1234567890123456789",
            "https://vm.tiktok.com/ZM123abc/",
            "https://vt.tiktok.com/ZM123abc/",
            "https://www.tiktok.com/t/ZM123abc/",
        ):
            if any(ie.suitable(url) for ie in extractors):
                raise AssertionError(f"A loaded extractor still accepts TikTok: {url}")
        print("Bundled yt-dlp: only YouTube/Instagram loaded; TikTok normal and short links rejected offline.")
