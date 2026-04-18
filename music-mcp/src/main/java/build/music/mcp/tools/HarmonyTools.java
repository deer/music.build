package build.music.mcp.tools;

import build.music.core.ChordSymbol;
import build.music.core.NoteEvent;
import build.music.harmony.ChordProgression;
import build.music.harmony.DiatonicTranspose;
import build.music.harmony.HarmonicAnalyzer;
import build.music.harmony.Harmonizer;
import build.music.harmony.Key;
import build.music.mcp.CompositionContext;
import build.music.mcp.ToolResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MCP tools for harmonic operations: set key, chord progressions, harmonize.
 */
public final class HarmonyTools {

    private HarmonyTools() {}

    /** Tool: harmony.set_key — set the current key for the composition. */
    public static ToolResult setKey(final CompositionContext ctx, final String keyDescription) {
        try {
            final Key key = Key.parse(keyDescription);
            ctx.setKey(key);
            final int sig = key.signatureAccidentals();
            final String sigDesc = sig == 0 ? "no sharps or flats"
                : sig > 0 ? sig + " sharp" + (sig == 1 ? "" : "s")
                : Math.abs(sig) + " flat" + (Math.abs(sig) == 1 ? "" : "s");
            return ToolResult.success("Key set to " + key + " (" + sigDesc + ").");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /** Tool: harmony.chord_progression — create a chord progression from Roman numerals. */
    public static ToolResult setChordProgression(final CompositionContext ctx, final String progressionStr) {
        try {
            final ChordProgression prog = ChordProgression.parse(progressionStr);
            ctx.setProgression(prog);

            String resolved = "";
            if (ctx.hasKey()) {
                final List<ChordSymbol> chords = prog.inKey(ctx.getKey());
                resolved = " → in " + ctx.getKey() + ": " +
                    chords.stream().map(ChordSymbol::toString).collect(Collectors.joining(", "));
            }
            return ToolResult.success("Chord progression set: " + prog + resolved + ".");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /** Tool: harmony.harmonize — add chord root accompaniment to a voice. */
    public static ToolResult harmonize(final CompositionContext ctx, final String voiceName,
            final String targetVoice, final int voicingOctave) {
        try {
            if (!ctx.hasKey()) {
                return ToolResult.error("No key set. Use harmony.set_key first.");
            }
            if (ctx.getProgression() == null) {
                return ToolResult.error("No chord progression set. Use harmony.chord_progression first.");
            }
            final List<NoteEvent> melody = ctx.getVoice(voiceName);
            final List<NoteEvent> harmony = Harmonizer.harmonize(
                melody, ctx.getKey(), ctx.getProgression(), voicingOctave, ctx.getTimeSignature());
            final String target = targetVoice != null ? targetVoice : voiceName + "_harmony";
            ctx.createVoice(target, harmony);
            return ToolResult.success("Created harmony voice '" + target + "' with " +
                harmony.size() + " chord root notes.");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /** Tool: harmony.suggest_harmony — auto-suggest a chord progression for a melody. */
    public static ToolResult suggestHarmony(final CompositionContext ctx, final String voiceName) {
        try {
            if (!ctx.hasKey()) {
                return ToolResult.error("No key set. Use harmony.set_key first.");
            }
            final List<NoteEvent> melody = ctx.getVoice(voiceName);
            final ChordProgression suggestion = Harmonizer.suggestHarmony(
                melody, ctx.getKey(), ctx.getTimeSignature());
            ctx.setProgression(suggestion);
            final List<ChordSymbol> chords = suggestion.inKey(ctx.getKey());
            final String chordsStr = chords.stream().map(ChordSymbol::toString).collect(Collectors.joining(", "));
            return ToolResult.success("Suggested harmony for '" + voiceName + "': " +
                suggestion + " → " + chordsStr + ". Progression saved.");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /** Tool: harmony.detect_key — detect the key of an existing voice. */
    public static ToolResult detectKey(final CompositionContext ctx, final String voiceName) {
        try {
            final List<NoteEvent> events = ctx.getVoice(voiceName);
            final Key detected = HarmonicAnalyzer.detectKey(events);
            ctx.setKey(detected);
            return ToolResult.success("Detected key: " + detected + ". Key has been set.");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /**
     * Tool: harmony.set_bars — declare chord changes per measure as concrete chord symbols.
     * Format: "1:Cm7 2:F7 3:BbM7 4:Eb" — bar number, colon, chord symbol.
     * Valid chord qualities: (none)=major, m=minor, 7=dom7, maj7, m7, m7b5, dim, dim7, aug, sus2, sus4.
     * Example: "1:Am7b5 2:D7 3:Gm7 4:Gm7" for a ii-V-i in G minor.
     * These chords are used by harmony.walking_bass and override harmony.chord_progression for those tools.
     */
    public static ToolResult setBarChords(final CompositionContext ctx, final String barChordsStr) {
        try {
            final Map<Integer, ChordSymbol> chords = new LinkedHashMap<>();
            final String[] tokens = barChordsStr.trim().split("\\s+");
            for (final String token : tokens) {
                final int colon = token.indexOf(':');
                if (colon < 0) {
                    return ToolResult.error(
                        "Invalid bar chord token '" + token +
                        "'. Expected 'bar:chord' e.g. '1:Cm7'. Got: '" + token + "'.");
                }
                final int bar = Integer.parseInt(token.substring(0, colon).trim());
                final ChordSymbol symbol = ChordSymbol.parse(token.substring(colon + 1).trim());
                chords.put(bar, symbol);
            }
            if (chords.isEmpty()) {
                return ToolResult.error("No bar chords found in input.");
            }
            ctx.setBarChords(chords);
            final String summary = chords.entrySet().stream()
                .map(e -> "bar " + e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", "));
            return ToolResult.success(
                "Set " + chords.size() + " bar chord(s): " + summary + ".");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /**
     * Tool: harmony.walking_bass — generate a 4/4 walking bass line over chord changes.
     * Produces a pattern of root-fifth-third-approach per bar.
     * Requires either harmony.set_bars (preferred) or harmony.set_key + harmony.chord_progression.
     *
     * @param targetVoice name for the generated bass voice (default "bass")
     * @param octave      octave for bass notes (default 2, i.e. C2–B2 range)
     * @param bars        number of bars (default = number of chords defined)
     */
    public static ToolResult walkingBass(
            final CompositionContext ctx, final String targetVoice, final int octave, final Integer bars) {
        try {
            List<ChordSymbol> chords;

            if (ctx.hasBarChords()) {
                // Use bar-level chord map (sorted by bar number)
                chords = ctx.getBarChords().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());
            } else if (ctx.hasKey() && ctx.getProgression() != null) {
                chords = ctx.getProgression().inKey(ctx.getKey());
            } else {
                return ToolResult.error(
                    "No chord information found. Use harmony.set_bars to declare chord changes, " +
                    "or use harmony.set_key + harmony.chord_progression first.");
            }

            // Optionally repeat/trim to requested bar count
            if (bars != null && bars > 0 && bars != chords.size()) {
                final List<ChordSymbol> repeated = new ArrayList<>();
                for (int i = 0; i < bars; i++) {
                    repeated.add(chords.get(i % chords.size()));
                }
                chords = repeated;
            }

            if (!ctx.hasKey()) {
                return ToolResult.error(
                    "No key set. Walking bass approach notes require a key. Use harmony.set_key first.");
            }

            final List<NoteEvent> bassLine = Harmonizer.walkingBass(chords, ctx.getKey(), octave, ctx.getTimeSignature());
            final String target = targetVoice != null && !targetVoice.isBlank() ? targetVoice : "bass";
            ctx.createVoice(target, bassLine);
            return ToolResult.success(
                "Generated walking bass '" + target + "': " + chords.size() +
                " bar(s), " + bassLine.size() + " notes at octave " + octave + ". " +
                "Assign to electric_bass or synth_bass for playback.");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /**
     * Tool: harmony.comp — generate a comping (chord accompaniment) voice over bar-level chord changes.
     *
     * Styles:
     *   quarter_stabs  — block chord on beats 2 and 4 (jazz comping default)
     *   on_beat        — block chord on every beat
     *   eighth_pump    — block chord on every eighth note (funk/rock)
     *   shell_voicings — root + 7th (or root + 5th for triads) on beats 2 and 4
     *   charleston     — dotted quarter + eighth + quarter (classic jazz Charleston rhythm)
     *
     * @param targetVoice name for the generated comping voice (default "comp")
     * @param octave      voicing octave (default 3 — mid-range Rhodes/piano comping)
     * @param style       comping style name (default "quarter_stabs")
     * @param bars        number of bars (default = number of chords defined)
     */
    public static ToolResult comp(
            final CompositionContext ctx, final String targetVoice, final int octave, final String style, final Integer bars) {
        try {
            List<ChordSymbol> chords;

            if (ctx.hasBarChords()) {
                chords = ctx.getBarChords().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());
            } else if (ctx.hasKey() && ctx.getProgression() != null) {
                chords = ctx.getProgression().inKey(ctx.getKey());
            } else {
                return ToolResult.error(
                    "No chord information found. Use harmony.set_bars to declare chord changes, " +
                    "or use harmony.set_key + harmony.chord_progression first.");
            }

            // Repeat/trim to requested bar count
            if (bars != null && bars > 0 && bars != chords.size()) {
                final List<ChordSymbol> repeated = new ArrayList<>();
                for (int i = 0; i < bars; i++) {
                    repeated.add(chords.get(i % chords.size()));
                }
                chords = repeated;
            }

            final String compStyle = style != null && !style.isBlank() ? style : "quarter_stabs";
            final List<NoteEvent> compVoice = Harmonizer.comp(chords, octave, compStyle, ctx.getTimeSignature());
            final String target = targetVoice != null && !targetVoice.isBlank() ? targetVoice : "comp";
            ctx.createVoice(target, compVoice);
            return ToolResult.success(
                "Generated comping voice '" + target + "' (" + compStyle + " style): " +
                chords.size() + " bar(s), " + compVoice.size() + " events at octave " + octave + ". " +
                "Assign to piano, rhodes, or guitar instrument for playback.");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /** Tool: harmony.diatonic_transpose — transpose a voice by scale steps within the key. */
    public static ToolResult diatonicTranspose(final CompositionContext ctx,
            final String voiceName, final int steps, final String targetVoice) {
        try {
            if (!ctx.hasKey()) {
                return ToolResult.error("No key set. Use harmony.set_key first.");
            }
            final List<NoteEvent> events = ctx.getVoice(voiceName);
            final List<NoteEvent> transposed = DiatonicTranspose.transpose(events, ctx.getKey(), steps);
            final String target = targetVoice != null ? targetVoice : voiceName + "_diatonic";
            ctx.createVoice(target, transposed);
            final String direction = steps >= 0 ? "up" : "down";
            return ToolResult.success("Diatonically transposed '" + voiceName + "' " + direction +
                " by " + Math.abs(steps) + " step" + (Math.abs(steps) == 1 ? "" : "s") +
                " in " + ctx.getKey() + " → created '" + target + "'.");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }
}
