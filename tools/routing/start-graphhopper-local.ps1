param(
    [string]$WorkDir = (Join-Path $PSScriptRoot ".local\graphhopper"),
    [string]$JavaExe = "C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot\bin\java.exe",
    [string]$GraphHopperVersion = "11.0",
    [int]$Port = 8989
)

$ErrorActionPreference = "Stop"

$jarPath = Join-Path $WorkDir "graphhopper-web-$GraphHopperVersion.jar"
$configPath = Join-Path $WorkDir "config.yml"

if (!(Test-Path -LiteralPath $JavaExe)) {
    throw "JDK 21 java.exe not found at '$JavaExe'."
}

if (!(Test-Path -LiteralPath $jarPath)) {
    throw "GraphHopper jar missing. Run tools\routing\setup-graphhopper-local.ps1 first."
}

if (!(Test-Path -LiteralPath $configPath)) {
    throw "GraphHopper config missing. Run tools\routing\setup-graphhopper-local.ps1 first."
}

Push-Location $WorkDir
try {
    Write-Host "Starting GraphHopper on http://127.0.0.1:$Port"
    & $JavaExe -Xmx2g -jar $jarPath server $configPath
}
finally {
    Pop-Location
}
