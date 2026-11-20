package dev.retrobit.gtlauncher;
/** Derived current for connected water. No direction data is written to saved worlds. */
final class WaterFlow {
    interface Terrain { int size(); int height(int c,int r); int block(int c,int r,int level); }
    private static boolean water(Terrain t,int c,int r) {return c>=0 && r>=0 && c<t.size() && r<t.size() && t.height(c,r)>0 && t.block(c,r,t.height(c,r)-1)==3;}
    private static int count(Terrain t,int c,int r,int dx,int dy) {int n=0;while(water(t,c+=dx,r+=dy))n++;return n;}
    static String direction(Terrain t,int c,int r) {
        if(t==null || !water(t,c,r))return null;
        int[][] steps={{0,-1},{1,0},{0,1},{-1,0}};String[] names={"N","E","S","W"};int best=0;String downhill=null;
        for(int i=0;i<4;i++){int x=c+steps[i][0],y=r+steps[i][1];if(water(t,x,y)){int drop=t.height(c,r)-t.height(x,y);if(drop>best){best=drop;downhill=names[i];}}}
        if(downhill!=null)return downhill;
        return count(t,c,r,1,0)+count(t,c,r,-1,0)>=count(t,c,r,0,1)+count(t,c,r,0,-1)?"E":"S";
    }
    private WaterFlow() {}
}
