"""Export frontend differences without touching the source repository index."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile


def git(repo, *args):
    return subprocess.run(['git', '-C', str(repo), *args], check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE).stdout


def normalized(data):
    return data.replace(b'\r\n', b'\n')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--repo', required=True)
    parser.add_argument('--base', required=True)
    parser.add_argument('--output', required=True)
    args = parser.parse_args()
    repo, output = Path(args.repo).resolve(), Path(args.output).resolve()
    output.mkdir(parents=True, exist_ok=True)
    base = git(repo, 'rev-parse', args.base).decode().strip()
    files = git(repo, 'diff', '--name-only', base, '--', 'ui').decode().splitlines()
    untracked = git(repo, 'ls-files', '--others', '--exclude-standard', '--', 'ui').decode().splitlines()
    # Only source additions are eligible; do not export arbitrary local files or credentials.
    additions = [p for p in untracked if p.startswith('ui/src/') and Path(p).suffix in {'.vue', '.ts', '.js', '.css', '.scss'}]
    core = git(repo, 'diff', '--binary', '--full-index', base, '--', 'ui', ':(exclude)ui/yarn.lock')
    sandbox = Path(tempfile.mkdtemp(prefix='maxkb-patch-check-'))
    git(sandbox, 'init', '-q')
    for name in files:
        result = subprocess.run(['git', '-C', str(repo), 'show', f'{base}:{name}'], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if result.returncode == 0:
            target = sandbox / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(result.stdout)
    # Generate new-file patches through a disposable index, never through the user's merge index.
    for name in additions:
        target = sandbox / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(normalized((repo / name).read_bytes()))
        git(sandbox, 'add', '--', name)
    if additions:
        core += git(sandbox, 'diff', '--cached', '--binary', '--full-index')
        for name in additions:
            (sandbox / name).unlink()
    (output / 'frontend-core.patch').write_bytes(core)
    lock = git(repo, 'diff', '--binary', '--full-index', base, '--', 'ui/yarn.lock')
    (output / 'frontend-lock.patch').write_bytes(lock)
    for patch in ['frontend-core.patch', 'frontend-lock.patch']:
        if (output / patch).stat().st_size:
            git(sandbox, 'apply', '--check', str(output / patch))
            git(sandbox, 'apply', str(output / patch))
    manifest = []
    for name in sorted(set(files + additions)):
        original, restored = repo / name, sandbox / name
        if original.exists():
            data = normalized(original.read_bytes())
            if not restored.exists() or normalized(restored.read_bytes()) != data:
                raise RuntimeError(f'Restored content differs: {name}')
            manifest.append({'file': name, 'sha256_lf': hashlib.sha256(data).hexdigest()})
        elif restored.exists():
            raise RuntimeError(f'Deleted file still exists: {name}')
    (output / 'environment.example').write_text('VITE_API_BASE_URL=/api\nVITE_PROXY_TARGET=http://127.0.0.1:10060\n', encoding='utf-8')
    metadata = {'base': base, 'base_description': 'Current fork merge baseline; not certified as an official upstream commit',
                'head_at_export': git(repo, 'rev-parse', 'HEAD').decode().strip(),
                'verified': 'git apply --check, apply and normalized content comparison in disposable directory',
                'files': manifest}
    (output / 'manifest.json').write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(f'Exported and verified {len(manifest)} frontend files against {base}.')
    print(f'Validation directory: {sandbox}')


if __name__ == '__main__':
    main()
