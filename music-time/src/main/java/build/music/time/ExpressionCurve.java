package build.music.time;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;

import java.lang.invoke.MethodHandles;

/**
 * A gradual expression (CC 11) change spanning a range of bars.
 *
 * <p>Values are in the MIDI CC range 0–127.
 * Curve options: "linear" or "exponential".
 */
public record ExpressionCurve(int startBar, int endBar, int fromValue, int toValue, String curve) {

    @Unmarshal
    public ExpressionCurve {
        if (startBar < 1) {
            throw new IllegalArgumentException("startBar must be ≥ 1, got: " + startBar);
        }
        if (endBar < startBar) {
            throw new IllegalArgumentException("endBar must be ≥ startBar");
        }
        if (fromValue < 0 || fromValue > 127) {
            throw new IllegalArgumentException("fromValue out of range 0–127: " + fromValue);
        }
        if (toValue < 0 || toValue > 127) {
            throw new IllegalArgumentException("toValue out of range 0–127: " + toValue);
        }
        if (!"linear".equals(curve) && !"exponential".equals(curve)) {
            throw new IllegalArgumentException("curve must be 'linear' or 'exponential', got: " + curve);
        }
    }

    @Marshal
    public void destructor(final Out<Integer> startBar,
                           final Out<Integer> endBar,
                           final Out<Integer> fromValue,
                           final Out<Integer> toValue,
                           final Out<String> curve) {
        startBar.set(this.startBar);
        endBar.set(this.endBar);
        fromValue.set(this.fromValue);
        toValue.set(this.toValue);
        curve.set(this.curve);
    }

    static {
        Marshalling.register(ExpressionCurve.class, MethodHandles.lookup());
    }

    /**
     * Returns one interpolated CC value per bar, from startBar to endBar inclusive.
     * Result length = endBar - startBar + 1.
     */
    public int[] interpolatedValues() {
        final int barCount = endBar - startBar + 1;
        final int[] values = new int[barCount];
        for (int i = 0; i < barCount; i++) {
            final double t = barCount == 1 ? 1.0 : (double) i / (barCount - 1);
            final double tCurved = "exponential".equals(curve) ? t * t : t;
            values[i] = (int) Math.round(fromValue + tCurved * (toValue - fromValue));
        }
        return values;
    }
}
