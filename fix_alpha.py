from PIL import Image

def clear_center(filepath, width_pct, height_pct):
    try:
        img = Image.open(filepath).convert("RGBA")
        pixels = img.load()
        width, height = img.size

        # Calculate bounding box for the clear zone
        left = int(width * (1 - width_pct) / 2)
        right = int(width * (1 + width_pct) / 2)
        top = int(height * (1 - height_pct) / 2)
        bottom = int(height * (1 + height_pct) / 2)

        # Force alpha to 0 in this zone
        for x in range(left, right):
            for y in range(top, bottom):
                pixels[x, y] = (0, 0, 0, 0)

        img.save(filepath)
        print(f"Successfully cleared center of {filepath}")
    except Exception as e:
        print(f"Error processing {filepath}: {e}")

# Clear the center 50% width and 40% height to ensure clear vision
clear_center("src/main/resources/assets/ironcore/textures/gui/hud_layout.png", 0.5, 0.4)
clear_center("src/main/resources/assets/ironcore/textures/gui/mask_frame.png", 0.5, 0.4)
