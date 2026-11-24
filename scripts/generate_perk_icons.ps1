# Generates the 16x16 perk icons for Heartwood, Forager, Vein Rhythm, Marathon Runner, and Cardio Master (pixel art in
# the style of the other unlockable icons). Run from the repo root: powershell -File scripts\generate_perk_icons.ps1
Add-Type -AssemblyName System.Drawing

$out = Join-Path (Split-Path -Parent $PSScriptRoot) 'src\main\resources\assets\aqtweaks\textures\unlockables'

function C([string]$hex) { return [Drawing.ColorTranslator]::FromHtml($hex) }

function Save-Icon([string]$name, [string[]]$rows, [hashtable]$palette) {
    if ($rows.Count -ne 16) { throw "$name has $($rows.Count) rows" }
    $bmp = New-Object Drawing.Bitmap 16, 16, ([Drawing.Imaging.PixelFormat]::Format32bppArgb)
    for ($y = 0; $y -lt 16; $y++) {
        if ($rows[$y].Length -ne 16) { throw "$name row $y has $($rows[$y].Length) columns" }
        for ($x = 0; $x -lt 16; $x++) {
            $ch = [string]$rows[$y][$x]
            if ($ch -eq '.') { $bmp.SetPixel($x, $y, [Drawing.Color]::FromArgb(0, 0, 0, 0)) }
            else { $bmp.SetPixel($x, $y, (C $palette[$ch])) }
        }
    }
    $bmp.Save((Join-Path $out "$name.png"), [Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

# Heartwood: the cut end of a log, bark ring, growth rings and a glowing heart.
$heart = @{
    'O' = '#2b1d33'; 'B' = '#5b3a24'; 'D' = '#46291a'; 'W' = '#dba966'; 'M' = '#b98443'
    'R' = '#d4513a'; 'H' = '#ffb255'; 'Y' = '#fff0a8'
}
Save-Icon 'heartwood' @(
    '....OOOOOOOO....',
    '..OOBBBBBBBBOO..',
    '.OBBDBBBBBBBDBO.',
    '.OBWWWWWWWWWWBO.',
    'OBBWMMMMMMMMWBBO',
    'OBWMWWWWWWWWMWBO',
    'OBWMWMMMMMMWMWBO',
    'OBWMWMRRRRMWMWBO',
    'OBWMWMRHYRMWMWBO',
    'OBWMWMRHHRMWMWBO',
    'OBWMWMRRRRMWMWBO',
    'OBWMWMMMMMMWMWBO',
    'OBWMWWWWWWWWMWBO',
    'OBBWMMMMMMMMWBBO',
    '.OBBWWWWWWWWBBO.',
    '..OOOOOOOOOOOO..'
) $heart

# Forager: a wicker basket full of greens with a red mushroom and a yellow flower.
$forage = @{
    'O' = '#2b1d33'; 'G' = '#3f9a4a'; 'K' = '#2c6f3a'; 'L' = '#8bd36f'
    'R' = '#d63a3a'; 's' = '#f6ecd8'; 'Y' = '#f6c93b'; 'Z' = '#c8861f'
    'T' = '#d9a35a'; 'A' = '#a8742f'; 'U' = '#7a4f22'
}
Save-Icon 'forager' @(
    '................',
    '.....OOOO.......',
    '....ORRRRO.OYO..',
    '...ORRsRRRO.YZYO',
    '...ORRRRsRO.OYO.',
    '....OOssOO..OGO.',
    '....OGsGOGGOOGO.',
    '..OOGGGGGGGKGGO.',
    '.OGLGGKGGLGGGGO.',
    '.OOOOOOOOOOOOOO.',
    '.OTTATTTATTTATO.',
    '.OATTATTTATTTAO.',
    '..OTATTATTATTO..',
    '..OATTATTTATTO..',
    '...OTATTATTAO...',
    '....OOOOOOOO....'
) $forage

# Vein Rhythm: connected steel pickaxe with curving swing-speed arcs trailing behind the head.
$veinRhythm = @{
    'O' = '#2b1d33'
    'S' = '#f2f6fb'; 'M' = '#c3cddb'; 'D' = '#7a879e'
    'L' = '#d9a35a'; 'W' = '#a8742f'; 'B' = '#6e4423'
    '1' = '#ffffff'; '2' = '#9be2ff'; '3' = '#4ba3e3'
}
Save-Icon 'vein_rhythm' @(
    '.....322111.....',
    '...32...........',
    '..2....3211.....',
    '..3..32.........',
    '........OOOOOO..',
    '.......OMSSSLO..',
    '.......OODDOMWO.',
    '..........OWBMSO',
    '.........OWBOOMO',
    '........OWBO.ODO',
    '.......OWBO..OSO',
    '......OWBO....OO',
    '.....OWBO.......',
    '....OWBO........',
    '...OWBO.........',
    '..OOOO..........'
) $veinRhythm

# Marathon Runner: cyan/white sprinting runner silhouette.
$marathonRunner = @{
    'O' = '#2b1d33'; 'W' = '#f2f6fb'
    '1' = '#ffffff'; '2' = '#9be2ff'; '3' = '#2e8bd6'
}
Save-Icon 'marathon_runner' @(
    '................',
    '.........OOO....',
    '........O21WO...',
    '........O322O...',
    '....OOOO.OOO....',
    '...O2112O22O....',
    '..O32OO21122OOO.',
    '..OO...O322111O.',
    '.......O322OOOO.',
    '......O3232O....',
    '....OO32OO32O...',
    '...O212O..O32O..',
    '..O322O....O32O.',
    '..OOOO......OOO.',
    '................',
    '................'
) $marathonRunner

# Cardio Master: Elenai Dodge 9x9 blue feather with outlined bright green plus.
$cardioMaster = @{
    'O' = '#2b1d33'; 'A' = '#000000'
    'B' = '#22a5f0'; 'C' = '#bedcfa'; 'D' = '#0f7bb4'
    'L' = '#8bff62'; 'G' = '#00bf0d'; 'E' = '#007809'
}
Save-Icon 'cardio_master' @(
    '................',
    '..........OOO...',
    '..........OLO...',
    '........OOOGOOO.',
    '........OLGGGGO.',
    '..AAA...OOOGOOO.',
    '.ABBCA....OEO...',
    '.ABDBCA...OOO...',
    '.ADBDBBA........',
    '..ADBDBBA.......',
    '...ADBDBA.......',
    '....ADBDA.......',
    '.....AAAA.......',
    '.........A......',
    '................',
    '................'
) $cardioMaster

"Wrote heartwood.png, forager.png, vein_rhythm.png, marathon_runner.png, and cardio_master.png to $out"
