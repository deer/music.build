package build.music.mcp.tools;

import build.music.core.Articulation;
import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.core.Rest;
import build.music.core.Velocity;
import build.music.mcp.CompositionContext;
import build.music.mcp.ToolResult;
import build.music.pitch.SpelledPitch;
import build.music.time.DottedValue;
import build.music.time.RhythmicValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pre-built drum pattern library.
 * <p>
 * GM drum note mapping (MIDI channel 9):
 * C2  (36) = Bass Drum
 * C#2 (37) = Side Stick
 * D2  (38) = Snare
 * E2  (40) = Electric Snare
 * F#2 (42) = Closed Hi-Hat
 * A#2 (46) = Open Hi-Hat
 * C#3 (49) = Crash
 * D#3 (51) = Ride
 */
public final class DrumPresets {

    private DrumPresets() {
    }

    public static Set<String> available() {
        return Set.of("house_4on4", "rock_8th", "rock_basic", "bossa_nova", "waltz", "waltz_jazz", "swing", "afrohouse");
    }

    /**
     * Returns a map of voice-name → one-bar note event list for the given preset.
     * The caller should repeat and register these as voices on channel 9.
     */
    public static Map<String, List<NoteEvent>> oneBar(final String presetName) {
        return switch (presetName.toLowerCase()) {
            case "house_4on4" -> houseOneBar();
            case "rock_8th" -> rockEighthOneBar();
            case "rock_basic" -> rockBasicOneBar();
            case "bossa_nova" -> bossaOneBar();
            case "waltz" -> waltzOneBar();
            case "waltz_jazz" -> waltzJazzOneBar();
            case "swing" -> swingOneBar();
            case "afrohouse" -> afroHouseOneBar();
            default -> throw new IllegalArgumentException(
                "Unknown drum preset '" + presetName + "'. Available: " + available());
        };
    }

    /**
     * Tool: drums.preset — load a preset and create voices in the context.
     */
    public static ToolResult loadPreset(final CompositionContext ctx, final String presetName, final int bars) {
        if (bars < 1 || bars > 64) {
            return ToolResult.error("bars must be between 1 and 64. Got: " + bars);
        }

        final Map<String, List<NoteEvent>> pattern;
        try {
            pattern = oneBar(presetName);
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }

        final List<String> createdVoices = new ArrayList<>();
        for (final Map.Entry<String, List<NoteEvent>> entry : pattern.entrySet()) {
            final String voiceName = "drums_" + entry.getKey();
            final List<NoteEvent> repeated = repeatPattern(entry.getValue(), bars);
            ctx.createVoice(voiceName, repeated);
            // Assign to channel 9 (GM percussion), program 0
            ctx.assignPart(voiceName, 9, 0, "drums");
            createdVoices.add(voiceName);
        }

        return ToolResult.success(
            "Loaded preset '" + presetName + "' for " + bars + " bars. " +
                "Created voices: " + createdVoices + ". All assigned to MIDI channel 9 (GM percussion).");
    }

    // --- Pattern definitions ---

    /**
     * house_4on4: four-on-the-floor kick, clap on 2&4, eighth-note hi-hats.
     */
    private static Map<String, List<NoteEvent>> houseOneBar() {
        final Map<String, List<NoteEvent>> map = new LinkedHashMap<>();

        // kick: C2/q C2/q C2/q C2/q
        map.put("kick", List.of(
            kick(), kick(), kick(), kick()
        ));

        // clap: r/q C#2/q r/q C#2/q
        map.put("clap", List.of(
            qr(), sidestick(), qr(), sidestick()
        ));

        // hihat: 8 eighth notes
        map.put("hihat", List.of(
            hihat(), hihat(), hihat(), hihat(),
            hihat(), hihat(), hihat(), hihat()
        ));

        return map;
    }

    /**
     * rock_8th: kick on 1&3, snare on 2&4, eighth hi-hats.
     */
    private static Map<String, List<NoteEvent>> rockEighthOneBar() {
        final Map<String, List<NoteEvent>> map = new LinkedHashMap<>();

        // kick: C2/q r/q C2/q r/q
        map.put("kick", List.of(kick(), qr(), kick(), qr()));

        // snare: r/q D2/q r/q D2/q
        map.put("snare", List.of(qr(), snare(), qr(), snare()));

        // hihat: 8 eighth notes
        map.put("hihat", List.of(
            hihat(), hihat(), hihat(), hihat(),
            hihat(), hihat(), hihat(), hihat()
        ));

        return map;
    }

    /**
     * rock_basic: kick on 1 and and-of-3, snare on 2&4, eighth hi-hats.
     */
    private static Map<String, List<NoteEvent>> rockBasicOneBar() {
        final Map<String, List<NoteEvent>> map = new LinkedHashMap<>();

        // kick: C2/q r/q C2/e C2/e r/q
        map.put("kick", List.of(kick(), qr(), kickE(), kickE(), qr()));

        // snare: r/q D2/q r/q D2/q
        map.put("snare", List.of(qr(), snare(), qr(), snare()));

        // hihat: 8 eighth notes
        map.put("hihat", List.of(
            hihat(), hihat(), hihat(), hihat(),
            hihat(), hihat(), hihat(), hihat()
        ));

        return map;
    }

    /**
     * bossa_nova: kick on dotted-quarter + eighth pattern, rim on offbeats,
     * closed hi-hat on every eighth, clave (tresillo) on dotted-quarter + dotted-quarter + quarter.
     * <p>
     * Tresillo (3+3+2 in 8th notes): hits on beats 1, and-of-2, beat-4 — the rhythmic
     * spine of bossa nova and much of Latin music. GM note 75 (Eb5) = Claves.
     */
    private static Map<String, List<NoteEvent>> bossaOneBar() {
        final Map<String, List<NoteEvent>> map = new LinkedHashMap<>();

        // kick: C2/dq C2/e r/q C2/q
        map.put("kick", List.of(kickDQ(), kickE(), qr(), kick()));

        // rim: E2/q r/e E2/e E2/q r/q
        map.put("rim", List.of(rim(), er(), rimE(), rim(), qr()));

        // hihat: closed hi-hat (F#2) on every eighth — 8 per bar
        map.put("hihat", List.of(
            hihat(), hihat(), hihat(), hihat(),
            hihat(), hihat(), hihat(), hihat()
        ));

        // clave: tresillo pattern — dq + dq + q (3+3+2 eighths = 1 bar)
        // GM 75 = Claves = Eb5
        map.put("clave", List.of(clave(), clave(), claveQ()));

        return map;
    }

    /**
     * waltz: 3/4 — kick on beat 1, snare on beats 2&3.
     */
    private static Map<String, List<NoteEvent>> waltzOneBar() {
        final Map<String, List<NoteEvent>> map = new LinkedHashMap<>();

        // kick: C2/q r/q r/q
        map.put("kick", List.of(kick(), qr(), qr()));

        // snare: r/q D2/q D2/q
        map.put("snare", List.of(qr(), snare(), snare()));

        return map;
    }

    /**
     * waltz_jazz: 3/4 — kick + hi-hat on beat 1, brushed snare (soft) on beats 2 & 3.
     * More idiomatic than "waltz" for jazz contexts: hi-hat layers with the kick on 1,
     * snare plays quietly (MP) suggesting brushes rather than sticks.
     */
    private static Map<String, List<NoteEvent>> waltzJazzOneBar() {
        final Map<String, List<NoteEvent>> map = new LinkedHashMap<>();

        // kick: C2/q r/q r/q
        map.put("kick", List.of(kick(), qr(), qr()));

        // hihat: F#2/q r/q r/q  (on beat 1 only, layered with kick)
        map.put("hihat", List.of(hihatQ(), qr(), qr()));

        // snare: r/q D2(soft)/q D2(soft)/q
        map.put("snare", List.of(qr(), softSnare(), softSnare()));

        return map;
    }

    /**
     * swing: ride on swing eighths, kick on half notes, foot hi-hat on 2&4.
     */
    private static Map<String, List<NoteEvent>> swingOneBar() {
        final Map<String, List<NoteEvent>> map = new LinkedHashMap<>();

        // ride: swing eighth approximation (dotted eighth + sixteenth pattern)
        // F#2/dq F#2/s F#2/dq F#2/s
        map.put("ride", List.of(rideDQ(), rideS(), rideDQ(), rideS()));

        // kick: C2/h C2/h
        map.put("kick", List.of(kickH(), kickH()));

        // foot_hihat: r/q F#2/q r/q F#2/q  (foot hi-hat on 2 and 4)
        map.put("foot_hihat", List.of(qr(), hihatFoot(), qr(), hihatFoot()));

        return map;
    }

    /**
     * afrohouse: syncopated kick (beat 1, and-of-2, beat 3 — NOT four-on-the-floor),
     * clap on 2&4, open hi-hat accent on and-of-2 and and-of-4, conga layer,
     * maracas running eighths.
     * <p>
     * GM notes beyond the standard range:
     * Bb2 (46) = Open Hi-Hat
     * D#4 (63) = Open Hi Conga
     * E4  (64) = Low Conga
     * Bb4 (70) = Maracas
     */
    private static Map<String, List<NoteEvent>> afroHouseOneBar() {
        final Map<String, List<NoteEvent>> map = new LinkedHashMap<>();

        // kick: beat 1, and-of-2, beat 3 — the syncopated Afrohouse kick feel
        // C2/q r/e C2/e C2/q r/q
        map.put("kick", List.of(kick(), er(), kickE(), kick(), qr()));

        // clap: backbeat on 2 and 4
        // r/q C#2/q r/q C#2/q
        map.put("clap", List.of(qr(), sidestick(), qr(), sidestick()));

        // hihat: closed eighths with open hat on and-of-2 and and-of-4 — creates breathability
        // F#2/e F#2/e Bb2/e F#2/e  F#2/e F#2/e Bb2/e F#2/e
        map.put("hihat", List.of(
            hihat(), hihat(), openHihat(), hihat(),
            hihat(), hihat(), openHihat(), hihat()
        ));

        // congas: hi conga on and-of-1 and and-of-3, low conga on and-of-2 and and-of-4
        // r/e D#4/e E4/e r/e  r/e D#4/e r/e E4/e
        map.put("congas", List.of(
            er(), hiConga(), lowConga(), er(),
            er(), hiConga(), er(), lowConga()
        ));

        // maracas: running eighths — essential Afrohouse texture layer
        // Bb4/e x8
        map.put("maracas", List.of(
            maracas(), maracas(), maracas(), maracas(),
            maracas(), maracas(), maracas(), maracas()
        ));

        return map;
    }

    // --- Note factory helpers ---

    private static Note note(final String pitch, final build.music.time.Duration duration) {
        return Note.of(SpelledPitch.parse(pitch), duration, Velocity.F, Articulation.NORMAL, false);
    }

    private static Note kick() {
        return note("C2", RhythmicValue.QUARTER);
    }

    private static Note kickE() {
        return note("C2", RhythmicValue.EIGHTH);
    }

    private static Note kickH() {
        return note("C2", RhythmicValue.HALF);
    }

    private static Note kickDQ() {
        return note("C2", new DottedValue(RhythmicValue.QUARTER, 1));
    }

    private static Note clave() {
        return note("Eb5", new DottedValue(RhythmicValue.QUARTER, 1));
    }

    private static Note claveQ() {
        return note("Eb5", RhythmicValue.QUARTER);
    }

    private static Note hihatQ() {
        return note("F#2", RhythmicValue.QUARTER);
    }

    private static Note softSnare() {
        return Note.of(SpelledPitch.parse("D2"), RhythmicValue.QUARTER, Velocity.MP, Articulation.NORMAL, false);
    }

    private static Note sidestick() {
        return note("C#2", RhythmicValue.QUARTER);
    }

    private static Note snare() {
        return note("D2", RhythmicValue.QUARTER);
    }

    private static Note rim() {
        return note("E2", RhythmicValue.QUARTER);
    }

    private static Note rimE() {
        return note("E2", RhythmicValue.EIGHTH);
    }

    private static Note hihat() {
        return note("F#2", RhythmicValue.EIGHTH);
    }

    private static Note hihatFoot() {
        return note("F#2", RhythmicValue.QUARTER);
    }

    private static Note rideDQ() {
        return note("D#3", new DottedValue(RhythmicValue.QUARTER, 1));
    }

    private static Note rideS() {
        return note("D#3", RhythmicValue.SIXTEENTH);
    }

    private static Note openHihat() {
        return note("Bb2", RhythmicValue.EIGHTH);
    }

    private static Note hiConga() {
        return note("D#4", RhythmicValue.EIGHTH);
    }

    private static Note lowConga() {
        return note("E4", RhythmicValue.EIGHTH);
    }

    private static Note maracas() {
        return Note.of(SpelledPitch.parse("Bb4"), RhythmicValue.EIGHTH, Velocity.P, Articulation.NORMAL, false);
    }

    private static Rest qr() {
        return Rest.of(RhythmicValue.QUARTER);
    }

    private static Rest er() {
        return Rest.of(RhythmicValue.EIGHTH);
    }

    private static List<NoteEvent> repeatPattern(final List<NoteEvent> bar, final int times) {
        final List<NoteEvent> result = new ArrayList<>(bar.size() * times);
        for (int i = 0; i < times; i++) {
            result.addAll(bar);
        }
        return result;
    }
}
