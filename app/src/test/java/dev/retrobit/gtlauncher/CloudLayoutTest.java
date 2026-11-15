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
            assertEquals(.5f*Math.min(28f/columns,14f/rows),cell,0.000001f);
            org.junit.Assert.assertTrue(cell*columns<=14.0001f);
            org.junit.Assert.assertTrue(cell*rows<=7.0001f);
        }
    }
}
