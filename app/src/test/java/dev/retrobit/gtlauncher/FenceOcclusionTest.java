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
    @Test public void stackFadesOnlyInFrontAndSkipsInvisibleAndSameOwner() {
        FenceOcclusion.Stack stack=new FenceOcclusion.Stack();
        FenceOcclusion.Bounds box=new FenceOcclusion.Bounds(0,0,10,10),far=new FenceOcclusion.Bounds(20,20,30,30);
        Object first=new Object(),second=new Object();
        assertFalse(stack.register(first,box,(x,y)->true,true));
        assertFalse(stack.register(first,box,(x,y)->true,true));
        assertFalse(stack.register(second,far,(x,y)->true,true));
        assertFalse(stack.register(second,box,(x,y)->false,true));
        assertTrue(stack.register(second,box,(x,y)->true,true));
        stack.clear();
        assertFalse(stack.register(first,box,(x,y)->true,false));
        assertFalse(stack.register(second,box,(x,y)->true,true));
    }
}
