package build.music.rules;

import build.music.score.Voice;

import java.util.List;

/**
 * A composable validation rule for musical content.
 */
public interface Rule {
    String name();

    String description();

    /**
     * Check a single voice in isolation.
     */
    List<Violation> check(Voice voice, String voiceName);

    /**
     * Check two voices for inter-voice issues (parallel motion, etc.). Default: no violations.
     */
    default List<Violation> checkPair(final Voice a, final String aName, final Voice b, final String bName) {
        return List.of();
    }
}
