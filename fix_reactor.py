import sys
from PIL import Image

def process_texture(input_path):
    try:
        img = Image.open(input_path).convert("RGBA")
        width, height = img.size
        
        # Determine target square size based on the largest dimension
        size = max(width, height)
        # Ensure it's a multiple of 16 (or at least reasonable, 1024 is good)
        size = 1024 if size > 512 else 512
        
        # Create a new blank (transparent) square image
        final_img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        
        # For items in GUI, we don't want to stretch and distort them (which causes "squished" look).
        # We want to PASTE them in the center of the square.
        # This way, Minecraft's 'item/generated' model will render it correctly without distortion.
        paste_x = (size - width) // 2
        paste_y = (size - height) // 2
        
        final_img.paste(img, (paste_x, paste_y))
        
        # Remove watermark if we assume it's in the bottom right of the ORIGINAL image area
        # User said "справа снизу в углу"
        # We'll just mask out a small rect in the bottom right of the pasted area
        pixels = final_img.load()
        watermark_width = int(width * 0.15) # 15% of width
        watermark_height = int(height * 0.15) # 15% of height
        
        for x in range(paste_x + width - watermark_width, paste_x + width):
            for y in range(paste_y + height - watermark_height, paste_y + height):
                pixels[x, y] = (0, 0, 0, 0)
                
        # Resize to 32x32 to match other items and prevent huge lag/weird rendering
        final_img = final_img.resize((32, 32), Image.LANCZOS)
        
        final_img.save(input_path)
        print(f"Processed {input_path}")
    except Exception as e:
        print(f"Error processing {input_path}: {e}")

if __name__ == "__main__":
    process_texture(sys.argv[1])
