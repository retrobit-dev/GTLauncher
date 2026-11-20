package dev.retrobit.gtlauncher;

/** Timed decorative bow, reset by movement, a world/tile change or disabled animation. */
final class PlayerIdleBow {
    static final long DELAY = 8000, DURATION = 2000, INTERVAL = 8000, SIT_DELAY = 30000, SIT_TRANSITION = 1800;
    private String key;
    private long since, elapsed, carry;
    private static float smooth(float t) { t=Math.max(0,Math.min(1,t));return t*t*(3-2*t); }
    static float amount(long elapsed) {
        if(elapsed<DELAY)return 0;
        long t=(elapsed-DELAY)%INTERVAL;
        if(t>=DURATION)return 0;
        if(t<500)return smooth(t/500f);
        if(t<1500)return 1;
        return 1-smooth((t-1500)/500f);
    }
    static float seatGroundWeight(float sitting) { return smooth(sitting*3-1); }
    static int frame(float value) { return value<.2f?0:value<.75f?1:2; }
    float update(long now, String nextKey, boolean enabled, boolean allowSitting) {
        if(!enabled || nextKey==null) { key=null;since=now;elapsed=0;carry=0;return 0; }
        if(!nextKey.equals(key)||now<since) { key=nextKey;since=now-carry;elapsed=carry;carry=0;return allowSitting && elapsed>=SIT_DELAY?0:amount(elapsed); }
        elapsed=now-since;
        return allowSitting && elapsed>=SIT_DELAY?0:amount(elapsed);
    }
    float sitting() { return key==null?0:smooth((elapsed-SIT_DELAY)/(float)SIT_TRANSITION); }
    long elapsed() { return elapsed; }
    void resume(long value) { key=null;elapsed=0;carry=Math.max(0,value); }
    void reset() { key=null;elapsed=0;carry=0; }
}
