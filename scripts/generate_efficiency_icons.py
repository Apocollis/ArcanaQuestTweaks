"""Builds two perk icons in textures/unlockables from sprites in scripts/icon_sources/:
  melee_efficiency.png  the vanilla iron sword + the Efficiency feather (lower-left, origin 0,7, same as the other
                        Efficiency perk icons)
  weapon_tempo.png      the vanilla iron sword + five gold tick marks in an arc (upper left), no feather
Run from the repo root: python scripts/generate_efficiency_icons.py [--write]
Without --write it only saves a preview to .plans/assets/efficiency_icons_preview.png."""
import sys
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "scripts/icon_sources"
TEX = ROOT / "src/main/resources/assets/aqtweaks/textures/unlockables"
GOLD = (246, 201, 59, 255)
TICKS = ((0, 3), (1, 1), (3, 0), (5, 1), (6, 3))  # each tick is two pixels tall


def melee() -> Image.Image:
    out = Image.open(SRC / "iron_sword.png").convert("RGBA")
    out.alpha_composite(Image.open(SRC / "efficiency_feather.png").convert("RGBA"), (0, 7))
    return out


def tempo() -> Image.Image:
    out = Image.open(SRC / "iron_sword.png").convert("RGBA")
    for x, y in TICKS:
        out.putpixel((x, y), GOLD)
        out.putpixel((x, y + 1), GOLD)
    return out


if __name__ == "__main__":
    icons = {"melee_efficiency": melee(), "weapon_tempo": tempo()}
    prev = ROOT / ".plans/assets/efficiency_icons_preview.png"
    prev.parent.mkdir(parents=True, exist_ok=True)
    s = Image.new("RGBA", (len(icons) * 208, 192), (60, 60, 70, 255))
    for i, im in enumerate(icons.values()):
        big = im.resize((192, 192), Image.NEAREST)
        s.paste(big, (i * 208, 0), big)
    s.save(prev)
    print("preview:", prev)
    if "--write" in sys.argv:
        for name, im in icons.items():
            im.save(TEX / f"{name}.png")
            print("wrote", name)
