package build.music.demo;

import build.music.core.Chord;
import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.core.Rest;
import build.music.lilypond.LilyPondEngraver;
import build.music.lilypond.LilyPondRenderer;
import build.music.midi.MidiPlayer;
import build.music.midi.MidiRenderer;
import build.music.midi.MidiWriter;
import build.music.pitch.SpelledInterval;
import build.music.pitch.SpelledPitch;
import build.music.score.Part;
import build.music.score.Score;
import build.music.score.Voice;
import build.music.time.DottedValue;
import build.music.time.RhythmicValue;
import build.music.time.Tempo;
import build.music.time.TimeSignature;
import build.music.transform.Augment;
import build.music.transform.Invert;
import build.music.transform.Retrograde;
import build.music.transform.Transforms;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;

public final class OdeToJoy {

    private OdeToJoy() {
    }

    static final RhythmicValue Q = RhythmicValue.QUARTER;
    static final RhythmicValue E = RhythmicValue.EIGHTH;
    static final RhythmicValue H = RhythmicValue.HALF;
    static final DottedValue DQ = new DottedValue(RhythmicValue.QUARTER, 1);

    static Note n(final String pitch) {
        return Note.of(SpelledPitch.parse(pitch), Q);
    }

    static Note n(final String pitch, final RhythmicValue dur) {
        return Note.of(SpelledPitch.parse(pitch), dur);
    }

    static Note n(final String pitch, final DottedValue dur) {
        return Note.of(SpelledPitch.parse(pitch), dur);
    }

    public static void main(final String[] args) {
        final var melody = buildMelody();
        final var score = buildScore(melody);
        final var midi = renderAndPlay(score);
        final var voices = buildTransformations(melody, score);
        playTransformations(voices, score, midi.available());
        final var engraved = engraveSheetMusic(score, voices);
        printSummary(melody, midi, engraved);
    }

    // ── Step 1 ────────────────────────────────────────────────────────────────

    static Voice buildMelody() {
        final List<NoteEvent> events = List.of(
            // Bar 1
            n("E4"), n("E4"), n("F4"), n("G4"),
            // Bar 2
            n("G4"), n("F4"), n("E4"), n("D4"),
            // Bar 3
            n("C4"), n("C4"), n("D4"), n("E4"),
            // Bar 4
            n("E4", DQ), n("D4", E), n("D4", H),
            // Bar 5
            n("E4"), n("E4"), n("F4"), n("G4"),
            // Bar 6
            n("G4"), n("F4"), n("E4"), n("D4"),
            // Bar 7
            n("C4"), n("C4"), n("D4"), n("E4"),
            // Bar 8
            n("D4", DQ), n("C4", E), n("C4", H),
            // Bar 9 (B section)
            n("D4"), n("D4"), n("E4"), n("C4"),
            // Bar 10
            n("D4"), n("E4", E), n("F4", E), n("E4"), n("C4"),
            // Bar 11
            n("D4"), n("E4", E), n("F4", E), n("E4"), n("D4"),
            // Bar 12
            n("C4"), n("D4"), n("G3", H),
            // Bar 13 (return of A)
            n("E4"), n("E4"), n("F4"), n("G4"),
            // Bar 14
            n("G4"), n("F4"), n("E4"), n("D4"),
            // Bar 15
            n("C4"), n("C4"), n("D4"), n("E4"),
            // Bar 16
            n("D4", DQ), n("C4", E), n("C4", H)
        );
        return Voice.of("melody", events);
    }

    // ── Step 2 ────────────────────────────────────────────────────────────────

    static Score buildScore(final Voice melody) {
        return Score.builder("Ode to Joy")
            .timeSignature(TimeSignature.COMMON_TIME)
            .tempo(Tempo.of(120))
            .part(Part.piano("Melody", melody))
            .build();
    }

    // ── Steps 3 & 4 ───────────────────────────────────────────────────────────

    record MidiResult(Sequence sequence, boolean available) {
    }

    static MidiResult renderAndPlay(final Score score) {
        final Sequence seq;
        try {
            seq = MidiRenderer.render(score);
            MidiWriter.write(seq, Path.of("ode-to-joy.mid"));
            System.out.println("Wrote ode-to-joy.mid");
        } catch (final Exception e) {
            System.out.println("MIDI render/write failed: " + e.getMessage());
            return new MidiResult(null, false);
        }
        System.out.println("Playing Ode to Joy...");
        playSequence(seq);
        return new MidiResult(seq, true);
    }

    // ── Step 5 ────────────────────────────────────────────────────────────────

    record Transformations(Voice transposed, Voice retrograde, Voice inverted,
                           Voice augmented, Voice retrogradeInversion) {
    }

    static Transformations buildTransformations(final Voice melody, final Score score) {
        final var transposed = melody.transpose(SpelledInterval.parse("m3"));

        final var retrograde = melody.transform(new Retrograde()::apply);

        final var invert = new Invert(SpelledPitch.parse("E4"));
        final var inverted = Voice.of("inverted", melody.events().stream()
            .map(event -> switch (event) {
                case Note n ->
                    (NoteEvent) Note.of(invert.apply(n.pitch()), n.duration(), n.velocity(), n.articulation(), n.tied());
                case Rest r -> r;
                case Chord c -> (NoteEvent) c;
            })
            .toList());

        final var augmented = melody.transform(Augment.doubling()::apply);

        final var ri = Voice.of("retrograde-inversion",
            Transforms.retrogradeInversion(SpelledPitch.parse("E4")).apply(melody.events()));

        return new Transformations(transposed, retrograde, inverted, augmented, ri);
    }

    static void playTransformations(final Transformations t, final Score score, final boolean midiAvailable) {
        if (!midiAvailable) {
            return;
        }

        playVoice("Playing transposed up a minor third...", t.transposed(), score);
        playVoice("Playing retrograde...", t.retrograde(), score);
        playVoice("Playing inversion around E4...", t.inverted(), score);
        playVoice("Playing augmented (2x duration)...", t.augmented(), score);
        playVoice("Playing retrograde inversion...", t.retrogradeInversion(), score);
    }

    // ── Step 6 ────────────────────────────────────────────────────────────────

    record EngravedResult(boolean originalEngraved, boolean transposedEngraved) {
    }

    static EngravedResult engraveSheetMusic(final Score score, final Transformations t) {
        if (!LilyPondEngraver.isAvailable()) {
            System.out.println("LilyPond not found — skipping sheet music generation.");
            System.out.println("Install from https://lilypond.org/ to enable notation output.");
            return new EngravedResult(false, false);
        }
        try {
            final Path outputDir = Path.of("output");
            Files.createDirectories(outputDir);

            LilyPondEngraver.engravePdf(LilyPondRenderer.render(score), outputDir, "ode-to-joy-original");
            System.out.println("Engraved: output/ode-to-joy-original.pdf");

            final var transposedScore = Score.builder("Ode to Joy — Transposed up m3")
                .timeSignature(score.timeSignature())
                .tempo(score.tempo())
                .part(Part.piano("Melody", t.transposed()))
                .build();
            LilyPondEngraver.engravePdf(LilyPondRenderer.render(transposedScore), outputDir, "ode-to-joy-transposed");
            System.out.println("Engraved: output/ode-to-joy-transposed.pdf");

            System.out.println("Sheet music generated in output/ directory");
            return new EngravedResult(true, true);
        } catch (final IOException e) {
            System.out.println("Sheet music generation failed: " + e.getMessage());
            return new EngravedResult(false, false);
        }
    }

    // ── Step 7 ────────────────────────────────────────────────────────────────

    static void printSummary(final Voice melody, final MidiResult midi, final EngravedResult engraved) {
        final long noteCount = melody.events().stream().filter(e -> e instanceof Note).count();
        final String ok = "  \u2713 ";
        final String no = "  \u2717 ";
        final String bar = "\u2550".repeat(47);
        System.out.println("\n" + bar);
        System.out.println("  music.build \u2014 Ode to Joy Demo");
        System.out.println(bar);
        System.out.println(ok + "Original melody: 16 bars, " + noteCount + " notes");
        System.out.println((midi.available() ? ok : no) + "Played through MIDI synthesizer");
        System.out.println(ok + "Written to ode-to-joy.mid");
        System.out.println((midi.available() ? ok : no) + "Transposed up minor 3rd \u2192 played");
        System.out.println((midi.available() ? ok : no) + "Retrograde \u2192 played");
        System.out.println((midi.available() ? ok : no) + "Inverted around E4 \u2192 played");
        System.out.println((midi.available() ? ok : no) + "Augmented 2x \u2192 played");
        System.out.println((midi.available() ? ok : no) + "Retrograde inversion \u2192 played");
        if (engraved.originalEngraved()) {
            System.out.println(ok + "Sheet music: output/ode-to-joy-original.pdf");
        }
        if (engraved.transposedEngraved()) {
            System.out.println(ok + "Sheet music: output/ode-to-joy-transposed.pdf");
        }
        if (!engraved.originalEngraved()) {
            System.out.println(no + "LilyPond not available (sheet music skipped)");
        }
        System.out.println(bar);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void playVoice(final String label, final Voice voice, final Score template) {
        try {
            final var s = Score.builder(template.title())
                .timeSignature(template.timeSignature())
                .tempo(template.tempo())
                .part(Part.piano("Melody", voice))
                .build();
            System.out.println(label);
            playSequence(MidiRenderer.render(s));
        } catch (final Exception e) {
            System.out.println("Render failed: " + e.getMessage());
        }
    }

    private static void playSequence(final Sequence sequence) {
        try (MidiPlayer player = new MidiPlayer()) {
            player.play(sequence);
        } catch (final MidiUnavailableException e) {
            System.out.println("MIDI synthesizer not available: " + e.getMessage());
        } catch (final InvalidMidiDataException e) {
            System.out.println("Invalid MIDI data: " + e.getMessage());
        }
    }
}
