#!/usr/bin/env python3
"""Delete the pairs save database. Useful for testing a clean state."""

import os
import sys
import platform
from pathlib import Path

APP_NAME = "pairs"
DB_FILENAME = "save.db"


def data_dir() -> Path:
    home = Path.home()
    system = platform.system()
    if system == "Darwin":
        return home / "Library" / "Application Support" / APP_NAME
    if system == "Windows":
        appdata = os.environ.get("APPDATA") or str(home)
        return Path(appdata) / APP_NAME
    # Linux / BSD: XDG Base Directory
    xdg = os.environ.get("XDG_DATA_HOME", "")
    if xdg:
        return Path(xdg) / APP_NAME
    return home / ".local" / "share" / APP_NAME


db_path = data_dir() / DB_FILENAME

if not db_path.exists():
    print(f"Database not found: {db_path}")
    sys.exit(0)

db_path.unlink()
print(f"Deleted: {db_path}")
