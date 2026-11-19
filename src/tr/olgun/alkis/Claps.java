package tr.olgun.alkis;

/** Processes ten-millisecond frames of mono audio at 16 kHz. */
final class Claps {
    private double floor = 100;
    private long onset = -1, first = -1, lastLoud = -100;
    private int frames;
    boolean accept(short[] samples, long now) {
        double sum = 0, edge = 0;
        int peak = 0;
        for (int i = 0; i < samples.length; i++) {
            int s = samples[i];
            sum += (double)s*s;
            peak = Math.max(peak, Math.abs(s));
            if (i > 0) {
                int d = s - samples[i-1];
                edge += (double)d*d;
            }
        }
        double rms = Math.sqrt(sum/samples.length);
        boolean loud = rms > Math.max(380, floor*2.7);
        frames++;
        if (loud) {
            if (now-lastLoud > 45 && peak > 1400 && edge > sum*0.12) onset = now;
            lastLoud = now;
        } else {
            floor += (Math.min(rms, floor*3+100)-floor)*0.035;
            if (onset >= 0 && now-lastLoud >= 45) {
                long hit = onset;
                onset = -1;
                if (frames > 35 && lastLoud-hit <= 140) {
                    long gap = hit-first;
                    if (first >= 0 && gap >= 170 && gap <= 1000) {
                        first = -1;
                        return true;
                    }
                    if (first < 0 || gap >= 170) first = hit;
                }
            }
        }
        return false;
    }
}
