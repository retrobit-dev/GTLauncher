package dev.retrobit.gtlauncher;
public class SkyBlendCheck {
 public static void main(String[] args) {
  for(double x=-22;x<15;x+=.1){double[] a=SkyBlend.weights(x),b=SkyBlend.weights(x+.001);if(Math.abs(a[0]+a[1]+a[2]-1)>1e-12)throw new AssertionError();for(int i=0;i<3;i++)if(Math.abs(a[i]-b[i])>.001)throw new AssertionError();}
  if(SkyBlend.weights(-20)[0]!=1 || SkyBlend.weights(20)[2]!=1)throw new AssertionError();System.out.println("Android continuous sky blend: OK");
 }
}
