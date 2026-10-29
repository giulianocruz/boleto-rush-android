$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$base = Split-Path -Parent $root
$assets = Join-Path $base 'app\src\main\res\drawable-nodpi'
$desktop = [Environment]::GetFolderPath('DesktopDirectory')
$target = Join-Path $desktop 'BoletoRush-WINDOWS-JOGAVEL-v1.4.exe'
$csc = Join-Path $env:WINDIR 'Microsoft.NET\Framework\v4.0.30319\csc.exe'
if (-not (Test-Path $csc)) { throw "Compilador C# .NET Framework não encontrado: $csc" }
$names = @(
'premium_promo.jpg','premium_running.jpg',
'spr_hero_idle.png','spr_hero_run.png','spr_hero_jump.png','spr_hero_hurt.png',
'spr_bill_idle.png','spr_bill_attack.png','spr_bill_damaged.png','spr_bill_defeated.png',
'spr_coin_front.png','spr_coin_angle.png','spr_coin_side.png','spr_coin_collect.png',
'spr_shield_idle.png','spr_shield_active.png','spr_shield_collect.png','spr_shield_impact.png'
)
$argslist = @('/nologo','/target:winexe','/optimize+','/platform:anycpu',
'/r:System.dll','/r:System.Windows.Forms.dll','/r:System.Drawing.dll','/r:System.Core.dll',
"/win32icon:$(Join-Path $root 'boleto-rush.ico')",
"/out:$target")
foreach($name in $names) {
    $path = Join-Path $assets $name
    if(-not (Test-Path $path)) { throw "Recurso não encontrado: $path" }
    $argslist += "/resource:$path,rush.$name"
}
$argslist += (Join-Path $root 'BoletoRushWindows.cs')
& $csc $argslist
if ($LASTEXITCODE -ne 0) { throw "Falha na compilação (exit $LASTEXITCODE)" }
Write-Output ("BUILD_OK " + $target)
Get-Item $target | Select-Object FullName,Length,LastWriteTime
