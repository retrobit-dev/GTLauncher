package dev.retrobit.gtlauncher;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
public class CloudLayoutTest {
    @Test public void weatherControlsCounts() {
        double[] covers={0,32,58,82}; int[] counts={3,4,6,8};
        for(int i=0;i<covers.length;i++) assertEquals(counts[i],CloudLayout.count(covers[i],-1));
        for(double cover:covers) {
            org.junit.Assert.assertTrue(CloudLayout.count(cover,0)<=4);
            org.junit.Assert.assertTrue(CloudLayout.count(cover,1)<=4);
            org.junit.Assert.assertTrue(CloudLayout.count(cover,3)>=5);
            org.junit.Assert.assertTrue(CloudLayout.count(cover,63)>=5);
        }
    }
    @Test public void cloudSizeIsFourTimesPrevious() {
        for(int columns : new int[]{16,30,40}) for(int rows : new int[]{8,13,20}) {
            float cell=CloudLayout.cellSize(28,14,columns,rows);
            assertEquals(4f*Math.min(28f/columns,14f/rows),cell,0.000001f);
            org.junit.Assert.assertTrue(cell*columns<=112.0001f);
            org.junit.Assert.assertTrue(cell*rows<=56.0001f);
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
