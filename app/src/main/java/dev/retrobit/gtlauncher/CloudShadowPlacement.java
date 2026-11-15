package dev.retrobit.gtlauncher;

/** Wrap projected cloud shadows into the interior of the island grid. */
final class CloudShadowPlacement {
    private static double wrap(double x,double period){return ((x%period)+period)%period;}
    static float[] onIsland(double x,double y,double cx,double cy,double tileWidth,double tileHeight) {
        double dx=(x-cx)/(tileWidth/2),dy=(y-cy)/(tileHeight/2);
        double a=wrap((dx+dy)/2+4,8)-4,b=wrap((dx-dy)/2+4,8)-4;
        return new float[]{(float)(cx+(a+b)*tileWidth/2),(float)(cy+(a-b)*tileHeight/2)};
    }
    private CloudShadowPlacement(){}
}
