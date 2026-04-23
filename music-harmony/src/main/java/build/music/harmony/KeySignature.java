package build.music.harmony;

import build.music.pitch.Accidental;
import build.music.pitch.NoteName;

import java.util.List;
import java.util.Objects;

/**
 * Key signature for notation purposes.
 */
public record KeySignature(Key key) {

    public KeySignature {
        Objects.requireNonNull(key, "key must not be null");
    }

    /**
     * Number of sharps (greater than 0) or flats (less than 0).
     */
    public int accidentalCount() {
        return key.signatureAccidentals();
    }

    /**
     * The accidental notes in order (for notation).
     */
    public List<NoteName> accidentals() {
        return key.accidentalNotes();
    }

    /**
     * LilyPond representation: "\key c \major", "\key fis \minor"
     * LilyPond uses: sharp = "is" suffix, flat = "es" suffix (or "as" for A, "es" for E).
     */
    public String toLilyPond() {
        final String tonicLy = lilypondNoteName(key.tonic(), key.accidental());
        final String mode = key.minor() ? "\\minor" : "\\major";
        return "\\key " + tonicLy + " " + mode;
    }

    private static String lilypondNoteName(final NoteName name, final Accidental acc) {
        final String base = name.name().toLowerCase();
        final String accStr = switch (acc) {
            case SHARP -> "is";
            case FLAT -> {
                // Special cases: aes for Ab, ees for Eb (LilyPond convention)
                yield switch (name) {
                    case A -> "es";  // aes
                    case E -> "es";  // ees
                    default -> "es";
                };
            }
            case DOUBLE_SHARP -> "isis";
            case DOUBLE_FLAT -> "eses";
            case NATURAL -> "";
        };
        return base + accStr;
    }
}
