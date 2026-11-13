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
        try { WorldStore.parseExport(world(portal+",{\"type\":\"road\",\"column\":1,\"row\":1,\"material\":\"dirt\"}"));fail("unsupported road cannot be discarded"); } catch(JSONException expected) { }
        try { WorldStore.parseExport(world(portal.replace(",\"destinationPortalId\":\"south\"", "")));fail("incomplete pair"); } catch(JSONException expected) { }
        try { WorldStore.parseExport(world(portal+","+portal));fail("duplicate portals"); } catch(JSONException expected) { }
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
