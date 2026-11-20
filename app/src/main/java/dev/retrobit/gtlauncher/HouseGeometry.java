package dev.retrobit.gtlauncher;

/** Projection of the aligned 512 x 490 standard house artwork onto its 3 x 3 footprint. */
final class HouseGeometry {
    static final float WIDTH=512f, HEIGHT=490f, PIVOT_X=256f, PIVOT_Y=482f;
    static final float BASE_LEFT=50f, BASE_RIGHT=430f, BASE_LEFT_Y=405f, BASE_RIGHT_Y=397f;
    final float halfDepth, sy, leftSx, leftShear, rightSx, rightShear;
    HouseGeometry(float tileWidth,float tileHeight,float scale) {
        float halfWidth=tileWidth*1.5f*scale;
        halfDepth=tileHeight*1.5f*scale;
        sy=85f/HEIGHT*scale;
        leftSx=halfWidth/(PIVOT_X-BASE_LEFT);
        rightSx=halfWidth/(BASE_RIGHT-PIVOT_X);
        leftShear=(-halfDepth-(BASE_LEFT_Y-PIVOT_Y)*sy)/(BASE_LEFT-PIVOT_X);
        rightShear=(-halfDepth-(BASE_RIGHT_Y-PIVOT_Y)*sy)/(BASE_RIGHT-PIVOT_X);
    }
    float sx(boolean left) { return left?leftSx:rightSx; }
    float shear(boolean left) { return left?leftShear:rightShear; }
    float[] project(float x,float y) {
        boolean left=x<PIVOT_X;float dx=x-PIVOT_X;
        return new float[]{dx*sx(left),(y-PIVOT_Y)*sy+dx*shear(left)};
    }
    float[] unproject(float x,float y) {
        boolean left=x<0;float dx=x/sx(left);
        return new float[]{PIVOT_X+dx,PIVOT_Y+(y-dx*shear(left))/sy};
    }
    /** Projected bounding rectangle in local coordinates, including roof overhangs. */
    float[] bounds(boolean mirror) {
        float minX=Float.POSITIVE_INFINITY,minY=Float.POSITIVE_INFINITY,maxX=Float.NEGATIVE_INFINITY,maxY=Float.NEGATIVE_INFINITY;
        for(float x:new float[]{0,PIVOT_X,WIDTH}) for(float y:new float[]{0,HEIGHT}) {
            float[] p=project(x,y);if(mirror)p[0]=-p[0];
            minX=Math.min(minX,p[0]);maxX=Math.max(maxX,p[0]);minY=Math.min(minY,p[1]);maxY=Math.max(maxY,p[1]);
        }
        return new float[]{minX,minY,maxX,maxY};
    }
}
