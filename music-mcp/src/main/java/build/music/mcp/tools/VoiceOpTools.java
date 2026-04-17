package build.music.mcp.tools;

import build.music.mcp.CompositionContext;
import build.music.mcp.ToolResult;
import build.music.score.Voice;
import build.music.time.TimeSignature;
import build.music.voice.VoiceOperations;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP tools for advanced voice operations: concat, repeat, slice, pad.
 */
public final class VoiceOpTools {

    private VoiceOpTools() {
    }

    /**
     * Tool: voice.concat — concatenate multiple voices sequentially.
     *
     * @param voiceNames comma or space-separated list of voice names
     * @param targetName name for the result voice
     */
    public static ToolResult concat(final CompositionContext ctx,
                                    final String voiceNames,
                                    final String targetName) {
        try {
            final String[] names = voiceNames.trim().split("[,\\s]+");
            final List<Voice> voices = new ArrayList<>();
            for (String name : names) {
                final String trimmed = name.trim();
                if (!trimmed.isEmpty()) {
                    voices.add(Voice.of(trimmed, ctx.getVoice(trimmed)));
                }
            }
            if (voices.isEmpty()) {
                return ToolResult.error("No voice names provided.");
            }
            final String target = targetName != null ? targetName : String.join("_", names);
            final Voice result = VoiceOperations.concat(target, voices);
            ctx.createVoice(target, result.events());
            return ToolResult.success("Concatenated " + voices.size() + " voices → '" + target +
                "' (" + result.events().size() + " events).");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /**
     * Tool: voice.repeat — repeat a voice N times.
     */
    public static ToolResult repeat(final CompositionContext ctx,
                                    final String voiceName,
                                    final int times,
                                    final String targetName) {
        try {
            final Voice voice = Voice.of(voiceName, ctx.getVoice(voiceName));
            final String target = targetName != null ? targetName : voiceName + "_x" + times;
            final Voice repeated = VoiceOperations.repeat(voice, target, times);
            ctx.createVoice(target, repeated.events());
            return ToolResult.success("Repeated '" + voiceName + "' " + times + " times → '" +
                target + "' (" + repeated.events().size() + " events).");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /**
     * Tool: voice.slice — extract a range of measures from a voice.
     */
    public static ToolResult sliceMeasures(final CompositionContext ctx,
                                           final String voiceName,
                                           final int startMeasure,
                                           final int endMeasure,
                                           final String targetName) {
        try {
            final Voice voice = Voice.of(voiceName, ctx.getVoice(voiceName));
            final TimeSignature ts = ctx.getTimeSignature();
            final String target = targetName != null ? targetName : voiceName + "_m" + startMeasure;
            final Voice sliced = VoiceOperations.sliceMeasures(voice, target, startMeasure, endMeasure, ts);
            ctx.createVoice(target, sliced.events());
            return ToolResult.success("Sliced '" + voiceName + "' measures " + startMeasure +
                "-" + (endMeasure - 1) + " → '" + target + "' (" + sliced.events().size() + " events).");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /**
     * Tool: voice.delete — remove a voice from the composition context.
     */
    public static ToolResult deleteVoice(final CompositionContext ctx, final String voiceName) {
        try {
            ctx.deleteVoice(voiceName);
            return ToolResult.success("Deleted voice '" + voiceName + "'.");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /**
     * Tool: voice.pad_to_measure — add leading rests so voice starts at specified measure.
     */
    public static ToolResult padToMeasure(final CompositionContext ctx,
                                          final String voiceName,
                                          final int startMeasure,
                                          final String targetName) {
        try {
            final Voice voice = Voice.of(voiceName, ctx.getVoice(voiceName));
            final TimeSignature ts = ctx.getTimeSignature();
            final Voice padded = VoiceOperations.padToMeasure(voice, startMeasure, ts);
            final String target = targetName != null ? targetName : voiceName;
            ctx.createVoice(target, padded.events());
            final int added = padded.events().size() - voice.events().size();
            return ToolResult.success("Padded '" + voiceName + "' with " + added +
                " rest event(s) to start at measure " + startMeasure + ".");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }
}
