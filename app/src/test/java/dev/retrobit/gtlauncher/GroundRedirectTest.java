package dev.retrobit.gtlauncher;
import org.junit.Test;
import static org.junit.Assert.*;
public class GroundRedirectTest {
 private final PortalTravel.Terrain open=new PortalTravel.Terrain(){public boolean canStand(int c,int r){return true;}public boolean canStep(int a,int b,int c,int d){return true;}};
 @Test public void reversesTowardNewTargetWithoutFinishingOldRoute() {
  PortalTravel.Tile a=new PortalTravel.Tile(4,1),b=new PortalTravel.Tile(5,1),target=new PortalTravel.Tile(2,1);
  GroundRedirect.Result r=GroundRedirect.plan(a,b,.2f,target,open,50);
  assertNotNull(r);assertTrue(r.reversed);assertEquals(b,r.route.get(0));assertEquals(a,r.route.get(1));assertEquals(target,r.route.get(r.route.size()-1));assertEquals(4,r.route.size());
 }
 @Test public void continuesViaCloserEndWhenTargetAhead() {
  PortalTravel.Tile a=new PortalTravel.Tile(4,1),b=new PortalTravel.Tile(5,1),target=new PortalTravel.Tile(7,1);
  GroundRedirect.Result r=GroundRedirect.plan(a,b,.3f,target,open,50);
  assertNotNull(r);assertFalse(r.reversed);assertEquals(a,r.route.get(0));assertEquals(b,r.route.get(1));assertEquals(target,r.route.get(r.route.size()-1));
 }
 @Test public void rejectsBlockedTarget() {
  PortalTravel.Terrain blocked=new PortalTravel.Terrain(){public boolean canStand(int c,int r){return c!=7 || r!=1;}public boolean canStep(int a,int b,int c,int d){return true;}};
  assertNull(GroundRedirect.plan(new PortalTravel.Tile(4,1),new PortalTravel.Tile(5,1),.5f,new PortalTravel.Tile(7,1),blocked,50));
 }
}
