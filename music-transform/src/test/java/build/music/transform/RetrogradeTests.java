package build.music.transform;

import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.pitch.*;
import build.music.time.RhythmicValue;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RetrogradeTests {

    @Test
    void retrograde_reversesOrder() {
        var c4 = Note.of(SpelledPitch.of(NoteName.C, Accidental.NATURAL, 4), RhythmicValue.QUARTER);
        var d4 = Note.of(SpelledPitch.of(NoteName.D, Accidental.NATURAL, 4), RhythmicValue.QUARTER);
        var e4 = Note.of(SpelledPitch.of(NoteName.E, Accidental.NATURAL, 4), RhythmicValue.QUARTER);
        var retrograde = new Retrograde();
        var result = retrograde.apply(List.of(c4, d4, e4));
        assertEquals(List.of(e4, d4, c4), result);
    }

    @Test
    void augment_doublesAllDurations() {
        var c4 = Note.of(SpelledPitch.of(NoteName.C, Accidental.NATURAL, 4), RhythmicValue.QUARTER);
        var aug = Augment.doubling();
        var result = aug.apply(List.of(c4));
        assertEquals(1, result.size());
        // quarter (1/4) doubled = half (1/2)
        assertEquals(build.music.time.Fraction.of(1, 2), result.getFirst().duration().fraction());
    }

    @Test
    void halving_halvesAllDurations() {
        var c4 = Note.of(SpelledPitch.of(NoteName.C, Accidental.NATURAL, 4), RhythmicValue.HALF);
        var diminish = Augment.halving();
        var result = diminish.apply(List.of(c4));
        // half (1/2) halved = quarter (1/4)
        assertEquals(build.music.time.Fraction.of(1, 4), result.getFirst().duration().fraction());
    }
}
