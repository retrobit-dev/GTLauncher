package dev.retrobit.gtlauncher;
import org.junit.Test;
import static org.junit.Assert.*;
public class IslandProjectionTest {
 @Test public void tileAndPointerCoordinatesAgree(){
  assertEquals(32f,28f*IslandProjection.BASE_ZOOM*2,0.0001f);
  assertEquals(16f,28f*(float)Math.sin(Math.toRadians(30))*IslandProjection.BASE_ZOOM*2,0.0001f);
  for(float value:new float[]{0,17,160,300}){float screen=(160+(value-160)*IslandProjection.BASE_ZOOM)*3;assertEquals(value,IslandProjection.toScene(screen,960,320,160),0.0001f);}
  for(float value:new float[]{0,77,182,200}){float screen=(182+(value-182)*IslandProjection.BASE_ZOOM)*3;assertEquals(value,IslandProjection.toScene(screen,600,200,182),0.0001f);}
 }
}
