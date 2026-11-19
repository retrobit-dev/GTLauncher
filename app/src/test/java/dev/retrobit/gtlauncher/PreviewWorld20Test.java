package dev.retrobit.gtlauncher;

import org.junit.Test;
import org.json.JSONArray;
import org.json.JSONObject;
import static org.junit.Assert.*;

public class PreviewWorld20Test {
    private JSONObject sample() throws Exception {
        JSONArray tiles=new JSONArray(); for(int i=0;i<400;i++) tiles.put(1);
        JSONObject world=new JSONObject().put("id","home").put("name","Home").put("description","")
                .put("color","#a8c795").put("gridSize",20).put("tiles",tiles)
                .put("showCharacter",true).put("characterColumn",9).put("characterRow",9)
                .put("characterScale",1).put("characterFacing","S")
                .put("house",new JSONObject().put("column",0).put("row",0).put("scale",1).put("facing","S").put("visible",true));
        return new JSONObject().put("format","gtlauncher-preview-world").put("version",2).put("world",world);
    }
    private void rejected(JSONObject o) throws Exception {
        try { PreviewWorld20.parse(o.toString()); fail("invalid or unsupported data must be rejected"); }
        catch (org.json.JSONException expected) { }
    }
    @Test public void rampsAreRejectedInsteadOfSilentlyDropped() throws Exception {
        JSONObject root=sample(),world=root.getJSONObject("world");
        world.put("ramps",new JSONArray().put(new JSONObject().put("column",10).put("row",10).put("direction","E").put("material",5)));
        root.put("version",21);rejected(root);
        root.put("version",20);rejected(root);
    }
    @Test public void houseTransparencyRequiresV18AndRoundTrips() throws Exception {
        JSONObject root=sample(),house=root.getJSONObject("world").getJSONObject("house");house.put("opacity",40);
        rejected(root);root.put("version",17);rejected(root);root.put("version",18);
        assertEquals(40,new JSONObject(PreviewWorld20.parse(root.toString()).toJson()).getJSONObject("world").getJSONObject("house").getInt("opacity"));
        house.put("opacity",41);rejected(root);house.put("opacity",95);rejected(root);house.put("opacity","40");rejected(root);
    }
    @Test public void objectOpacityRequiresV19AndIsValidated() throws Exception {
        JSONObject root=sample(),w=root.getJSONObject("world");root.put("version",19);
        JSONObject tree=new JSONObject().put("column",10).put("row",10).put("opacity",30);
        JSONObject fence=new JSONObject().put("column",4).put("row",4).put("edge","S").put("material","wood").put("opacity",55);
        JSONObject bridge=new JSONObject().put("column",14).put("row",14).put("axis","E").put("material","wood").put("opacity",75);
        w.put("orangeTrees",new JSONArray().put(tree)).put("fences",new JSONArray().put(fence)).put("bridges",new JSONArray().put(bridge));
        PreviewWorld20 p=PreviewWorld20.parse(root.toString());assertEquals(75,p.bridges.get(0).opacity);assertEquals(30,new JSONObject(p.toJson()).getJSONObject("world").getJSONArray("orangeTrees").getJSONObject(0).getInt("opacity"));
        root.put("version",18);rejected(root);root.put("version",19);
        tree.put("opacity",31);rejected(root);tree.put("opacity",30);fence.put("opacity",-5);rejected(root);fence.put("opacity",55);bridge.put("opacity","75");rejected(root);
    }
    @Test public void blockOpacityIsPerLevelValidatedAndVersioned() throws Exception {
        JSONObject root=stacked(20),w=root.getJSONObject("world");w.getJSONArray("blockStacks").getJSONArray(10*20+10).put(2);w.getJSONArray("tiles").put(10*20+10,2);
        JSONObject e=new JSONObject().put("column",10).put("row",10).put("level",1).put("opacity",45);JSONArray entries=new JSONArray().put(e);w.put("blockOpacity",entries);
        PreviewWorld20 p=PreviewWorld20.parse(root.toString());assertEquals(45,p.blockOpacity(10,10,1));assertEquals(0,p.blockOpacity(10,10,0));assertTrue(p.hasBlockOpacity());
        root.put("version",19);rejected(root);root.put("version",20);e.put("level",2);rejected(root);e.put("level",1);e.put("opacity",46);rejected(root);e.put("opacity",45);entries.put(new JSONObject(e.toString()));rejected(root);
    }
    @Test public void allTerrainAndHouseRoundTrip() throws Exception {
        JSONObject root=sample(), w=root.getJSONObject("world");
        w.getJSONArray("tiles").put(399,0).put(398,2).put(397,3).put(396,4).put(395,5);
        PreviewWorld20 parsed=PreviewWorld20.parse(root.toString());
        assertEquals("home",parsed.id); assertEquals(5,parsed.tile(15,19)); assertTrue(parsed.canStand(17,19));
        assertFalse(parsed.canStand(19,19)); assertTrue(parsed.canStand(18,19)); assertFalse(parsed.canStand(20,0));
        assertEquals(w.getJSONArray("tiles").toString(), new JSONObject(parsed.toJson()).getJSONObject("world").getJSONArray("tiles").toString());
        assertEquals(w.getJSONObject("house").toString(), new JSONObject(parsed.toJson()).getJSONObject("world").getJSONObject("house").toString());
        assertEquals(parsed.toJson(), PreviewWorld20.parse(parsed.toJson()).toJson());
    }
    @Test public void twentyFiveByTwentyFiveRoundTripAndBounds() throws Exception {
        JSONObject root=sample(), w=root.getJSONObject("world");
        JSONArray tiles=new JSONArray(); for(int i=0;i<625;i++) tiles.put(1);
        tiles.put(624,3); w.put("gridSize",25).put("tiles",tiles).put("characterColumn",24).put("characterRow",24);
        PreviewWorld20 parsed=PreviewWorld20.parse(root.toString());
        assertEquals(25,parsed.size); assertEquals(3,parsed.tile(24,24)); assertTrue(parsed.canStand(24,24));
        assertFalse(parsed.canStand(25,24)); assertEquals(parsed.toJson(),PreviewWorld20.parse(parsed.toJson()).toJson());
        w.put("gridSize",24); rejected(root); w.put("gridSize",25); w.getJSONObject("house").put("column",23); rejected(root);
    }
    @Test public void fiftyByFiftyRoundTrip() throws Exception {
        JSONObject root=sample(), w=root.getJSONObject("world");
        JSONArray tiles=new JSONArray(); for(int i=0;i<2500;i++) tiles.put(1);
        tiles.put(2499,3); w.put("gridSize",50).put("tiles",tiles).put("characterColumn",49).put("characterRow",49);
        PreviewWorld20 parsed=PreviewWorld20.parse(root.toString());
        assertEquals(50,parsed.size); assertEquals(3,parsed.tile(49,49)); assertFalse(parsed.canStand(50,49));
        assertEquals(parsed.toJson(),PreviewWorld20.parse(parsed.toJson()).toJson());
    }
    @Test public void hundredByHundredRoundTrip() throws Exception {
        JSONObject root=sample(), w=root.getJSONObject("world");
        JSONArray tiles=new JSONArray(); for(int i=0;i<10000;i++) tiles.put(1);
        tiles.put(9999,3); w.put("gridSize",100).put("tiles",tiles).put("characterColumn",99).put("characterRow",99);
        PreviewWorld20 parsed=PreviewWorld20.parse(root.toString());
        assertEquals(100,parsed.size); assertEquals(3,parsed.tile(99,99));
        assertFalse(parsed.canStand(100,99)); assertEquals(parsed.toJson(),PreviewWorld20.parse(parsed.toJson()).toJson());
    }
    @Test public void tilledTerrainRequiresVersionThree() throws Exception {
        JSONObject root=sample();root.getJSONObject("world").getJSONArray("tiles").put(200,6);
        rejected(root);root.put("version",3);
        PreviewWorld20 parsed=PreviewWorld20.parse(root.toString());assertEquals(6,parsed.tile(0,10));assertTrue(parsed.canStand(0,10));
        assertEquals(parsed.toJson(),PreviewWorld20.parse(parsed.toJson()).toJson());
    }
    private JSONObject stacked(int version) throws Exception {
        JSONObject root=sample();root.put("version",version);JSONArray all=new JSONArray();for(int i=0;i<400;i++)all.put(new JSONArray().put(1));root.getJSONObject("world").put("blockStacks",all);return root;
    }
    private void rejectedStack(JSONObject root) throws Exception {
        try {PreviewWorld20.parse(root.toString());fail("invalid stacked world");}catch(org.json.JSONException expected){}
    }
    @Test public void stackedValidationPreservesBlocksAndEnforcesCliffs() throws Exception {
        for(int version:new int[]{4,5,6,7,13,14,15,16,17}) {
            JSONObject root=stacked(version),world=root.getJSONObject("world");JSONArray stacks=world.getJSONArray("blockStacks");
            stacks.getJSONArray(10*20+10).put(2);world.getJSONArray("tiles").put(10*20+10,2);
            stacks.getJSONArray(10*20+11).put(2).put(4);world.getJSONArray("tiles").put(10*20+11,4);
            PreviewWorld20 parsed=PreviewWorld20.parse(root.toString());
            assertEquals(2,parsed.height(10,10));assertEquals(1,parsed.block(10,10,0));assertEquals(2,parsed.block(10,10,1));assertEquals(3,parsed.height(11,10));
            assertTrue(parsed.canStep(9,10,10,10));assertTrue(parsed.canStep(10,10,11,10));assertFalse(parsed.canStep(11,10,12,10));
            assertEquals(stacks.toString(),new JSONObject(parsed.toJson()).getJSONObject("world").getJSONArray("blockStacks").toString());
            assertEquals(parsed.toJson(),PreviewWorld20.parse(parsed.toJson()).toJson());
        }
    }
    @Test public void stackedVersionAndFootprintValidation() throws Exception {
        JSONObject root=stacked(4),w=root.getJSONObject("world");w.getJSONArray("blockStacks").getJSONArray(0).put(1);rejectedStack(root); // house must be level
        root=stacked(5);w=root.getJSONObject("world");w.put("orangeTrees",new JSONArray().put(new JSONObject().put("column",10).put("row",10)));
        w.getJSONArray("blockStacks").getJSONArray(10*20+10).put(1);rejectedStack(root); // tree footprint not level
        root=stacked(15);w=root.getJSONObject("world");w.put("bridges",new JSONArray().put(new JSONObject().put("column",10).put("row",10).put("axis","E").put("material","wood")));
        w.getJSONArray("blockStacks").getJSONArray(10*20+11).put(1);rejectedStack(root); // bridge footprint not level
        root=stacked(13);w=root.getJSONObject("world");w.getJSONArray("blockStacks").getJSONArray(10*20+10).put(7).put(1);rejectedStack(root); // hidden grass-top requires v14
        root.put("version",14);PreviewWorld20.parse(root.toString());
        root=stacked(4);w=root.getJSONObject("world");w.getJSONArray("blockStacks").getJSONArray(10*20+10).put(2);rejectedStack(root); // top mismatch
        root=stacked(4);root.getJSONObject("world").remove("blockStacks");rejectedStack(root);
        root=stacked(3);rejectedStack(root);
    }
    @Test public void largeStackedWorldRoundTripsMaximumHeight() throws Exception {
        JSONObject root=sample(),w=root.getJSONObject("world");root.put("version",14);JSONArray tiles=new JSONArray(),stacks=new JSONArray();
        for(int i=0;i<10000;i++){tiles.put(1);stacks.put(new JSONArray().put(1));}
        JSONArray tall=stacks.getJSONArray(90*100+90);for(int i=1;i<31;i++)tall.put(7);tall.put(10);tiles.put(90*100+90,10);
        w.put("gridSize",100).put("tiles",tiles).put("blockStacks",stacks);
        PreviewWorld20 parsed=PreviewWorld20.parse(root.toString());assertEquals(32,parsed.height(90,90));assertEquals(7,parsed.block(90,90,20));assertEquals(10,parsed.tile(90,90));assertFalse(parsed.canStep(89,90,90,90));
        assertEquals(parsed.toJson(),PreviewWorld20.parse(parsed.toJson()).toJson());
    }
    @Test public void rejectsUnknownOrMalformedData() throws Exception {
        JSONObject root=sample(); root.getJSONObject("world").put("objects",new JSONArray()); rejected(root);
        root=sample(); root.getJSONObject("world").getJSONObject("house").put("design","new"); rejected(root);
        root=sample(); root.getJSONObject("world").getJSONArray("tiles").put(0,6); rejected(root);
        root=sample(); root.put("version",1); root.getJSONObject("world").getJSONArray("tiles").put(399,2); rejected(root);
        root=sample(); root.getJSONObject("world").getJSONArray("tiles").put(0,0); rejected(root);
        root=sample(); root.getJSONObject("world").put("characterColumn",20); rejected(root);
        root=sample(); root.getJSONObject("world").getJSONArray("tiles").put(189,0); rejected(root);
    }
}
