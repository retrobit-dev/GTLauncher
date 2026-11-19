package dev.retrobit.gtlauncher;

import org.junit.Test;
import org.json.JSONException;
import org.json.JSONObject;
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
        try { WorldStore.parseExport(world(portal+",{\"type\":\"road\",\"column\":1,\"row\":1,\"material\":\"marble\"}"));fail("unsupported road cannot be discarded"); } catch(JSONException expected) { }
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
    @Test public void sellingBoxVersionAndPersistence() throws JSONException {
        String box="{\"type\":\"selling-box\",\"column\":1,\"row\":1,\"facing\":\"N\"}";
        WorldStore store=new WorldStore(prefs());String file=world(box).replace("\"version\":28","\"version\":30");
        assertTrue(store.upsertAndSelect(WorldStore.parseExport(file)));assertEquals("selling-box",store.activeWorld().surfaces.get(0).type);assertEquals("N",store.activeWorld().surfaces.get(0).material);
        try {WorldStore.parseExport(world(box));fail("selling box requires v30");}catch(JSONException expected){}
        try {store.upsertAndSelect(WorldStore.parseExport(world("")));fail("legacy must not erase selling box");}catch(JSONException expected){}
    }
    @Test public void opacityPersistsAndOldImportCannotErase() throws JSONException {
        String house="{\"type\":\"house\",\"id\":\"h1\",\"column\":0,\"row\":0,\"scale\":1,\"facing\":\"S\",\"visible\":true,\"opacity\":40}";
        String box="{\"type\":\"storage-box\",\"column\":5,\"row\":4,\"facing\":\"N\",\"opacity\":75}";
        String file=world(house+","+box).replace("\"version\":28","\"version\":31");
        WorldStore store=new WorldStore(prefs());assertTrue(store.upsertAndSelect(WorldStore.parseExport(file)));
        assertEquals(40,store.activeWorld().houseOpacity);assertEquals(75,store.activeWorld().surfaces.get(0).opacity);
        try {store.upsertAndSelect(WorldStore.parseExport(world("")));fail("legacy import erases opacity");} catch(JSONException expected) {}
        assertEquals(40,store.activeWorld().houseOpacity);
        try {WorldStore.parseExport(file.replace("\"opacity\":75","\"opacity\":73"));fail("invalid opacity");}catch(JSONException expected){}
        try {WorldStore.parseExport(file.replace("\"version\":31","\"version\":30"));fail("opacity requires v31");}catch(JSONException expected){}
        String faded=world(portal.replace("\"variant\":\"portal\"","\"opacity\":50,\"variant\":\"portal\"")).replace("\"version\":28","\"version\":31");
        WorldStore portalStore=new WorldStore(prefs());assertTrue(portalStore.upsertAndSelect(WorldStore.parseExport(faded)));
        assertEquals(50,portalStore.activeWorld().portals.get(0).opacity);
        try {portalStore.upsertAndSelect(WorldStore.parseExport(world(portal)));fail("old file cannot erase portal opacity");}catch(JSONException expected){}
    }
    @Test public void orchardTreesAndBushVariantsPersist() throws JSONException {
        for(String species:new String[]{"apple","orange","mango","pear","guava","starfruit"}) {
            String tree="{\"type\":\"tree\",\"column\":1,\"row\":1,\"species\":\""+species+"\"}";
            String file=world(tree).replace("\"version\":28","\"version\":14");
            WorldStore store=new WorldStore(prefs());assertTrue(store.upsertAndSelect(WorldStore.parseExport(file)));
            assertEquals(species,store.activeWorld().surfaces.get(0).material);
            try {WorldStore.parseExport(file.replace("\"column\":1","\"column\":8"));fail("orchard tree needs 2x2 tiles");}catch(JSONException expected){}
        }
        for(String variant:new String[]{"round","flower","berry","spiky","tiered","wild-grass"}) {
            String bush="{\"type\":\"bush\",\"column\":1,\"row\":1,\"variant\":\""+variant+"\"}";
            String file=world(bush).replace("\"version\":28","\"version\":29");
            WorldStore store=new WorldStore(prefs());assertTrue(store.upsertAndSelect(WorldStore.parseExport(file)));
            assertEquals(variant,store.activeWorld().surfaces.get(0).material);
        }
        String overlap="{\"type\":\"tree\",\"column\":1,\"row\":1,\"species\":\"apple\"},{\"type\":\"bush\",\"column\":2,\"row\":2,\"variant\":\"berry\"}";
        try {WorldStore.parseExport(world(overlap).replace("\"version\":28","\"version\":14"));fail("tree footprint overlaps bush");}catch(JSONException expected){}
        String legacy="{\"type\":\"bush\",\"column\":1,\"row\":1}";
        assertEquals("round",WorldStore.parseExport(world(legacy).replace("\"version\":28","\"version\":9")).surfaces.get(0).material);
        try {WorldStore.parseExport(world(legacy.replace("\"row\":1","\"row\":1,\"variant\":\"flower\"")).replace("\"version\":28","\"version\":9"));fail("v9 cannot contain variant");}catch(JSONException expected){}
    }
    @Test public void decorativeFlowersPersistAndShareTiles() throws JSONException {
        String[] names={"rose","tulip","lavender","daisy","marigold","bluebell","jasmine","ylang","frangipani","hibiscus","periwinkle"};
        for(String species:names) {
            String flower="{\"type\":\"flower\",\"column\":1,\"row\":1,\"species\":\""+species+"\"}";
            String bush="{\"type\":\"bush\",\"column\":1,\"row\":1,\"variant\":\"round\"}";
            String character="{\"type\":\"character\",\"column\":4,\"row\":4,\"scale\":1,\"facing\":\"S\"}";
            String file=world(flower+","+bush+","+character).replace("\"version\":28","\"version\":21");
            WorldStore store=new WorldStore(prefs());assertTrue(store.upsertAndSelect(WorldStore.parseExport(file)));
            assertEquals(species,store.activeWorld().surfaces.get(0).material);
            try {store.upsertAndSelect(WorldStore.parseExport(world("").replace("\"version\":28","\"version\":20")));fail("older import loses flowers");}catch(JSONException expected){}
        }
        String jasmine="{\"type\":\"flower\",\"column\":1,\"row\":1,\"species\":\"jasmine\"}";
        try {WorldStore.parseExport(world(jasmine).replace("\"version\":28","\"version\":20"));fail("v20 lacks jasmine");}catch(JSONException expected){}
        try {WorldStore.parseExport(world(jasmine+","+jasmine).replace("\"version\":28","\"version\":21"));fail("duplicate flower");}catch(JSONException expected){}
        String water="{\"type\":\"road\",\"column\":1,\"row\":1,\"material\":\"water\"}";
        try {WorldStore.parseExport(world(jasmine+","+water).replace("\"version\":28","\"version\":21"));fail("flower on water");}catch(JSONException expected){}
    }
    @Test public void fenceVariantsPersistAndOldImportsAreBlocked() throws JSONException {
        for(String variant:new String[]{"slat","stone","iron"}) {
            String fence="{\"type\":\"fence\",\"id\":\"a\",\"column\":1,\"row\":1,\"edge\":\"N\",\"variant\":\""+variant+"\",\"opacity\":50}";
            String file=world(fence).replace("\"version\":28","\"version\":31");WorldStore store=new WorldStore(prefs());
            assertTrue(store.upsertAndSelect(WorldStore.parseExport(file)));assertEquals(variant,store.activeWorld().surfaces.get(0).material);assertEquals(50,store.activeWorld().surfaces.get(0).opacity);
            try {store.upsertAndSelect(WorldStore.parseExport(world("").replace("\"version\":28","\"version\":7")));fail("older import erases variant");}catch(JSONException expected){}
            try {WorldStore.parseExport(file.replace(variant,"unknown"));fail("unknown variant");}catch(JSONException expected){}
        }
    }
    @Test public void roadMaterialsDirectionsAndLegacyProtection() throws JSONException {
        for(String material:new String[]{"dirt","stone","gravel","paving","asphalt"})for(String facing:new String[]{"N","E","S","W"}) {
            String road="{\"type\":\"road\",\"column\":1,\"row\":1,\"material\":\""+material+"\",\"facing\":\""+facing+"\"}";
            String file=world(road).replace("\"version\":28","\"version\":19");
            WorldStore store=new WorldStore(prefs());assertTrue(store.upsertAndSelect(WorldStore.parseExport(file)));
            assertEquals(material,store.activeWorld().surfaces.get(0).material);assertEquals(facing,store.activeWorld().surfaces.get(0).edge);
            try {store.upsertAndSelect(WorldStore.parseExport(world("").replace("\"version\":28","\"version\":8")));fail("legacy export cannot erase road direction");}catch(JSONException expected){}
            try {WorldStore.parseExport(file.replace("\"version\":19","\"version\":8"));fail("old version cannot have facing");}catch(JSONException expected){}
        }
    }
    @Test public void wideHousePersistsAndValidatesFootprint() throws JSONException {
        String wide="{\"type\":\"house\",\"column\":1,\"row\":1,\"scale\":1,\"facing\":\"N\",\"visible\":true,\"design\":\"wide\"}";
        String file=world(wide).replace("\"version\":28","\"version\":15");WorldStore store=new WorldStore(prefs());
        assertTrue(store.upsertAndSelect(WorldStore.parseExport(file)));assertEquals("wide",store.activeWorld().houseDesign);assertEquals("N",store.activeWorld().houseFacing);
        try {store.upsertAndSelect(WorldStore.parseExport(world("").replace("\"version\":28","\"version\":14")));fail("legacy erases design");}catch(JSONException expected){}
        try {WorldStore.parseExport(file.replace("\"column\":1","\"column\":6"));fail("wide house out of bounds");}catch(JSONException expected){}
        String tree="{\"type\":\"tree\",\"column\":4,\"row\":1,\"species\":\"mango\"}";
        try {WorldStore.parseExport(world(wide+","+tree).replace("\"version\":28","\"version\":15"));fail("tree overlaps wide wing");}catch(JSONException expected){}
        try {WorldStore.parseExport(file.replace("\"version\":15","\"version\":14"));fail("old version cannot contain wide design");}catch(JSONException expected){}
    }
    @Test public void multipleHousesSurviveMovementAndRejectCollisions() throws JSONException {
        String first="{\"type\":\"house\",\"id\":\"first\",\"column\":0,\"row\":0,\"scale\":1,\"facing\":\"S\",\"visible\":true,\"opacity\":50}";
        String second="{\"type\":\"house\",\"id\":\"second\",\"column\":5,\"row\":0,\"scale\":1,\"facing\":\"N\",\"visible\":true,\"design\":\"wide\"}";
        String character="{\"type\":\"character\",\"column\":4,\"row\":4,\"scale\":1,\"facing\":\"S\"}";
        String file=world(first+","+second+","+character).replace("\"version\":28","\"version\":31");WorldStore store=new WorldStore(prefs());
        assertTrue(store.upsertAndSelect(WorldStore.parseExport(file)));assertEquals(2,store.activeWorld().houses.size());
        assertEquals("first",store.activeWorld().houses.get(0).id);assertEquals(50,store.activeWorld().houses.get(0).opacity);assertEquals("wide",store.activeWorld().houses.get(1).design);
        assertTrue(store.moveCharacter("home",4,5,"S"));assertEquals(2,store.activeWorld().houses.size());
        try {store.upsertAndSelect(WorldStore.parseExport(world("").replace("\"version\":28","\"version\":15")));fail("legacy erases houses");}catch(JSONException expected){}
        try {WorldStore.parseExport(file.replace("\"id\":\"second\"","\"id\":\"first\""));fail("duplicate IDs");}catch(JSONException expected){}
        try {WorldStore.parseExport(file.replace("\"column\":5","\"column\":2"));fail("overlapping houses");}catch(JSONException expected){}
        String tree="{\"type\":\"tree\",\"column\":7,\"row\":1,\"species\":\"banana\"}";
        try {WorldStore.parseExport(world(first+","+second+","+tree).replace("\"version\":28","\"version\":31"));fail("second house collision");}catch(JSONException expected){}
    }
    @Test public void inventoryTransfersAndProtectsBoxImport() throws JSONException {
        SharedPreferences inventoryPrefs=prefs();inventoryPrefs.edit().putString("items_v1","[{\"name\":\"Apel\",\"quantity\":3}]").commit();
        InventoryStore inventory=new InventoryStore(inventoryPrefs);String key=InventoryStore.boxKey("home",1,1);
        assertTrue(inventory.transfer(key,"apel",2,true));assertEquals(1,inventory.bag().getJSONObject(0).getInt("quantity"));assertEquals(2,inventory.box(key).getJSONObject(0).getInt("quantity"));
        String before=inventoryPrefs.getString("items_v1",null);try{inventory.transfer(key,"Apel",3,false);fail("cannot overdraw");}catch(JSONException expected){}assertEquals(before,inventoryPrefs.getString("items_v1",null));
        WorldStore worlds=new WorldStore(prefs(),inventory);String box="{\"type\":\"storage-box\",\"column\":1,\"row\":1}";
        assertTrue(worlds.upsertAndSelect(WorldStore.parseExport(world(box))));
        try{worlds.upsertAndSelect(WorldStore.parseExport(world("")));fail("cannot remove filled box");}catch(JSONException expected){}
        assertEquals(1,worlds.activeWorld().surfaces.size());assertTrue(inventory.transfer(key,"APEL",2,false));assertEquals(3,inventory.bag().getJSONObject(0).getInt("quantity"));assertTrue(worlds.upsertAndSelect(WorldStore.parseExport(world(""))));
        inventoryPrefs.edit().putString("items_v1","{broken").commit();try{inventory.changeBag("Pir",1);fail("corrupt data");}catch(JSONException expected){}assertEquals("{broken",inventoryPrefs.getString("items_v1",null));
    }
    @Test public void harvestPersistsAtomicallyWithBoxAndBlocksRepeatedCycles() throws JSONException {
        SharedPreferences prefs=prefs();prefs.edit().putString("items_v1","[{\"name\":\"Apel\",\"quantity\":2}]").commit();InventoryStore inventory=new InventoryStore(prefs);
        String box=InventoryStore.boxKey("home",1,1),tree=InventoryStore.treeKey("home",2,2,"apple");
        assertTrue(inventory.transfer(box,"Apel",1,true));assertTrue(inventory.harvest(tree,"2026-01-01","Apel"));assertEquals(2,inventory.bag().getJSONObject(0).getInt("quantity"));assertEquals(1,inventory.box(box).getJSONObject(0).getInt("quantity"));
        String before=prefs.getString("items_v1",null);assertTrue(new InventoryStore(prefs).wasHarvested(tree,"2026-01-01"));
        try{inventory.harvest(tree,"2026-01-01","Apel");fail("duplicate");}catch(JSONException expected){}assertEquals(before,prefs.getString("items_v1",null));
        try{inventory.harvest(tree,"2025-12-31","Apel");fail("older cycle");}catch(JSONException expected){}
        try{inventory.harvest(tree,"2026-02-30","Apel");fail("invalid date");}catch(JSONException expected){}
        assertTrue(inventory.harvest(tree,"2026-03-01","Apel"));assertEquals(3,inventory.bag().getJSONObject(0).getInt("quantity"));assertEquals(1,inventory.box(box).length());
        assertTrue(inventory.transfer(box,"Apel",1,false));assertTrue(inventory.wasHarvested(tree,"2026-03-01"));
        SharedPreferences full=prefs();full.edit().putString("items_v1","[{\"name\":\"Apel\",\"quantity\":999999}]").commit();InventoryStore fullInventory=new InventoryStore(full);before=full.getString("items_v1",null);
        try{fullInventory.harvest(tree,"2026-01-01","Apel");fail("full stack");}catch(JSONException expected){}assertEquals(before,full.getString("items_v1",null));assertFalse(fullInventory.wasHarvested(tree,"2026-01-01"));
    }
    @Test public void portableWorldStoresMovesAndPreservesTerrain() throws Exception {
        org.json.JSONArray tiles=new org.json.JSONArray(); for(int i=0;i<625;i++)tiles.put(1);
        tiles.put(624,3);tiles.put(623,0);tiles.put(100,5);
        org.json.JSONObject house=new org.json.JSONObject().put("column",0).put("row",0).put("visible",true).put("scale",1).put("facing","S");
        org.json.JSONObject w=new org.json.JSONObject().put("id","home").put("name","Home").put("description","").put("color","#a8c795")
                .put("gridSize",25).put("tiles",tiles).put("house",house).put("showCharacter",true)
                .put("characterColumn",12).put("characterRow",12).put("characterScale",1).put("characterFacing","S");
        String json=new org.json.JSONObject().put("format","gtlauncher-preview-world").put("version",2).put("world",w).toString();
        WorldStore store=new WorldStore(prefs()); assertTrue(store.upsertAndSelect(WorldStore.parseExport(json)));
        assertEquals(25,store.activeWorld().gridSize()); assertFalse(store.activeWorld().tileExists(23,24));
        assertTrue(store.moveCharacter("home",24,24,"E"));
        WorldStore.World restored=store.activeWorld(); assertEquals(24,restored.characterColumn); assertEquals(3,restored.previewData.tile(24,24));
        assertEquals(5,restored.previewData.tile(0,4)); assertEquals(1,restored.houses.size());
        assertFalse(store.tillTile("home",24,24)); assertFalse(store.tillTile("home",23,24));
        assertTrue(store.tillTile("home",24,23)); assertEquals(6,store.activeWorld().previewData.tile(24,23));
        assertEquals(3,new org.json.JSONObject(store.activeWorld().previewData.toJson()).getInt("version"));
        assertFalse(store.tillTile("home",24,23)); assertFalse(store.tillTile("home",0,0));
        assertFalse(store.moveCharacter("home",23,24,"E")); assertFalse(store.moveCharacter("home",0,0,"E"));
        assertFalse(store.moveCharacter("home",25,24,"E"));
        try {store.upsertAndSelect(WorldStore.parseExport(world("")));fail("legacy replacement cannot drop terrain");} catch(JSONException expected) {}
        assertEquals(3,store.activeWorld().previewData.tile(24,24));
        w.put("unsupported",true); try {WorldStore.parseExport(new org.json.JSONObject().put("format","gtlauncher-preview-world").put("version",2).put("world",w).toString());fail("unknown data");} catch(JSONException expected) {}
    }

    @Test public void handActionsPreserveObjectsAndCommitPickupWithBag() throws Exception {
        String bush="{\"type\":\"bush\",\"column\":1,\"row\":1,\"variant\":\"tiered\"}";
        String character="{\"type\":\"character\",\"column\":2,\"row\":1,\"scale\":1,\"facing\":\"W\"}";
        String house="{\"type\":\"house\",\"column\":4,\"row\":4,\"scale\":1,\"facing\":\"S\",\"visible\":true}";
        SharedPreferences preferences=prefs();WorldStore store=new WorldStore(preferences);
        assertTrue(store.upsertAndSelect(WorldStore.parseExport(world(bush+","+character+","+house))));
        WorldStore.World old=store.activeWorld();WorldStore.SurfaceObject plant=old.surfaces.get(0);
        assertFalse(store.moveSelectedObject("home",null,plant,2,1));assertFalse(store.moveSelectedObject("home",null,plant,5,5));
        assertTrue(store.moveSelectedObject("home",null,plant,1,3));assertFalse(store.takeSelectedObject("home",plant));
        WorldStore.SurfaceObject moved=store.activeWorld().surfaces.get(0);assertEquals(1,moved.column);assertEquals(3,moved.row);assertEquals("tiered",moved.material);
        WorldStore.House h=store.activeWorld().houses.get(0);assertFalse(store.moveSelectedObject("home",h,null,0,0));assertTrue(store.moveSelectedObject("home",h,null,5,0));
        assertEquals(5,store.activeWorld().houses.get(0).column);assertFalse(store.moveSelectedObject("home",h,null,0,5));
        preferences.edit().putString("object_bag_v1","invalid").commit();String original=preferences.getString("worlds_v1",null);
        try{store.takeSelectedObject("home",moved);fail("invalid bag must not remove object");}catch(JSONException expected){}assertEquals(original,preferences.getString("worlds_v1",null));
        preferences.edit().putString("object_bag_v1","[]").commit();assertTrue(store.takeSelectedObject("home",moved));assertEquals(0,store.activeWorld().surfaces.size());
        assertEquals("tiered",store.objectBag().getJSONObject(0).getJSONObject("object").getString("variant"));assertEquals("home",store.objectBag().getJSONObject(0).getString("worldId"));
        assertFalse(store.takeSelectedObject("home",moved));assertEquals(1,store.objectBag().length());
    }
    @Test public void portableHouseMovePreservesTerrainAndRejectsCollision() throws Exception {
        org.json.JSONArray tiles=new org.json.JSONArray();for(int i=0;i<625;i++)tiles.put(1);tiles.put(20*25+20,3);
        org.json.JSONObject house=new org.json.JSONObject().put("column",0).put("row",0).put("visible",true).put("scale",1).put("facing","S");
        org.json.JSONObject w=new org.json.JSONObject().put("id","home").put("name","Home").put("description","").put("color","#a8c795").put("gridSize",25).put("tiles",tiles).put("house",house).put("showCharacter",true).put("characterColumn",12).put("characterRow",12).put("characterScale",1).put("characterFacing","S");
        WorldStore store=new WorldStore(prefs());assertTrue(store.upsertAndSelect(WorldStore.parseExport(new org.json.JSONObject().put("format","gtlauncher-preview-world").put("version",2).put("world",w).toString())));
        WorldStore.House old=store.activeWorld().houses.get(0);assertFalse(store.moveSelectedObject("home",old,null,11,11));assertFalse(store.moveSelectedObject("home",old,null,19,19));
        assertTrue(store.moveSelectedObject("home",old,null,15,15));assertEquals(15,store.activeWorld().houses.get(0).column);assertEquals(3,store.activeWorld().previewData.tile(20,20));assertFalse(store.moveSelectedObject("home",old,null,6,6));
    }
    private String portableWorld(int version, int firstTile) throws JSONException {
        org.json.JSONArray tiles=new org.json.JSONArray();for(int i=0;i<400;i++) tiles.put(i==0?firstTile:1);
        org.json.JSONObject w=new org.json.JSONObject().put("id","home").put("name","Home").put("description","")
                .put("color","#a8c795").put("gridSize",20).put("tiles",tiles).put("house",org.json.JSONObject.NULL)
                .put("showCharacter",true).put("characterColumn",5).put("characterRow",5).put("characterScale",1).put("characterFacing","S");
        return new org.json.JSONObject().put("format","gtlauncher-preview-world").put("version",version).put("world",w).toString();
    }
    @Test public void differentSchemasNeverOverwriteAndPreservePreferences() throws JSONException {
        String bush="{\"type\":\"bush\",\"column\":1,\"row\":1,\"variant\":\"tiered\"}";
        SharedPreferences p=prefs();WorldStore store=new WorldStore(p);
        assertTrue(store.upsertAndSelect(WorldStore.parseExport(world(bush))));
        String before=p.getString("worlds_v1",null),active=store.activeId();
        try {store.upsertAndSelect(WorldStore.parseExport(portableWorld(2,1)));fail("portable format must not erase legacy objects");} catch(JSONException expected) {}
        assertEquals(before,p.getString("worlds_v1",null));assertEquals(active,store.activeId());assertEquals(1,store.activeWorld().surfaces.size());
        p=prefs();store=new WorldStore(p);assertTrue(store.upsertAndSelect(WorldStore.parseExport(portableWorld(2,1))));
        before=p.getString("worlds_v1",null);active=store.activeId();
        try {store.upsertAndSelect(WorldStore.parseExport(world(bush)));fail("legacy format must not erase terrain");} catch(JSONException expected) {}
        assertEquals(before,p.getString("worlds_v1",null));assertEquals(active,store.activeId());assertNotNull(store.activeWorld().previewData);
    }
    @Test public void oldPortableVersionCannotEraseTilledTerrain() throws JSONException {
        SharedPreferences p=prefs();WorldStore store=new WorldStore(p);
        assertTrue(store.upsertAndSelect(WorldStore.parseExport(portableWorld(3,6))));
        String before=p.getString("worlds_v1",null);
        try {store.upsertAndSelect(WorldStore.parseExport(portableWorld(2,1)));fail("v2 must not erase tilled terrain");} catch(JSONException expected) {}
        assertEquals(before,p.getString("worlds_v1",null));assertEquals(6,store.activeWorld().previewData.tile(0,0));
        assertTrue(store.upsertAndSelect(WorldStore.parseExport(portableWorld(3,1))));
    }
    @Test public void newerPortableVersionsAreRejectedWithoutChangingStoredWorld() throws JSONException {
        SharedPreferences p=prefs();WorldStore store=new WorldStore(p);
        assertTrue(store.upsertAndSelect(WorldStore.parseExport(portableWorld(3,6))));
        String before=p.getString("worlds_v1",null),active=store.activeId();
        for(int version=4;version<=8;version++) {
            try {store.upsertAndSelect(WorldStore.parseExport(portableWorld(version,1)));fail("unsupported version");}
            catch(JSONException expected) {assertEquals("Unsupported world version",expected.getMessage());}
            assertEquals(before,p.getString("worlds_v1",null));assertEquals(active,store.activeId());
        }
    }
    private String coniferWorld() throws JSONException {
        org.json.JSONObject root=new org.json.JSONObject(portableWorld(3,1));root.put("version",8);
        root.getJSONObject("world").put("orangeTrees",new org.json.JSONArray()
                .put(new org.json.JSONObject().put("column",6).put("row",5).put("kind","conifer").put("size",1).put("facing","E"))
                .put(new org.json.JSONObject().put("column",10).put("row",10).put("kind","conifer")));
        return root.toString();
    }
    @Test public void oldLargeConiferFormatRemainsSupported() throws JSONException {
        org.json.JSONObject root=new org.json.JSONObject(coniferWorld());root.put("version",7);
        root.getJSONObject("world").getJSONArray("orangeTrees").getJSONObject(0).remove("size");
        WorldStore.World world=WorldStore.parseExport(root.toString());
        assertEquals(2,WorldStore.treeFootprint(world.surfaces.get(0)));assertEquals(7,world.previewData.version);
    }
    @Test public void flatConifersSurviveImportMovementAndTilling() throws JSONException {
        SharedPreferences p=prefs();WorldStore store=new WorldStore(p);
        assertTrue(store.upsertAndSelect(WorldStore.parseExport(coniferWorld())));
        WorldStore.World w=store.activeWorld();assertEquals(2,w.surfaces.size());
        assertEquals("conifer-small",w.surfaces.get(0).material);assertEquals("E",w.surfaces.get(0).edge);
        assertEquals(1,WorldStore.treeFootprint(w.surfaces.get(0)));assertEquals(2,WorldStore.treeFootprint(w.surfaces.get(1)));
        assertFalse(store.moveCharacter("home",6,5,"E"));assertFalse(store.moveCharacter("home",11,11,"E"));
        assertFalse(store.tillTile("home",6,5));assertTrue(store.tillTile("home",5,6));
        assertTrue(store.moveCharacter("home",6,6,"E"));w=store.activeWorld();
        assertEquals(6,w.previewData.tile(5,6));assertEquals(8,w.previewData.version);assertEquals(2,w.surfaces.size());
        org.json.JSONObject saved=new org.json.JSONObject(w.previewData.toJson());
        assertEquals(1,saved.getJSONObject("world").getJSONArray("orangeTrees").getJSONObject(0).getInt("size"));
        String before=p.getString("worlds_v1",null);
        try {store.upsertAndSelect(WorldStore.parseExport(portableWorld(3,1)));fail("old file cannot replace trees");}catch(JSONException expected){}
        assertEquals(before,p.getString("worlds_v1",null));
    }
    @Test public void unsupportedTreeFieldsOrTerrainAreNeverDropped() throws JSONException {
        org.json.JSONObject root=new org.json.JSONObject(coniferWorld());
        for(int scenario=0;scenario<6;scenario++) {
            org.json.JSONObject copy=new org.json.JSONObject(root.toString()), w=copy.getJSONObject("world"), t=w.getJSONArray("orangeTrees").getJSONObject(0);
            if(scenario==0)copy.put("version",7);
            if(scenario==1)t.put("kind","orange");
            if(scenario==2)t.put("extra",true);
            if(scenario==3)w.put("blockStacks",new org.json.JSONArray());
            if(scenario==4){t.put("column",5);t.put("row",5);}
            if(scenario==5){t.put("column",10);t.put("row",10);}
            try {WorldStore.parseExport(copy.toString());fail("must reject unsupported, invalid or colliding data");}catch(JSONException expected){}
        }
    }
    @Test public void flatBridgesArePersistedAndProtectOldImports() throws JSONException {
        SharedPreferences p=prefs();WorldStore store=new WorldStore(p);JSONObject source=new JSONObject(portableWorld(3,1));source.getJSONObject("world").getJSONArray("tiles").put(10*20+11,3);
        assertTrue(store.upsertAndSelect(WorldStore.parseExport(source.toString())));assertTrue(store.placePreviewBridge("home",10,10,"E","wood"));
        WorldStore.World saved=store.activeWorld();assertEquals(15,saved.previewData.version);assertEquals(1,saved.previewData.bridges.size());assertTrue(saved.previewData.canStep(10,10,11,10));assertFalse(saved.previewData.canStep(11,10,11,9));
        assertTrue(saved.previewData.onBridge(11,10));assertFalse(store.placePreviewBridge("home",10,10,"E","stone"));
        BridgeGeometry.Bridge bridge=saved.previewData.bridges.get(0);assertFalse(store.editPreviewBridge("home",bridge,-1,10,false));assertTrue(store.editPreviewBridge("home",bridge,12,10,false));saved=store.activeWorld();assertEquals(12,saved.previewData.bridges.get(0).column);assertFalse(store.editPreviewBridge("home",bridge,0,0,true));
        assertTrue(store.editPreviewBridge("home",saved.previewData.bridges.get(0),10,10,false));saved=store.activeWorld();String before=p.getString("worlds_v1",null);
        try{store.upsertAndSelect(WorldStore.parseExport(portableWorld(3,1)));fail("older import would discard bridges");}catch(JSONException expected){}assertEquals(before,p.getString("worlds_v1",null));
        JSONObject root=new JSONObject(saved.previewData.toJson());root.put("version",14);try{WorldStore.parseExport(root.toString());fail("old version cannot contain bridges");}catch(JSONException expected){}
        assertTrue(store.editPreviewBridge("home",saved.previewData.bridges.get(0),0,0,true));assertEquals(0,store.activeWorld().previewData.bridges.size());
    }
    @Test public void longerBridgesRoundTripAndCannotBeDowngraded() throws JSONException {
        WorldStore store=new WorldStore(prefs());JSONObject source=new JSONObject(portableWorld(3,1));org.json.JSONArray tiles=source.getJSONObject("world").getJSONArray("tiles");tiles.put(5*20+6,3);tiles.put(5*20+7,3);for(int r=10;r<=12;r++)tiles.put(r*20+9,3);store.upsertAndSelect(WorldStore.parseExport(source.toString()));
        assertTrue(store.placePreviewBridge("home",5,5,"E","wood",4));assertTrue(store.placePreviewBridge("home",9,9,"S","stone",5));
        WorldStore.World saved=store.activeWorld();assertEquals(16,saved.previewData.version);assertEquals(4,saved.previewData.bridges.get(0).length);assertEquals(5,saved.previewData.bridges.get(1).length);assertEquals(3,saved.previewData.tile(6,5));assertEquals(3,saved.previewData.tile(9,11));assertTrue(saved.previewData.onBridge(9,11));
        assertTrue(saved.previewData.canStep(9,12,9,13));assertFalse(saved.previewData.canStep(9,12,10,12));
        assertTrue(store.placePreviewBridge("home",2,2,"E","stone",3));assertEquals(16,store.activeWorld().previewData.version);
        JSONObject old=new JSONObject(saved.previewData.toJson());old.put("version",15);try{WorldStore.parseExport(old.toString());fail("v15 cannot contain length");}catch(JSONException expected){}
        try{store.upsertAndSelect(WorldStore.parseExport(portableWorld(3,1)));fail("cannot downgrade");}catch(JSONException expected){}
    }
    @Test public void tallStoneWallRoundTripAndDowngradeGuard() throws JSONException {
        WorldStore store=new WorldStore(prefs());store.upsertAndSelect(WorldStore.parseExport(portableWorld(3,1)));
        assertTrue(store.placePreviewFence("home",5,5,"S","stone-tall"));WorldStore.World saved=store.activeWorld();assertEquals(17,saved.previewData.version);assertEquals("preview-stone-tall",saved.surfaces.get(0).material);
        assertTrue(FenceEdges.blocks(5,5,"S",5,5,5,6));
        java.util.List<FenceMesh.Face> mesh=FenceMesh.faces(0,0,14,7,true,true,null);float min=0;for(FenceMesh.Face f:mesh)for(int i=1;i<f.points.length;i+=2)min=Math.min(min,f.points[i]);assertTrue(Math.abs(min+127.4f)<1f);
        JSONObject old=new JSONObject(saved.previewData.toJson());old.put("version",16);try{WorldStore.parseExport(old.toString());fail("v16 cannot represent tall walls");}catch(JSONException expected){}
        JSONObject older=new JSONObject(saved.previewData.toJson());older.put("version",16);older.getJSONObject("world").getJSONArray("fences").getJSONObject(0).put("material","stone");try{store.upsertAndSelect(WorldStore.parseExport(older.toString()));fail("cannot downgrade tall walls");}catch(JSONException expected){}
        assertEquals("preview-stone-tall",store.activeWorld().surfaces.get(0).material);
    }
    @Test public void grassToppedBlocksRoundTripAndRejectOldImports() throws JSONException {
        SharedPreferences p=prefs();WorldStore store=new WorldStore(p);
        org.json.JSONObject root=new org.json.JSONObject(portableWorld(14,7));org.json.JSONArray tiles=root.getJSONObject("world").getJSONArray("tiles");tiles.put(1,8);tiles.put(2,9);tiles.put(3,10);
        assertTrue(store.upsertAndSelect(WorldStore.parseExport(root.toString())));for(int i=0;i<4;i++)assertEquals(7+i,store.activeWorld().previewData.tile(i,0));
        assertTrue(store.placePreviewFence("home",8,8,"S","wood"));assertEquals(14,store.activeWorld().previewData.version);
        String before=p.getString("worlds_v1",null);try{store.upsertAndSelect(WorldStore.parseExport(portableWorld(3,1)));fail("old format cannot replace new terrain");}catch(JSONException expected){}assertEquals(before,p.getString("worlds_v1",null));
        root.put("version",13);try{WorldStore.parseExport(root.toString());fail("new tiles require v14");}catch(JSONException expected){}
        root.put("version",14);root.getJSONObject("world").put("blockStacks",new org.json.JSONArray());try{WorldStore.parseExport(root.toString());fail("Android does not yet support stacks");}catch(JSONException expected){}root.getJSONObject("world").remove("blockStacks");
        tiles.put(0,11);try{WorldStore.parseExport(root.toString());fail("unknown tile");}catch(JSONException expected){}
    }
    @Test public void previewFencePlacementImportAndDeletionAreLossless() throws JSONException {
        SharedPreferences p=prefs();WorldStore store=new WorldStore(p);
        assertTrue(store.upsertAndSelect(WorldStore.parseExport(portableWorld(3,1))));
        assertTrue(store.placePreviewFence("home",6,5,"E","wood"));
        assertFalse(store.placePreviewFence("home",7,5,"W","stone"));
        assertTrue(store.placePreviewFence("home",7,5,"E","stone"));
        WorldStore.World w=store.activeWorld();assertEquals(13,w.previewData.version);assertEquals(2,w.surfaces.size());
        assertEquals("preview-wood",w.surfaces.get(0).material);
        assertTrue(store.moveCharacter("home",6,5,"E"));
        w=WorldStore.parseExport(store.activeWorld().previewData.toJson());assertEquals(2,w.surfaces.size());assertEquals(6,w.characterColumn);
        String saved=p.getString("worlds_v1",null);
        try{store.upsertAndSelect(WorldStore.parseExport(portableWorld(3,1)));fail("old format would discard fences");}catch(JSONException expected){}
        assertEquals(saved,p.getString("worlds_v1",null));
        assertTrue(store.deleteSelectedPlant("home",w.surfaces.get(0)));assertEquals(1,store.activeWorld().surfaces.size());
        assertTrue(store.deleteSelectedPlant("home",store.activeWorld().surfaces.get(0)));assertEquals(0,store.activeWorld().surfaces.size());
    }
    @Test public void previewGroupEditsAreAtomicAndPreserveObjects() throws JSONException {
        SharedPreferences p=prefs();WorldStore store=new WorldStore(p);
        assertTrue(store.upsertAndSelect(WorldStore.parseExport(portableWorld(3,1))));
        assertTrue(store.placeWildGrass("home",10,10));assertTrue(store.placePreviewFence("home",11,10,"S","wood"));
        WorldStore.World before=store.activeWorld();java.util.List<WorldStore.SurfaceObject> selected=new java.util.ArrayList<>(before.surfaces);
        String saved=p.getString("worlds_v1",null);assertFalse(store.editPreviewObjectGroup("home",selected,100,0,false));assertEquals(saved,p.getString("worlds_v1",null));
        assertTrue(store.editPreviewObjectGroup("home",selected,1,0,false));WorldStore.World moved=store.activeWorld();assertEquals(11,moved.surfaces.get(0).column);assertEquals(12,moved.surfaces.get(1).column);
        assertFalse(store.editPreviewObjectGroup("home",selected,0,0,true));assertTrue(store.editPreviewObjectGroup("home",moved.surfaces,0,0,true));assertEquals(0,store.activeWorld().surfaces.size());
    }
    @Test public void portableFenceSolidsHaveDepthAndAccurateHitGeometry() {
        for(String edge:new String[]{"N","E","S","W"}) {
            float[] p=FenceMesh.endpoints(100,100,28,14,edge);
            java.util.List<FenceMesh.Face> wood=FenceMesh.faces(p[0],p[1],p[2],p[3],false);
            java.util.List<FenceMesh.Face> stone=FenceMesh.faces(p[0],p[1],p[2],p[3],true);
            assertEquals(14,wood.size());assertEquals(28,stone.size());
            FenceMesh.Face f=stone.get(1);float x=0,y=0;for(int i=0;i<8;i+=2){x+=f.points[i]/4;y+=f.points[i+1]/4;}
            assertTrue(FenceMesh.contains(stone,x,y));assertFalse(FenceMesh.contains(stone,-1000,-1000));
            float woodTop=Float.MAX_VALUE,stoneTop=Float.MAX_VALUE;
            for(FenceMesh.Face face:wood)for(int i=1;i<face.points.length;i+=2)woodTop=Math.min(woodTop,face.points[i]);
            for(FenceMesh.Face face:stone)for(int i=1;i<face.points.length;i+=2)stoneTop=Math.min(stoneTop,face.points[i]);
            assertTrue(stoneTop>woodTop);
        }
    }
    @Test public void fenceShadowStaysBoundedAndFollowsLight() {
        float[] right=FenceShadow.offset(0,0,16,8,-500,200,175),left=FenceShadow.offset(0,0,16,8,500,200,175);
        assertTrue(right[0]>0);assertTrue(left[0]<0);
        assertTrue(Math.abs(right[0])<=4.8f);assertTrue(Math.abs(right[1])+8<=25);
    }
    @Test public void fenceBoundaryBlocksBothDirectionsOnly() {
        assertTrue(FenceEdges.blocks(4,5,"E",4,5,5,5));
        assertTrue(FenceEdges.blocks(5,5,"W",5,5,4,5));
        assertTrue(FenceEdges.blocks(4,6,"N",4,5,4,6));
        assertFalse(FenceEdges.blocks(4,5,"E",4,5,4,6));
        assertFalse(FenceEdges.blocks(4,5,"E",4,5,5,6));
        assertFalse(FenceEdges.blocks(4,5,"?",4,5,5,5));
    }
    @Test public void placeAndImportWildGrassWithoutLosingWorld() throws JSONException {
        SharedPreferences p=prefs();WorldStore store=new WorldStore(p);
        assertTrue(store.upsertAndSelect(WorldStore.parseExport(portableWorld(3,1))));
        String before=p.getString("worlds_v1",null);
        assertFalse(store.placeWildGrass("home",5,5));assertFalse(store.placeWildGrass("home",20,0));
        assertEquals(before,p.getString("worlds_v1",null));
        assertTrue(store.placeWildGrass("home",6,5));WorldStore.World w=store.activeWorld();
        assertEquals(9,w.previewData.version);assertEquals("wild-grass",w.surfaces.get(0).material);assertEquals("bush",w.surfaces.get(0).type);
        assertFalse(WorldStore.portableSmall(w.surfaces.get(0)));assertTrue(store.moveCharacter("home",6,5,"E"));
        assertEquals(6,WorldStore.parseExport(store.activeWorld().previewData.toJson()).characterColumn);assertTrue(store.moveCharacter("home",5,5,"W"));
        assertFalse(store.placeWildGrass("home",6,5));assertFalse(store.tillTile("home",6,5));
        assertFalse(store.moveSelectedObject("home",null,w.surfaces.get(0),5,5));
        assertTrue(store.moveSelectedObject("home",null,w.surfaces.get(0),7,5));
        assertFalse(store.moveSelectedObject("home",null,w.surfaces.get(0),8,5));
        assertEquals(7,store.activeWorld().surfaces.get(0).column);
        assertTrue(store.tillTile("home",5,6));assertEquals(9,store.activeWorld().previewData.version);
        String json=store.activeWorld().previewData.toJson();
        assertEquals("wild-grass",WorldStore.parseExport(json).surfaces.get(0).material);
        String saved=p.getString("worlds_v1",null);
        try {store.upsertAndSelect(WorldStore.parseExport(portableWorld(3,1)));fail("older format erases grass");}catch(JSONException expected){}
        assertEquals(saved,p.getString("worlds_v1",null));
        org.json.JSONObject malformed=new org.json.JSONObject(json);malformed.put("version",8);
        try {WorldStore.parseExport(malformed.toString());fail("grass requires v9");}catch(JSONException expected){}
    }
    @Test public void mixedFlatConiferAndGrassIsLossless() throws JSONException {
        SharedPreferences p=prefs();WorldStore store=new WorldStore(p);
        assertTrue(store.upsertAndSelect(WorldStore.parseExport(coniferWorld())));
        assertTrue(store.placeWildGrass("home",7,5));
        WorldStore.World w=store.activeWorld();assertEquals(3,w.surfaces.size());assertEquals(9,w.previewData.version);
        assertEquals("wild-grass",w.surfaces.get(2).material);assertFalse(store.placeWildGrass("home",11,11));
        assertEquals(3,WorldStore.parseExport(w.previewData.toJson()).surfaces.size());
    }
    @Test public void deletePlantPreservesWorldAndRejectsStaleSelection() throws JSONException {
        SharedPreferences p=prefs();WorldStore store=new WorldStore(p);
        assertTrue(store.upsertAndSelect(WorldStore.parseExport(coniferWorld())));
        assertTrue(store.placeWildGrass("home",7,5));
        WorldStore.World before=store.activeWorld();WorldStore.SurfaceObject grass=before.surfaces.get(2);
        assertFalse(store.deleteSelectedPlant("other",grass));
        assertTrue(store.deleteSelectedPlant("home",grass));
        assertFalse(store.deleteSelectedPlant("home",grass));
        WorldStore.World after=store.activeWorld();assertEquals(2,after.surfaces.size());
        assertEquals(9,after.previewData.version);assertEquals(before.characterColumn,after.characterColumn);
        assertTrue(store.deleteSelectedPlant("home",after.surfaces.get(0)));
        assertEquals(1,store.activeWorld().surfaces.size());
        String bush="{\"type\":\"bush\",\"column\":1,\"row\":2,\"variant\":\"wild-grass\"}";
        WorldStore nativeStore=new WorldStore(prefs());assertTrue(nativeStore.upsertAndSelect(WorldStore.parseExport(world(bush).replace("\"version\":28","\"version\":29"))));
        assertTrue(nativeStore.deleteSelectedPlant("home",nativeStore.activeWorld().surfaces.get(0)));
        assertEquals(0,nativeStore.activeWorld().surfaces.size());
    }
    @Test public void bushVersionPlacementMoveAndDeletionAreLossless() throws JSONException {
        SharedPreferences p=prefs();WorldStore store=new WorldStore(p);
        assertTrue(store.upsertAndSelect(WorldStore.parseExport(portableWorld(3,1))));
        assertTrue(store.placePreviewPlant("home",6,5,"bush"));
        WorldStore.World w=store.activeWorld();assertEquals(10,w.previewData.version);
        assertTrue(WorldStore.isPreviewBush(w.surfaces.get(0)));assertFalse(WorldStore.portableSmall(w.surfaces.get(0)));
        assertFalse(store.placeWildGrass("home",6,5));assertTrue(store.placeWildGrass("home",8,5));
        w=store.activeWorld();assertEquals(10,w.previewData.version);assertEquals(2,w.surfaces.size());
        assertTrue(store.moveSelectedObject("home",null,w.surfaces.get(0),7,5));
        w=store.activeWorld();assertEquals(7,w.surfaces.get(0).column);
        org.json.JSONObject malformed=new org.json.JSONObject(w.previewData.toJson());malformed.put("version",9);
        try {WorldStore.parseExport(malformed.toString());fail("bush requires v10");}catch(JSONException expected){}
        String saved=p.getString("worlds_v1",null);
        try {store.upsertAndSelect(WorldStore.parseExport(portableWorld(3,1)));fail("old import erases bush");}catch(JSONException expected){}
        assertEquals(saved,p.getString("worlds_v1",null));
        assertTrue(store.deleteSelectedPlant("home",w.surfaces.get(0)));
        assertTrue(store.deleteSelectedPlant("home",store.activeWorld().surfaces.get(0)));
        assertEquals(0,store.activeWorld().surfaces.size());assertEquals(10,store.activeWorld().previewData.version);
    }
    @Test public void largeAndVariantBushesValidateFullFootprint() throws JSONException {
        SharedPreferences p=prefs();WorldStore store=new WorldStore(p);
        assertTrue(store.upsertAndSelect(WorldStore.parseExport(portableWorld(3,1))));
        assertTrue(store.placePreviewPlant("home",7,7,"bush",2,"flower"));
        WorldStore.World w=store.activeWorld();assertEquals(12,w.previewData.version);WorldStore.SurfaceObject bush=w.surfaces.get(0);
        assertEquals(2,WorldStore.surfaceFootprint(bush));assertEquals("preview-bush-flower-large",bush.material);
        assertTrue(store.moveCharacter("home",8,8,"S"));assertEquals(8,WorldStore.parseExport(store.activeWorld().previewData.toJson()).characterColumn);assertTrue(store.moveCharacter("home",5,5,"N"));assertFalse(store.placeWildGrass("home",8,8));
        assertFalse(store.placePreviewPlant("home",19,19,"bush",2,"berry"));
        assertFalse(store.placePreviewPlant("home",8,8,"bush",1,"berry"));
        assertTrue(store.placePreviewPlant("home",11,11,"bush",1,"berry"));
        assertFalse(store.moveSelectedObject("home",null,bush,10,10));
        assertTrue(store.moveSelectedObject("home",null,bush,13,13));
        w=store.activeWorld();assertEquals(2,w.surfaces.size());assertEquals(13,w.surfaces.get(0).column);
        org.json.JSONObject old=new org.json.JSONObject(w.previewData.toJson());old.put("version",10);
        try{WorldStore.parseExport(old.toString());fail("v10 cannot hold new variants");}catch(JSONException expected){}
        org.json.JSONObject legacy=new org.json.JSONObject(w.previewData.toJson());legacy.put("version",11);
        org.json.JSONArray plants=legacy.getJSONObject("world").getJSONArray("orangeTrees");
        for(int i=0;i<plants.length();i++){plants.getJSONObject(i).remove("plantId");plants.getJSONObject(i).remove("plantedAt");}
        String before=p.getString("worlds_v1",null);
        try{store.upsertAndSelect(WorldStore.parseExport(legacy.toString()));fail("v11 cannot replace identified plants");}catch(JSONException expected){}
        assertEquals(before,p.getString("worlds_v1",null));
        org.json.JSONObject duplicate=new org.json.JSONObject(w.previewData.toJson());org.json.JSONArray duplicatePlants=duplicate.getJSONObject("world").getJSONArray("orangeTrees");
        duplicatePlants.getJSONObject(1).put("plantId",duplicatePlants.getJSONObject(0).getString("plantId"));
        try{WorldStore.parseExport(duplicate.toString());fail("duplicate identity");}catch(JSONException expected){}

        assertTrue(store.deleteSelectedPlant("home",w.surfaces.get(0)));assertEquals(1,store.activeWorld().surfaces.size());
        assertEquals("preview-bush-berry",store.activeWorld().surfaces.get(0).material);
    }
    @Test public void bushHarvestCyclePreservesExistingInventory() throws JSONException {
        WorldStore store=new WorldStore(prefs());assertTrue(store.upsertAndSelect(WorldStore.parseExport(portableWorld(3,1))));
        assertTrue(store.placePreviewPlant("home",6,5,"bush",1,"flower"));
        WorldStore.SurfaceObject plant=store.activeWorld().surfaces.get(0);long born=BushPhenology.born(plant);
        assertFalse(BushPhenology.inSeason(plant,born));assertFalse(BushPhenology.inSeason(plant,born+179999));
        assertTrue(BushPhenology.inSeason(plant,born+180000));String key=BushPhenology.key("home",plant),cycle=BushPhenology.cycle(plant,born+180000);
        SharedPreferences p=prefs();InventoryStore bag=new InventoryStore(p);assertTrue(bag.changeBag("Batu",4));
        assertTrue(bag.harvest(key,cycle,BushPhenology.item(plant)));assertTrue(bag.wasHarvested(key,cycle));
        assertEquals(2,bag.bag().length());String saved=p.getString("items_v1",null);
        try{bag.harvest(key,cycle,"Melati");fail("duplicate harvest");}catch(JSONException expected){}
        assertEquals(saved,p.getString("items_v1",null));
        assertTrue(bag.harvest(key,BushPhenology.cycle(plant,born+480000),"Melati"));
        assertEquals(4,bag.bag().getJSONObject(0).getInt("quantity"));
        String id=new org.json.JSONObject(plant.raw).getString("plantId");
        assertTrue(store.moveSelectedObject("home",null,plant,7,5));
        assertEquals(id,new org.json.JSONObject(store.activeWorld().surfaces.get(0).raw).getString("plantId"));
        assertEquals(key,BushPhenology.key("home",store.activeWorld().surfaces.get(0)));
    }
    @Test public void legacyFlowerAndBerryRemainAvailableWithoutChangingWorld() throws JSONException {
        String flower="{\"type\":\"bush\",\"column\":1,\"row\":2,\"variant\":\"flower\"}";
        String berry="{\"type\":\"bush\",\"column\":3,\"row\":4,\"variant\":\"berry\"}";
        WorldStore store=new WorldStore(prefs());assertTrue(store.upsertAndSelect(WorldStore.parseExport(world(flower+","+berry))));
        WorldStore.SurfaceObject a=store.activeWorld().surfaces.get(0),b=store.activeWorld().surfaces.get(1);
        assertTrue(BushPhenology.harvestable(a));assertTrue(BushPhenology.harvestable(b));
        assertEquals("Melati",BushPhenology.item(a));assertEquals("Murbei",BushPhenology.item(b));
        assertEquals("home:bush:legacy-flower",BushPhenology.key("home",a));
        assertEquals("home:bush:legacy-berry",BushPhenology.key("home",b));
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
