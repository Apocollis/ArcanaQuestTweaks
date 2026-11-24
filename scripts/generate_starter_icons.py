"""Generates the six starter class emblems as hand-pixelled sprites in the style of the perk icons.

Run from the repo root:  python scripts/generate_starter_icons.py [--preview]
Each emblem is drawn on a 16x16 grid (outline added automatically around every opaque pixel, light from the top left,
three-step ramps per material) and written at 2x nearest-neighbour as 32x32 to
src/main/resources/assets/aqtweaks/textures/gui/starter/class_<id>.png.  --preview also writes a sheet to
.plans/assets/starter_icons_preview.png (the six icons on the panel colour, at 1x and 8x).
"""
import os
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'aqtweaks', 'textures', 'gui', 'starter')
OUTLINE = (0x2B, 0x1D, 0x33)

PAL = {
    # steel light to dark
    'a': '#f2f6fb', 'b': '#c3cddb', 'c': '#8794ab', 'd': '#55607a',
    # gold light, mid, dark
    'e': '#ffe9a0', 'f': '#f2bd45', 'g': '#b07a1c',
    # red
    'r': '#e8695a', 'h': '#b83a36', 'k': '#7d2428',
    # green
    'l': '#98dd72', 'm': '#4aa04f', 'n': '#2c6f3a',
    # wood
    'p': '#d9a35a', 'q': '#a8742f', 's': '#6e4423',
    # leather
    't': '#c98a58', 'u': '#8f5a35', 'v': '#5c3820',
    # parchment
    'w': '#faf1dc', 'x': '#dcc9a4', 'y': '#b39a6e',
    # stone
    'A': '#c4c9d6', 'B': '#8a91a6', 'C': '#5b6177',
    # violet
    'D': '#c9a0f5', 'E': '#8a5fcc', 'F': '#523a94',
    # ore / glints
    'G': '#ffb347', 'H': '#d9822b', 'I': '#9c5a1c', 'J': '#ffffff',
    # blue
    'K': '#7fb2e8', 'L': '#4a78b8',
    # window glow
    'M': '#ffe27a', 'N': '#ff9d3b',
    # smoke
    'S': '#dfe3ec',
}


def rgb(ch):
    h = PAL[ch].lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


class Sprite:
    def __init__(self):
        self.px = {}

    def put(self, x, y, ch):
        if 0 <= x < 14 and 0 <= y < 14:
            self.px[(x, y)] = ch

    def rows(self, top, rows, left=0):
        for dy, row in enumerate(rows):
            for dx, ch in enumerate(row):
                if ch != '.':
                    self.put(left + dx, top + dy, ch)

    def line(self, x0, y0, x1, y1, ch):
        dx, dy = abs(x1 - x0), -abs(y1 - y0)
        sx = 1 if x0 < x1 else -1
        sy = 1 if y0 < y1 else -1
        err = dx + dy
        while True:
            self.put(x0, y0, ch)
            if x0 == x1 and y0 == y1:
                break
            e2 = 2 * err
            if e2 >= dy:
                err += dy
                x0 += sx
            if e2 <= dx:
                err += dx
                y0 += sy

    def image(self):
        img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        # the 14x14 art sits at offset (1,1) so the outline always fits
        solid = {(x + 1, y + 1): ch for (x, y), ch in self.px.items()}
        for (x, y), ch in solid.items():
            img.putpixel((x, y), rgb(ch) + (255,))
        for y in range(16):
            for x in range(16):
                if (x, y) in solid:
                    continue
                if any(((x + dx, y + dy) in solid) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    img.putpixel((x, y), OUTLINE + (255,))
        return img


# ------------------------------------------------------------------ the six emblems

def warrior():
    s = Sprite()
    # a steel heater shield, red field in two tones, a round gold boss and a worn rim
    s.rows(0, [
        'aaaaaaaaaaaabc',
        'arrrrrrhhhhhkc',
        'arrrrrrhhhhhkc',
        'arrrrrrhhhhhkc',
        'arrrrrrhhhhhkc',
        'arrrrrrhhhhhkc',
        'arrrrrrhhhhhkc',
        'arrrrrrhhhhhkc',
        '.arrrrrhhhhkc.',
        '.arrrrrhhhhkc.',
        '..arrrrhhhkc..',
        '...arrrhhkc...',
        '....arrhkc....',
        '.....ahkc.....',
    ])
    s.rows(3, ['.ee.', 'effg', 'fggg', '.gg.'], left=5)
    s.put(6, 4, 'J')
    s.put(1, 1, 'e')   # rivets on the rim
    s.put(12, 1, 'g')
    return s


def archer():
    s = Sprite()
    # bow: arc bulging left, wood light on the outside
    arc = [(8, 0), (7, 1), (6, 2), (5, 3), (4, 4), (4, 5), (3, 6), (3, 7), (4, 8), (4, 9), (5, 10), (6, 11), (7, 12), (8, 13)]
    for i, (x, y) in enumerate(arc):
        s.put(x, y, 'p')
        s.put(x + 1, y, 'q')
    # the bow tips
    s.put(9, 0, 's')
    s.put(9, 13, 's')
    # string
    s.line(9, 2, 9, 11, 'b')
    # arrow: shaft, steel head up and right, red fletching down and left
    s.line(3, 11, 10, 4, 'x')
    s.line(3, 12, 10, 5, 'y')
    for (x, y, ch) in [(13, 0, 'a'), (12, 1, 'a'), (13, 1, 'b'), (11, 2, 'b'), (12, 2, 'b'), (13, 2, 'c'), (11, 3, 'c'),
                       (12, 3, 'd'), (10, 3, 'b')]:
        s.put(x, y, ch)
    for (x, y, ch) in [(1, 13, 'h'), (2, 12, 'r'), (1, 12, 'r'), (2, 13, 'h'), (3, 11, 'r'), (2, 11, 'r'), (3, 12, 'h'),
                       (0, 13, 'k'), (2, 10, 'r')]:
        s.put(x, y, ch)
    return s


def scout():
    s = Sprite()
    # a dagger on the diagonal: steel blade (light edge, dark edge), gold guard, leather grip, gold pommel
    s.line(10, 3, 7, 6, 'a')
    s.line(11, 3, 8, 6, 'b')
    s.line(11, 4, 9, 6, 'c')
    s.put(11, 2, 'a')
    s.put(12, 3, 'c')
    # guard across the blade base
    s.line(4, 5, 9, 10, 'f')
    s.line(5, 5, 9, 9, 'e')
    s.line(4, 6, 8, 10, 'g')
    # grip
    s.line(5, 8, 2, 11, 'u')
    s.line(6, 8, 3, 11, 't')
    s.line(5, 9, 3, 11, 'v')
    # pommel
    s.rows(11, ['.ff', 'fge', '.gg'], left=0)
    # a rope loop and a small blue gem in the guard
    s.put(6, 7, 'K')
    return s


def miner():
    s = Sprite()
    # pickaxe: wooden haft bottom-left to top-right, steel head curving over the top
    s.line(1, 12, 9, 4, 'q')
    s.line(2, 12, 10, 4, 'p')
    s.line(1, 13, 9, 5, 's')
    # head: a curved bar through the top of the haft
    head = [(2, 3), (3, 2), (4, 1), (5, 1), (6, 1), (7, 1), (8, 1), (9, 1), (10, 1), (11, 2), (12, 3), (12, 4)]
    for (x, y) in head:
        s.put(x, y, 'b')
        s.put(x, y + 1, 'c')
    for (x, y) in [(4, 1), (5, 1), (6, 1), (7, 1), (8, 1)]:
        s.put(x, y, 'a')
    s.put(2, 3, 'b')
    s.put(12, 4, 'd')
    s.put(12, 5, 'd')
    s.put(2, 4, 'd')
    # an ore chunk with glints
    s.rows(9, ['....', '.HG.', 'HGGI', '.III'], left=10)
    s.put(11, 10, 'J')
    return s


def homesteader():
    s = Sprite()
    # chimney first (the roof overlaps its base) and a puff of smoke
    s.rows(1, ['AA', 'BC', 'BC', 'BC'], left=8)
    s.put(9, 0, 'S')
    s.put(10, 0, 'S')
    # roof: red, light on the left, dark on the right, overhanging the walls
    s.rows(3, [
        '......rh......',
        '.....rrhh.....',
        '....rrrhhh....',
        '...rrrrhhhk...',
        '..rrrrrhhhhk..',
        '.rrrrrrhhhhhk.',
    ])
    s.rows(9, [
        '..wwwwwwwwxx..',
        '..wwwwwwwwxx..',
        '..wwwwwwwwxx..',
        '..wwwwwwwwxx..',
        '..wwwwwwwwxy..',
        '..xxxxxxxxyy..',
    ])
    # door and lit window
    s.rows(9, ['qq', 'qs', 'qs', 'us'], left=5)
    s.put(6, 11, 'f')
    s.rows(9, ['MM', 'MN'], left=9)
    s.line(9, 8, 10, 8, 's')
    return s


def scholar():
    s = Sprite()
    # a four-point star over an open tome
    s.rows(0, [
        '......ee......',
        '......ef......',
        '....eeeeff....',
        '.....eefff....',
        '......ff......',
    ])
    # open book: violet cover, cream pages, writing lines
    s.rows(5, [
        '.DDDDDDDDDDDD.',
        'DEwwwwwFwwwwxF',
        'DEwyywwFwwyyxF',
        'DEwwwwwFwwwwxF',
        'DEwyyywFwyyyxF',
        'DEwwwwwFwwwwxF',
        'DEwyywwFwwyyxF',
        'DExxxxxFxxxxxF',
        '.FFFFFFFFFFFF.',
    ])
    s.put(7, 5, 'D')
    # ribbon
    s.put(7, 12, 'h')
    s.put(7, 13, 'k')
    return s


ICONS = [('warrior', warrior), ('archer', archer), ('scout', scout), ('miner', miner), ('homesteader', homesteader),
         ('scholar', scholar)]


def main():
    os.makedirs(OUT, exist_ok=True)
    sheet = Image.new('RGBA', (6 * 16 * 9 + 8, 16 * 9 + 8 + 32 + 8), (0x4F, 0x47, 0x3C, 255))
    for i, (name, fn) in enumerate(ICONS):
        img = fn().image()
        big = img.resize((32, 32), Image.NEAREST)
        big.save(os.path.join(OUT, 'class_%s.png' % name))
        mag = img.resize((16 * 8, 16 * 8), Image.NEAREST)
        sheet.paste(mag, (4 + i * 16 * 9, 4), mag)
        sheet.paste(big, (4 + i * 16 * 9, 16 * 8 + 12), big)
    if '--preview' in sys.argv:
        folder = os.path.join(ROOT, '.plans', 'assets')
        os.makedirs(folder, exist_ok=True)
        sheet.save(os.path.join(folder, 'starter_icons_preview.png'))
        print('preview written')
    print('wrote', len(ICONS), 'icons to', OUT)


if __name__ == '__main__':
    main()
