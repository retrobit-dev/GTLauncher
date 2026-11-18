package dev.retrobit.gtlauncher;

/** Timed decorative bow, reset by movement, a world/tile change or disabled animation. */
final class PlayerIdleBow {
    static final long DELAY = 20000, DURATION = 4000;
    private String key;
    private long since;
    private static float smooth(float t) { t=Math.max(0,Math.min(1,t));return t*t*(3-2*t); }
    static float amount(long elapsed) {
        if(elapsed<DELAY)return 0;
        long t=(elapsed-DELAY)%(DELAY+DURATION);
        if(t>=DURATION)return 0;
        if(t<1000)return smooth(t/1000f);
        if(t<3000)return 1;
        return 1-smooth((t-3000)/1000f);
    }
    float update(long now, String nextKey, boolean enabled) {
        if(!enabled || nextKey==null) { key=null;since=now;return 0; }
        if(!nextKey.equals(key)||now<since) { key=nextKey;since=now;return 0; }
        return amount(now-since);
    }
    void reset() { key=null; }
}
