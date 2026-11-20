package dev.retrobit.gtlauncher;
import org.junit.Test;
import static org.junit.Assert.*;
public class RoadGeometryTest {
    @Test public void rotationChangesOnlyTextureCoordinates() {
        assertArrayEquals(new float[]{.2f,.3f},RoadGeometry.rotate(.2f,.3f,"S"),.00001f);
        assertArrayEquals(new float[]{.7f,.2f},RoadGeometry.rotate(.2f,.3f,"W"),.00001f);
        assertArrayEquals(new float[]{.8f,.7f},RoadGeometry.rotate(.2f,.3f,"N"),.00001f);
        assertArrayEquals(new float[]{.3f,.8f},RoadGeometry.rotate(.2f,.3f,"E"),.00001f);
    }
    @Test public void junctionNotchesAndCompatibleNeighbors() {
        RoadGeometry.Occupied none=(c,r,w)->false;
        float[][] single=RoadGeometry.outline(4,4,false,none);assertEquals(4,single.length);assertEquals(.12f,single[0][0],.0001f);
        RoadGeometry.Occupied l=(c,r,w)->!w && ((c==3&&r==4)||(c==4&&r==3));
        float[][] junction=RoadGeometry.outline(4,4,false,l);assertEquals(6,junction.length);assertEquals(0f,junction[0][0],.0001f);assertEquals(.12f,junction[0][1],.0001f);
        float[][] water=RoadGeometry.outline(4,4,true,l);assertEquals(4,water.length);assertEquals(.12f,water[0][0],.0001f);
    }
}
