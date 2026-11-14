package dev.retrobit.gtlauncher;

import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Bag and storage box contents are committed together in one preference. */
final class InventoryStore {
    static final String KEY="items_v1";
    private final SharedPreferences prefs;
    InventoryStore(SharedPreferences prefs){this.prefs=prefs;}
    private static void validateItems(JSONArray items) throws JSONException {
        if(items.length()>100)throw new JSONException("Too many items");
        Set<String> names=new HashSet<>();
        for(int i=0;i<items.length();i++){
            JSONObject item=items.getJSONObject(i);Object n=item.get("name"),q=item.get("quantity");
            if(item.length()!=2 || !(n instanceof String) || !(q instanceof Number))throw new JSONException("Invalid item");
            String name=(String)n;double value=((Number)q).doubleValue();
            if(name.isEmpty() || !name.equals(name.trim()) || name.length()>40 || !Double.isFinite(value) || value<1 || value>999999 || value!=Math.floor(value) || !names.add(name.toLowerCase(Locale.ROOT)))throw new JSONException("Invalid item");
        }
    }
    private JSONObject readState() throws JSONException {
        String raw=prefs.getString(KEY,null);
        if(raw==null)raw="[]";
        org.json.JSONTokener tokener=new org.json.JSONTokener(raw);Object value=tokener.nextValue();if(tokener.nextClean()!=0)throw new JSONException("Trailing inventory data");JSONObject state;
        if(value instanceof JSONArray)state=new JSONObject().put("version",2).put("bag",value).put("boxes",new JSONObject());
        else if(value instanceof JSONObject)state=(JSONObject)value;
        else throw new JSONException("Invalid inventory");
        Object versionValue=state.opt("version");if(!(versionValue instanceof Number))throw new JSONException("Invalid inventory version");
        double version=((Number)versionValue).doubleValue();if(!((version==2 && state.length()==3) || (version==3 && state.length()==4)))throw new JSONException("Invalid inventory version");
        JSONArray bag=state.getJSONArray("bag");JSONObject boxes=state.getJSONObject("boxes");validateItems(bag);
        java.util.Iterator<String> keys=boxes.keys();while(keys.hasNext()){String key=keys.next();validateBoxKey(key);validateItems(boxes.getJSONArray(key));}
        if(version==3){JSONObject harvests=state.getJSONObject("harvests");java.util.Iterator<String> ids=harvests.keys();while(ids.hasNext()){String id=ids.next();validateBoxKey(id);Object cycle=harvests.get(id);if(!(cycle instanceof String) || !validCycle((String)cycle))throw new JSONException("Invalid harvest history");}}
        return state;
    }
    private static void validateBoxKey(String key) throws JSONException {if(key==null || key.isEmpty() || key.length()>200)throw new JSONException("Invalid box");}
    static String boxKey(String worldId,int column,int row){return worldId+":"+column+","+row;}
    JSONArray bag() throws JSONException {return readState().getJSONArray("bag");}
    JSONArray box(String key) throws JSONException {validateBoxKey(key);JSONObject boxes=readState().getJSONObject("boxes");return boxes.has(key)?boxes.getJSONArray(key):new JSONArray();}
    static String treeKey(String worldId,int column,int row,String species){return boxKey(worldId,column,row)+":"+species;}
    private static boolean validCycle(String cycle) {
        if(cycle==null || !cycle.matches("\\d{4}-\\d{2}-\\d{2}"))return false;
        try{return java.time.LocalDate.parse(cycle).toString().equals(cycle);}catch(java.time.DateTimeException e){return false;}
    }
    boolean wasHarvested(String key,String cycle) throws JSONException {
        validateBoxKey(key);if(!validCycle(cycle))throw new JSONException("Invalid cycle");JSONObject state=readState();
        if(state.getInt("version")<3)return false;JSONObject history=state.getJSONObject("harvests");return history.has(key) && history.getString(key).compareTo(cycle)>=0;
    }
    boolean harvest(String key,String cycle,String itemName) throws JSONException {
        validateBoxKey(key);if(!validCycle(cycle))throw new JSONException("Invalid cycle");JSONObject state=readState();
        JSONObject history=state.getInt("version")==3?state.getJSONObject("harvests"):new JSONObject();
        if(history.has(key) && history.getString(key).compareTo(cycle)>=0)throw new JSONException("Already harvested");
        JSONArray updated=adjust(state.getJSONArray("bag"),itemName,1);
        history.put(key,cycle);state.put("bag",updated);state.put("harvests",history);state.put("version",3);return save(state);
    }
    private static JSONArray adjust(JSONArray items,String name,int delta) throws JSONException {
        if(name==null || name.isEmpty() || !name.equals(name.trim()) || name.length()>40 || delta==0)throw new JSONException("Invalid change");
        JSONArray next=new JSONArray();boolean found=false;
        for(int i=0;i<items.length();i++){
            JSONObject item=items.getJSONObject(i);String current=item.getString("name");int count=item.getInt("quantity");
            if(current.equalsIgnoreCase(name)){found=true;long value=(long)count+delta;if(value<0 || value>999999)throw new JSONException("Invalid quantity");if(value>0)next.put(new JSONObject().put("name",current).put("quantity",value));}
            else next.put(item);
        }
        if(!found){if(delta<1 || delta>999999 || items.length()>=100)throw new JSONException("Missing item or full");next.put(new JSONObject().put("name",name).put("quantity",delta));}
        return next;
    }
    private boolean save(JSONObject state) {return prefs.edit().putString(KEY,state.toString()).commit();}
    boolean changeBag(String name,int delta) throws JSONException {JSONObject state=readState();state.put("bag",adjust(state.getJSONArray("bag"),name,delta));return save(state);}
    boolean transfer(String key,String name,int quantity,boolean toBox) throws JSONException {
        validateBoxKey(key);if(quantity<1 || quantity>999999)throw new JSONException("Invalid quantity");
        JSONObject state=readState(),boxes=state.getJSONObject("boxes");JSONArray bag=state.getJSONArray("bag"),box=boxes.has(key)?boxes.getJSONArray(key):new JSONArray();
        JSONArray source=toBox?bag:box;String original=null;for(int i=0;i<source.length();i++){String n=source.getJSONObject(i).getString("name");if(n.equalsIgnoreCase(name)){original=n;break;}}
        if(original==null)throw new JSONException("Missing item");
        JSONArray nextSource=adjust(source,original,-quantity),nextDestination=adjust(toBox?box:bag,original,quantity);
        JSONArray nextBox=toBox?nextDestination:nextSource;state.put("bag",toBox?nextSource:nextDestination);
        if(nextBox.length()==0)boxes.remove(key);else boxes.put(key,nextBox);
        return save(state);
    }
}
