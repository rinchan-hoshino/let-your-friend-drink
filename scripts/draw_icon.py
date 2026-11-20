#!/usr/bin/env python3
"""Generate the 64x64 pixel-art Let Your Friend Drink 2.0 badge icon (8x nearest-neighbor -> 512x512)."""
from pathlib import Path
import math
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1]
SIZE = 64
SCALE = 8


def hex_rgba(h: str, a: int = 255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def blend(dst, src):
    sr, sg, sb, sa = src
    if sa == 0:
        return dst
    if sa == 255:
        return src
    dr, dg, db, da = dst
    inv = 255 - sa
    out_a = sa + (da * inv) // 255
    if out_a == 0:
        return (0, 0, 0, 0)
    r = (sr * sa + dr * da * inv // 255) // out_a
    g = (sg * sa + dg * da * inv // 255) // out_a
    b = (sb * sa + db * da * inv // 255) // out_a
    return (r, g, b, out_a)


def write_png(path: Path, width: int, height: int, rgba_bytes: bytes):
    def chunk(tag: bytes, data: bytes) -> bytes:
        return (
            struct.pack("!I", len(data))
            + tag
            + data
            + struct.pack("!I", zlib.crc32(tag + data) & 0xFFFFFFFF)
        )

    raw = bytearray()
    stride = width * 4
    for y in range(height):
        raw.append(0)
        raw.extend(rgba_bytes[y * stride : (y + 1) * stride])

    png = bytearray(b"\x89PNG\r\n\x1a\n")
    png.extend(chunk(b"IHDR", struct.pack("!IIBBBBB", width, height, 8, 6, 0, 0, 0)))
    png.extend(chunk(b"IDAT", zlib.compress(bytes(raw), 9)))
    png.extend(chunk(b"IEND", b""))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(bytes(png))


def inside_rounded_rect(x: int, y: int, x0: int, y0: int, x1: int, y1: int, r: int) -> bool:
    if x < x0 or x > x1 or y < y0 or y > y1:
        return False
    cx = x0 + r if x < x0 + r else (x1 - r if x > x1 - r else x)
    cy = y0 + r if y < y0 + r else (y1 - r if y > y1 - r else y)
    dx = x - cx
    dy = y - cy
    return dx * dx + dy * dy <= r * r


def draw_tilted_bottle(
    canvas,
    cx: float,
    cy: float,
    angle_deg: float,
    liq_main,
    liq_hi,
    liq_dark,
    liq_top,
    bubble_col,
):
    """Rasterize a crisp pixel-art vanilla-style potion/honey bottle tilted around (cx, cy)."""
    # Bottle local coordinates (u across width, v from top of cork to bottom of flask)
    # Local grid: u in [-9, 9], v in [-14, 13]
    # Regions in local space:
    # - Cork: |u| <= 2.3, v in [-13.5, -10.2]
    # - Rim:  |u| <= 3.6, v in [-10.2, -8.3]
    # - Neck: |u| <= 2.4, v in [-8.3, -4.6]
    # - Body (rounded flask): ellipse/rounded box |u| <= 7.4, v in [-4.6, 11.4]
    outline = hex_rgba("#1A120E")
    cork_hi = hex_rgba("#C89B63")
    cork_mid = hex_rgba("#9E6F3F")
    cork_dark = hex_rgba("#6E4925")
    glass_hi = hex_rgba("#F2FAFF")
    glass_edge = hex_rgba("#BBD9EE")
    glass_mid = hex_rgba("#82AAC7")
    glass_dark = hex_rgba("#527490")

    rad = math.radians(-angle_deg)
    ca = math.cos(rad)
    sa = math.sin(rad)

    def classify(u: float, v: float):
        # Cork
        if abs(u) <= 2.2 and -13.2 <= v < -9.8:
            return "cork"
        # Lip / Rim
        if abs(u) <= 3.5 and -9.8 <= v < -7.8:
            return "rim"
        # Neck
        if abs(u) <= 2.3 and -7.8 <= v < -4.2:
            return "neck"
        # Flask shoulder + round body
        if -4.2 <= v <= 11.2:
            # smooth shoulder from v=-4.2 (half-width 2.4) to v=0.0 (half-width 7.2)
            if v < 0.0:
                t = (v + 4.2) / 4.2
                hw = 2.4 + 4.8 * (t ** 0.75)
            elif v <= 7.0:
                hw = 7.2
            else:
                t = (v - 7.0) / 4.2
                hw = 7.2 * math.sqrt(max(0.0, 1.0 - t * t * 0.85))
            if abs(u) <= hw:
                # Inner glass wall thickness ~1.35px
                inner_hw = max(0.0, hw - 1.45)
                if abs(u) <= inner_hw and v >= -2.5 and v <= 9.6:
                    if v >= 0.4:
                        return "liquid"
                    return "air"
                return "glass"
        return None

    # Build local mask on screen pixels first so we can draw a razor-sharp 1px dark outline
    shape = {}
    for py in range(4, SIZE - 4):
        for px in range(4, SIZE - 4):
            dx = px - cx
            dy = py - cy
            u = dx * ca - dy * sa
            v = dx * sa + dy * ca
            kind = classify(u, v)
            if kind is not None:
                shape[(px, py)] = (kind, u, v)

    # 1px crisp dark outline around silhouette
    for (px, py) in list(shape.keys()):
        for ox, oy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            nx, ny = px + ox, py + oy
            if (nx, ny) not in shape and 0 <= nx < SIZE and 0 <= ny < SIZE:
                canvas[ny][nx] = blend(canvas[ny][nx], outline)

    # Fill pixels
    for (px, py), (kind, u, v) in shape.items():
        if kind == "cork":
            if u < -0.5 or v < -12.0:
                col = cork_hi
            elif u > 1.0:
                col = cork_dark
            else:
                col = cork_mid
        elif kind == "rim":
            col = glass_hi if (u < -0.8 or v < -8.9) else (glass_dark if u > 1.8 else glass_edge)
        elif kind == "neck":
            col = glass_hi if u < -0.7 else (glass_dark if u > 1.1 else glass_mid)
        elif kind == "air":
            # Subtle translucent glass interior
            col = hex_rgba("#D8EEFA", 125) if u < -1.8 else hex_rgba("#6E90AC", 95)
        elif kind == "glass":
            if u < -4.2 and v < 5.0:
                col = glass_hi
            elif u > 4.2 or v > 9.0:
                col = glass_dark
            elif u < 0.0:
                col = glass_edge
            else:
                col = glass_mid
        else:  # liquid
            if v < 1.65:
                col = liq_top
            elif u < -2.4 and v < 5.5:
                col = liq_hi
            elif u > 2.6 or v > 7.4:
                col = liq_dark
            else:
                col = liq_main
            # Specular glass reflection streak on left side of liquid
            if -4.6 <= u <= -3.3 and 1.8 <= v <= 6.2:
                col = blend(col, hex_rgba("#FFFFFF", 165))
            # Tiny effervescent pixel bubbles
            if (abs(u - 1.2) < 0.8 and abs(v - 4.2) < 0.8) or (abs(u + 0.6) < 0.7 and abs(v - 6.6) < 0.7):
                col = bubble_col
        canvas[py][px] = blend(canvas[py][px], col)


def draw_sparkle(canvas, cx: int, cy: int, radius: int, core, edge, dark):
    """Draw a crisp emerald happy-villager star sparkle."""
    pts = [(cx, cy)]
    for d in range(1, radius + 1):
        pts.extend([(cx + d, cy), (cx - d, cy), (cx, cy + d), (cx, cy - d)])
    # Outline
    pt_set = set(pts)
    for x, y in pts:
        for ox, oy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            nx, ny = x + ox, y + oy
            if (nx, ny) not in pt_set and 0 <= nx < SIZE and 0 <= ny < SIZE:
                canvas[ny][nx] = blend(canvas[ny][nx], dark)
    for x, y in pts:
        dist = abs(x - cx) + abs(y - cy)
        col = core if dist <= 1 else edge
        if 0 <= x < SIZE and 0 <= y < SIZE:
            canvas[y][x] = blend(canvas[y][x], col)


def draw_heart(canvas, ox: int, oy: int):
    """Draw a crisp 9x8 pixel-art heart."""
    pattern = [
        "..XX.XX..",
        ".XHHXRRX.",
        "XHHHRRRRX",
        "XHRRRRRDX",
        ".XRRRRDX.",
        "..XRRDX..",
        "...XDX...",
        "....X....",
    ]
    palette = {
        "X": hex_rgba("#240C14"),
        "H": hex_rgba("#FFB8D0"),
        "R": hex_rgba("#FF4D79"),
        "D": hex_rgba("#C9244E"),
    }
    for row_idx, row in enumerate(pattern):
        for col_idx, ch in enumerate(row):
            if ch != ".":
                x, y = ox + col_idx, oy + row_idx
                if 0 <= x < SIZE and 0 <= y < SIZE:
                    canvas[y][x] = blend(canvas[y][x], palette[ch])


def draw_icon():
    canvas = [[(0, 0, 0, 0) for _ in range(SIZE)] for _ in range(SIZE)]

    def put(x: int, y: int, color):
        if 0 <= x < SIZE and 0 <= y < SIZE:
            canvas[y][x] = blend(canvas[y][x], color)

    # 1. Warm deep-oak rounded riveted badge
    outer_rim = hex_rgba("#1A110B")
    trim_hi = hex_rgba("#B6854C")
    trim_mid = hex_rgba("#8A5D30")
    trim_bot = hex_rgba("#5A3A1C")
    inner_groove = hex_rgba("#24170E")
    bg_top = hex_rgba("#4A2F1D")
    bg_mid = hex_rgba("#3B2416")
    bg_bot = hex_rgba("#2B190F")

    for y in range(SIZE):
        for x in range(SIZE):
            if not inside_rounded_rect(x, y, 2, 2, 61, 61, 11):
                continue
            if not inside_rounded_rect(x, y, 3, 3, 60, 60, 10):
                put(x, y, outer_rim)
            elif not inside_rounded_rect(x, y, 6, 6, 57, 57, 8):
                # Warm oak/bronze beveled frame
                if y < 18 or x < 14:
                    put(x, y, trim_hi if (x + y) % 5 != 0 else trim_mid)
                elif y > 46 or x > 49:
                    put(x, y, trim_bot)
                else:
                    put(x, y, trim_mid)
            elif not inside_rounded_rect(x, y, 7, 7, 56, 56, 7):
                put(x, y, inner_groove)
            else:
                # Warm oak interior with subtle horizontal wood planks + radial warmth
                dx = (x - 32) / 26.0
                dy = (y - 30) / 26.0
                dist = math.sqrt(dx * dx + dy * dy)
                if dist < 0.62:
                    col = bg_top
                elif dist < 0.95:
                    col = bg_mid
                else:
                    col = bg_bot
                # Subtle oak plank seam lines
                if y in (19, 32, 45) and (x % 3 != 0):
                    col = blend(col, hex_rgba("#1C1009", 85))
                elif (x * 7 + y * 13) % 19 == 0:
                    col = blend(col, hex_rgba("#6E472A", 35))
                put(x, y, col)

    # 4 corner brass rivets
    rivet_dark = hex_rgba("#2B1A0D")
    rivet_mid = hex_rgba("#C89852")
    rivet_hi = hex_rgba("#F5D896")
    for rx, ry in ((9, 9), (53, 9), (9, 53), (53, 53)):
        for dy in range(3):
            for dx in range(3):
                put(rx + dx - 1, ry + dy - 1, rivet_dark)
        put(rx, ry, rivet_mid)
        put(rx - 1, ry - 1, rivet_hi)

    # Subtle warm glow behind the clinking bottles
    for y in range(14, 52):
        for x in range(12, 52):
            dist = math.hypot(x - 32.0, y - 34.0)
            if dist < 18.0:
                alpha = int((1.0 - dist / 18.0) * 52)
                put(x, y, hex_rgba("#FFD58A", alpha))

    # 2. Left tilted Amber Honey Bottle (clinking inward to the right)
    draw_tilted_bottle(
        canvas,
        cx=22.5,
        cy=39.0,
        angle_deg=24.0,
        liq_main=hex_rgba("#F2A01D"),
        liq_hi=hex_rgba("#FFD95E"),
        liq_dark=hex_rgba("#BA640C"),
        liq_top=hex_rgba("#FFEA8A"),
        bubble_col=hex_rgba("#FFF5C2"),
    )

    # 3. Right tilted Crystal Pink Healing Potion Bottle (clinking inward to the left)
    draw_tilted_bottle(
        canvas,
        cx=41.5,
        cy=39.0,
        angle_deg=-24.0,
        liq_main=hex_rgba("#F54B8E"),
        liq_hi=hex_rgba("#FF99C4"),
        liq_dark=hex_rgba("#B81D5E"),
        liq_top=hex_rgba("#FFC2DE"),
        bubble_col=hex_rgba("#FFE5F1"),
    )

    # 4. Clink impact rays right between the bottle necks
    clink_gold = hex_rgba("#FFF4B8")
    for px, py in ((31, 25), (32, 24), (33, 25), (32, 27)):
        put(px, py, clink_gold)

    # 5. Emerald Happy-Villager sparkles + central pixel heart above the bottles
    draw_heart(canvas, 28, 12)
    draw_sparkle(
        canvas, 17, 17, 3,
        hex_rgba("#D6FFE0"), hex_rgba("#3EE86C"), hex_rgba("#12381C")
    )
    draw_sparkle(
        canvas, 46, 16, 3,
        hex_rgba("#D6FFE0"), hex_rgba("#3EE86C"), hex_rgba("#12381C")
    )
    draw_sparkle(
        canvas, 32, 22, 2,
        hex_rgba("#E4FFEA"), hex_rgba("#4AF27A"), hex_rgba("#12381C")
    )
    # Extra tiny emerald star dots
    for sx, sy in ((22, 13), (41, 13), (14, 24), (49, 23)):
        put(sx, sy, hex_rgba("#4AF27A"))

    # Upscale 8x nearest-neighbor -> 512x512
    out_w = SIZE * SCALE
    out_h = SIZE * SCALE
    rgba = bytearray(out_w * out_h * 4)
    for y in range(SIZE):
        for x in range(SIZE):
            r, g, b, a = canvas[y][x]
            for sy in range(SCALE):
                row_off = (y * SCALE + sy) * out_w * 4
                for sx in range(SCALE):
                    idx = row_off + (x * SCALE + sx) * 4
                    rgba[idx] = r
                    rgba[idx + 1] = g
                    rgba[idx + 2] = b
                    rgba[idx + 3] = a

    targets = [
        ROOT / "common/src/main/resources/assets/let_your_friend_drink/icon.png",
        ROOT / "docs/icon.png",
    ]
    for target in targets:
        write_png(target, out_w, out_h, bytes(rgba))
        print(f"Wrote {target} ({out_w}x{out_h})")


if __name__ == "__main__":
    draw_icon()
