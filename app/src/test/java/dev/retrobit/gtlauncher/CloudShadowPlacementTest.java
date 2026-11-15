package dev.retrobit.gtlauncher;
import org.junit.Test;
import static org.junit.Assert.*;
public class CloudShadowPlacementTest {
 @Test public void allProjectedCentersWrapInsideIsland() {
  for(int x=-800;x<=800;x+=13) for(int y=-400;y<=400;y+=9) {
   float[] p=CloudShadowPlacement.onIsland(x,y,160,97.5,28,11.833311,9);
   double dx=(p[0]-160)/14,dy=(p[1]-97.5)/(11.833311/2);
   assertTrue(Math.abs((dx+dy)/2)<=4.001);assertTrue(Math.abs((dx-dy)/2)<=4.001);
  }
 }
 @Test public void largeGridCoversEntireIsland() {
  float[] p=CloudShadowPlacement.onIsland(160+8*14,97.5+8*7,160,97.5,28,14,20);
  assertEquals(160+8*14,p[0],.001);assertEquals(97.5+8*7,p[1],.001);
 }
}
