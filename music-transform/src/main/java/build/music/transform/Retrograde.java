package build.music.transform;

import build.music.core.NoteEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Reverses the temporal order of a sequence of NoteEvents. */
public record Retrograde() implements MelodicTransform {

    @Override
    public List<NoteEvent> apply(final List<NoteEvent> input) {
        final List<NoteEvent> reversed = new ArrayList<>(input);
        Collections.reverse(reversed);
        return List.copyOf(reversed);
    }
}
