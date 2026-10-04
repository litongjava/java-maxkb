param(
  [string]$DockerExecutable,
  [string]$DockerHost,
  [string]$Distribution,
  [string]$Image
)
$ErrorActionPreference = 'Stop'

# 默认按 mosskb-web/my.txt 的实际配置验证,命令行参数只用于临时覆盖。
$myTxt = Join-Path $PSScriptRoot '../mosskb-web/my.txt'
function Get-MySetting([string]$key) {
  if (!(Test-Path $myTxt)) {
    return $null
  }
  foreach ($line in Get-Content $myTxt) {
    $trimmed = $line.Trim()
    if ($trimmed.StartsWith('#') -or !$trimmed.Contains('=')) {
      continue
    }
    $name, $value = $trimmed.Split('=', 2)
    if ($name.Trim() -eq $key) {
      return $value.Trim()
    }
  }
  return $null
}
if (!$DockerExecutable) { $DockerExecutable = Get-MySetting 'kb.python.docker' }
if (!$DockerHost) { $DockerHost = Get-MySetting 'kb.python.docker.host' }
if (!$Distribution) { $Distribution = Get-MySetting 'kb.python.wsl.distribution' }
if (!$Image) { $Image = Get-MySetting 'kb.python.image' }
if (!$DockerExecutable -and !$Distribution) { $DockerExecutable = 'docker' }
if (!$Image) { $Image = 'python:3.12-slim' }

# 与 IsolatedPythonExecutor.dockerCommand() 保持一致:先可执行文件(或 WSL 前缀),再 --host 全局参数。
$docker = @()
if ($Distribution) {
  $docker = @('wsl.exe', '--distribution', $Distribution, '--user', 'root', '--exec', 'docker')
} else {
  $docker = @($DockerExecutable)
}
if ($DockerHost) {
  $docker += @('--host', $DockerHost)
}

$runner = Get-Content -Raw -Encoding utf8 (Join-Path $PSScriptRoot '../mosskb-business/src/main/resources/python/runner.py')
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
$payload = @{ code = $code; params = @{} } | ConvertTo-Json -Compress
# PowerShell 向原生命令传递含引号的整段脚本会被剥引号,所以 runner 用 base64 传,内容里没有需要转义的字符。
$runnerBase64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($runner))
$bootstrap = "import base64;exec(base64.b64decode('$runnerBase64'))"
$prefix = @()
if ($docker.Count -gt 1) {
  $prefix = $docker[1..($docker.Count - 1)]
}
$arguments = $prefix + @(
  'run', '--rm', "--pull=never", '--network=none', '--read-only', '--cap-drop=ALL',
  '--security-opt=no-new-privileges', '--user=65534:65534', '--pids-limit=32',
  '--memory=256m', '--memory-swap=256m', '--cpus=1',
  '--tmpfs=/tmp:rw,noexec,nosuid,size=16m', '-i', $Image, 'python', '-I', '-B', '-c', $bootstrap)
Write-Output "engine: $($docker -join ' ')"
$output = $payload | & $docker[0] $arguments
if ($LASTEXITCODE -ne 0) {
  throw 'Python isolation verification did not complete.'
}
$result = $output | ConvertFrom-Json
if (-not $result.ok -or $result.data.uid -ne 65534 -or -not $result.data.secret_absent -or -not $result.data.read_only -or -not $result.data.network_blocked) {
  throw 'Container did not satisfy the required isolation checks.'
}
$result.data | ConvertTo-Json
