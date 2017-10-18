::Summary
::Command Line Usage:
::imgSrc: Full Path for Image
::palMethod: palFileMethod [0:ASCII(.GPL|.PAL), 1:Binary(YY .PAL), 2:Extract from Last Block of PNG]
::palSrc:(Used if Method 0 or 1 selected): Full Path for Pal File.
::sprTarget(optional): Name of Sprite that will be created. Will default to name of imgSrc with new extension. 
::romTarget(optional): Path of Rom to patch.
::Example Usage with separate Palette File
::java PNGto4BPP "imgSrc=C:\Users\Zayik\Documents\GitHub\fatmanspanda\ALTTP\Shadow_palette.PAL" "palSrc=C:\Users\Zayik\Documents\GitHub\fatmanspanda\ALTTP\Shadow_sheet.png" "palOption=1" "romTarget=C:\Users\Zayik\Documents\GitHub\fatmanspanda\ALTTP\Zelda_Japan.sfc"


java PNGto4BPP "imgSrc=C:\Users\Zayik\Documents\GitHub\fatmanspanda\ALTTP\Shadow_sheet.png" "palOption=2" "romTarget=C:\Users\Zayik\Documents\GitHub\fatmanspanda\ALTTP\Zelda_Japan.sfc"
