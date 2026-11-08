import os
from PIL import Image, ImageOps, ImageFilter, ImageDraw

def generate_icons():
    src_path = r"C:\Users\35010286\.gemini\antigravity\brain\ed6eb7a8-e8c8-420a-a7c9-86e297b33480\.user_uploaded\media_1792250311710_8b05439e.png"
    if not os.path.exists(src_path):
        print(f"Error: {src_path} not found")
        return

    img = Image.open(src_path).convert("RGBA")
    w, h = img.size

    # Base canvas for 1024x1024 master
    master_size = 1024
    master = Image.new("RGBA", (master_size, master_size), (5, 7, 10, 255)) # sleek deep charcoal/black

    # Target size for the logo inside the master canvas
    target_h = int(master_size * 0.75)
    target_w = int(w * (target_h / h))
    logo_resized = img.resize((target_w, target_h), Image.Resampling.LANCZOS)

    # Center the logo
    offset_x = (master_size - target_w) // 2
    offset_y = (master_size - target_h) // 2
    master.paste(logo_resized, (offset_x, offset_y), logo_resized)

    # Master high-res Play Store icon
    master_512 = master.resize((512, 512), Image.Resampling.LANCZOS)
    os.makedirs("app/src/main/res/drawable", exist_ok=True)
    master_512.save("app/src/main/res/drawable/ic_dping_logo.png", "PNG")
    master_512.save("dping_icon_512.png", "PNG")

    # Mipmap dimensions
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192
    }

    # For adaptive foreground: 432x432 (central 72dp = 216px is safe area)
    fg_canvas = Image.new("RGBA", (432, 432), (0, 0, 0, 0))
    fg_target_h = int(432 * 0.65)
    fg_target_w = int(w * (fg_target_h / h))
    fg_resized = img.resize((fg_target_w, fg_target_h), Image.Resampling.LANCZOS)
    fg_canvas.paste(fg_resized, ((432 - fg_target_w) // 2, (432 - fg_target_h) // 2), fg_resized)
    fg_canvas.save("app/src/main/res/drawable/ic_launcher_foreground.png", "PNG")

    for folder, size in densities.items():
        out_dir = os.path.join("app", "src", "main", "res", folder)
        os.makedirs(out_dir, exist_ok=True)

        # Standard icon
        icon = master.resize((size, size), Image.Resampling.LANCZOS)
        icon.save(os.path.join(out_dir, "ic_launcher.png"), "PNG")

        # Round icon with circular mask
        mask = Image.new("L", (size, size), 0)
        draw = ImageDraw.Draw(mask)
        draw.ellipse((0, 0, size - 1, size - 1), fill=255)
        round_icon = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        round_icon.paste(icon, (0, 0), mask)
        round_icon.save(os.path.join(out_dir, "ic_launcher_round.png"), "PNG")
        print(f"Generated {folder} ({size}x{size})")

    print("High-res icons generated successfully!")

if __name__ == "__main__":
    generate_icons()
