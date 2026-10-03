param(
  [Parameter(Mandatory = $true)][string]$Repository,
  [Parameter(Mandatory = $true)][string]$Bundle,
  [switch]$IncludeLock,
  [switch]$Apply
)
$ErrorActionPreference = 'Stop'
$repoPath = (Resolve-Path -LiteralPath $Repository).Path
$bundlePath = (Resolve-Path -LiteralPath $Bundle).Path
$patches = @((Join-Path $bundlePath 'frontend-core.patch'))
if ($IncludeLock) {
  $patches += Join-Path $bundlePath 'frontend-lock.patch'
}
foreach ($patch in $patches) {
  if (!(Test-Path -LiteralPath $patch)) {
    throw "Patch not found: $patch"
  }
}
$merge = & git -C $repoPath rev-parse -q --verify MERGE_HEAD 2>$null
if ($LASTEXITCODE -eq 0) {
  throw 'Finish or resolve the existing merge before restoring customizations.'
}
& git -C $repoPath apply --check @patches
if ($LASTEXITCODE -ne 0) {
  throw 'Patch check failed. Review upstream changes manually; no changes were applied.'
}
if ($Apply) {
  & git -C $repoPath apply @patches
  if ($LASTEXITCODE -ne 0) {
    throw 'Patch application failed.'
  }
  Write-Output 'Applied. Review the diff, merge environment settings, install dependencies and run the UI build and acceptance checks.'
} else {
  Write-Output 'Check passed. Nothing changed. Use -Apply to apply after review.'
}
