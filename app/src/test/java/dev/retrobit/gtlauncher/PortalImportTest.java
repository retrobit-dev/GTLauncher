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
