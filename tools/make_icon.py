"""Extract the gold lock logo from the source image and render Android adaptive icon layers.

usage: python make_icon.py <source image> <res dir> <preview png>
"""
import sys
from pathlib import Path
from PIL import Image

src, res_dir, preview = sys.argv[1], Path(sys.argv[2]), sys.argv[3]

ACCENT = (0xFF, 0xB9, 0x4C)       # LockColors.accent
BACKGROUND = (0x15, 0x1A, 0x33)   # LockColors.background
LOGO_FRACTION = 0.48              # share of the 108dp canvas; leaves margin inside circular masks

im = Image.open(src).convert("RGB")
w, h = im.size
px = im.load()

# Gold pixels have red well above blue; white text and the dark background do not.
mask = Image.new("L", (w, h), 0)
mp = mask.load()
for y in range(h):
    for x in range(w):
        r, g, b = px[x, y]
        a = (r - b - 20) / 90
        mp[x, y] = int(max(0.0, min(1.0, a)) * 255)

bbox = mask.point(lambda v: 255 if v > 60 else 0).getbbox()
print("logo bbox", bbox)
logo = mask.crop(bbox)

def render(size: int) -> Image.Image:
    lw, lh = logo.size
    scale = size * LOGO_FRACTION / max(lw, lh)
    resized = logo.resize((round(lw * scale), round(lh * scale)), Image.LANCZOS)
    alpha = Image.new("L", (size, size), 0)
    alpha.paste(resized, ((size - resized.width) // 2, (size - resized.height) // 2))
    layer = Image.new("RGBA", (size, size), ACCENT + (0,))
    layer.putalpha(alpha)
    return layer

densities = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}
for name, size in densities.items():
    out = res_dir / f"mipmap-{name}"
    out.mkdir(parents=True, exist_ok=True)
    render(size).save(out / "ic_launcher_foreground.png")

# Preview: foreground on background, clipped to a rounded square like a launcher would.
big = render(432)
bg = Image.new("RGBA", (432, 432), BACKGROUND + (255,))
bg.alpha_composite(big)
round_mask = Image.new("L", (432, 432), 0)
from PIL import ImageDraw
ImageDraw.Draw(round_mask).rounded_rectangle((36, 36, 396, 396), radius=90, fill=255)
final = Image.new("RGBA", (432, 432), (255, 255, 255, 255))
final.paste(bg, (0, 0), round_mask)
final.save(preview)
print("done")
