package tr.olgun.alkis;

public class PickupTest {
    static boolean simulate(int mode){
        Pickup pickup=new Pickup();
        for(int i=0;i<100;i++){
            float x=0,z=9.81f;
            if(mode==0)x=i%2==0?0.45f:-0.45f;
            if(mode==1 && i==20)x=5;
            if(mode==2 && i>20)x=Math.min(4,(i-20)*0.12f);
            if(mode==3 && i>20 && i<36)z+=2;
            if(pickup.moved(i*20_000_000L,x,0,z))return true;
        }
        return false;
    }
    public static void main(String[] args){
        if(simulate(0)||simulate(1)||!simulate(2)||!simulate(3))throw new AssertionError();
    }
}
