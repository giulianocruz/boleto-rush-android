$ErrorActionPreference = 'Stop'
$root = Join-Path $env:USERPROFILE 'AndroidStudioProjects\BoletoRush'
$zip = Join-Path $env:USERPROFILE 'Downloads\out. 26 - 20_30 (1).zip'
if (!(Test-Path $zip)) { throw "Missing user zip at $zip" }
$assets = Join-Path $root 'arte_premium\recebidas\flow_20261027'
$res = Join-Path $root 'app\src\main\res\drawable-nodpi'
$store = Join-Path $root 'store'
New-Item -ItemType Directory -Force -Path $assets,$res,$store|Out-Null
Expand-Archive -LiteralPath $zip -DestinationPath $assets -Force
$files = Get-ChildItem $assets -File -Filter '*.jpg'
foreach($f in $files){ Write-Output ('IMPORT '+$f.Name+' '+$f.Length) }
Add-Type -AssemblyName System.Drawing
function ExportJpg($f,$out,[int]$maxW,[int]$maxH,[long]$quality=79){
  $src=[Drawing.Image]::FromFile($f)
  try {
    $scale=[math]::Min([math]::Min($maxW/$src.Width,$maxH/$src.Height),1)
    $w=[int][math]::Round($src.Width*$scale);$h=[int][math]::Round($src.Height*$scale)
    $bitmap=[Drawing.Bitmap]::new($w,$h)
    try {
      $g=[Drawing.Graphics]::FromImage($bitmap)
      try {
        $g.InterpolationMode='HighQualityBicubic'
        $g.CompositingQuality='HighQuality'
        $g.DrawImage($src,0,0,$w,$h)
      } finally {$g.Dispose()}
      $codec=[Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()|Where-Object {$_.MimeType -eq 'image/jpeg'}|Select-Object -First 1
      $params=[Drawing.Imaging.EncoderParameters]::new(1)
      $params.Param[0]=[Drawing.Imaging.EncoderParameter]::new([Drawing.Imaging.Encoder]::Quality,$quality)
      $bitmap.Save($out,$codec,$params);$params.Dispose()
    } finally {$bitmap.Dispose()}
  }finally{$src.Dispose()}
}
$map=@(
 @{ prefix='Wallet_running_from_flying_bills'; res='premium_running.jpg'; maxW=1376;maxH=768 },
 @{ prefix='Mobile_game_UI_kit_assets'; res='premium_ui_reference.jpg'; maxW=1376;maxH=768 },
 @{ prefix='Game_Over_UI_Mockup'; res='premium_end_reference.jpg'; maxW=1376;maxH=768 },
 @{ prefix='Mobile_game_store_screenshot_mockup'; res='premium_store_reference.jpg'; maxW=1376;maxH=768 }
)
foreach($m in $map){
 $file=$files|Where-Object {$_.Name.StartsWith($m.prefix)}|Select-Object -First 1
 if (!$file) { throw "Missing $($m.prefix)" }
 $out=if($m.res -eq 'premium_running.jpg'){Join-Path $res $m.res}else{Join-Path $store $m.res}
 ExportJpg $file.FullName $out $m.maxW $m.maxH
 Write-Output ('EXPORTED '+$out+' '+(Get-Item $out).Length)
}
Write-Output ('ASSET_FILES='+$files.Count)
