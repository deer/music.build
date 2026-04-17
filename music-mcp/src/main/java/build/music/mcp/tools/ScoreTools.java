package build.music.mcp.tools;

import build.music.mcp.CompositionContext;
import build.music.mcp.ToolResult;
import build.music.time.Fraction;
import build.music.time.Tempo;
import build.music.time.TimeSignature;

import java.util.Map;

/**
 * Tools for managing score-level metadata and instrument assignments.
 */
public final class ScoreTools {

    private ScoreTools() {}

    // General MIDI program numbers by instrument name.
    // "drums" is a sentinel (program 0, but channel 9 is forced — see assignInstrument).
    private static final Map<String, Integer> INSTRUMENTS = Map.ofEntries(
        // Keyboards / tuned percussion
        Map.entry("piano",              0),
        Map.entry("bright_piano",       1),
        Map.entry("honky_tonk",         3),  // Honky-tonk Piano (slightly detuned)
        Map.entry("electric_piano",     4),  // Electric Piano 1 (Rhodes-like)
        Map.entry("rhodes",             4),  // Alias for electric_piano
        Map.entry("harpsichord",        6),
        Map.entry("vibraphone",        11),
        Map.entry("marimba",           12),
        Map.entry("organ",             19),
        Map.entry("accordion",         21),
        // Guitar / bass
        Map.entry("guitar",            24),
        Map.entry("electric_guitar",   27),  // Clean electric guitar
        Map.entry("electric_bass",     33),
        Map.entry("synth_bass",        38),
        // Strings
        Map.entry("violin",            40),
        Map.entry("viola",             41),
        Map.entry("cello",             42),
        Map.entry("strings",           48),
        Map.entry("synth_strings",     50),
        // Choir
        Map.entry("choir",             52),
        // Brass / winds
        Map.entry("trumpet",           56),
        Map.entry("trombone",          57),
        Map.entry("tuba",              58),
        Map.entry("french_horn",       60),
        Map.entry("brass",             61),  // Brass section
        Map.entry("saxophone",         66),  // Tenor sax
        Map.entry("oboe",              68),
        Map.entry("bassoon",           70),
        Map.entry("clarinet",          71),
        Map.entry("flute",             73),
        // Synth leads & pads
        Map.entry("synth_lead",        81),  // Lead 2 sawtooth — classic house/trance lead
        Map.entry("synth_pad",         89),  // Pad 2 warm — classic house chord pad
        // Drums (channel 9 is forced regardless of program number)
        Map.entry("drums",              0)
    );

    /**
     * Tool: score.set_metadata — set title, tempo, and/or time signature.
     * All parameters are optional; only non-null values are updated.
     */
    public static ToolResult setMetadata(
            final CompositionContext ctx,
            final String title,
            final Integer tempo,
            final String timeSignature) {
        try {
            if (title != null && !title.isBlank()) {
                ctx.setTitle(title);
            }
            if (tempo != null) {
                if (tempo < 1 || tempo > 400) {
                    return ToolResult.error("Tempo must be between 1 and 400 BPM. Got: " + tempo);
                }
                ctx.setTempo(Tempo.of(tempo));
            }
            if (timeSignature != null && !timeSignature.isBlank()) {
                final TimeSignature ts = parseTimeSignature(timeSignature);
                ctx.setTimeSignature(ts);
            }
            return ToolResult.success("Metadata updated. " + ctx.describe().lines().limit(3)
                .reduce("", (a, b) -> a + b + " ").strip());
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /**
     * Tool: score.assign_instrument — assign a voice to a MIDI instrument.
     *
     * @param instrument instrument name, e.g. "piano", "strings", "flute"
     */
    public static ToolResult assignInstrument(
            final CompositionContext ctx, final String voiceName, final String instrument) {
        try {
            if (!ctx.hasVoice(voiceName)) {
                return ToolResult.error(
                    "Voice '" + voiceName + "' does not exist. Available voices: " +
                    ctx.voiceNames() + ". Create it first with voice.create.");
            }
            final String key = instrument.toLowerCase().replace(' ', '_');
            final Integer program = INSTRUMENTS.get(key);
            if (program == null) {
                return ToolResult.error(
                    "Unknown instrument '" + instrument + "'. Available: " +
                    String.join(", ", INSTRUMENTS.keySet()) + ".");
            }
            // Drum voices are always on channel 9 (GM percussion channel).
            // All other voices get the next free melodic channel (skipping 9).
            final int channel = key.equals("drums") ? 9 : assignChannel(ctx, voiceName);
            ctx.assignPart(voiceName, channel, program, instrument);
            return ToolResult.success(
                "Assigned '" + voiceName + "' to " + instrument +
                " (GM program " + program + ", channel " + channel + ").");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /**
     * Tool: score.set_swing — enable swing quantization for MIDI playback.
     * Swing delays off-beat eighth notes, giving jazz/funk/blues feel.
     *
     * @param ratioStr "2/3" for standard jazz swing (2:1 ratio), "3/5" for light swing,
     *                 or "0" / null to disable swing
     */
    public static ToolResult setSwing(final CompositionContext ctx, final String ratioStr) {
        try {
            if (ratioStr == null || ratioStr.isBlank() || ratioStr.equals("0")) {
                ctx.setSwingRatio(null);
                return ToolResult.success("Swing disabled — eighth notes will play straight.");
            }
            final Fraction ratio = parseFraction(ratioStr);
            if (ratio.toDouble() <= 0.5 || ratio.toDouble() >= 1.0) {
                return ToolResult.error(
                    "Swing ratio must be between 0.5 and 1.0 (exclusive). " +
                    "Typical values: 2/3 (standard jazz), 3/5 (light), 7/10 (medium).");
            }
            ctx.setSwingRatio(ratio);
            final long delay = Math.round((ratio.toDouble() - 0.5) * 480);
            return ToolResult.success(
                "Swing set to " + ratio + " (" + delay + " ticks delay on off-beat eighths). " +
                "Jazz standard: 2/3. Export MIDI to hear the effect.");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /**
     * Tool: score.set_tempo_change — declare a gradual tempo change across a bar span.
     * Generates interpolated MIDI tempo events and a rit./accel. notation mark in LilyPond.
     *
     * @param startBar  first bar of the change
     * @param endBar    last bar (target tempo reached here)
     * @param toBpm     target BPM at endBar
     * @param curve     "linear" (default) or "exponential"
     */
    public static ToolResult setTempoChange(
            final CompositionContext ctx, final int startBar, final int endBar, final int toBpm, final String curve) {
        try {
            final String resolvedCurve = (curve == null || curve.isBlank()) ? "linear" : curve.toLowerCase();
            ctx.addTempoChange(startBar, endBar, toBpm, resolvedCurve);
            final String direction = toBpm < ctx.getTempo().bpm() ? "ritardando" : "accelerando";
            return ToolResult.success(
                "Added " + direction + ": bars " + startBar + "–" + endBar +
                " → " + toBpm + " BPM (" + resolvedCurve + " curve). " +
                "Will affect both MIDI playback and LilyPond notation.");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /** Tool: score.describe — get a full description of the current composition state. */
    public static ToolResult describeScore(final CompositionContext ctx) {
        return ToolResult.success(ctx.describe());
    }

    /** Tool: score.clear — clear the entire composition and start fresh. */
    public static ToolResult clearScore(final CompositionContext ctx) {
        ctx.clear();
        return ToolResult.success("Composition cleared. Ready to start fresh.");
    }

    // --- Internal helpers ---

    private static TimeSignature parseTimeSignature(final String s) {
        final int slash = s.indexOf('/');
        if (slash < 0) {
            throw new IllegalArgumentException(
                "Invalid time signature '" + s + "'. Expected format like '4/4', '3/4', '6/8'.");
        }
        try {
            final int beats = Integer.parseInt(s.substring(0, slash).trim());
            final int unit = Integer.parseInt(s.substring(slash + 1).trim());
            return new TimeSignature(beats, unit);
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException(
                "Invalid time signature '" + s + "'. Expected format like '4/4', '3/4', '6/8'.");
        }
    }

    private static Fraction parseFraction(final String s) {
        final int slash = s.indexOf('/');
        if (slash < 0) {
            try {
                return Fraction.of(Integer.parseInt(s.trim()), 1);
            } catch (final NumberFormatException e) {
                throw new IllegalArgumentException("Invalid fraction '" + s + "'. Expected 'N/M' or integer.");
            }
        }
        try {
            final int num = Integer.parseInt(s.substring(0, slash).trim());
            final int den = Integer.parseInt(s.substring(slash + 1).trim());
            return Fraction.of(num, den);
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("Invalid fraction '" + s + "'. Expected 'N/M' (e.g. '2/3').");
        }
    }

    private static int assignChannel(final CompositionContext ctx, final String voiceName) {
        // Find the lowest channel not already assigned to another voice, skipping channel 9 (percussion)
        final var used = ctx.usedChannels();
        for (int ch = 0; ch <= 15; ch++) {
            if (ch == 9) {
                continue; // percussion
            }
            if (!used.contains(ch)) {
                return ch;
            }
        }
        // All 15 melodic channels are taken — wrap around starting from 0
        return 0;
    }
}
