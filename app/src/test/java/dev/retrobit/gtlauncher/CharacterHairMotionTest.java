package dev.retrobit.gtlauncher;
import org.junit.Test;
import static org.junit.Assert.*;
public class CharacterHairMotionTest {
    @Test public void subtleGaitMotionFadesAboveFace() {
        assertEquals(.2f,CharacterHairMotion.offset(0,1,0,1,true),.00001f);
        assertEquals(.32f,CharacterHairMotion.offset(0,1,1,1,true),.00001f);
        assertEquals(-CharacterHairMotion.offset(.04f,1,0,1,true),CharacterHairMotion.offset(.04f,-1,0,1,true),.00001f);
        assertTrue(CharacterHairMotion.offset(.08f,1,1,1,true)<CharacterHairMotion.offset(.02f,1,1,1,true));
        assertEquals(0,CharacterHairMotion.offset(.16f,1,1,1,true),0);
        assertEquals(0,CharacterHairMotion.offset(0,1,1,1,false),0);
        assertEquals(0,CharacterHairMotion.offset(0,0,1,1,true),0);
        assertEquals(0,CharacterHairMotion.offset(Float.NaN,1,1,1,true),0);
    }
}
