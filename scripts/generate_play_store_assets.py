from __future__ import annotations

from pathlib import Path
from typing import Iterable

from PIL import Image, ImageDraw, ImageFont, ImageFilter


ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "play-store-assets"
SCREENSHOT_DIR = OUT_DIR / "phone-screenshots"


def _font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    candidates: list[str] = []
    if bold:
        candidates.extend(
            [
                "C:/Windows/Fonts/segoeuib.ttf",
                "C:/Windows/Fonts/arialbd.ttf",
            ]
        )
    else:
        candidates.extend(
            [
                "C:/Windows/Fonts/segoeui.ttf",
                "C:/Windows/Fonts/arial.ttf",
            ]
        )

    # Pillow bundled font fallback.
    try:
        pil_fonts = Path(ImageFont.__file__).resolve().parent / "fonts"
        candidates.extend(
            [
                str(pil_fonts / "DejaVuSans-Bold.ttf") if bold else str(pil_fonts / "DejaVuSans.ttf"),
                str(pil_fonts / "DejaVuSans.ttf"),
            ]
        )
    except Exception:
        pass

    for candidate in candidates:
        try:
            return ImageFont.truetype(candidate, size=size)
        except OSError:
            continue
    return ImageFont.load_default()


def _vertical_gradient(size: tuple[int, int], top: tuple[int, int, int], bottom: tuple[int, int, int]) -> Image.Image:
    w, h = size
    img = Image.new("RGB", size)
    px = img.load()
    for y in range(h):
        t = y / max(h - 1, 1)
        color = (
            int(top[0] * (1 - t) + bottom[0] * t),
            int(top[1] * (1 - t) + bottom[1] * t),
            int(top[2] * (1 - t) + bottom[2] * t),
        )
        for x in range(w):
            px[x, y] = color
    return img


def _draw_centered_text(draw: ImageDraw.ImageDraw, xy: tuple[float, float], text: str, font: ImageFont.ImageFont, fill):
    bbox = draw.textbbox((0, 0), text, font=font)
    w = bbox[2] - bbox[0]
    h = bbox[3] - bbox[1]
    draw.text((xy[0] - w / 2, xy[1] - h / 2), text, font=font, fill=fill)


def generate_icon() -> Path:
    size = 512
    img = _vertical_gradient((size, size), (15, 39, 30), (43, 88, 64)).convert("RGBA")
    draw = ImageDraw.Draw(img, "RGBA")

    # Ambient glows.
    draw.ellipse((65, 35, 285, 255), fill=(255, 226, 148, 42))
    draw.ellipse((185, 210, 460, 485), fill=(55, 122, 82, 56))

    # Plant body.
    draw.ellipse((130, 338, 382, 452), fill=(86, 52, 30, 255))
    draw.rounded_rectangle((244, 174, 269, 374), radius=11, fill=(106, 178, 86, 255))
    draw.polygon([(252, 256), (188, 212), (144, 273), (219, 307)], fill=(88, 186, 96, 255))
    draw.polygon([(259, 239), (330, 187), (374, 250), (292, 301)], fill=(129, 206, 95, 255))
    draw.ellipse((214, 132, 300, 218), fill=(245, 182, 74, 255))

    # Soft vignette.
    vignette = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    vdraw = ImageDraw.Draw(vignette, "RGBA")
    vdraw.rounded_rectangle((0, 0, size - 1, size - 1), radius=115, outline=(0, 0, 0, 72), width=10)
    img = Image.alpha_composite(img, vignette)

    out = OUT_DIR / "app-icon-512.png"
    img.convert("RGB").save(out, format="PNG", optimize=True)
    return out


def _draw_phone_mock(draw: ImageDraw.ImageDraw, rect: tuple[int, int, int, int]):
    x1, y1, x2, y2 = rect
    draw.rounded_rectangle(rect, radius=42, fill=(22, 34, 28, 220), outline=(240, 246, 240, 110), width=4)
    in_rect = (x1 + 24, y1 + 24, x2 - 24, y2 - 24)
    draw.rounded_rectangle(in_rect, radius=28, fill=(246, 249, 245, 255))

    ix1, iy1, ix2, iy2 = in_rect
    draw.rounded_rectangle((ix1 + 30, iy1 + 30, ix2 - 30, iy1 + 220), radius=24, fill=(234, 242, 235, 255))
    draw.text((ix1 + 52, iy1 + 72), "Bugun neye odaklanacaksin?", font=_font(26, bold=True), fill=(31, 56, 42))
    draw.text((ix1 + 52, iy1 + 124), "Plani Baslat (25 dk)", font=_font(24), fill=(64, 87, 73))

    cy = iy1 + 270
    for i in range(3):
        top = cy + i * 120
        draw.rounded_rectangle((ix1 + 30, top, ix2 - 30, top + 92), radius=20, fill=(255, 255, 255, 255))
        draw.text((ix1 + 54, top + 30), f"Gorev {i + 1}  -  +{(i + 1) * 15} seed", font=_font(22), fill=(42, 68, 52))


def generate_feature_graphic() -> Path:
    img = _vertical_gradient((1024, 500), (16, 40, 31), (46, 90, 66)).convert("RGBA")
    draw = ImageDraw.Draw(img, "RGBA")

    draw.ellipse((-80, -120, 360, 320), fill=(255, 224, 140, 38))
    draw.ellipse((710, 260, 1140, 660), fill=(80, 162, 112, 45))

    draw.text((68, 86), "FocusFarm", font=_font(74, bold=True), fill=(248, 251, 246, 255))
    draw.text((72, 178), "Pomodoro ile odaklan,", font=_font(40, bold=False), fill=(228, 240, 231, 255))
    draw.text((72, 228), "bahceni buyut.", font=_font(40, bold=False), fill=(228, 240, 231, 255))

    badges: Iterable[str] = ("Gorevler", "Seri Takibi", "Haftalik Rapor")
    bx = 72
    by = 312
    for badge in badges:
        tw, th = draw.textbbox((0, 0), badge, font=_font(24))[2:]
        draw.rounded_rectangle((bx, by, bx + tw + 36, by + 46), radius=23, fill=(255, 255, 255, 32), outline=(255, 255, 255, 62), width=2)
        draw.text((bx + 18, by + 10), badge, font=_font(24), fill=(241, 248, 242, 255))
        bx += tw + 56

    _draw_phone_mock(draw, (650, 42, 952, 470))

    out = OUT_DIR / "feature-graphic-1024x500.png"
    img.convert("RGB").save(out, format="PNG", optimize=True)
    return out


def _base_screen(title: str, subtitle: str) -> tuple[Image.Image, ImageDraw.ImageDraw]:
    img = _vertical_gradient((1080, 1920), (18, 42, 32), (49, 95, 70)).convert("RGBA")
    draw = ImageDraw.Draw(img, "RGBA")

    draw.ellipse((-120, -160, 500, 460), fill=(255, 219, 128, 36))
    draw.ellipse((720, 1280, 1320, 1970), fill=(84, 160, 112, 58))

    draw.rounded_rectangle((58, 88, 1022, 1848), radius=56, fill=(245, 249, 244, 248))
    draw.rounded_rectangle((420, 108, 660, 128), radius=10, fill=(198, 207, 198, 240))

    draw.text((112, 190), title, font=_font(56, bold=True), fill=(27, 52, 38, 255))
    draw.text((112, 272), subtitle, font=_font(34), fill=(71, 92, 79, 255))
    return img, draw


def generate_screenshot_home() -> Path:
    img, draw = _base_screen("Gunluk odak ritmi", "Planli seanslarla hedefini koru")

    draw.rounded_rectangle((108, 370, 972, 650), radius=34, fill=(229, 241, 231, 255))
    draw.text((148, 428), "Bugun neye odaklanacaksin?", font=_font(40, bold=True), fill=(30, 58, 43))
    draw.rounded_rectangle((148, 508, 522, 590), radius=22, fill=(52, 125, 83, 255))
    draw.text((187, 533), "Plani Baslat (25 dk)", font=_font(28, bold=True), fill=(244, 250, 246))

    cards = [("Gorev", "+30 seed"), ("Haftalik Hedef", "%68"), ("Streak", "7 gun")]
    y = 710
    for label, value in cards:
        draw.rounded_rectangle((108, y, 972, y + 200), radius=30, fill=(255, 255, 255, 255))
        draw.text((156, y + 46), label, font=_font(34), fill=(66, 89, 75))
        draw.text((156, y + 98), value, font=_font(48, bold=True), fill=(34, 64, 47))
        y += 230

    out = SCREENSHOT_DIR / "screenshot-01-home-1080x1920.png"
    img.convert("RGB").save(out, format="PNG", optimize=True)
    return out


def generate_screenshot_session() -> Path:
    img, draw = _base_screen("Odak seansi", "Dikkatini koru, bitkini buyut")

    draw.ellipse((248, 430, 832, 1014), outline=(74, 143, 98, 255), width=18)
    draw.ellipse((302, 484, 778, 960), fill=(236, 245, 237, 255))
    _draw_centered_text(draw, (540, 630), "18:42", _font(94, bold=True), (34, 68, 48, 255))
    _draw_centered_text(draw, (540, 730), "kalan sure", _font(34), (77, 98, 85, 255))

    draw.rounded_rectangle((200, 1090, 880, 1190), radius=28, fill=(50, 120, 81, 255))
    _draw_centered_text(draw, (540, 1140), "Odaga Devam Et", _font(36, bold=True), (245, 250, 246, 255))

    draw.rounded_rectangle((200, 1220, 880, 1320), radius=28, fill=(232, 238, 233, 255))
    _draw_centered_text(draw, (540, 1268), "Mola Ver", _font(34), (58, 84, 67, 255))

    out = SCREENSHOT_DIR / "screenshot-02-session-1080x1920.png"
    img.convert("RGB").save(out, format="PNG", optimize=True)
    return out


def generate_screenshot_garden() -> Path:
    img, draw = _base_screen("Bahcem", "Tamamlanan her seans yeni bir bitki")

    x = 110
    y = 386
    for i in range(6):
        col = i % 2
        row = i // 2
        left = x + col * 444
        top = y + row * 360
        draw.rounded_rectangle((left, top, left + 410, top + 320), radius=28, fill=(255, 255, 255, 255))
        draw.ellipse((left + 138, top + 52, left + 272, top + 186), fill=(248, 189, 77, 255))
        draw.rounded_rectangle((left + 194, top + 150, left + 216, top + 240), radius=10, fill=(100, 170, 88, 255))
        draw.ellipse((left + 116, top + 212, left + 294, top + 272), fill=(99, 65, 39, 255))
        draw.text((left + 42, top + 270), f"Bitki #{i + 1}", font=_font(28, bold=True), fill=(40, 67, 50))

    out = SCREENSHOT_DIR / "screenshot-03-garden-1080x1920.png"
    img.convert("RGB").save(out, format="PNG", optimize=True)
    return out


def generate_screenshot_shop() -> Path:
    img, draw = _base_screen("Premium", "Reklamsiz deneyim ve ozel bitkiler")

    plans = [
        ("Aylik", "79,99 TL"),
        ("Yillik", "499,99 TL"),
        ("Omur Boyu", "1499,99 TL"),
    ]
    y = 388
    for idx, (name, price) in enumerate(plans):
        fill = (231, 242, 233, 255) if idx == 1 else (255, 255, 255, 255)
        draw.rounded_rectangle((108, y, 972, y + 250), radius=30, fill=fill)
        draw.text((156, y + 56), name, font=_font(42, bold=True), fill=(35, 63, 47))
        draw.text((156, y + 128), price, font=_font(36), fill=(66, 89, 75))
        if idx == 1:
            draw.rounded_rectangle((738, y + 48, 930, y + 112), radius=20, fill=(52, 125, 83, 255))
            draw.text((772, y + 66), "Populer", font=_font(24, bold=True), fill=(244, 250, 246))
        y += 286

    out = SCREENSHOT_DIR / "screenshot-04-premium-1080x1920.png"
    img.convert("RGB").save(out, format="PNG", optimize=True)
    return out


def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    SCREENSHOT_DIR.mkdir(parents=True, exist_ok=True)

    files = [
        generate_icon(),
        generate_feature_graphic(),
        generate_screenshot_home(),
        generate_screenshot_session(),
        generate_screenshot_garden(),
        generate_screenshot_shop(),
    ]

    print("Generated:")
    for path in files:
        print(path)


if __name__ == "__main__":
    main()
