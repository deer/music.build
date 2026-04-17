package build.music.pitch;

public interface Pitch {
    SpelledPitch spelled();
    int midi();
    double frequency(Tuning tuning);
    PitchClass pitchClass();
}
