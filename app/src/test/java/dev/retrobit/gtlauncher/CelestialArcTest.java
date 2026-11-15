package dev.retrobit.gtlauncher;
import org.junit.Test;
import static org.junit.Assert.*;
public class CelestialArcTest {
 @Test public void islandArcHasThreeHundredUnitRadius() {
  for (int hour=-90;hour<=90;hour+=15) {
   float[] offset=CelestialArc.islandOffset(hour,0,0,300);
   assertEquals(300,Math.hypot(offset[0],offset[1]),.0001);
  }
  float[] rise=CelestialArc.islandOffset(-90,0,0,300),peak=CelestialArc.islandOffset(0,0,0,300),set=CelestialArc.islandOffset(90,0,0,300);
  assertEquals(300,rise[0],.001);assertEquals(0,rise[1],.001);
  assertEquals(0,peak[0],.001);assertEquals(-300,peak[1],.001);
  assertEquals(-300,set[0],.001);assertEquals(0,set[1],.001);
 }
 @Test public void arcHeightIs300() {
  float[] peak=CelestialArc.islandOffset(0,0,0,300,300);
  assertEquals(0,peak[0],.001);assertEquals(-300,peak[1],.001);
  float[] rise=CelestialArc.islandOffset(-90,0,0,300,300);
  assertEquals(300,rise[0],.001);assertEquals(0,rise[1],.001);
 }
 @Test public void risePeakSetAndLatitude() {
  double lat=-7,dec=20,angle=Math.toDegrees(Math.acos(-Math.tan(Math.toRadians(lat))*Math.tan(Math.toRadians(dec))));
  float[] rise=CelestialArc.position(-angle,dec,lat,320,85,12.5f),peak=CelestialArc.position(0,dec,lat,320,85,12.5f),set=CelestialArc.position(angle,dec,lat,320,85,12.5f);
  assertEquals(307.5f,rise[0],.001f);assertEquals(85,rise[1],.001f);assertEquals(160,peak[0],.001f);assertEquals(12.5f,peak[1],.001f);assertEquals(12.5f,set[0],.001f);assertEquals(85,set[1],.001f);
  float[] polar=CelestialArc.position(0,23,89,320,85,12.5f);assertTrue(Float.isFinite(polar[0])&&Float.isFinite(polar[1]));
 }
}
