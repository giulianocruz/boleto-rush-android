$ErrorActionPreference='Stop'
$project = Join-Path $env:USERPROFILE 'AndroidStudioProjects\BoletoRush'
$zip = Join-Path $env:USERPROFILE 'Downloads\out. 26 - 20_30.zip'
$stage = Join-Path $project 'arte_premium\recebidas\flow_20261026'
$res = Join-Path $project 'app\src\main\res\drawable-nodpi'
$store = Join-Path $project 'store'
New-Item -ItemType Directory -Force -Path $stage,$res,$store | Out-Null
Expand-Archive -LiteralPath $zip -DestinationPath $stage -Force
Add-Type -AssemblyName System.Drawing
$pics = Get-ChildItem -LiteralPath $stage -Filter '*.jpg'
$sheet = $pics | Where-Object { $_.Name -like 'Game_sprite*' } | Select-Object -First 1
$promo = $pics | Where-Object { $_.Name -like 'Wallet_mascot*' } | Select-Object -First 1
$icon = $pics | Where-Object { $_.Name -like 'Wallet_holding*' } | Select-Object -First 1
if (-not ($sheet -and $promo -and $icon)){throw 'One or more supplied images missing'}
Copy-Item -LiteralPath $promo.FullName -Destination (Join-Path $res 'premium_promo.jpg') -Force
Copy-Item -LiteralPath $icon.FullName -Destination (Join-Path $res 'premium_icon.jpg') -Force
$cs=@'
using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
public static class SpritePrep {
  public static void Slice(string source, string output, int l, int t, int r, int b) {
    using(var src = new Bitmap(source))
    using(var crop = new Bitmap(r-l, b-t, PixelFormat.Format32bppArgb))
    using(var finished = new Bitmap(256, 256, PixelFormat.Format32bppArgb)) {
      Color bg = src.GetPixel(0,0);
      for (int y=0; y<b-t; y++) for (int x=0; x<r-l; x++) {
        Color p = src.GetPixel(l+x,t+y);
        int delta = Math.Max(Math.Abs(p.R-bg.R), Math.Max(Math.Abs(p.G-bg.G),Math.Abs(p.B-bg.B)));
        // Soften JPEG edge without bringing solid sheet background into game.
        int a = delta < 17 ? 0 : (delta > 63 ? 255 : (delta-17)*255/46);
        crop.SetPixel(x,y,Color.FromArgb(a,p.R,p.G,p.B));
      }
      using(var g=Graphics.FromImage(finished)) {
        g.Clear(Color.Transparent);
        g.CompositingMode=CompositingMode.SourceOver;
        g.InterpolationMode=InterpolationMode.HighQualityBicubic;
        g.SmoothingMode=SmoothingMode.HighQuality;
        int margin=9; int ww=256-margin*2; int hh=256-margin*2;
        float scale=Math.Min((float)ww/crop.Width,(float)hh/crop.Height);
        int dw=(int)(crop.Width*scale),dh=(int)(crop.Height*scale);
        g.DrawImage(crop,new Rectangle((256-dw)/2,(256-dh)/2,dw,dh),new Rectangle(0,0,crop.Width,crop.Height),GraphicsUnit.Pixel);
      }
      finished.Save(output,ImageFormat.Png);
    }
  }
  public static void SquareIcon(string source,string target){
    using(var src=new Bitmap(source)) using(var dst=new Bitmap(512,512))using(var g=Graphics.FromImage(dst)){
      g.InterpolationMode=InterpolationMode.HighQualityBicubic;g.DrawImage(src,new Rectangle(0,0,512,512));dst.Save(target,ImageFormat.Png);
    }
  }
  public static void Feature(string source,string target){
    using(var src=new Bitmap(source))using(var dst=new Bitmap(1024,500))using(var g=Graphics.FromImage(dst)){
      g.InterpolationMode=InterpolationMode.HighQualityBicubic;
      // Cropping some lower sidewalk preserves entire central logo.
      g.DrawImage(src,new Rectangle(0,0,1024,500),new Rectangle(0,0,src.Width,src.Width*500/1024),GraphicsUnit.Pixel);
      dst.Save(target,ImageFormat.Jpeg);
    }
  }
}
'@
Add-Type -TypeDefinition $cs -ReferencedAssemblies 'System.Drawing'
$cols = @(@(44,269),@(279,520),@(530,760),@(769,1012))
$rows = @(@(94,281),@(341,525),@(585,760),@(809,981))
$names = @(
 @('spr_hero_idle','spr_hero_run','spr_hero_jump','spr_hero_hurt'),
 @('spr_bill_idle','spr_bill_attack','spr_bill_damaged','spr_bill_defeated'),
 @('spr_coin_front','spr_coin_angle','spr_coin_side','spr_coin_collect'),
 @('spr_shield_collect','spr_shield_idle','spr_shield_active','spr_shield_impact')
)
for($y=0;$y -lt 4;$y++){
 for($x=0;$x -lt 4;$x++){
  $n=$names[$y][$x]
  [SpritePrep]::Slice($sheet.FullName,(Join-Path $res "$n.png"),$cols[$x][0],$rows[$y][0],$cols[$x][1],$rows[$y][1])
  Write-Output ("SPRITE "+$n)
 }
}
[SpritePrep]::SquareIcon($icon.FullName,(Join-Path $store 'premium_play_icon_512.png'))
[SpritePrep]::Feature($promo.FullName,(Join-Path $store 'premium_feature_1024x500.jpg'))
Write-Output ('SPRITE_COUNT='+(Get-ChildItem $res 'spr_*.png').Count)
Write-Output ('ASSET_SIZE_MB='+[math]::Round(((Get-ChildItem $res -File|Measure-Object Length -Sum).Sum/1MB),2))
