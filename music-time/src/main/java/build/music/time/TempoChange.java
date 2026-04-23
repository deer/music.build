package build.music.time;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;

import java.lang.invoke.MethodHandles;

/**
 * A gradual tempo change spanning a range of bars.
 *
 * <p>The {@code fromBpm} is resolved at build time from the composition's tempo at
 * {@code startBar}. {@code toBpm} is the target reached at {@code endBar}.
 *
 * <p>Curve options:
 * <ul>
 *   <li>"linear" — BPM changes by equal steps each bar</li>
 *   <li>"exponential" — BPM change accelerates (more natural for ritardando)</li>
 * </ul>
 */
public record TempoChange(int startBar, int endBar, int fromBpm, int toBpm, String curve) {

    @Unmarshal
    public TempoChange {
        if (startBar < 1) {
            throw new IllegalArgumentException("startBar must be ≥ 1, got: " + startBar);
        }
        if (endBar < startBar) {
            throw new IllegalArgumentException("endBar must be ≥ startBar");
        }
        if (fromBpm < 1 || fromBpm > 400) {
            throw new IllegalArgumentException("fromBpm out of range: " + fromBpm);
        }
        if (toBpm < 1 || toBpm > 400) {
            throw new IllegalArgumentException("toBpm out of range: " + toBpm);
        }
        if (!"linear".equals(curve) && !"exponential".equals(curve)) {
            throw new IllegalArgumentException("curve must be 'linear' or 'exponential', got: " + curve);
        }
    }

    @Marshal
    public void destructor(final Out<Integer> startBar,
                           final Out<Integer> endBar,
                           final Out<Integer> fromBpm,
                           final Out<Integer> toBpm,
                           final Out<String> curve) {
        startBar.set(this.startBar);
        endBar.set(this.endBar);
        fromBpm.set(this.fromBpm);
        toBpm.set(this.toBpm);
        curve.set(this.curve);
    }

    static {
        Marshalling.register(TempoChange.class, MethodHandles.lookup());
    }

    /**
     * Whether this is a deceleration (ritardando).
     */
    public boolean isDecelerating() {
        return toBpm < fromBpm;
    }

    /**
     * Returns one interpolated BPM per bar, from startBar to endBar inclusive.
     * Result length = endBar - startBar + 1.
     */
    public int[] interpolatedBpms() {
        final int barCount = endBar - startBar + 1;
        final int[] bpms = new int[barCount];
        for (int i = 0; i < barCount; i++) {
            final double t = barCount == 1 ? 1.0 : (double) i / (barCount - 1); // 0.0 → 1.0
            final double tCurved = "exponential".equals(curve) ? t * t : t;
            bpms[i] = (int) Math.round(fromBpm + tCurved * (toBpm - fromBpm));
        }
        return bpms;
    }
}
