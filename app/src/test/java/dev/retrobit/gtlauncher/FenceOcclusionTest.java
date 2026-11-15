package dev.retrobit.gtlauncher;

import org.junit.Test;
import static org.junit.Assert.*;

public class FenceOcclusionTest {
    @Test public void considersOverlappingOpaquePixelsNotJustRectangles() {
        FenceOcclusion.Bounds fence=new FenceOcclusion.Bounds(0,0,10,10),object=new FenceOcclusion.Bounds(5,5,15,15);
        assertTrue(FenceOcclusion.overlaps(fence,(x,y)->true,object,(x,y)->true));
        assertFalse(FenceOcclusion.overlaps(fence,(x,y)->x<4,object,(x,y)->true));
        assertFalse(FenceOcclusion.overlaps(fence,(x,y)->true,object,(x,y)->x>10));
        assertFalse(FenceOcclusion.overlaps(fence,(x,y)->true,new FenceOcclusion.Bounds(10,0,20,10),(x,y)->true));
        assertFalse(FenceOcclusion.overlaps(fence,(x,y)->true,new FenceOcclusion.Bounds(20,20,30,30),(x,y)->true));
    }
}
