package dev.retrobit.gtlauncher;
import org.junit.Test;
import static org.junit.Assert.*;
public class IslandProjectionTest {
 @Test public void tileAndPointerCoordinatesAgree(){
  for (float width : new float[]{320,640,960,2560}) {
   float sx=IslandProjection.scale(width,320);
   assertEquals(32f,28f*sx*width/320,0.0001f);
   for(float value:new float[]{0,17,160,300}) {
    float screen=(160+(value-160)*sx)*width/320;
    assertEquals(value,IslandProjection.toScene(screen,width,320,160),0.001f);
   }
  }
  for (float height : new float[]{200,400,600,1600}) {
   float sy=IslandProjection.scale(height,200);
   assertEquals(16f,14f*sy*height/200,0.0001f);
   for(float value:new float[]{0,77,182,200}) {
    float screen=(182+(value-182)*sy)*height/200;
    assertEquals(value,IslandProjection.toScene(screen,height,200,182),0.001f);
   }
  }
 }
}
