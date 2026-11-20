package dev.retrobit.gtlauncher;
/** Continuous solar elevation weights; the three values always sum to one. */
final class SkyBlend {
    private static double smooth(double low,double high,double n){double t=Math.max(0,Math.min(1,(n-low)/(high-low)));return t*t*(3-2*t);}
    static double[] weights(double altitude){double rise=smooth(-18,-6,altitude),day=smooth(-2,12,altitude);return new double[]{1-rise,rise*(1-day),day};}
    private SkyBlend(){}
}
