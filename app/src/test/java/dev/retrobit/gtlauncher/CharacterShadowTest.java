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
 @Test public void lowSunIsCappedAndFinite() {
  float[] v=CharacterShadow.offset(90,0,25);
  assertTrue(Float.isFinite(v[0])&&Float.isFinite(v[1]));
  assertTrue(Math.abs(v[0])<=75);
 }
}
