"""生成磁场仪表的矢量几何（17 根刻度 + 指针胶囊），参数与 sf_gauge_ring.png 完全对齐。
   画布 300×300，圆心 (150,150)，量程 135° → 405°（顺时针 270°）。
"""
import math

CX = CY = 150.0
START, SWEEP = 135.0, 270.0
R_TICK_IN, R_TICK_OUT = 78.0, 90.0
R_NEEDLE = 100.0
TICK_W, NEEDLE_W = 4.5, 13.0
HUB_R, HUB_RING, HUB_DOT = 8.0, 2.5, 2.6


def pt(deg, r):
    a = math.radians(deg)
    return (CX + r * math.cos(a), CY + r * math.sin(a))


ticks = []
for i in range(17):
    a = START + SWEEP * i / 16.0
    x0, y0 = pt(a, R_TICK_IN)
    x1, y1 = pt(a, R_TICK_OUT)
    ticks.append(f"M{x0:.1f} {y0:.1f}L{x1:.1f} {y1:.1f}")

print("=== 刻度（17 根，stroke #8FB6E0 / w4.5 / 圆头 / opacity .35）===")
print(''.join(ticks))
print()


def needle(prog, label):
    a = START + SWEEP * prog
    tx, ty = pt(a, R_NEEDLE)
    print(f"=== 指针 {label}（prog={prog:.2f}, 角度={a:.1f}°）===")
    print(f"M{CX:.1f} {CY:.1f}L{tx:.1f} {ty:.1f}")
    print()


needle(0.62, "06 检测中")
needle(0.0, "06b 未开始")
print("=== 中心轴 ===")
print(f"circle cx={CX} cy={CY} r={HUB_R} / stroke #4DA6FF w{HUB_RING} / dot r={HUB_DOT}")
print()
print("=== 放射角参考 ===")
for p in [0, 0.25, 0.5, 0.62, 0.75, 0.9, 1.0]:
    print(f"  {p*100:5.0f}% -> {START + SWEEP*p:6.1f}°")
