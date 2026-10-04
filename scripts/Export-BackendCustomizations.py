"""Export Java source/migration customizations without altering the working index."""
import argparse
import hashlib
import json
import subprocess
import tempfile
from pathlib import Path


def git(repo, *args):
    return subprocess.run(["git", "-C", str(repo), *args], check=True,
                          stdout=subprocess.PIPE, stderr=subprocess.PIPE).stdout


def eligible(name):
    source = name.startswith(("mosskb-business/src/", "mosskb-web/src/", "scripts/"))
    return source and Path(name).suffix in {".java", ".sql", ".py", ".ps1", ".sh"}


def normalized(value):
    return value.replace(b"\r\n", b"\n")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", default=".")
    parser.add_argument("--base", default="HEAD")
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    repo, output = Path(args.repo).resolve(), Path(args.output).resolve()
    base = git(repo, "rev-parse", args.base).decode().strip()
    tracked = [p for p in git(repo, "diff", "--name-only", base).decode().splitlines() if eligible(p)]
    additions = [p for p in git(repo, "ls-files", "--others", "--exclude-standard").decode().splitlines() if eligible(p)]
    files = sorted(set(tracked + additions))
    sandbox = Path(tempfile.mkdtemp(prefix="mosskb-backend-check-"))
    git(sandbox, "init", "-q")
    for name in tracked:
        previous = subprocess.run(["git", "-C", str(repo), "show", f"{base}:{name}"], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if previous.returncode == 0:
            target = sandbox / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(previous.stdout)
    patch = git(repo, "diff", "--binary", "--full-index", base, "--", *tracked) if tracked else b""
    for name in additions:
        target = sandbox / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(normalized((repo / name).read_bytes()))
        git(sandbox, "add", "--", name)
    if additions:
        patch += git(sandbox, "diff", "--cached", "--binary", "--full-index")
        for name in additions:
            (sandbox / name).unlink()
    output.mkdir(parents=True, exist_ok=True)
    patchfile = output / "java-backend.patch"
    patchfile.write_bytes(patch)
    if patch:
        git(sandbox, "apply", "--check", str(patchfile))
        git(sandbox, "apply", str(patchfile))
    manifest = []
    for name in files:
        original, restored = repo / name, sandbox / name
        if original.exists():
            data = normalized(original.read_bytes())
            if not restored.exists() or normalized(restored.read_bytes()) != data:
                raise RuntimeError("Restored content differs: " + name)
            manifest.append({"file": name, "sha256_lf": hashlib.sha256(data).hexdigest()})
        elif restored.exists():
            raise RuntimeError("Deleted file still exists: " + name)
    (output / "java-backend-manifest.json").write_text(json.dumps({
        "base": base, "scope": "Java source, tests, migrations and scripts; excludes local credentials, properties, POM and docs",
        "verified": True, "files": manifest}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Exported and verified {len(manifest)} backend files.")


if __name__ == "__main__":
    main()
