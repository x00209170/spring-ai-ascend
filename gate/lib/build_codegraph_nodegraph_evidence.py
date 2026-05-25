#!/usr/bin/env python3
"""Build local CodeGraph nodegraph evidence from `.codegraph/codegraph.db`.

The CodeGraph SQLite index is a regenerated local artifact and stays ignored by
git. This helper records its auditable shape without committing the database:
file/node/edge totals, unresolved-reference count, schema versions, languages,
and node-kind distribution.
"""

from __future__ import annotations

import argparse
import shutil
import sqlite3
import subprocess
import sys
import tempfile
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

import yaml


REPO_DEFAULT = Path(__file__).resolve().parents[2]


def _git_value(root: Path, *args: str) -> str | None:
    try:
        result = subprocess.run(
            ["git", *args],
            cwd=root,
            text=True,
            capture_output=True,
            check=False,
        )
    except OSError:
        return None
    if result.returncode != 0:
        return None
    return result.stdout.strip() or None


def _git_status_dirty(root: Path) -> bool | None:
    try:
        result = subprocess.run(
            ["git", "status", "--short"],
            cwd=root,
            text=True,
            capture_output=True,
            check=False,
        )
    except OSError:
        return None
    if result.returncode != 0:
        return None
    return bool(result.stdout.strip())


def _copy_db_for_reading(db_path: Path, dest_dir: Path) -> Path:
    dest = dest_dir / "codegraph.db"
    for suffix in ("", "-wal", "-shm"):
        source = Path(str(db_path) + suffix)
        if source.exists():
            shutil.copy2(source, Path(str(dest) + suffix))
    return dest


def _count(cur: sqlite3.Cursor, table: str) -> int:
    return int(cur.execute(f"select count(*) from {table}").fetchone()[0])


def _existing_tables(cur: sqlite3.Cursor) -> set[str]:
    return {
        str(row[0])
        for row in cur.execute("select name from sqlite_master where type='table'")
    }


def _nodegraph_metrics(db_path: Path) -> dict[str, Any]:
    if not db_path.is_file():
        raise FileNotFoundError(f"{db_path} is missing")

    with tempfile.TemporaryDirectory(prefix="codegraph-nodegraph-") as tmp:
        readable_db = _copy_db_for_reading(db_path, Path(tmp))
        with sqlite3.connect(readable_db) as con:
            cur = con.cursor()
            tables = _existing_tables(cur)
            required = {"files", "nodes", "edges"}
            missing = sorted(required - tables)
            if missing:
                raise RuntimeError(f"CodeGraph database is missing required table(s): {', '.join(missing)}")

            unresolved_refs = _count(cur, "unresolved_refs") if "unresolved_refs" in tables else 0
            schema_versions = (
                [int(row[0]) for row in cur.execute("select version from schema_versions order by version")]
                if "schema_versions" in tables
                else []
            )
            nodes_by_kind = {
                str(kind): int(count)
                for kind, count in cur.execute(
                    "select kind, count(*) from nodes group by kind order by kind"
                )
            }
            languages = (
                [str(row[0]) for row in cur.execute("select distinct language from files order by language")]
                if "files" in tables
                else []
            )

            return {
                "files": _count(cur, "files"),
                "nodes": _count(cur, "nodes"),
                "edges": _count(cur, "edges"),
                "unresolved_refs": unresolved_refs,
                "schema_versions": schema_versions,
                "languages": languages,
                "nodes_by_kind": nodes_by_kind,
            }


def build_evidence(root: Path) -> dict[str, Any]:
    db_path = root / ".codegraph" / "codegraph.db"
    commit_sha = _git_value(root, "rev-parse", "HEAD")

    return {
        "schema_version": 1,
        "generated_at_utc": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "tool": "codegraph",
        "repository": {
            "root": str(root),
            "commit_sha": commit_sha,
            "dirty": _git_status_dirty(root),
        },
        "artifact": {
            "path": ".codegraph/codegraph.db",
            "tracked": False,
            "db_size_bytes": db_path.stat().st_size if db_path.exists() else 0,
            "note": "Local regenerated CodeGraph nodegraph; database remains git-ignored.",
        },
        "nodegraph": _nodegraph_metrics(db_path),
    }


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", default=str(REPO_DEFAULT), help="Repository root")
    parser.add_argument("--output", help="Write YAML evidence to this file")
    args = parser.parse_args(argv)

    try:
        evidence = build_evidence(Path(args.root).resolve())
    except Exception as exc:  # noqa: BLE001 - CLI should report any unreadable artifact
        print(f"FAIL: codegraph_nodegraph_evidence -- {exc}", file=sys.stderr)
        return 1

    rendered = yaml.safe_dump(evidence, sort_keys=False, allow_unicode=False)
    if args.output:
        output = Path(args.output)
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(rendered, encoding="utf-8")
    else:
        sys.stdout.write(rendered)
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
