package build.music.transform;

import build.music.pitch.*;
import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.core.Rest;
import build.music.time.RhythmicValue;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TransposeTests {

    @Test
    void transpose_C4_upPerfectFifth_givesG4() {
        var c4 = SpelledPitch.of(NoteName.C, Accidental.NATURAL, 4);
        var p5 = SpelledInterval.of(IntervalQuality.PERFECT, IntervalSize.FIFTH);
        var t = new Transpose(p5);
        var result = t.apply(c4);
        assertEquals(SpelledPitch.of(NoteName.G, Accidental.NATURAL, 4), result);
    }

    @Test
    void transpose_thenInverse_isIdentity() {
        var c4 = SpelledPitch.of(NoteName.C, Accidental.NATURAL, 4);
        var p5 = SpelledInterval.of(IntervalQuality.PERFECT, IntervalSize.FIFTH);
        var t = new Transpose(p5);
        var result = t.inverse().apply(t.apply(c4));
        assertEquals(c4, result);
    }

    @Test
    void transposePitches_transposesNotes_leavesRestsAlone() {
        var c4 = SpelledPitch.of(NoteName.C, Accidental.NATURAL, 4);
        var note = Note.of(c4, RhythmicValue.QUARTER);
        var rest = Rest.of(RhythmicValue.QUARTER);
        var p5 = SpelledInterval.of(IntervalQuality.PERFECT, IntervalSize.FIFTH);
        var result = Transforms.transposePitches(List.of(note, rest), p5);
        assertEquals(2, result.size());
        assertInstanceOf(Note.class, result.get(0));
        assertInstanceOf(Rest.class, result.get(1));
        assertEquals(67, ((Note) result.get(0)).midi()); // G4
    }

}
