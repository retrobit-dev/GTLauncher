package dev.retrobit.gtlauncher;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class IdleWaterRouteTest {
    @Test public void nearestDryThroughWaterAndObstacles() {
        PortalTravel.DryTerrain terrain=new PortalTravel.DryTerrain() {
            public boolean canStand(int c,int r) { return r==2 && c>=1 && c<=4; }
            public boolean canStep(int a,int b,int c,int d) { return true; }
            public boolean isDry(int c,int r) { return c==4; }
        };
        List<PortalTravel.Tile> route=PortalTravel.pathToNearestDry(new PortalTravel.Tile(1,2),terrain,5);
        assertNotNull(route); assertEquals(4,route.size());
        assertEquals(new PortalTravel.Tile(4,2),route.get(3));
        assertNull(PortalTravel.pathToNearestDry(new PortalTravel.Tile(5,2),terrain,5));
    }
    @Test public void inaccessibleDryTileDoesNotWin() {
        PortalTravel.DryTerrain terrain=new PortalTravel.DryTerrain() {
            public boolean canStand(int c,int r) { return r==1 && c<=3; }
            public boolean canStep(int a,int b,int c,int d) { return !(a==1 && c==2); }
            public boolean isDry(int c,int r) { return c>=2; }
        };
        assertNull(PortalTravel.pathToNearestDry(new PortalTravel.Tile(0,1),terrain,5));
    }
    @Test public void idleCarryResumesAndResetDiscardsIt() {
        PlayerIdleBow idle=new PlayerIdleBow();
        idle.update(1000,"water",true,false);
        idle.update(6000,"water",true,false);
        assertEquals(5000,idle.elapsed());
        idle.reset(); idle.resume(5000);
        idle.update(10000,"dry",true,true);
        idle.update(13000,"dry",true,true);
        assertEquals(8000,idle.elapsed());
        assertEquals(0f,idle.update(13000,"dry",false,true),0f);
        idle.update(14000,"dry",true,true);
        assertEquals(0,idle.elapsed());
    }
}
