param([string]$Distribution = 'Ubuntu')
$ErrorActionPreference = 'Stop'
# Use Docker Engine inside WSL; Docker Desktop is not required.
$scriptFile = Join-Path $PSScriptRoot 'setup-python-sandbox.sh'
$linuxPath = (& wsl.exe --distribution $Distribution --user root --exec wslpath -a $scriptFile 2>$null)
if ($LASTEXITCODE -ne 0) {
  throw 'WSL Linux distribution is not ready. Enable VirtualMachinePlatform, restart Windows if requested, then run: wsl --install -d Ubuntu --no-launch'
}
& wsl.exe --distribution $Distribution --user root --exec bash $linuxPath.Trim()
if ($LASTEXITCODE -ne 0) {
  throw 'Docker Engine setup failed. Review the output before retrying.'
}
Write-Output "Set kb.python.wsl.distribution=$Distribution in maxkb-web/my.txt, then restart the Java service."
