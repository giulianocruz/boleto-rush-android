$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;
public static class WinIconNative { [DllImport("user32.dll")] public static extern bool DestroyIcon(IntPtr hIcon); }
"@
$root=Split-Path -Parent $MyInvocation.MyCommand.Path
$imgpath=Join-Path (Split-Path -Parent $root) 'app\src\main\res\drawable-nodpi\premium_icon.jpg'
$dest=Join-Path $root 'boleto-rush.ico'
$img=[System.Drawing.Image]::FromFile($imgpath)
$bitmap=New-Object System.Drawing.Bitmap(64,64)
$g=[System.Drawing.Graphics]::FromImage($bitmap)
$g.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.DrawImage($img,0,0,64,64)
$g.Dispose()
$img.Dispose()
$hicon=$bitmap.GetHicon()
try {
 $icon=[System.Drawing.Icon]::FromHandle($hicon)
 $file=[System.IO.File]::Create($dest)
 try { $icon.Save($file) } finally { $file.Dispose();$icon.Dispose() }
}finally {
 [void][WinIconNative]::DestroyIcon($hicon)
 $bitmap.Dispose()
}
Write-Output "ICON_OK $dest"
