package tr.olgun.alkis;
import java.util.Random;
class DetectorTest {
 static int run(int[] hits,int duration){
  Claps c=new Claps(); Random rand=new Random(1); int count=0;
  for(int t=0;t<3000;t+=10){
   boolean clap=false;for(int h:hits) if(t>=h && t<h+duration)clap=true;
   short[] f=new short[160];for(int i=0;i<f.length;i++) f[i]=(short)(rand.nextGaussian()*(clap?2600:60));
   if(c.accept(f,t))count++;
  }return count;
 }
 public static void main(String[] args){
  if(run(new int[]{600,1000},40)!=1 || run(new int[]{600},40)!=0 || run(new int[]{600,2000},40)!=0 || run(new int[]{600,1000},250)!=0 ||run(new int[]{},0)!=0) throw new AssertionError();
  System.out.println("5 detector scenarios passed");
 }
}
