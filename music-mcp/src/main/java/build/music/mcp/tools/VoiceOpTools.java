package build.music.mcp.tools;

import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.mcp.CompositionContext;
import build.music.mcp.ToolResult;
import build.music.pitch.SpelledPitch;
import build.music.score.Voice;
import build.music.time.Fraction;
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
     * Tool: voice.trim — truncate a voice to N bars, writing back in place.
     */
    public static ToolResult trimVoice(final CompositionContext ctx,
                                       final String voiceName,
                                       final int bars) {
        try {
            final Voice voice = Voice.of(voiceName, ctx.getVoice(voiceName));
            final Voice trimmed = VoiceOperations.sliceMeasures(voice, voiceName, 1, bars + 1, ctx.getTimeSignature());
            ctx.createVoice(voiceName, trimmed.events());
            return ToolResult.success("Trimmed '" + voiceName + "' to " + bars + " bar(s) (" + trimmed.events().size() + " events).");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /**
     * Tool: voice.set_bar — replace a single bar's content with new notes.
     */
    public static ToolResult setBar(final CompositionContext ctx,
                                    final String voiceName,
                                    final int barNum,
                                    final String notes) {
        try {
            final BarRegion split = splitAroundBars(ctx.getVoice(voiceName), barNum, barNum, ctx.getTimeSignature());
            final List<NoteEvent> newNotes = CreateNoteTools.parseNoteSequence(notes);
            final List<NoteEvent> combined = new ArrayList<>();
            combined.addAll(split.before());
            combined.addAll(newNotes);
            combined.addAll(split.after());
            ctx.createVoice(voiceName, combined);
            return ToolResult.success("Replaced bar " + barNum + " in '" + voiceName + "' with " + newNotes.size() + " event(s).");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /**
     * Tool: voice.replace_range — replace a bar range with new notes.
     */
    public static ToolResult replaceRange(final CompositionContext ctx,
                                          final String voiceName,
                                          final int fromBar,
                                          final int toBar,
                                          final String notes) {
        try {
            final BarRegion split = splitAroundBars(ctx.getVoice(voiceName), fromBar, toBar, ctx.getTimeSignature());
            final List<NoteEvent> newNotes = CreateNoteTools.parseNoteSequence(notes);
            final List<NoteEvent> combined = new ArrayList<>();
            combined.addAll(split.before());
            combined.addAll(newNotes);
            combined.addAll(split.after());
            ctx.createVoice(voiceName, combined);
            return ToolResult.success("Replaced bars " + fromBar + "-" + toBar + " in '" + voiceName + "' with " + newNotes.size() + " event(s).");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /**
     * Tool: voice.replace_note — surgically replace the first matching note in a bar.
     * Matches by pitch; old must be a single note token (e.g. "C4/q").
     */
    public static ToolResult replaceNote(final CompositionContext ctx,
                                         final String voiceName,
                                         final int barNum,
                                         final String oldNote,
                                         final String newNote) {
        try {
            final List<NoteEvent> parsedOld = CreateNoteTools.parseNoteSequence(oldNote);
            if (parsedOld.isEmpty() || !(parsedOld.get(0) instanceof Note oldN)) {
                return ToolResult.error("old must be a single note (not a rest or chord).");
            }
            final SpelledPitch target = oldN.pitch().spelled();

            final List<NoteEvent> parsedNew = CreateNoteTools.parseNoteSequence(newNote);
            if (parsedNew.isEmpty()) {
                return ToolResult.error("new note sequence is empty.");
            }

            final TimeSignature ts = ctx.getTimeSignature();
            final Fraction measureDur = ts.measureDuration();
            final Fraction barStart = measureDur.multiply(barNum - 1);
            final Fraction barEnd = barStart.add(measureDur);

            final List<NoteEvent> events = new ArrayList<>(ctx.getVoice(voiceName));
            Fraction pos = Fraction.ZERO;
            boolean replaced = false;
            for (int i = 0; i < events.size(); i++) {
                final NoteEvent event = events.get(i);
                if (pos.compareTo(barEnd) >= 0) {
                    break;
                }
                if (pos.compareTo(barStart) >= 0 && event instanceof Note n && n.pitch().spelled().equals(target)) {
                    events.remove(i);
                    events.addAll(i, parsedNew);
                    replaced = true;
                    break;
                }
                pos = pos.add(event.duration().fraction());
            }

            if (!replaced) {
                return ToolResult.error("Note '" + oldNote + "' not found in bar " + barNum + " of voice '" + voiceName + "'.");
            }
            ctx.createVoice(voiceName, events);
            return ToolResult.success("Replaced '" + oldNote + "' with '" + newNote + "' in bar " + barNum + " of '" + voiceName + "'.");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /**
     * Tool: voice.measure_count — return the number of complete bars in a voice.
     */
    public static ToolResult measureCount(final CompositionContext ctx, final String voiceName) {
        try {
            final Voice voice = Voice.of(voiceName, ctx.getVoice(voiceName));
            final int bars = VoiceOperations.measureCount(voice, ctx.getTimeSignature());
            return ToolResult.success("Voice '" + voiceName + "' has " + bars + " complete bar(s).");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    // --- Internal helpers ---

    private record BarRegion(List<NoteEvent> before, List<NoteEvent> after) {
    }

    private static BarRegion splitAroundBars(final List<NoteEvent> events,
                                             final int fromBar,
                                             final int toBar,
                                             final TimeSignature ts) {
        final Fraction measureDur = ts.measureDuration();
        final Fraction startPos = measureDur.multiply(fromBar - 1);
        final Fraction endPos = measureDur.multiply(toBar);
        final List<NoteEvent> before = new ArrayList<>();
        final List<NoteEvent> after = new ArrayList<>();
        Fraction pos = Fraction.ZERO;
        for (final NoteEvent event : events) {
            if (pos.compareTo(startPos) < 0) {
                before.add(event);
            } else if (pos.compareTo(endPos) >= 0) {
                after.add(event);
            }
            pos = pos.add(event.duration().fraction());
        }
        return new BarRegion(before, after);
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
