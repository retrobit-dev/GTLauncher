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
