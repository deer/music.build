package build.music.mcp.tools;

import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.core.Ornament;
import build.music.core.Rest;
import build.music.time.DottedValue;
import build.music.time.RhythmicValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateNoteToolsTests {

    @Test
    void parseSingleNote() {
        List<NoteEvent> events = CreateNoteTools.parseNoteSequence("C4/q");
        assertEquals(1, events.size());
        assertInstanceOf(Note.class, events.getFirst());
        Note note = (Note) events.getFirst();
        assertEquals(60, note.midi()); // C4 = MIDI 60
        assertEquals(RhythmicValue.QUARTER, note.duration());
    }

    @Test
    void parseRest() {
        List<NoteEvent> events = CreateNoteTools.parseNoteSequence("r/h");
        assertEquals(1, events.size());
        assertInstanceOf(Rest.class, events.getFirst());
        assertEquals(RhythmicValue.HALF, events.getFirst().duration());
    }

    @Test
    void parseThreeNoteSequence() {
        List<NoteEvent> events = CreateNoteTools.parseNoteSequence("C4/q D4/q E4/q");
        assertEquals(3, events.size());
        assertEquals(60, ((Note) events.get(0)).midi()); // C4
        assertEquals(62, ((Note) events.get(1)).midi()); // D4
        assertEquals(64, ((Note) events.get(2)).midi()); // E4
    }

    @Test
    void parseDottedValues() {
        List<NoteEvent> events = CreateNoteTools.parseNoteSequence("C4/dq D4/de E4/dh");
        assertInstanceOf(DottedValue.class, events.get(0).duration());
        assertInstanceOf(DottedValue.class, events.get(1).duration());
        assertInstanceOf(DottedValue.class, events.get(2).duration());

        DottedValue dq = (DottedValue) events.get(0).duration();
        assertEquals(RhythmicValue.QUARTER, dq.base());
        assertEquals(1, dq.dots());
    }

    @Test
    void parseAllDurationCodes() {
        assertEquals(RhythmicValue.WHOLE, CreateNoteTools.parseDuration("w"));
        assertEquals(RhythmicValue.HALF, CreateNoteTools.parseDuration("h"));
        assertEquals(RhythmicValue.QUARTER, CreateNoteTools.parseDuration("q"));
        assertEquals(RhythmicValue.EIGHTH, CreateNoteTools.parseDuration("e"));
        assertEquals(RhythmicValue.SIXTEENTH, CreateNoteTools.parseDuration("s"));
    }

    @Test
    void parseNoteWithAccidentals() {
        List<NoteEvent> events = CreateNoteTools.parseNoteSequence("C#4/q Bb3/h");
        assertEquals(2, events.size());
        assertEquals(61, ((Note) events.get(0)).midi()); // C#4
        assertEquals(58, ((Note) events.get(1)).midi()); // Bb3
    }

    @Test
    void parseNotesWithRests() {
        List<NoteEvent> events = CreateNoteTools.parseNoteSequence("C4/q r/q E4/q r/q");
        assertEquals(4, events.size());
        assertInstanceOf(Note.class, events.get(0));
        assertInstanceOf(Rest.class, events.get(1));
        assertInstanceOf(Note.class, events.get(2));
        assertInstanceOf(Rest.class, events.get(3));
    }

    @Test
    void invalidTokenGivesClearError() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> CreateNoteTools.parseNoteSequence("C4"));
        assertTrue(ex.getMessage().contains("C4"));
        assertTrue(ex.getMessage().contains("pitch/duration"));
    }

    @Test
    void unknownDurationCodeGivesClearError() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> CreateNoteTools.parseNoteSequence("C4/x"));
        assertTrue(ex.getMessage().contains("x"));
        assertTrue(ex.getMessage().contains("w, h, q, e, s"));
    }

    @Test
    void emptyInputThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> CreateNoteTools.parseNoteSequence(""));
        assertThrows(IllegalArgumentException.class,
            () -> CreateNoteTools.parseNoteSequence("   "));
    }

    @Test
    void parseOrnamentSuffixes() {
        Note roll = (Note) CreateNoteTools.parseToken("A4/e~roll");
        Note trill = (Note) CreateNoteTools.parseToken("A4/e~trill");
        Note mord = (Note) CreateNoteTools.parseToken("A4/e~mord");
        Note prall = (Note) CreateNoteTools.parseToken("A4/e~prall");
        assertEquals(Ornament.ROLL, roll.ornament().orElseThrow());
        assertEquals(Ornament.TRILL, trill.ornament().orElseThrow());
        assertEquals(Ornament.MORDENT, mord.ornament().orElseThrow());
        assertEquals(Ornament.PRALL, prall.ornament().orElseThrow());
    }

    @Test
    void parseGraceNotePrefix() {
        Note n = (Note) CreateNoteTools.parseToken("{G4}A4/q");
        assertEquals(69, n.midi()); // A4
        assertEquals(1, n.graceNotes().size());
        assertEquals(67, n.graceNotes().getFirst().midi()); // G4
    }

    @Test
    void parseMultipleGraceNotes() {
        Note n = (Note) CreateNoteTools.parseToken("{G4 A4}B4/e");
        assertEquals(71, n.midi()); // B4
        assertEquals(2, n.graceNotes().size());
        assertEquals(67, n.graceNotes().get(0).midi()); // G4
        assertEquals(69, n.graceNotes().get(1).midi()); // A4
    }

    @Test
    void formatEventRoundTripsOrnament() {
        Note n = (Note) CreateNoteTools.parseToken("A4/e~roll");
        String formatted = CreateNoteTools.formatEvent(n);
        assertTrue(formatted.contains("~roll"), "expected ~roll in: " + formatted);
        assertTrue(formatted.contains("A4/e"), "expected pitch/dur in: " + formatted);
    }

    @Test
    void formatEventRoundTripsGraceNotes() {
        Note n = (Note) CreateNoteTools.parseToken("{G4}A4/q");
        String formatted = CreateNoteTools.formatEvent(n);
        assertTrue(formatted.startsWith("{G4}"), "expected grace prefix in: " + formatted);
        assertTrue(formatted.contains("A4/q"), "expected main note in: " + formatted);
    }

    @Test
    void parseMultipleGraceNotesInSequence() {
        // Regression: multi-grace-note tokens were split by the whitespace tokenizer in parseNoteSequence
        var events = CreateNoteTools.parseNoteSequence("A4/q {G4 A4}B4/e C5/q");
        assertEquals(3, events.size());
        Note grace = (Note) events.get(1);
        assertEquals(71, grace.midi()); // B4
        assertEquals(2, grace.graceNotes().size());
        assertEquals(67, grace.graceNotes().get(0).midi()); // G4
        assertEquals(69, grace.graceNotes().get(1).midi()); // A4
    }

    @Test
    void unclosedGraceBraceThrows() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> CreateNoteTools.parseToken("{G4 A4/q"));
        assertTrue(ex.getMessage().contains("Unclosed '{'"));
    }
}
