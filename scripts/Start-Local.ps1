param(
  [string]$JavaExecutable = 'java',
  [string]$NodeExecutable = 'node',
  [string]$MavenExecutable = 'mvn.cmd',
  [int]$BackendPort = 10060,
  [switch]$Build
)
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$webRoot = Join-Path $projectRoot 'maxkb-web'
$uiRoot = (Resolve-Path (Join-Path $projectRoot '../MaxKB/ui')).Path
if (!(Test-Path (Join-Path $webRoot 'my.txt')) -or !(Test-Path (Join-Path $webRoot 'secrets.txt'))) {
  throw 'Create maxkb-web/my.txt and maxkb-web/secrets.txt before starting.'
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
$jar = Get-ChildItem (Join-Path $webRoot 'target') -Filter 'maxkb-web-*.jar' | Where-Object { $_.Name -notmatch '-(sources|javadoc)\.jar$' } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (!$jar) {
  throw 'No backend jar found. Run this script with -Build first.'
}
if (!(Test-Path (Join-Path $uiRoot 'node_modules/vite/bin/vite.js'))) {
  throw 'Run npm install in MaxKB/ui first.'
}
New-Item -ItemType Directory -Force (Join-Path $webRoot 'logs'),(Join-Path $uiRoot '.local') | Out-Null
$backend = Get-NetTCPConnection -LocalPort $BackendPort -State Listen -ErrorAction SilentlyContinue
if (!$backend) {
  $backendProcess = Start-Process -FilePath $JavaExecutable -ArgumentList '-jar',('"'+$jar.FullName+'"'),"--server.port=$BackendPort" -WorkingDirectory $webRoot -RedirectStandardOutput (Join-Path $webRoot 'logs/startup.log') -RedirectStandardError (Join-Path $webRoot 'logs/startup-error.log') -WindowStyle Hidden -PassThru
  Set-Content (Join-Path $webRoot 'logs/backend.pid') $backendProcess.Id
}
$frontend = Get-NetTCPConnection -LocalPort 3000 -State Listen -ErrorAction SilentlyContinue
if (!$frontend) {
  $previousTarget = $env:VITE_PROXY_TARGET
  try {
    $env:VITE_PROXY_TARGET = "http://127.0.0.1:$BackendPort"
    $frontendProcess = Start-Process -FilePath $NodeExecutable -ArgumentList 'node_modules/vite/bin/vite.js','--host','127.0.0.1' -WorkingDirectory $uiRoot -RedirectStandardOutput (Join-Path $uiRoot '.local/vite.log') -RedirectStandardError (Join-Path $uiRoot '.local/vite-error.log') -WindowStyle Hidden -PassThru
    Set-Content (Join-Path $uiRoot '.local/frontend.pid') $frontendProcess.Id
  } finally {
    $env:VITE_PROXY_TARGET = $previousTarget
  }
}
Write-Output "UI: http://localhost:3000/ui/  Backend: http://localhost:$BackendPort"
Write-Output 'Existing listeners are reused; check the startup logs if either page does not open.'
