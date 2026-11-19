package tr.olgun.alkis;

/** Detect sustained acceleration or tilt relative to the phone's resting pose. */
final class Pickup {
    private boolean initialized;
    private double restX,restY,restZ,x,y,z;
    private long previous, movingSince=-1;
    boolean moved(long timestamp,float ax,float ay,float az){
        if(!initialized){
            restX=x=ax;restY=y=ay;restZ=z=az;
            initialized=true;previous=timestamp;return false;
        }
        double seconds=(timestamp-previous)*1e-9;
        previous=timestamp;
        if(seconds<=0 || seconds>0.25) movingSince=-1;
        double weight=1-Math.exp(-Math.max(0,seconds)/0.055);
        x+=weight*(ax-x);y+=weight*(ay-y);z+=weight*(az-z);
        double dx=x-restX,dy=y-restY,dz=z-restZ;
        if(dx*dx+dy*dy+dz*dz<1.0){movingSince=-1;return false;}
        if(movingSince<0)movingSince=timestamp;
        return timestamp-movingSince>=120_000_000L;
    }
}
