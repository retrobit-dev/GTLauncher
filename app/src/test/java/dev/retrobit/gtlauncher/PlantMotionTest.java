package dev.retrobit.gtlauncher;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlantMotionTest {
 @Test public void allPlantTypesMoveAndCalmRespected() {
  for(String type:new String[]{"tree","bush","sunflower","flower"}) for(int direction=0;direction<360;direction+=45) {
   double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
   for(int t=0;t<8;t++){double n=PlantMotion.sway(true,10,direction,0,t,4,4,type,"banana");min=Math.min(min,n);max=Math.max(max,n);}
   assertTrue(max-min>.1);assertEquals(0,PlantMotion.sway(false,10,direction,0,2,4,4,type,"banana"),0);
   assertEquals(0,PlantMotion.sway(true,0,direction,0,2,4,4,type,"banana"),0);
  }
 }
}
