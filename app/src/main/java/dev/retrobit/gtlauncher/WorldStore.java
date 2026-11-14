package dev.retrobit.gtlauncher;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.UUID;

/** Browser-exported world data stored locally on this Android device. */
final class WorldStore {
    private static final String WORLDS_KEY = "worlds_v1";
    private static final String ACTIVE_KEY = "active_world_v1";
    static final String BASE_ID = "base";
    private final SharedPreferences prefs;

    static final class SurfaceObject {
        final String type, edge, material, raw;
        final int column, row, opacity;
        SurfaceObject(String type,int column,int row,String edge,String material,String raw) {
            this(type,column,row,edge,material,raw,-1);
        }
        SurfaceObject(String type,int column,int row,String edge,String material,String raw,int opacity) {
            this.type=type;this.column=column;this.row=row;this.edge=edge;this.material=material;this.raw=raw;this.opacity=opacity;
        }
    }
    static final class World {
        final String id, name, description, color;
        final boolean showCharacter, showHouse, hasHouse;
        final int houseColumn, houseRow, houseOpacity;
        final double houseScale;
        final String houseFacing, houseId;
        final int formatVersion;
        final int characterColumn, characterRow;
        final double characterScale;
        final String characterFacing;
        final List<PortalTravel.Portal> portals;
        final List<SurfaceObject> surfaces;
        World(String id, String name, String description, String color,
                boolean showCharacter, int column, int row, double scale, String facing,
                boolean hasHouse, boolean showHouse, int houseColumn, int houseRow, double houseScale, String houseFacing, String houseId, int houseOpacity, int formatVersion, List<PortalTravel.Portal> portals, List<SurfaceObject> surfaces) {
            this.id = id; this.name = name; this.description = description; this.color = color;
            this.showCharacter = showCharacter; this.characterColumn = column;
            this.characterRow = row; this.characterScale = scale; this.characterFacing = facing;
            this.hasHouse = hasHouse; this.showHouse = showHouse; this.houseColumn = houseColumn;
            this.houseRow = houseRow; this.houseScale = houseScale; this.houseFacing = houseFacing;
            this.houseId = houseId; this.houseOpacity=houseOpacity; this.formatVersion = formatVersion; this.portals = java.util.Collections.unmodifiableList(new ArrayList<>(portals)); this.surfaces = java.util.Collections.unmodifiableList(new ArrayList<>(surfaces));
        }
    }

    WorldStore(SharedPreferences prefs) { this.prefs = prefs; }

    private static String requiredString(JSONObject object, String key) throws JSONException {
        Object value = object.get(key);
        if (!(value instanceof String)) throw new JSONException("Invalid " + key);
        return (String) value;
    }

    private static World parseWorld(JSONObject object, String id, int formatVersion) throws JSONException {
        String name = requiredString(object, "name").trim();
        String description = requiredString(object, "description").trim();
        String color = requiredString(object, "color");
        if (name.isEmpty() || name.length() > 48 || description.length() > 160
                || !color.matches("#[0-9a-fA-F]{6}")) throw new JSONException("Invalid world fields");
        boolean visible = false, house = false, houseVisible = false;
        int houseColumn = 0, houseRow = 0, houseOpacity = -1;
        double houseScale = 1.0;
        String houseFacing = "S", houseId=null;
        int column = 4, row = 4;
        double scale = 1.0;
        String facing = "S";
        List<PortalTravel.Portal> portals = new ArrayList<>();
        List<SurfaceObject> surfaces = new ArrayList<>();
        java.util.Set<String> fenceIds=new java.util.HashSet<>(), fenceEdges=new java.util.HashSet<>(), roadTiles=new java.util.HashSet<>();
        if (object.has("objects")) {
            Object value = object.get("objects");
            if (!(value instanceof JSONArray)) throw new JSONException("Invalid objects");
            JSONArray objects = (JSONArray) value;
            if (objects.length() > (formatVersion >= 21 ? 687 : formatVersion >= 17 ? 606 : formatVersion >= 16 ? 525 : formatVersion >= 12 ? 445 : formatVersion >= 9 ? 364 : formatVersion >= 8 ? 283 : formatVersion >= 7 ? 202 : formatVersion >= 5 ? 2 : 1)) throw new JSONException("Unsupported objects");
            for (int i = 0; i < objects.length(); i++) {
                JSONObject character = objects.getJSONObject(i);
                String type = requiredString(character, "type");
                if (formatVersion >= 8 && "road".equals(type)) {
                    requireOnlyKeys(character,"type","column","row","material","facing");
                    if(formatVersion<19 && character.has("facing"))throw new JSONException("Unsupported road direction");
                    Object col=character.get("column"),rowObject=character.get("row");
                    if (!(col instanceof Number) || !(rowObject instanceof Number) || ((Number)col).doubleValue()!=Math.rint(((Number)col).doubleValue()) || ((Number)rowObject).doubleValue()!=Math.rint(((Number)rowObject).doubleValue())) throw new JSONException("Invalid road");
                    int c=((Number)col).intValue(),r=((Number)rowObject).intValue();String material=requiredString(character,"material");
                    if(c<0||c>8||r<0||r>8||!roadTiles.add(c+","+r)) throw new JSONException("Invalid road");
                    if(!java.util.Arrays.asList("dirt","stone","gravel").contains(material) && !(formatVersion>=11 && material.equals("water")) && !(formatVersion>=12 && java.util.Arrays.asList("paving","asphalt").contains(material))) throw new JSONException("Unsupported object");
                    String roadFacing=character.has("facing")?requiredString(character,"facing"):null;
                    if(roadFacing!=null && !java.util.Arrays.asList("N","E","S","W").contains(roadFacing))throw new JSONException("Invalid road direction");
                    surfaces.add(new SurfaceObject(type,c,r,roadFacing,material,character.toString()));continue;
                }
                if (formatVersion >= 7 && "fence".equals(type)) {
                    requireOpacityKeys(character,formatVersion,"type","id","column","row","edge","variant","destinationWorldId","destinationPortalId");
                    int opacity=readOpacity(character,formatVersion);
                    String variant=character.has("variant") ? requiredString(character,"variant") : "";
                    if (!variant.isEmpty() && !java.util.Arrays.asList("portal","slat","stone","iron").contains(variant)) throw new JSONException("Unsupported object");
                    if (character.has("variant") && (variant.isEmpty() || formatVersion<10)) throw new JSONException("Unsupported fence variant");
                    if (variant.equals("portal") && formatVersion<22) throw new JSONException("Unsupported object");
                    String portalId=requiredString(character,"id"), edge=requiredString(character,"edge");
                    Object c=character.get("column"), r=character.get("row");
                    if (!portalId.matches("[A-Za-z0-9_-]{1,128}") || !(c instanceof Number) || !(r instanceof Number)
                            || ((Number)c).doubleValue()!=Math.rint(((Number)c).doubleValue()) || ((Number)r).doubleValue()!=Math.rint(((Number)r).doubleValue())) throw new JSONException("Invalid portal");
                    int col=((Number)c).intValue(), portalRow=((Number)r).intValue();
                    if (col<0 || col>8 || portalRow<0 || portalRow>8 || !(edge.equals("N")||edge.equals("E")||edge.equals("S")||edge.equals("W"))) throw new JSONException("Invalid fence edge");
                    int nc=col+(edge.equals("E")?1:edge.equals("W")?-1:0), nr=portalRow+(edge.equals("S")?1:edge.equals("N")?-1:0);
                    int start=portalRow*9+col, end=nr*9+nc;
                    String edgeKey=(nc>=0 && nc<9 && nr>=0 && nr<9) ? Math.min(start,end)+":"+Math.max(start,end) : col+","+portalRow+","+edge;
                    if(!fenceIds.add(portalId) || !fenceEdges.add(edgeKey)) throw new JSONException("Duplicate fence");
                    if (!variant.equals("portal")) {
                        if(character.has("destinationWorldId") || character.has("destinationPortalId")) throw new JSONException("Invalid fence pair");
                        surfaces.add(new SurfaceObject(type,col,portalRow,edge,variant,character.toString(),opacity));continue;
                    }
                    if (!((edge.equals("N") && portalRow==0) || (edge.equals("S") && portalRow==8) || (edge.equals("W") && col==0) || (edge.equals("E") && col==8))) throw new JSONException("Invalid portal edge");
                    if (character.has("destinationWorldId") != character.has("destinationPortalId")) throw new JSONException("Incomplete portal pair");
                    String destWorld=null,destPortal=null;
                    if (character.has("destinationWorldId")) {
                        if(formatVersion<28) throw new JSONException("Unsupported object");
                        destWorld=requiredString(character,"destinationWorldId");destPortal=requiredString(character,"destinationPortalId");
                        if (!destWorld.matches("[A-Za-z0-9_-]{1,128}") || !destPortal.matches("[A-Za-z0-9_-]{1,128}")) throw new JSONException("Invalid portal pair");
                    }
                    for (PortalTravel.Portal previous : portals) if (previous.id.equals(portalId) || (previous.tile.column==col && previous.tile.row==portalRow && previous.edge.equals(edge))) throw new JSONException("Duplicate portal");
                    portals.add(new PortalTravel.Portal(id,portalId,edge,new PortalTravel.Tile(col,portalRow),destWorld,destPortal,opacity)); continue;
                }
                if(formatVersion>=20 && "flower".equals(type)) {
                    requireOnlyKeys(character,"type","column","row","species");
                    Object col=character.get("column"),rowObject=character.get("row");
                    if(!(col instanceof Number)||!(rowObject instanceof Number)||((Number)col).doubleValue()!=Math.rint(((Number)col).doubleValue())||((Number)rowObject).doubleValue()!=Math.rint(((Number)rowObject).doubleValue()))throw new JSONException("Invalid flower tile");
                    int c=((Number)col).intValue(),r=((Number)rowObject).intValue();String species=requiredString(character,"species");
                    if(c<0||c>8||r<0||r>8||!java.util.Arrays.asList("rose","tulip","lavender","daisy","marigold","bluebell","jasmine","ylang","frangipani","hibiscus","periwinkle").contains(species))throw new JSONException("Invalid flower");
                    if(formatVersion<21 && !java.util.Arrays.asList("rose","tulip","lavender","daisy","marigold","bluebell").contains(species))throw new JSONException("Unsupported flower");
                    for(SurfaceObject prior:surfaces)if(prior.type.equals("flower")&&prior.column==c&&prior.row==r)throw new JSONException("Duplicate flower");
                    surfaces.add(new SurfaceObject(type,c,r,null,species,character.toString()));continue;
                }
                if ((formatVersion>=9 && "bush".equals(type)) || (formatVersion>=14 && "tree".equals(type)) || (formatVersion>=18 && "sunflower".equals(type)) || (formatVersion>=17 && "mailbox".equals(type)) || (formatVersion>=26 && "storage-box".equals(type)) || (formatVersion>=30 && "selling-box".equals(type))) {
                    if(type.equals("bush")) requireOpacityKeys(character,formatVersion,"type","column","row","variant");
                    else if(type.equals("tree")) requireOpacityKeys(character,formatVersion,"type","column","row","species");
                    else if(type.equals("sunflower") && formatVersion>=19) requireOpacityKeys(character,formatVersion,"type","column","row","facing");
                    else if((type.equals("storage-box") || type.equals("selling-box")) && formatVersion>=27) requireOpacityKeys(character,formatVersion,"type","column","row","facing");
                    else requireOpacityKeys(character,formatVersion,"type","column","row");
                    int opacity=readOpacity(character,formatVersion);
                    Object co=character.get("column"),ro=character.get("row");
                    if(!(co instanceof Number) || !(ro instanceof Number) || ((Number)co).doubleValue()!=Math.rint(((Number)co).doubleValue()) || ((Number)ro).doubleValue()!=Math.rint(((Number)ro).doubleValue())) throw new JSONException("Invalid object tile");
                    int c=((Number)co).intValue(),r=((Number)ro).intValue();
                    if(c<0||c>8||r<0||r>8) throw new JSONException("Invalid object tile");
                    if(type.equals("bush") && formatVersion<10 && character.has("variant"))throw new JSONException("Unexpected bush variant");
                    String variant=type.equals("bush") ? (character.has("variant") ? requiredString(character,"variant") : "round") : type.equals("tree") ? requiredString(character,"species") : (type.equals("storage-box") || type.equals("selling-box") || type.equals("sunflower")) && character.has("facing") ? requiredString(character,"facing") : type.equals("sunflower") ? "S" : "";
                    if(type.equals("bush") && !((formatVersion>=9 && variant.equals("round")) || (formatVersion>=10 && java.util.Arrays.asList("flower","berry").contains(variant)) || (formatVersion>=11 && java.util.Arrays.asList("spiky","tiered").contains(variant)) || (formatVersion>=29 && variant.equals("wild-grass")))) throw new JSONException("Unsupported object");
                    if(type.equals("tree") && !java.util.Arrays.asList("apple","orange","mango","pear","guava","starfruit").contains(variant) && !(formatVersion>=25 && (variant.equals("banana") || variant.equals("palm")))) throw new JSONException("Unsupported object");
                    if(type.equals("tree") && !(variant.equals("banana") || variant.equals("palm")) && (c>7||r>7)) throw new JSONException("Invalid tree tile");
                    if(type.equals("sunflower") && !java.util.Arrays.asList("N","E","S","W").contains(variant)) throw new JSONException("Invalid direction");
                    if((type.equals("storage-box") || type.equals("selling-box")) && !variant.isEmpty() && !java.util.Arrays.asList("N","E","S","W").contains(variant)) throw new JSONException("Invalid direction");
                    for(SurfaceObject previous:surfaces) if(!previous.type.equals("road") && !previous.type.equals("fence") && !previous.type.equals("flower") && previous.column==c && previous.row==r) throw new JSONException("Object collision");
                    surfaces.add(new SurfaceObject(type,c,r,null,variant,character.toString(),opacity));continue;
                }
                if ("house".equals(type)) {
                    if (formatVersion < 5 || house) throw new JSONException("Unsupported house");
                    if (formatVersion >= 6) {
                        if(formatVersion>=16) {
                            requireOpacityKeys(character,formatVersion,"type","id","column","row","scale","facing","visible");
                            if(character.has("id")) {houseId=requiredString(character,"id"); if(!houseId.matches("[A-Za-z0-9_-]{1,128}")) throw new JSONException("Invalid house id");}
                        } else requireOpacityKeys(character,formatVersion, "type", "column", "row", "scale", "facing", "visible");
                        houseOpacity=readOpacity(character,formatVersion);
                        Object hc = character.get("column"), hr = character.get("row"), hs = character.get("scale"), hv = character.get("visible");
                        if (!(hc instanceof Number) || !(hr instanceof Number) || !(hs instanceof Number) || !(hv instanceof Boolean))
                            throw new JSONException("Invalid house geometry");
                        double c = ((Number) hc).doubleValue(), r = ((Number) hr).doubleValue();
                        houseScale = ((Number) hs).doubleValue();
                        houseFacing = requiredString(character, "facing");
                        if (c != Math.rint(c) || r != Math.rint(r) || c < 0 || c > 6 || r < 0 || r > 6
                                || !Double.isFinite(houseScale) || houseScale < 0.5 || houseScale > 1.5
                                || !(houseFacing.equals("N") || houseFacing.equals("E") || houseFacing.equals("S") || houseFacing.equals("W")))
                            throw new JSONException("Invalid house geometry");
                        houseColumn = (int) c; houseRow = (int) r; houseVisible = (Boolean) hv;
                    } else {
                        requireOnlyKeys(character, "type", "column", "row");
                        Object hc = character.get("column"), hr = character.get("row");
                        if (!(hc instanceof Number) || !(hr instanceof Number)
                                || ((Number) hc).doubleValue() != 0 || ((Number) hr).doubleValue() != 0)
                            throw new JSONException("Unsupported house location");
                        houseVisible = true;
                    }
                    house = true; continue;
                }
                if (visible || !"character".equals(type)) throw new JSONException("Unsupported object");
                requireOnlyKeys(character, "type", "column", "row", "scale", "facing");
                if (formatVersion >= 4) {
                    facing = requiredString(character, "facing");
                    if (!facing.equals("N") && !facing.equals("E") && !facing.equals("S") && !facing.equals("W"))
                        throw new JSONException("Invalid character direction");
                } else if (character.has("facing")) throw new JSONException("Unexpected direction");
                if (!"character".equals(requiredString(character, "type"))) throw new JSONException("Unsupported object");
                Object col = character.get("column"), r = character.get("row"), size = character.get("scale");
                if (!(col instanceof Number) || !(r instanceof Number) || !(size instanceof Number))
                    throw new JSONException("Invalid character geometry");
                double colValue = ((Number) col).doubleValue(), rowValue = ((Number) r).doubleValue();
                scale = ((Number) size).doubleValue();
                if (colValue != Math.rint(colValue) || rowValue != Math.rint(rowValue)
                        || colValue < 0 || colValue > 8 || rowValue < 0 || rowValue > 8
                        || !Double.isFinite(scale) || scale < 0.5 || scale > 1.5)
                    throw new JSONException("Invalid character geometry");
                column = (int) colValue; row = (int) rowValue; visible = true;
            }
        }
        for(SurfaceObject flower:surfaces) if(flower.type.equals("flower"))
            for(SurfaceObject water:surfaces) if(water.type.equals("road") && "water".equals(water.material) && water.column==flower.column && water.row==flower.row)throw new JSONException("Flower on water");
        // Validate the complete tree footprint, not only its anchor tile.
        for(SurfaceObject tree:surfaces) if(tree.type.equals("tree")) {
            int size=(tree.material.equals("banana") || tree.material.equals("palm"))?1:2;
            for(int dc=0;dc<size;dc++)for(int dr=0;dr<size;dr++) {
                int c=tree.column+dc,r=tree.row+dr;
                if(visible && c==column && r==row || house && houseVisible && c>=houseColumn && c<=houseColumn+2 && r>=houseRow && r<=houseRow+2)throw new JSONException("Object collision");
                for(SurfaceObject other:surfaces) {
                    if(other==tree || other.type.equals("fence") || other.type.equals("flower") || other.type.equals("road") && !"water".equals(other.material))continue;
                    int otherSize=other.type.equals("tree") && !(other.material.equals("banana")||other.material.equals("palm"))?2:1;
                    if(c>=other.column && c<other.column+otherSize && r>=other.row && r<other.row+otherSize)throw new JSONException("Object collision");
                }
            }
        }
        // New one-tile plants must never hide under an existing blocker, even if the
        // objects arrive in the opposite order in the file.
        for (SurfaceObject plant : surfaces) {
            boolean tropical=plant.type.equals("tree") && (plant.material.equals("banana") || plant.material.equals("palm"));
            if (!tropical && !plant.type.equals("sunflower")) continue;
            int c=plant.column,r=plant.row;
            if (visible && c==column && r==row || house && houseVisible && c>=houseColumn && c<=houseColumn+2 && r>=houseRow && r<=houseRow+2) throw new JSONException("Object collision");
            for (SurfaceObject other:surfaces) {
                if (other==plant || other.type.equals("fence") || other.type.equals("flower")) continue;
                if (other.type.equals("road") && !"water".equals(other.material)) continue;
                int size=other.type.equals("tree") && !(other.material.equals("banana") || other.material.equals("palm"))?2:1;
                if(c>=other.column && c<other.column+size && r>=other.row && r<other.row+size) throw new JSONException("Object collision");
            }
        }
        return new World(id, name, description, color, visible, column, row, scale, facing,
                house, houseVisible, houseColumn, houseRow, houseScale, houseFacing, houseId, houseOpacity, formatVersion, portals, surfaces);
    }

    private static boolean hasOverlappingFlowers(List<SurfaceObject> surfaces) {
        for(SurfaceObject f:surfaces)if(f.type.equals("flower"))for(SurfaceObject b:surfaces)
            if((b.type.equals("bush")||b.type.equals("sunflower"))&&f.column==b.column&&f.row==b.row)return true;
        return false;
    }
    private static int readOpacity(JSONObject object,int version) throws JSONException {
        if(!object.has("opacity"))return -1;
        if(version<31)throw new JSONException("Unsupported opacity");
        Object value=object.get("opacity");
        if(!(value instanceof Number))throw new JSONException("Invalid opacity");
        double opacity=((Number)value).doubleValue();
        if(!Double.isFinite(opacity) || opacity<0 || opacity>90 || opacity%5!=0)throw new JSONException("Invalid opacity");
        return (int)opacity;
    }
    private static void requireOpacityKeys(JSONObject object,int version,String... allowed) throws JSONException {
        readOpacity(object,version);
        String[] keys=java.util.Arrays.copyOf(allowed,allowed.length+1);keys[allowed.length]="opacity";
        requireOnlyKeys(object,keys);
    }

    private static void requireOnlyKeys(JSONObject object, String... allowed) throws JSONException {
        Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            boolean found = false;
            for (String item : allowed) if (item.equals(key)) found = true;
            if (!found) throw new JSONException("Unsupported field: " + key);
        }
    }

    static World parseExport(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        if (!"gtlauncher-world".equals(requiredString(root, "format"))
                || !(root.get("version") instanceof Number)
                || !(root.get("world") instanceof JSONObject)) {
            throw new JSONException("Unsupported world format");
        }
        double version = ((Number) root.get("version")).doubleValue();
        JSONObject object = root.getJSONObject("world");
        if (version == 1.0) {
            // Legacy exports had no stable identifier; reject unknown fields rather than discard them.
            requireOnlyKeys(root, "format", "version", "world");
            requireOnlyKeys(object, "name", "description", "color");
            return parseWorld(object, UUID.randomUUID().toString(), 1);
        }
        if (version != 2.0 && version != 3.0 && version != 4.0 && version != 5.0 && version != 6.0 && version != 7.0 && version != 8.0 && version != 9.0 && version != 10.0 && version != 11.0 && version != 12.0 && version != 14.0 && version != 16.0 && version != 17.0 && version != 18.0 && version != 19.0 && version != 20.0 && version != 21.0 && version != 22.0 && version != 25.0 && version != 26.0 && version != 27.0 && version != 28.0 && version != 29.0 && version != 30.0 && version != 31.0) throw new JSONException("Unsupported world version");
        requireOnlyKeys(root, "format", "version", "world");
        requireOnlyKeys(object, "id", "name", "description", "color", "objects");
        String id = requiredString(object, "id");
        if (!id.matches("[A-Za-z0-9_-]{1,128}") || BASE_ID.equals(id))
            throw new JSONException("Invalid world id");
        Object objects = object.get("objects");
        if (!(objects instanceof JSONArray) || (version == 2.0 && ((JSONArray) objects).length() != 0))
            throw new JSONException("Unsupported world objects");
        return parseWorld(object, id, (int) version);
    }

    private static int storedVersion(JSONObject item) throws JSONException {
        if (!item.has("objects")) return 3;
        Object value = item.get("objects");
        if (!(value instanceof JSONArray)) throw new JSONException("Invalid objects");
        JSONArray objects = (JSONArray) value;
        int version = 3;
        for (int i = 0; i < objects.length(); i++) {
            JSONObject obj = objects.getJSONObject(i);
            if(obj.has("opacity"))version=Math.max(version,31);
            if("flower".equals(obj.optString("type")))version=Math.max(version,21);
            if ("selling-box".equals(obj.optString("type"))) version = Math.max(version, 30);
            else if ("sunflower".equals(obj.optString("type")) || "tree".equals(obj.optString("type")) && ("banana".equals(obj.optString("species")) || "palm".equals(obj.optString("species"))) || "bush".equals(obj.optString("type")) && "wild-grass".equals(obj.optString("variant"))) version = Math.max(version, 29);
            else if ("fence".equals(obj.optString("type")) || "road".equals(obj.optString("type")) || "bush".equals(obj.optString("type")) || "tree".equals(obj.optString("type")) || "mailbox".equals(obj.optString("type")) || "storage-box".equals(obj.optString("type"))) version = Math.max(version, 28);
            else if ("house".equals(obj.optString("type"))) version = Math.max(version, obj.has("id") ? 28 : obj.has("visible") || obj.has("scale") || obj.has("facing") ? 6 : 5);
            else if (obj.has("facing")) version = Math.max(version, 4);
        }
        return version;
    }

    List<World> list() throws JSONException {
        JSONArray array = new JSONArray(prefs.getString(WORLDS_KEY, "[]"));
        if (array.length() > 1000) throw new JSONException("Too many worlds");
        List<World> worlds = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            String id = requiredString(item, "id");
            if (id.isEmpty() || BASE_ID.equals(id)) throw new JSONException("Invalid world id");
            for (World previous : worlds) if (previous.id.equals(id)) throw new JSONException("Duplicate world id");
            worlds.add(parseWorld(item, id, storedVersion(item)));
        }
        return worlds;
    }

    String activeId() { return prefs.getString(ACTIVE_KEY, BASE_ID); }

    World activeWorld() throws JSONException {
        String id = activeId();
        for (World world : list()) if (world.id.equals(id)) return world;
        return null; // Base World (or the removed built-in Home) has the base appearance
    }

    boolean select(String id) throws JSONException {
        boolean found = BASE_ID.equals(id);
        for (World world : list()) if (world.id.equals(id)) found = true;
        return found && prefs.edit().putString(ACTIVE_KEY, id).commit();
    }

    boolean upsertAndSelect(World world) throws JSONException {
        List<World> existing = list(); // Never overwrite unreadable stored data.
        boolean found = false;
        for (int i = 0; i < existing.size(); i++) {
            if (existing.get(i).id.equals(world.id)) {
                if(world.formatVersion<21 && existing.get(i).surfaces.stream().anyMatch(o->o.type.equals("flower") && !java.util.Arrays.asList("rose","tulip","lavender","daisy","marigold","bluebell").contains(o.material)))throw new JSONException("Legacy export cannot replace flower variant");
                if(world.formatVersion<21 && hasOverlappingFlowers(existing.get(i).surfaces))throw new JSONException("Legacy export cannot replace overlapping flowers");
                if(world.formatVersion<20 && existing.get(i).surfaces.stream().anyMatch(o->o.type.equals("flower")))throw new JSONException("Legacy export cannot replace flowers");
                if(world.formatVersion<31 && (existing.get(i).houseOpacity>=0 || existing.get(i).surfaces.stream().anyMatch(o->o.opacity>=0) || existing.get(i).portals.stream().anyMatch(p->p.opacity>=0)))throw new JSONException("Legacy export cannot replace opacity");
                if (world.formatVersion < 30 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("selling-box"))) throw new JSONException("Legacy export cannot replace selling box");
                if (world.formatVersion < 25 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("tree") && (o.material.equals("banana") || o.material.equals("palm")))) throw new JSONException("Legacy export cannot replace tropical trees");
                if (world.formatVersion < 18 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("sunflower")) || world.formatVersion < 19 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("sunflower") && !o.material.equals("S"))) throw new JSONException("Legacy export cannot replace sunflower");
                if (world.formatVersion < 29 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("bush") && o.material.equals("wild-grass"))) throw new JSONException("Legacy export cannot replace wild grass");
                if (world.formatVersion < 16 && existing.get(i).houseId != null) throw new JSONException("Legacy export cannot replace house identity");
                if (world.formatVersion < 27 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("storage-box") && !o.material.isEmpty())) throw new JSONException("Legacy export cannot replace storage direction");
                if (world.formatVersion < 26 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("storage-box"))) throw new JSONException("Legacy export cannot replace storage");
                if (world.formatVersion < 17 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("mailbox"))) throw new JSONException("Legacy export cannot replace mailbox");
                if (world.formatVersion < 14 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("tree"))) throw new JSONException("Legacy export cannot replace tree");
                if(world.formatVersion<11 && existing.get(i).surfaces.stream().anyMatch(o->o.type.equals("bush") && (o.material.equals("spiky") || o.material.equals("tiered"))))throw new JSONException("Legacy export cannot replace bush variant");
                if(world.formatVersion<10 && existing.get(i).surfaces.stream().anyMatch(o->o.type.equals("bush") && (o.material.equals("flower") || o.material.equals("berry"))))throw new JSONException("Legacy export cannot replace bush variant");
                if (world.formatVersion < 9 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("bush"))) throw new JSONException("Legacy export cannot replace bush");
                if(world.formatVersion<19 && existing.get(i).surfaces.stream().anyMatch(o->o.type.equals("road") && o.edge!=null))throw new JSONException("Legacy export cannot replace road direction");
                if(world.formatVersion<12 && existing.get(i).surfaces.stream().anyMatch(o->o.type.equals("road") && ("paving".equals(o.material)||"asphalt".equals(o.material))))throw new JSONException("Legacy export cannot replace road material");
                if (world.formatVersion < 8 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("road"))) throw new JSONException("Legacy export cannot replace road data");
                if(world.formatVersion<10 && existing.get(i).surfaces.stream().anyMatch(o->o.type.equals("fence") && o.material!=null && !o.material.isEmpty()))throw new JSONException("Legacy export cannot replace fence variant");
                if (world.formatVersion < 7 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("fence"))) throw new JSONException("Legacy export cannot replace fence data");
                if (world.formatVersion < 22 && !existing.get(i).portals.isEmpty()) throw new JSONException("Legacy export cannot replace portal data");
                if (world.formatVersion < 11 && existing.get(i).surfaces.stream().anyMatch(o -> "water".equals(o.material))) throw new JSONException("Legacy export cannot replace water data");
                if (world.formatVersion < 28 && existing.get(i).portals.stream().anyMatch(p -> p.destinationWorldId != null))
                    throw new JSONException("Legacy export cannot replace portal data");
                // A legacy file cannot express characters: reject rather than silently erase one.
                if (world.formatVersion < 3 && existing.get(i).showCharacter)
                    throw new JSONException("Legacy export cannot replace a world with a character");
                if (world.formatVersion < 4 && existing.get(i).showCharacter
                        && !"S".equals(existing.get(i).characterFacing))
                    throw new JSONException("Legacy export cannot replace a rotated character");
                if (world.formatVersion < 5 && existing.get(i).hasHouse)
                    throw new JSONException("Legacy export cannot replace a world with a house");
                if (world.formatVersion == 5 && existing.get(i).hasHouse &&
                        (existing.get(i).houseColumn != 0 || existing.get(i).houseRow != 0 || existing.get(i).houseScale != 1.0
                        || !"S".equals(existing.get(i).houseFacing) || !existing.get(i).showHouse))
                    throw new JSONException("Legacy export cannot replace edited house");
                existing.set(i, world);
                found = true;
                break;
            }
        }
        if (!found) {
            if (existing.size() >= 1000) throw new JSONException("Too many worlds");
            existing.add(world);
        }
        return saveWorlds(existing, world.id);
    }

    private boolean saveWorlds(List<World> worlds, String activeId) throws JSONException {
        JSONArray array = new JSONArray();
        for (World item : worlds) {
            JSONObject object = new JSONObject();
            object.put("id", item.id); object.put("name", item.name);
            object.put("description", item.description); object.put("color", item.color);
            JSONArray objects = new JSONArray();
            if (item.showCharacter) {
                JSONObject character = new JSONObject();
                character.put("type", "character"); character.put("column", item.characterColumn);
                character.put("row", item.characterRow); character.put("scale", item.characterScale);
                character.put("facing", item.characterFacing);
                objects.put(character);
            }
            if (item.hasHouse) {
                JSONObject house = new JSONObject();
                house.put("type", "house"); if(item.houseId!=null) house.put("id",item.houseId); house.put("column", item.houseColumn); house.put("row", item.houseRow);
                if(item.houseOpacity>=0)house.put("opacity",item.houseOpacity);
                house.put("scale", item.houseScale); house.put("facing", item.houseFacing); house.put("visible", item.showHouse);
                objects.put(house);
            }
            for (SurfaceObject surface : item.surfaces) objects.put(new JSONObject(surface.raw));
            for (PortalTravel.Portal portal : item.portals) {
                JSONObject p=new JSONObject();p.put("type","fence");p.put("variant","portal");p.put("id",portal.id);
                p.put("column",portal.tile.column);p.put("row",portal.tile.row);p.put("edge",portal.edge);
                if(portal.destinationWorldId!=null){p.put("destinationWorldId",portal.destinationWorldId);p.put("destinationPortalId",portal.destinationPortalId);}
                if(portal.opacity>=0)p.put("opacity",portal.opacity);
                objects.put(p);
            }
            object.put("objects", objects);
            array.put(object);
        }
        return prefs.edit().putString(WORLDS_KEY, array.toString())
                .putString(ACTIVE_KEY, activeId).commit();
    }

    boolean moveCharacter(String worldId, int column, int row, String facing) throws JSONException {
        if (!worldId.equals(activeId()) || column<0 || column>8 || row<0 || row>8 || !java.util.Arrays.asList("N","E","S","W").contains(facing)) return false;
        List<World> worlds=list();
        for(int i=0;i<worlds.size();i++) { World w=worlds.get(i); if(w.id.equals(worldId) && w.showCharacter) {
            worlds.set(i, new World(w.id,w.name,w.description,w.color,true,column,row,w.characterScale,facing,w.hasHouse,w.showHouse,w.houseColumn,w.houseRow,w.houseScale,w.houseFacing,w.houseId,w.houseOpacity,w.formatVersion,w.portals,w.surfaces));
            return saveWorlds(worlds,worldId);
        }}
        return false;
    }
    boolean travel(String sourceWorldId, String portalId) throws JSONException {
        if (!sourceWorldId.equals(activeId())) return false;
        List<World> worlds=list(); World source=null,destination=null; PortalTravel.Portal from=null,to=null;
        for(World w:worlds) if(w.id.equals(sourceWorldId)) source=w;
        if(source==null || !source.showCharacter) return false;
        for(PortalTravel.Portal p:source.portals) if(p.id.equals(portalId)) from=p;
        if(from==null) return false;
        for(World w:worlds) if(w.id.equals(from.destinationWorldId)) destination=w;
        if(destination==null) return false;
        for(PortalTravel.Portal p:destination.portals) if(p.id.equals(from.destinationPortalId)) to=p;
        final World dest=destination;
        PortalTravel.Terrain terrain=new PortalTravel.Terrain() {
            public boolean canStand(int c,int r){if(dest.showHouse && c>=dest.houseColumn && c<=dest.houseColumn+2 && r>=dest.houseRow && r<=dest.houseRow+2) return false;for(SurfaceObject o:dest.surfaces) {if(o.type.equals("tree") && c>=o.column && c<o.column+((o.material.equals("banana") || o.material.equals("palm"))?1:2) && r>=o.row && r<o.row+((o.material.equals("banana") || o.material.equals("palm"))?1:2)) return false;if((o.type.equals("bush")||o.type.equals("sunflower")||o.type.equals("mailbox")||o.type.equals("storage-box")||o.type.equals("selling-box")||(o.type.equals("road") && "water".equals(o.material))) && o.column==c && o.row==r)return false;}return true;}
            public boolean canStep(int a,int b,int c,int d){return true;}
        };
        if(!PortalTravel.canTravel(from,to,new PortalTravel.Tile(source.characterColumn,source.characterRow),terrain)) return false;
        for(int i=0;i<worlds.size();i++) if(worlds.get(i).id.equals(destination.id)) {
            worlds.set(i,new World(dest.id,dest.name,dest.description,dest.color,true,to.tile.column,to.tile.row,source.characterScale,source.characterFacing,dest.hasHouse,dest.showHouse,dest.houseColumn,dest.houseRow,dest.houseScale,dest.houseFacing,dest.houseId,dest.houseOpacity,dest.formatVersion,dest.portals,dest.surfaces)); break;
        }
        return saveWorlds(worlds,dest.id);
    }
}
