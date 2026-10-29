$ErrorActionPreference='Stop'
Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;
public static class RushNative {
 [StructLayout(LayoutKind.Sequential)] public struct RECT {public int Left,Top,Right,Bottom;}
 [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd,out RECT r);
 [DllImport("user32.dll")] public static extern bool GetClientRect(IntPtr hWnd,out RECT r);
 [DllImport("user32.dll")] public static extern bool PrintWindow(IntPtr hWnd,IntPtr hdc,uint flags);
 [DllImport("user32.dll")] public static extern bool PostMessage(IntPtr hWnd,uint msg,IntPtr wp,IntPtr lp);
}
"@
Add-Type -AssemblyName System.Drawing
$exe=Join-Path ([Environment]::GetFolderPath('DesktopDirectory')) 'BoletoRush-WINDOWS-JOGAVEL-v1.4.exe'
$proc=Start-Process -FilePath $exe -PassThru
Start-Sleep -Seconds 2
$proc.Refresh()
$handle=$proc.MainWindowHandle
if($handle -eq [IntPtr]::Zero){throw "Janela indisponível"}
$r=New-Object RushNative+RECT
[void][RushNative]::GetWindowRect($handle,[ref]$r)
$w=$r.Right-$r.Left;$h=$r.Bottom-$r.Top
Write-Output "WINDOW_RECT=$w x $h"
$folder=Split-Path -Parent $MyInvocation.MyCommand.Path
function Save-Rush([string]$name) {
 $bmp=New-Object System.Drawing.Bitmap($w,$h)
 $g=[System.Drawing.Graphics]::FromImage($bmp)
 $hdc=$g.GetHdc()
 try { $ok=[RushNative]::PrintWindow($handle,$hdc,0) } finally {$g.ReleaseHdc($hdc)}
 $g.Dispose()
 $p=Join-Path $folder $name
 $bmp.Save($p,[System.Drawing.Imaging.ImageFormat]::Png)
 $bmp.Dispose()
 Write-Output ("SHOT="+$p+" OK="+$ok)
}
Save-Rush "qa-menu.png"
function Click-Rush([int]$x,[int]$y) {
 $l=[IntPtr]::new(($y -shl 16) -bor $x)
 [void][RushNative]::PostMessage($handle,0x0201,[IntPtr]1,$l)
 Start-Sleep -Milliseconds 80
 [void][RushNative]::PostMessage($handle,0x0202,[IntPtr]0,$l)
}
Write-Output ('BEFORE_CLICK='+(-not $proc.HasExited))
Click-Rush 255 521
Start-Sleep -Seconds 2
$proc.Refresh()
Write-Output ('AFTER_CLICK='+(-not $proc.HasExited))
Save-Rush "qa-gameplay.png"
Write-Output ("STILL_RUNNING="+(-not $proc.HasExited))
