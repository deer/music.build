package build.music.harmony;

import build.base.marshalling.Marshalling;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;

import java.util.List;

@Singular
public enum ScaleType implements Trait {
    MAJOR(List.of(2, 2, 1, 2, 2, 2, 1)),
    NATURAL_MINOR(List.of(2, 1, 2, 2, 1, 2, 2)),
    HARMONIC_MINOR(List.of(2, 1, 2, 2, 1, 3, 1)),
    MELODIC_MINOR(List.of(2, 1, 2, 2, 2, 2, 1)),
    DORIAN(List.of(2, 1, 2, 2, 2, 1, 2)),
    PHRYGIAN(List.of(1, 2, 2, 2, 1, 2, 2)),
    LYDIAN(List.of(2, 2, 2, 1, 2, 2, 1)),
    MIXOLYDIAN(List.of(2, 2, 1, 2, 2, 1, 2)),
    AEOLIAN(List.of(2, 1, 2, 2, 1, 2, 2)),
    LOCRIAN(List.of(1, 2, 2, 1, 2, 2, 2)),
    PENTATONIC_MAJOR(List.of(2, 2, 3, 2, 3)),
    PENTATONIC_MINOR(List.of(3, 2, 2, 3, 2)),
    BLUES(List.of(3, 2, 1, 1, 3, 2)),
    CHROMATIC(List.of(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)),
    WHOLE_TONE(List.of(2, 2, 2, 2, 2, 2));

    private final List<Integer> intervals;

    ScaleType(final List<Integer> intervals) {
        this.intervals = List.copyOf(intervals);
    }

    /**
     * Number of degrees in this scale.
     */
    public int degreeCount() {
        return intervals.size();
    }

    /**
     * The semitone intervals between consecutive scale degrees.
     */
    public List<Integer> intervals() {
        return intervals;
    }

    static {
        Marshalling.registerEnum(ScaleType.class);
    }
}
