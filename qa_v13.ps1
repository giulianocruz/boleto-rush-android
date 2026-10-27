$ErrorActionPreference='Continue'
$adb="$env:ANDROID_HOME\platform-tools\adb.exe"
$root=$PSScriptRoot
$passed=0;$failed=0
function Assert([string]$what,[bool]$v){if($v){$script:passed++;Write-Output "PASS | $what"}else{$script:failed++;Write-Output "FAIL | $what"}}
function UI{
  & $adb shell uiautomator dump /sdcard/qa_v13.xml 2>$null|Out-Null
  return ((& $adb shell cat /sdcard/qa_v13.xml 2>$null)-join '')
}
function Tap([string]$x,[string]$text){
  $match=[regex]::Match($x,'text="'+[regex]::Escape($text)+'"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')
  if(!$match.Success){Write-Output "MISSING $text";return}
  $xx=[int](([int]$match.Groups[1].Value+[int]$match.Groups[3].Value)/2)
  $yy=[int](([int]$match.Groups[2].Value+[int]$match.Groups[4].Value)/2)
  & $adb shell input tap $xx $yy
  Write-Output "TAPPED $text"
}
for($i=0;$i -lt 40;$i++){
  $boot=((& $adb shell getprop sys.boot_completed 2>$null)-join '').Trim()
  if($boot -eq '1'){break}
  Start-Sleep -Seconds 3
}
Assert 'Android booted' ($boot -eq '1')
if($boot -ne '1'){exit 3}
$apk="$root\app\build\outputs\apk\debug\app-debug.apk"
& $adb install -r $apk
Assert 'APK installed successfully' ($LASTEXITCODE -eq 0)
& $adb logcat -c
& $adb shell am force-stop br.com.proximaera.boletorush
& $adb shell am start -n br.com.proximaera.boletorush/.MainActivity
Start-Sleep -Seconds 3
$x=UI
if($x.Contains('System UI isn')) {
    Tap $x 'Wait'
    Start-Sleep -Seconds 3
    $x=UI
}
Assert 'Menu and image-screen loaded' ($x.Contains('JOGAR AGORA'))
Assert 'Game version 1.3' ($x.Contains('v1.3'))
if($x.Contains('JOGAR AGORA')){
    & $adb shell screencap -p /sdcard/qa_v13_menu.png
    & $adb pull /sdcard/qa_v13_menu.png "$root\store\premium_v13_menu.png" 2>&1 | Out-Null
    Tap $x 'JOGAR AGORA'
    Start-Sleep -Seconds 2
    $x=UI
    Assert 'Gameplay started' ($x.Contains('PAUSAR') -and $x.Contains('PONTOS'))
    & $adb shell screencap -p /sdcard/qa_v13_game.png
    & $adb pull /sdcard/qa_v13_game.png "$root\store\premium_v13_game.png" 2>&1 | Out-Null
    Tap $x 'PAUSAR'
    Start-Sleep -Seconds 1
    $x=UI
    Assert 'Pause menu works' ($x.Contains('CONTINUAR'))
    Tap $x 'CONTINUAR'
    Start-Sleep -Seconds 2
    $x=UI
    Assert 'Resume works' ($x.Contains('PAUSAR'))
    & $adb shell input swipe 200 1900 900 1900 260
    Start-Sleep -Seconds 1
    $x=UI
    Assert 'App process alive' (([string]((& $adb shell pidof br.com.proximaera.boletorush)-join '')).Trim().Length -gt 0)
}
$crash=((& $adb logcat -d -t 1000 -s AndroidRuntime:E 2>$null)-join '')
Assert 'No fatal Java crash in sampled log' (-not $crash.Contains('FATAL EXCEPTION'))
Write-Output "SUMMARY PASS=$passed FAIL=$failed"
"PASS=$passed FAIL=$failed"|Set-Content "$root\qa_v13_result.txt"
