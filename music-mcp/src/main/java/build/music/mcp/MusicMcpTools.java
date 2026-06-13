package build.music.mcp;

import build.music.mcp.tools.DrumPresets;
import build.music.mcp.tools.ExportTools;
import build.music.mcp.tools.FormTools;
import build.music.mcp.tools.HarmonyTools;
import build.music.mcp.tools.InstrumentTools;
import build.music.mcp.tools.QueryTools;
import build.music.mcp.tools.RulesTools;
import build.music.mcp.tools.SaveLoadTools;
import build.music.mcp.tools.ScoreTools;
import build.music.mcp.tools.TransformTools;
import build.music.mcp.tools.VoiceOpTools;
import build.music.mcp.tools.VoiceTools;
import build.serve.mcp.McpServer;
import build.serve.mcp.McpTool;
import build.serve.mcp.McpToolAnnotations;
import build.serve.mcp.ToolParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers all music.build MCP tools on an {@link McpServer.Builder}.
 *
 * <p>This is the stable public API for embedding the composition toolset in any MCP server.
 * Use {@link ExportOptions#diskAndBytes()} for local dev servers (console UI reads from disk),
 * {@link ExportOptions#bytesOnly()} for hosted deployments (no disk accumulation).
 */
public final class MusicMcpTools {

    private MusicMcpTools() {
    }

    private static final McpToolAnnotations READ_ONLY =
        McpToolAnnotations.builder().readOnlyHint(true).build();
    private static final McpToolAnnotations DESTRUCTIVE =
        McpToolAnnotations.builder().destructiveHint(true).build();
    private static final McpToolAnnotations IDEMPOTENT =
        McpToolAnnotations.builder().idempotentHint(true).build();
    private static final McpToolAnnotations OPEN_WORLD =
        McpToolAnnotations.builder().openWorldHint(true).build();

    private static final List<String> DYNAMICS =
        List.of("ppp", "pp", "p", "mp", "mf", "f", "ff", "fff");
    private static final List<String> CURVE =
        List.of("linear", "exponential");

    public static void registerAll(final McpServer.Builder builder,
                                   final CompositionContextProvider provider,
                                   final ExportOptions exportOptions) {
        // Voice tools
        builder.tool(voiceCreateTool(provider));
        builder.tool(voiceAppendTool(provider));
        builder.tool(voiceFromMotifTool(provider));
        builder.tool(voiceListTool(provider));
        builder.tool(voiceDeleteTool(provider));
        // Score tools
        builder.tool(scoreSetMetadataTool(provider));
        builder.tool(scoreAssignInstrumentTool(provider));
        builder.tool(scoreDescribeTool(provider));
        builder.tool(scoreClearTool(provider));
        // Transform tools
        builder.tool(transformTransposeTool(provider));
        builder.tool(transformInvertTool(provider));
        builder.tool(transformRetrogradeTool(provider));
        builder.tool(transformAugmentTool(provider));
        builder.tool(motifSaveTool(provider));
        // Query tools
        builder.tool(queryVoiceTool(provider));
        builder.tool(queryMotifTool(provider));
        // Export tools
        builder.tool(exportAllTool(provider, exportOptions));
        builder.tool(exportMidiTool(provider, exportOptions));
        builder.tool(exportLilypondTool(provider, exportOptions));
        builder.tool(exportMusicXmlTool(provider, exportOptions));
        // Harmony tools
        builder.tool(harmonySetKeyTool(provider));
        builder.tool(harmonyChordProgressionTool(provider));
        builder.tool(harmonySetBarsTool(provider));
        builder.tool(harmonyHarmonizeTool(provider));
        builder.tool(harmonySuggestHarmonyTool(provider));
        builder.tool(harmonyDetectKeyTool(provider));
        builder.tool(harmonyDiatonicTransposeTool(provider));
        builder.tool(harmonyWalkingBassTool(provider));
        builder.tool(harmonyCompTool(provider));
        // Per-voice settings
        builder.tool(voiceSetDynamicsTool(provider));
        builder.tool(voiceSetArticulationTool(provider));
        builder.tool(voiceAddExpressionCurveTool(provider));
        // Advanced voice operations
        builder.tool(voiceConcatTool(provider));
        builder.tool(voiceRepeatTool(provider));
        builder.tool(voiceSliceTool(provider));
        builder.tool(voicePadToMeasureTool(provider));
        // Write-back voice editing
        builder.tool(voiceTrimTool(provider));
        builder.tool(voiceSetBarTool(provider));
        builder.tool(voiceReplaceRangeTool(provider));
        builder.tool(voiceReplaceNoteTool(provider));
        builder.tool(voiceMeasureCountTool(provider));
        // Form tools
        builder.tool(formCreateSectionTool(provider));
        builder.tool(formRepeatSectionTool(provider));
        builder.tool(formSetEndingTool(provider));
        builder.tool(formBuildTool(provider));
        builder.tool(formDescribeTool(provider));
        // Rules tools
        builder.tool(rulesCheckTool(provider));
        builder.tool(rulesCheckRangeTool(provider));
        // Instrument tools
        builder.tool(instrumentInfoTool());
        // Drum presets
        builder.tool(drumsPresetTool(provider));
        // Swing + tempo
        builder.tool(scoreSetSwingTool(provider));
        builder.tool(scoreSetTempoChangeTool(provider));
        // Save / load
        builder.tool(scoreSaveTool(provider));
        builder.tool(scoreLoadTool(provider));
        builder.tool(scoreLoadMidiTool(provider));
        builder.tool(scoreLoadAbcTool(provider));
    }

    // --- Tool factory methods ---

    private static McpTool voiceCreateTool(final CompositionContextProvider provider) {
        final var name = ToolParam.string("name", "Voice name (e.g. 'melody', 'bass', 'counterpoint')")
            .minLength(1);
        final var notes = ToolParam.string("notes", "Note sequence, e.g. 'E4/q E4/q F4/q G4/q G4/q F4/q E4/q D4/q'")
            .minLength(1);
        return MusicToolDef.of(provider,
            "voice.create",
            "Create a named voice from a note sequence. " +
                "The voice is added to the composition and can be used for export or transformation. " +
                "Note format: 'pitch/duration' — e.g. 'C4/q E4/q G4/q C5/h'. " +
                "Duration codes: w=whole, h=half, q=quarter, e=eighth, s=sixteenth, dh, dq, de for dotted. " +
                "Rests: 'r/q' = quarter rest. " +
                "Chords: '<C4 E4 G4>/q' = C major triad as quarter chord. " +
                "Articulations: append ~stac (staccato), ~acc (accent), ~ten (tenuto), ~marc (marcato), ~leg (legato). " +
                "Example: 'C4/q~stac D4/e E4/e <F4 A4 C5>/h r/q'")
            .param(name)
            .param(notes)
            .handle((ctx, args) -> VoiceTools.createVoice(ctx, name.extract(args), notes.extract(args)));
    }

    private static McpTool voiceAppendTool(final CompositionContextProvider provider) {
        final var name = ToolParam.string("name", "Name of the existing voice to append to")
            .minLength(1);
        final var notes = ToolParam.string("notes", "Note sequence to append")
            .minLength(1);
        return MusicToolDef.of(provider,
            "voice.append",
            "Append additional notes to an existing voice. " +
                "Uses the same note format as voice.create: 'pitch/duration' tokens, chords '<C4 E4 G4>/q', and rests 'r/q'. " +
                "Example: build a melody bar-by-bar — create with voice.create, then voice.append each new bar.")
            .param(name)
            .param(notes)
            .handle((ctx, args) -> VoiceTools.appendToVoice(ctx, name.extract(args), notes.extract(args)));
    }

    private static McpTool voiceFromMotifTool(final CompositionContextProvider provider) {
        final var voiceName = ToolParam.string("voice_name", "Name for the new voice")
            .minLength(1);
        final var motifName = ToolParam.string("motif_name", "Name of the motif to copy")
            .minLength(1);
        final var transform = ToolParam.string("transform", "Optional: transpose, invert, retrograde, or augment")
            .values(List.of("transpose", "invert", "retrograde", "augment"))
            .optional();
        final var interval = ToolParam.string("interval", "For transpose: interval string, e.g. 'P5', 'm3', 'M2'")
            .minLength(1)
            .optional();
        final var direction = ToolParam.string("direction", "For transpose: direction")
            .values(List.of("up", "down"))
            .optional();
        final var axis = ToolParam.string("axis", "For invert: axis pitch, e.g. 'C4'")
            .minLength(1)
            .optional();
        final var factor = ToolParam.string("factor", "For augment: fraction string, e.g. '2/1', '1/2', '3/2'")
            .minLength(1)
            .optional();
        return MusicToolDef.of(provider,
            "voice.from_motif",
            "Advanced: Create a voice by applying a transform to a saved motif. " +
                "Example: take 'theme_a', transpose up P5, and use it as a new voice. " +
                "transform values: 'transpose' (requires interval arg, e.g. 'P5'), " +
                "'invert' (requires axis arg, e.g. 'C4'), 'retrograde', " +
                "'augment' (requires factor arg, e.g. '2/1'). " +
                "Omit transform to copy the motif unchanged.")
            .param(voiceName)
            .param(motifName)
            .param(transform)
            .param(interval)
            .param(direction)
            .param(axis)
            .param(factor)
            .handle((ctx, args) -> {
                final String transformVal = transform.extract(args);
                Map<String, String> transformArgs = null;
                if (transformVal != null) {
                    transformArgs = new HashMap<>();
                    final String intervalVal = interval.extract(args);
                    if (intervalVal != null) {
                        transformArgs.put("interval", intervalVal);
                    }
                    final String directionVal = direction.extract(args);
                    if (directionVal != null) {
                        transformArgs.put("direction", directionVal);
                    }
                    final String axisVal = axis.extract(args);
                    if (axisVal != null) {
                        transformArgs.put("axis", axisVal);
                    }
                    final String factorVal = factor.extract(args);
                    if (factorVal != null) {
                        transformArgs.put("factor", factorVal);
                    }
                }
                return VoiceTools.createVoiceFromMotif(ctx, voiceName.extract(args), motifName.extract(args),
                    transformVal, transformArgs);
            });
    }

    private static McpTool voiceListTool(final CompositionContextProvider provider) {
        return MusicToolDef.of(provider,
            "voice.list",
            "List all voices in the current composition with bar counts, event counts, and total duration.")
            .annotations(READ_ONLY)
            .handle((ctx, args) -> VoiceTools.listVoices(ctx));
    }

    private static McpTool voiceDeleteTool(final CompositionContextProvider provider) {
        final var name = ToolParam.string("name", "Name of the voice to delete")
            .minLength(1);
        return MusicToolDef.of(provider,
            "voice.delete",
            "Remove a voice from the composition. Use this to clean up scratch or scaffolding voices " +
                "that should not appear in the final score or form sections.")
            .param(name)
            .handle((ctx, args) -> VoiceOpTools.deleteVoice(ctx, name.extract(args)));
    }

    private static McpTool scoreSetMetadataTool(final CompositionContextProvider provider) {
        final var title = ToolParam.string("title", "Composition title")
            .minLength(1)
            .optional();
        final var tempo = ToolParam.integer("tempo", "Tempo in BPM (1–400)")
            .min(1).max(400)
            .optional();
        final var timeSignature = ToolParam.string("time_signature", "Time signature, e.g. '4/4', '3/4', '6/8'")
            .minLength(3)
            .optional();
        return MusicToolDef.of(provider,
            "score.set_metadata",
            "Set the title, tempo, and/or time signature of the composition. " +
                "All parameters are optional — only provided values are updated. " +
                "Time signature format: '4/4', '3/4', '6/8', etc.")
            .param(title)
            .param(tempo)
            .param(timeSignature)
            .annotations(IDEMPOTENT)
            .handle((ctx, args) -> ScoreTools.setMetadata(ctx, title.extract(args), tempo.extract(args), timeSignature.extract(args)));
    }

    private static McpTool scoreAssignInstrumentTool(final CompositionContextProvider provider) {
        final var voice = ToolParam.string("voice", "Name of the voice to assign")
            .minLength(1);
        final var instrument = ToolParam.string("instrument", "Instrument name (e.g. 'piano', 'strings', 'flute')")
            .minLength(1);
        return MusicToolDef.of(provider,
            "score.assign_instrument",
            "Assign a voice to a MIDI instrument for playback and export. " +
                "Keyboards: piano, bright_piano, honky_tonk, electric_piano (Rhodes), rhodes, harpsichord, organ, accordion. " +
                "Tuned perc: vibraphone, marimba. " +
                "Guitar/bass: guitar, electric_guitar, electric_bass, synth_bass. " +
                "Strings: violin, viola, cello, strings, synth_strings. " +
                "Choir: choir. " +
                "Brass/winds: trumpet, trombone, tuba, french_horn, brass, saxophone, clarinet, flute, oboe, bassoon. " +
                "Synth: synth_lead (sawtooth), synth_pad (warm pad). " +
                "Percussion: drums (forces channel 9, GM drum map). " +
                "Example: assign 'melody' to 'synth_lead', 'chords' to 'electric_piano', 'kick' to 'drums'.")
            .param(voice)
            .param(instrument)
            .annotations(IDEMPOTENT)
            .handle((ctx, args) -> ScoreTools.assignInstrument(ctx, voice.extract(args), instrument.extract(args)));
    }

    private static McpTool scoreDescribeTool(final CompositionContextProvider provider) {
        return MusicToolDef.of(provider,
            "score.describe",
            "Get a complete description of the current composition state: " +
                "title, tempo, time signature, all voices with note counts, motifs, and instrument assignments. " +
                "Call this to understand the current state before deciding what to do next.")
            .annotations(READ_ONLY)
            .handle((ctx, args) -> ScoreTools.describeScore(ctx));
    }

    private static McpTool scoreClearTool(final CompositionContextProvider provider) {
        return MusicToolDef.of(provider,
            "score.clear",
            "Clear the entire composition and start fresh. " +
                "Removes all voices, motifs, and instrument assignments. Resets title and tempo to defaults.")
            .annotations(DESTRUCTIVE)
            .handle((ctx, args) -> ScoreTools.clearScore(ctx));
    }

    private static McpTool transformTransposeTool(final CompositionContextProvider provider) {
        final var voice = ToolParam.string("voice", "Name of the voice to transpose")
            .minLength(1);
        final var interval = ToolParam.string("interval", "Interval string, e.g. 'P5', 'm3', 'M2'")
            .minLength(2);
        final var direction = ToolParam.string("direction", "Direction of transposition")
            .values(List.of("up", "down"))
            .optional("up");
        final var targetVoice = ToolParam.string("target_voice", "Optional: name for the resulting voice")
            .minLength(1)
            .optional();
        return MusicToolDef.of(provider,
            "transform.transpose",
            "Transpose all notes in a voice by a chromatic interval. " +
                "Interval format: quality+size — 'P5' (perfect fifth), 'm3' (minor third), " +
                "'M2' (major second), 'P8' (octave), 'A4' (augmented fourth/tritone). " +
                "Creates a new voice named '{voice}_transposed' unless target_voice is specified. " +
                "Example: transpose 'melody' up by M3 to create a parallel third harmony voice.")
            .param(voice)
            .param(interval)
            .param(direction)
            .param(targetVoice)
            .handle((ctx, args) -> TransformTools.transpose(ctx, voice.extract(args), interval.extract(args),
                direction.extract(args), targetVoice.extract(args)));
    }

    private static McpTool transformInvertTool(final CompositionContextProvider provider) {
        final var voice = ToolParam.string("voice", "Name of the voice to invert")
            .minLength(1);
        final var axis = ToolParam.string("axis", "Axis pitch to invert around, e.g. 'C4', 'E4'")
            .minLength(2);
        final var targetVoice = ToolParam.string("target_voice", "Optional: name for the resulting voice")
            .minLength(1)
            .optional();
        return MusicToolDef.of(provider,
            "transform.invert",
            "Advanced: Invert a voice around an axis pitch (mirror melodic intervals). " +
                "Each note is reflected to the same interval below the axis that it was above it. " +
                "Creates a new voice named '{voice}_inverted' unless target_voice is specified.")
            .param(voice)
            .param(axis)
            .param(targetVoice)
            .handle((ctx, args) -> TransformTools.invert(ctx, voice.extract(args), axis.extract(args), targetVoice.extract(args)));
    }

    private static McpTool transformRetrogradeTool(final CompositionContextProvider provider) {
        final var voice = ToolParam.string("voice", "Name of the voice to reverse")
            .minLength(1);
        final var targetVoice = ToolParam.string("target_voice", "Optional: name for the resulting voice")
            .minLength(1)
            .optional();
        return MusicToolDef.of(provider,
            "transform.retrograde",
            "Advanced: Reverse the temporal order of all events in a voice (play it backwards). " +
                "Creates a new voice named '{voice}_retrograde' unless target_voice is specified.")
            .param(voice)
            .param(targetVoice)
            .handle((ctx, args) -> TransformTools.retrograde(ctx, voice.extract(args), targetVoice.extract(args)));
    }

    private static McpTool transformAugmentTool(final CompositionContextProvider provider) {
        final var voice = ToolParam.string("voice", "Name of the voice to scale")
            .minLength(1);
        final var factor = ToolParam.string("factor", "Duration scale factor as fraction, e.g. '2/1', '1/2', '3/2'")
            .minLength(3);
        final var targetVoice = ToolParam.string("target_voice", "Optional: name for the resulting voice")
            .minLength(1)
            .optional();
        return MusicToolDef.of(provider,
            "transform.augment",
            "Advanced: Scale all durations in a voice by a rational factor. " +
                "Factor '2/1' = augmentation (double duration), '1/2' = diminution (halve), '3/2' = dotted feel. " +
                "Creates a new voice named '{voice}_augmented' unless target_voice is specified.")
            .param(voice)
            .param(factor)
            .param(targetVoice)
            .handle((ctx, args) -> TransformTools.augment(ctx, voice.extract(args), factor.extract(args), targetVoice.extract(args)));
    }

    private static McpTool motifSaveTool(final CompositionContextProvider provider) {
        final var voice = ToolParam.string("voice", "Name of the voice to extract from")
            .minLength(1);
        final var motifName = ToolParam.string("motif_name", "Name to give the saved motif")
            .minLength(1);
        final var startNote = ToolParam.integer("start_note", "0-based start index (inclusive), default 0")
            .min(0)
            .optional();
        final var endNote = ToolParam.integer("end_note", "0-based end index (exclusive), default = end of voice")
            .min(1)
            .optional();
        return MusicToolDef.of(provider,
            "motif.save",
            "Save a slice of a voice as a named motif for later reuse with voice.from_motif. " +
                "start_note and end_note are 0-based indices (end_note is exclusive). " +
                "Omit both to save the entire voice. " +
                "Example: save the first 4 notes of 'melody' as 'opening_theme', then use voice.from_motif to create variations.")
            .param(voice)
            .param(motifName)
            .param(startNote)
            .param(endNote)
            .handle((ctx, args) -> TransformTools.saveMotif(ctx, voice.extract(args), motifName.extract(args),
                startNote.extract(args), endNote.extract(args)));
    }

    private static McpTool queryVoiceTool(final CompositionContextProvider provider) {
        final var voice = ToolParam.string("voice", "Name of the voice to display")
            .minLength(1);
        return MusicToolDef.of(provider,
            "query.voice",
            "Display the notes of a specific voice in a human-readable format with bar lines. " +
                "Use this to inspect a voice before transforming or exporting.")
            .param(voice)
            .annotations(READ_ONLY)
            .handle((ctx, args) -> QueryTools.queryVoice(ctx, voice.extract(args)));
    }

    private static McpTool queryMotifTool(final CompositionContextProvider provider) {
        final var motif = ToolParam.string("motif", "Name of the motif to display")
            .minLength(1);
        return MusicToolDef.of(provider,
            "query.motif",
            "Display the notes of a saved motif.")
            .param(motif)
            .annotations(READ_ONLY)
            .handle((ctx, args) -> QueryTools.queryMotif(ctx, motif.extract(args)));
    }

    private static McpTool exportAllTool(final CompositionContextProvider provider,
                                         final ExportOptions exportOptions) {
        final var folder = ToolParam.string("folder", "Output folder name (optional, defaults to composition title)")
            .minLength(1)
            .optional();
        return MusicToolDef.of(provider,
            "export.all",
            "Export the current composition to a folder containing both a MIDI file (.mid) and " +
                "LilyPond source (.ly). If LilyPond is installed, also engraves a PDF. " +
                "The folder is created in the current working directory. " +
                "Folder name defaults to the composition title if not specified.")
            .param(folder)
            .annotations(OPEN_WORLD)
            .handle((ctx, args) -> ExportTools.exportAll(ctx, folder.extract(args), exportOptions));
    }

    private static McpTool exportMidiTool(final CompositionContextProvider provider,
                                          final ExportOptions exportOptions) {
        final var filename = ToolParam.string("filename", "Output filename, e.g. 'my_piece.mid' (optional)")
            .minLength(1)
            .optional();
        return MusicToolDef.of(provider,
            "export.midi",
            "Render the current composition to a Standard MIDI File (.mid). " +
                "Filename is optional — defaults to '{title}.mid'. " +
                "Returns the path to the written file.")
            .param(filename)
            .annotations(OPEN_WORLD)
            .handle((ctx, args) -> ExportTools.exportMidi(ctx, filename.extract(args), exportOptions));
    }

    private static McpTool exportLilypondTool(final CompositionContextProvider provider,
                                              final ExportOptions exportOptions) {
        final var filename = ToolParam.string("filename", "Output base filename without extension (optional)")
            .minLength(1)
            .optional();
        return MusicToolDef.of(provider,
            "export.lilypond",
            "Render the current composition to LilyPond source (.ly file). " +
                "If LilyPond is installed on PATH, also engraves to PDF. " +
                "Returns the LilyPond source text in the data field.")
            .param(filename)
            .annotations(OPEN_WORLD)
            .handle((ctx, args) -> ExportTools.exportLilypond(ctx, filename.extract(args), exportOptions));
    }

    private static McpTool exportMusicXmlTool(final CompositionContextProvider provider,
                                              final ExportOptions exportOptions) {
        final var filename = ToolParam.string("filename", "Output base filename without extension (optional)")
            .minLength(1)
            .optional();
        return MusicToolDef.of(provider,
            "export.musicxml",
            "Export the current composition to MusicXML 4.0 (.musicxml). " +
                "MusicXML is the standard interchange format for notation software — " +
                "opens in MuseScore, Dorico, Sibelius, Finale, and most DAWs. " +
                "Filename is optional — defaults to '{title}.musicxml'.")
            .param(filename)
            .annotations(OPEN_WORLD)
            .handle((ctx, args) -> ExportTools.exportMusicXml(ctx, filename.extract(args), exportOptions));
    }

    private static McpTool harmonySetKeyTool(final CompositionContextProvider provider) {
        final var key = ToolParam.string("key", "Key description, e.g. 'C major', 'G major', 'D minor'")
            .minLength(1);
        return MusicToolDef.of(provider, "harmony.set_key",
            "Set the musical key for the composition. " +
                "Format: 'Tonic Mode' — e.g. 'C major', 'A minor', 'F# minor', 'Bb major'. " +
                "The key is used for harmonization, diatonic transposition, and chord progressions.")
            .param(key)
            .annotations(IDEMPOTENT)
            .handle((ctx, args) -> HarmonyTools.setKey(ctx, key.extract(args)));
    }

    private static McpTool harmonyChordProgressionTool(final CompositionContextProvider provider) {
        final var progression = ToolParam.string("progression", "Roman numeral progression, e.g. 'I IV V I', 'ii V I'")
            .minLength(1);
        return MusicToolDef.of(provider, "harmony.chord_progression",
            "Set a chord progression using Roman numerals. " +
                "Format: space or dash-separated Roman numerals, e.g. 'I IV V I', 'ii V I', 'I V vi IV'. " +
                "Upper case = major chord, lower case = minor, 'o' suffix = diminished, '7' = seventh. " +
                "Requires harmony.set_key to be called first for full resolution.")
            .param(progression)
            .annotations(IDEMPOTENT)
            .handle((ctx, args) -> HarmonyTools.setChordProgression(ctx, progression.extract(args)));
    }

    private static McpTool harmonySetBarsTool(final CompositionContextProvider provider) {
        final var bars = ToolParam.string("bars", "Bar chord string, e.g. '1:Cm7 2:F7 3:Bb7 4:Eb'")
            .minLength(1);
        return MusicToolDef.of(provider, "harmony.set_bars",
            "Declare chord changes per measure as concrete chord symbols (not Roman numerals). " +
                "Format: '1:Cm7 2:F7 3:BbM7 4:Eb' — bar number, colon, chord symbol. " +
                "Chord qualities: (bare root)=major, m=minor, 7=dom7, maj7, m7, m7b5, dim, dim7, aug, sus2, sus4. " +
                "Examples: 'Gm7', 'C7', 'Fm7b5', 'Bb7', 'EbM7'. " +
                "Use for jazz chord sheets, ii-V-I sequences, blues changes. " +
                "These chords are used by harmony.walking_bass and override harmony.chord_progression for those tools. " +
                "Example: harmony.set_bars '1:Gm7 2:C7 3:Fm7 4:Bb7 5:Ebmaj7 6:Ebmaj7 7:Cm7 8:D7'")
            .param(bars)
            .annotations(IDEMPOTENT)
            .handle((ctx, args) -> HarmonyTools.setBarChords(ctx, bars.extract(args)));
    }

    private static McpTool harmonyHarmonizeTool(final CompositionContextProvider provider) {
        final var voice = ToolParam.string("voice", "Melody voice to harmonize")
            .minLength(1);
        final var targetVoice = ToolParam.string("target_voice", "Name for the harmony voice (optional)")
            .minLength(1)
            .optional();
        final var octave = ToolParam.integer("octave", "Octave for chord roots (0–8), e.g. 3 for bass")
            .min(0).max(8)
            .optional(3);
        return MusicToolDef.of(provider, "harmony.harmonize",
            "Add chord root accompaniment to a melody voice based on the current chord progression. " +
                "Requires harmony.set_key and harmony.chord_progression to be called first. " +
                "Creates a new voice with chord roots (one per chord in the progression). " +
                "Example: key=C major, progression=I IV V I, melody='bass' → creates bass line of C F G C roots.")
            .param(voice)
            .param(targetVoice)
            .param(octave)
            .handle((ctx, args) -> HarmonyTools.harmonize(ctx, voice.extract(args), targetVoice.extract(args), octave.extract(args)));
    }

    private static McpTool harmonySuggestHarmonyTool(final CompositionContextProvider provider) {
        final var voice = ToolParam.string("voice", "Melody voice to analyze")
            .minLength(1);
        return MusicToolDef.of(provider, "harmony.suggest_harmony",
            "Analyze a melody voice and suggest a chord progression based on its pitch content. " +
                "Finds the best-fitting diatonic chord for each measure, saves as current progression. " +
                "Example result: 'I IV I V' for a simple melody in C major. " +
                "Requires harmony.set_key to be called first.")
            .param(voice)
            .handle((ctx, args) -> HarmonyTools.suggestHarmony(ctx, voice.extract(args)));
    }

    private static McpTool harmonyDetectKeyTool(final CompositionContextProvider provider) {
        final var voice = ToolParam.string("voice", "Voice to analyze for key detection")
            .minLength(1);
        return MusicToolDef.of(provider, "harmony.detect_key",
            "Advanced: Detect the likely key of a voice using pitch class distribution analysis. " +
                "Sets the detected key as the current key. Use harmony.set_key when you know the key.")
            .param(voice)
            .handle((ctx, args) -> HarmonyTools.detectKey(ctx, voice.extract(args)));
    }

    private static McpTool harmonyDiatonicTransposeTool(final CompositionContextProvider provider) {
        final var voice = ToolParam.string("voice", "Voice to transpose")
            .minLength(1);
        final var steps = ToolParam.integer("steps", "Number of scale steps (positive=up, negative=down)");
        final var targetVoice = ToolParam.string("target_voice", "Name for result voice (optional)")
            .minLength(1)
            .optional();
        return MusicToolDef.of(provider, "harmony.diatonic_transpose",
            "Transpose a voice by scale steps within the current key, preserving key membership. " +
                "Example: steps=2 in C major moves C→E, D→F (a diatonic third, not always a major third). " +
                "Steps=-1 moves down one scale step. " +
                "Useful for harmonizing in thirds or sixths. Requires harmony.set_key first.")
            .param(voice)
            .param(steps)
            .param(targetVoice)
            .handle((ctx, args) -> HarmonyTools.diatonicTranspose(ctx, voice.extract(args), steps.extract(args), targetVoice.extract(args)));
    }

    private static McpTool harmonyWalkingBassTool(final CompositionContextProvider provider) {
        final var targetVoice = ToolParam.string("target_voice", "Name for the bass voice (default: 'bass')")
            .minLength(1)
            .optional();
        final var octave = ToolParam.integer("octave", "Bass octave (0–8, default: 2 — C2 to B2 range)")
            .min(0).max(8)
            .optional(2);
        final var bars = ToolParam.integer("bars", "Number of bars (default: use all defined chords)")
            .min(1)
            .optional();
        final var velocity = ToolParam.string("velocity", "Velocity/dynamics: ppp, pp, p, mp, mf (default), f, ff, fff")
            .values(DYNAMICS)
            .optional();
        final var approach = ToolParam.string("approach", "Approach note style: chromatic (default), diatonic, none")
            .values(List.of("chromatic", "diatonic", "none"))
            .optional();
        return MusicToolDef.of(provider, "harmony.walking_bass",
            "Generate a walking bass line over chord changes, adapted to the current time signature. " +
                "4/4: root-fifth-third-approach (4 quarter notes). 3/4: root-fifth-fifth. " +
                "6/8: root-fifth (2 dotted-quarters). 2/4: root-approach. " +
                "Requires harmony.set_bars (preferred) or harmony.set_key + harmony.chord_progression. " +
                "Set score.set_swing to add shuffle feel after generating. " +
                "Example usage: set_bars → walking_bass target_voice='bass' octave=2 → assign to electric_bass.")
            .param(targetVoice)
            .param(octave)
            .param(bars)
            .param(velocity)
            .param(approach)
            .handle((ctx, args) -> HarmonyTools.walkingBass(ctx, targetVoice.extract(args), octave.extract(args),
                bars.extract(args), velocity.extract(args), approach.extract(args)));
    }

    private static McpTool harmonyCompTool(final CompositionContextProvider provider) {
        final var targetVoice = ToolParam.string("target_voice", "Name for the comping voice (default: 'comp')")
            .minLength(1)
            .optional();
        final var octave = ToolParam.integer("octave", "Voicing octave (0–8, default: 3 — mid-range)")
            .min(0).max(8)
            .optional(3);
        final var style = ToolParam.string("style", "Comping style: quarter_stabs, on_beat, eighth_pump, shell_voicings, charleston (default: quarter_stabs)")
            .values(List.of("quarter_stabs", "on_beat", "eighth_pump", "shell_voicings", "charleston"))
            .optional();
        final var bars = ToolParam.integer("bars", "Number of bars (default: use all defined chords)")
            .min(1)
            .optional();
        final var velocity = ToolParam.string("velocity", "Velocity/dynamics: ppp, pp, p, mp, mf (default), f, ff, fff")
            .values(DYNAMICS)
            .optional();
        return MusicToolDef.of(provider, "harmony.comp",
            "Generate a comping (chord accompaniment) voice over bar-level chord changes. " +
                "Styles: " +
                "'quarter_stabs' — block chord on beats 2 and 4 (default, jazz comping); " +
                "'on_beat' — block chord on every beat; " +
                "'eighth_pump' — block chord on every eighth note (funk/rock); " +
                "'shell_voicings' — root + 7th on beats 2 and 4 (sparse jazz voicing); " +
                "'charleston' — dotted quarter + eighth + quarter + rest (classic jazz Charleston rhythm). " +
                "Requires harmony.set_bars (preferred) or harmony.set_key + harmony.chord_progression. " +
                "Example: set_bars → comp style='charleston' octave=3 → assign to piano.")
            .param(targetVoice)
            .param(octave)
            .param(style)
            .param(bars)
            .param(velocity)
            .handle((ctx, args) -> HarmonyTools.comp(ctx, targetVoice.extract(args), octave.extract(args),
                style.extract(args), bars.extract(args), velocity.extract(args)));
    }

    private static McpTool voiceSetDynamicsTool(final CompositionContextProvider provider) {
        final var voice = ToolParam.string("voice", "Name of the voice to set dynamics for")
            .minLength(1);
        final var dynamics = ToolParam.string("dynamics", "Dynamics level: ppp, pp, p, mp, mf, f, ff, fff")
            .values(DYNAMICS);
        return MusicToolDef.of(provider,
            "voice.set_dynamics",
            "Set the dynamics (volume) for a voice. " +
                "Applies to all notes in the voice when rendering to MIDI. " +
                "Example: set 'bass' to forte for a driving bass line, 'pad' to piano for background chords. " +
                "Dynamics values: ppp, pp, p, mp, mf (default), f, ff, fff.")
            .param(voice)
            .param(dynamics)
            .handle((ctx, args) -> VoiceTools.setDynamics(ctx, voice.extract(args), dynamics.extract(args)));
    }

    private static McpTool voiceSetArticulationTool(final CompositionContextProvider provider) {
        final var voice = ToolParam.string("voice", "Name of the voice to set articulation for")
            .minLength(1);
        final var articulation = ToolParam.string("articulation", "Articulation: normal, staccato, accent, tenuto, marcato, legato, portato")
            .values(List.of("normal", "staccato", "accent", "tenuto", "marcato", "legato", "portato"));
        final var fromBar = ToolParam.integer("from_bar", "First bar of the range (1-based, optional)")
            .min(1)
            .optional();
        final var toBar = ToolParam.integer("to_bar", "Last bar of the range (inclusive, optional)")
            .min(1)
            .optional();
        return MusicToolDef.of(provider,
            "voice.set_articulation",
            "Set articulation for a voice, optionally scoped to a bar range. " +
                "Without from_bar/to_bar, applies to the whole voice. " +
                "With from_bar/to_bar, only those bars are affected — e.g. make just the final phrase tenuto. " +
                "Affects both MIDI playback (note duration) and LilyPond notation marks. " +
                "Articulations: normal (default), staccato, accent, tenuto, marcato, legato, portato.")
            .param(voice)
            .param(articulation)
            .param(fromBar)
            .param(toBar)
            .handle((ctx, args) -> VoiceTools.setArticulation(ctx, voice.extract(args), articulation.extract(args),
                fromBar.extract(args), toBar.extract(args)));
    }

    private static McpTool voiceAddExpressionCurveTool(final CompositionContextProvider provider) {
        final var voice = ToolParam.string("voice", "Name of the voice")
            .minLength(1);
        final var startBar = ToolParam.integer("start_bar", "First bar of the curve (1-based)")
            .min(1);
        final var endBar = ToolParam.integer("end_bar", "Last bar of the curve (inclusive)")
            .min(1);
        final var fromValue = ToolParam.integer("from_value", "CC 11 value at start_bar (0–127)")
            .min(0).max(127);
        final var toValue = ToolParam.integer("to_value", "CC 11 value at end_bar (0–127)")
            .min(0).max(127);
        final var curve = ToolParam.string("curve", "Shape: 'linear' or 'exponential'")
            .values(CURVE);
        return MusicToolDef.of(provider,
            "voice.add_expression_curve",
            "Add a gradual CC 11 (expression) curve to a voice. " +
                "The renderer interpolates CC 11 events at each bar boundary between startBar and endBar. " +
                "Values are MIDI CC range 0–127 (0=silent, 127=full). " +
                "curve: 'linear' for equal steps, 'exponential' for a more natural swell. " +
                "Example: crescendo 'melody' from pp (30) to ff (110) over bars 9–16.")
            .param(voice)
            .param(startBar)
            .param(endBar)
            .param(fromValue)
            .param(toValue)
            .param(curve)
            .handle((ctx, args) -> VoiceTools.addExpressionCurve(ctx,
                voice.extract(args), startBar.extract(args), endBar.extract(args),
                fromValue.extract(args), toValue.extract(args), curve.extract(args)));
    }

    private static McpTool voiceConcatTool(final CompositionContextProvider provider) {
        final var voiceNames = ToolParam.string("voice_names", "Comma-separated voice names, e.g. 'verse1, chorus, verse2'")
            .minLength(1);
        final var targetVoice = ToolParam.string("target_voice", "Name for the resulting concatenated voice")
            .minLength(1);
        return MusicToolDef.of(provider, "voice.concat",
            "Concatenate multiple voices into a single voice sequentially. " +
                "Voice B starts exactly where voice A ends — essential for ABA form, verse-chorus, etc. " +
                "voice_names: comma-separated list of voice names in order. " +
                "Example: concat 'verse1, chorus, verse2, chorus' into 'full_melody'.")
            .param(voiceNames)
            .param(targetVoice)
            .handle((ctx, args) -> VoiceOpTools.concat(ctx, voiceNames.extract(args), targetVoice.extract(args)));
    }

    private static McpTool voiceRepeatTool(final CompositionContextProvider provider) {
        final var voice = ToolParam.string("voice", "Voice to repeat")
            .minLength(1);
        final var times = ToolParam.integer("times", "Number of repetitions (>= 1)")
            .min(1);
        final var targetVoice = ToolParam.string("target_voice", "Name for the repeated voice (optional)")
            .minLength(1)
            .optional();
        return MusicToolDef.of(provider, "voice.repeat",
            "Repeat a voice N times, creating a new concatenated voice. " +
                "Useful for ostinatos, repeated motifs, and riff-based composition. " +
                "Example: create a 1-bar bass riff, then voice.repeat it 8 times for an 8-bar groove.")
            .param(voice)
            .param(times)
            .param(targetVoice)
            .handle((ctx, args) -> VoiceOpTools.repeat(ctx, voice.extract(args), times.extract(args), targetVoice.extract(args)));
    }

    private static McpTool voiceSliceTool(final CompositionContextProvider provider) {
        final var voice = ToolParam.string("voice", "Voice to slice")
            .minLength(1);
        final var startMeasure = ToolParam.integer("start_measure", "First measure to include (1-based)")
            .min(1);
        final var endMeasure = ToolParam.integer("end_measure", "Measure to stop before (exclusive, >= 2)")
            .min(2);
        final var targetVoice = ToolParam.string("target_voice", "Name for the sliced voice (optional)")
            .minLength(1)
            .optional();
        return MusicToolDef.of(provider, "voice.slice",
            "Advanced: Extract a range of measures from a voice. " +
                "start_measure and end_measure are 1-based; end_measure is exclusive. " +
                "E.g., start=1, end=3 extracts measures 1 and 2. " +
                "For most form-building use cases, prefer form.create_section and form.repeat_section.")
            .param(voice)
            .param(startMeasure)
            .param(endMeasure)
            .param(targetVoice)
            .handle((ctx, args) -> VoiceOpTools.sliceMeasures(ctx, voice.extract(args),
                startMeasure.extract(args), endMeasure.extract(args), targetVoice.extract(args)));
    }

    private static McpTool voicePadToMeasureTool(final CompositionContextProvider provider) {
        final var voice = ToolParam.string("voice", "Voice to pad")
            .minLength(1);
        final var startMeasure = ToolParam.integer("start_measure", "Measure number where the voice should begin (1-based)")
            .min(1);
        final var targetVoice = ToolParam.string("target_voice", "Name for the padded voice (optional, replaces original if omitted)")
            .minLength(1)
            .optional();
        return MusicToolDef.of(provider, "voice.pad_to_measure",
            "Advanced: Add leading rests to a voice so it starts at a given measure. " +
                "start_measure=3 prepends 2 measures of rests before the voice. " +
                "Replaces the need for manual 'r/w r/w' padding. " +
                "For most use cases, prefer form.create_section which handles alignment automatically.")
            .param(voice)
            .param(startMeasure)
            .param(targetVoice)
            .handle((ctx, args) -> VoiceOpTools.padToMeasure(ctx, voice.extract(args),
                startMeasure.extract(args), targetVoice.extract(args)));
    }

    private static McpTool voiceTrimTool(final CompositionContextProvider provider) {
        final var voice = ToolParam.string("voice", "Name of the voice to trim")
            .minLength(1);
        final var bars = ToolParam.integer("bars", "Number of bars to keep (>= 1)")
            .min(1);
        return MusicToolDef.of(provider, "voice.trim",
            "Truncate a voice to N bars, discarding everything after. Writes back in place.")
            .param(voice)
            .param(bars)
            .handle((ctx, args) -> VoiceOpTools.trimVoice(ctx, voice.extract(args), bars.extract(args)));
    }

    private static McpTool voiceSetBarTool(final CompositionContextProvider provider) {
        final var voice = ToolParam.string("voice", "Voice name")
            .minLength(1);
        final var bar = ToolParam.integer("bar", "Bar number to replace (1-based)")
            .min(1);
        final var notes = ToolParam.string("notes", "New note sequence for the bar, e.g. 'C4/q D4/q E4/q F4/q'")
            .minLength(1);
        return MusicToolDef.of(provider, "voice.set_bar",
            "Replace a single bar's content with new notes. " +
                "Uses the same note syntax as voice.create. " +
                "Useful for fixing a wrong bar without recreating the whole voice.")
            .param(voice)
            .param(bar)
            .param(notes)
            .handle((ctx, args) -> VoiceOpTools.setBar(ctx, voice.extract(args), bar.extract(args), notes.extract(args)));
    }

    private static McpTool voiceReplaceRangeTool(final CompositionContextProvider provider) {
        final var voice = ToolParam.string("voice", "Voice name")
            .minLength(1);
        final var fromBar = ToolParam.integer("from_bar", "First bar to replace (1-based, inclusive)")
            .min(1);
        final var toBar = ToolParam.integer("to_bar", "Last bar to replace (1-based, inclusive)")
            .min(1);
        final var notes = ToolParam.string("notes", "Replacement note sequence")
            .minLength(1);
        return MusicToolDef.of(provider, "voice.replace_range",
            "Replace a range of bars with new notes. " +
                "from_bar and to_bar are both inclusive. " +
                "The new notes replace the entire span — they don't have to fill the same number of bars.")
            .param(voice)
            .param(fromBar)
            .param(toBar)
            .param(notes)
            .handle((ctx, args) -> VoiceOpTools.replaceRange(ctx, voice.extract(args),
                fromBar.extract(args), toBar.extract(args), notes.extract(args)));
    }

    private static McpTool voiceReplaceNoteTool(final CompositionContextProvider provider) {
        final var voice = ToolParam.string("voice", "Voice name")
            .minLength(1);
        final var bar = ToolParam.integer("bar", "Bar number (1-based)")
            .min(1);
        final var old = ToolParam.string("old", "Note to find by pitch, e.g. 'C4/q'")
            .minLength(1);
        final var neu = ToolParam.string("new", "Replacement note(s), e.g. 'D4/q'")
            .minLength(1);
        return MusicToolDef.of(provider, "voice.replace_note",
            "Surgically replace a single note in a bar. " +
                "Finds the first note in the given bar whose pitch matches 'old', replaces it with 'new'. " +
                "old and new are full note tokens including duration, e.g. 'C4/q' or 'F#4/e'. " +
                "Use query.voice to inspect bar content before calling this.")
            .param(voice)
            .param(bar)
            .param(old)
            .param(neu)
            .handle((ctx, args) -> VoiceOpTools.replaceNote(ctx, voice.extract(args),
                bar.extract(args), old.extract(args), neu.extract(args)));
    }

    private static McpTool voiceMeasureCountTool(final CompositionContextProvider provider) {
        final var voice = ToolParam.string("voice", "Voice name")
            .minLength(1);
        return MusicToolDef.of(provider, "voice.measure_count",
            "Return the number of complete bars in a voice. " +
                "Also shown in voice.list — use this when you only need the count for one voice.")
            .param(voice)
            .annotations(READ_ONLY)
            .handle((ctx, args) -> VoiceOpTools.measureCount(ctx, voice.extract(args)));
    }

    private static McpTool formCreateSectionTool(final CompositionContextProvider provider) {
        final var name = ToolParam.string("name", "Section name, e.g. 'A', 'B', 'Verse', 'Chorus'")
            .minLength(1);
        final var measures = ToolParam.integer("measures", "How many measures this section spans (optional — inferred from voices if omitted)")
            .min(1)
            .optional();
        final var voiceNames = ToolParam.string("voice_names", "Comma-separated voice names (optional, defaults to all)")
            .minLength(1)
            .optional();
        return MusicToolDef.of(provider, "form.create_section",
            "Create a named section from the current voices (or specified voices). " +
                "Sections are the building blocks of formal plans: A, B, Verse, Chorus, etc. " +
                "measures is optional — if omitted, inferred from the longest provided voice. " +
                "Explicit measures overrides inference; a warning is returned if they disagree. " +
                "voice_names: optional comma-separated list; uses all voices if omitted.")
            .param(name)
            .param(measures)
            .param(voiceNames)
            .handle((ctx, args) -> FormTools.createSection(ctx, name.extract(args),
                voiceNames.extract(args), measures.extract(args)));
    }

    private static McpTool formRepeatSectionTool(final CompositionContextProvider provider) {
        final var section = ToolParam.string("section", "Name of section to repeat (must exist)")
            .minLength(1);
        final var newLabel = ToolParam.string("new_label", "Optional display label for this occurrence")
            .minLength(1)
            .optional();
        return MusicToolDef.of(provider, "form.repeat_section",
            "Repeat a previously defined section in the formal plan. " +
                "Creates ABA, AABA, rondo forms, etc. " +
                "new_label: optional display label (e.g. 'A\\'' for the return of A).")
            .param(section)
            .param(newLabel)
            .handle((ctx, args) -> FormTools.repeatSection(ctx, section.extract(args), newLabel.extract(args)));
    }

    private static McpTool formSetEndingTool(final CompositionContextProvider provider) {
        final var section = ToolParam.string("section", "Section to attach the ending to")
            .minLength(1);
        final var pass = ToolParam.integer("pass", "Which repetition this ending plays on (1-based)")
            .min(1);
        final var endingSection = ToolParam.string("ending_section", "Name of a previously created section providing the ending bars")
            .minLength(1);
        return MusicToolDef.of(provider, "form.set_ending",
            "Register a per-pass ending for a section — standard 1st/2nd ending volta brackets. " +
                "Create the ending bars as a separate section first (e.g. 'A_end_1', 'A_end_2'), " +
                "then call form.set_ending to attach them. " +
                "The ending section's measure count determines how many tail bars are replaced. " +
                "Example: 8-bar section A with 1-bar endings → bar 8 differs between pass 1 and pass 2. " +
                "form.build will render \\repeat volta brackets in LilyPond and correct MIDI per pass.")
            .param(section)
            .param(pass)
            .param(endingSection)
            .handle((ctx, args) -> FormTools.setEnding(ctx, section.extract(args),
                pass.extract(args), endingSection.extract(args)));
    }

    private static McpTool formBuildTool(final CompositionContextProvider provider) {
        return MusicToolDef.of(provider, "form.build",
            "Assemble the score from the formal plan. " +
                "Concatenates sections in order, filling missing voices with rests. " +
                "Creates assembled voices in the composition context.")
            .handle((ctx, args) -> FormTools.buildScore(ctx));
    }

    private static McpTool formDescribeTool(final CompositionContextProvider provider) {
        return MusicToolDef.of(provider, "form.describe",
            "Show the current formal plan structure and section count.")
            .annotations(READ_ONLY)
            .handle((ctx, args) -> FormTools.describeForm(ctx));
    }

    private static McpTool rulesCheckTool(final CompositionContextProvider provider) {
        return MusicToolDef.of(provider, "rules.check",
            "Run composition rules against all voices and report any violations. " +
                "Checks voice leading (large leaps, repetition) and meter consistency. " +
                "Violations are advisory — they do not prevent composition.")
            .annotations(READ_ONLY)
            .handle((ctx, args) -> RulesTools.check(ctx));
    }

    private static McpTool rulesCheckRangeTool(final CompositionContextProvider provider) {
        final var voice = ToolParam.string("voice", "Voice to check")
            .minLength(1);
        final var instrument = ToolParam.string("instrument", "Instrument name, e.g. 'Flute', 'Violin'")
            .minLength(1);
        return MusicToolDef.of(provider, "rules.check_range",
            "Check whether a voice fits within an instrument's written range. " +
                "Reports notes that are too high, too low, or in extreme registers. " +
                "Instrument names: Piano, Flute, Violin, Cello, Trumpet, French Horn, Clarinet, etc.")
            .param(voice)
            .param(instrument)
            .annotations(READ_ONLY)
            .handle((ctx, args) -> RulesTools.checkRange(ctx, voice.extract(args), instrument.extract(args)));
    }

    private static McpTool instrumentInfoTool() {
        final var instrument = ToolParam.string("instrument", "Instrument name, e.g. 'Flute', 'Violin', 'Piano'")
            .minLength(1);
        return MusicToolDef.ofNoCtx(
            "instrument.info",
            "Get information about an instrument: range, family, MIDI program, clef. " +
                "Instrument names: Piano, Flute, Oboe, Clarinet, Bassoon, Trumpet, French Horn, " +
                "Trombone, Tuba, Violin, Viola, Cello, Double Bass, Harp, Organ, etc.")
            .param(instrument)
            .annotations(READ_ONLY)
            .handle(args -> InstrumentTools.info(instrument.extract(args)));
    }

    private static McpTool drumsPresetTool(final CompositionContextProvider provider) {
        final var preset = ToolParam.string("preset", "Preset name: house_4on4, rock_8th, rock_basic, bossa_nova, waltz, waltz_jazz, swing, afrohouse")
            .values(List.of("house_4on4", "rock_8th", "rock_basic", "bossa_nova", "waltz", "waltz_jazz", "swing", "afrohouse"));
        final var bars = ToolParam.integer("bars", "Number of bars to generate (1–64)")
            .min(1).max(64);
        return MusicToolDef.of(provider, "drums.preset",
            "Load a drum preset pattern for the specified number of bars. " +
                "Creates multiple voices (drums_kick, drums_snare, drums_hihat, etc.) assigned to MIDI channel 9 (GM percussion). " +
                "Example: drums.preset house_4on4 for 8 bars gives a four-on-the-floor dance pattern. " +
                "Available presets: " +
                "house_4on4 (four-on-floor kick, clap on 2&4, eighth hi-hats), " +
                "rock_8th (kick on 1&3, snare on 2&4, eighth hi-hats), " +
                "rock_basic (syncopated kick variation), " +
                "bossa_nova (dotted-quarter kick, rim shot pattern), " +
                "waltz (3/4 kick on 1, snare on 2&3), " +
                "waltz_jazz (3/4 kick+hihat on 1, brushed snare on 2&3), " +
                "swing (ride swing eighths, half-note kick, foot hi-hat on 2&4), " +
                "afrohouse (syncopated kick on 1/and-of-2/3, open hi-hat accents, conga layer, maracas eighths).")
            .param(preset)
            .param(bars)
            .handle((ctx, args) -> DrumPresets.loadPreset(ctx, preset.extract(args), bars.extract(args)));
    }

    private static McpTool scoreSetSwingTool(final CompositionContextProvider provider) {
        final var ratio = ToolParam.string("ratio", "Swing ratio between 0.5 and 1.0 — e.g. '2/3' (standard), '3/5' (light), '0' to disable")
            .minLength(1);
        return MusicToolDef.of(provider, "score.set_swing",
            "Enable swing quantization for MIDI playback. " +
                "Swing delays off-beat eighth notes to create the jazz/funk/blues lilt. " +
                "Standard jazz swing: ratio=2/3 (long-short 2:1). Light shuffle: 3/5. " +
                "Only affects eighth notes; quarter notes, dotted values, and triplets are unchanged. " +
                "Set ratio to '0' to disable swing and return to straight timing. " +
                "Example: score.set_swing 2/3 → jazz standard swing feel.")
            .param(ratio)
            .annotations(IDEMPOTENT)
            .handle((ctx, args) -> ScoreTools.setSwing(ctx, ratio.extract(args)));
    }

    private static McpTool scoreSetTempoChangeTool(final CompositionContextProvider provider) {
        final var startBar = ToolParam.integer("start_bar", "First bar of the tempo change (1-based)")
            .min(1);
        final var endBar = ToolParam.integer("end_bar", "Last bar — target tempo reached here")
            .min(1);
        final var toBpm = ToolParam.integer("to_bpm", "Target BPM at end_bar (1–400)")
            .min(1).max(400);
        final var curve = ToolParam.string("curve", "Interpolation curve: 'linear' (default) or 'exponential'")
            .values(CURVE)
            .optional();
        return MusicToolDef.of(provider, "score.set_tempo_change",
            "Declare a gradual tempo change (ritardando or accelerando) across a bar span. " +
                "The change starts at start_bar and reaches to_bpm by end_bar. " +
                "Affects both MIDI playback (interpolated tempo events per bar) and LilyPond notation (rit./accel. mark). " +
                "curve: 'linear' = equal BPM steps; 'exponential' = change accelerates toward end (more natural for rit.). " +
                "Example: start_bar=22 end_bar=24 to_bpm=60 curve=exponential for a 3-bar ritardando.")
            .param(startBar)
            .param(endBar)
            .param(toBpm)
            .param(curve)
            .handle((ctx, args) -> ScoreTools.setTempoChange(ctx,
                startBar.extract(args), endBar.extract(args), toBpm.extract(args), curve.extract(args)));
    }

    private static McpTool scoreSaveTool(final CompositionContextProvider provider) {
        final var filename = ToolParam.string("filename", "Filename (without path) to save, e.g. 'my_song' or 'my_song.json'")
            .minLength(1);
        return MusicToolDef.of(provider, "score.save",
            "Save the current composition to a JSON snapshot file under generated_tracks/. " +
                "The snapshot captures all voices, motifs, metadata, key, swing, bar chords, and tempo changes. " +
                "Use score.load to restore the session later. " +
                "Example: score.save filename='my_song' — writes generated_tracks/my_song.json")
            .param(filename)
            .annotations(OPEN_WORLD)
            .handle((ctx, args) -> SaveLoadTools.save(ctx, filename.extract(args)));
    }

    private static McpTool scoreLoadTool(final CompositionContextProvider provider) {
        final var filename = ToolParam.string("filename", "Filename (without path) to load, e.g. 'my_song' or 'my_song.json'")
            .minLength(1);
        return MusicToolDef.of(provider, "score.load",
            "Load a composition from a JSON snapshot file under generated_tracks/. " +
                "Replaces all current state with the saved session. " +
                "Example: score.load filename='my_song' — reads generated_tracks/my_song.json")
            .param(filename)
            .annotations(OPEN_WORLD)
            .handle((ctx, args) -> SaveLoadTools.load(ctx, filename.extract(args)));
    }

    private static McpTool scoreLoadMidiTool(final CompositionContextProvider provider) {
        final var path = ToolParam.string("path", "Path to the MIDI file, e.g. 'generated_tracks/1_my_song/my_song.mid'")
            .minLength(1);
        return MusicToolDef.of(provider, "score.load_midi",
            "Import a MIDI file as voices into the current composition. " +
                "Creates one voice per non-empty MIDI track, named 'voice-0', 'voice-1', etc. " +
                "Also sets the tempo from the MIDI file. " +
                "Existing voices are not cleared — call score.clear first if you want a fresh start. " +
                "Note: MIDI loses enharmonic spelling; all black keys come back as sharps (C#, F#, etc.). " +
                "path can be absolute or relative to the server's working directory.")
            .param(path)
            .annotations(OPEN_WORLD)
            .handle((ctx, args) -> SaveLoadTools.loadMidi(ctx, path.extract(args)));
    }

    private static McpTool scoreLoadAbcTool(final CompositionContextProvider provider) {
        final var abc = ToolParam.string("abc", "Full ABC notation string, e.g. 'X:1\\nT:My Tune\\nL:1/8\\nK:G\\nGABc|...'")
            .minLength(1);
        return MusicToolDef.of(provider, "score.load_abc",
            "Parse an ABC notation string into a voice and add it to the current composition. " +
                "ABC notation is a text format widely used for folk and traditional music " +
                "(thesession.org, abcnotation.com have 400k+ tunes). " +
                "Supports: T:/L:/M:/K:/Q: headers, accidentals (^=sharp, _=flat, ==natural), " +
                "octave markers (' raises, , lowers), length multipliers (A2, A/, A3/2), " +
                "ties (-), bar lines, and repeats. " +
                "Sets tempo from Q: if present. Voice is named after the T: title field. " +
                "Existing voices are not cleared — call score.clear first if you want a fresh start.")
            .param(abc)
            .handle((ctx, args) -> SaveLoadTools.loadAbc(ctx, abc.extract(args)));
    }
}
