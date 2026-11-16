package dev.retrobit.gtlauncher;

import org.junit.Test;
import static org.junit.Assert.*;

public class GameplayTargetTest {
    @Test public void frontAndFacingCoverFourDirections() {
        assertEquals(new PortalTravel.Tile(4,3),GameplayTarget.front(4,4,"N"));
        assertEquals(new PortalTravel.Tile(5,4),GameplayTarget.front(4,4,"E"));
        assertEquals(new PortalTravel.Tile(4,5),GameplayTarget.front(4,4,"S"));
        assertEquals(new PortalTravel.Tile(3,4),GameplayTarget.front(4,4,"W"));
        assertNull(GameplayTarget.front(4,4,"?"));
        PortalTravel.Tile start=new PortalTravel.Tile(4,4);
        for(String facing:new String[]{"N","E","S","W"})assertEquals(facing,GameplayTarget.direction(start,GameplayTarget.front(4,4,facing)));
        assertNull(GameplayTarget.direction(start,start));
    }
}
