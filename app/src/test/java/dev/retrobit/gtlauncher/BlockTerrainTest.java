package dev.retrobit.gtlauncher;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class BlockTerrainTest {
    private JSONArray stacks(int... levels) {
        JSONArray all=new JSONArray();
        for(int height:levels){JSONArray stack=new JSONArray();for(int i=0;i<height;i++)stack.put(i%6+1);all.put(stack);}
        return all;
    }
    private JSONArray surfaces(JSONArray stacks) throws JSONException {
        JSONArray top=new JSONArray();
        for(int i=0;i<stacks.length();i++){JSONArray s=stacks.getJSONArray(i);top.put(s.length()==0?0:s.getInt(s.length()-1));}
        return top;
    }
    @Test public void stepsAndShortestRoutesAcrossStacks() throws JSONException {
        // Heights 1-2-3-5: cliff is too high, detour via 4-4 is possible.
        JSONArray stacks=stacks(0,0,0,0,0, 0,1,2,3,5, 0,0,0,4,4, 0,0,0,0,0, 0,0,0,0,0);
        BlockTerrain g=BlockTerrain.parse(5,surfaces(stacks),stacks);
        assertTrue(g.canStep(1,1,2,1));assertTrue(g.canStep(2,1,1,1));
        assertFalse(g.canStep(3,1,4,1));assertFalse(g.canStep(1,1,1,0));assertFalse(g.canStep(1,1,3,1));
        PortalTravel.Terrain t=new PortalTravel.Terrain() {
            public boolean canStand(int c,int r){return g.canStand(c,r);}
            public boolean canStep(int a,int b,int c,int d){return g.canStep(a,b,c,d);}
        };
        List<PortalTravel.Tile> route=PortalTravel.path(new PortalTravel.Tile(3,1),new PortalTravel.Tile(4,1),t,5);
        assertNotNull(route);assertEquals(4,route.size());assertEquals(new PortalTravel.Tile(4,1),route.get(3));
    }
    @Test public void validationFailsClosedAndCopiesArrays() throws JSONException {
        JSONArray stacks=stacks(1,2,0,1), top=surfaces(stacks);
        BlockTerrain g=BlockTerrain.parse(2,top,stacks);
        stacks.getJSONArray(1).put(1,6);top.put(1,6);
        assertEquals(2,g.height(1,0));assertEquals(2,g.top(1,0));assertEquals(1,g.block(1,0,0));
        try {BlockTerrain.parse(2,top,stacks.put(0,new JSONArray().put(1).put(2)));fail("top mismatch");}catch(JSONException expected){}
        JSONArray tall=new JSONArray();for(int i=0;i<33;i++)tall.put(1);
        try {BlockTerrain.parse(1,new JSONArray().put(1),new JSONArray().put(tall));fail("height limit");}catch(JSONException expected){}
        try {BlockTerrain.parse(1,new JSONArray().put(1),new JSONArray().put(new JSONArray().put(0)));fail("invalid block");}catch(JSONException expected){}
        try {BlockTerrain.parse(1,new JSONArray().put(1.5),null);fail("fractional tile");}catch(JSONException expected){}
        try {BlockTerrain.parse(2,new JSONArray().put(1),null);fail("incorrect size");}catch(JSONException expected){}
    }
    @Test public void bridgeFootprintAndRailingsAreDirectional() {
        for(String axis:new String[]{"E","S"})for(String material:new String[]{"wood","stone"}) {
            BridgeGeometry.Bridge b=new BridgeGeometry.Bridge(5,5,axis,material);
            java.util.List<BridgeGeometry.Bridge> group=java.util.Collections.singletonList(b);
            int dc=axis.equals("E")?1:0,dr=axis.equals("S")?1:0;
            assertEquals(0,BridgeGeometry.indexAt(b,5,5));assertEquals(2,BridgeGeometry.indexAt(b,5+dc*2,5+dr*2));
            assertTrue(BridgeGeometry.permitsStep(group,5-dc,5-dr,5,5));
            assertTrue(BridgeGeometry.permitsStep(group,5+dc,5+dr,5+dc*2,5+dr*2));
            assertTrue(BridgeGeometry.permitsStep(group,5+dc*2,5+dr*2,5+dc*3,5+dr*3));
            assertFalse(BridgeGeometry.permitsStep(group,5+dc,5+dr,5+dc+dr,5+dr+dc));
        }
    }
    @Test public void flatWorldCompatibility() throws JSONException {
        BlockTerrain g=BlockTerrain.parse(2,new JSONArray().put(1).put(3).put(0).put(6),null);
        assertEquals(1,g.height(0,0));assertEquals(0,g.height(0,1));assertEquals(3,g.top(1,0));
        assertTrue(g.canStep(0,0,1,0));assertFalse(g.canStep(0,0,0,1));assertFalse(g.canStand(-1,0));
    }
}
