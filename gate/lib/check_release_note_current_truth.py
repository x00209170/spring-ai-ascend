#!/usr/bin/env python3
"""Validate the latest active release note does not publish live placeholders."""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path


PLACEHOLDER_PATTERNS = (
    ("pending-formal-validator-run", re.compile(r"pending-formal-validator-run")),
    ("TO BE GENERATED", re.compile(r"TO BE GENERATED")),
    ("TBD", re.compile(r"\bTBD\b")),
    ("TODO-template", re.compile(r"\bTODO-template\b")),
    ("TODO", re.compile(r"\bTODO\b")),
)


def latest_release(root: Path) -> Path | None:
    release_dir = root / "docs" / "logs" / "releases"
    files = sorted(release_dir.glob("*.md")) if release_dir.is_dir() else []

    def key(path: Path) -> tuple[int, str]:
        match = re.search(r"rc([0-9]+)", path.name)
        return int(match.group(1)) if match else 0, path.name

    return sorted(files, key=key)[-1] if files else None


def frontmatter(text: str) -> dict[str, str]:
    match = re.match(r"^---\s*\n(.*?)\n---\s*\n", text, re.DOTALL)
    if not match:
        return {}
    result: dict[str, str] = {}
    for line in match.group(1).splitlines():
        if ":" not in line:
            continue
        key, value = line.split(":", 1)
        result[key.strip()] = value.strip().strip("'\"")
    return result


def is_allowed_placeholder_citation(line: str) -> bool:
    """Allow classification prose that names the placeholder family itself."""
    allowed_markers = (
        "F-placeholder-leaks-into-active-corpus",
        "placeholder token",
        "placeholder tokens",
        "anonymous slugs",
    )
    return any(marker in line for marker in allowed_markers)


def placeholder_hits(path: Path, text: str) -> list[str]:
    hits: list[str] = []
    in_fence = False
    for line_no, line in enumerate(text.splitlines(), start=1):
        if line.strip().startswith("```"):
            in_fence = not in_fence
        if in_fence or is_allowed_placeholder_citation(line):
            continue
        for label, pattern in PLACEHOLDER_PATTERNS:
            if pattern.search(line):
                hits.append(f"{path.as_posix()}:{line_no}:{label}")
    return hits


def latest_review_response(root: Path) -> Path | None:
    review_dir = root / "docs" / "logs" / "reviews"
    files = sorted(review_dir.glob("*response*.md")) if review_dir.is_dir() else []
    return sorted(files, key=lambda path: path.name)[-1] if files else None


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", default=".", help="Repository root")
    args = parser.parse_args(argv)

    root = Path(args.root).resolve()
    release = latest_release(root)
    if release is None:
        print("no release notes found", file=sys.stderr)
        return 1

    text = release.read_text(encoding="utf-8", errors="replace")
    fm = frontmatter(text)
    if fm.get("superseded_by"):
        return 0
    status = fm.get("status", "")
    active_note = (
        fm.get("formal_release") == "true"
        or "ship" in status
        or "release" in status
        or "closure" in status
        or "Release Decision" in text
        or "Release Note" in text
    )
    if active_note:
        hits = placeholder_hits(release.relative_to(root), text)
        if hits:
            rel = release.relative_to(root).as_posix()
            print(f"{rel}: current release note contains live placeholder tokens: {', '.join(hits)}")
            return 1

    response = latest_review_response(root)
    if response is not None:
        response_text = response.read_text(encoding="utf-8", errors="replace")
        hits = placeholder_hits(response.relative_to(root), response_text)
        if hits:
            rel = response.relative_to(root).as_posix()
            print(f"{rel}: current review response contains live placeholder tokens: {', '.join(hits)}")
            return 1

    candidate = fm.get("release_candidate_commit", "")
    formal_note = fm.get("formal_release") == "true" or candidate or "formal" in status
    if formal_note and not re.fullmatch(r"[0-9a-f]{40}", candidate):
        rel = release.relative_to(root).as_posix()
        print(f"{rel}: release_candidate_commit must be a 40-character git SHA")
        return 1

    if fm.get("formal_release") == "true" and not fm.get("evidence_bundle"):
        rel = release.relative_to(root).as_posix()
        print(f"{rel}: formal_release true requires evidence_bundle")
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
