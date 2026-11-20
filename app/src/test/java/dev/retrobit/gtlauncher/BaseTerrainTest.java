package dev.retrobit.gtlauncher;
import org.junit.Test;
import static org.junit.Assert.*;
public class BaseTerrainTest {
 @Test public void everyBaseTileHasFourEarthBlocksUnderGrass() {
  assertEquals(50,BaseTerrain.SIZE);assertEquals(5,BaseTerrain.HEIGHT);
  for(int row=0;row<BaseTerrain.SIZE;row++)for(int column=0;column<BaseTerrain.SIZE;column++){
   for(int level=0;level<4;level++)assertEquals(2,BaseTerrain.block(column,row,level));
   assertEquals(1,BaseTerrain.block(column,row,4));
  }
  try{BaseTerrain.block(50,0,0);fail("outside base");}catch(IndexOutOfBoundsException expected){}
  try{BaseTerrain.block(0,0,5);fail("outside stack");}catch(IndexOutOfBoundsException expected){}
 }
}
