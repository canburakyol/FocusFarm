"""Builds the Play Store screenshots in docs/store/<lang>/ from raw emulator captures.

Usage: python docs/store/compose_store.py <raw_dir>
Optional AI backgrounds: put 1080x1920 images at docs/store/ai/bg_<n>.png (n = 1..7);
when present they replace the drawn background for that slide.
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.abspath(__file__))
FONT = os.path.join(ROOT, "..", "..", "app", "src", "main", "res", "font", "dm_sans.ttf")
W, H = 1080, 1920
CREAM = (251, 246, 236)
FOREST = (47, 93, 70)
INK = (28, 40, 33)

# (raw screen, accent colour, dark slide, {lang: (headline, subline)})
SLIDES = [
    ("home", (217, 98, 59), False, {
        "tr": ("Telefonu bırak.\nBahçeni büyüt.", "Tek dokunuşla odaklanmaya başla."),
        "en": ("Put your phone down.\nGrow your garden.", "Start focusing with one tap."),
    }),
    ("session", (242, 179, 61), False, {
        "tr": ("Odaklandıkça\nbitkin büyür.", "Erken çıkarsan bitkin solar."),
        "en": ("Your plant grows\nwhile you focus.", "Leave early and it withers."),
    }),
    ("garden", (157, 195, 154), False, {
        "tr": ("Her seans\nbahçende kalır.", "Odak geçmişin, gözünün önünde."),
        "en": ("Every session stays\nin your garden.", "Your focus history at a glance."),
    }),
    ("strict", (127, 194, 196), False, {
        "tr": ("İşine yarayan\nuygulamalar serbest.", "Katı modda izinli uygulamaları seç."),
        "en": ("Keep the apps\nthat help you.", "Pick allowed apps in strict mode."),
    }),
    ("shop", (181, 131, 90), False, {
        "tr": ("Seed'lerinle\nyeni bitkiler al.", "Tohum Pazarı'nda 12 farklı bitki."),
        "en": ("Spend seeds on\nnew plants.", "12 plants in the Seed Market."),
    }),
    ("stats", (217, 98, 59), False, {
        "tr": ("Serini koru,\nilerlemeni gör.", "Günlük hedef, seri ve istatistikler."),
        "en": ("Keep your streak,\nsee your progress.", "Daily goal, streaks and stats."),
    }),
    ("dark_session", (127, 207, 160), True, {
        "tr": ("Gece de\ngöz yormaz.", "Koyu tema ile sakin odak."),
        "en": ("Easy on the eyes\nat night.", "Calm focus in dark theme."),
    }),
]


def font(size, weight):
    f = ImageFont.truetype(FONT, size)
    f.set_variation_by_axes([min(max(size / 2, 9), 40), weight])
    return f


def background(index, accent, dark):
    ai = os.path.join(ROOT, "ai", f"bg_{index}.png")
    if os.path.exists(ai):
        return Image.open(ai).convert("RGB").resize((W, H))
    base = (24, 38, 31) if dark else CREAM
    img = Image.new("RGB", (W, H), base)
    d = ImageDraw.Draw(img)
    # accent sits behind the phone so headline text always lands on the plain base colour
    d.ellipse((W // 2 - 560, 640, W // 2 + 560, 1760), fill=(47, 93, 70) if dark else accent)
    hill = (36, 62, 49) if dark else FOREST
    d.ellipse((620, 1480, 1500, 2360), fill=hill)
    d.ellipse((-420, 1700, 420, 2500), fill=tuple(min(255, c + 40) for c in hill))
    return img


def rounded(im, radius):
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, *im.size), radius, fill=255)
    out = Image.new("RGBA", im.size)
    out.paste(im, (0, 0), mask)
    return out


def phone(screen):
    sw, sh = 612, 1360
    bezel = 20
    scr = rounded(screen.convert("RGB").resize((sw, sh), Image.LANCZOS), 44)
    body = Image.new("RGBA", (sw + 2 * bezel, sh + 2 * bezel), (0, 0, 0, 0))
    ImageDraw.Draw(body).rounded_rectangle((0, 0, *body.size), 62, fill=(22, 26, 24))
    body.alpha_composite(scr, (bezel, bezel))
    return body


def compose(index, slide, lang, raw_dir):
    name, accent, dark, texts = slide
    head, sub = texts[lang]
    img = background(index, accent, dark).convert("RGBA")
    d = ImageDraw.Draw(img)
    fg = (240, 246, 242) if dark else INK
    d.multiline_text((72, 96), head, font=font(84, 760), fill=fg, spacing=10)
    head_h = d.multiline_textbbox((72, 96), head, font=font(84, 760), spacing=10)[3]
    d.text((74, head_h + 26), sub, font=font(38, 450), fill=(200, 214, 206) if dark else (78, 92, 84))

    ph = phone(Image.open(os.path.join(raw_dir, f"{lang}_{name}.png")))
    x = (W - ph.width) // 2
    y = H - ph.height - 50
    shadow = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle((x + 10, y + 26, x + ph.width + 10, y + ph.height + 26), 62, fill=(0, 0, 0, 90))
    img.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(28)))
    img.alpha_composite(ph, (x, y))

    out_dir = os.path.join(ROOT, lang)
    os.makedirs(out_dir, exist_ok=True)
    path = os.path.join(out_dir, f"{index}_{name}.png")
    img.convert("RGB").save(path, optimize=True)
    return path


TAGLINES = {"tr": "Telefonu bırak. Bahçeni büyüt.", "en": "Put your phone down. Grow your garden."}


def feature_graphic(lang, raw_dir):
    fw, fh = 1024, 500
    ai = os.path.join(ROOT, "ai", "feature_bg.png")
    if os.path.exists(ai):
        img = Image.open(ai).convert("RGBA").resize((fw, fh))
    else:
        img = Image.new("RGBA", (fw, fh), CREAM + (255,))
        d = ImageDraw.Draw(img)
        d.ellipse((600, -120, 1180, 460), fill=(242, 179, 61))
        d.ellipse((480, 360, 1300, 900), fill=FOREST)
    # the sunflower hero from the real home screen
    hero = Image.open(os.path.join(raw_dir, f"{lang}_home.png")).convert("RGB").crop((238, 455, 842, 1059))
    mask = Image.new("L", hero.size, 0)
    ImageDraw.Draw(mask).ellipse((0, 0, *hero.size), fill=255)
    hero = hero.resize((380, 380), Image.LANCZOS)
    img.paste(hero, (604, 60), mask.resize((380, 380)))
    d = ImageDraw.Draw(img)
    d.text((64, 150), "FocusFarm", font=font(92, 780), fill=FOREST)
    d.text((68, 268), TAGLINES[lang], font=font(36, 500), fill=INK)
    path = os.path.join(ROOT, lang, "feature_graphic.png")
    img.convert("RGB").save(path, optimize=True)
    return path


if __name__ == "__main__":
    raw = sys.argv[1]
    for lang in ("tr", "en"):
        os.makedirs(os.path.join(ROOT, lang), exist_ok=True)
        print(feature_graphic(lang, raw))
        for i, s in enumerate(SLIDES, start=1):
            p = compose(i, s, lang, raw)
            im = Image.open(p)
            assert im.size == (W, H), p
            print(p)
