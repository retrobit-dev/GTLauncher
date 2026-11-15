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
}
