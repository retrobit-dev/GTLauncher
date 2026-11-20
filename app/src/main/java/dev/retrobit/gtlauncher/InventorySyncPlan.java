package dev.retrobit.gtlauncher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Pure conflict decision only. This class does not perform I/O or connect to Drive. */
final class InventorySyncPlan {
    enum Action { MATCH, UPLOAD, DOWNLOAD, INITIAL_CONFLICT, BOTH_CHANGED, REMOTE_MISSING }

    // Compares normalized, fully validated snapshots; JSON property order and
    // legacy array/v2 envelopes don't produce spurious conflicts. Array order
    // remains significant, making ambiguous differences conservative.
    private static Object structural(Object value) throws JSONException {
        if(value instanceof JSONObject){
            JSONObject object=(JSONObject)value;Map<String,Object> sorted=new TreeMap<>();
            java.util.Iterator<String> keys=object.keys();while(keys.hasNext()){String key=keys.next();sorted.put(key,structural(object.get(key)));}return sorted;
        }
        if(value instanceof JSONArray){
            JSONArray array=(JSONArray)value;List<Object> list=new ArrayList<>();for(int i=0;i<array.length();i++)list.add(structural(array.get(i)));return list;
        }
        if(value instanceof Number)return ((Number)value).longValue();
        return value;
    }
    private static Object normalized(String raw) throws JSONException {
        JSONObject state=InventoryStore.parseSnapshot(raw);
        JSONObject result=new JSONObject().put("version",3).put("bag",state.getJSONArray("bag")).put("boxes",state.getJSONObject("boxes"));
        result.put("harvests",state.getInt("version")==3?state.getJSONObject("harvests"):new JSONObject());
        return structural(result);
    }
    /** null local means never initialized; null base means no prior sync; null remote means absent. */
    static Action decide(String local,String base,String remote) throws JSONException {
        Object current=normalized(local),baseline=base==null?null:normalized(base),cloud=remote==null?null:normalized(remote);
        if(cloud!=null && current.equals(cloud))return Action.MATCH;
        if(cloud==null && baseline!=null)return Action.REMOTE_MISSING;
        if(baseline==null && cloud!=null && local!=null)return Action.INITIAL_CONFLICT;
        if(baseline!=null && !current.equals(baseline) && !baseline.equals(cloud))return Action.BOTH_CHANGED;
        if(cloud!=null && (local==null && baseline==null || current.equals(baseline)))return Action.DOWNLOAD;
        if(cloud==null || cloud.equals(baseline))return Action.UPLOAD;
        throw new JSONException("Unresolved sync state");
    }
    private InventorySyncPlan() {}
}
