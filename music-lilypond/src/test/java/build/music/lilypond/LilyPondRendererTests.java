package build.music.lilypond;

import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.core.Rest;
import build.music.pitch.SpelledPitch;
import build.music.score.Part;
import build.music.score.Score;
import build.music.score.Voice;
import build.music.time.DottedValue;
import build.music.time.Fraction;
import build.music.time.RhythmicValue;
import build.music.time.Tempo;
import build.music.time.TimeSignature;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LilyPondRendererTests {

    @Test
    void renderPitchNatural() {
        assertEquals("c'", LilyPondRenderer.renderPitch(SpelledPitch.parse("C4")));
        assertEquals("e'", LilyPondRenderer.renderPitch(SpelledPitch.parse("E4")));
        assertEquals("g'", LilyPondRenderer.renderPitch(SpelledPitch.parse("G4")));
        assertEquals("c''", LilyPondRenderer.renderPitch(SpelledPitch.parse("C5")));
        assertEquals("c", LilyPondRenderer.renderPitch(SpelledPitch.parse("C3")));
        assertEquals("c,", LilyPondRenderer.renderPitch(SpelledPitch.parse("C2")));
    }

    @Test
    void renderPitchAccidentals() {
        assertEquals("cis'", LilyPondRenderer.renderPitch(SpelledPitch.parse("C#4")));
        assertEquals("bes'", LilyPondRenderer.renderPitch(SpelledPitch.parse("Bb4")));
        assertEquals("fis'", LilyPondRenderer.renderPitch(SpelledPitch.parse("F#4")));
    }

    @Test
    void renderDurationPlain() {
        assertEquals("1", LilyPondRenderer.renderDuration(Fraction.ONE));
        assertEquals("2", LilyPondRenderer.renderDuration(Fraction.HALF));
        assertEquals("4", LilyPondRenderer.renderDuration(Fraction.QUARTER));
        assertEquals("8", LilyPondRenderer.renderDuration(Fraction.EIGHTH));
        assertEquals("16", LilyPondRenderer.renderDuration(Fraction.of(1, 16)));
    }

    @Test
    void renderDurationDotted() {
        assertEquals("4.", LilyPondRenderer.renderDuration(Fraction.of(3, 8))); // dotted quarter
        assertEquals("2.", LilyPondRenderer.renderDuration(Fraction.of(3, 4))); // dotted half
        assertEquals("8.", LilyPondRenderer.renderDuration(Fraction.of(3, 16))); // dotted eighth
    }

    @Test
    void renderEventsInsertsBarLines() {
        List<NoteEvent> events = List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("E4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("G4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER)
        );
        String result = LilyPondRenderer.renderEvents(events, TimeSignature.COMMON_TIME);
        assertTrue(result.contains("|"), "Should contain bar lines");
    }

    @Test
    void renderScoreContainsTitle() {
        var voice = Voice.of("melody", List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER)
        ));
        var score = Score.builder("My Song")
            .part(Part.piano("Piano", voice))
            .build();
        String ly = LilyPondRenderer.render(score);
        assertTrue(ly.contains("My Song"));
        assertTrue(ly.contains("\\version"));
        assertTrue(ly.contains("\\score"));
        assertTrue(ly.contains("\\layout"));
        assertTrue(ly.contains("\\midi"));
    }

    @Test
    void renderScoreContainsNoteNames() {
        var voice = Voice.of("melody", List.of(
            Note.of(SpelledPitch.parse("E4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("F4"), RhythmicValue.QUARTER)
        ));
        var score = Score.builder("Test").part(Part.piano("P", voice)).build();
        String ly = LilyPondRenderer.render(score);
        assertTrue(ly.contains("e'4"));
        assertTrue(ly.contains("f'4"));
    }

    @Test
    void chooseClefTrebleForHighPitches() {
        List<NoteEvent> events = List.of(Note.of(SpelledPitch.parse("E4"), RhythmicValue.QUARTER));
        assertEquals("treble", LilyPondRenderer.chooseClef(events));
    }

    @Test
    void chooseClefBassForLowPitches() {
        List<NoteEvent> events = List.of(Note.of(SpelledPitch.parse("C2"), RhythmicValue.QUARTER));
        assertEquals("bass", LilyPondRenderer.chooseClef(events));
    }

    @Test
    void renderRestAppearance() {
        var events = List.<NoteEvent>of(Rest.of(RhythmicValue.QUARTER));
        String result = LilyPondRenderer.renderEvents(events, TimeSignature.COMMON_TIME);
        assertTrue(result.contains("r4"));
    }
}
