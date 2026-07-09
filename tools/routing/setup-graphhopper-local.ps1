param(
    [string]$WorkDir = (Join-Path $PSScriptRoot ".local\graphhopper"),
    [string]$JavaExe = "C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot\bin\java.exe",
    [string]$GraphHopperVersion = "11.0",
    [string]$OsmPbfUrl = "https://download.geofabrik.de/europe/monaco-latest.osm.pbf"
)

$ErrorActionPreference = "Stop"

function Save-UrlIfMissing {
    param(
        [Parameter(Mandatory = $true)][string]$Url,
        [Parameter(Mandatory = $true)][string]$Path
    )

    if (Test-Path -LiteralPath $Path) {
        Write-Host "Exists: $Path"
        return
    }

    Write-Host "Downloading: $Url"
    Invoke-WebRequest -UseBasicParsing -Uri $Url -OutFile $Path
}

if (!(Test-Path -LiteralPath $JavaExe)) {
    throw "JDK 21 java.exe not found at '$JavaExe'. Install Eclipse Temurin 21 or pass -JavaExe."
}

New-Item -ItemType Directory -Force -Path $WorkDir | Out-Null

$jarName = "graphhopper-web-$GraphHopperVersion.jar"
$jarPath = Join-Path $WorkDir $jarName
$jarUrl = "https://repo1.maven.org/maven2/com/graphhopper/graphhopper-web/$GraphHopperVersion/$jarName"
Save-UrlIfMissing -Url $jarUrl -Path $jarPath

$pbfName = Split-Path -Leaf $OsmPbfUrl
$pbfPath = Join-Path $WorkDir $pbfName
Save-UrlIfMissing -Url $OsmPbfUrl -Path $pbfPath

$configUrl = "https://raw.githubusercontent.com/graphhopper/graphhopper/$GraphHopperVersion/config-example.yml"
$configPath = Join-Path $WorkDir "config.yml"
Save-UrlIfMissing -Url $configUrl -Path $configPath

$builtInModelNames = @(
    "car.json",
    "foot.json",
    "foot_elevation.json",
    "bike.json",
    "bike_elevation.json"
)

foreach ($model in $builtInModelNames) {
    $localModel = Join-Path $WorkDir $model
    if (Test-Path -LiteralPath $localModel) {
        Remove-Item -LiteralPath $localModel -Force
        Write-Host "Removed local built-in model shadow: $localModel"
    }
}

$config = Get-Content -Raw -LiteralPath $configPath
$config = $config -replace '(?m)^  datareader\.file:.*$', "  datareader.file: `"$pbfName`""
$config = $config -replace '(?m)^  graph\.location:.*$', '  graph.location: graph-cache'
$config = $config -replace '(?m)^  graph\.elevation\.provider:.*$', '  graph.elevation.provider: ""'
$config = $config -replace '(?m)^  import\.osm\.ignored_highways:.*$', "  import.osm.ignored_highways: ''"
$config = $config -replace '(?ms)^  profiles:.*?^  profiles_ch:', @"
  profiles:
   - name: car
     custom_model_files: [car.json]
   - name: foot
     custom_model_files: [foot.json]

  profiles_ch:
"@
$config = $config -replace '(?m)^  graph\.encoded_values:.*$', '  graph.encoded_values: car_access, car_average_speed, road_access, road_class, road_environment, roundabout, foot_access, foot_priority, foot_average_speed, foot_road_access, hike_rating, mtb_rating, country, ferry_speed'
Set-Content -LiteralPath $configPath -Value $config -NoNewline

$graphCache = Join-Path $WorkDir "graph-cache"
if (Test-Path -LiteralPath $graphCache) {
    Remove-Item -LiteralPath $graphCache -Recurse -Force
    Write-Host "Removed graph cache so profile/config changes are re-imported: $graphCache"
}

& $JavaExe -version

Write-Host ""
Write-Host "GraphHopper local setup written to: $WorkDir"
Write-Host "Start server:"
Write-Host ".\tools\routing\start-graphhopper-local.ps1"
