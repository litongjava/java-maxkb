param([string]$Distribution = 'Ubuntu')
$ErrorActionPreference = 'Stop'
$runner = Get-Content -Raw -Encoding utf8 (Join-Path $PSScriptRoot '../maxkb-business/src/main/resources/python/runner.py')
$code = @'
def main():
    import os, socket
    checks = {"uid": os.getuid(), "secret_absent": "GITEE_API_KEY" not in os.environ}
    try:
        open("/sandbox-write-test", "w").write("blocked")
        checks["read_only"] = False
    except OSError:
        checks["read_only"] = True
    try:
        socket.create_connection(("1.1.1.1", 443), timeout=1)
        checks["network_blocked"] = False
    except OSError:
        checks["network_blocked"] = True
    return checks
'@
$payload = @{ code=$code; params=@{} } | ConvertTo-Json -Compress
$output = $payload | & wsl.exe --distribution $Distribution --user root --exec docker run --rm --pull=never --network=none --read-only --cap-drop=ALL --security-opt=no-new-privileges --user=65534:65534 --pids-limit=32 --memory=256m --memory-swap=256m --cpus=1 --tmpfs=/tmp:rw,noexec,nosuid,size=16m -i python:3.12-slim python -I -B -c $runner
if ($LASTEXITCODE -ne 0) {
  throw 'Python isolation verification did not complete.'
}
$result = $output | ConvertFrom-Json
if (-not $result.ok -or $result.data.uid -ne 65534 -or -not $result.data.secret_absent -or -not $result.data.read_only -or -not $result.data.network_blocked) {
  throw 'Container did not satisfy the required isolation checks.'
}
$result.data | ConvertTo-Json
