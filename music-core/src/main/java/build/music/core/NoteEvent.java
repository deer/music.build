package build.music.core;

import build.music.time.Duration;

public sealed interface NoteEvent permits Note, Rest, Chord {
    Duration duration();
}
