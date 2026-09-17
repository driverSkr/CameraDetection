"""磁场仪表几何校验预览：把环 + 刻度 + 指针 + 读数合成到深底，用于在落到画布前先确认比例。"""
import math
import os
from PIL import Image, ImageDraw, ImageFont

OUT = r"C:\Android\driverSkr\CameraDetection\docs\ui-redesign\assets"
BG = (0, 0, 0, 255)
SS = 3
S = 300 * SS


def load(name):
    return Image.open(os.path.join(OUT, name)).convert("RGBA").resize((S, S), Image.LANCZOS)


def font(size, bold=True):
    for f in ["msyhbd.ttc" if bold else "msyh.ttc", "arialbd.ttf" if bold else "arial.ttf"]:
        p = os.path.join(r"C:\Windows\Fonts", f)
        if os.path.exists(p):
            try:
                return ImageFont.truetype(p, size * SS)
            except Exception:
                pass
    return ImageFont.load_default()


CX = CY = 150.0
R_TICK_IN, R_TICK_OUT = 78.0, 90.0
R_NEEDLE = 100.0
NEEDLE_W = 13.0
START, SWEEP = 135.0, 270.0


def pt(deg, r):
    a = math.radians(deg)
    return (CX + r * math.cos(a), CY + r * math.sin(a))


def render(name, prog, idle):
    base = Image.new("RGBA", (S, S), BG)
    base.alpha_composite(load("sf_gauge_ring_idle.png" if idle else "sf_gauge_ring.png"))

    d = ImageDraw.Draw(base)
    # 刻度：17 根，首尾对齐 0% / 100%
    for i in range(17):
        a = START + SWEEP * i / 16.0
        p0 = pt(a, R_TICK_IN)
        p1 = pt(a, R_TICK_OUT)
        d.line([(p0[0] * SS, p0[1] * SS), (p1[0] * SS, p1[1] * SS)],
               fill=(143, 182, 224, 90), width=int(4.5 * SS))
        for p in (p0, p1):
            d.ellipse([(p[0] - 2.25) * SS, (p[1] - 2.25) * SS,
                       (p[0] + 2.25) * SS, (p[1] + 2.25) * SS], fill=(143, 182, 224, 90))

    # 指针：圆头胶囊
    ang = START + SWEEP * prog
    tip = pt(ang, R_NEEDLE)
    hw = NEEDLE_W / 2
    d.line([(CX * SS, CY * SS), (tip[0] * SS, tip[1] * SS)],
           fill=(234, 242, 255, 255), width=int(NEEDLE_W * SS))
    d.ellipse([(CX - hw) * SS, (CY - hw) * SS, (CX + hw) * SS, (CY + hw) * SS],
              fill=(234, 242, 255, 255))
    d.ellipse([(tip[0] - hw) * SS, (tip[1] - hw) * SS,
               (tip[0] + hw) * SS, (tip[1] + hw) * SS], fill=(234, 242, 255, 255))
    # 轴心
    d.ellipse([(CX - 7) * SS, (CY - 7) * SS, (CX + 7) * SS, (CY + 7) * SS], fill=(10, 10, 11, 255))
    d.ellipse([(CX - 7) * SS, (CY - 7) * SS, (CX + 7) * SS, (CY + 7) * SS],
              outline=(77, 166, 255, 255), width=int(2.5 * SS))
    d.ellipse([(CX - 2.5) * SS, (CY - 2.5) * SS, (CX + 2.5) * SS, (CY + 2.5) * SS],
              fill=(77, 166, 255, 255))

    # 读数
    num = "0" if idle else "126"
    f_num = font(44)
    f_unit = font(15, bold=False)
    nw = d.textlength(num, font=f_num)
    uw = d.textlength(" μT", font=f_unit)
    total = nw + uw
    x = (CX - total / 2 / SS) * SS
    y = 230 * SS
    d.text((x, y), num, font=f_num, fill=(255, 255, 255, 255))
    d.text((x + nw, y + 22 * SS), " μT", font=f_unit, fill=(179, 179, 179, 255))

    base.resize((300 * 2, 300 * 2), Image.LANCZOS).save(os.path.join(OUT, name))
    print("->", name)


render("sf_gauge_preview_active.png", 0.62, False)
render("sf_gauge_preview_idle.png", 0.0, True)
