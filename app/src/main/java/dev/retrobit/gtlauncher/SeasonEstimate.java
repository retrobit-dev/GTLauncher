package dev.retrobit.gtlauncher;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Locale;

/** Rainfall-only, non-official estimate based on three complete calendar dasarian. */
final class SeasonEstimate {
    private static final class Period {
        LocalDate first, last;
        double sum;
        int part;
        boolean complete() {
            int start = part == 1 ? 1 : part == 2 ? 11 : 21;
            int end = part == 1 ? 10 : part == 2 ? 20 : last.lengthOfMonth();
            return first.getDayOfMonth() == start && last.getDayOfMonth() == end;
        }
    }
    static String fetch(double latitude, double longitude) throws Exception {
        LocalDate today=LocalDate.now(ZoneOffset.UTC);
        String url=String.format(Locale.US, "https://archive-api.open-meteo.com/v1/archive?latitude=%.5f&longitude=%.5f&start_date=%s&end_date=%s&daily=precipitation_sum&timezone=auto", latitude, longitude, today.minusDays(550), today.minusDays(7));
        HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();
        connection.setConnectTimeout(9000); connection.setReadTimeout(9000);
        try {
            if(connection.getResponseCode()!=200) throw new Exception("Rainfall unavailable");
            StringBuilder json=new StringBuilder();
            try(BufferedReader reader=new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line; while((line=reader.readLine())!=null) {json.append(line); if(json.length()>500000) throw new Exception("Response too large");}
            }
            return classify(new JSONObject(json.toString()),today);
        } finally {connection.disconnect();}
    }
    static String classify(JSONObject data, LocalDate today) throws Exception {
        JSONObject daily=data.getJSONObject("daily"); JSONArray dates=daily.getJSONArray("time"), rain=daily.getJSONArray("precipitation_sum");
        if(dates.length()<60||dates.length()!=rain.length()) throw new Exception("Incomplete rainfall");
        ArrayList<Period> periods=new ArrayList<>(); LocalDate previous=null;
        for(int i=0;i<dates.length();i++) {
            LocalDate d=LocalDate.parse(dates.getString(i)); Object value=rain.get(i);
            if(!(value instanceof Number)) throw new Exception("Missing rainfall");
            double n=((Number)value).doubleValue();
            if(!Double.isFinite(n)||n<0||(previous!=null&&!d.equals(previous.plusDays(1)))) throw new Exception("Invalid rainfall");
            previous=d; int part=d.getDayOfMonth()<=10?1:d.getDayOfMonth()<=20?2:3;
            Period p=periods.isEmpty()?null:periods.get(periods.size()-1);
            if(p==null||p.part!=part||p.first.getMonthValue()!=d.getMonthValue()||p.first.getYear()!=d.getYear()){p=new Period();p.first=d;p.part=part;periods.add(p);}
            p.last=d;p.sum+=n;
        }
        if(previous.isAfter(today)||previous.isBefore(today.minusDays(9))) throw new Exception("Stale rainfall");
        ArrayList<Period> complete=new ArrayList<>();for(Period p:periods) if(p.complete())complete.add(p);
        String season="unknown";
        for(int i=2;i<complete.size();i++) {
            boolean wet=true,dry=true;for(int j=i-2;j<=i;j++){wet &= complete.get(j).sum>=50;dry &= complete.get(j).sum<50;}
            if(wet)season="wet";else if(dry)season="dry";
        }
        return season;
    }
}
