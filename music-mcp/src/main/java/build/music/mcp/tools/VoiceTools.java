package build.music.mcp.tools;

import build.music.core.Articulation;
import build.music.core.NoteEvent;
import build.music.core.Velocity;
import build.music.mcp.CompositionContext;
import build.music.mcp.ToolResult;
import build.music.pitch.SpelledInterval;
import build.music.pitch.SpelledPitch;
import build.music.score.Voice;
import build.music.time.Fraction;
import build.music.transform.Augment;
import build.music.transform.Invert;
import build.music.transform.Retrograde;
import build.music.transform.Transpose;
import build.music.voice.VoiceOperations;

import java.util.List;
import java.util.Map;

/**
 * Tools for creating and managing voices in the composition context.
 */
public final class VoiceTools {

    private VoiceTools() {
    }

    /**
     * Tool: voice.create — create a named voice from a note sequence string.
     */
    public static ToolResult createVoice(final CompositionContext ctx,
                                         final String name,
                                         final String notes) {
        try {
            final List<NoteEvent> events = CreateNoteTools.parseNoteSequence(notes);
            ctx.createVoice(name, events);
            return ToolResult.success(
                "Created voice '" + name + "' with " + events.size() + " events.");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /**
     * Tool: voice.append — append notes to an existing voice.
     */
    public static ToolResult appendToVoice(final CompositionContext ctx,
                                           final String name,
                                           final String notes) {
        try {
            final List<NoteEvent> events = CreateNoteTools.parseNoteSequence(notes);
            ctx.appendToVoice(name, events);
            final int total = ctx.getVoice(name).size();
            return ToolResult.success(
                "Appended " + events.size() + " events to voice '" + name +
                    "'. Total: " + total + " events.");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /**
     * Tool: voice.from_motif — create a voice by copying a motif, optionally transformed.
     *
     * @param transform     one of: "transpose", "invert", "retrograde", "augment", or null
     * @param transformArgs e.g. {"interval":"P5","direction":"up"} for transpose
     */
    public static ToolResult createVoiceFromMotif(final CompositionContext ctx,
                                                  final String voiceName,
                                                  final String motifName,
                                                  final String transform,
                                                  final Map<String, String> transformArgs) {
        try {
            final List<NoteEvent> source = ctx.getMotif(motifName);
            final Voice motifVoice = Voice.of(motifName, source);

            final List<NoteEvent> resultEvents;
            if (transform == null || transform.isBlank()) {
                resultEvents = source;
            } else {
                resultEvents = applyTransform(motifVoice, transform, transformArgs).events();
            }

            ctx.createVoice(voiceName, resultEvents);
            return ToolResult.success(
                "Created voice '" + voiceName + "' from motif '" + motifName +
                    "' (" + resultEvents.size() + " events)" +
                    (transform != null && !transform.isBlank() ? " with " + transform : "") + ".");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /**
     * Tool: voice.set_dynamics — set the dynamics (velocity) for a voice.
     */
    public static ToolResult setDynamics(final CompositionContext ctx,
                                         final String voiceName,
                                         final String dynamicsStr) {
        try {
            final Velocity velocity = switch (dynamicsStr.toLowerCase()) {
                case "ppp" -> Velocity.PPP;
                case "pp" -> Velocity.PP;
                case "p" -> Velocity.P;
                case "mp" -> Velocity.MP;
                case "mf" -> Velocity.MF;
                case "f" -> Velocity.F;
                case "ff" -> Velocity.FF;
                case "fff" -> Velocity.FFF;
                default -> throw new IllegalArgumentException(
                    "Unknown dynamics '" + dynamicsStr + "'. Valid: ppp, pp, p, mp, mf, f, ff, fff.");
            };
            ctx.setDynamics(voiceName, velocity);
            return ToolResult.success(
                "Set dynamics for '" + voiceName + "' to " + dynamicsStr.toLowerCase() +
                    " (velocity " + velocity.value() + ").");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /**
     * Tool: voice.set_articulation — set articulation for a voice, optionally scoped to a bar range.
     * If from_bar and to_bar are provided, only those bars are affected; otherwise the whole voice.
     */
    public static ToolResult setArticulation(final CompositionContext ctx,
                                             final String voiceName,
                                             final String artStr,
                                             final Integer fromBar,
                                             final Integer toBar) {
        try {
            final Articulation articulation = switch (artStr.toLowerCase()) {
                case "normal" -> Articulation.NORMAL;
                case "staccato" -> Articulation.STACCATO;
                case "accent" -> Articulation.ACCENT;
                case "tenuto" -> Articulation.TENUTO;
                case "marcato" -> Articulation.MARCATO;
                case "legato" -> Articulation.LEGATO;
                case "portato" -> Articulation.PORTATO;
                default -> throw new IllegalArgumentException(
                    "Unknown articulation '" + artStr +
                        "'. Valid: normal, staccato, accent, tenuto, marcato, legato, portato.");
            };
            if (fromBar != null && toBar != null) {
                ctx.setArticulationRange(voiceName, articulation, fromBar, toBar);
                return ToolResult.success("Set articulation for '" + voiceName +
                    "' bars " + fromBar + "-" + toBar + " to " + artStr.toLowerCase() + ".");
            } else {
                ctx.setArticulation(voiceName, articulation);
                return ToolResult.success("Set articulation for '" + voiceName +
                    "' to " + artStr.toLowerCase() + ".");
            }
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /**
     * Tool: voice.list — list all voices with event counts and total duration.
     */
    public static ToolResult listVoices(final CompositionContext ctx) {
        if (ctx.voiceNames().isEmpty()) {
            return ToolResult.success("No voices yet. Use voice.create to add one.");
        }
        final var sb = new StringBuilder("Voices:\n");
        for (String name : ctx.voiceNames()) {
            final List<NoteEvent> events = ctx.getVoice(name);
            final Fraction total = events.stream()
                .map(e -> e.duration().fraction())
                .reduce(Fraction.ZERO, Fraction::add);
            final Voice v = Voice.of(name, events);
            final int bars = VoiceOperations.measureCount(v, ctx.getTimeSignature());
            sb.append("  ").append(name).append(": ")
                .append(bars).append(" bars, ")
                .append(events.size()).append(" events, duration ").append(total).append("\n");
        }
        return ToolResult.success(sb.toString().stripTrailing());
    }

    // --- Internal helpers ---

    private static Voice applyTransform(final Voice voice,
                                        final String transform,
                                        final Map<String, String> args) {
        return switch (transform.toLowerCase()) {
            case "transpose" -> {
                final String intervalStr = requireArg(args, "interval", "transpose");
                final SpelledInterval interval = SpelledInterval.parse(intervalStr);
                final String direction = args != null ? args.getOrDefault("direction", "up") : "up";
                if ("down".equalsIgnoreCase(direction)) {
                    yield voice.transform(TransformTools.pitchTransformToMelodic(Transpose.down(interval)));
                } else {
                    yield voice.transpose(interval);
                }
            }
            case "invert" -> {
                final String axisStr = requireArg(args, "axis", "invert");
                final SpelledPitch axis = SpelledPitch.parse(axisStr);
                final Invert invert = new Invert(axis);
                yield voice.transform(TransformTools.pitchTransformToMelodic(invert));
            }
            case "retrograde" -> voice.transform(new Retrograde()::apply);
            case "augment" -> {
                final String factorStr = requireArg(args, "factor", "augment");
                final Fraction factor = TransformTools.parseFraction(factorStr);
                yield voice.transform(new Augment(factor)::apply);
            }
            default -> throw new IllegalArgumentException(
                "Unknown transform '" + transform +
                    "'. Valid transforms: transpose, invert, retrograde, augment.");
        };
    }

    private static String requireArg(final Map<String, String> args,
                                     final String key,
                                     final String toolName) {
        if (args == null || !args.containsKey(key)) {
            throw new IllegalArgumentException(
                "Transform '" + toolName + "' requires argument '" + key + "'.");
        }
        return args.get(key);
    }
}
