"""Native 32x32 variants of the six starter class emblems (more detail than the 16x16-at-2x set).

Run from the repo root:
  python scripts/generate_starter_icons_32.py            -> writes only the comparison sheet .plans/assets/starter_icons_32_preview.png
  python scripts/generate_starter_icons_32.py --write    -> also overwrites the six class_<id>.png files (32x32)

Drawing model: shapes are filled with a light-to-dark ramp across the shape (light from the top left), a bevel pass
lightens pixels on a top or left edge and darkens those on a bottom or right edge, and a 1 px outline #2b1d33 is added
around everything. Colours match the 16x16 set and the perk icons.
"""
import math
import os
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'aqtweaks', 'textures', 'gui', 'starter')
SHEET = os.path.join(ROOT, '.plans', 'assets', 'starter_icons_32_preview.png')
OUTLINE = (0x2B, 0x1D, 0x33)
N = 30  # inner canvas; the outline sits at offset 1

RAMPS = {
    'steel': ['#f2f6fb', '#c3cddb', '#8794ab', '#55607a', '#3b455c'],
    'gold': ['#fff3b8', '#ffe9a0', '#f2bd45', '#b07a1c', '#7a5212'],
    'red': ['#f08a7a', '#e8695a', '#b83a36', '#7d2428', '#561519'],
    'wood': ['#ecbf80', '#d9a35a', '#a8742f', '#6e4423', '#4a2c14'],
    'leather': ['#d9a074', '#c98a58', '#8f5a35', '#5c3820', '#3c2313'],
    'paper': ['#fffaf0', '#faf1dc', '#dcc9a4', '#b39a6e', '#8a7348'],
    'stone': ['#d5d9e4', '#c4c9d6', '#8a91a6', '#5b6177', '#3f4458'],
    'violet': ['#ddc0ff', '#c9a0f5', '#8a5fcc', '#523a94', '#37266a'],
    'green': ['#b6ee90', '#98dd72', '#4aa04f', '#2c6f3a', '#1d4a27'],
    'ore': ['#ffd17a', '#ffb347', '#d9822b', '#9c5a1c', '#6a3a10'],
    'glow': ['#fffbd0', '#ffe27a', '#ffc04a', '#ff9d3b', '#d9702a'],
    'blue': ['#b9d9ff', '#7fb2e8', '#4a78b8', '#2f4f88', '#1e3560'],
    'smoke': ['#ffffff', '#f0f3f8', '#dfe3ec', '#c0c6d4', '#9aa2b4'],
}


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


class Canvas:
    def __init__(self):
        self.px = {}   # (x, y) -> (family, ramp index)

    def set(self, x, y, fam, idx):
        if 0 <= x < N and 0 <= y < N:
            self.px[(x, y)] = (fam, max(0, min(len(RAMPS[fam]) - 1, idx)))

    def clear(self, x, y):
        self.px.pop((x, y), None)

    # ---- shapes --------------------------------------------------------------------------------------------
    def poly(self, pts, fam, shade=(0.0, 1.0), light=(-1.0, -1.0)):
        """Scanline fill; shade is the (start, end) ramp position (0 lightest, 1 darkest) across the shape."""
        xs = [p[0] for p in pts]
        ys = [p[1] for p in pts]
        x0, x1, y0, y1 = min(xs), max(xs), min(ys), max(ys)
        lx, ly = light
        norm = math.hypot(lx, ly) or 1.0
        lx, ly = lx / norm, ly / norm
        proj = [(x - x0) * -lx + (y - y0) * -ly for x, y in pts]
        pmin, pmax = min(proj), max(proj)
        span = (pmax - pmin) or 1.0
        n = len(RAMPS[fam])
        for y in range(int(math.floor(y0)), int(math.ceil(y1)) + 1):
            for x in range(int(math.floor(x0)), int(math.ceil(x1)) + 1):
                if self._inside(x + 0.5, y + 0.5, pts):
                    t = (((x - x0) * -lx + (y - y0) * -ly) - pmin) / span
                    t = shade[0] + (shade[1] - shade[0]) * max(0.0, min(1.0, t))
                    pos = t * (n - 1)
                    base = int(math.floor(pos))
                    frac = pos - base
                    if frac > 0.5 + (0.12 if (x + y) % 2 else -0.12):
                        base += 1
                    self.set(x, y, fam, base)

    @staticmethod
    def _inside(px, py, pts):
        inside = False
        j = len(pts) - 1
        for i in range(len(pts)):
            xi, yi = pts[i]
            xj, yj = pts[j]
            if (yi > py) != (yj > py) and px < (xj - xi) * (py - yi) / ((yj - yi) or 1e-9) + xi:
                inside = not inside
            j = i
        return inside

    def rect(self, x, y, w, h, fam, shade=(0.0, 1.0), light=(-1.0, -1.0)):
        self.poly([(x, y), (x + w, y), (x + w, y + h), (x, y + h)], fam, shade, light)

    def ellipse(self, cx, cy, rx, ry, fam, shade=(0.0, 1.0)):
        pts = [(cx + rx * math.cos(a * math.pi / 18), cy + ry * math.sin(a * math.pi / 18)) for a in range(36)]
        self.poly(pts, fam, shade)

    def thick(self, x0, y0, x1, y1, w, fam, shade=(0.0, 1.0)):
        dx, dy = x1 - x0, y1 - y0
        ln = math.hypot(dx, dy) or 1.0
        nx, ny = -dy / ln * w / 2, dx / ln * w / 2
        self.poly([(x0 + nx, y0 + ny), (x1 + nx, y1 + ny), (x1 - nx, y1 - ny), (x0 - nx, y0 - ny)], fam, shade)

    def dot(self, x, y, fam, idx):
        self.set(x, y, fam, idx)

    # ---- passes --------------------------------------------------------------------------------------------
    def bevel(self, fams=None):
        """Lighten pixels with a transparent neighbour above or to the left, darken those below or to the right."""
        out = {}
        for (x, y), (fam, idx) in self.px.items():
            if fams and fam not in fams:
                continue
            light = ((x, y - 1) not in self.px) or ((x - 1, y) not in self.px)
            dark = ((x, y + 1) not in self.px) or ((x + 1, y) not in self.px)
            if light and not dark:
                out[(x, y)] = (fam, idx - 1)
            elif dark and not light:
                out[(x, y)] = (fam, idx + 1)
        for k, (fam, idx) in out.items():
            self.px[k] = (fam, max(0, min(len(RAMPS[fam]) - 1, idx)))

    def image(self):
        img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
        solid = {}
        for (x, y), (fam, idx) in self.px.items():
            solid[(x + 1, y + 1)] = hexrgb(RAMPS[fam][idx])
        for (x, y), c in solid.items():
            img.putpixel((x, y), c + (255,))
        for y in range(32):
            for x in range(32):
                if (x, y) in solid:
                    continue
                if any(((x + dx, y + dy) in solid) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    img.putpixel((x, y), OUTLINE + (255,))
        return img



def rot(pts, deg, ox, oy):
    """Rotate local points clockwise (screen coordinates) by deg and move them to (ox, oy)."""
    a = math.radians(deg)
    ca, sa = math.cos(a), math.sin(a)
    return [(ox + x * ca - y * sa, oy + x * sa + y * ca) for (x, y) in pts]


def tpoly(c, pts, deg, ox, oy, fam, shade=(0.0, 1.0)):
    c.poly(rot(pts, deg, ox, oy), fam, shade)


def tthick(c, x0, y0, x1, y1, w, deg, ox, oy, fam, shade=(0.0, 1.0)):
    (a, b), (d, e) = rot([(x0, y0), (x1, y1)], deg, ox, oy)
    c.thick(a, b, d, e, w, fam, shade)


def tdot(c, x, y, deg, ox, oy, fam, idx):
    (a, b), = rot([(x, y)], deg, ox, oy)
    c.dot(int(round(a)), int(round(b)), fam, idx)


# ------------------------------------------------------------------------------------------------ the six emblems

def warrior():
    c = Canvas()
    # sword behind the shield: a clean straight-edged blade from the top right, hilt outside the lower left of the shield
    c.poly([(25.8, 1.8), (29.1, 0.9), (28.2, 4.2), (11.2, 21.2), (8.8, 18.8)], 'steel', (0.0, 1.0), light=(-1, -1))
    c.thick(27.4, 2.6, 11.5, 18.5, 0.8, 'steel', (0.0, 0.1))     # a light fuller down the middle
    # hilt exactly on the blade's axis (27,3) to (3,27): crossguard square to it, then grip and pommel
    c.thick(4, 21, 9, 26, 2.4, 'gold', (0.1, 0.8))
    c.thick(5.6, 24.4, 2.4, 27.6, 2.8, 'leather')
    c.ellipse(1.8, 28.4, 1.9, 1.9, 'gold', (0.0, 0.8))
    # heater shield, moved 2 px to the right so more of the hilt shows
    dx = 2
    shield = [(3 + dx, 6), (22 + dx, 6), (22 + dx, 17), (20 + dx, 22), (15 + dx, 27), (12.5 + dx, 29), (10 + dx, 27), (5 + dx, 22), (3 + dx, 17)]
    c.poly(shield, 'steel', (0.1, 0.9))
    inner = [(5 + dx, 8), (20 + dx, 8), (20 + dx, 17), (18 + dx, 21), (14 + dx, 25), (12.5 + dx, 26.5), (11 + dx, 25), (7 + dx, 21), (5 + dx, 17)]
    c.poly(inner, 'red', (0.0, 1.0))
    c.ellipse(12.5 + dx, 14, 4.2, 4.2, 'gold', (0.0, 1.0))
    c.ellipse(12.0 + dx, 13.5, 2.0, 2.0, 'gold', (0.0, 0.3))
    c.dot(11 + dx, 12, 'gold', 0)
    for (x, y) in [(5, 7), (20, 7), (5, 16), (20, 16), (8, 22), (17, 22)]:
        c.dot(x + dx, y, 'steel', 0)
    c.bevel({'steel', 'gold'})
    return c

def archer():
    c = Canvas()
    # bow on the right, belly toward the target; string drawn back to the nock; arrow flies right
    limb = [(12, 2), (16, 4), (20, 8), (22, 13), (22, 18), (20, 23), (16, 27), (12, 29)]
    for i in range(len(limb) - 1):
        (x0, y0), (x1, y1) = limb[i], limb[i + 1]
        c.thick(x0, y0, x1, y1, 2.4, 'wood', (0.0, 1.0))
        c.thick(x0 - 0.5, y0 - 0.5, x1 - 0.5, y1 - 0.5, 0.8, 'wood', (0.0, 0.1))
    c.thick(22, 12, 22, 19, 3.0, 'leather', (0.0, 1.0))      # grip
    c.rect(10, 1, 3, 3, 'steel', (0.0, 0.7))                # nocks
    c.rect(10, 28, 3, 3, 'steel', (0.0, 0.7))
    # string: from the upper nock back to the arrow nock and out to the lower nock
    c.thick(11, 3, 7, 15, 1.0, 'paper', (0.2, 0.6))
    c.thick(7, 15, 11, 29, 1.0, 'paper', (0.2, 0.6))
    # arrow: horizontal at the middle, steel head at the right, three red fletching feathers at the left
    c.thick(4, 15, 26, 15, 2.2, 'wood', (0.0, 0.9))
    c.thick(4, 14.4, 26, 14.4, 0.8, 'wood', (0.0, 0.1))
    c.poly([(24, 10), (29, 15), (24, 20), (26, 15)], 'steel', (0.0, 0.9))
    c.poly([(2, 13), (7, 13), (9, 15), (4, 15)], 'red', (0.0, 0.7))
    c.poly([(2, 17), (7, 17), (9, 15), (4, 15)], 'red', (0.3, 1.0))
    c.bevel({'steel', 'wood', 'leather'})
    return c

def scout():
    c = Canvas()
    # two crossed daggers (a rogue's crest): each blade is longer than its handle and the daggers cross on the blades
    def dagger(deg, shade_start):
        # place the dagger so that the point 8 px up its blade is the common crossing point (15, 12)
        (cx, cy), = rot([(0, -6)], deg, 0, 0)
        ox, oy = 15 - cx, 13 - cy
        blade = [(0, -17), (1.6, -11), (2.0, -2.2), (-2.0, -2.2), (-1.6, -11)]
        tpoly(c, blade, deg, ox, oy, 'steel', (shade_start, 1.0))
        tthick(c, -0.6, -15, -0.6, -3, 0.8, deg, ox, oy, 'steel', (0.0, 0.1))
        tpoly(c, [(-4.6, -2.2), (4.6, -2.2), (4.6, 0.2), (-4.6, 0.2)], deg, ox, oy, 'gold', (0.0, 1.0))
        tdot(c, 0, -1.0, deg, ox, oy, 'blue', 1)
        tpoly(c, [(-1.7, 0.2), (1.7, 0.2), (1.7, 5.6), (-1.7, 5.6)], deg, ox, oy, 'leather', (0.0, 1.0))
        for y in (1.6, 3.4):
            tthick(c, -1.7, y, 1.7, y + 1.0, 0.8, deg, ox, oy, 'leather', (0.8, 1.0))
        (px, py), = rot([(0, 7.4)], deg, ox, oy)
        c.ellipse(px, py, 2.0, 2.0, 'gold', (0.0, 0.9))
    dagger(43, 0.25)     # tip up and to the right
    dagger(-43, 0.0)     # tip up and to the left, on top
    c.bevel({'steel', 'gold', 'leather'})
    return c

def miner():
    c = Canvas()
    # Reference-style pickaxe: 3 px diagonal wooden haft, tight steel socket hub with rounded wooden nub, and
    # smooth monotonic 1 px staircases along both the outer and inner curves of each crescent blade.

    # 1) Wooden haft along x + y = 29
    for y in range(7, 28):
        x = 29 - y
        c.set(x - 1, y, 'wood', 1)
        c.set(x, y, 'wood', 2)
        c.set(x + 1, y, 'wood', 3)
    c.clear(1, 27)
    c.clear(3, 27)
    c.set(2, 27, 'wood', 2)
    c.set(2, 26, 'wood', 1)
    c.clear(23, 7)
    c.set(21, 6, 'wood', 1)
    c.set(22, 6, 'wood', 2)
    c.set(22, 7, 'wood', 3)
    for y in (15, 20, 24):
        c.set(29 - y, y, 'wood', 3)

    # 2) Tight steel socket hub (wrapping (18..21, 8..11))
    hub = {
        7:  [19, 20],
        8:  [18, 19, 20, 21],
        9:  [18, 19, 20, 21],
        10: [18, 19, 20, 21],
        11: [19, 20, 21],
    }
    for y, xs in hub.items():
        for x in xs:
            c.set(x, y, 'steel', 2)
    for (x, y) in ((19, 7), (18, 8), (19, 8), (18, 9)):
        c.set(x, y, 'steel', 1)
    for (x, y) in ((21, 9), (20, 10), (21, 10), (20, 11), (21, 11)):
        c.set(x, y, 'steel', 3)

    # 3) Upper/left crescent arm: smooth staircase along both outer & inner edges, no concavity
    left_arm = {
        4: [11, 12, 13, 14, 15],
        5: [8, 9, 10, 11, 12, 13, 14, 15, 16, 17],
        6: [6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19],
        7: [15, 16, 17, 18],
    }
    for y, xs in left_arm.items():
        for x in xs:
            c.set(x, y, 'steel', 2)
    # Outer edge highlights (top edge of arc)
    for x in range(11, 16):
        c.set(x, 4, 'steel', 0)
    for x in (8, 9, 10, 16, 17):
        c.set(x, 5, 'steel', 0 if x <= 10 else 1)
    for x in (6, 7):
        c.set(x, 6, 'steel', 1)
    # Inner edge shadow (underside bevel)
    for x in range(9, 15):
        c.set(x, 6, 'steel', 3)
    for x in (15, 16, 17):
        c.set(x, 7, 'steel', 3)
    c.set(16, 7, 'steel', 4)

    # 4) Lower/right crescent arm: exact mirror across x + y = 29, shaded slightly darker
    for y, xs in left_arm.items():
        for x in xs:
            mx, my = 29 - y, 29 - x
            c.set(mx, my, 'steel', 3)
            if y == 4 or (y == 5 and x in (8, 9, 10, 16, 17)) or (y == 6 and x in (6, 7)):
                c.set(mx, my, 'steel', 1 if my < 17 else 2)
            elif (y == 6 and 9 <= x <= 14) or (y == 7 and 15 <= x <= 17):
                c.set(mx, my, 'steel', 4)
    c.set(23, 23, 'steel', 3)

    return c

def homesteader():
    c = Canvas()
    # chimney with smoke
    c.rect(19, 6, 5, 8, 'stone', (0.0, 1.0))
    c.rect(18, 5, 7, 2, 'stone', (0.0, 0.5))
    for (x, y, r) in [(22, 3, 1.6), (25, 1, 1.3)]:
        c.ellipse(x, y, r, r, 'smoke', (0.0, 0.7))
    # walls
    c.rect(4, 15, 22, 13, 'paper', (0.0, 1.0))
    c.rect(4, 26, 22, 3, 'stone', (0.0, 1.0))            # stone footing
    for x in range(5, 25, 4):
        c.dot(x, 27, 'stone', 3)
    # roof: red shingles, light left to dark right, overhanging
    roof = [(1, 16), (15, 2), (29, 16), (26, 16), (15, 6), (4, 16)]
    c.poly([(1, 16), (15, 2), (29, 16)], 'red', (0.0, 1.0))
    for y in range(5, 16, 3):
        for x in range(2, 29):
            if (y, x) and c.px.get((x, y)) is not None and (x + y) % 5 != 0:
                pass
    for y in range(6, 16, 3):
        for x in range(3, 28):
            if (x, y) in c.px:
                fam, idx = c.px[(x, y)]
                if fam == 'red':
                    c.dot(x, y, 'red', idx + 1)
    # door with an arch and a knob
    c.poly([(10, 28), (10, 20), (11, 18), (14, 17), (17, 18), (18, 20), (18, 28)], 'wood', (0.0, 1.0))
    c.thick(14, 18, 14, 28, 0.8, 'wood', (0.7, 0.9))
    c.dot(16, 24, 'gold', 1)
    # lit window with a cross frame
    c.rect(20, 18, 5, 5, 'glow', (0.0, 1.0))
    c.thick(22.5, 18, 22.5, 23, 0.9, 'wood', (0.5, 0.9))
    c.thick(20, 20.5, 25, 20.5, 0.9, 'wood', (0.5, 0.9))
    c.bevel({'paper', 'stone', 'wood', 'red'})
    return c


def scholar():
    c = Canvas()

    def fill(x0, y0, x1, y1, fam, idx):
        for yy in range(y0, y1 + 1):
            for xx in range(x0, x1 + 1):
                c.set(xx, yy, fam, idx)

    # Full-width open tome in the wave-arched reference silhouette, centred vertically on the 30x30 canvas.

    # Top and bottom curve y-coordinates for left-half columns x = 2..13 (mirrored on right x = 29 - x)
    top_y = {
        2: 8, 3: 8, 4: 8, 5: 7, 6: 7, 7: 6, 8: 5, 9: 5, 10: 5, 11: 6, 12: 7, 13: 8,
    }
    bot_y = {
        2: 20, 3: 20, 4: 20, 5: 19, 6: 18, 7: 17, 8: 17, 9: 17, 10: 18, 11: 19, 12: 20, 13: 21,
    }

    # 1) Violet cover frame: vertical outer rims, dark interior cover hollow, flat front cover bar, gold corners
    fill(1, 9, 1, 24, 'violet', 1)
    fill(28, 9, 28, 24, 'violet', 3)
    c.set(1, 9, 'violet', 2)
    c.set(28, 9, 'violet', 3)
    fill(2, 21, 27, 23, 'violet', 4)
    fill(2, 23, 27, 23, 'violet', 3)
    fill(1, 24, 28, 24, 'violet', 2)
    fill(14, 24, 28, 24, 'violet', 3)
    fill(13, 25, 16, 25, 'violet', 4)
    c.set(14, 25, 'violet', 3)
    c.set(15, 25, 'violet', 3)
    for (x, y, idx) in ((1, 24, 1), (2, 24, 1), (1, 23, 2), (28, 24, 2), (27, 24, 1), (28, 23, 2)):
        c.set(x, y, 'gold', idx)

    # 2) Wavy page stack underneath the open pages (4 rows following bot_y)
    for x in range(2, 14):
        yb = bot_y[x]
        for dy in range(1, 5):
            yy = yb + dy
            if yy <= 23:
                idx = 2 if dy == 1 else (4 if dy == 2 else (3 if dy == 3 else 4))
                c.set(x, yy, 'paper', idx)
                c.set(29 - x, yy, 'paper', min(4, idx + (1 if x <= 4 and dy == 1 else 0)))

    # 3) Open cream pages spanning x=2..13 and x=16..27
    for x in range(2, 14):
        yt, yb = top_y[x], bot_y[x]
        for y in range(yt, yb + 1):
            if x == 13:
                idx = 2
            elif x >= 11 or x <= 3:
                idx = 1
            else:
                idx = 0
            c.set(x, y, 'paper', idx)
            xr = 29 - x
            idxr = 2 if x == 13 else (1 if x >= 10 or x <= 4 else 0)
            c.set(xr, y, 'paper', idxr)
        if 4 <= x <= 11:
            c.set(x, yt, 'paper', 0)

    # 4) Faint horizontal text lines
    rows = [
        (10, (4, 11), (18, 25)),
        (12, (4, 10), (18, 24)),
        (14, (4, 11), (18, 25)),
        (16, (4, 9),  (18, 23)),
    ]
    for y, (la, lb), (ra, rb) in rows:
        for x in range(la, lb + 1):
            if (x * 3 + y) % 5 != 0:
                c.set(x, y, 'paper', 2)
        for x in range(ra, rb + 1):
            if (x * 3 + y) % 5 != 0:
                c.set(x, y, 'paper', 2)

    # 5) Dark purple spine line down the centre
    fill(14, 8, 15, 24, 'violet', 4)
    for y in range(9, 21, 3):
        c.set(14, y, 'violet', 3)
        c.set(15, y + 1, 'violet', 3)

    # 6) Red ribbon bookmark hanging over the bottom spine
    fill(14, 21, 15, 26, 'red', 2)
    c.set(14, 21, 'red', 1)
    c.set(15, 25, 'red', 3)
    c.set(14, 26, 'red', 3)
    c.set(15, 26, 'red', 3)

    return c

ICONS = [('warrior', warrior), ('archer', archer), ('scout', scout), ('miner', miner), ('homesteader', homesteader),
         ('scholar', scholar)]


def main():
    write = '--write' in sys.argv
    old_dir = OUT
    sheet = Image.new('RGBA', (6 * 32 * 8 + 4, 32 * 8 + 4 + 2 * (32 * 2 + 8) + 8), (0x4F, 0x47, 0x3C, 255))
    for i, (name, fn) in enumerate(ICONS):
        img = fn().image()
        mag = img.resize((32 * 8, 32 * 8), Image.NEAREST)
        sheet.paste(mag, (2 + i * 32 * 8, 2), mag)
        sheet.paste(img.resize((64, 64), Image.NEAREST), (2 + i * 32 * 8, 32 * 8 + 8), img.resize((64, 64), Image.NEAREST))
        # the current shipped 16x16-at-2x emblem for comparison
        cur = os.path.join(old_dir, 'class_%s.png' % name)
        if os.path.exists(cur) and not write:
            cimg = Image.open(cur).convert('RGBA')
            sheet.paste(cimg.resize((64, 64), Image.NEAREST), (2 + i * 32 * 8 + 80, 32 * 8 + 8), cimg.resize((64, 64), Image.NEAREST))
        if write:
            img.save(os.path.join(OUT, 'class_%s.png' % name))
    os.makedirs(os.path.dirname(SHEET), exist_ok=True)
    sheet.save(SHEET)
    print('sheet written', SHEET, '(icons overwritten)' if write else '(shipped icons untouched)')


if __name__ == '__main__':
    main()
