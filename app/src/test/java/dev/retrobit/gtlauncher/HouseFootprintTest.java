package dev.retrobit.gtlauncher;
import org.junit.Test;
import static org.junit.Assert.*;
public class HouseFootprintTest {
    @Test public void fiveDimensionsAndBounds(){
        String[] names={"3x3","3x4","4x3","3x5","5x3"};int[] cols={3,3,4,3,5},rows={3,4,3,5,3};
        for(int i=0;i<names.length;i++){
            HouseFootprint f=HouseFootprint.of(names[i]);assertEquals(cols[i],f.columns);assertEquals(rows[i],f.rows);
            assertTrue(f.fits(9-cols[i],9-rows[i]));assertFalse(f.fits(10-cols[i],9-rows[i]));assertFalse(f.fits(Integer.MAX_VALUE,0));assertFalse(f.fits(9-cols[i],10-rows[i]));
            assertTrue(f.contains(0,0,cols[i]-1,rows[i]-1));assertFalse(f.contains(0,0,cols[i],rows[i]-1));assertFalse(f.contains(0,0,cols[i]-1,rows[i]));
            assertFalse(f.overlaps(0,0,HouseFootprint.of(null),cols[i],0));assertTrue(f.overlaps(0,0,HouseFootprint.of(null),cols[i]-1,0));
        }
        assertEquals(4,HouseFootprint.of("wide").columns);assertEquals(3,HouseFootprint.of(null).columns);
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsUnknown(){HouseFootprint.of("other");}
}
