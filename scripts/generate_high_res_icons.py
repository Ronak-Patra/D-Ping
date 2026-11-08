import os
from PIL import Image, ImageFilter, ImageEnhance, ImageDraw

def generate_high_res_icons():
    src_path = r"C:\Users\35010286\.gemini\antigravity\brain\ed6eb7a8-e8c8-420a-a7c9-86e297b33480\.user_uploaded\media_1792250311710_8b05439e.png"
    if not os.path.exists(src_path):
        print(f"Error: {src_path} not found")
        return

    orig = Image.open(src_path).convert("RGBA")
    w, h = orig.size

    # Step 1: Clean background - replace near-black pixels with uniform deep obsidian #05080E
    cleaned_pixels = orig.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = cleaned_pixels[x, y]
            # If dark background pixel, set to solid obsidian
            if r < 18 and g < 18 and b < 24:
                cleaned_pixels[x, y] = (5, 8, 14, 255)

    # Multi-stage progressive upscaling with edge sharpening
    step1 = orig.resize((w * 2, h * 2), Image.Resampling.LANCZOS)
    step1 = step1.filter(ImageFilter.UnsharpMask(radius=2, percent=140, threshold=1))
    step2 = step1.resize((w * 4, h * 4), Image.Resampling.LANCZOS)
    step2 = step2.filter(ImageFilter.UnsharpMask(radius=2, percent=150, threshold=1))
    
    # 8x super-res for ultra sharpness
    super_res = step2.resize((1024, int(1024 * (h / w))), Image.Resampling.LANCZOS)
    super_res = super_res.filter(ImageFilter.UnsharpMask(radius=2.5, percent=160, threshold=2))
    
    # Boost cyan/teal vibrance
    color_enhancer = ImageEnhance.Color(super_res)
    super_res = color_enhancer.enhance(1.2)
    contrast_enhancer = ImageEnhance.Contrast(super_res)
    super_res = contrast_enhancer.enhance(1.15)

    # Create 1024x1024 master canvas
    master_1024 = Image.new("RGBA", (1024, 1024), (5, 8, 14, 255))
    sw, sh = super_res.size
    # Center on canvas with aesthetic margin
    target_sh = int(1024 * 0.82)
    target_sw = int(sw * (target_sh / sh))
    super_fitted = super_res.resize((target_sw, target_sh), Image.Resampling.LANCZOS)
    
    offset_x = (1024 - target_sw) // 2
    offset_y = (1024 - target_sh) // 2
    master_1024.paste(super_fitted, (offset_x, offset_y), super_fitted)

    # Save 512x512 master icons
    master_512 = master_1024.resize((512, 512), Image.Resampling.LANCZOS)
    master_512 = master_512.filter(ImageFilter.UnsharpMask(radius=1.5, percent=120, threshold=1))
    master_512.save("dping_icon_512.png", "PNG", optimize=True)
    master_512.save("app/src/main/res/drawable/ic_dping_logo.png", "PNG", optimize=True)

    # Adaptive Icon Foreground (432x432, safe central circle diameter is 288px)
    fg_canvas = Image.new("RGBA", (432, 432), (0, 0, 0, 0))
    fg_h = int(432 * 0.70)
    fg_w = int(sw * (fg_h / sh))
    fg_resized = super_res.resize((fg_w, fg_h), Image.Resampling.LANCZOS)
    fg_resized = fg_resized.filter(ImageFilter.UnsharpMask(radius=1.2, percent=120, threshold=1))
    fg_canvas.paste(fg_resized, ((432 - fg_w) // 2, (432 - fg_h) // 2), fg_resized)
    fg_canvas.save("app/src/main/res/drawable/ic_launcher_foreground.png", "PNG", optimize=True)

    # Densities for standard launchers
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

        # Standard icon
        icon = master_1024.resize((size, size), Image.Resampling.LANCZOS)
        icon = icon.filter(ImageFilter.UnsharpMask(radius=1.0, percent=110, threshold=1))
        icon.save(os.path.join(out_dir, "ic_launcher.png"), "PNG", optimize=True)

        # Round icon with circular clip and subtle neon ring
        mask = Image.new("L", (size, size), 0)
        draw_mask = ImageDraw.Draw(mask)
        draw_mask.ellipse((0, 0, size - 1, size - 1), fill=255)
        
        round_icon = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        round_icon.paste(icon, (0, 0), mask)
        
        # Subtle glowing rim
        draw_rim = ImageDraw.Draw(round_icon)
        draw_rim.ellipse((0, 0, size - 1, size - 1), outline=(0, 229, 255, 120), width=max(1, size // 48))
        round_icon.save(os.path.join(out_dir, "ic_launcher_round.png"), "PNG", optimize=True)
        print(f"Generated ultra-crisp {folder} ({size}x{size})")

    print("Ultra high-resolution icons generated successfully!")

if __name__ == "__main__":
    generate_high_res_icons()
