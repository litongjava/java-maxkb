"""Refresh a provider catalog through the authenticated Java administration API.

Examples:
  python scripts/Sync-ModelCatalog.py --source openrouter --token-file TOKEN_FILE
  python scripts/Sync-ModelCatalog.py --source gitee --token-file TOKEN_FILE
Gitee discovery uses GITEE_API_KEY from the environment, never a command argument.
"""
import argparse
import json
import os
import re
import urllib.request
from pathlib import Path


def fetch(url, headers=None, body=None, method=None):
    data = None if body is None else json.dumps(body).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers=headers or {}, method=method)
    with urllib.request.urlopen(req, timeout=90) as response:
        return json.load(response)


def entries(source, data):
    result = []
    for model in data:
        name = model["id"]
        if name.endswith(":batch"):
            continue
        if source == "openrouter":
            modalities = model.get("architecture", {}).get("output_modalities", [])
            if "text" not in modalities or "image" in modalities or "audio" in modalities:
                continue
            kind = "LLM"
        else:
            # Gitee /models supplies no capability metadata. Only identify clear text
            # families; do not advertise image/audio/reranking models as chat models.
            lower = name.lower()
            if any(word in lower for word in ["rerank", "tts", "asr", "audio", "ocr", "omni", "guard"]):
                continue
            if "embedding" in lower or lower.startswith("bge-"):
                kind = "EMBEDDING"
            elif re.match(r"^(deepseek|qwen|glm|kimi|minimax|llama|gemma|mistral|internlm|yi-)", lower):
                kind = "LLM"
            else:
                continue
        result.append({"name": name, "model_type": kind,
                       "desc": model.get("name", name) + "（平台目录；实际可用性以当前密钥和保存校验为准）"})
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", choices=["openrouter", "gitee"], required=True)
    parser.add_argument("--token-file", type=Path, required=True)
    parser.add_argument("--api", default="http://localhost:10060/api")
    args = parser.parse_args()
    if args.source == "openrouter":
        remote = fetch("https://openrouter.ai/api/v1/models")
    else:
        key = os.environ.get("GITEE_API_KEY")
        if not key:
            parser.error("Set GITEE_API_KEY in the environment first")
        remote = fetch("https://ai.gitee.com/v1/models", {"Authorization": "Bearer " + key})
    models = entries(args.source, remote["data"])
    if not models:
        raise RuntimeError("Empty catalog: nothing updated")
    token = args.token_file.read_text(encoding="utf-8-sig").strip()
    result = fetch(args.api.rstrip("/") + "/provider/catalog",
                   {"Authorization": "Bearer " + token, "Content-Type": "application/json"},
                   {"provider": "model_" + args.source + "_provider", "models": models}, "PUT")
    if result.get("code") != 200:
        raise RuntimeError("Catalog update rejected: " + str(result.get("message")))
    print(json.dumps({"provider": args.source, "imported": len(models)}, ensure_ascii=False))


if __name__ == "__main__":
    main()
