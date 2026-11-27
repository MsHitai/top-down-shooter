#!/usr/bin/env python3
"""Generate the Fluffy King window and desktop icons with Pillow."""

import argparse
from pathlib import Path

from PIL import Image, ImageDraw

from gen_sprites import king_frame


def portrait():
    icon = Image.new("RGBA", (32, 32))
    draw = ImageDraw.Draw(icon)
    draw.rounded_rectangle((0, 0, 31, 31), radius=7, fill="#22142f")
    draw.rounded_rectangle((1, 1, 30, 30), radius=6, fill="#9860a7")
    draw.rounded_rectangle((2, 2, 29, 29), radius=5, fill="#4c295d")
    draw.rounded_rectangle((3, 3, 28, 27), radius=4, fill="#694075")
    king = king_frame()
    king = king.crop(king.getbbox())
    icon.alpha_composite(king, ((32 - king.width) // 2, (32 - king.height) // 2))
    return icon


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    resources = Path(__file__).resolve().parents[1] / "src/main/resources"
    parser.add_argument("--window-dir", type=Path, default=resources)
    parser.add_argument("--desktop-dir", type=Path, help="also write ICO and ICNS files here")
    args = parser.parse_args()
    icon = portrait()
    args.window_dir.mkdir(parents=True, exist_ok=True)
    for size in (16, 32, 64, 128):
        icon.resize((size, size), Image.Resampling.NEAREST).save(args.window_dir / f"fluffy-icon-{size}.png")
    if args.desktop_dir:
        args.desktop_dir.mkdir(parents=True, exist_ok=True)
        icon.resize((256, 256), Image.Resampling.NEAREST).save(args.desktop_dir / "EnterTheFluffy.ico")
        icon.resize((1024, 1024), Image.Resampling.NEAREST).save(args.desktop_dir / "EnterTheFluffy.icns")


if __name__ == "__main__":
    main()
