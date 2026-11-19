package dev.retrobit.gtlauncher;

import org.json.JSONException;
import org.json.JSONObject;
import java.time.Instant;

/** Accelerated game cycle: three minutes vegetative and two minutes harvestable. */
final class BushPhenology {
    static final long PERIOD=300_000L, READY=180_000L;
    static boolean harvestable(WorldStore.SurfaceObject o) {
        return o!=null && o.type.equals("bush") && ((WorldStore.isPreviewBush(o) && (o.material.startsWith("preview-bush-flower") || o.material.startsWith("preview-bush-berry"))) || o.material.equals("flower") || o.material.equals("berry"));
    }
    static String key(String worldId,WorldStore.SurfaceObject o) throws JSONException {
        JSONObject data=new JSONObject(o.raw);
        String variant=data.getString("variant");
        return worldId+":bush:"+(data.has("plantId")?data.getString("plantId"):"legacy-"+variant);
    }
    static long born(WorldStore.SurfaceObject o) throws JSONException {return new JSONObject(o.raw).optLong("plantedAt",0);}
    static boolean inSeason(WorldStore.SurfaceObject o,long now) throws JSONException {
        long start=born(o);return harvestable(o) && now>=start && (now-start)%PERIOD>=READY;
    }
    static String cycle(WorldStore.SurfaceObject o,long now) throws JSONException {
        long start=born(o);if(now<start)throw new JSONException("Plant time in future");
        return Instant.ofEpochMilli(start+Math.floorDiv(now-start,PERIOD)*PERIOD).truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString();
    }
    static String item(WorldStore.SurfaceObject o) {return o.material.equals("flower") || o.material.startsWith("preview-bush-flower")?"Melati":"Murbei";}
    private BushPhenology() {}
}
