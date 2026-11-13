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
