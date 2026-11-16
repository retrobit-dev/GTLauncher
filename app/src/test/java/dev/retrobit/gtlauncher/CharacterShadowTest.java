package dev.retrobit.gtlauncher;
import org.junit.Test;
import static org.junit.Assert.*;
public class CharacterShadowTest {
 @Test public void shadowPointsAwayFromLightAndShortensAtNoon() {
  float[] north=CharacterShadow.offset(0,30,25),south=CharacterShadow.offset(180,30,25);
  assertTrue(north[0]<0);assertTrue(south[0]>0);
  assertTrue(north[1]>0);assertTrue(south[1]<0);
  float[] high=CharacterShadow.offset(0,80,25);
  assertTrue(Math.hypot(high[0],high[1])<Math.hypot(north[0],north[1]));
 }
 @Test public void projectedShadowFitsCharacterDimensions() {
  float width=13.5f,height=25f;
  for(int azimuth=0;azimuth<360;azimuth+=15) for(int altitude=1;altitude<=90;altitude+=5) {
   float[] ray=CharacterShadow.offset(azimuth,altitude,height);
   if(Math.abs(ray[1])<height*.12f)ray[1]=Math.copySign(height*.12f,ray[1]);
   float scale=CharacterShadow.fitScale(width,height,ray[0],ray[1]);
   assertTrue((width+Math.abs(ray[0]))*scale<=width+0.0001f);
   assertTrue(Math.abs(ray[1])*scale<=height+0.0001f);
  }
 }
 @Test public void lowSunIsCappedAndFinite() {
  float[] v=CharacterShadow.offset(90,0,25);
  assertTrue(Float.isFinite(v[0])&&Float.isFinite(v[1]));
  assertTrue(Math.abs(v[0])<=75);
 }
}
