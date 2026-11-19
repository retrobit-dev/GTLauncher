package dev.retrobit.gtlauncher;

/** Weather counts and island-wide decorative cloud-shadow layout. */
final class CloudLayout {
    static int count(double cover, int weatherCode) {
        double value = Double.isFinite(cover) ? Math.max(0,Math.min(100,cover)) : 0;
        if(weatherCode>=95 && weatherCode<=99)return Math.min(10,7+(int)Math.floor(value/25));
        if(weatherCode==0 || weatherCode==1)return 3;
        return Math.max(3,Math.min(6,3+(int)Math.floor(value/25)));
    }
    static float cellSize(float tileWidth,float tileHeight,int columns,int rows,int gridSize,int count) {
        if(tileWidth<=0 || tileHeight<=0 || columns<=0 || rows<=0 || gridSize<=0 || count<=0)throw new IllegalArgumentException("Invalid cloud dimensions");
        double span=Math.max(6,gridSize*.9/Math.sqrt(count));
        return (float)(.25*span*Math.min(tileWidth/columns,tileHeight/rows));
    }
    static double[] seed(int index,int count,int gridSize,double jitterEast,double jitterNorth) {
        double east=(index+.5+(jitterEast-.5)*.3)/count;
        double north=((index*.6180339887498949+.5+(jitterNorth-.5)*.12)%1+1)%1;
        return new double[]{east*gridSize,north*gridSize};
    }
    static float shadowBlur(double cover,int code) {
        return cover>=65 || code==3 || code==45 || code==48 || code>=51 ? 3f:1.5f;
    }
    static double shadowFade(double seconds,boolean animationEnabled) {
        return animationEnabled ? .6+.4*(1+Math.cos(2*Math.PI*seconds/24.0))/2 : 1;
    }
    private CloudLayout() {}
}
