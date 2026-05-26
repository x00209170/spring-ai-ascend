#!/usr/bin/env python3
"""Validate contract-catalog SPI truth against the latest release note."""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path


PROMOTED_NAMES = ("Skill", "AgentRegistry")


def read_text(path: Path) -> str:
    if not path.is_file():
        return ""
    return path.read_text(encoding="utf-8", errors="replace")


def latest_release(root: Path) -> Path | None:
    release_dir = root / "docs" / "logs" / "releases"
    files = sorted(release_dir.glob("*.md")) if release_dir.is_dir() else []

    def key(path: Path) -> tuple[int, str]:
        match = re.search(r"rc([0-9]+)", path.name)
        return int(match.group(1)) if match else 0, path.name

    return sorted(files, key=key)[-1] if files else None


def active_spi_total(catalog: str) -> int | None:
    match = re.search(r"\*\*Active SPI interfaces \(([0-9]+) total\):\*\*", catalog)
    return int(match.group(1)) if match else None


def module_sum(catalog: str) -> int:
    total = 0
    in_section = False
    for line in catalog.splitlines():
        if line.strip() == "**Count by module:**" or line.startswith("**SPI count by module"):
            in_section = True
            continue
        if in_section and line.startswith("## "):
            break
        if not in_section:
            continue
        match = re.match(r"\| `[^`]+` \| ([0-9]+) \(", line)
        if match:
            total += int(match.group(1))
    return total


def latest_release_spi_total(text: str) -> int | None:
    match = re.search(r"Active SPI interfaces:\s*([0-9]+)\s+total", text)
    return int(match.group(1)) if match else None


def contract_surface_failures(root: Path) -> list[str]:
    """Check cross-surface declarations that review found can drift silently."""

    failures: list[str] = []
    chat = read_text(root / "docs" / "contracts" / "chat-advisor.v1.yaml")
    if not chat:
        return failures

    agent_contract = read_text(root / "docs" / "contracts" / "agent-definition.v1.yaml")
    streaming_contract = read_text(root / "docs" / "contracts" / "model-streaming.v1.yaml")
    agent_definition_java = read_text(
        root
        / "agent-service"
        / "src"
        / "main"
        / "java"
        / "com"
        / "huawei"
        / "ascend"
        / "service"
        / "agent"
        / "spi"
        / "AgentDefinition.java"
    )
    advisor_binding_java = read_text(
        root
        / "agent-service"
        / "src"
        / "main"
        / "java"
        / "com"
        / "huawei"
        / "ascend"
        / "service"
        / "agent"
        / "spi"
        / "AdvisorBinding.java"
    )

    if "requestEnvelope" in chat or "responseEnvelope" in chat:
        failures.append("chat-advisor contract must use typed modelRequest/modelResponse carriers, not raw envelopes")
    if "modelRequest" in chat and "AdvisedModelRequest" not in chat:
        failures.append("chat-advisor modelRequest field must name AdvisedModelRequest")
    if "modelResponse" in chat and "AdvisedModelResponse" not in chat:
        failures.append("chat-advisor modelResponse field must name AdvisedModelResponse")

    if "advisorBindings" in chat or "AgentDefinition.advisorBindings" in chat:
        if "advisorBindings" not in agent_contract:
            failures.append("chat-advisor binding claims advisorBindings but agent-definition contract lacks the field")
        if "AdvisorBinding" not in agent_contract:
            failures.append("agent-definition contract must describe AdvisorBinding when chat-advisor binds through it")
        if "List<AdvisorBinding> advisorBindings" not in agent_definition_java:
            failures.append("AgentDefinition.java must carry List<AdvisorBinding> advisorBindings")
        if "record AdvisorBinding" not in advisor_binding_java:
            failures.append("AdvisorBinding.java same-package SPI carrier is missing")
        joined = "\n".join([chat, agent_contract, agent_definition_java])
        if re.search(r"advisorBindings[^\\n;]*ChatAdvisor|List<ChatAdvisor>\\s+advisorBindings", joined):
            failures.append("AgentDefinition advisorBindings must not depend on ChatAdvisor; use AdvisorBinding by name")

    chat_sequence = re.search(r"sequence_id:\s*([A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+)", chat)
    streaming_sequence = re.search(
        r"sequence_id:\s*([A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+)", streaming_contract
    )
    if chat_sequence and streaming_contract:
        if not streaming_sequence:
            failures.append("model-streaming contract must declare the advisor/model hook sequence_id")
        elif streaming_sequence.group(1) != chat_sequence.group(1):
            failures.append(
                "chat-advisor and model-streaming hook sequence_id values must match "
                f"({chat_sequence.group(1)} != {streaming_sequence.group(1)})"
            )

    return failures


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", default=".", help="Repository root")
    args = parser.parse_args(argv)

    root = Path(args.root).resolve()
    catalog_path = root / "docs" / "contracts" / "contract-catalog.md"
    catalog = catalog_path.read_text(encoding="utf-8", errors="replace")
    failures: list[str] = []

    total = active_spi_total(catalog)
    if total is None:
        failures.append("contract catalog missing Active SPI interfaces total")
    else:
        summed = module_sum(catalog)
        if summed != total:
            failures.append(f"contract catalog module count sum {summed} != active total {total}")

    if "**Design-named SPIs (deferred W2+):**" in catalog:
        stale_text = catalog.split("**Design-named SPIs (deferred W2+):**", 1)[-1]
        for name in PROMOTED_NAMES:
            if re.search(rf"`{name}`[^\\n]*\\|[^\\n]*(W2|post-W4)", stale_text):
                failures.append(f"promoted SPI {name} must not be listed as deferred design-only")

    release = latest_release(root)
    if release is not None and total is not None:
        release_text = release.read_text(encoding="utf-8", errors="replace")
        release_total = latest_release_spi_total(release_text)
        if release_total is not None and release_total != total:
            failures.append(
                f"{release.relative_to(root)} active SPI total {release_total} != catalog total {total}"
            )

    failures.extend(contract_surface_failures(root))

    if failures:
        print("; ".join(failures))
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
