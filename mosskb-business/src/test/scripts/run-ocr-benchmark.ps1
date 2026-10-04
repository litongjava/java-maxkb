param(
  [Parameter(Mandatory = $true)][string]$Pdf,
  [string]$OutputDirectory = 'target/ocr-benchmark/current',
  [string]$Models = '',
  [int]$TimeoutMinutes = 45
)

$ErrorActionPreference = 'Stop'
$pdfFile = (Resolve-Path -LiteralPath $Pdf).Path
$moduleDirectory = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../../..')).Path
Push-Location -LiteralPath $moduleDirectory
try {
  & mvn -B test-compile dependency:build-classpath '-DskipTests' '-Dmdep.outputFile=target/ocr-classpath.txt'
  if ($LASTEXITCODE -ne 0) { throw 'Maven compilation failed' }
  $benchmarkClasspath = 'target/test-classes;target/classes;' + (Get-Content -LiteralPath 'target/ocr-classpath.txt' -Raw).Trim()
  $jvmArguments = @(
    '-Dfile.encoding=UTF-8',
    '-Dsun.stdout.encoding=UTF-8',
    '-Dsun.stderr.encoding=UTF-8',
    '-Docr.benchmark=true',
    "-Docr.pdf=$pdfFile",
    "-Docr.output=$OutputDirectory",
    "-Docr.timeoutMinutes=$TimeoutMinutes"
  )
  if ($Models) { $jvmArguments += "-Docr.models=$Models" }
  & java @jvmArguments -cp $benchmarkClasspath nexus.io.mosskb.ocr.DocumentOcrBenchmarkTest
  if ($LASTEXITCODE -ne 0) { throw 'Benchmark stopped; inspect saved state and raw responses before retrying' }
} finally {
  Pop-Location
}
