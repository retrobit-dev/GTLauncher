package dev.retrobit.gtlauncher;
import org.junit.Test;
import static org.junit.Assert.*;
public class HouseShadowTest {
 @Test public void lightDirectionAndBounds(){
  for(float s:new float[]{.5f,1f,1.5f})for(int a=0;a<360;a+=5)for(int e=0;e<=90;e+=5){float w=84*s,h=85*s;float[] p=HouseShadow.projection(w,h,a,e);assertTrue(Float.isFinite(p[0])&&Float.isFinite(p[1])&&p[2]>0&&p[2]<=1);assertTrue((w/2+Math.abs(p[0]))*p[2]<=w/2+.001);assertTrue(Math.abs(p[1])*p[2]<=h/2+.001);}
  float[] n=HouseShadow.projection(84,85,0,30),s=HouseShadow.projection(84,85,180,30);assertTrue(n[0]<0 && s[0]>0);assertTrue(n[1]>0 && s[1]<0);assertTrue(Math.abs(HouseShadow.projection(84,85,0,80)[0])<Math.abs(n[0]));
 }
}
