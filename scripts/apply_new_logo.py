import os
from PIL import Image, ImageFilter, ImageEnhance

SRC_PATH = r"C:\Users\35010286\.gemini\antigravity\brain\ed6eb7a8-e8c8-420a-a7c9-86e297b33480\.user_uploaded\media_1792256890412_526e9634.png"
RES_DIR = r"d:\AntiGravity\D Ping\app\src\main\res"

def create_transparent_version(img):
    """Converts a glowing image on black into RGBA with smooth alpha channel."""
    rgba = img.convert("RGBA")
    data = rgba.getdata()
    new_data = []
    for item in data:
        r, g, b, _ = item
        brightness = max(r, g, b)
        if brightness < 15:
            new_data.append((0, 0, 0, 0))
        else:
            # Map brightness to alpha: bright pixels are 100% opaque, glows fade out
            alpha = min(255, int(brightness * 1.3))
            # Enhance cyan saturation slightly
            new_data.append((r, g, b, alpha))
    rgba.putdata(new_data)
    return rgba

def generate_logo_assets():
    src_img = Image.open(SRC_PATH).convert("RGBA")
    print(f"Loaded source logo from {SRC_PATH}, size: {src_img.size}")

    # 1. High-resolution master of full logo (Emblem + 'D Ping' text)
    # Upscale 4x with Lanczos to 792x740, then sharpen slightly for crispness
    full_hires = src_img.resize((src_img.width * 4, src_img.height * 4), Image.Resampling.LANCZOS)
    enhancer = ImageEnhance.Sharpness(full_hires)
    full_hires = enhancer.enhance(1.2)

    # Save master hires to drawable as ic_dping_logo.png
    drawable_dir = os.path.join(RES_DIR, "drawable")
    os.makedirs(drawable_dir, exist_ok=True)
    full_hires.save(os.path.join(drawable_dir, "ic_dping_logo.png"), "PNG")
    print(f"Saved {os.path.join(drawable_dir, 'ic_dping_logo.png')} ({full_hires.size})")

    # 2. Extract emblem (the D + waveform + radar, without text) for square launcher icon
    # Bounding box of emblem in original: x: 30 to 165, y: 8 to 140
    emblem_crop = src_img.crop((28, 6, 168, 142))
    
    # Place emblem centered on a square canvas with padding
    square_size = max(emblem_crop.width, emblem_crop.height) + 16
    emblem_square = Image.new("RGBA", (square_size, square_size), (0, 0, 0, 255))
    offset_x = (square_size - emblem_crop.width) // 2
    offset_y = (square_size - emblem_crop.height) // 2
    emblem_square.paste(emblem_crop, (offset_x, offset_y), emblem_crop)

    # Upscale emblem to 1024x1024 high resolution master
    emblem_1024 = emblem_square.resize((1024, 1024), Image.Resampling.LANCZOS)
    enhancer_sq = ImageEnhance.Sharpness(emblem_1024)
    emblem_1024 = enhancer_sq.enhance(1.2)

    # 3. Generate Android Mipmap densities for launcher icons
    density_sizes = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192
    }

    # Circular mask for round icons
    for folder, size in density_sizes.items():
        target_dir = os.path.join(RES_DIR, folder)
        os.makedirs(target_dir, exist_ok=True)

        # Standard launcher icon (square with subtle rounded corner or direct square)
        icon_square = emblem_1024.resize((size, size), Image.Resampling.LANCZOS)
        icon_square.save(os.path.join(target_dir, "ic_launcher.png"), "PNG")

        # Round launcher icon
        icon_round = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        # Create circle mask
        mask = Image.new("L", (size, size), 0)
        from PIL import ImageDraw
        draw = ImageDraw.Draw(mask)
        draw.ellipse((0, 0, size - 1, size - 1), fill=255)

        # Paste onto black circular canvas
        round_canvas = Image.new("RGBA", (size, size), (0, 0, 0, 255))
        round_canvas.paste(icon_square, (0, 0))
        icon_round.paste(round_canvas, (0, 0), mask)
        icon_round.save(os.path.join(target_dir, "ic_launcher_round.png"), "PNG")
        print(f"Generated {folder}: {size}x{size}")

    print("All raster logo assets successfully generated!")

if __name__ == "__main__":
    generate_logo_assets()
