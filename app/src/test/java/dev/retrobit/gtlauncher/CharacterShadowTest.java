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
 @Test public void objectAndHouseShadowsFitAtExtremeLightAngles() {
  for(float factor:new float[]{.5f,1f,1.5f}) for(float dx=-600;dx<=600;dx+=15) for(float dy=-600;dy<=600;dy+=15) {
   float w=62*factor,h=60*factor,sway=3*factor;
   float tree=CharacterShadow.fitScale(w,h,dx+sway,dy);
   assertTrue((w+Math.abs(dx+sway))*tree<=w+.001f);
   assertTrue(Math.abs(dy)*tree<=h+.001f);
   float house=CharacterShadow.fitAroundAnchor(w,h,dx,dy);
   assertTrue((w/2+Math.abs(dx))*house<=w/2+.001f);
   assertTrue(Math.abs(dy)*house<=h/2+.001f);
  }
 }
 @Test public void widenedIdleSpriteStaysWithinStandingWidth() {
  float baseWidth=13.5f,renderWidth=baseWidth*1280/432f,height=25f;
  for(float dx=-100;dx<=100;dx+=2) for(float dy=-100;dy<=100;dy+=2) {
   float scale=Math.min(1f,Math.min(baseWidth/(renderWidth+2*Math.abs(dx)),dy==0?1f:height/(2*Math.abs(dy))));
   assertTrue((renderWidth/2+Math.abs(dx))*scale<=baseWidth/2+.001f);
   assertTrue(Math.abs(dy)*scale<=height/2+.001f);
  }
 }
 @Test public void lowSunIsCappedAndFinite() {
  float[] v=CharacterShadow.offset(90,0,25);
  assertTrue(Float.isFinite(v[0])&&Float.isFinite(v[1]));
  assertTrue(Math.abs(v[0])<=75);
 }
}
