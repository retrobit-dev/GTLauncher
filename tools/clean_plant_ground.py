"""Remove baked-in pale ground shading before anchoring plant artwork."""
def clean_ground_shadow(im):
    im=im.copy();px=im.load();w,h=im.size
    # Concept shadows are low-chroma, pale pixels below the foliage. Restrict
    # removal to the bottom quarter so highlights and colored blossoms survive.
    for y in range(h*3//4,h):
        for x in range(w):
            r,g,b,a=px[x,y]
            if a and min(r,g,b)>130 and max(r,g,b)-min(r,g,b)<48: px[x,y]=(0,0,0,0)
    # Remove any remaining faint shadow below the last row of dark foliage.
    last=0
    for y in range(h):
        if sum(1 for x in range(w) if px[x,y][3] and max(px[x,y][:3])<130)>=5: last=y
    im=im.crop((0,0,w,min(h,last+2)))
    return im.crop(im.getbbox())
