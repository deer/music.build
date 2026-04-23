package build.music.mcp;

import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.core.Rest;
import build.music.pitch.SpelledPitch;
import build.music.score.Score;
import build.music.time.RhythmicValue;
import build.music.time.Tempo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompositionContextTests {

    private CompositionContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new CompositionContext();
    }

    @Test
    void createVoiceAndRetrieve() {
        List<NoteEvent> events = List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("D4"), RhythmicValue.QUARTER)
        );
        ctx.createVoice("melody", events);

        List<NoteEvent> retrieved = ctx.getVoice("melody");
        assertEquals(2, retrieved.size());
        assertTrue(ctx.voiceNames().contains("melody"));
    }

    @Test
    void appendToVoiceGrowsList() {
        List<NoteEvent> initial = List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER)
        );
        ctx.createVoice("melody", initial);

        List<NoteEvent> more = List.of(
            Note.of(SpelledPitch.parse("E4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("G4"), RhythmicValue.QUARTER)
        );
        ctx.appendToVoice("melody", more);

        assertEquals(3, ctx.getVoice("melody").size());
    }

    @Test
    void saveMotifAndRetrieve() {
        List<NoteEvent> events = List.of(
            Note.of(SpelledPitch.parse("E4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("F4"), RhythmicValue.QUARTER)
        );
        ctx.saveMotif("theme_a", events);

        List<NoteEvent> retrieved = ctx.getMotif("theme_a");
        assertEquals(2, retrieved.size());
        assertTrue(ctx.motifNames().contains("theme_a"));
    }

    @Test
    void buildScoreWithMultipleVoices() {
        ctx.setTitle("Test Score");
        ctx.setTempo(Tempo.of(100));

        ctx.createVoice("melody", List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER)
        ));
        ctx.createVoice("bass", List.of(
            Note.of(SpelledPitch.parse("C2"), RhythmicValue.HALF)
        ));

        Score score = ctx.buildScore();
        assertEquals("Test Score", score.title());
        assertEquals(2, score.scoreParts().size());
        assertEquals(100, score.tempo().bpm());
    }

    @Test
    void describeReturnsReadableSummary() {
        ctx.setTitle("My Piece");
        ctx.createVoice("melody", List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER)
        ));
        ctx.saveMotif("theme", List.of(
            Note.of(SpelledPitch.parse("E4"), RhythmicValue.QUARTER)
        ));

        String desc = ctx.describe();
        assertTrue(desc.contains("My Piece"));
        assertTrue(desc.contains("melody"));
        assertTrue(desc.contains("theme"));
        assertTrue(desc.contains("1 events"));
    }

    @Test
    void getMissingVoiceThrowsWithHelpfulMessage() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> ctx.getVoice("nonexistent"));
        assertTrue(ex.getMessage().contains("nonexistent"));
        assertTrue(ex.getMessage().contains("voice.create"));
    }

    @Test
    void getMissingMotifThrowsWithHelpfulMessage() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> ctx.getMotif("nonexistent"));
        assertTrue(ex.getMessage().contains("nonexistent"));
        assertTrue(ex.getMessage().contains("motif.save"));
    }

    @Test
    void assignPartToMissingVoiceThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> ctx.assignPart("missing", 0, 0, "piano"));
    }

    @Test
    void clearResetsEverything() {
        ctx.setTitle("Something");
        ctx.createVoice("v1", List.of(Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER)));
        ctx.saveMotif("m1", List.of(Note.of(SpelledPitch.parse("D4"), RhythmicValue.QUARTER)));

        ctx.clear();

        assertEquals("Untitled", ctx.getTitle());
        assertTrue(ctx.voiceNames().isEmpty());
        assertTrue(ctx.motifNames().isEmpty());
    }

    @Test
    void buildScoreWithRestInVoice() {
        ctx.createVoice("melody", List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER),
            Rest.of(RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("E4"), RhythmicValue.QUARTER)
        ));

        Score score = ctx.buildScore();
        assertEquals(1, score.scoreParts().size());
        assertEquals(3, score.scoreParts().getFirst().voice().events().size());
    }
}
