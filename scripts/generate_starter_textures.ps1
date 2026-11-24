# Generates the starter class screen textures (Stoneborn-style frames and the six class emblems).
# Run from the repo root:  powershell -ExecutionPolicy Bypass -File scripts\generate_starter_textures.ps1
# Output: src/main/resources/assets/aqtweaks/textures/gui/starter/*.png
#
# Palette sampled from the Stoneborn pack (STONEBORN-1.12.2-V3.1: generic_54.png, widgets.png):
#   outline #000000, highlight #675D4E, body #4F473C, shadow #221E19, inset #151310, button #37322A,
#   gold #BD862B, dark gold #493706.
# The textures ship in the mod jar, so they render with Stoneborn on or off; the look matches it.

Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$out = Join-Path $root 'src\main\resources\assets\aqtweaks\textures\gui\starter'
New-Item -ItemType Directory -Force $out | Out-Null

function C([string]$hex) { return [Drawing.ColorTranslator]::FromHtml($hex) }

$black = C '#000000'
$hiStone = C '#675D4E'
$body = C '#4F473C'
$shadow = C '#221E19'
$inset = C '#151310'
$btn = C '#37322A'
$gold = C '#BD862B'
$darkGold = C '#493706'

# ---------------------------------------------------------------- nine-slice frames

# $rings: array of @(hi, lo) colours by distance from the edge; $fill: colour inside the rings.
# Pixels on the top/left side of a ring use hi, bottom/right use lo. The four extreme corner pixels are transparent.
function New-Frame([string]$name, [int]$w, [int]$h, $rings, $fill, $accents) {
    $bmp = New-Object Drawing.Bitmap $w, $h, ([Drawing.Imaging.PixelFormat]::Format32bppArgb)
    for ($y = 0; $y -lt $h; $y++) {
        for ($x = 0; $x -lt $w; $x++) {
            $d = [Math]::Min([Math]::Min($x, $y), [Math]::Min($w - 1 - $x, $h - 1 - $y))
            $isCorner = (($x -eq 0 -or $x -eq $w - 1) -and ($y -eq 0 -or $y -eq $h - 1))
            if ($isCorner) { $bmp.SetPixel($x, $y, [Drawing.Color]::FromArgb(0, 0, 0, 0)); continue }
            if ($d -lt $rings.Count) {
                $topLeft = (($y -eq $d) -and ($x -lt $w - 1 - $d)) -or (($x -eq $d) -and ($y -lt $h - 1 - $d))
                $pair = $rings[$d]
                if ($topLeft) { $bmp.SetPixel($x, $y, $pair[0]) } else { $bmp.SetPixel($x, $y, $pair[1]) }
            } else {
                $bmp.SetPixel($x, $y, $fill)
            }
        }
    }
    foreach ($a in $accents) {
        $ax = $a[0]; $ay = $a[1]; $col = $a[2]
        foreach ($p in @(@($ax, $ay), @(($w - 1 - $ax), $ay), @($ax, ($h - 1 - $ay)), @(($w - 1 - $ax), ($h - 1 - $ay)))) {
            $bmp.SetPixel($p[0], $p[1], $col)
        }
    }
    $bmp.Save((Join-Path $out $name), [Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

# Window panel: black edge, bevel, recessed inner frame, gold filigree in each corner.
$mid = C '#3C352C'
$mid2 = C '#5A5144'
$panelRings = @(
    @($black, $black),
    @($hiStone, $shadow),
    @($body, $body), @($body, $body), @($body, $body), @($body, $body),
    @($shadow, $mid2),
    @($mid, $mid)
)
$panelAccents = @(
    @(2, 2, $gold), @(3, 2, $gold), @(4, 2, $gold), @(5, 2, $gold),
    @(2, 3, $gold), @(2, 4, $gold), @(2, 5, $gold),
    @(3, 3, $darkGold), @(4, 3, $darkGold), @(3, 4, $darkGold)
)
New-Frame 'panel.png' 64 64 $panelRings $body $panelAccents

# Option rows: inset boxes. Idle is dark stone, hover has a gold-brown edge, selected a bright gold edge.
$optIdleRings = @(@($black, $black), @($btn, $hiStone))
New-Frame 'option.png' 32 32 $optIdleRings $inset @()
$optHoverRings = @(@($black, $black), @((C '#8A6A22'), (C '#493706')))
New-Frame 'option_hover.png' 32 32 $optHoverRings (C '#1B1812') @()
$optSelRings = @(@($black, $black), @($gold, $gold), @((C '#7A5A1E'), (C '#7A5A1E')))
New-Frame 'option_selected.png' 32 32 $optSelRings (C '#2A2113') @(@(2, 2, $gold), @(3, 2, $gold), @(2, 3, $gold))

# Buttons.
New-Frame 'button.png' 32 20 @(@($black, $black), @($hiStone, $shadow)) $btn @()
New-Frame 'button_hover.png' 32 20 @(@($black, $black), @($gold, $darkGold)) (C '#4A4336') @()
New-Frame 'button_disabled.png' 32 20 @(@($black, $black), @($btn, $inset)) (C '#28241E') @()

# Divider: thin gold line over a dark one, stretched horizontally (32 x 4).
$div = New-Object Drawing.Bitmap 32, 4, ([Drawing.Imaging.PixelFormat]::Format32bppArgb)
for ($x = 0; $x -lt 32; $x++) {
    $div.SetPixel($x, 0, [Drawing.Color]::FromArgb(0, 0, 0, 0))
    $div.SetPixel($x, 1, $gold)
    $div.SetPixel($x, 2, $shadow)
    $div.SetPixel($x, 3, [Drawing.Color]::FromArgb(0, 0, 0, 0))
}
$div.Save((Join-Path $out 'divider.png'), [Drawing.Imaging.ImageFormat]::Png)
$div.Dispose()

# ---------------------------------------------------------------- class emblems (32 x 32)

$steel = C '#B8B8C0'
$steelMid = C '#7E7E88'
$steelDark = C '#4A4A52'
$wood = C '#8A5A2B'
$woodDark = C '#5A3A1A'
$red = C '#A02A2A'
$leather = C '#7A4B2A'
$parch = C '#D8C79A'
$parchDark = C '#A8946A'
$fire = C '#E07A20'
$yellow = C '#F0C840'
$roof = C '#8A3A2A'
$wall = C '#A08A64'

function New-Canvas() {
    $b = New-Object Drawing.Bitmap 32, 32, ([Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [Drawing.Graphics]::FromImage($b)
    $g.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::None
    $g.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::Half
    $g.Clear([Drawing.Color]::FromArgb(0, 0, 0, 0))
    return @{ Bitmap = $b; G = $g }
}

# A thick line with a black outline.
function Thick($g, [float]$x1, [float]$y1, [float]$x2, [float]$y2, [float]$w, $col) {
    $po = New-Object Drawing.Pen $black, ($w + 2)
    $po.StartCap = 'Round'; $po.EndCap = 'Round'
    $g.DrawLine($po, $x1, $y1, $x2, $y2)
    $pc = New-Object Drawing.Pen $col, $w
    $pc.StartCap = 'Round'; $pc.EndCap = 'Round'
    $g.DrawLine($pc, $x1, $y1, $x2, $y2)
    $po.Dispose(); $pc.Dispose()
}

# A filled polygon with a black outline.
function Poly($g, $pts, $col) {
    $arr = [Drawing.PointF[]]$pts
    $br = New-Object Drawing.SolidBrush $col
    $g.FillPolygon($br, $arr)
    $pen = New-Object Drawing.Pen $black, 1
    $g.DrawPolygon($pen, $arr)
    $br.Dispose(); $pen.Dispose()
}

function Save-Canvas($cv, [string]$name) {
    $cv.G.Dispose()
    $cv.Bitmap.Save((Join-Path $out $name), [Drawing.Imaging.ImageFormat]::Png)
    $cv.Bitmap.Dispose()
}

function P([float]$x, [float]$y) { return New-Object Drawing.PointF $x, $y }

# The six class emblems (class_*.png) are drawn by scripts/generate_starter_icons.py (hand-pixelled sprites).
# This script no longer writes them, so running it never overwrites the emblems.

Write-Output "Wrote textures to $out"
Get-ChildItem $out -Filter *.png | ForEach-Object {
    $im = [Drawing.Bitmap]::FromFile($_.FullName); "{0}  {1}x{2}" -f $_.Name, $im.Width, $im.Height; $im.Dispose()
}
