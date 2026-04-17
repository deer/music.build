package build.music.transform;

import build.music.core.NoteEvent;

import java.util.List;

/**
 * Transform that operates on sequences of NoteEvents.
 */
public interface MelodicTransform extends Transform<List<NoteEvent>> {
}
