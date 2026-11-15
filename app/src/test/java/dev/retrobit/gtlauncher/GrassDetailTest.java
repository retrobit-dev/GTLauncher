package dev.retrobit.gtlauncher;
import org.junit.Test;
import static org.junit.Assert.*;
public class GrassDetailTest {
    @Test public void stableSparseVariantsAcrossIsland() {
        int[] counts = new int[4];
        for (int y=0;y<20;y++) for (int x=0;x<20;x++) {
            int variant=GrassDetail.variant(x,y);
            assertTrue(variant>=0 && variant<4);
            assertEquals(variant,GrassDetail.variant(x,y));
            counts[variant]++;
        }
        assertArrayEquals(new int[]{186,98,58,58}, counts);
        // Unlike the old parity-based pattern, diagonal neighbours aren't all identical.
        assertNotEquals(GrassDetail.variant(0,0),GrassDetail.variant(1,1));
    }
}
