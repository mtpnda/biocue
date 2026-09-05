from PIL import Image
import os

source_image_path = "/Users/valdiearsanur/.gemini/antigravity/brain/b4b615bf-942b-409f-84fc-fdf1d7ed20f8/biocue_app_icon_1788099790047.jpg"
apps = [
    "/Users/valdiearsanur/AndroidStudioProjects/biocue/mobile/src/main/res",
    "/Users/valdiearsanur/AndroidStudioProjects/biocue/wear/src/main/res"
]

sizes = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192
}

try:
    img = Image.open(source_image_path)
    
    # We want a circular/rounded square launcher icon, but standard Android icons are usually provided as PNGs
    # so we'll convert the JPG to PNG and resize it.
    for app_res in apps:
        for dpi, size in sizes.items():
            folder_path = os.path.join(app_res, f"mipmap-{dpi}")
            os.makedirs(folder_path, exist_ok=True)
            
            # Normal launcher icon
            resized_img = img.resize((size, size), Image.Resampling.LANCZOS)
            out_path = os.path.join(folder_path, "ic_launcher.png")
            resized_img.save(out_path, "PNG")
            
            # Round launcher icon
            out_path_round = os.path.join(folder_path, "ic_launcher_round.png")
            resized_img.save(out_path_round, "PNG")
            
    print("Icons generated successfully!")
except Exception as e:
    print(f"Error generating icons: {e}")
