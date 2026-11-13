package dev.retrobit.gtlauncher;

import org.junit.Test;
import org.json.JSONException;
import android.content.SharedPreferences;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class PortalImportTest {
    private String world(String objects) {
        return "{\"format\":\"gtlauncher-world\",\"version\":28,\"world\":{\"id\":\"home\",\"name\":\"Home\",\"description\":\"\",\"color\":\"#a8c795\",\"objects\":["+objects+"]}}";
    }
    private final String portal="{\"type\":\"fence\",\"id\":\"north\",\"column\":6,\"row\":0,\"edge\":\"N\",\"variant\":\"portal\",\"destinationWorldId\":\"farm\",\"destinationPortalId\":\"south\"}";
    @Test public void readPortalAndCharacter() throws JSONException {
        WorldStore.World w=WorldStore.parseExport(world("{\"type\":\"character\",\"column\":6,\"row\":2,\"scale\":1,\"facing\":\"N\"},"+portal));
        assertEquals(1,w.portals.size()); assertEquals("farm",w.portals.get(0).destinationWorldId);assertEquals("south",w.portals.get(0).destinationPortalId);assertTrue(w.showCharacter);
    }
    @Test public void rejectUnsupportedObjectAndHalfPair() throws JSONException {
        try { WorldStore.parseExport(world(portal+",{\"type\":\"road\",\"column\":1,\"row\":1,\"material\":\"stone\"}"));fail("unsupported road cannot be discarded"); } catch(JSONException expected) { }
        try { WorldStore.parseExport(world(portal.replace(",\"destinationPortalId\":\"south\"", "")));fail("incomplete pair"); } catch(JSONException expected) { }
        try { WorldStore.parseExport(world(portal+","+portal));fail("duplicate portals"); } catch(JSONException expected) { }
    }
    @Test public void preserveFenceAndWaterAcrossStorage() throws JSONException {
        String fence="{\"type\":\"fence\",\"id\":\"f1\",\"column\":0,\"row\":0,\"edge\":\"W\"}";
        String road="{\"type\":\"road\",\"column\":2,\"row\":2,\"material\":\"water\"}";
        WorldStore store=new WorldStore(prefs());
        WorldStore.World imported=WorldStore.parseExport(world(portal+","+fence+","+road));
        assertEquals(2,imported.surfaces.size());assertTrue(store.upsertAndSelect(imported));
        WorldStore.World restored=store.activeWorld();assertEquals(2,restored.surfaces.size());
        assertEquals("W",restored.surfaces.get(0).edge);assertEquals("water",restored.surfaces.get(1).material);
        try {store.upsertAndSelect(WorldStore.parseExport(world("").replace("\"version\":28","\"version\":4")));fail("legacy world must not erase objects");} catch(JSONException expected) {}
    }
    @Test public void preserveObjectsPresentInHome() throws JSONException {
        String house="{\"type\":\"house\",\"id\":\"legacy-house\",\"column\":2,\"row\":0,\"scale\":1.25,\"facing\":\"S\",\"visible\":true}";
        String bush="{\"type\":\"bush\",\"column\":5,\"row\":1,\"variant\":\"tiered\"}";
        String tree="{\"type\":\"tree\",\"column\":0,\"row\":0,\"species\":\"mango\"}";
        String mailbox="{\"type\":\"mailbox\",\"column\":2,\"row\":3}";
        String box="{\"type\":\"storage-box\",\"column\":5,\"row\":3,\"facing\":\"N\"}";
        WorldStore store=new WorldStore(prefs());WorldStore.World imported=WorldStore.parseExport(world(house+","+bush+","+tree+","+mailbox+","+box+","+portal));
        assertTrue(store.upsertAndSelect(imported));WorldStore.World restored=store.activeWorld();
        assertEquals("legacy-house",restored.houseId);assertEquals(1.25,restored.houseScale,0.001);assertEquals(4,restored.surfaces.size());assertEquals("N",restored.surfaces.get(3).material);
        assertEquals("mango",restored.surfaces.get(1).material);assertEquals("farm",restored.portals.get(0).destinationWorldId);
        try {store.upsertAndSelect(WorldStore.parseExport(world("").replace("\"version\":28","\"version\":26")));fail("legacy import cannot erase orientation");} catch(JSONException expected) {}
    }
    @Test public void wildGrassVersionAndPersistence() throws JSONException {
        String grass="{\"type\":\"bush\",\"column\":1,\"row\":2,\"variant\":\"wild-grass\"}";
        String input=world(grass).replace("\"version\":28","\"version\":29");
        WorldStore store=new WorldStore(prefs());assertTrue(store.upsertAndSelect(WorldStore.parseExport(input)));
        assertEquals("wild-grass",store.activeWorld().surfaces.get(0).material);
        try {WorldStore.parseExport(world(grass));fail("grass requires version 29");} catch(JSONException expected) {}
        try {store.upsertAndSelect(WorldStore.parseExport(world("")));fail("older import cannot erase grass");} catch(JSONException expected) {}
    }
    @Test public void tropicalPlantsPersistAndRespectTiles() throws JSONException {
        String banana="{\"type\":\"tree\",\"column\":8,\"row\":8,\"species\":\"banana\"}";
        String palm="{\"type\":\"tree\",\"column\":7,\"row\":8,\"species\":\"palm\"}";
        String flower="{\"type\":\"sunflower\",\"column\":6,\"row\":8,\"facing\":\"N\"}";
        String basicFlower=world(flower.replace(",\"facing\":\"N\"","")).replace("\"version\":28","\"version\":18");
        WorldStore basicStore=new WorldStore(prefs());assertTrue(basicStore.upsertAndSelect(WorldStore.parseExport(basicFlower)));assertTrue(basicStore.upsertAndSelect(WorldStore.parseExport(basicFlower)));
        String file=world(banana+","+palm+","+flower).replace("\"version\":28","\"version\":25");
        WorldStore store=new WorldStore(prefs());assertTrue(store.upsertAndSelect(WorldStore.parseExport(file)));
        assertEquals("banana",store.activeWorld().surfaces.get(0).material);assertEquals("palm",store.activeWorld().surfaces.get(1).material);assertEquals("N",store.activeWorld().surfaces.get(2).material);
        try {WorldStore.parseExport(world(banana+","+flower.replace("\"column\":6","\"column\":8")));fail("overlap");}catch(JSONException expected){}
        try {store.upsertAndSelect(WorldStore.parseExport(world("").replace("\"version\":28","\"version\":22")));fail("older import cannot erase plants");}catch(JSONException expected){}
    }
    private SharedPreferences prefs() {
        Map<String,String> data=new HashMap<>();
        return (SharedPreferences) Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{SharedPreferences.class},(proxy,method,args)-> {
            if(method.getName().equals("getString")) return data.getOrDefault((String)args[0],(String)args[1]);
            if(method.getName().equals("edit")) {
                Map<String,String> staged=new HashMap<>();
                return Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{SharedPreferences.Editor.class},(editor,op,params)-> {
                    if(op.getName().equals("putString")){staged.put((String)params[0],(String)params[1]);return editor;}
                    if(op.getName().equals("commit")){data.putAll(staged);return true;}
                    throw new UnsupportedOperationException(op.getName());
                });
            }
            throw new UnsupportedOperationException(method.getName());
        });
    }
    @Test public void persistAndTravelWithoutErasingPortalData() throws JSONException {
        String character="{\"type\":\"character\",\"column\":6,\"row\":0,\"scale\":1,\"facing\":\"N\"}";
        String south="{\"type\":\"fence\",\"id\":\"south\",\"column\":6,\"row\":8,\"edge\":\"S\",\"variant\":\"portal\",\"destinationWorldId\":\"home\",\"destinationPortalId\":\"north\"}";
        WorldStore store=new WorldStore(prefs());
        assertTrue(store.upsertAndSelect(WorldStore.parseExport(world(character+","+portal))));
        String farm=world(south).replace("\"id\":\"home\",\"name\":\"Home\"","\"id\":\"farm\",\"name\":\"Farm\"");
        assertTrue(store.upsertAndSelect(WorldStore.parseExport(farm)));
        assertTrue(store.select("home"));assertTrue(store.travel("home","north"));
        assertEquals("farm",store.activeId());WorldStore.World active=store.activeWorld();
        assertEquals(8,active.characterRow);assertEquals("N",active.characterFacing);assertEquals("home",active.portals.get(0).destinationWorldId);
        assertTrue(store.travel("farm","south")); assertEquals("home",store.activeId());
        try {store.upsertAndSelect(WorldStore.parseExport(world(character).replace("\"version\":28","\"version\":4")));fail("old import must not erase portals");} catch(JSONException expected) {}
    }

}
