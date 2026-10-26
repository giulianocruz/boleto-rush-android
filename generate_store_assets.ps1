Add-Type -AssemblyName System.Drawing
$ErrorActionPreference='Stop'
$root=Join-Path $PSScriptRoot 'store'
New-Item -ItemType Directory -Path $root -Force | Out-Null
function Brush([string]$hex) { return [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml($hex)) }
function Image([int]$w,[int]$h,[bool]$alpha) {
  $fmt=if($alpha){[System.Drawing.Imaging.PixelFormat]::Format32bppArgb}else{[System.Drawing.Imaging.PixelFormat]::Format24bppRgb}
  $bmp=[System.Drawing.Bitmap]::new($w,$h,$fmt)
  $g=[System.Drawing.Graphics]::FromImage($bmp)
  $g.SmoothingMode=[System.Drawing.Drawing2D.SmoothingMode]::HighQuality
  $g.TextRenderingHint=[System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
  $g.Clear([System.Drawing.ColorTranslator]::FromHtml('#071326'))
  return @($bmp,$g)
}
function Txt($g,$text,[int]$size,[int]$x,[int]$y,[string]$color,[string]$face='Segoe UI') {
  $font=[System.Drawing.Font]::new($face,$size,[System.Drawing.FontStyle]::Bold,[System.Drawing.GraphicsUnit]::Pixel)
  $brush=Brush $color
  $g.DrawString($text,$font,$brush,[float]$x,[float]$y)
  $font.Dispose();$brush.Dispose()
}
function Wallet($g,[int]$x,[int]$y,[int]$w,[int]$h) {
  $g.FillRectangle((Brush '#0A7785'),$x+12,$y+10,$w,$h)
  $g.FillRectangle((Brush '#25E2D1'),$x,$y,$w,$h-10)
  $g.FillRectangle((Brush '#073849'),$x+18,$y+($h/3),$w-36,($h/3))
  $g.FillEllipse((Brush '#FFD166'),$x+($w/2)-13,$y+($h/2)-13,26,26)
}
$img=Image 512 512 $true; $bmp=$img[0];$g=$img[1]
$g.FillEllipse((Brush '#124461'),-110,-170,600,510)
$g.FillEllipse((Brush '#0B2940'),-70,-135,480,380)
Wallet $g 89 192 300 195
$g.FillEllipse((Brush '#B88324'),333,101,120,120)
$g.FillEllipse((Brush '#FFD166'),343,110,100,100)
Txt $g '$' 70 369 111 '#75450C'
Txt $g 'BR!' 75 154 240 '#FFFFFF'
$bmp.Save((Join-Path $root 'play-icon-512.png'),[System.Drawing.Imaging.ImageFormat]::Png);$g.Dispose();$bmp.Dispose()
$img=Image 1024 500 $false;$bmp=$img[0];$g=$img[1]
$g.FillEllipse((Brush '#123B57'),-320,-360,1120,800)
$g.FillEllipse((Brush '#0C293F'),-220,-300,900,650)
for($i=0;$i -lt 24;$i++){
  $x=$i*46
  $height=55+($i*19 % 9)*15
  $g.FillRectangle((Brush '#163148'),$x,500-$height,39,$height)
}
Txt $g 'PROXIMA ERA GAMES' 24 70 47 '#25E2D1'
Txt $g 'BOLETO' 111 61 124 '#FFFFFF'
Txt $g 'RUSH!' 123 61 225 '#25E2D1'
Txt $g 'A vida cobra. Voce desvia.' 31 70 375 '#FFD166'
Wallet $g 677 179 255 172
$g.FillEllipse((Brush '#B88324'),812,85,117,117)
$g.FillEllipse((Brush '#FFD166'),823,95,95,95)
Txt $g '$' 64 849 99 '#75450C'
$bmp.Save((Join-Path $root 'feature-graphic-1024x500.png'),[System.Drawing.Imaging.ImageFormat]::Png);$g.Dispose();$bmp.Dispose()
Write-Output (Get-ChildItem $root -File | Select-Object Name,Length | Format-Table | Out-String)
