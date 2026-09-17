"""生成 SpyFinder 新 UI 的雷达/命中点素材（解析式，天然平滑，无需模糊后处理）。

参考 main 分支现有实现（img_radar_bg / img_radar_detect / img_position）的形态与透明度层次，
但配色改为新 UI 的电光蓝体系，并按深浅重新标定 alpha。
输出到 docs/ui-redesign/assets/ 目录，均为 RGBA PNG。
"""
import os
import numpy as np
from PIL import Image

OUT = r"C:\Android\driverSkr\CameraDetection\docs\ui-redesign\assets"
os.makedirs(OUT, exist_ok=True)

S = 1200                      # 输出画布边长
R = S / 2.0                   # 半径（以半边长归一）
yy, xx = np.mgrid[0:S, 0:S].astype(np.float64)
cx = cy = S / 2.0
r = np.hypot(xx - cx, yy - cy) / R                 # 0~1 归一化半径（角落 >1）
th = np.degrees(np.arctan2(yy - cy, xx - cx))      # -180~180，y 向下


def rgba(color, alpha):
    """color: (r,g,b) 0-255；alpha: 0-1 数组 -> RGBA uint8"""
    a = np.clip(alpha, 0, 1) * 255
    out = np.zeros((S, S, 4), dtype=np.uint8)
    out[..., 0], out[..., 1], out[..., 2] = color
    out[..., 3] = a.astype(np.uint8)
    return out


def smoothstep(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def band(center, width, amp):
    """高斯环带：宽柔光 + 细亮心，做出「涟漪高光」的圆润感"""
    return amp * np.exp(-((r - center) / width) ** 2)


def ribbon(center, width, amp, line=0.85):
    """宽柔带 + 中心细亮线（模拟原素材的环形高光）"""
    return band(center, width, amp) + band(center, width * 0.24, amp * line)


# ───────────────────────── 1. 雷达底纹：柔和同心涟漪 ─────────────────────────
# 标定依据（main/img_radar_bg.webp 径向实测）：环带峰值 ≈0.20，环数 2~3，环与环之间近零，
# 圆心基本无光。此处保留同样的「离散环带」节奏，仅把配色换成电光蓝并略柔化边缘。
ripple = np.zeros_like(r)
ripple += 0.02 * np.exp(-(r / 0.40) ** 2)                  # 极淡中心柔光
ripple += ribbon(0.34, 0.060, 0.115, line=0.55)            # 内环
ripple += ribbon(0.64, 0.072, 0.170, line=0.60)            # 中环（最亮）
ripple += ribbon(0.92, 0.070, 0.085, line=0.55)            # 外环
ripple *= 1.0 - smoothstep(0.98, 1.10, r)                  # 四角收掉
Image.fromarray(rgba((86, 168, 255), ripple)).save(os.path.join(OUT, "sf_radar_ripple.png"))
print("sf_radar_ripple.png  peak alpha = %.3f" % ripple.max())

# ───────────────────────── 2. 扫描扇面：柔和光弧（无硬前沿） ─────────────────────────
# 标定依据（main/img_radar_detect.webp 角度实测）：峰值 ≈0.24，是一个左右对称、约 80~90°
# 的平滑光弧，两端自然衰减到 0，**没有**硬边前沿——这正是「丝滑感」的来源。
# 这里沿用「单瓣平滑光弧」结构，只做轻微前后不对称以保留扫描的方向感。
A_CENTER = -90.0                               # 光弧中心角（配合旋转动画起始相位）
SIG_LEAD, SIG_TAIL = 19.0, 34.0                # 前沿（顺时针方向）更紧，拖尾更长
SIG_PEAK = 0.36
with np.errstate(invalid="ignore"):
    delta = ((th - A_CENTER + 180.0) % 360.0) - 180.0       # -180~180，0=弧心
    sig = np.where(delta > 0, SIG_TAIL, SIG_LEAD)           # 拖尾一侧更宽
    lobe = np.exp(-(delta / sig) ** 2)
sweep = SIG_PEAK * lobe
sweep *= 1.0 - smoothstep(0.80, 0.98, r)                    # 外缘柔化（不切边）
sweep *= 0.55 + 0.45 * smoothstep(0.02, 0.30, r)             # 圆心不塌成硬点，但保留中心亮度
Image.fromarray(rgba((93, 172, 255), sweep)).save(os.path.join(OUT, "sf_radar_sweep.png"))
print("sf_radar_sweep.png   peak alpha = %.3f" % sweep.max())

# ───────────────────────── 3. 命中点：实心红点 + 外光晕 ─────────────────────────
# 标定依据（main/img_position.webp 径向实测）：r<0.62 为**不透明实心圆**，0.62~0.85 软收边。
# 之前做成纯径向渐变，落在画面上就成了「一团红雾」，现改为实心核 + 光晕。
hit_core = 0.92 * (1.0 - smoothstep(0.50, 0.64, r))          # 实心核
hit_halo = 0.34 * np.exp(-(r / 0.62) ** 2)                   # 光晕
hit = np.clip(hit_core + hit_halo, 0, 1) * (1.0 - smoothstep(0.86, 1.0, r))
Image.fromarray(rgba((255, 104, 108), hit)).save(os.path.join(OUT, "sf_hit_glow.png"))
print("sf_hit_glow.png      peak alpha = %.3f" % hit.max())

# ───────────────────────── 4. 预览图（合成到页面深底，便于肉眼校验） ─────────────────────────
def load_premul(name):
    a = np.asarray(Image.open(os.path.join(OUT, name)).convert("RGBA")).astype(np.float64)
    return a


def over(bottom, top):
    a = top[..., 3:4] / 255.0
    return bottom * (1 - a) + top[..., :3] * a


page = np.zeros((S, S, 3), dtype=np.float64)
page[..., 0], page[..., 1], page[..., 2] = 5, 7, 12          # 页面深底 #05070C
base = np.dstack([page, np.full((S, S, 1), 255.0)])
comp = over(base[..., :3], load_premul("sf_radar_ripple.png"))
comp = over(comp, load_premul("sf_radar_sweep.png"))
# 两个命中点：分别放在右上与左下，尺寸按页面上的 34px 等比换算
for (px, py) in [(0.68, 0.26), (0.30, 0.72)]:
    dot = np.asarray(Image.open(os.path.join(OUT, "sf_hit_glow.png")).convert("RGBA")
                     .resize((150, 150), Image.LANCZOS)).astype(np.float64)
    x0, y0 = int(px * S - 75), int(py * S - 75)
    patch = comp[y0:y0 + 150, x0:x0 + 150]
    comp[y0:y0 + 150, x0:x0 + 150] = over(patch, dot)
Image.fromarray(comp.astype(np.uint8)).resize((640, 640), Image.LANCZOS).save(
    os.path.join(OUT, "sf_radar_preview.png"))
print("sf_radar_preview.png 预览已生成")


# ───────────────────────── 5. 磁场仪表：满量程渐变圆环（270°，圆头） ─────────────────────────
# 几何：300×300 画布，圆心 (150,150)，带中心半径 118，带宽 18，135° 起顺时针扫 270°
GS = 300
SS = 3                                  # 超采样倍数，保证边缘平滑
G = GS * SS
yy, xx = np.mgrid[0:G, 0:G].astype(np.float64)
gc = G / 2.0
rr = np.hypot(xx - gc, yy - gc) / SS     # 换算回 300 画布像素
tt = np.degrees(np.arctan2(yy - gc, xx - gc))
tt = np.where(tt < 135.0, tt + 360.0, tt)          # 映射到 135~495
prog = (tt - 135.0) / 270.0                        # 0~1 量程进度

R_BAND, W_BAND = 118.0, 18.0
ARC_LEN = R_BAND * np.radians(270.0)
over = np.where(prog < 0, -prog * ARC_LEN, np.where(prog > 1, (prog - 1) * ARC_LEN, 0.0))
dist = np.sqrt((rr - R_BAND) ** 2 + over ** 2)     # 到带形（含圆头）的距离
core = 1.0 - smoothstep(W_BAND / 2 - 1.1, W_BAND / 2 + 0.7, dist)
glow = (1.0 - smoothstep(W_BAND / 2, W_BAND / 2 * 2.3, dist)) * 0.20


def ramp(p, sat_mul=1.0, val_mul=1.0):
    """量程配色（HSV 插值，避免 RGB 混出灰调）：品牌蓝 → 青 → 安全绿 → 琥珀 → 风险红。
    sat_mul / val_mul 在 HSV 空间直接缩放入口，用于生成「未激活」的低饱和刻度带。"""
    import colorsys
    stops = [(0.00, 212, 0.80, 1.00),
             (0.26, 190, 0.68, 1.00),
             (0.45, 170, 0.58, 0.98),
             (0.58, 52, 0.90, 1.00),
             (0.78, 32, 0.94, 1.00),
             (1.00, 357, 0.80, 0.96)]
    lut = []
    for i in range(513):
        q = i / 512.0
        for k in range(len(stops) - 1):
            q0, h0, s0, v0 = stops[k]
            q1, h1, s1, v1 = stops[k + 1]
            if q0 <= q <= q1:
                f = (q - q0) / (q1 - q0)
                dh = ((h1 - h0 + 180.0) % 360.0) - 180.0   # 走短弧，避免绕一整圈
                h = (h0 + dh * f) % 360.0
                s = (s0 + (s1 - s0) * f) * sat_mul
                v = (v0 + (v1 - v0) * f) * val_mul
                r, g, b = colorsys.hsv_to_rgb(h / 360.0, min(max(s, 0.0), 1.0), min(max(v, 0.0), 1.0))
                lut.append((r * 255, g * 255, b * 255))
                break
    lut = np.array(lut)
    idx = np.clip((np.clip(p, 0, 1) * 512).astype(int), 0, 512)
    return lut[idx]


def gauge_png(name, alpha_scale, with_glow, sat_mul=1.0, val_mul=1.0):
    col = ramp(prog, sat_mul=sat_mul, val_mul=val_mul)
    a = core * alpha_scale
    if with_glow:
        a = np.clip(a + glow, 0, 1)
    img = np.zeros((G, G, 4), dtype=np.uint8)
    img[..., 0] = col[..., 0].astype(np.uint8)
    img[..., 1] = col[..., 1].astype(np.uint8)
    img[..., 2] = col[..., 2].astype(np.uint8)
    img[..., 3] = (np.clip(a, 0, 1) * 255).astype(np.uint8)
    Image.fromarray(img).resize((GS, GS), Image.LANCZOS).save(os.path.join(OUT, name))
    print(f"{name:26s} 尺寸 {GS}×{GS}  R={R_BAND} w={W_BAND} sat×{sat_mul} val×{val_mul} α{alpha_scale}")


gauge_png("sf_gauge_ring.png", 1.0, True)        # 检测中
# 未开始：直接压暗饱和色会让黄/橙段在深底上发脏（偏土色），改为「降饱和 + 微降明度」，
# 让整圈读作一条中性的冷灰刻度带，明确传达「未激活」。
gauge_png("sf_gauge_ring_idle.png", 0.72, False, sat_mul=0.26, val_mul=0.86)

print("\n输出目录:", OUT)
