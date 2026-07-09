param(
    [string]$WorkDir = (Join-Path $PSScriptRoot ".local\graphhopper"),
    [string]$BaseUrl = "http://127.0.0.1:8989",
    [string]$OutputFile = "sample-foot-route.json"
)

$ErrorActionPreference = "Stop"

New-Item -ItemType Directory -Force -Path $WorkDir | Out-Null

$query = "/route?profile=foot&point=43.7384,7.4246&point=43.7339,7.4213&locale=en&instructions=true&points_encoded=false&ch.disable=true"
$uri = "$BaseUrl$query"
$outputPath = Join-Path $WorkDir $OutputFile

Write-Host "Querying: $uri"
$response = Invoke-WebRequest -UseBasicParsing -Uri $uri
$response.Content | Set-Content -LiteralPath $outputPath -NoNewline

$json = $response.Content | ConvertFrom-Json
$path = $json.paths[0]

Write-Host "Saved: $outputPath"
Write-Host "Distance meters: $($path.distance)"
Write-Host "Duration ms: $($path.time)"
Write-Host "Instruction count: $($path.instructions.Count)"
