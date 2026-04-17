package build.music.pitch;

public enum PitchClass {
    C, CS, D, DS, E, F, FS, G, GS, A, AS, B;

    public int semitone() {
        return ordinal();
    }

    public static PitchClass of(final int semitone) {
        return values()[((semitone % 12) + 12) % 12];
    }
}
