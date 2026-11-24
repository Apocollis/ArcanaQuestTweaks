"""Build Smart HUD's slot-frame sheet in the Stoneborn hotbar style.

Smart HUD (smarthud:textures/hud/elements.png) draws a frame from 22 px tall pieces at v = 0 or 22:
start cap (u 0..11), end cap (u 11..22), middle pieces (u 22..32 and u 32..42, alternating). A hotbar is the same
construction (182 px = caps of 11 + slots of 20), so the pieces are cut straight from Stoneborn's widgets.png.

Usage:
  python scripts/generate_smarthud_stoneborn.py <STONEBORN-1.12.2-V3.1.zip> <out.png> [<additions-pack.zip>]
With a third argument the sheet is also written to assets/smarthud/textures/hud/elements.png in that pack
(a copy of the pack is kept next to it as <name>.pre-smarthud.bak first).
"""
import io
import shutil
import sys
import zipfile

from PIL import Image

ENTRY = "assets/smarthud/textures/hud/elements.png"


def build(widgets: Image.Image) -> Image.Image:
    bar = widgets.crop((0, 0, 182, 22)).convert("RGBA")
    pieces = [
        (0, bar.crop((0, 0, 11, 22))),       # start cap
        (11, bar.crop((171, 0, 182, 22))),   # end cap
        (22, bar.crop((11, 0, 21, 22))),     # middle piece A
        (32, bar.crop((21, 0, 31, 22))),     # middle piece B
    ]
    sheet = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    for v in (0, 22):  # both Smart HUD styles get the same Stoneborn look
        for u, img in pieces:
            sheet.paste(img, (u, v))
    return sheet


def main() -> None:
    if len(sys.argv) < 3:
        sys.exit(__doc__)
    with zipfile.ZipFile(sys.argv[1]) as z:
        widgets = Image.open(io.BytesIO(z.read("assets/minecraft/textures/gui/widgets.png")))
    sheet = build(widgets)
    sheet.save(sys.argv[2])
    print("wrote", sys.argv[2])
    if len(sys.argv) > 3:
        pack = sys.argv[3]
        shutil.copyfile(pack, pack + ".pre-smarthud.bak")
        buf = io.BytesIO()
        sheet.save(buf, "PNG")
        tmp = pack + ".tmp"
        with zipfile.ZipFile(pack) as src, zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED) as dst:
            for item in src.infolist():
                if item.filename != ENTRY:
                    dst.writestr(item, src.read(item.filename))
            dst.writestr(ENTRY, buf.getvalue())
        shutil.move(tmp, pack)
        print("updated", pack)


if __name__ == "__main__":
    main()
