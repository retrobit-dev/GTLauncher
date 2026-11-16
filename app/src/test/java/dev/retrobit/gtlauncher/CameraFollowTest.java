package dev.retrobit.gtlauncher;
import org.junit.Test;
import static org.junit.Assert.*;
public class CameraFollowTest {
 @Test public void easesWithoutOvershootingAndSettlesInsideDeadZone() {
  CameraFollow c=new CameraFollow();assertFalse(c.advance(0,0,2,2,0,false));
  assertTrue(c.advance(10,0,2,2,.016f,false));assertTrue(c.x()>0 && c.x()<8);
  float previous=c.x();for(int i=0;i<100;i++){c.advance(10,0,2,2,.016f,false);assertTrue(c.x()>=previous && c.x()<=8.001f);previous=c.x();}
  assertEquals(8,c.x(),.01);assertEquals(0,c.y(),.01);
 }
 @Test public void snapsForReducedMotionAndResetsForWorldChange() {
  CameraFollow c=new CameraFollow();c.advance(0,0,1,1,0,false);assertFalse(c.advance(10,0,1,1,.016f,true));assertEquals(9,c.x(),.001f);
  c.reset();assertFalse(c.ready());c.advance(90,60,1,1,0,false);assertEquals(90,c.x(),0);assertEquals(60,c.y(),0);
  c.advance(400,300,1,1,.016f,false);assertEquals(400,c.x(),0);
 }
 @Test public void stableAcrossFrameRates() {
  CameraFollow a=new CameraFollow(),b=new CameraFollow();a.advance(0,0,0,0,0,false);b.advance(0,0,0,0,0,false);
  for(int i=0;i<60;i++)a.advance(20,0,0,0,1f/60,false);
  for(int i=0;i<30;i++)b.advance(20,0,0,0,1f/30,false);
  assertEquals(a.x(),b.x(),.0001);
 }
}
