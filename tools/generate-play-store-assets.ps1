$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$out = Join-Path $root "play-store-assets"
New-Item -ItemType Directory -Force -Path $out | Out-Null

function New-Bitmap([int]$w, [int]$h) {
  $bmp = New-Object System.Drawing.Bitmap $w, $h, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit
  return @($bmp, $g)
}

function Save-Png($bmp, $g, [string]$path) {
  $g.Dispose()
  $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
  $bmp.Dispose()
}

function Brush([string]$hex) {
  return New-Object System.Drawing.SolidBrush ([System.Drawing.ColorTranslator]::FromHtml($hex))
}

function Pen([string]$hex, [float]$width) {
  return New-Object System.Drawing.Pen ([System.Drawing.ColorTranslator]::FromHtml($hex)), $width
}

function FontObj([string]$name, [float]$size, [System.Drawing.FontStyle]$style = [System.Drawing.FontStyle]::Regular) {
  return [System.Drawing.Font]::new($name, $size, $style, [System.Drawing.GraphicsUnit]::Pixel)
}

function RoundedRect([System.Drawing.Graphics]$g, [System.Drawing.Brush]$brush, [float]$x, [float]$y, [float]$w, [float]$h, [float]$r) {
  $path = New-Object System.Drawing.Drawing2D.GraphicsPath
  $d = $r * 2
  $path.AddArc($x, $y, $d, $d, 180, 90)
  $path.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
  $path.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
  $path.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
  $path.CloseFigure()
  $g.FillPath($brush, $path)
  $path.Dispose()
}

function Draw-ShieldLogo([System.Drawing.Graphics]$g, [float]$cx, [float]$cy, [float]$scale) {
  $cyan = Brush "#22d3ee"
  $blue = Brush "#2563eb"
  $white = Brush "#ffffff"
  $shield = @(
    [System.Drawing.PointF]::new($cx, $cy - 42*$scale),
    [System.Drawing.PointF]::new($cx - 34*$scale, $cy - 26*$scale),
    [System.Drawing.PointF]::new($cx - 34*$scale, $cy + 6*$scale),
    [System.Drawing.PointF]::new($cx - 22*$scale, $cy + 34*$scale),
    [System.Drawing.PointF]::new($cx, $cy + 50*$scale),
    [System.Drawing.PointF]::new($cx + 22*$scale, $cy + 34*$scale),
    [System.Drawing.PointF]::new($cx + 34*$scale, $cy + 6*$scale),
    [System.Drawing.PointF]::new($cx + 34*$scale, $cy - 26*$scale)
  )
  $g.FillPolygon($blue, $shield)
  $inner = @(
    [System.Drawing.PointF]::new($cx, $cy - 32*$scale),
    [System.Drawing.PointF]::new($cx - 24*$scale, $cy - 20*$scale),
    [System.Drawing.PointF]::new($cx - 24*$scale, $cy + 4*$scale),
    [System.Drawing.PointF]::new($cx - 14*$scale, $cy + 25*$scale),
    [System.Drawing.PointF]::new($cx, $cy + 36*$scale),
    [System.Drawing.PointF]::new($cx + 14*$scale, $cy + 25*$scale),
    [System.Drawing.PointF]::new($cx + 24*$scale, $cy + 4*$scale),
    [System.Drawing.PointF]::new($cx + 24*$scale, $cy - 20*$scale)
  )
  $g.FillPolygon($cyan, $inner)
  RoundedRect $g $white ($cx - 23*$scale) ($cy - 1*$scale) (46*$scale) (30*$scale) (7*$scale)
  $lockPen = New-Object System.Drawing.Pen ([System.Drawing.ColorTranslator]::FromHtml("#ffffff")), (6*$scale)
  $g.DrawArc($lockPen, $cx - 16*$scale, $cy - 20*$scale, 32*$scale, 34*$scale, 200, 140)
  $key = Brush "#0f172a"
  $g.FillEllipse($key, $cx - 5*$scale, $cy + 9*$scale, 10*$scale, 10*$scale)
  $g.FillRectangle($key, $cx - 2*$scale, $cy + 17*$scale, 4*$scale, 8*$scale)
}

function Draw-AppIcon {
  $items = New-Bitmap 512 512
  $bmp = $items[0]; $g = $items[1]
  $rect = [System.Drawing.Rectangle]::new(0,0,512,512)
  $bg = New-Object System.Drawing.Drawing2D.LinearGradientBrush $rect, ([System.Drawing.ColorTranslator]::FromHtml("#07111f")), ([System.Drawing.ColorTranslator]::FromHtml("#0f3b83")), 45
  $g.FillRectangle($bg, $rect)
  RoundedRect $g (Brush "#10213a") 42 42 428 428 96
  Draw-ShieldLogo $g 256 246 3.35
  $font = FontObj "Segoe UI" 44 ([System.Drawing.FontStyle]::Bold)
  $sf = New-Object System.Drawing.StringFormat
  $sf.Alignment = [System.Drawing.StringAlignment]::Center
  $g.DrawString("SafeNet", $font, (Brush "#e0f2fe"), [System.Drawing.RectangleF]::new(0,405,512,64), $sf)
  Save-Png $bmp $g (Join-Path $out "app-icon-512.png")
}

function Draw-FeatureGraphic {
  $items = New-Bitmap 1024 500
  $bmp = $items[0]; $g = $items[1]
  $rect = [System.Drawing.Rectangle]::new(0,0,1024,500)
  $bg = New-Object System.Drawing.Drawing2D.LinearGradientBrush $rect, ([System.Drawing.ColorTranslator]::FromHtml("#07111f")), ([System.Drawing.ColorTranslator]::FromHtml("#134e8f")), 25
  $g.FillRectangle($bg, $rect)
  Draw-ShieldLogo $g 830 236 2.2
  $title = FontObj "Segoe UI" 64 ([System.Drawing.FontStyle]::Bold)
  $sub = FontObj "Segoe UI" 30
  $small = FontObj "Segoe UI" 24
  $g.DrawString("SafeNet VPN", $title, (Brush "#f8fafc"), 72, 112)
  $g.DrawString("Secure AmneziaWG VPN access", $sub, (Brush "#bae6fd"), 76, 194)
  RoundedRect $g (Brush "#0ea5e9") 78 278 245 58 18
  $g.DrawString("Fast Connect", $small, (Brush "#ffffff"), 118, 292)
  RoundedRect $g (Brush "#1e293b") 350 278 245 58 18
  $g.DrawString("Secure Access", $small, (Brush "#cbd5e1"), 390, 292)
  Save-Png $bmp $g (Join-Path $out "feature-graphic-1024x500.png")
}

function Draw-PhoneFrame([System.Drawing.Graphics]$g) {
  RoundedRect $g (Brush "#020617") 255 72 570 1776 64
  RoundedRect $g (Brush "#0f172a") 282 102 516 1716 44
  RoundedRect $g (Brush "#020617") 456 124 168 28 14
}

function Draw-HomeScreenshot {
  $items = New-Bitmap 1080 1920
  $bmp = $items[0]; $g = $items[1]
  $g.FillRectangle((Brush "#07111f"), 0, 0, 1080, 1920)
  Draw-PhoneFrame $g
  Draw-ShieldLogo $g 540 280 1.15
  $title = FontObj "Segoe UI" 48 ([System.Drawing.FontStyle]::Bold)
  $body = FontObj "Segoe UI" 25
  $small = FontObj "Segoe UI" 21
  $sf = New-Object System.Drawing.StringFormat
  $sf.Alignment = [System.Drawing.StringAlignment]::Center
  $g.DrawString("SafeNet VPN", $title, (Brush "#f8fafc"), [System.Drawing.RectangleF]::new(300,380,480,62), $sf)
  RoundedRect $g (Brush "#082f49") 340 500 400 400 200
  RoundedRect $g (Brush "#0ea5e9") 396 556 288 288 144
  $g.DrawString("CONNECTED", (FontObj "Segoe UI" 34 ([System.Drawing.FontStyle]::Bold)), (Brush "#ffffff"), [System.Drawing.RectangleF]::new(300,938,480,52), $sf)
  $g.DrawString("AmneziaWG tunnel is active", $body, (Brush "#bae6fd"), [System.Drawing.RectangleF]::new(300,995,480,42), $sf)
  RoundedRect $g (Brush "#13243a") 340 1100 400 132 24
  $g.DrawString("Server", $small, (Brush "#94a3b8"), 378, 1124)
  $g.DrawString("SafeNetApp", (FontObj "Segoe UI" 32 ([System.Drawing.FontStyle]::Bold)), (Brush "#f8fafc"), 378, 1158)
  RoundedRect $g (Brush "#13243a") 340 1270 400 132 24
  $g.DrawString("Traffic", $small, (Brush "#94a3b8"), 378, 1294)
  $g.DrawString("Private and encrypted", (FontObj "Segoe UI" 30 ([System.Drawing.FontStyle]::Bold)), (Brush "#f8fafc"), 378, 1328)
  RoundedRect $g (Brush "#0ea5e9") 340 1514 400 74 24
  $g.DrawString("Disconnect", (FontObj "Segoe UI" 28 ([System.Drawing.FontStyle]::Bold)), (Brush "#ffffff"), [System.Drawing.RectangleF]::new(340,1532,400,44), $sf)
  Save-Png $bmp $g (Join-Path $out "phone-screenshot-1-home.png")
}

function Draw-NotificationsScreenshot {
  $items = New-Bitmap 1080 1920
  $bmp = $items[0]; $g = $items[1]
  $g.FillRectangle((Brush "#07111f"), 0, 0, 1080, 1920)
  Draw-PhoneFrame $g
  $title = FontObj "Segoe UI" 42 ([System.Drawing.FontStyle]::Bold)
  $body = FontObj "Segoe UI" 24
  $small = FontObj "Segoe UI" 20
  $g.DrawString("SafeNet VPN", $title, (Brush "#f8fafc"), 340, 210)
  $g.DrawString("Notifications", (FontObj "Segoe UI" 34 ([System.Drawing.FontStyle]::Bold)), (Brush "#bae6fd"), 340, 300)
  $cards = @(
    @("System Update", "Admin notices and service updates appear here.", "Now"),
    @("VPN Access", "Your device is ready to connect securely.", "Today"),
    @("Support", "Telegram support: @myozin99", "SafeNet")
  )
  $y = 410
  foreach ($card in $cards) {
    RoundedRect $g (Brush "#13243a") 330 $y 420 184 24
    RoundedRect $g (Brush "#0ea5e9") 362 ($y + 32) 54 54 18
    $g.DrawString($card[0], (FontObj "Segoe UI" 28 ([System.Drawing.FontStyle]::Bold)), (Brush "#f8fafc"), 438, ($y + 30))
    $g.DrawString($card[2], $small, (Brush "#38bdf8"), 438, ($y + 68))
    $g.DrawString($card[1], $body, (Brush "#cbd5e1"), [System.Drawing.RectangleF]::new(362,($y + 112),350,60))
    $y += 226
  }
  RoundedRect $g (Brush "#082f49") 330 1240 420 220 28
  $sf = New-Object System.Drawing.StringFormat
  $sf.Alignment = [System.Drawing.StringAlignment]::Center
  $g.DrawString("Admin alerts, VPN updates, and support in one clean inbox.", (FontObj "Segoe UI" 24 ([System.Drawing.FontStyle]::Bold)), (Brush "#f8fafc"), [System.Drawing.RectangleF]::new(360,1290,360,120), $sf)
  Save-Png $bmp $g (Join-Path $out "phone-screenshot-2-notifications.png")
}

function Draw-TabletScreenshot {
  $items = New-Bitmap 1920 1080
  $bmp = $items[0]; $g = $items[1]
  $rect = [System.Drawing.Rectangle]::new(0,0,1920,1080)
  $bg = New-Object System.Drawing.Drawing2D.LinearGradientBrush $rect, ([System.Drawing.ColorTranslator]::FromHtml("#07111f")), ([System.Drawing.ColorTranslator]::FromHtml("#0f3b83")), 28
  $g.FillRectangle($bg, $rect)

  $title = FontObj "Segoe UI" 70 ([System.Drawing.FontStyle]::Bold)
  $heading = FontObj "Segoe UI" 40 ([System.Drawing.FontStyle]::Bold)
  $body = FontObj "Segoe UI" 28
  $small = FontObj "Segoe UI" 23
  Draw-ShieldLogo $g 214 154 1.05
  $g.DrawString("SafeNet VPN", $title, (Brush "#f8fafc"), 310, 92)
  $g.DrawString("Secure AmneziaWG access for phones and tablets", $body, (Brush "#bae6fd"), 314, 174)

  RoundedRect $g (Brush "#0f172a") 90 280 520 680 36
  RoundedRect $g (Brush "#13243a") 128 326 444 122 24
  $g.DrawString("VPN Status", $small, (Brush "#94a3b8"), 164, 348)
  $g.DrawString("Connected", $heading, (Brush "#f8fafc"), 164, 382)
  RoundedRect $g (Brush "#0ea5e9") 128 500 444 106 26
  $sf = New-Object System.Drawing.StringFormat
  $sf.Alignment = [System.Drawing.StringAlignment]::Center
  $g.DrawString("Disconnect", (FontObj "Segoe UI" 32 ([System.Drawing.FontStyle]::Bold)), (Brush "#ffffff"), [System.Drawing.RectangleF]::new(128,532,444,52), $sf)
  RoundedRect $g (Brush "#13243a") 128 666 444 224 28
  $g.DrawString("Current Server", $small, (Brush "#94a3b8"), 166, 700)
  $g.DrawString("SafeNetApp", (FontObj "Segoe UI" 36 ([System.Drawing.FontStyle]::Bold)), (Brush "#f8fafc"), 166, 740)
  $g.DrawString("AmneziaWG tunnel active", $body, (Brush "#bae6fd"), 166, 796)

  RoundedRect $g (Brush "#0f172a") 690 280 1140 680 36
  $g.DrawString("Notifications", $heading, (Brush "#f8fafc"), 742, 332)
  $cards = @(
    @("System Update", "Admin notices and service updates appear here.", "Now"),
    @("VPN Access", "Your tablet is ready to connect securely.", "Today"),
    @("Support", "Telegram support: @myozin99", "SafeNet")
  )
  $x = 742
  foreach ($card in $cards) {
    RoundedRect $g (Brush "#13243a") $x 430 320 324 26
    RoundedRect $g (Brush "#0ea5e9") ($x + 34) 466 58 58 18
    $g.DrawString($card[0], (FontObj "Segoe UI" 28 ([System.Drawing.FontStyle]::Bold)), (Brush "#f8fafc"), ($x + 34), 548)
    $g.DrawString($card[2], $small, (Brush "#38bdf8"), ($x + 34), 588)
    $g.DrawString($card[1], $body, (Brush "#cbd5e1"), [System.Drawing.RectangleF]::new(($x + 34),636,250,84))
    $x += 354
  }
  RoundedRect $g (Brush "#082f49") 742 814 1034 92 24
  $g.DrawString("Fast VPN controls, server visibility, and admin alerts.", $body, (Brush "#e0f2fe"), [System.Drawing.RectangleF]::new(782,840,954,44), $sf)

  Save-Png $bmp $g (Join-Path $out "tablet-10-inch-screenshot-1.png")
}

Draw-AppIcon
Draw-FeatureGraphic
Draw-HomeScreenshot
Draw-NotificationsScreenshot
Draw-TabletScreenshot

@"
SafeNet VPN Play Store Assets

Generated files:
- app-icon-512.png
- feature-graphic-1024x500.png
- phone-screenshot-1-home.png
- phone-screenshot-2-notifications.png
- tablet-10-inch-screenshot-1.png

Privacy Policy URL:
https://safenetapp.truehand.top/privacy
"@ | Set-Content -Encoding ASCII (Join-Path $out "README.txt")
