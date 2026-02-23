#!/usr/bin/env python3
"""
Local skill + DashScope quick test (OpenAI-compatible endpoint, no extra deps).

Usage:
  python scripts/skill_dashscope_test.py --list-skills
  python scripts/skill_dashscope_test.py --prompt "帮我总结这段话"
  python scripts/skill_dashscope_test.py --skill team/custom/python-test --prompt "介绍一下你自己"
"""

from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path
from urllib import request, error


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run DashScope chat with local SKILL.md prompts.")
    parser.add_argument("--prompt", default="请用简短方式介绍你自己。", help="User prompt")
    parser.add_argument(
        "--skill-dir",
        default="backend/skills/runtime",
        help="Local skill root directory (default: backend/skills/runtime)",
    )
    parser.add_argument(
        "--skill",
        action="append",
        default=[],
        help="Skill name to apply, can be used multiple times. Example: team/custom/python-test",
    )
    parser.add_argument("--list-skills", action="store_true", help="List discovered local skills and exit")
    parser.add_argument(
        "--model",
        default=os.getenv("DASHSCOPE_CHAT_MODEL", "qwen3.5-plus"),
        help="DashScope model name (default from DASHSCOPE_CHAT_MODEL or qwen3.5-plus)",
    )
    parser.add_argument(
        "--base-url",
        default=os.getenv("DASHSCOPE_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1"),
        help="DashScope OpenAI-compatible base URL",
    )
    return parser.parse_args()


def discover_skills(skill_root: Path) -> dict[str, str]:
    skills: dict[str, str] = {}
    if not skill_root.exists():
        return skills

    for file_path in skill_root.rglob("SKILL.md"):
        rel = file_path.relative_to(skill_root)
        # expect: <skill/name/path>/<version>/SKILL.md
        parts = rel.parts
        if len(parts) < 3:
            continue
        skill_name_parts = parts[:-2]
        skill_name = "/".join(skill_name_parts)
        try:
            content = file_path.read_text(encoding="utf-8").strip()
        except OSError:
            continue
        if content:
            skills[skill_name] = content
    return skills


def build_system_prompt(selected: dict[str, str]) -> str:
    if not selected:
        return "You are a helpful assistant."

    chunks = ["You are a helpful assistant. Follow the skills below strictly.", ""]
    for name, content in selected.items():
        chunks.append(f"[SKILL] {name}")
        chunks.append(content)
        chunks.append("")
    return "\n".join(chunks).strip()


def call_dashscope(base_url: str, api_key: str, model: str, system_prompt: str, user_prompt: str) -> str:
    endpoint = base_url.rstrip("/") + "/chat/completions"
    payload = {
        "model": model,
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ],
        "stream": False,
        "temperature": 0.2,
    }

    req = request.Request(
        endpoint,
        data=json.dumps(payload).encode("utf-8"),
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        },
        method="POST",
    )
    try:
        with request.urlopen(req, timeout=60) as resp:
            body = resp.read().decode("utf-8")
    except error.HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="ignore")
        raise RuntimeError(f"HTTP {exc.code}: {detail}") from exc
    except error.URLError as exc:
        raise RuntimeError(f"Network error: {exc}") from exc

    try:
        data = json.loads(body)
        return data["choices"][0]["message"]["content"]
    except Exception as exc:  # noqa: BLE001
        raise RuntimeError(f"Unexpected response: {body}") from exc


def main() -> int:
    args = parse_args()
    api_key = os.getenv("DASHSCOPE_API_KEY")
    if not api_key:
        print("ERROR: DASHSCOPE_API_KEY is not set.", file=sys.stderr)
        return 2

    skill_root = Path(args.skill_dir)
    all_skills = discover_skills(skill_root)

    if args.list_skills:
        if not all_skills:
            print(f"No skills found in: {skill_root}")
            return 0
        print(f"Discovered skills in {skill_root}:")
        for name in sorted(all_skills):
            print(f"- {name}")
        return 0

    selected_skills: dict[str, str] = {}
    if args.skill:
        for name in args.skill:
            if name not in all_skills:
                print(f"ERROR: skill not found: {name}", file=sys.stderr)
                return 3
            selected_skills[name] = all_skills[name]
    else:
        # default: apply all discovered skills so it's easy to verify behavior quickly
        selected_skills = dict(sorted(all_skills.items()))

    system_prompt = build_system_prompt(selected_skills)
    print(f"Using model: {args.model}")
    print(f"Using skills: {', '.join(selected_skills.keys()) if selected_skills else '(none)'}")
    print("-" * 60)
    print(f"User: {args.prompt}")
    print("-" * 60)
    answer = call_dashscope(args.base_url, api_key, args.model, system_prompt, args.prompt)
    print("Assistant:")
    print(answer)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
