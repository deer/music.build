package build.music.lilypond;

import build.music.core.Articulation;
import build.music.core.Chord;
import build.music.core.ChordSymbol;
import build.music.core.ControlChange;
import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.core.ProgramChange;
import build.music.core.Rest;
import build.music.harmony.KeySignature;
import build.music.pitch.Accidental;
import build.music.pitch.Pitch;
import build.music.pitch.SpelledPitch;
import build.music.score.Part;
import build.music.score.Score;
import build.music.score.StructuredVoice;
import build.music.score.StructuredVoice.Segment;
import build.music.time.Fraction;
import build.music.time.Tempo;
import build.music.time.TempoChange;
import build.music.time.TimeSignature;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts musical structures to LilyPond source text.
 */
public final class LilyPondRenderer {

    private LilyPondRenderer() {
    }

    /**
     * Render a complete Score to LilyPond source.
     */
    public static String render(final Score score) {
        final String keyStr = score.key() != null
            ? new KeySignature(score.key()).toLilyPond()
            : "\\key c \\major";

        final var sb = new StringBuilder();
        sb.append("\\version \"2.24.0\"\n\n");
        sb.append("\\header {\n");
        sb.append("  title = \"").append(escapeLy(score.title())).append("\"\n");
        sb.append("}\n\n");
        sb.append("\\score {\n");
        sb.append("  <<\n");

        // Chord names staff (above all instrument staves)
        if (score.barChords() != null && !score.barChords().isEmpty()) {
            sb.append("    \\new ChordNames {\n");
            sb.append("      \\chordmode {\n");
            sb.append("        ");
            sb.append(renderChordNames(score.barChords(), score));
            sb.append("\n      }\n");
            sb.append("    }\n");
        }

        // Build a bar→tempoMark map for the first staff (shows notation marks once)
        final Map<Integer, String> tempoMarks = buildTempoMarks(score);

        // Index structured voices by name for quick lookup
        final Map<String, StructuredVoice> svByName = new HashMap<>();
        for (final StructuredVoice sv : score.structuredVoices()) {
            svByName.put(sv.name(), sv);
        }

        final List<Part> orderedParts = score.scoreParts().stream()
            .sorted(Comparator.comparingInt(p -> "treble".equals(chooseClef(p.voice().events())) ? 0 : 1))
            .toList();

        boolean firstPart = true;
        for (final Part part : orderedParts) {
            final String clef = chooseClef(part.voice().events());
            sb.append("    \\new Staff \\with { instrumentName = \"")
                .append(escapeLy(part.name())).append("\" } {\n");
            sb.append("      \\clef ").append(clef).append("\n");
            sb.append("      ").append(keyStr).append("\n");
            sb.append("      \\time ").append(score.timeSignature().beats())
                .append("/").append(score.timeSignature().beatUnit()).append("\n");
            sb.append("      \\tempo 4 = ").append(score.tempo().bpm()).append("\n");
            sb.append("      ");
            final Map<Integer, String> marks = firstPart ? tempoMarks : Map.of();
            final StructuredVoice sv = svByName.get(part.name());
            if (sv != null) {
                sb.append(renderStructured(sv, score.timeSignature(), marks));
            } else {
                sb.append(renderEvents(part.voice().events(), score.timeSignature(), marks));
            }
            sb.append("\n    }\n");
            firstPart = false;
        }

        sb.append("  >>\n");
        sb.append("  \\layout {}\n");
        sb.append("  \\midi {}\n");
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Render a single voice to LilyPond source (for quick previews).
     */
    public static String render(final List<NoteEvent> events, final String title, final TimeSignature ts, final Tempo tempo) {
        final var sb = new StringBuilder();
        sb.append("\\version \"2.24.0\"\n\n");
        sb.append("\\header {\n");
        sb.append("  title = \"").append(escapeLy(title)).append("\"\n");
        sb.append("}\n\n");
        sb.append("\\score {\n");
        sb.append("  {\n");
        sb.append("    \\clef ").append(chooseClef(events)).append("\n");
        sb.append("    \\key c \\major\n");
        sb.append("    \\time ").append(ts.beats()).append("/").append(ts.beatUnit()).append("\n");
        sb.append("    \\tempo 4 = ").append(tempo.bpm()).append("\n");
        sb.append("    ");
        sb.append(renderEvents(events, ts));
        sb.append("\n  }\n");
        sb.append("  \\layout {}\n");
        sb.append("  \\midi {}\n");
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Render a StructuredVoice using volta bracket notation for Volta segments.
     * Plain segments render normally. Tempo marks are injected in the first plain/volta segment.
     */
    private static String renderStructured(
        final StructuredVoice sv, final TimeSignature ts, final Map<Integer, String> tempoMarks) {
        final var sb = new StringBuilder();
        // We track a global bar counter to inject tempo marks at the right position.
        // For simplicity, tempo marks are only injected in Plain segments (not inside volta).
        final int[] barCounter = {1};

        for (final Segment seg : sv.segments()) {
            if (seg instanceof Segment.Plain plain) {
                // Build a local marks map offset by barCounter
                final int startBar = barCounter[0];
                final Map<Integer, String> localMarks = new HashMap<>();
                for (final Map.Entry<Integer, String> e : tempoMarks.entrySet()) {
                    if (e.getKey() >= startBar) {
                        localMarks.put(e.getKey() - startBar + 1, e.getValue());
                    }
                }
                sb.append(renderEvents(plain.events(), ts, localMarks));
                sb.append(" ");
                // Advance bar counter
                final Fraction dur = plain.events().stream()
                    .map(e -> e.duration().fraction())
                    .reduce(Fraction.ZERO, Fraction::add);
                barCounter[0] += (int) Math.round(dur.toDouble() / ts.measureDuration().toDouble());
            } else if (seg instanceof Segment.Volta volta) {
                final int passCount = volta.endings().size();
                sb.append("\\repeat volta ").append(passCount).append(" {\n        ");
                sb.append(renderEvents(volta.body(), ts)).append("\n      } ");
                sb.append("\\alternative {\n");
                for (final List<NoteEvent> ending : volta.endings()) {
                    sb.append("        { ").append(renderEvents(ending, ts)).append(" }\n");
                }
                sb.append("      } ");
                // Each full pass = body + one ending
                final Fraction bodyDur = volta.body().stream()
                    .map(e -> e.duration().fraction())
                    .reduce(Fraction.ZERO, Fraction::add);
                final Fraction endDur = volta.endings().isEmpty() ? Fraction.ZERO :
                    volta.endings().get(0).stream()
                    .map(e -> e.duration().fraction())
                    .reduce(Fraction.ZERO, Fraction::add);
                final int barsPerPass = (int) Math.round(
                    (bodyDur.add(endDur)).toDouble() / ts.measureDuration().toDouble());
                barCounter[0] += barsPerPass * passCount;
            }
        }
        return sb.toString().stripTrailing();
    }

    /**
     * Build a map of bar-number → LilyPond tempo command string from the score's TempoChanges.
     * The start bar of each span gets a text mark ("rit." or "accel.") plus initial BPM.
     * Subsequent bars in the span get plain \tempo marks for MIDI accuracy.
     */
    private static Map<Integer, String> buildTempoMarks(final Score score) {
        if (score.tempoChanges().isEmpty()) {
            return Map.of();
        }
        final Map<Integer, String> marks = new HashMap<>();
        for (final TempoChange tc : score.tempoChanges()) {
            final int[] bpms = tc.interpolatedBpms();
            final String label = tc.isDecelerating() ? "rit." : "accel.";
            for (int i = 0; i < bpms.length; i++) {
                final int bar = tc.startBar() + i;
                if (i == 0) {
                    marks.put(bar, "\\tempo \"" + label + "\" 4 = " + bpms[i]);
                } else {
                    marks.put(bar, "\\tempo 4 = " + bpms[i]);
                }
            }
        }
        return marks;
    }

    /**
     * Render note events as LilyPond music, inserting bar lines and optional tempo marks.
     */
    static String renderEvents(final List<NoteEvent> events, final TimeSignature ts) {
        return renderEvents(events, ts, Map.of());
    }

    /**
     * Render note events as LilyPond music, inserting bar lines at measure boundaries.
     */
    static String renderEvents(final List<NoteEvent> events, final TimeSignature ts, final Map<Integer, String> tempoMarks) {
        final Fraction measureDur = ts.measureDuration();
        Fraction cursor = Fraction.ZERO;
        int currentBar = 1;
        final var sb = new StringBuilder();
        // Inject tempo mark for bar 1 if any
        if (tempoMarks.containsKey(1)) {
            sb.append(tempoMarks.get(1)).append(" ");
        }

        int i = 0;
        while (i < events.size()) {
            final NoteEvent event = events.get(i);

            // Detect start of a tuplet group: consecutive events sharing the same tuplet fraction
            final Fraction f = event.duration().fraction();
            final String tupletBase = tupletBaseDuration(f);
            if (tupletBase != null) {
                // Collect the full run of events with this same fraction
                int j = i;
                while (j < events.size() && events.get(j).duration().fraction().equals(f)) {
                    j++;
                }
                // Wrap the group in \tuplet 3/2 { ... }
                sb.append("\\tuplet 3/2 { ");
                for (int k = i; k < j; k++) {
                    final NoteEvent te = events.get(k);
                    switch (te) {
                        case Note n -> {
                            sb.append(renderPitch(n.pitch().spelled()));
                            sb.append(tupletBase);
                            sb.append(renderArticulation(n.articulation()));
                            if (n.tied()) {
                                sb.append("~");
                            }
                        }
                        case Chord c -> {
                            sb.append("<");
                            boolean first = true;
                            for (final Pitch p : c.pitches()) {
                                if (!first) {
                                    sb.append(" ");
                                }
                                sb.append(renderPitch(p.spelled()));
                                first = false;
                            }
                            sb.append(">").append(tupletBase);
                        }
                        case Rest r -> sb.append("r").append(tupletBase);
                        case ControlChange cc -> {} // no LilyPond representation
                        case ProgramChange pc -> {} // no LilyPond representation
                    }
                    cursor = cursor.add(te.duration().fraction());
                    if (cursor.compareTo(measureDur) >= 0) {
                        while (cursor.compareTo(measureDur) >= 0) {
                            cursor = cursor.subtract(measureDur);
                        }
                        sb.append(" |");
                        currentBar++;
                        final String mark = tempoMarks.get(currentBar);
                        if (mark != null) {
                            sb.append(" ").append(mark);
                        }
                    }
                    if (k < j - 1) {
                        sb.append(" ");
                    }
                }
                sb.append(" } ");
                i = j;
                continue;
            }

            // Normal (non-tuplet) event
            switch (event) {
                case Note n -> {
                    sb.append(renderPitch(n.pitch().spelled()));
                    sb.append(renderDuration(n.duration().fraction()));
                    sb.append(renderArticulation(n.articulation()));
                    if (n.tied()) {
                        sb.append("~");
                    }
                }
                case Chord c -> {
                    sb.append("<");
                    boolean first = true;
                    for (final Pitch p : c.pitches()) {
                        if (!first) {
                            sb.append(" ");
                        }
                        sb.append(renderPitch(p.spelled()));
                        first = false;
                    }
                    sb.append(">");
                    sb.append(renderDuration(c.duration().fraction()));
                }
                case Rest r -> {
                    sb.append("r");
                    sb.append(renderDuration(event.duration().fraction()));
                }
                case ControlChange cc -> {} // no LilyPond representation
                case ProgramChange pc -> {} // no LilyPond representation
            }
            cursor = cursor.add(event.duration().fraction());
            if (cursor.compareTo(measureDur) >= 0) {
                while (cursor.compareTo(measureDur) >= 0) {
                    cursor = cursor.subtract(measureDur);
                }
                sb.append(" |");
                currentBar++;
                final String mark = tempoMarks.get(currentBar);
                if (mark != null) {
                    sb.append(" ").append(mark);
                }
            }
            sb.append(" ");
            i++;
        }

        return sb.toString().stripTrailing();
    }

    /**
     * If the fraction corresponds to a triplet note value, return the base duration string
     * to use inside a {@code \tuplet 3/2 { }} block (e.g. "4" for quarter triplet).
     * Returns null for all other fractions.
     */
    private static String tupletBaseDuration(final Fraction f) {
        if (f.equals(Fraction.of(1, 3))) {
            return "2";
        }   // half triplet
        if (f.equals(Fraction.of(1, 6))) {
            return "4";
        }   // quarter triplet
        if (f.equals(Fraction.of(1, 12))) {
            return "8";
        }   // eighth triplet
        if (f.equals(Fraction.of(1, 24))) {
            return "16";
        }  // sixteenth triplet
        return null;
    }

    /**
     * Convert a SpelledPitch to LilyPond pitch notation (e.g. cis', bes'').
     */
    static String renderPitch(final SpelledPitch pitch) {
        final String noteName = pitch.name().name().toLowerCase();
        String accSuffix = switch (pitch.accidental()) {
            case SHARP -> "is";
            case FLAT -> "es";
            case DOUBLE_SHARP -> "isis";
            case DOUBLE_FLAT -> "eses";
            case NATURAL -> "";
        };
        // Special cases: Eb → "ees", Ab → "aes" (LilyPond conventions)
        if (pitch.accidental() == Accidental.FLAT && pitch.name().name().equals("E")) {
            accSuffix = "es";
        }
        if (pitch.accidental() == Accidental.FLAT && pitch.name().name().equals("A")) {
            accSuffix = "es";
        }

        final String octaveSuffix;
        final int oct = pitch.octave();
        if (oct >= 4) {
            octaveSuffix = "'".repeat(oct - 3);
        } else if (oct <= 2) {
            octaveSuffix = ",".repeat(3 - oct);
        } else {
            octaveSuffix = ""; // octave 3
        }

        return noteName + accSuffix + octaveSuffix;
    }

    /**
     * Convert a duration fraction to LilyPond duration string (e.g. "4", "2.", "8").
     */
    static String renderDuration(final Fraction f) {
        // Check dotted values first
        if (f.equals(Fraction.of(3, 4))) {
            return "2.";
        }   // dotted half
        if (f.equals(Fraction.of(3, 8))) {
            return "4.";
        }   // dotted quarter
        if (f.equals(Fraction.of(3, 16))) {
            return "8.";
        }   // dotted eighth
        if (f.equals(Fraction.of(3, 32))) {
            return "16.";
        }  // dotted sixteenth
        if (f.equals(Fraction.of(7, 8))) {
            return "2..";
        }  // double-dotted half
        if (f.equals(Fraction.of(7, 16))) {
            return "4..";
        }  // double-dotted quarter
        // Plain values
        if (f.equals(Fraction.ONE)) {
            return "1";
        }
        if (f.equals(Fraction.HALF)) {
            return "2";
        }
        if (f.equals(Fraction.QUARTER)) {
            return "4";
        }
        if (f.equals(Fraction.EIGHTH)) {
            return "8";
        }
        if (f.equals(Fraction.of(1, 16))) {
            return "16";
        }
        if (f.equals(Fraction.of(1, 32))) {
            return "32";
        }
        if (f.equals(Fraction.of(1, 64))) {
            return "64";
        }
        // Fallback: approximate
        final double d = f.toDouble();
        if (d >= 0.9) {
            return "1";
        }
        if (d >= 0.45) {
            return "2";
        }
        if (d >= 0.22) {
            return "4";
        }
        if (d >= 0.11) {
            return "8";
        }
        return "16";
    }

    /**
     * Convert an Articulation to LilyPond articulation suffix.
     */
    static String renderArticulation(final Articulation articulation) {
        return switch (articulation) {
            case STACCATO -> "-.";
            case STACCATISSIMO -> "-!";
            case ACCENT -> "->";
            case TENUTO -> "--";
            case MARCATO -> "-^";
            case PORTATO -> "-_";
            case NORMAL, LEGATO -> "";
        };
    }

    /**
     * Choose treble or bass clef based on average MIDI number of pitched events.
     */
    static String chooseClef(final List<NoteEvent> events) {
        long count = 0;
        long sum = 0;
        for (final NoteEvent e : events) {
            if (e instanceof Note n) {
                sum += n.midi();
                count++;
            } else if (e instanceof Chord c) {
                for (final Pitch p : c.pitches()) {
                    sum += p.midi();
                    count++;
                }
            }
        }
        if (count == 0) {
            return "treble";
        }
        return (sum / count >= 60) ? "treble" : "bass";
    }

    /**
     * Render bar chords as LilyPond chordmode content.
     * Each bar gets one whole-note chord symbol; missing bars get a spacer (s1).
     * Bar numbers are 1-based; the total measure count is derived from score duration.
     */
    private static String renderChordNames(final Map<Integer, ChordSymbol> barChords, final Score score) {
        int totalBars = (int) Math.ceil(
            score.duration().toDouble() / score.timeSignature().measureDuration().toDouble());
        if (totalBars < 1) {
            totalBars = barChords.keySet().stream().mapToInt(i -> i).max().orElse(1);
        }

        final var sb = new StringBuilder();
        for (int bar = 1; bar <= totalBars; bar++) {
            final ChordSymbol cs = barChords.get(bar);
            if (cs != null) {
                sb.append(chordToChordMode(cs));
            } else {
                sb.append("s");
            }
            sb.append("1");
            if (bar < totalBars) {
                sb.append(" | ");
            }
        }
        return sb.toString();
    }

    /**
     * Convert a ChordSymbol to a LilyPond chordmode root+modifier string (without duration).
     * Examples: C major → "c", Dm7 → "d:m7", G7 → "g:7", BbM7 → "bes:maj7".
     */
    static String chordToChordMode(final ChordSymbol cs) {
        // Root note name (lowercase)
        final String root = cs.root().name().toLowerCase();
        // Accidental suffix
        String acc = switch (cs.rootAccidental()) {
            case SHARP -> "is";
            case FLAT -> "es";
            case DOUBLE_SHARP -> "isis";
            case DOUBLE_FLAT -> "eses";
            case NATURAL -> "";
        };
        // LilyPond special cases: Eb → "ees", Ab → "aes"
        if (cs.rootAccidental() == Accidental.FLAT && cs.root().name().equals("E")) {
            acc = "es";
        }
        if (cs.rootAccidental() == Accidental.FLAT && cs.root().name().equals("A")) {
            acc = "es";
        }

        // Quality modifier (empty = major, LilyPond default)
        final String quality = switch (cs.quality()) {
            case MAJOR -> "";
            case MINOR -> ":m";
            case DIMINISHED -> ":dim";
            case AUGMENTED -> ":aug";
            case DOMINANT_7 -> ":7";
            case MAJOR_7 -> ":maj7";
            case MINOR_7 -> ":m7";
            case HALF_DIMINISHED_7 -> ":m7.5-";
            case DIMINISHED_7 -> ":dim7";
            case MINOR_MAJOR_7 -> ":m7+";
            case AUGMENTED_MAJOR_7 -> ":aug7+";
            case SUSPENDED_2 -> ":sus2";
            case SUSPENDED_4 -> ":sus4";
        };

        return root + acc + quality;
    }

    private static String escapeLy(final String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
