package dev.retrobit.gtlauncher;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class PortalTravelTest {
    private final PortalTravel.Tile north = new PortalTravel.Tile(6,0), south = new PortalTravel.Tile(6,8);
    private final PortalTravel.Portal home = new PortalTravel.Portal("home","g1","N",north,"farm","g9");
    private final PortalTravel.Portal farm = new PortalTravel.Portal("farm","g9","S",south,"home","g1");
    private final PortalTravel.Terrain clear = new PortalTravel.Terrain() {
        public boolean canStand(int c,int r) { return true; }
        public boolean canStep(int c,int r,int x,int y) { return true; }
    };
    @Test public void linkedOppositeEdgesRequireBothWorldsAndReciprocity() {
        assertTrue(PortalTravel.arePaired(home,farm));
        assertFalse(PortalTravel.arePaired(home,new PortalTravel.Portal("farm","g9","S",south,"else","g1")));
        assertFalse(PortalTravel.arePaired(home,new PortalTravel.Portal("farm","g9","N",north,"home","g1")));
        assertFalse(PortalTravel.arePaired(home,null));
    }
    @Test public void findExactPortalWithoutCrossingClosedEdges() {
        PortalTravel.Terrain terrain = new PortalTravel.Terrain() {
            public boolean canStand(int c,int r) { return !(c==6 && r==1); }
            public boolean canStep(int c,int r,int x,int y) { return true; }
        };
        List<PortalTravel.Tile> path=PortalTravel.pathToPortal(new PortalTravel.Tile(6,2),home,terrain);
        assertNotNull(path); assertEquals(north,path.get(path.size()-1)); assertFalse(path.contains(new PortalTravel.Tile(6,1)));
        PortalTravel.Terrain closed = new PortalTravel.Terrain() {
            public boolean canStand(int c,int r) { return true; }
            public boolean canStep(int c,int r,int x,int y) { return false; }
        };
        assertNull(PortalTravel.pathToPortal(new PortalTravel.Tile(6,2),home,closed));
        assertEquals(1,PortalTravel.pathToPortal(north,home,closed).size());
    }
    @Test public void pathToOrdinaryTileRespectsEdgesAndOccupancy() {
        PortalTravel.Terrain blocked=new PortalTravel.Terrain(){
            public boolean canStand(int c,int r){return !(c==2 && r==1);}
            public boolean canStep(int c,int r,int x,int y){return !(c==1 && r==1 && x==1 && y==2);}
        };
        List<PortalTravel.Tile> path=PortalTravel.path(new PortalTravel.Tile(1,1),new PortalTravel.Tile(3,1),blocked);
        assertNotNull(path);assertFalse(path.contains(new PortalTravel.Tile(2,1)));assertEquals(new PortalTravel.Tile(3,1),path.get(path.size()-1));
        assertNull(PortalTravel.path(new PortalTravel.Tile(1,1),new PortalTravel.Tile(2,1),blocked));assertNull(PortalTravel.path(new PortalTravel.Tile(1,1),new PortalTravel.Tile(-1,1),blocked));
    }
    @Test public void routeUsesShortestAvailableDetourAroundObjectsAndFences() {
        PortalTravel.Terrain obstacle=new PortalTravel.Terrain(){
            public boolean canStand(int c,int r){return !(c==1 && r==0);}
            public boolean canStep(int c,int r,int x,int y){return true;}
        };
        List<PortalTravel.Tile> route=PortalTravel.path(new PortalTravel.Tile(0,0),new PortalTravel.Tile(2,0),obstacle,25);
        assertNotNull(route);assertEquals(5,route.size());assertFalse(route.contains(new PortalTravel.Tile(1,0)));
        PortalTravel.Terrain fence=new PortalTravel.Terrain(){
            public boolean canStand(int c,int r){return true;}
            public boolean canStep(int c,int r,int x,int y){return !((c==0&&r==0&&x==1&&y==0)||(c==1&&r==0&&x==0&&y==0));}
        };
        route=PortalTravel.path(new PortalTravel.Tile(0,0),new PortalTravel.Tile(1,0),fence,25);
        assertNotNull(route);assertEquals(4,route.size());
    }
    @Test public void pathSupportsExplicitTwentyTileGridWithoutChangingLegacyBounds() {
        PortalTravel.Tile start = new PortalTravel.Tile(9,9), end = new PortalTravel.Tile(19,19);
        assertNull(PortalTravel.path(start,end,clear));
        List<PortalTravel.Tile> path = PortalTravel.path(start,end,clear,20);
        assertNotNull(path); assertEquals(21,path.size()); assertEquals(end,path.get(path.size()-1));
        assertNull(PortalTravel.path(start,new PortalTravel.Tile(20,19),clear,20));
        assertNull(PortalTravel.path(start,end,clear,26));
        assertNotNull(PortalTravel.path(start,new PortalTravel.Tile(24,24),clear,25));
        assertNull(PortalTravel.path(start,new PortalTravel.Tile(25,24),clear,25));
        PortalTravel.Terrain blocked = new PortalTravel.Terrain() {
            public boolean canStand(int c,int r) { return !(c==10 && r==9); }
            public boolean canStep(int c,int r,int x,int y) { return true; }
        };
        assertFalse(PortalTravel.path(start,end,blocked,20).contains(new PortalTravel.Tile(10,9)));
    }
    @Test public void pathBesideChoosesReachableClosestFootprintEdge() {
        PortalTravel.Terrain terrain=new PortalTravel.Terrain(){
            public boolean canStand(int c,int r){return !(c==4 && r==4) && !(c==3 && r==4);}
            public boolean canStep(int c,int r,int x,int y){return !(c==2 && r==3 && x==3 && y==3);}
        };
        List<PortalTravel.Tile> route=PortalTravel.pathBeside(new PortalTravel.Tile(1,4),4,4,1,1,terrain);
        assertNotNull(route);PortalTravel.Tile end=route.get(route.size()-1);assertEquals(1,Math.abs(end.column-4)+Math.abs(end.row-4));assertNotEquals(new PortalTravel.Tile(3,4),end);
        for(int i=1;i<route.size();i++){PortalTravel.Tile a=route.get(i-1),b=route.get(i);assertTrue(terrain.canStand(b.column,b.row));assertTrue(terrain.canStep(a.column,a.row,b.column,b.row));}
        PortalTravel.Terrain trapped=new PortalTravel.Terrain(){public boolean canStand(int c,int r){return c==0 && r==0;}public boolean canStep(int c,int r,int x,int y){return false;}};
        assertNull(PortalTravel.pathBeside(new PortalTravel.Tile(0,0),4,4,1,1,trapped));
        assertNull(PortalTravel.pathBeside(new PortalTravel.Tile(0,0),8,8,2,1,clear));
        assertEquals(1,PortalTravel.pathBeside(new PortalTravel.Tile(0,1),1,1,2,2,clear).size());
    }
    @Test public void pathBesideHouseOnTwentyFiveGrid() {
        PortalTravel.Terrain terrain=new PortalTravel.Terrain(){
            public boolean canStand(int c,int r){return !(c>=20 && c<23 && r>=20 && r<23);}
            public boolean canStep(int c,int r,int x,int y){return true;}
        };
        List<PortalTravel.Tile> route=PortalTravel.pathBeside(new PortalTravel.Tile(24,24),20,20,3,3,terrain,25);
        assertNotNull(route); PortalTravel.Tile end=route.get(route.size()-1);
        int dx=Math.max(Math.max(20-end.column,0),end.column-22),dy=Math.max(Math.max(20-end.row,0),end.row-22);
        assertEquals(1,dx+dy); assertNull(PortalTravel.pathBeside(new PortalTravel.Tile(24,24),23,23,3,3,terrain,25));
    }
    @Test public void confirmChecksFrontPairAndDestinationOccupancy() {
        assertTrue(PortalTravel.canTravel(home,farm,north,clear));
        assertFalse(PortalTravel.canTravel(home,farm,new PortalTravel.Tile(6,1),clear));
        PortalTravel.Terrain blocked = new PortalTravel.Terrain() {
            public boolean canStand(int c,int r) { return !(c==6 && r==8); }
            public boolean canStep(int c,int r,int x,int y) { return true; }
        };
        assertFalse(PortalTravel.canTravel(home,farm,north,blocked));
        assertFalse(PortalTravel.canTravel(home,null,north,clear));
    }
}
