package build.music.abc;

import build.base.parsing.Filter;
import build.base.parsing.Scanner;
import build.music.core.Chord;
import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.core.Rest;
import build.music.core.Velocity;
import build.music.pitch.Accidental;
import build.music.pitch.NoteName;
import build.music.pitch.Pitch;
import build.music.pitch.SpelledPitch;
import build.music.score.Voice;
import build.music.time.Fraction;
import build.music.time.Tempo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Parses ABC notation into Voice objects.
 * <p>
 * Supported: single and multi-voice tunes (V: headers), M:/L:/T:/K:/Q: headers,
 * bar lines, repeats, ties, dotted notes, octave markers (' and ,), accidentals,
 * chord events ([CEG]), tuplets ((3, (p:q:r)).
 * Not supported: grace notes ({...}), {@code >} {@code <} dotted shorthand.
 */
public final class AbcReader {

    private AbcReader() {
    }

    public record AbcImport(List<Voice> voices, Tempo tempo, String title) {
    }

    private static final NoteName[] SHARP_ORDER = {
        NoteName.F, NoteName.C, NoteName.G, NoteName.D, NoteName.A, NoteName.E, NoteName.B
    };
    private static final NoteName[] FLAT_ORDER = {
        NoteName.B, NoteName.E, NoteName.A, NoteName.D, NoteName.G, NoteName.C, NoteName.F
    };

    public static AbcImport read(final String abc) {
        final String[] lines = abc.split("\n");

        String title = "";
        Fraction defaultLength = Fraction.of(1, 8);
        Tempo tempo = Tempo.of(120);
        Map<NoteName, Accidental> keyAccidentals = Map.of();
        int bodyStart = lines.length;

        for (int i = 0; i < lines.length; i++) {
            final String line = lines[i].trim();
            if (line.startsWith("%") || line.isEmpty()) {
                continue;
            }
            if (line.startsWith("T:") && title.isEmpty()) {
                title = line.substring(2).trim();
            } else if (line.startsWith("L:")) {
                defaultLength = parseFraction(line.substring(2).trim());
            } else if (line.startsWith("Q:")) {
                tempo = parseTempo(line.substring(2).trim(), defaultLength);
            } else if (line.startsWith("K:")) {
                keyAccidentals = parseKeySignature(line.substring(2).trim());
                bodyStart = i + 1;
                break;
            }
        }

        // Build per-voice body content, splitting on V: lines in the body
        final String defaultVoiceName = title.isBlank() ? "voice" : title.toLowerCase().replace(' ', '_');
        final List<String> voiceOrder = new ArrayList<>();
        final Map<String, StringBuilder> voiceBodies = new LinkedHashMap<>();
        String currentVoice = defaultVoiceName;
        voiceOrder.add(currentVoice);
        voiceBodies.put(currentVoice, new StringBuilder());

        for (int i = bodyStart; i < lines.length; i++) {
            String line = lines[i];
            final int pct = line.indexOf('%');
            if (pct >= 0) {
                line = line.substring(0, pct);
            }
            final String trimmedLine = line.trim();
            if (trimmedLine.startsWith("V:")) {
                final String vSpec = trimmedLine.substring(2).trim();
                final String vName = vSpec.split("\\s+")[0].toLowerCase();
                currentVoice = vName.isEmpty() ? defaultVoiceName : vName;
                if (!voiceBodies.containsKey(currentVoice)) {
                    voiceOrder.add(currentVoice);
                    voiceBodies.put(currentVoice, new StringBuilder());
                }
                continue;
            }
            if (line.endsWith("\\")) {
                line = line.substring(0, line.length() - 1);
            }
            voiceBodies.get(currentVoice).append(line).append(' ');
        }

        final List<Voice> voices = new ArrayList<>();
        for (final String vName : voiceOrder) {
            final List<NoteEvent> events = parseBody(
                voiceBodies.get(vName).toString(), defaultLength, keyAccidentals);
            if (!events.isEmpty()) {
                voices.add(Voice.of(vName, events));
            }
        }

        if (voices.isEmpty()) {
            voices.add(Voice.of(defaultVoiceName, List.of()));
        }

        return new AbcImport(Collections.unmodifiableList(voices), tempo, title);
    }

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    private static List<NoteEvent> parseBody(
        final String body,
        final Fraction defaultLength,
        final Map<NoteName, Accidental> keyAccidentals) {

        final List<NoteEvent> events = new ArrayList<>();
        final Map<NoteName, Accidental> measureAccidentals = new EnumMap<>(NoteName.class);
        boolean tieNext = false;
        int tupletRemaining = 0;
        Fraction tupletMultiplier = Fraction.of(1, 1);

        try (var sc = new Scanner(body).register(Filter.WHITESPACE)) {
            while (sc.hasNext()) {

                // Guitar chord annotation "..."
                if (sc.follows("\"")) {
                    sc.consume(1);
                    sc.skipUntil("\"");
                    sc.skip("\"");
                    continue;
                }

                // Grace notes {...}
                if (sc.follows("{")) {
                    sc.consume(1);
                    sc.skipUntil("}");
                    sc.skip("}");
                    continue;
                }

                // Decorations !...!
                if (sc.follows("!")) {
                    sc.consume(1);
                    sc.skipUntil("!");
                    sc.skip("!");
                    continue;
                }

                // Tuplet (3, (3:2:3 — open paren followed by a digit
                if (sc.follows(Pattern.compile("\\([0-9]"))) {
                    sc.consume(1); // (
                    final String spec = sc.consumeWhile(Pattern.compile("[0-9:]"));
                    final String[] parts = spec.split(":");
                    final int p = Integer.parseInt(parts[0]);
                    final int q = parts.length >= 2 ? Integer.parseInt(parts[1]) : defaultTupletQ(p);
                    tupletRemaining = parts.length >= 3 ? Integer.parseInt(parts[2]) : p;
                    tupletMultiplier = Fraction.of(q, p);
                    continue;
                }

                // Slur open/close and + decoration
                if (sc.follows("(") || sc.follows(")") || sc.follows("+")) {
                    sc.consume(1);
                    continue;
                }

                // Inline field [X:...] — letter followed by colon inside brackets
                if (sc.follows(Pattern.compile("\\[[A-Z]:"))) {
                    sc.consume(1); // [
                    sc.skipUntil("]");
                    sc.skip("]");
                    continue;
                }

                // Chord [CEG] — parse pitches, emit Chord event; [| is handled below
                if (sc.follows("[") && !sc.follows("[|")) {
                    final List<Pitch> pitches = parseChordPitches(sc, measureAccidentals, keyAccidentals);
                    final Fraction dur = readLength(sc, defaultLength);
                    final Fraction actualDur = applyTuplet(dur, tupletRemaining > 0, tupletMultiplier);
                    if (tupletRemaining > 0) {
                        tupletRemaining--;
                    }
                    if (!pitches.isEmpty()) {
                        events.add(Chord.of(pitches, new FractionDuration(actualDur), Velocity.MF));
                    }
                    tieNext = false;
                    continue;
                }

                // Bar lines: |, ||, |:, :|, [|, |], and variants — reset measure accidentals
                if (sc.follows("|") || sc.follows(":") || sc.follows("[|")) {
                    sc.skipWhile(Pattern.compile("[|:\\[\\]]"));
                    // Skip repeat-ending number: |1, |2, etc.
                    if (sc.follows(Pattern.compile("[0-9]"))) {
                        sc.consume(1);
                    }
                    measureAccidentals.clear();
                    tieNext = false;
                    continue;
                }

                // > < dotted-note shorthand — notes keep their written lengths
                if (sc.follows(">") || sc.follows("<")) {
                    sc.consume(1);
                    continue;
                }

                // Stray tie marker (tie is normally consumed after the note below)
                if (sc.follows("-")) {
                    sc.consume(1);
                    continue;
                }

                // Accidentals
                Accidental explicit = null;
                if (sc.follows("^")) {
                    sc.consume(1);
                    if (sc.follows("^")) {
                        sc.consume(1);
                        explicit = Accidental.DOUBLE_SHARP;
                    } else {
                        explicit = Accidental.SHARP;
                    }
                } else if (sc.follows("_")) {
                    sc.consume(1);
                    if (sc.follows("_")) {
                        sc.consume(1);
                        explicit = Accidental.DOUBLE_FLAT;
                    } else {
                        explicit = Accidental.FLAT;
                    }
                } else if (sc.follows("=")) {
                    sc.consume(1);
                    explicit = Accidental.NATURAL;
                }

                // Rest: z (audible) or x (invisible)
                if (sc.follows("z") || sc.follows("x")) {
                    sc.consume(1);
                    final Fraction dur = readLength(sc, defaultLength);
                    final Fraction actualDur = applyTuplet(dur, tupletRemaining > 0, tupletMultiplier);
                    if (tupletRemaining > 0) {
                        tupletRemaining--;
                    }
                    tieNext = false;
                    events.add(Rest.of(new FractionDuration(actualDur)));
                    continue;
                }

                // Note: A-G or a-g
                final var noteChar = sc.optionallyConsume(Pattern.compile("[A-Ga-g]"));
                if (noteChar.isPresent()) {
                    final char c = noteChar.get().charAt(0);
                    final int baseOctave = Character.isUpperCase(c) ? 3 : 4;
                    final NoteName name = charToNoteName(c);

                    int octave = baseOctave;
                    octave += sc.consumeWhile(Pattern.compile("'")).length();
                    octave -= sc.consumeWhile(Pattern.compile(",")).length();

                    final Fraction dur = readLength(sc, defaultLength);
                    final Fraction actualDur = applyTuplet(dur, tupletRemaining > 0, tupletMultiplier);
                    if (tupletRemaining > 0) {
                        tupletRemaining--;
                    }

                    final Accidental acc;
                    if (explicit != null) {
                        acc = explicit;
                        measureAccidentals.put(name, explicit);
                    } else if (measureAccidentals.containsKey(name)) {
                        acc = measureAccidentals.get(name);
                    } else {
                        acc = keyAccidentals.getOrDefault(name, Accidental.NATURAL);
                    }

                    final SpelledPitch pitch = SpelledPitch.of(name, acc, octave);

                    // Merge if tied from previous note with matching pitch
                    if (tieNext && !events.isEmpty()
                        && events.get(events.size() - 1) instanceof Note prevNote
                        && prevNote.pitch().spelled().equals(pitch)) {
                        final Fraction merged = ((FractionDuration) prevNote.duration()).fraction().add(actualDur);
                        events.set(events.size() - 1, Note.of(pitch, new FractionDuration(merged)));
                        tieNext = sc.follows("-");
                        if (tieNext) {
                            sc.consume(1);
                        }
                        continue;
                    }

                    events.add(Note.of(pitch, new FractionDuration(actualDur)));
                    tieNext = sc.follows("-");
                    if (tieNext) {
                        sc.consume(1);
                    }
                    continue;
                }

                // Anything else — skip
                sc.consume(1);
            }
        } catch (final Exception e) {
            // Scanner.close() is AutoCloseable via Reader; no checked exceptions expected
        }

        return Collections.unmodifiableList(events);
    }

    private static List<Pitch> parseChordPitches(final Scanner sc,
                                                 final Map<NoteName, Accidental> measureAccidentals,
                                                 final Map<NoteName, Accidental> keyAccidentals) {

        sc.consume(1); // [
        final List<Pitch> pitches = new ArrayList<>();
        while (sc.hasNext() && !sc.follows("]")) {
            Accidental chordAcc = null;
            if (sc.follows("^")) {
                sc.consume(1);
                if (sc.follows("^")) {
                    sc.consume(1);
                    chordAcc = Accidental.DOUBLE_SHARP;
                } else {
                    chordAcc = Accidental.SHARP;
                }
            } else if (sc.follows("_")) {
                sc.consume(1);
                if (sc.follows("_")) {
                    sc.consume(1);
                    chordAcc = Accidental.DOUBLE_FLAT;
                } else {
                    chordAcc = Accidental.FLAT;
                }
            } else if (sc.follows("=")) {
                sc.consume(1);
                chordAcc = Accidental.NATURAL;
            }

            final var cn = sc.optionallyConsume(Pattern.compile("[A-Ga-g]"));
            if (cn.isEmpty()) {
                sc.consume(1);
                continue;
            }
            final char cc = cn.get().charAt(0);
            final int baseOct = Character.isUpperCase(cc) ? 3 : 4;
            final NoteName cName = charToNoteName(cc);
            int cOctave = baseOct;
            cOctave += sc.consumeWhile(Pattern.compile("'")).length();
            cOctave -= sc.consumeWhile(Pattern.compile(",")).length();
            sc.optionallyConsume(LENGTH_PATTERN); // per-note lengths inside chord are ignored

            final Accidental cAcc = chordAcc != null ? chordAcc
                : measureAccidentals.getOrDefault(cName,
                keyAccidentals.getOrDefault(cName, Accidental.NATURAL));
            pitches.add(SpelledPitch.of(cName, cAcc, cOctave));
        }
        if (sc.follows("]")) {
            sc.skip("]");
        }
        return pitches;
    }

    private static Fraction applyTuplet(
        final Fraction dur,
        final boolean inTuplet,
        final Fraction multiplier) {
        return inTuplet ? dur.multiply(multiplier) : dur;
    }

    private static int defaultTupletQ(final int p) {
        return switch (p) {
            case 2 -> 3;
            case 3 -> 2;
            case 4 -> 3;
            case 6 -> 2;
            case 8 -> 3;
            default -> 2;
        };
    }

    private static final Pattern LENGTH_PATTERN = Pattern.compile("(?:[0-9]+/?[0-9]*|/[0-9]*)");

    private static Fraction readLength(final Scanner sc, final Fraction defaultLength) {
        final var matched = sc.optionallyConsume(LENGTH_PATTERN);
        if (matched.isEmpty()) {
            return defaultLength;
        }
        final String s = matched.get();
        final int slash = s.indexOf('/');
        if (slash < 0) {
            return defaultLength.multiply(Integer.parseInt(s));
        }
        final int num = slash == 0 ? 1 : Integer.parseInt(s.substring(0, slash));
        final String denStr = s.substring(slash + 1);
        final int den = denStr.isEmpty() ? 2 : Integer.parseInt(denStr);
        return defaultLength.multiply(Fraction.of(num, den));
    }

    private static Tempo parseTempo(final String q, final Fraction defaultLength) {
        final String trimmed = q.trim();
        final int eqIdx = trimmed.indexOf('=');
        if (eqIdx >= 0) {
            try {
                final int bpm = Integer.parseInt(trimmed.substring(eqIdx + 1).trim());
                final Fraction beat = parseFractionSafe(trimmed.substring(0, eqIdx).trim(), defaultLength);
                final double quarterBpm = bpm * beat.toDouble() * 4.0;
                return Tempo.of(Math.clamp((int) Math.round(quarterBpm), 1, 400));
            } catch (final NumberFormatException e) {
                return Tempo.of(120);
            }
        }
        try {
            final int bpm = Integer.parseInt(trimmed);
            final double quarterBpm = bpm * defaultLength.toDouble() * 4.0;
            return Tempo.of(Math.clamp((int) Math.round(quarterBpm), 1, 400));
        } catch (final NumberFormatException e) {
            return Tempo.of(120);
        }
    }

    private static Fraction parseFractionSafe(final String s, final Fraction fallback) {
        try {
            return parseFraction(s);
        } catch (final Exception e) {
            return fallback;
        }
    }

    private static Fraction parseFraction(final String s) {
        final String t = s.trim();
        final int slash = t.indexOf('/');
        if (slash < 0) {
            return Fraction.of(Integer.parseInt(t), 1);
        }
        return Fraction.of(
            Integer.parseInt(t.substring(0, slash).trim()),
            Integer.parseInt(t.substring(slash + 1).trim()));
    }

    private static Map<NoteName, Accidental> parseKeySignature(final String key) {
        final String normalized = normalizeKey(key.trim());
        final int sharps = keySharps(normalized);
        return buildAccidentalMap(sharps);
    }

    private static String normalizeKey(final String key) {
        if (key.isEmpty()) {
            return "C";
        }
        final String lower = key.toLowerCase();
        if (lower.startsWith("hp") || lower.startsWith("none") || lower.startsWith("free")) {
            return "C";
        }

        final char tonic = Character.toUpperCase(key.charAt(0));
        final StringBuilder sb = new StringBuilder().append(tonic);
        int idx = 1;

        if (idx < key.length() && key.charAt(idx) == 'b') {
            sb.append('b');
            idx++;
        } else if (idx < key.length() && key.charAt(idx) == '#') {
            sb.append('#');
            idx++;
        }

        if (idx < key.length()) {
            final char next = key.charAt(idx);
            if ((next == 'm' || next == 'M') && !key.substring(idx).toLowerCase().startsWith("maj")) {
                sb.append('m');
            }
        }

        return sb.toString();
    }

    private static int keySharps(final String key) {
        return switch (key) {
            case "C" -> 0;
            case "G" -> 1;
            case "D" -> 2;
            case "A" -> 3;
            case "E" -> 4;
            case "B" -> 5;
            case "F#" -> 6;
            case "C#" -> 7;
            case "F" -> -1;
            case "Bb" -> -2;
            case "Eb" -> -3;
            case "Ab" -> -4;
            case "Db" -> -5;
            case "Gb" -> -6;
            case "Cb" -> -7;
            case "Am" -> 0;
            case "Em" -> 1;
            case "Bm" -> 2;
            case "F#m" -> 3;
            case "C#m" -> 4;
            case "G#m" -> 5;
            case "D#m" -> 6;
            case "A#m" -> 7;
            case "Dm" -> -1;
            case "Gm" -> -2;
            case "Cm" -> -3;
            case "Fm" -> -4;
            case "Bbm" -> -5;
            case "Ebm" -> -6;
            case "Abm" -> -7;
            default -> 0;
        };
    }

    private static Map<NoteName, Accidental> buildAccidentalMap(final int sharps) {
        final Map<NoteName, Accidental> map = new EnumMap<>(NoteName.class);
        if (sharps > 0) {
            for (int i = 0; i < sharps && i < 7; i++) {
                map.put(SHARP_ORDER[i], Accidental.SHARP);
            }
        } else if (sharps < 0) {
            for (int i = 0; i < -sharps && i < 7; i++) {
                map.put(FLAT_ORDER[i], Accidental.FLAT);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private static NoteName charToNoteName(final char c) {
        return switch (Character.toUpperCase(c)) {
            case 'C' -> NoteName.C;
            case 'D' -> NoteName.D;
            case 'E' -> NoteName.E;
            case 'F' -> NoteName.F;
            case 'G' -> NoteName.G;
            case 'A' -> NoteName.A;
            case 'B' -> NoteName.B;
            default -> throw new IllegalArgumentException("Not a note name: " + c);
        };
    }
}
