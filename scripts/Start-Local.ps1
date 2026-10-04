# 在 Windows 上本地启动 Java MossKB：先确保后端 JAR 已构建，再后台启动 Java 与前端 Vite。
#
# 参数：
#   -Build             先执行 Maven 打包（跳过测试、Javadoc 与 GPG 签名）。
#   -JavaExecutable    指定 java 可执行文件，默认 PATH 上的 java。
#   -NodeExecutable    指定 node 可执行文件，默认 PATH 上的 node。
#   -MavenExecutable   指定 mvn 可执行文件，默认 PATH 上的 mvn.cmd。
#   -BackendPort       后端端口，默认 10060。
#   -FrontendPort      前端端口，默认 3000。
#
# 示例：
#   .\scripts\Start-Local.ps1 -Build
#   .\scripts\Start-Local.ps1

param(
  [string]$JavaExecutable = 'java',
  [string]$NodeExecutable = 'node',
  [string]$MavenExecutable = 'mvn.cmd',
  [int]$BackendPort = 10060,
  [int]$FrontendPort = 3000,
  [switch]$Build
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$webRoot = Join-Path $projectRoot 'mosskb-web'
$uiRoot = Join-Path $projectRoot '../java-mosskb-ui'

if (!(Test-Path (Join-Path $webRoot 'my.txt')) -or !(Test-Path (Join-Path $webRoot 'secrets.txt'))) {
  throw 'Create mosskb-web/my.txt and mosskb-web/secrets.txt before starting.'
}
if (!(Test-Path (Join-Path $uiRoot 'package.json'))) {
  throw 'Frontend not found. Expected the java-mosskb-ui directory next to java-mosskb.'
}

if ($Build) {
  Push-Location $projectRoot
  try {
    & $MavenExecutable clean '-Dmaven.test.skip=true' '-Dmaven.javadoc.skip=true' '-Dgpg.skip=true' -Pproduction package
    if ($LASTEXITCODE -ne 0) {
      throw 'Backend build failed.'
    }
  } finally {
    Pop-Location
  }
}

$jar = Get-ChildItem (Join-Path $webRoot 'target') -Filter 'mosskb-web-*.jar' | Where-Object { $_.Name -notmatch '-(sources|javadoc)\.jar$' } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (!$jar) {
  throw 'No backend jar found. Run this script with -Build first.'
}
if (!(Test-Path (Join-Path $uiRoot 'node_modules/vite/bin/vite.js'))) {
  throw 'Run npm install in java-mosskb-ui first.'
}

New-Item -ItemType Directory -Force (Join-Path $webRoot 'logs'),(Join-Path $uiRoot '.local') | Out-Null

$backend = Get-NetTCPConnection -LocalPort $BackendPort -State Listen -ErrorAction SilentlyContinue
if (!$backend) {
  $backendProcess = Start-Process -FilePath $JavaExecutable -ArgumentList '-jar',('"'+$jar.FullName+'"'),"--server.port=$BackendPort" -WorkingDirectory $webRoot -RedirectStandardOutput (Join-Path $webRoot 'logs/startup.log') -RedirectStandardError (Join-Path $webRoot 'logs/startup-error.log') -WindowStyle Hidden -PassThru
  Set-Content (Join-Path $webRoot 'logs/backend.pid') $backendProcess.Id
}

$frontend = Get-NetTCPConnection -LocalPort $FrontendPort -State Listen -ErrorAction SilentlyContinue
if (!$frontend) {
  $previousTarget = $env:VITE_PROXY_TARGET
  try {
    $env:VITE_PROXY_TARGET = "http://127.0.0.1:$BackendPort"
    $frontendProcess = Start-Process -FilePath $NodeExecutable -ArgumentList 'node_modules/vite/bin/vite.js','--host','127.0.0.1','--port',"$FrontendPort" -WorkingDirectory $uiRoot -RedirectStandardOutput (Join-Path $uiRoot '.local/vite.log') -RedirectStandardError (Join-Path $uiRoot '.local/vite-error.log') -WindowStyle Hidden -PassThru
    Set-Content (Join-Path $uiRoot '.local/frontend.pid') $frontendProcess.Id
  } finally {
    $env:VITE_PROXY_TARGET = $previousTarget
  }
}

Write-Output "UI: http://localhost:$FrontendPort/ui/  Backend: http://localhost:$BackendPort"
Write-Output 'Existing listeners are reused; check the startup logs if either page does not open.'
