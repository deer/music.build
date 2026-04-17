package build.music.pitch;

public record EqualTemperament(double referenceFrequency, int referenceMidi) implements Tuning {

    public static EqualTemperament standard() {
        return new EqualTemperament(440.0, 69);
    }

    @Override
    public double frequency(final int midiNumber) {
        return referenceFrequency * Math.pow(2.0, (midiNumber - referenceMidi) / 12.0);
    }

    @Override
    public String name() {
        return "12-TET (" + referenceFrequency + " Hz)";
    }
}
