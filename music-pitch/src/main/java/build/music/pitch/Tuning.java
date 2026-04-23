package build.music.pitch;

public interface Tuning {
    double frequency(int midiNumber);

    String name();
}
