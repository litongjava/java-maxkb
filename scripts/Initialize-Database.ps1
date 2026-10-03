param(
  [Parameter(Mandatory=$true)][string]$PostgresBin,
  [string]$ConfigFile = (Join-Path $PSScriptRoot '../maxkb-web/my.txt')
)
$ErrorActionPreference = 'Stop'
$settings = @{}
Get-Content -LiteralPath $ConfigFile | ForEach-Object {
  if ($_ -match '^([^#=]+)=(.*)$') {
    $settings[$matches[1].Trim()] = $matches[2].Trim()
  }
}
$databaseUri = [uri]($settings['jdbc.url'] -replace '^jdbc:', '')
$previousPassword = $env:PGPASSWORD
try {
  $env:PGPASSWORD = $settings['jdbc.pswd']
  foreach ($migration in @('init-db.sql', '002-gitee-models.sql', '003-filename-capacity.sql', '004-keyword-search.sql', '005-agent-context.sql', '006-python-functions.sql', '007-user-sessions.sql', '008-model-catalog.sql', '009-model-catalog-snapshot.sql', '010-api-keys.sql', '011-share-link.sql')) {
    & (Join-Path $PostgresBin 'psql.exe') -X -w -h $databaseUri.Host -p $databaseUri.Port -U $settings['jdbc.user'] -d $databaseUri.AbsolutePath.TrimStart('/') -v ON_ERROR_STOP=1 -f (Join-Path $PSScriptRoot $migration)
    if ($LASTEXITCODE -ne 0) {
      throw "Database migration failed: $migration"
    }
  }
} finally {
  $env:PGPASSWORD = $previousPassword
}
Write-Output 'Database schema and Gitee model entries are ready.'
