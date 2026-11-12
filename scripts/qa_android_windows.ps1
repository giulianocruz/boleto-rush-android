#Requires -Version 5.1
<#
.SYNOPSIS
  Boleto Rush Android QA (Windows, ADB, no app-data deletion).
.DESCRIPTION
  Preflight is read-only. -Run starts only the independent .qa package.
  -Install explicitly installs/replaces the QA package without clearing data.
  -Capture saves a local screenshot, logs and metadata; nothing is uploaded.
.EXAMPLE
  .\qa_android_windows.ps1 -ApkPath ".\BoletoRush-2.3.10-RECIBOS-CORRETOS-QA.apk" -Install -Run -Capture
  .\qa_android_windows.ps1 -ApkPath ".\BoletoRush-2.3.10-RECIBOS-CORRETOS-QA.apk" -CollectOnly
#>
[CmdletBinding()]
param(
    [string]$ApkPath = "",
    [switch]$Install,
    [switch]$Run,
    [switch]$Capture,
    [switch]$CollectOnly,
    [string]$DeviceSerial = "",
    [string]$EvidenceDirectory = ""
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$qaPackage = 'br.com.proximaera.boletorush.qa'
$qaActivity = "$qaPackage/.MainActivity"

function Stop-QA([string]$text) {
    Write-Host ("[ERRO] " + $text) -ForegroundColor Red
    throw $text
}
function Invoke-Adb([string[]]$Arguments, [switch]$AcceptNonzero) {
    $argsList = @()
    if ($script:selectedSerial) { $argsList += @('-s', $script:selectedSerial) }
    $argsList += $Arguments
    $result = & $script:adbExe @argsList 2>&1
    $exitCode = $LASTEXITCODE
    if ($exitCode -ne 0 -and -not $AcceptNonzero) {
        Stop-QA ("ADB command failed (code $exitCode): adb " + ($Arguments -join ' ') +
                 " | " + (($result | Out-String).Trim()))
    }
    return $result
}
function Capture-Png([string]$target) {
    # Streaming binary through cmdlets in Windows PowerShell 5.1 can corrupt PNG.
    # Copy the raw ADB stdout stream directly instead, without touching /sdcard.
    $info = New-Object System.Diagnostics.ProcessStartInfo
    $info.FileName = $script:adbExe
    $info.Arguments = "-s $script:selectedSerial exec-out screencap -p"
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $info
    $file = $null
    try {
        if (-not $process.Start()) { Stop-QA "ADB screenshot process failed to start" }
        $file = [System.IO.File]::Open($target, [System.IO.FileMode]::CreateNew)
        $process.StandardOutput.BaseStream.CopyTo($file)
        $file.Close()
        $file = $null
        $errorText = $process.StandardError.ReadToEnd()
        $process.WaitForExit()
        if ($process.ExitCode -ne 0) { throw "Screenshot failed: $errorText" }
        $bytes = [System.IO.File]::ReadAllBytes($target)
        if ($bytes.Length -lt 8 -or $bytes[0] -ne 137 -or
            $bytes[1] -ne 80 -or $bytes[2] -ne 78 -or $bytes[3] -ne 71) {
            throw 'ADB screenshot is not a valid PNG'
        }
    } catch {
        if ($file) { $file.Close() }
        if (Test-Path -LiteralPath $target) { Remove-Item -LiteralPath $target -Force }
        throw
    } finally {
        $process.Dispose()
    }
}
function Find-AndroidTool([string]$tool) {
    $fromPath = Get-Command $tool -ErrorAction SilentlyContinue
    if ($fromPath) { return $fromPath.Source }
    $sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
    if ($tool -eq 'adb.exe') {
        $guess = Join-Path $sdk 'platform-tools\adb.exe'
        if (Test-Path -LiteralPath $guess) { return $guess }
    } else {
        $buildTools = Join-Path $sdk 'build-tools'
        if (Test-Path -LiteralPath $buildTools) {
            foreach ($version in (Get-ChildItem -LiteralPath $buildTools -Directory |
                Sort-Object Name -Descending)) {
                $candidate = Join-Path $version.FullName $tool
                if (Test-Path -LiteralPath $candidate) { return $candidate }
            }
        }
    }
    return ""
}

$adbExe = Find-AndroidTool 'adb.exe'
$aaptExe = Find-AndroidTool 'aapt.exe'
if (-not $adbExe) { Stop-QA "ADB not found. Install Android SDK Platform Tools." }
if (-not $aaptExe) { Stop-QA "aapt not found. Install Android SDK Build Tools." }
$script:selectedSerial = ''
if ([string]::IsNullOrWhiteSpace($ApkPath)) {
    $ApkPath = Join-Path $PSScriptRoot '..\app\build\outputs\apk\debug\app-debug.apk'
}
if (-not (Test-Path -LiteralPath $ApkPath -PathType Leaf)) {
    Stop-QA "APK not found: $ApkPath"
}
$ApkPath = (Resolve-Path -LiteralPath $ApkPath).Path
$packageLine = @(& $aaptExe dump badging $ApkPath 2>&1) |
    Where-Object { $_ -match "^package: name=" } | Select-Object -First 1
if ($LASTEXITCODE -ne 0 -or -not $packageLine) {
    Stop-QA "Cannot read APK package metadata"
}
$packageLine = [string]$packageLine
if ($packageLine -notmatch "^package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'") {
    Stop-QA "Unrecognized APK metadata: $packageLine"
}
$actualPackage = $Matches[1]
$code = $Matches[2]
$name = $Matches[3]
if ($actualPackage -ne $qaPackage) {
    Stop-QA "REFUSED: this is not the independent QA package ($actualPackage)."
}
$hash = (Get-FileHash -LiteralPath $ApkPath -Algorithm SHA256).Hash
Write-Host "[OK] QA APK: $actualPackage / $name (code $code)" -ForegroundColor Green
Write-Host "[OK] SHA-256: $hash"
Write-Host "[OK] ADB: $adbExe"

$rawDevices = @(Invoke-Adb -Arguments @('devices','-l'))
$ready = @()
$unauthorized = @()
foreach ($line in $rawDevices) {
    if ($line -match '^(\S+)\s+device(?:\s|$)') { $ready += $Matches[1] }
    if ($line -match '^(\S+)\s+(unauthorized|offline)(?:\s|$)') { $unauthorized += $Matches[1] }
}
if ($unauthorized.Count -gt 0) {
    Write-Warning "Unauthorized/offline phone detected. Unlock it and accept USB debugging."
}
if (-not $Install -and -not $Run -and -not $Capture -and -not $CollectOnly) {
    # Read-only preflight, including when no phone is attached.
    Write-Host ("[PRÉ-VERIFICAÇÃO] READY_DEVICES=" + $ready.Count)
    if ($ready.Count -eq 0) { Write-Host '[INFO] Connect an Android phone, enable USB debugging and authorize this PC.' }
    Write-Host "[OK] No app was installed, started, reset or uploaded."
    return
}
if ($Install -and -not $Run) { Stop-QA "Use -Install together with -Run to confirm active test intent." }
if ($Capture -and -not $Run -and -not $CollectOnly) { Stop-QA "Use -Capture together with -Run or -CollectOnly." }
if ($CollectOnly -and ($Install -or $Run)) {
    Stop-QA "-CollectOnly is read-only and cannot be combined with -Install or -Run."
}
if ($DeviceSerial -and $DeviceSerial -notmatch '^[a-zA-Z0-9_.:-]+$') {
    Stop-QA "Invalid device serial characters"
}
if ($DeviceSerial) {
    if ($ready -notcontains $DeviceSerial) {
        Stop-QA "Selected Android device is not authorized/connected."
    }
    $script:selectedSerial = $DeviceSerial
} elseif ($ready.Count -eq 1) {
    $script:selectedSerial = $ready[0]
} else {
    Stop-QA "Expected exactly one authorized phone, found $($ready.Count). Use -DeviceSerial when there are multiple."
}
Write-Host "[OK] Authorized device selected." -ForegroundColor Green
$startedAt = [DateTimeOffset]::Now.ToString('o')

if ($CollectOnly) {
    # Do not launch or install anything; this is a post-gameplay evidence collection.
    $pidResult = @(Invoke-Adb -Arguments @('shell','pidof',$qaPackage) -AcceptNonzero)
    $pidText = (($pidResult | Out-String).Trim())
    if ($pidText -notmatch '^\d+(\s+\d+)*$') {
        Stop-QA "QA game is not running. Open the .qa game on the phone and try again."
    }

    # Never capture a screenshot of a different foreground app or personal screen.
    $activityDump = @(Invoke-Adb -Arguments @('shell','dumpsys','activity','activities'))
    $activityLines = @($activityDump | Where-Object {
        $_ -match '(?i)topResumedActivity|mResumedActivity'
    })
    if (-not ($activityLines | Where-Object {
        $_ -match [regex]::Escape($qaPackage)
    })) {
        Stop-QA "QA game is not the resumed foreground app. Open Boleto Rush QA before collecting."
    }

    if (-not $EvidenceDirectory) {
        $EvidenceDirectory = Join-Path (Split-Path -Parent $ApkPath) (
            'QA-Evidencias\' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-pos-partida')
    }
    New-Item -Path $EvidenceDirectory -ItemType Directory -Force | Out-Null
    $EvidenceDirectory = (Resolve-Path -LiteralPath $EvidenceDirectory).Path

    $screenshot = Join-Path $EvidenceDirectory '01-tela-atual.png'
    Capture-Png $screenshot
    $logFile = Join-Path $EvidenceDirectory '02-logcat-app.txt'
    $log = @(Invoke-Adb -Arguments @('logcat','-d','-t','1200',
        ("--pid=" + $pidText.Split(' ')[0]),'*:W') -AcceptNonzero)
    $log | Out-File -LiteralPath $logFile -Encoding UTF8

    $report = [ordered]@{
        status = 'POST_GAMEPLAY_EVIDENCE_COLLECTED_MANUAL_REVIEW_REQUIRED'
        package = $actualPackage
        versionName = $name
        versionCode = $code
        apkSha256 = $hash
        collectedAt = $startedAt
        foregroundValidated = $true
        screenCaptured = (Test-Path -LiteralPath $screenshot)
        logcatFile = (Split-Path $logFile -Leaf)
        note = 'Read-only, no app restart or installation. Gameplay and ad behavior must be reviewed manually.'
    }
    $report | ConvertTo-Json -Depth 5 | Out-File -LiteralPath (
        Join-Path $EvidenceDirectory '03-resultado.json') -Encoding UTF8
    Write-Host ("[OK] Post-gameplay evidence saved locally: " + $EvidenceDirectory) -ForegroundColor Green
    return
}

if ($Install) {
    Write-Host '[AÇÃO] Installing/updating ONLY .qa package, preserving user app data ...'
    Invoke-Adb -Arguments @('install','-r',$ApkPath) | ForEach-Object { Write-Host $_ }
}
Write-Host '[AÇÃO] Opening QA game without clearing app data or changing screen resolution ...'
Invoke-Adb -Arguments @('shell','am','start','-W','-n',$qaActivity) |
    ForEach-Object { Write-Host $_ }
Start-Sleep -Seconds 6
$pidResult = @(Invoke-Adb -Arguments @('shell','pidof',$qaPackage) -AcceptNonzero)
$pidText = (($pidResult | Out-String).Trim())
if ($pidText -notmatch '^\d+(\s+\d+)*$') {
    Stop-QA "The game did not stay alive after launch. Review logcat via Android Studio."
}
Write-Host "[OK] Game process alive." -ForegroundColor Green
if (-not $Capture) {
    Write-Host '[OK] Startup check completed. Play manually to validate gameplay/UMP.'
    return
}

if (-not $EvidenceDirectory) {
    $EvidenceDirectory = Join-Path (Split-Path -Parent $ApkPath) ('QA-Evidencias\' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
}
New-Item -Path $EvidenceDirectory -ItemType Directory -Force | Out-Null
$EvidenceDirectory = (Resolve-Path -LiteralPath $EvidenceDirectory).Path

$screenshot = Join-Path $EvidenceDirectory '01-abertura.png'
Capture-Png $screenshot
$logFile = Join-Path $EvidenceDirectory '02-logcat-app.txt'
# Collect only local application runtime + warnings; no global buffer clear.
$log = @(Invoke-Adb -Arguments @('logcat','-d','-t','1200',("--pid=" + $pidText.Split(' ')[0]),'*:W') -AcceptNonzero)
$log | Out-File -LiteralPath $logFile -Encoding UTF8
$report = [ordered]@{
    status = 'STARTUP_OK_MANUAL_GAMEPLAY_PENDING'
    package = $actualPackage
    versionName = $name
    versionCode = $code
    apkSha256 = $hash
    startTime = $startedAt
    screenCaptured = (Test-Path -LiteralPath $screenshot)
    note = 'Local evidence only. Not an automated full-round or AdMob UI certification.'
    requiredManualChecks = @(
        'Complete classic 45s and marathon 90s rounds',
        'Verify three boss waves and monthly stamp integrity',
        'Confirm album, relic store and weekly rewards',
        'Review UMP privacy options and test rewarded ads without double credit',
        'Review screenshots and logcat for crashes, jank and layout clipping'
    )
}
$report | ConvertTo-Json -Depth 5 | Out-File -LiteralPath (
    Join-Path $EvidenceDirectory '03-resultado.json') -Encoding UTF8
Write-Host ("[OK] Evidence saved locally: " + $EvidenceDirectory) -ForegroundColor Green
Write-Host '[IMPORTANTE] The automated check confirms only launch and process survival. Complete real gameplay manually.'
