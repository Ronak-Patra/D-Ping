import os
from PIL import Image, ImageDraw, ImageFilter, ImageFont

def draw_rebuilt_vector_logo():
    # Render at 2048x2048 for pristine supersampling (100% crisp)
    S = 2048
    img = Image.new("RGBA", (S, S), (5, 8, 14, 255)) # Deep Obsidian Charcoal
    
    # Glow layer & Core layer
    glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    core = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    
    draw_g = ImageDraw.Draw(glow)
    draw_c = ImageDraw.Draw(core)

    cyan_core = (0, 240, 255, 255)
    cyan_glow = (0, 220, 255, 220)

    # 1. Geometry of the Capital "D"
    # Spine: x from 480 to 580 (width ~100px), y from 300 to 1250
    # Outer arc: radius extending to x ~1400, y centered around 775
    # Let's draw the "D" using thick stroke path
    d_x = 520
    d_top = 280
    d_bottom = 1260
    d_width = 760
    stroke_w = 84
    
    # Outer D shape
    # Rounded rectangle for the D's right curve
    # Left spine:
    spine_x = d_x
    # Draw D outline
    # Top horizontal bar, curved right side, bottom horizontal bar, left vertical spine
    d_points = [
        (spine_x, d_top),
        (spine_x + 360, d_top),
    ]
    # Arc points for D curve
    import math
    arc_cx = spine_x + 340
    arc_cy = (d_top + d_bottom) // 2
    arc_rx = 460
    arc_ry = (d_bottom - d_top) // 2
    
    # Spine line
    def draw_thick_line(draw, p1, p2, color, w):
        draw.line([p1, p2], fill=color, width=w)

    # Draw spine
    draw_thick_line(draw_c, (spine_x, d_top), (spine_x, d_bottom), cyan_core, stroke_w)
    draw_thick_line(draw_g, (spine_x, d_top), (spine_x, d_bottom), cyan_glow, stroke_w + 30)

    # Draw top bar
    draw_thick_line(draw_c, (spine_x, d_top), (arc_cx, d_top), cyan_core, stroke_w)
    draw_thick_line(draw_g, (spine_x, d_top), (arc_cx, d_top), cyan_glow, stroke_w + 30)

    # Draw bottom bar
    draw_thick_line(draw_c, (spine_x, d_bottom), (arc_cx, d_bottom), cyan_core, stroke_w)
    draw_thick_line(draw_g, (spine_x, d_bottom), (arc_cx, d_bottom), cyan_glow, stroke_w + 30)

    # Draw outer arc
    arc_bbox = [arc_cx - arc_rx, d_top, arc_cx + arc_rx, d_bottom]
    draw_c.arc(arc_bbox, start=270, end=90, fill=cyan_core, width=stroke_w)
    draw_g.arc(arc_bbox, start=270, end=90, fill=cyan_glow, width=stroke_w + 30)

    # 2. Pulse Waveform (ECG heartbeat) across middle
    # Middle line y ~ 780
    py = 780
    pulse_w = 64
    pulse_pts = [
        (340, py),          # Starts left outside D
        (650, py),          # Goes through spine
        (710, py),          # Short flat
        (760, py + 120),    # Small dip down
        (850, py - 380),    # Huge sharp peak UP
        (940, py + 340),    # Deep trough DOWN
        (1010, py - 110),   # Rebound up
        (1070, py + 40),    # Mini settle
        (1120, py),         # Back to baseline
        (1340, py)          # Ends inside right curve
    ]

    for i in range(len(pulse_pts) - 1):
        p1 = pulse_pts[i]
        p2 = pulse_pts[i+1]
        draw_thick_line(draw_c, p1, p2, cyan_core, pulse_w)
        draw_thick_line(draw_g, p1, p2, cyan_glow, pulse_w + 26)

    # 3. Radar in Bottom-Right
    radar_cx = 1320
    radar_cy = 1180
    radar_r = 230
    radar_w = 40

    # Draw 3 concentric rings
    for r in [radar_r, int(radar_r * 0.68), int(radar_r * 0.38)]:
        bbox = [radar_cx - r, radar_cy - r, radar_cx + r, radar_cy + r]
        draw_c.ellipse(bbox, outline=cyan_core, width=radar_w)
        draw_g.ellipse(bbox, outline=cyan_glow, width=radar_w + 20)

    # Draw Radar Sweep Pie Wedge (from -60 deg to 0 deg)
    draw_c.pieslice([radar_cx - radar_r, radar_cy - radar_r, radar_cx + radar_r, radar_cy + radar_r],
                   start=300, end=360, fill=(0, 240, 255, 140))
    # Radar sweep arm line
    arm_x = radar_cx + int(radar_r * math.cos(math.radians(300)))
    arm_y = radar_cy + int(radar_r * math.sin(math.radians(300)))
    draw_thick_line(draw_c, (radar_cx, radar_cy), (arm_x, arm_y), cyan_core, radar_w)

    # Center dot of radar
    draw_c.ellipse([radar_cx - 24, radar_cy - 24, radar_cx + 24, radar_cy + 24], fill=cyan_core)

    # 4. Text "D Ping" underneath
    # We draw geometric lettering for "D  P i n g"
    # Or load standard clean TrueType font
    font = None
    try:
        font = ImageFont.truetype("arialbd.ttf", 190)
    except:
        try:
            font = ImageFont.truetype("segoeuib.ttf", 190)
        except:
            font = ImageFont.load_default()

    text = "D Ping"
    if font:
        # Measure text
        bbox = draw_c.textbbox((0, 0), text, font=font)
        tw = bbox[2] - bbox[0]
        th = bbox[3] - bbox[1]
        tx = (S - tw) // 2
        ty = 1580
        draw_c.text((tx, ty), text, font=font, fill=cyan_core)
        draw_g.text((tx, ty), text, font=font, fill=cyan_glow)

    # Apply soft Gaussian glow to glow layer
    glow_blurred = glow.filter(ImageFilter.GaussianBlur(radius=28))
    glow_wide = glow.filter(ImageFilter.GaussianBlur(radius=64))

    # Composite layers: Background + Wide Glow + Soft Glow + Core
    result = Image.alpha_composite(img, glow_wide)
    result = Image.alpha_composite(result, glow_blurred)
    result = Image.alpha_composite(result, core)

    # Downsample with Lanczos to 1024x1024 for pristine quality
    master_1024 = result.resize((1024, 1024), Image.Resampling.LANCZOS)
    master_512 = master_1024.resize((512, 512), Image.Resampling.LANCZOS)

    # Save master high-res files
    master_1024.save("dping_icon_1024.png", "PNG", optimize=True)
    master_512.save("dping_icon_512.png", "PNG", optimize=True)
    master_512.save("app/src/main/res/drawable/ic_dping_logo.png", "PNG", optimize=True)

    # Adaptive Foreground (432x432)
    # The icon mark (without the bottom text) centered inside the 288px safe zone
    fg_canvas = Image.new("RGBA", (432, 432), (0, 0, 0, 0))
    # Crop the central mark (y from 240 to 1480, x from 300 to 1700)
    mark_crop = result.crop((280, 240, 1660, 1480))
    target_h = int(432 * 0.65)
    target_w = int(mark_crop.width * (target_h / mark_crop.height))
    mark_resized = mark_crop.resize((target_w, target_h), Image.Resampling.LANCZOS)
    fg_canvas.paste(mark_resized, ((432 - target_w) // 2, (432 - target_h) // 2), mark_resized)
    fg_canvas.save("app/src/main/res/drawable/ic_launcher_foreground.png", "PNG", optimize=True)

    # Launcher Mipmaps
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192
    }

    for folder, size in densities.items():
        out_dir = os.path.join("app", "src", "main", "res", folder)
        os.makedirs(out_dir, exist_ok=True)
        icon = master_1024.resize((size, size), Image.Resampling.LANCZOS)
        icon.save(os.path.join(out_dir, "ic_launcher.png"), "PNG", optimize=True)

        # Round icon with circular clip
        mask = Image.new("L", (size, size), 0)
        d_mask = ImageDraw.Draw(mask)
        d_mask.ellipse((0, 0, size - 1, size - 1), fill=255)
        round_icon = Image.new("RGBA", (size, size), (5, 8, 14, 255))
        round_icon.paste(icon, (0, 0), mask)
        d_round = ImageDraw.Draw(round_icon)
        d_round.ellipse((0, 0, size - 1, size - 1), outline=(0, 240, 255, 160), width=max(1, size // 48))
        round_icon.save(os.path.join(out_dir, "ic_launcher_round.png"), "PNG", optimize=True)

    print("Rebuilt Vector & Ultra High-Res Logo successfully generated!")

if __name__ == "__main__":
    draw_rebuilt_vector_logo()
