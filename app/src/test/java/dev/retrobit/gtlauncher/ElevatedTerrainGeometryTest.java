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
    @Test public void jumpOnlyForOneLevelAndArcIsReversible() {
        for(int[] heights:new int[][]{{1,2},{2,1},{10,11},{11,10}}) {
            assertEquals(0,ElevatedTerrainGeometry.jumpHeight(heights[0],heights[1],0,8),1e-5);
            assertEquals(6,ElevatedTerrainGeometry.jumpHeight(heights[0],heights[1],.5f,8),1e-5);
            assertEquals(0,ElevatedTerrainGeometry.jumpHeight(heights[0],heights[1],1,8),1e-5);
            for(float t:new float[]{.1f,.3f,.7f,.9f})assertEquals(ElevatedTerrainGeometry.jumpHeight(heights[0],heights[1],t,8),ElevatedTerrainGeometry.jumpHeight(heights[1],heights[0],1-t,8),1e-5);
        }
        assertEquals(0,ElevatedTerrainGeometry.jumpHeight(1,3,.5f,8),0);assertEquals(0,ElevatedTerrainGeometry.jumpHeight(2,2,.5f,8),0);
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
    @Test public void rampFacesArePickedWithAndWithoutStackedTerrain() {
        for(String direction:new String[]{"N","E","S","W"})for(int height:new int[]{1,2}) {
            RampGeometry.Shape shape=RampGeometry.create(direction,28,14,4);
            java.util.List<RampGeometry.Vertex[]> faces=new java.util.ArrayList<>();faces.add(shape.top);
            for(RampGeometry.Vertex[] face:shape.sides){boolean u=true,v=true;for(RampGeometry.Vertex p:face){u&=p.u==1;v&=p.v==1;}if(u||v)faces.add(face);}
            if(direction.equals("E")||direction.equals("S"))faces.add(shape.highFace);
            for(RampGeometry.Vertex[] face:faces) {
                float x=ElevatedTerrainGeometry.centreX(1,1,160,28),y=ElevatedTerrainGeometry.centreY(1,1,height,100,14,4);
                for(RampGeometry.Vertex p:face){x+=(float)p.x/face.length;y+=(float)p.y/face.length;}
                assertEquals(new PortalTravel.Tile(1,1),ElevatedTerrainGeometry.tileAt(x,y,3,(c,r)->c==1&&r==1?height:0,(c,r)->c==1&&r==1?direction:null,160,100,28,14,4,20));
            }
        }
    }
    @Test public void exposedWallsArePickableAndForegroundWins() {
        ElevatedTerrainGeometry.Heights h=(c,r)->c==0&&r==0?3:0;
        float cx=ElevatedTerrainGeometry.centreX(0,0,160,28),cy=ElevatedTerrainGeometry.centreY(0,0,3,100,14,8);
        assertEquals(new PortalTravel.Tile(0,0),ElevatedTerrainGeometry.tileAt(cx-7,cy+12,2,h,160,100,28,14,8,20));
        assertEquals(null,ElevatedTerrainGeometry.tileAt(cx-7,cy+60,2,h,160,100,28,14,8,20));
    }
    @Test public void fenceJunctionSharesWoodPostAcrossElevations() {
        WorldStore.SurfaceObject a=new WorldStore.SurfaceObject("fence",0,0,"S","preview-wood","{}"), b=new WorldStore.SurfaceObject("fence",1,0,"S","preview-wood","{}");
        java.util.List<WorldStore.SurfaceObject> fences=java.util.Arrays.asList(a,b);
        java.util.Map<WorldStore.SurfaceObject,FenceJoins.Join> flat=FenceJoins.plan(fences,o->1),stepped=FenceJoins.plan(fences,o->o==a?1:2);
        assertTrue(flat.get(a).linkB || flat.get(a).linkA);assertTrue(flat.get(b).linkA || flat.get(b).linkB);
        assertFalse(stepped.get(a).linkA || stepped.get(a).linkB);assertFalse(stepped.get(b).linkA || stepped.get(b).linkB);
        assertTrue(stepped.get(a).postB);assertFalse(stepped.get(b).postA);assertEquals(1,stepped.get(a).postExtraB);
        java.util.List<FenceMesh.Face> faces=FenceMesh.faces(0,0,14,7,false,stepped.get(a));
        boolean extendsAbove=false;for(FenceMesh.Face face:faces)for(int i=1;i<face.points.length;i+=2)if(face.points[i]<-15)extendsAbove=true;assertTrue(extendsAbove);
    }
}
