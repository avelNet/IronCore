from PIL import Image

def remove_background(filepath):
    try:
        img = Image.open(filepath).convert("RGBA")
        pixels = img.load()
        width, height = img.size

        # Find the background color from the very top-left pixel (assuming it's empty grid)
        # We will use a threshold to remove anything similar to the grid color
        # The grid is usually dark, while the UI elements are bright cyan/red/white
        
        for x in range(width):
            for y in range(height):
                r, g, b, a = pixels[x, y]
                
                # If the pixel is dark (part of the grid or black background)
                # and NOT part of the bright UI elements
                # Thresholds: low RGB values mean it's black/dark gray grid
                if r < 40 and g < 40 and b < 40:
                    pixels[x, y] = (r, g, b, 0) # Set Alpha to 0 (fully transparent)

        img.save(filepath)
        print(f"Background removed successfully for {filepath}")
    except Exception as e:
        print(f"Error processing {filepath}: {e}")

remove_background("src/main/resources/assets/ironcore/textures/gui/hud_layout.png")
remove_background("src/main/resources/assets/ironcore/textures/gui/mask_frame.png")
