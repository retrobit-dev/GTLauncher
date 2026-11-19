package dev.retrobit.gtlauncher;
import org.junit.Test;
import static org.junit.Assert.*;
public class ElevatedTerrainGeometryTest {
    @Test public void projectsAndInterpolatesHeight() {
        assertEquals(0,ElevatedTerrainGeometry.elevation(1,8),0);assertEquals(16,ElevatedTerrainGeometry.elevation(3,8),0);
        assertEquals(12,ElevatedTerrainGeometry.stepElevation(2,3,.5f,8),0);
        assertEquals(16,ElevatedTerrainGeometry.stepElevation(2,3,3,8),0);
        assertEquals(0,ElevatedTerrainGeometry.exposedLevels(1,3));assertEquals(2,ElevatedTerrainGeometry.exposedLevels(3,1));
    }
    @Test public void picksElevatedTopAndSkipsHoles() {
        int[][] h={{1,0,1},{1,2,1},{1,1,3}};
        ElevatedTerrainGeometry.Heights heights=(c,r)->h[r][c];
        for(int[] t:new int[][]{{0,0},{2,0},{0,1},{1,1},{2,1},{0,2},{1,2},{2,2}}) {
            float x=ElevatedTerrainGeometry.centreX(t[0],t[1],160,28), y=ElevatedTerrainGeometry.centreY(t[0],t[1],h[t[1]][t[0]],100,14,8);
            PortalTravel.Tile hit=ElevatedTerrainGeometry.topAt(x,y,3,heights,160,100,28,14,8);
            // A taller, more frontward tile can occlude the centre of a rear tile.
            assertNotNull(hit);
            assertTrue(hit.column+hit.row>=t[0]+t[1]);
        }
        assertEquals(null,ElevatedTerrainGeometry.topAt(-100,-100,3,heights,160,100,28,14,8));
        assertEquals(null,ElevatedTerrainGeometry.topAt(Float.NaN,0,3,heights,160,100,28,14,8));
        assertTrue(ElevatedTerrainGeometry.topContains(160,100,160,100,28,14));
        assertFalse(ElevatedTerrainGeometry.topContains(175,100,160,100,28,14));
    }
    @Test public void exposedWallsArePickableAndForegroundWins() {
        ElevatedTerrainGeometry.Heights h=(c,r)->c==0&&r==0?3:0;
        float cx=ElevatedTerrainGeometry.centreX(0,0,160,28),cy=ElevatedTerrainGeometry.centreY(0,0,3,100,14,8);
        assertEquals(new PortalTravel.Tile(0,0),ElevatedTerrainGeometry.tileAt(cx-7,cy+12,2,h,160,100,28,14,8,20));
        assertEquals(null,ElevatedTerrainGeometry.tileAt(cx-7,cy+60,2,h,160,100,28,14,8,20));
    }
    @Test public void fenceJunctionsOnlyMergeAtEqualElevation() {
        WorldStore.SurfaceObject a=new WorldStore.SurfaceObject("fence",0,0,"S","preview-wood","{}"), b=new WorldStore.SurfaceObject("fence",1,0,"S","preview-wood","{}");
        java.util.List<WorldStore.SurfaceObject> fences=java.util.Arrays.asList(a,b);
        java.util.Map<WorldStore.SurfaceObject,FenceJoins.Join> flat=FenceJoins.plan(fences,o->1),stepped=FenceJoins.plan(fences,o->o==a?1:2);
        assertTrue(flat.get(a).linkB || flat.get(a).linkA);assertTrue(flat.get(b).linkA || flat.get(b).linkB);
        assertFalse(stepped.get(a).linkA || stepped.get(a).linkB);assertFalse(stepped.get(b).linkA || stepped.get(b).linkB);
    }
}
