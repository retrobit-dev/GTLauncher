package dev.retrobit.gtlauncher;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
public class CloudLayoutTest {
    @Test public void scalesDensityWithIslandAreaAndCover() {
        double[] covers={0,32,58,82};
        int[] base={3,6,8,11},large={13,25,38,50};
        for(int i=0;i<covers.length;i++) {
            assertEquals(base[i],CloudLayout.count(covers[i],9));
            assertEquals(large[i],CloudLayout.count(covers[i],20));
        }
    }
    @Test public void cloudFitsOneTile() {
        for(int columns : new int[]{16,30,40}) for(int rows : new int[]{8,13,20}) {
            float cell=CloudLayout.cellSize(28,14,columns,rows);
            assertEquals(Math.min(28f/columns,14f/rows),cell,0.000001f);
            org.junit.Assert.assertTrue(cell*columns<=28.0001f);
            org.junit.Assert.assertTrue(cell*rows<=14.0001f);
        }
    }
    @Test public void fadeInOutAndReducedMotion() {
        assertEquals(1, CloudLayout.shadowFade(0,true),1e-9);
        assertEquals(.5, CloudLayout.shadowFade(6,true),1e-9);
        assertEquals(0, CloudLayout.shadowFade(12,true),1e-9);
        assertEquals(.5, CloudLayout.shadowFade(18,true),1e-9);
        assertEquals(1, CloudLayout.shadowFade(12,false),1e-9);
    }
}
