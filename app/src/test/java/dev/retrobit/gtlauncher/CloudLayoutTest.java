package dev.retrobit.gtlauncher;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
public class CloudLayoutTest {
    @Test public void scalesDensityWithIslandAreaAndCover() {
        double[] covers={0,32,58,82};
        int[] base={1,2,3,4},large={25,50,75,99};
        for(int i=0;i<covers.length;i++) {
            assertEquals(base[i],CloudLayout.count(covers[i],9));
            assertEquals(large[i],CloudLayout.count(covers[i],20));
        }
    }
    @Test public void cloudFitsOneTile() {
        for(int columns : new int[]{16,30,40}) for(int rows : new int[]{8,13,20}) {
            float cell=CloudLayout.cellSize(28,14,columns,rows);
            org.junit.Assert.assertTrue(cell*columns<=28.0001f);
            org.junit.Assert.assertTrue(cell*rows<=14.0001f);
        }
    }
}
