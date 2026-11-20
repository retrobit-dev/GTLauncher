package dev.retrobit.gtlauncher;

/** Water connectivity is per block level; a fall links exposed surfaces at different heights. */
final class StackWater {
    interface Terrain { int size(); int height(int c,int r); int block(int c,int r,int level); }
    private static boolean inside(Terrain t,int c,int r) { return c>=0 && r>=0 && c<t.size() && r<t.size(); }
    static boolean atLevel(Terrain t,int c,int r,int level) {
        return t!=null && level>=0 && inside(t,c,r) && t.height(c,r)>level && t.block(c,r,level)==3;
    }
    static int fall(Terrain t,int c,int r,int nc,int nr) {
        if(t==null || !inside(t,c,r) || !inside(t,nc,nr) || Math.abs(c-nc)+Math.abs(r-nr)!=1)return 0;
        int h=t.height(c,r),n=t.height(nc,nr);if(h==0||n==0 || t.block(c,r,h-1)!=3 || t.block(nc,nr,n-1)!=3)return 0;
        return Math.max(0,h-n);
    }
    private StackWater() {}
}
