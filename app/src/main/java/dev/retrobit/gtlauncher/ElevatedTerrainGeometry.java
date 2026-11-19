package dev.retrobit.gtlauncher;

/** Scene-space projection of preview block heights. Does not enable Android import by itself. */
final class ElevatedTerrainGeometry {
    interface Heights { int height(int column,int row); }
    static float elevation(int height,float blockHeight) { return Math.max(0,height-1)*blockHeight; }
    static float stepElevation(int fromHeight,int toHeight,float progress,float blockHeight) {
        float p=Math.max(0,Math.min(1,progress));
        return elevation(fromHeight,blockHeight)*(1-p)+elevation(toHeight,blockHeight)*p;
    }
    static float centreX(int c,int r,float originX,float tileWidth) { return originX+(c-r)*tileWidth/2f; }
    static float centreY(int c,int r,int height,float originY,float tileHeight,float blockHeight) {
        return originY+(c+r)*tileHeight/2f-elevation(height,blockHeight);
    }
    static boolean topContains(float x,float y,float cx,float cy,float tileWidth,float tileHeight) {
        return Math.abs(x-cx)/(tileWidth/2f)+Math.abs(y-cy)/(tileHeight/2f)<=1f;
    }
    /** Frontmost top diamond at this scene point, or null for a hole/outside. */
    static PortalTravel.Tile topAt(float x,float y,int size,Heights heights,float originX,float originY,float tileWidth,float tileHeight,float blockHeight) {
        if(heights==null || size<1 || tileWidth<=0 || tileHeight<=0 || blockHeight<=0 || !Float.isFinite(x) || !Float.isFinite(y))return null;
        for(int diagonal=2*size-2;diagonal>=0;diagonal--)for(int row=Math.min(size-1,diagonal);row>=0;row--) {
            int col=diagonal-row;if(col<0 || col>=size)continue;int h=heights.height(col,row);if(h<=0)continue;
            if(topContains(x,y,centreX(col,row,originX,tileWidth),centreY(col,row,h,originY,tileHeight,blockHeight),tileWidth,tileHeight))return new PortalTravel.Tile(col,row);
        }
        return null;
    }
    /** Number of front-facing block levels visible above an adjacent column. */
    static int exposedLevels(int height,int neighborHeight) {return Math.max(0,height-Math.max(0,neighborHeight));}
    private ElevatedTerrainGeometry() {}
}
