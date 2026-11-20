package dev.retrobit.gtlauncher;

/** Scene-space projection and hit testing for preview block heights. */
final class ElevatedTerrainGeometry {
    interface Heights { int height(int column,int row); }
    interface Ramps { String direction(int column,int row); }
    interface SurfaceOffset { float offset(int column,int row); }
    static float elevation(int height,float blockHeight) { return Math.max(0,height-1)*blockHeight; }
    static float stepElevation(int fromHeight,int toHeight,float progress,float blockHeight) {
        float p=Math.max(0,Math.min(1,progress));
        return elevation(fromHeight,blockHeight)*(1-p)+elevation(toHeight,blockHeight)*p;
    }
    static float jumpHeight(int fromHeight,int toHeight,float progress,float blockHeight) {
        if(fromHeight<1 || toHeight<1 || Math.abs(fromHeight-toHeight)!=1 || !Float.isFinite(progress) || !Float.isFinite(blockHeight) || blockHeight<=0)return 0;
        float p=Math.max(0,Math.min(1,progress));return (float)Math.sin(Math.PI*p)*blockHeight*.75f;
    }
    static float centreX(int c,int r,float originX,float tileWidth) { return originX+(c-r)*tileWidth/2f; }
    static float centreY(int c,int r,int height,float originY,float tileHeight,float blockHeight) {
        return originY+(c+r)*tileHeight/2f-elevation(height,blockHeight);
    }
    static boolean topContains(float x,float y,float cx,float cy,float tileWidth,float tileHeight) {
        return Math.abs(x-cx)/(tileWidth/2f)+Math.abs(y-cy)/(tileHeight/2f)<=1f;
    }
    /** Frontmost visible top or exposed front wall, in the same diagonal order as terrain. */
    static PortalTravel.Tile topAt(float x,float y,int size,Heights heights,float originX,float originY,float tileWidth,float tileHeight,float blockHeight) {
        return tileAt(x,y,size,heights,originX,originY,tileWidth,tileHeight,blockHeight,0);
    }
    static PortalTravel.Tile tileAt(float x,float y,int size,Heights heights,float originX,float originY,float tileWidth,float tileHeight,float blockHeight,float baseDepth) {
        return tileAt(x,y,size,heights,null,originX,originY,tileWidth,tileHeight,blockHeight,baseDepth);
    }
    static PortalTravel.Tile tileAt(float x,float y,int size,Heights heights,Ramps ramps,float originX,float originY,float tileWidth,float tileHeight,float blockHeight,float baseDepth) {
        return tileAt(x,y,size,heights,ramps,null,originX,originY,tileWidth,tileHeight,blockHeight,baseDepth);
    }
    static PortalTravel.Tile tileAt(float x,float y,int size,Heights heights,Ramps ramps,SurfaceOffset offsets,float originX,float originY,float tileWidth,float tileHeight,float blockHeight,float baseDepth) {
        if(heights==null || size<1 || tileWidth<=0 || tileHeight<=0 || blockHeight<=0 || !Float.isFinite(x) || !Float.isFinite(y))return null;
        for(int diagonal=2*size-2;diagonal>=0;diagonal--)for(int row=size-1;row>=0;row--) {
            int col=diagonal-row;if(col<0 || col>=size)continue;int h=heights.height(col,row);if(h<=0)continue;
            float cx=centreX(col,row,originX,tileWidth),cy=centreY(col,row,h,originY,tileHeight,blockHeight),dx=x-cx,hw=tileWidth/2f,hh=tileHeight/2f;
            String direction=ramps==null?null:ramps.direction(col,row);float topY=cy+(offsets==null?0:offsets.offset(col,row));
            if(direction!=null && RampGeometry.hit(direction,tileWidth,tileHeight,blockHeight,x-cx,y-topY))return new PortalTravel.Tile(col,row);
            if(topContains(x,y,cx,topY,tileWidth,tileHeight))return new PortalTravel.Tile(col,row);
            if(Math.abs(dx)<=hw) {
                int nc=col+(dx>=0?1:0),nr=row+(dx<0?1:0),neighbor=nc>=size || nr>=size?0:heights.height(nc,nr);
                float face=exposedLevels(h,neighbor)*blockHeight+(neighbor==0?Math.max(0,baseDepth-blockHeight):0);
                float edgeY=cy+hh*(1-Math.abs(dx)/hw);
                if(face>0 && y>=edgeY && y<=edgeY+face)return new PortalTravel.Tile(col,row);
            }
        }
        return null;
    }
    /** Number of front-facing block levels visible above an adjacent column. */
    static int exposedLevels(int height,int neighborHeight) {return Math.max(0,height-Math.max(0,neighborHeight));}
    private ElevatedTerrainGeometry() {}
}
