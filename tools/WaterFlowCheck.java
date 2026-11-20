package dev.retrobit.gtlauncher;
public final class WaterFlowCheck {
    public static void main(String[] args) {
        final int[][] h=new int[20][20];final int[][] t=new int[20][20];
        WaterFlow.Terrain terrain=new WaterFlow.Terrain(){public int size(){return 20;}public int height(int c,int r){return h[r][c];}public int block(int c,int r,int level){return t[r][c];}};
        for(int x=5;x<8;x++){h[5][x]=1;t[5][x]=3;}
        if(!"E".equals(WaterFlow.direction(terrain,6,5)))throw new AssertionError();
        h[5][6]=2;if(!"E".equals(WaterFlow.direction(terrain,6,5)))throw new AssertionError();
        h[5][5]=0;h[4][6]=1;t[4][6]=3;if(!"N".equals(WaterFlow.direction(terrain,6,5)))throw new AssertionError();
        if(WaterFlow.direction(terrain,1,1)!=null)throw new AssertionError();
        System.out.println("Android derived current long axis and downhill: OK");
    }
}
