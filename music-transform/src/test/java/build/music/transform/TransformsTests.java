package build.music.transform;

import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.pitch.*;
import build.music.time.RhythmicValue;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TransformsTests {

    @Test
    void compose_transposeAndRetrograde() {
        var c4 = Note.of(SpelledPitch.of(NoteName.C, Accidental.NATURAL, 4), RhythmicValue.QUARTER);
        var d4 = Note.of(SpelledPitch.of(NoteName.D, Accidental.NATURAL, 4), RhythmicValue.QUARTER);
        var p5 = SpelledInterval.of(IntervalQuality.PERFECT, IntervalSize.FIFTH);

        // compose: first transpose up P5, then retrograde
        Transform<List<NoteEvent>> combined = Transforms.compose(
            (Transform<List<NoteEvent>>) events -> Transforms.transposePitches(events, p5),
            new Retrograde()
        );

        var result = combined.apply(List.of(c4, d4));
        assertEquals(2, result.size());
        // After transpose: [G4, A4]. After retrograde: [A4, G4]
        assertEquals(69, ((Note) result.get(0)).midi()); // A4
        assertEquals(67, ((Note) result.get(1)).midi()); // G4
    }

    @Test
    void identity_returnsInputUnchanged() {
        var c4 = Note.of(SpelledPitch.of(NoteName.C, Accidental.NATURAL, 4), RhythmicValue.QUARTER);
        var input = List.of((NoteEvent) c4);
        Transform<List<NoteEvent>> id = Transform.identity();
        assertSame(input, id.apply(input));
    }

    @Test
    void retrogradeInversion_combinesBothOperations() {
        // C4 D4 E4 → retrograde → E4 D4 C4 → invert around C4 → C4 Bb3 Ab3 (approx)
        var axis = SpelledPitch.of(NoteName.C, Accidental.NATURAL, 4);
        var c4 = Note.of(SpelledPitch.of(NoteName.C, Accidental.NATURAL, 4), RhythmicValue.QUARTER);
        var d4 = Note.of(SpelledPitch.of(NoteName.D, Accidental.NATURAL, 4), RhythmicValue.QUARTER);
        var e4 = Note.of(SpelledPitch.of(NoteName.E, Accidental.NATURAL, 4), RhythmicValue.QUARTER);

        var ri = Transforms.retrogradeInversion(axis);
        var result = ri.apply(List.of(c4, d4, e4));
        assertEquals(3, result.size());
        // E4 (MIDI 64) is 4 semitones above C4 (60), so inverted = 4 below = MIDI 56 = Ab3
        // D4 (MIDI 62) is 2 semitones above C4 (60), so inverted = 2 below = MIDI 58 = Bb3
        // C4 (MIDI 60) is axis, inverted = C4 (MIDI 60)
        assertEquals(56, ((Note) result.get(0)).midi()); // inverted E4
        assertEquals(58, ((Note) result.get(1)).midi()); // inverted D4
        assertEquals(60, ((Note) result.get(2)).midi()); // inverted C4 = C4
    }
}
