package dev.retrobit.gtlauncher;

import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Approximate mature-tree stages from a complete UTC weather archive, matching preview. */
final class TreePhenology {
    private static final String[] TYPES={"apple","pear","orange","mango","guava","starfruit","banana","palm"};
    private static final int[][] DURATIONS={{14,130,28},{14,140,28},{21,240,45},{21,120,28},{21,120,14},{14,85,21},{21,120,30},{30,330,90}};
    static final class Result {
        final Map<String,String> stages,fruitCycles;
        final LocalDate lastDate;
        Result(Map<String,String> stages,Map<String,String> cycles,LocalDate lastDate){this.stages=Collections.unmodifiableMap(stages);this.fruitCycles=Collections.unmodifiableMap(cycles);this.lastDate=lastDate;}
    }
    private static final class HourCount {int hours,chill;}
    private static double number(JSONArray array,int index) throws Exception {
        Object value=array.get(index);if(!(value instanceof Number))throw new Exception("Missing weather value");
        double n=((Number)value).doubleValue();if(!Double.isFinite(n))throw new Exception("Invalid weather value");return n;
    }
    private static LocalDate date(String value) throws Exception {
        if(value==null || !value.matches("\\d{4}-\\d{2}-\\d{2}"))throw new Exception("Invalid date");
        try {LocalDate d=LocalDate.parse(value);if(!d.toString().equals(value))throw new Exception("Invalid date");return d;}catch(java.time.DateTimeException e){throw new Exception("Invalid date",e);}
    }
    static Result calculate(JSONObject input,LocalDate today,double latitude) throws Exception {
        if(today==null || !Double.isFinite(latitude))throw new Exception("Invalid location or date");
        JSONObject daily=input.getJSONObject("daily"),hourly=input.getJSONObject("hourly");
        JSONArray dates=daily.getJSONArray("time"),rain=daily.getJSONArray("precipitation_sum"),temps=daily.getJSONArray("temperature_2m_mean"),hours=hourly.getJSONArray("time"),hourTemps=hourly.getJSONArray("temperature_2m");
        int n=dates.length();if(n<600 || rain.length()!=n || temps.length()!=n || hours.length()!=hourTemps.length())throw new Exception("Incomplete weather archive");
        LocalDate first=date(dates.getString(0)),last=date(dates.getString(n-1));long freshness=ChronoUnit.DAYS.between(last,today);if(freshness<0 || freshness>9)throw new Exception("Stale weather archive");
        Map<LocalDate,HourCount> counts=new HashMap<>();
        for(int i=0;i<hours.length();i++){
            String h=hours.getString(i);if(h.length()<10)throw new Exception("Invalid hour");LocalDate d=date(h.substring(0,10));double t=number(hourTemps,i);
            HourCount count=counts.computeIfAbsent(d,k->new HourCount());count.hours++;if(t>=0 && t<=7.2)count.chill++;
        }
        double[] precipitation=new double[n],temperature=new double[n];int[] chill=new int[n];LocalDate[] days=new LocalDate[n];
        for(int i=0;i<n;i++){
            LocalDate d=date(dates.getString(i));if(!d.equals(first.plusDays(i)))throw new Exception("Non-contiguous dates");
            double r=number(rain,i),t=number(temps,i);HourCount c=counts.get(d);
            if(r<0 || c==null || c.hours!=24)throw new Exception("Incomplete daily weather");
            days[i]=d;precipitation[i]=r;temperature[i]=t;chill[i]=c.chill;
        }
        LocalDate[] bloom=new LocalDate[TYPES.length];Integer winterYear=null;int chilled=0;boolean[] triggered=new boolean[2];Long[] tropicalPeriod={null,null};
        for(int i=45;i<n;i++) {
            LocalDate d=days[i];int resetMonth=latitude>=0?8:2,season=d.getYear()-(d.getMonthValue()<resetMonth?1:0);
            if(winterYear==null || winterYear!=season){winterYear=season;chilled=0;triggered=new boolean[2];}
            chilled+=chill[i];boolean warm=true;for(int j=i-6;j<=i;j++)if(temperature[j]<10)warm=false;
            for(int j=0;j<2;j++)if(!triggered[j] && chilled>=(j==0?600:450) && warm){bloom[j]=d;triggered[j]=true;}
            double dry=0,wet=0,monthly=0;for(int j=i-44;j<i-6;j++)dry+=precipitation[j];for(int j=i-6;j<=i;j++)wet+=precipitation[j];for(int j=i-29;j<=i;j++)monthly+=precipitation[j];
            if(dry<=60 && wet>=20 && temperature[i]>=18)for(int j=2;j<=3;j++)if(bloom[j]==null || ChronoUnit.DAYS.between(bloom[j],d)>=300)bloom[j]=d;
            if(temperature[i]>=20 && monthly>=60)for(int j=4;j<=5;j++)if(bloom[j]==null || ChronoUnit.DAYS.between(bloom[j],d)>=180)bloom[j]=d;
            for(int j=0;j<2;j++){
                long period=Math.floorDiv(d.toEpochDay(),j==0?240:480);
                if(temperature[i]>=22 && monthly>=60 && (tropicalPeriod[j]==null || tropicalPeriod[j]!=period)){bloom[j+6]=d;tropicalPeriod[j]=period;}
            }
        }
        Map<String,String> stages=new LinkedHashMap<>(),cycles=new LinkedHashMap<>();
        for(int i=0;i<TYPES.length;i++) {
            long age=bloom[i]==null?Long.MAX_VALUE:ChronoUnit.DAYS.between(bloom[i],today);int[] duration=DURATIONS[i];
            String stage=age<0?"vegetative":age<duration[0]?"flower":age<duration[1]?"fruit":age<duration[1]+duration[2]?"ripe":"vegetative";
            stages.put(TYPES[i],stage);cycles.put(TYPES[i],bloom[i]==null || age<0?null:bloom[i].toString());
        }
        return new Result(stages,cycles,last);
    }
}
