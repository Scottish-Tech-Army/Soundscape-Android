param(
    [string]$AndroidSdk = (Join-Path $env:LOCALAPPDATA "Android\Sdk"),
    [string]$Serial = "",
    [string]$PackageName = "org.scottishtecharmy.soundscape",
    [string]$GraphHopperBaseUrl = "http://127.0.0.1:8989",
    [double]$StartLatitude = 43.7384,
    [double]$StartLongitude = 7.4246,
    [double]$DestinationLatitude = 43.7339,
    [double]$DestinationLongitude = 7.4213,
    [double]$OffRouteLatitude = 43.7440,
    [double]$OffRouteLongitude = 7.4260,
    [double]$SecondOffRouteLatitude = 43.7441,
    [double]$SecondOffRouteLongitude = 7.4261,
    [int]$UiTimeoutSeconds = 30,
    [int]$RouteReadyTimeoutSeconds = 30,
    [switch]$SkipBuild,
    [switch]$EnableTalkBack
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$adb = Join-Path $AndroidSdk "platform-tools\adb.exe"
$gradle = Join-Path $repoRoot "gradlew.bat"
$appApk = Join-Path $repoRoot "app\build\outputs\apk\debug\app-debug.apk"
$debugSetLocationAction = "$PackageName.DEBUG_SET_LOCATION"
$adbTarget = @()
if ($Serial -ne "") {
    $adbTarget = @("-s", $Serial)
}

function Invoke-Adb {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)

    & $adb @adbTarget @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "adb failed: $($Arguments -join ' ')"
    }
}

function Invoke-AdbAllowFailure {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)

    & $adb @adbTarget @Arguments | Out-Null
}

function Invoke-AdbQuiet {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)

    & $adb @adbTarget @Arguments *> $null
    if ($LASTEXITCODE -ne 0) {
        throw "adb failed: $($Arguments -join ' ')"
    }
}

function Set-EmulatorLocation {
    param(
        [Parameter(Mandatory = $true)][double]$Latitude,
        [Parameter(Mandatory = $true)][double]$Longitude,
        [int]$Repeat = 1,
        [int]$DelayMilliseconds = 1200
    )

    for ($i = 0; $i -lt $Repeat; $i++) {
        Invoke-Adb @("emu", "geo", "fix", "$Longitude", "$Latitude")
        if ($i -lt ($Repeat - 1)) {
            Start-Sleep -Milliseconds $DelayMilliseconds
        }
    }
}

function Set-AppDebugLocation {
    param(
        [Parameter(Mandatory = $true)][double]$Latitude,
        [Parameter(Mandatory = $true)][double]$Longitude
    )

    Invoke-Adb @(
        "shell",
        "am",
        "startservice",
        "-a",
        $debugSetLocationAction,
        "-n",
        "$PackageName/.services.SoundscapeService",
        "--ed",
        "latitude",
        "$Latitude",
        "--ed",
        "longitude",
        "$Longitude"
    )
}

function Test-GraphHopperRoute {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][double]$Latitude,
        [Parameter(Mandatory = $true)][double]$Longitude
    )

    $query = "/route?profile=foot&point=$Latitude,$Longitude&point=$DestinationLatitude,$DestinationLongitude&locale=en&instructions=true&points_encoded=false&ch.disable=true"
    $uri = "$GraphHopperBaseUrl$query"

    Write-Host "Checking GraphHopper $Name route: $uri"
    $response = Invoke-WebRequest -UseBasicParsing -Uri $uri -TimeoutSec 10
    $json = $response.Content | ConvertFrom-Json
    if ($null -eq $json.paths -or $json.paths.Count -eq 0) {
        throw "GraphHopper returned no route paths for $Name."
    }
    if ($null -eq $json.paths[0].instructions -or $json.paths[0].instructions.Count -eq 0) {
        throw "GraphHopper returned no route instructions for $Name."
    }
    Write-Host "GraphHopper $Name route OK: $([math]::Round($json.paths[0].distance)) meters, $($json.paths[0].instructions.Count) instructions"
}

function Test-GraphHopper {
    Test-GraphHopperRoute -Name "start" -Latitude $StartLatitude -Longitude $StartLongitude
    Test-GraphHopperRoute -Name "off-route" -Latitude $OffRouteLatitude -Longitude $OffRouteLongitude
    Test-GraphHopperRoute -Name "second off-route" -Latitude $SecondOffRouteLatitude -Longitude $SecondOffRouteLongitude
}

function Get-Logcat {
    return [string]::Join("`n", (& $adb @adbTarget @("logcat", "-d", "-v", "brief")))
}

function Get-SmokeLogcat {
    $patterns = @(
        "Turn-by-turn",
        "GraphHopper",
        "LocationDetailsViewModel",
        "SoundscapeService",
        "SoundscapeServiceConnection",
        "SoundscapeIntents",
        "MainActivity"
    )
    $pattern = $patterns -join "|"
    return [string]::Join("`n", (
        & $adb @adbTarget @("logcat", "-d", "-v", "brief") |
            Select-String -Pattern $pattern |
            ForEach-Object { $_.Line }
    ))
}

function Get-LogTail {
    param([Parameter(Mandatory = $true)][string]$Log)

    return ($Log -split "`n" | Select-Object -Last 80) -join "`n"
}

function Wait-ForLog {
    param(
        [Parameter(Mandatory = $true)][string]$Pattern,
        [Parameter(Mandatory = $true)][int]$TimeoutSeconds
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $log = Get-Logcat
        if ($log -match $Pattern) {
            return $true
        }
        Start-Sleep -Seconds 1
    }
    return $false
}

function Get-UiXml {
    Invoke-AdbQuiet @("shell", "uiautomator", "dump", "/sdcard/window.xml")
    return [string]::Join("`n", (& $adb @adbTarget @("exec-out", "cat", "/sdcard/window.xml")))
}

function Wait-ForResourceBounds {
    param(
        [Parameter(Mandatory = $true)][string[]]$ResourceIds,
        [Parameter(Mandatory = $true)][int]$TimeoutSeconds
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            [xml]$xml = Get-UiXml
            foreach ($resourceId in $ResourceIds) {
                $node = $xml.SelectSingleNode("//*[@resource-id='$resourceId']")
                if ($null -ne $node) {
                    $bounds = $node.bounds
                    if ($bounds -match "\[(\d+),(\d+)\]\[(\d+),(\d+)\]") {
                        $left = [int]$Matches[1]
                        $top = [int]$Matches[2]
                        $right = [int]$Matches[3]
                        $bottom = [int]$Matches[4]
                        return @{
                            X = [int](($left + $right) / 2)
                            Y = [int](($top + $bottom) / 2)
                        }
                    }
                }
            }
        } catch {
            Start-Sleep -Seconds 1
        }
        Start-Sleep -Seconds 1
    }

    throw "Resource id not found: $($ResourceIds -join ', ')"
}

function Set-TestPreferences {
    $prefsFile = "$PackageName`_preferences.xml"
    $tmpPrefs = Join-Path ([System.IO.Path]::GetTempPath()) "soundscape-local-smoke-preferences.xml"
    $prefsXml = @"
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <boolean name="FirstLaunch" value="false" />
    <boolean name="ShowMap" value="false" />
    <string name="LastNewRelease">local-smoke</string>
</map>
"@

    Set-Content -LiteralPath $tmpPrefs -Value $prefsXml -Encoding UTF8
    Invoke-AdbQuiet @("push", $tmpPrefs, "/data/local/tmp/soundscape-local-smoke-preferences.xml")
    Invoke-AdbQuiet @("shell", "chmod", "644", "/data/local/tmp/soundscape-local-smoke-preferences.xml")
    Invoke-AdbQuiet @("shell", "run-as", $PackageName, "mkdir", "-p", "shared_prefs")
    Invoke-AdbQuiet @("shell", "run-as", $PackageName, "cp", "/data/local/tmp/soundscape-local-smoke-preferences.xml", "shared_prefs/$prefsFile")
}

function Grant-TestPermissions {
    $permissions = @(
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.RECORD_AUDIO"
    )

    foreach ($permission in $permissions) {
        Invoke-AdbAllowFailure @("shell", "pm", "grant", $PackageName, $permission)
    }
    Invoke-AdbAllowFailure @("shell", "dumpsys", "deviceidle", "whitelist", "+$PackageName")
}

function Enable-TalkBackIfRequested {
    if (!$EnableTalkBack) {
        return $false
    }

    $packages = [string]::Join("`n", (& $adb @adbTarget @("shell", "pm", "list", "packages", "com.google.android.marvin.talkback")))
    if ($packages -notmatch "com.google.android.marvin.talkback") {
        throw "TalkBack package missing on emulator."
    }

    Invoke-Adb @("shell", "settings", "put", "secure", "enabled_accessibility_services", "com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService")
    Invoke-Adb @("shell", "settings", "put", "secure", "accessibility_enabled", "1")
    Invoke-AdbAllowFailure @("shell", "pm", "grant", "com.google.android.marvin.talkback", "android.permission.POST_NOTIFICATIONS")
    return $true
}

if (!(Test-Path -LiteralPath $adb)) {
    throw "adb.exe not found at '$adb'."
}

Test-GraphHopper

if (!$SkipBuild) {
    Write-Host "Building debug APK"
    & $gradle ":app:assembleDebug" "--console=plain"
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle assembleDebug failed."
    }
}

if (!(Test-Path -LiteralPath $appApk)) {
    throw "Debug APK missing: $appApk"
}

Write-Host "Installing app"
Invoke-Adb @("install", "-r", $appApk)
Set-TestPreferences
Grant-TestPermissions
$talkBackEnabledForSmoke = Enable-TalkBackIfRequested

Write-Host "Starting route smoke scenario"
Invoke-AdbAllowFailure @("shell", "am", "force-stop", $PackageName)
Set-EmulatorLocation -Latitude $StartLatitude -Longitude $StartLongitude -Repeat 2
Invoke-Adb @("logcat", "-c")
Invoke-Adb @(
    "shell",
    "am",
    "start",
    "-a",
    "android.intent.action.VIEW",
    "-d",
    "soundscape:$DestinationLatitude,$DestinationLongitude",
    "-p",
    $PackageName
)

$directionsResourceIds = @(
    "locationDetailsStartDirections",
    "$PackageName`:id/locationDetailsStartDirections"
)
$tapPoint = Wait-ForResourceBounds -ResourceIds $directionsResourceIds -TimeoutSeconds $UiTimeoutSeconds
if (!(Wait-ForLog -Pattern "LocationDetailsViewModel.*serviceBoundState true|SoundscapeServiceConnection.*onServiceConnected" -TimeoutSeconds $UiTimeoutSeconds)) {
    $tail = Get-LogTail -Log (Get-SmokeLogcat)
    throw "Service did not bind before directions activation. Log tail:`n$tail"
}
Set-EmulatorLocation -Latitude $StartLatitude -Longitude $StartLongitude -Repeat 2
Set-AppDebugLocation -Latitude $StartLatitude -Longitude $StartLongitude
if (!(Wait-ForLog -Pattern "Debug static location set: $StartLatitude,$StartLongitude" -TimeoutSeconds 10)) {
    $tail = Get-LogTail -Log (Get-SmokeLogcat)
    throw "Debug start location was not applied. Log tail:`n$tail"
}
Start-Sleep -Seconds 2

Write-Host "Activating Start Directions at $($tapPoint.X),$($tapPoint.Y)"
if ($talkBackEnabledForSmoke) {
    Invoke-Adb @("shell", "input", "tap", "$($tapPoint.X)", "$($tapPoint.Y)")
    Start-Sleep -Milliseconds 500
    Invoke-Adb @("shell", "input", "tap", "$($tapPoint.X)", "$($tapPoint.Y)")
    Start-Sleep -Milliseconds 80
    Invoke-Adb @("shell", "input", "tap", "$($tapPoint.X)", "$($tapPoint.Y)")
} else {
    Invoke-Adb @("shell", "input", "tap", "$($tapPoint.X)", "$($tapPoint.Y)")
}

if (!(Wait-ForLog -Pattern "Turn-by-turn route ready" -TimeoutSeconds $RouteReadyTimeoutSeconds)) {
    $tail = Get-LogTail -Log (Get-SmokeLogcat)
    throw "Route did not become ready. Log tail:`n$tail"
}
Write-Host "Route start OK"

Write-Host "Testing silent reroute after off-route location persists"
Invoke-Adb @("logcat", "-c")
Set-AppDebugLocation -Latitude $OffRouteLatitude -Longitude $OffRouteLongitude
Start-Sleep -Seconds 6
Set-AppDebugLocation -Latitude $SecondOffRouteLatitude -Longitude $SecondOffRouteLongitude
Start-Sleep -Seconds 2

$rerouteLog = Get-Logcat
if ($rerouteLog -match "Turn-by-turn event: OffRoute") {
    throw "Off-route event was spoken/logged. Silent reroute expected."
}
if (!(Wait-ForLog -Pattern "Turn-by-turn event: Instruction" -TimeoutSeconds 20)) {
    $tail = Get-LogTail -Log (Get-SmokeLogcat)
    throw "No reroute instruction observed after off-route movement. Log tail:`n$tail"
}

Write-Host "Silent reroute OK"
Write-Host "Local turn-by-turn smoke passed"
