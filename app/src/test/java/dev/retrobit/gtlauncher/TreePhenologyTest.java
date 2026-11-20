package dev.retrobit.gtlauncher;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.time.LocalDate;
import java.util.Locale;
import static org.junit.Assert.*;

public class TreePhenologyTest {
    private static final LocalDate TODAY=LocalDate.parse("2026-11-08");
    private JSONObject dataset(int coldEnd,int wetEnd,int warmStart,boolean hot) throws Exception {
        JSONArray dates=new JSONArray(),rain=new JSONArray(),means=new JSONArray(),hours=new JSONArray(),hourly=new JSONArray();
        for(int offset=746;offset>=7;offset--){
            String date=TODAY.minusDays(offset).toString();boolean cold=coldEnd>=0 && offset>=coldEnd && offset<coldEnd+25 || warmStart>=0 && offset>warmStart;
            int rainfall=3;if(wetEnd>=0){if(offset>=wetEnd+7 && offset<wetEnd+45)rainfall=0;if(offset>=wetEnd && offset<wetEnd+7)rainfall=4;}
            int temperature=cold?4:hot?26:20;dates.put(date);rain.put(rainfall);means.put(temperature);
            for(int h=0;h<24;h++){hours.put(date+String.format(Locale.ROOT,"T%02d:00",h));hourly.put(temperature);}
        }
        return new JSONObject().put("daily",new JSONObject().put("time",dates).put("precipitation_sum",rain).put("temperature_2m_mean",means)).put("hourly",new JSONObject().put("time",hours).put("temperature_2m",hourly));
    }
    @Test public void agreesWithPreviewForChillDroughtAndTropics() throws Exception {
        TreePhenology.Result cold=TreePhenology.calculate(dataset(150,-1,-1,false),TODAY,10);
        assertEquals("ripe",cold.stages.get("apple"));assertEquals("ripe",cold.stages.get("pear"));assertEquals("2026-06-18",cold.fruitCycles.get("apple"));assertEquals("2026-06-12",cold.fruitCycles.get("guava"));assertNull(cold.fruitCycles.get("banana"));
        TreePhenology.Result rain=TreePhenology.calculate(dataset(-1,130,-1,false),TODAY,-6);
        assertEquals("ripe",rain.stages.get("mango"));assertEquals("fruit",rain.stages.get("orange"));assertEquals("2026-06-29",rain.fruitCycles.get("mango"));assertEquals("2026-07-12",rain.fruitCycles.get("guava"));
        TreePhenology.Result warm=TreePhenology.calculate(dataset(-1,-1,90,false),TODAY,-6);
        assertEquals("ripe",warm.stages.get("starfruit"));assertEquals("2026-08-10",warm.fruitCycles.get("starfruit"));assertEquals("2026-08-16",warm.fruitCycles.get("apple"));
        TreePhenology.Result tropical=TreePhenology.calculate(dataset(-1,-1,350,true),TODAY,-6);
        assertEquals("ripe",tropical.stages.get("banana"));assertEquals("fruit",tropical.stages.get("palm"));assertEquals("2026-07-06",tropical.fruitCycles.get("banana"));assertEquals("2026-07-06",tropical.fruitCycles.get("palm"));
    }
    @Test public void rejectsMissingStaleOrInvalidWeather() throws Exception {
        JSONObject data=dataset(-1,-1,-1,true);data.getJSONObject("hourly").getJSONArray("temperature_2m").put(42,JSONObject.NULL);
        try{TreePhenology.calculate(data,TODAY,0);fail("missing hourly data");}catch(Exception expected){}
        try{TreePhenology.calculate(dataset(-1,-1,-1,false),TODAY.plusDays(12),0);fail("stale");}catch(Exception expected){}
        JSONObject invalid=dataset(-1,-1,-1,false);invalid.getJSONObject("daily").getJSONArray("precipitation_sum").put(30,-1);
        try{TreePhenology.calculate(invalid,TODAY,0);fail("negative rain");}catch(Exception expected){}
        JSONObject gaps=dataset(-1,-1,-1,false);gaps.getJSONObject("daily").getJSONArray("time").put(10,"2025-01-01");
        try{TreePhenology.calculate(gaps,TODAY,0);fail("date gap");}catch(Exception expected){}
    }
    @Test public void slidingWindowPreservesTropicalCycle() throws Exception {
        JSONObject full=dataset(-1,-1,-1,true);TreePhenology.Result first=TreePhenology.calculate(full,TODAY,-6);
        JSONObject daily=full.getJSONObject("daily"),hourly=full.getJSONObject("hourly");
        for(String key:new String[]{"time","precipitation_sum","temperature_2m_mean"})daily.getJSONArray(key).remove(0);
        for(String key:new String[]{"time","temperature_2m"})for(int i=0;i<24;i++)hourly.getJSONArray(key).remove(0);
        TreePhenology.Result second=TreePhenology.calculate(full,TODAY,-6);assertEquals(first.fruitCycles.get("banana"),second.fruitCycles.get("banana"));assertEquals(first.fruitCycles.get("palm"),second.fruitCycles.get("palm"));
    }
}
