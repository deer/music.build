package build.music.instrument;

import build.music.core.Articulation;
import build.music.pitch.SpelledPitch;

import java.util.List;
import java.util.Optional;

import static build.music.instrument.InstrumentFamily.BRASS;
import static build.music.instrument.InstrumentFamily.KEYBOARD;
import static build.music.instrument.InstrumentFamily.STRING;
import static build.music.instrument.InstrumentFamily.WOODWIND;

/**
 * Catalog of standard orchestral instruments with standard ranges.
 * Ranges from standard orchestration references (Adler, Blatter).
 */
public final class Instruments {

    private Instruments() {
    }

    // --- Woodwinds ---

    public static final Instrument PICCOLO = Instrument.of(
        "Piccolo", WOODWIND,
        range("D5", "C8"), range("D5", "A7"),
        72, standardArticulations(), true, null);

    public static final Instrument FLUTE = Instrument.of(
        "Flute", WOODWIND,
        range("C4", "C7"), range("C4", "A6"),
        73, standardArticulations(), false, null);

    public static final Instrument ALTO_FLUTE = Instrument.of(
        "Alto Flute", WOODWIND,
        range("C4", "C7"), range("D4", "G6"),
        73, standardArticulations(), true, null);

    public static final Instrument OBOE = Instrument.of(
        "Oboe", WOODWIND,
        range("Bb3", "A6"), range("C4", "F6"),
        68, standardArticulations(), false, null);

    public static final Instrument ENGLISH_HORN = Instrument.of(
        "English Horn", WOODWIND,
        range("B3", "G6"), range("E4", "C6"),
        69, standardArticulations(), true, null);

    public static final Instrument CLARINET = Instrument.of(
        "Clarinet in Bb", WOODWIND,
        range("E3", "C7"), range("G3", "G6"),
        71, standardArticulations(), true, null);

    public static final Instrument BASS_CLARINET = Instrument.of(
        "Bass Clarinet", WOODWIND,
        range("Bb2", "G5"), range("E3", "C5"),
        71, standardArticulations(), true, null);

    public static final Instrument BASSOON = Instrument.of(
        "Bassoon", WOODWIND,
        range("Bb1", "Eb5"), range("C2", "C5"),
        70, standardArticulations(), false, null);

    public static final Instrument CONTRABASSOON = Instrument.of(
        "Contrabassoon", WOODWIND,
        range("Bb0", "Bb3"), range("C1", "F3"),
        70, standardArticulations(), true, null);

    // --- Brass ---

    public static final Instrument TRUMPET = Instrument.of(
        "Trumpet in Bb", BRASS,
        range("E3", "C6"), range("G3", "G5"),
        56, standardArticulations(), true, null);

    public static final Instrument FRENCH_HORN = Instrument.of(
        "French Horn in F", BRASS,
        range("B1", "F5"), range("C3", "C5"),
        60, standardArticulations(), true, null);

    public static final Instrument TROMBONE = Instrument.of(
        "Trombone", BRASS,
        range("E2", "F5"), range("G2", "Bb4"),
        57, standardArticulations(), false, null);

    public static final Instrument BASS_TROMBONE = Instrument.of(
        "Bass Trombone", BRASS,
        range("Bb1", "Bb4"), range("E2", "G4"),
        57, standardArticulations(), false, null);

    public static final Instrument TUBA = Instrument.of(
        "Tuba", BRASS,
        range("Bb0", "F4"), range("D1", "C4"),
        58, standardArticulations(), false, null);

    // --- Strings ---

    public static final Instrument VIOLIN = Instrument.of(
        "Violin", STRING,
        range("G3", "E7"), range("G3", "A6"),
        40, stringArticulations(), false, null);

    public static final Instrument VIOLA = Instrument.of(
        "Viola", STRING,
        range("C3", "E6"), range("C3", "D6"),
        41, stringArticulations(), false, null);

    public static final Instrument CELLO = Instrument.of(
        "Cello", STRING,
        range("C2", "C6"), range("C2", "E5"),
        42, stringArticulations(), false, null);

    public static final Instrument DOUBLE_BASS = Instrument.of(
        "Double Bass", STRING,
        range("E1", "C5"), range("E1", "G4"),
        43, stringArticulations(), false, null);

    public static final Instrument HARP = Instrument.of(
        "Harp", STRING,
        range("B0", "G#7"), range("C1", "G7"),
        46, List.of(Articulation.NORMAL, Articulation.STACCATO), false, null);

    // --- Keyboard ---

    public static final Instrument PIANO = Instrument.of(
        "Piano", KEYBOARD,
        range("A0", "C8"), range("C2", "C7"),
        0, keyboardArticulations(), false, null);

    public static final Instrument ORGAN = Instrument.of(
        "Organ", KEYBOARD,
        range("C2", "C7"), range("C2", "C7"),
        19, keyboardArticulations(), false, null);

    public static final Instrument HARPSICHORD = Instrument.of(
        "Harpsichord", KEYBOARD,
        range("F1", "F6"), range("C2", "F6"),
        6, keyboardArticulations(), false, null);

    public static final Instrument CELESTA = Instrument.of(
        "Celesta", KEYBOARD,
        range("C4", "C8"), range("C4", "C8"),
        8, keyboardArticulations(), true, null);

    // --- Lookup methods ---

    private static final List<Instrument> ALL = List.of(
        PICCOLO, FLUTE, ALTO_FLUTE, OBOE, ENGLISH_HORN, CLARINET, BASS_CLARINET,
        BASSOON, CONTRABASSOON,
        TRUMPET, FRENCH_HORN, TROMBONE, BASS_TROMBONE, TUBA,
        VIOLIN, VIOLA, CELLO, DOUBLE_BASS, HARP,
        PIANO, ORGAN, HARPSICHORD, CELESTA
    );

    public static Optional<Instrument> byName(final String name) {
        return ALL.stream()
            .filter(i -> i.name().equalsIgnoreCase(name))
            .findFirst();
    }

    public static Optional<Instrument> byMidiProgram(final int program) {
        return ALL.stream()
            .filter(i -> i.midiProgram() == program)
            .findFirst();
    }

    public static List<Instrument> byFamily(final InstrumentFamily family) {
        return ALL.stream()
            .filter(i -> i.family() == family)
            .toList();
    }

    public static List<Instrument> all() {
        return ALL;
    }

    // --- Factory helpers ---

    private static PitchRange range(final String low, final String high) {
        return PitchRange.of(SpelledPitch.parse(low), SpelledPitch.parse(high));
    }

    static List<Articulation> standardArticulations() {
        return List.of(Articulation.NORMAL, Articulation.STACCATO, Articulation.ACCENT,
            Articulation.TENUTO);
    }

    static List<Articulation> stringArticulations() {
        return List.of(Articulation.NORMAL, Articulation.STACCATO, Articulation.ACCENT,
            Articulation.TENUTO, Articulation.LEGATO, Articulation.PORTATO);
    }

    static List<Articulation> keyboardArticulations() {
        return List.of(Articulation.NORMAL, Articulation.STACCATO, Articulation.TENUTO);
    }
}
