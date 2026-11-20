package dev.retrobit.gtlauncher;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Trim bottom layers only, retaining the five uppermost blocks and all other world data. */
final class IslandHeightReset {
    static String clean(String raw) throws JSONException {
        JSONArray entries=new JSONArray(raw);boolean changed=false;
        for(int i=0;i<entries.length();i++) {
            JSONObject item=entries.getJSONObject(i);if(!item.has("previewExport"))continue;
            String source=item.getString("previewExport");PreviewWorld20.parse(source);
            JSONObject root=new JSONObject(source),w=root.getJSONObject("world");JSONArray stacks=w.optJSONArray("blockStacks");if(stacks==null)continue;
            int size=w.getInt("gridSize"),n=stacks.length();int[] cuts=new int[n];boolean any=false;
            boolean[] ramp=new boolean[n];JSONArray ramps=w.optJSONArray("ramps");if(ramps!=null)for(int j=0;j<ramps.length();j++){JSONObject r=ramps.getJSONObject(j);ramp[r.getInt("row")*size+r.getInt("column")]=true;}
            for(int j=0;j<n;j++){JSONArray stack=stacks.getJSONArray(j);cuts[j]=Math.max(0,stack.length()-(ramp[j]?4:5));if(cuts[j]>0){any=true;JSONArray trimmed=new JSONArray();for(int k=cuts[j];k<stack.length();k++)trimmed.put(stack.get(k));stacks.put(j,trimmed);}}
            if(!any)continue;
            JSONArray opacity=w.optJSONArray("blockOpacity");if(opacity!=null){JSONArray kept=new JSONArray();for(int j=0;j<opacity.length();j++){
                JSONObject o=opacity.getJSONObject(j);int level=o.getInt("level")-cuts[o.getInt("row")*size+o.getInt("column")];if(level>=0){o.put("level",level);kept.put(o);}}
                w.put("blockOpacity",kept);
            }
            JSONArray bridges=w.optJSONArray("bridges");if(bridges!=null)for(int j=0;j<bridges.length();j++){
                JSONObject b=bridges.getJSONObject(j);if(b.optBoolean("free",false))b.put("deck",Math.max(1,b.getInt("deck")-cuts[b.getInt("row")*size+b.getInt("column")]));
            }
            PreviewWorld20.parse(root.toString()); // Fail atomically if objects or legacy bridges no longer fit.
            item.put("previewExport",root.toString());changed=true;
        }
        return changed?entries.toString():raw;
    }
    private IslandHeightReset() {}
}
