package dev.retrobit.gtlauncher;

/** Timed decorative bow, reset by movement, a world/tile change or disabled animation. */
final class PlayerIdleBow {
    static final long DELAY = 3000, DURATION = 2000, INTERVAL = 3000;
    private String key;
    private long since;
    private static float smooth(float t) { t=Math.max(0,Math.min(1,t));return t*t*(3-2*t); }
    static float amount(long elapsed) {
        if(elapsed<DELAY)return 0;
        long t=(elapsed-DELAY)%INTERVAL;
        if(t>=DURATION)return 0;
        if(t<500)return smooth(t/500f);
        if(t<1500)return 1;
        return 1-smooth((t-1500)/500f);
    }
    static int frame(float value) { return value<.2f?0:value<.75f?1:2; }
    float update(long now, String nextKey, boolean enabled) {
        if(!enabled || nextKey==null) { key=null;since=now;return 0; }
        if(!nextKey.equals(key)||now<since) { key=nextKey;since=now;return 0; }
        return amount(now-since);
    }
    void reset() { key=null; }
}
