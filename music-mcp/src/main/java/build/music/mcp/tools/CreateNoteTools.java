package build.music.mcp.tools;

import build.music.core.Articulation;
import build.music.core.Chord;
import build.music.core.ControlChange;
import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.core.ProgramChange;
import build.music.core.Rest;
import build.music.core.Velocity;
import build.music.pitch.Pitch;
import build.music.pitch.SpelledPitch;
import build.music.time.DottedValue;
import build.music.time.Duration;
import build.music.time.RhythmicValue;
import build.music.time.Tuplet;

import java.util.ArrayList;
import java.util.List;

/**
 * Tools for creating notes, rests, chords, and note sequences.
 */
public final class CreateNoteTools {

    private CreateNoteTools() {
    }

    /**
     * Parse a compact note sequence string into a list of NoteEvents.
     * <p>
     * Format: "pitch/duration" tokens separated by spaces.
     * Pitch: "C4", "C#4", "Bb3", etc. Use "r" for a rest.
     * Chord: "&lt;C4 E4 G4&gt;/q" — angle-bracket-enclosed space-separated pitches.
     * Duration codes:
     * w=whole, h=half, q=quarter, e=eighth, s=sixteenth
     * dh=dotted half, dq=dotted quarter, de=dotted eighth
     * Articulations (append after duration with ~):
     * ~stac=staccato, ~acc=accent, ~ten=tenuto, ~marc=marcato, ~leg=legato
     * Ties: append ~tie to hold a note into the next bar: "G4/h~tie G4/q"
     * <p>
     * Examples:
     * "C4/q D4/q E4/q F4/q"         — four quarter notes
     * "C4/dq D4/e E4/h"             — dotted quarter, eighth, half
     * "C4/q r/q E4/q r/q"           — notes with rests
     * "&lt;C4 E4 G4&gt;/q &lt;F4 A4 C5&gt;/h"   — two chords
     * "C4/q~stac D4/q~acc E4/h"     — staccato C, accented D, normal E
     */
    public static List<NoteEvent> parseNoteSequence(final String notes) {
        if (notes == null || notes.isBlank()) {
            throw new IllegalArgumentException("Note sequence must not be empty.");
        }

        final List<NoteEvent> events = new ArrayList<>();
        final String input = notes.trim();
        int i = 0;
        while (i < input.length()) {
            // Skip whitespace
            while (i < input.length() && Character.isWhitespace(input.charAt(i))) {
                i++;
            }
            if (i >= input.length()) {
                break;
            }

            if (input.charAt(i) == '<') {
                // Chord token: collect until closing >
                final int close = input.indexOf('>', i);
                if (close < 0) {
                    throw new IllegalArgumentException(
                        "Unclosed '<' in note sequence. Chord syntax: '<C4 E4 G4>/q'.");
                }
                final String pitchesStr = input.substring(i + 1, close).trim();
                // Everything after > until whitespace is the /duration part
                int tokenEnd = close + 1;
                while (tokenEnd < input.length() && !Character.isWhitespace(input.charAt(tokenEnd))) {
                    tokenEnd++;
                }
                final String durPart = input.substring(close + 1, tokenEnd);
                events.add(parseChordToken(pitchesStr, durPart));
                i = tokenEnd;
            } else {
                // Regular token (note, rest, CC, or program change) — read until whitespace
                int tokenEnd = i;
                while (tokenEnd < input.length() && !Character.isWhitespace(input.charAt(tokenEnd))) {
                    tokenEnd++;
                }
                final String token = input.substring(i, tokenEnd);
                final String lower = token.toLowerCase();
                if (lower.startsWith("cc:")) {
                    events.add(parseCcToken(token));
                } else if (lower.startsWith("pc:")) {
                    events.add(parsePcToken(token));
                } else {
                    events.add(parseToken(token));
                }
                i = tokenEnd;
            }
        }

        if (events.isEmpty()) {
            throw new IllegalArgumentException("Note sequence must not be empty.");
        }
        return events;
    }

    /**
     * Parse a chord token: pitchesStr = "C4 E4 G4", durPart = "/q" or "/q~stac".
     */
    private static NoteEvent parseChordToken(final String pitchesStr, final String durPart) {
        if (!durPart.startsWith("/")) {
            throw new IllegalArgumentException(
                "Chord must be followed by a duration like '/q'. Got: '" + durPart + "'.");
        }
        final String afterSlash = durPart.substring(1);
        // Check for articulation suffix
        final int tildeIdx = afterSlash.indexOf('~');
        final String durationCode = tildeIdx >= 0 ? afterSlash.substring(0, tildeIdx) : afterSlash;
        final Duration duration = parseDuration(durationCode);

        final String[] pitchTokens = pitchesStr.trim().split("\\s+");
        if (pitchTokens.length < 2) {
            throw new IllegalArgumentException(
                "Chord must have at least 2 pitches. Got: '" + pitchesStr + "'.");
        }
        final List<Pitch> pitches = new ArrayList<>();
        for (final String pt : pitchTokens) {
            pitches.add(SpelledPitch.parse(pt.trim()));
        }
        return Chord.of(pitches, duration, Velocity.MF);
    }

    /**
     * Parse a single note/rest token like "C4/q", "r/h", "C4/q~stac".
     */
    static ControlChange parseCcToken(final String token) {
        // Formats: "cc:11:80" or "cc:pan:64"
        final String[] parts = token.split(":", 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException(
                "Invalid CC token '" + token + "'. Expected 'cc:NUMBER:VALUE' or 'cc:NAME:VALUE' " +
                    "(e.g. 'cc:pan:64', 'cc:expr:80', 'cc:11:80').");
        }
        final int ccNum = parseCcNumber(parts[1]);
        final int value;
        try {
            value = Integer.parseInt(parts[2]);
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException(
                "Invalid CC value in '" + token + "'. Expected a number 0-127.");
        }
        return new ControlChange(ccNum, value);
    }

    private static int parseCcNumber(final String nameOrNumber) {
        return switch (nameOrNumber.toLowerCase()) {
            case "mod", "modulation" -> 1;
            case "vol", "volume" -> 7;
            case "pan" -> 10;
            case "expr", "expression" -> 11;
            case "sustain" -> 64;
            case "reverb" -> 91;
            case "chorus" -> 93;
            default -> {
                try {
                    yield Integer.parseInt(nameOrNumber);
                } catch (final NumberFormatException e) {
                    throw new IllegalArgumentException(
                        "Unknown CC name '" + nameOrNumber + "'. Use 0-127 or a name: " +
                            "mod, vol, pan, expr, sustain, reverb, chorus.");
                }
            }
        };
    }

    static ProgramChange parsePcToken(final String token) {
        // Format: "pc:73"
        final String[] parts = token.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException(
                "Invalid program change token '" + token + "'. Expected 'pc:NUMBER' (e.g. 'pc:40').");
        }
        try {
            return new ProgramChange(Integer.parseInt(parts[1]));
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException(
                "Invalid program number in '" + token + "'. Expected a number 0-127.");
        }
    }

    static NoteEvent parseToken(final String token) {
        final int slash = token.lastIndexOf('/');
        if (slash < 0) {
            throw new IllegalArgumentException(
                "Invalid note token '" + token + "'. Expected format 'pitch/duration' (e.g. 'C4/q' or 'r/h').");
        }
        final String pitchPart = token.substring(0, slash);
        final String afterSlash = token.substring(slash + 1);

        // Check for suffix(es) after ~
        // Supported: ~stac, ~acc, ~ten, ~marc, ~leg, ~tie (and combinations: ~stac~tie)
        final int tildeIdx = afterSlash.indexOf('~');
        final String durationCode = tildeIdx >= 0 ? afterSlash.substring(0, tildeIdx) : afterSlash;
        final String suffixStr = tildeIdx >= 0 ? afterSlash.substring(tildeIdx + 1) : "";

        final Duration duration = parseDuration(durationCode);

        // Parse suffixes — split on ~ to allow ~stac~tie~vel:90 etc.
        boolean tied = false;
        Articulation articulation = Articulation.NORMAL;
        Velocity velocity = Velocity.MF;
        for (final String suffix : suffixStr.split("~")) {
            if (suffix.isEmpty()) {
                continue;
            }
            if (suffix.equalsIgnoreCase("tie")) {
                tied = true;
            } else if (suffix.toLowerCase().startsWith("vel:")) {
                try {
                    velocity = Velocity.of(Integer.parseInt(suffix.substring(4)));
                } catch (final NumberFormatException e) {
                    throw new IllegalArgumentException(
                        "Invalid velocity in '" + suffix + "'. Expected ~vel:N where N is 0-127.");
                }
            } else {
                articulation = parseArticulation(suffix);
            }
        }

        if (pitchPart.equalsIgnoreCase("r")) {
            return Rest.of(duration);
        }
        final SpelledPitch pitch = SpelledPitch.parse(pitchPart);
        return Note.of(pitch, duration, velocity, articulation, tied);
    }

    static Duration parseDuration(final String code) {
        return switch (code.toLowerCase()) {
            case "w" -> RhythmicValue.WHOLE;
            case "h" -> RhythmicValue.HALF;
            case "q" -> RhythmicValue.QUARTER;
            case "e" -> RhythmicValue.EIGHTH;
            case "s" -> RhythmicValue.SIXTEENTH;
            case "dh" -> new DottedValue(RhythmicValue.HALF, 1);
            case "dq" -> new DottedValue(RhythmicValue.QUARTER, 1);
            case "de" -> new DottedValue(RhythmicValue.EIGHTH, 1);
            case "h3" -> Tuplet.triplet(RhythmicValue.HALF);
            case "q3" -> Tuplet.triplet(RhythmicValue.QUARTER);
            case "e3" -> Tuplet.triplet(RhythmicValue.EIGHTH);
            case "s3" -> Tuplet.triplet(RhythmicValue.SIXTEENTH);
            default -> throw new IllegalArgumentException(
                "Unknown duration code '" + code + "'. Valid codes: w, h, q, e, s, dh, dq, de, h3, q3, e3, s3.");
        };
    }

    static Articulation parseArticulation(final String code) {
        return switch (code.toLowerCase()) {
            case "stac" -> Articulation.STACCATO;
            case "stacs" -> Articulation.STACCATISSIMO;
            case "acc" -> Articulation.ACCENT;
            case "ten" -> Articulation.TENUTO;
            case "marc" -> Articulation.MARCATO;
            case "leg" -> Articulation.LEGATO;
            case "port" -> Articulation.PORTATO;
            default -> throw new IllegalArgumentException(
                "Unknown articulation code '" + code +
                    "'. Valid codes: stac, stacs, acc, ten, marc, leg, port.");
        };
    }

    /**
     * Format a list of NoteEvents as a compact human-readable string.
     */
    public static String formatSequence(final List<NoteEvent> events) {
        final var sb = new StringBuilder();
        for (final NoteEvent event : events) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(formatEvent(event));
        }
        return sb.toString();
    }

    static String formatEvent(final NoteEvent event) {
        return switch (event) {
            case Note n -> {
                final String art = n.articulation() != Articulation.NORMAL
                    ? "~" + n.articulation().name().toLowerCase()
                    : "";
                final String vel = n.velocity().value() != Velocity.MF.value()
                    ? "~vel:" + n.velocity().value()
                    : "";
                final String tie = n.tied() ? "~tie" : "";
                yield n.pitch().spelled().toString() + "/" + formatDuration(n.duration()) + art + vel + tie;
            }
            case Rest r -> "r/" + formatDuration(r.duration());
            case ControlChange cc -> "cc:" + cc.cc() + ":" + cc.value();
            case ProgramChange pc -> "pc:" + pc.program();
            case Chord c -> {
                final StringBuilder sb = new StringBuilder("<");
                for (int i = 0; i < c.pitches().size(); i++) {
                    if (i > 0) {
                        sb.append(" ");
                    }
                    sb.append(c.pitches().get(i).spelled().toString());
                }
                sb.append(">/").append(formatDuration(c.duration()));
                yield sb.toString();
            }
        };
    }

    static String formatDuration(final Duration d) {
        return switch (d) {
            case RhythmicValue rv -> switch (rv) {
                case WHOLE -> "w";
                case HALF -> "h";
                case QUARTER -> "q";
                case EIGHTH -> "e";
                case SIXTEENTH -> "s";
                default -> rv.fraction().toString();
            };
            case DottedValue dv -> switch (dv.base()) {
                case HALF -> "dh";
                case QUARTER -> "dq";
                case EIGHTH -> "de";
                default -> dv.fraction().toString();
            };
            case Tuplet t when t.actual() == 3 && t.normal() == 2 -> switch (t.unit()) {
                case HALF -> "h3";
                case QUARTER -> "q3";
                case EIGHTH -> "e3";
                case SIXTEENTH -> "s3";
                default -> t.fraction().toString();
            };
            default -> d.fraction().toString();
        };
    }
}
