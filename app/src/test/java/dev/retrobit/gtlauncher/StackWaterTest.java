package dev.retrobit.gtlauncher;
import org.junit.Test;
import static org.junit.Assert.*;
public class StackWaterTest {
    @Test public void waterConnectsAtSameLevelAndFallsOnlyDownhill() {
        int[][][] b={{{1,3,3},{1,3},{1}},{{1,3,3},{1,3,3},{1,3}}};
        StackWater.Terrain t=new StackWater.Terrain(){public int size(){return 3;}public int height(int c,int r){return r<2?b[r][c].length:0;}public int block(int c,int r,int level){return b[r][c][level];}};
        assertTrue(StackWater.atLevel(t,0,0,1));assertTrue(StackWater.atLevel(t,1,0,1));assertFalse(StackWater.atLevel(t,1,0,2));
        assertEquals(1,StackWater.fall(t,0,0,1,0));assertEquals(0,StackWater.fall(t,1,0,0,0));
        assertEquals(0,StackWater.fall(t,0,0,0,1));assertEquals(0,StackWater.fall(t,1,0,2,0));assertEquals(0,StackWater.fall(t,0,0,-1,0));assertEquals(0,StackWater.fall(t,0,0,1,1));
    }
}
