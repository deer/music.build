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
import build.music.pitch.typesystem.MusicCodeModel;
import build.serve.mcp.McpContent;
import build.serve.mcp.McpServer;
import build.serve.mcp.McpTool;
import build.serve.mcp.McpToolResult;
import build.serve.mcp.McpTools;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Base64;
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

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MusicMcpTools() {
    }

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
    }

    // --- Tool factory methods ---

    private static McpTool voiceCreateTool(final CompositionContextProvider provider) {
        return tool(provider,
            "voice.create",
            "Create a named voice from a note sequence. " +
                "The voice is added to the composition and can be used for export or transformation. " +
                "Note format: 'pitch/duration' — e.g. 'C4/q E4/q G4/q C5/h'. " +
                "Duration codes: w=whole, h=half, q=quarter, e=eighth, s=sixteenth, dh, dq, de for dotted. " +
                "Rests: 'r/q' = quarter rest. " +
                "Chords: '<C4 E4 G4>/q' = C major triad as quarter chord. " +
                "Articulations: append ~stac (staccato), ~acc (accent), ~ten (tenuto), ~marc (marcato), ~leg (legato). " +
                "Example: 'C4/q~stac D4/e E4/e <F4 A4 C5>/h r/q'",
            McpTools.schema(MAPPER,
                Map.of(
                    "name", "Voice name (e.g. 'melody', 'bass', 'counterpoint')",
                    "notes", "Note sequence, e.g. 'E4/q E4/q F4/q G4/q G4/q F4/q E4/q D4/q'"
                ),
                List.of("name", "notes")),
            (ctx, args) -> VoiceTools.createVoice(ctx, str(args, "name"), str(args, "notes"))
        );
    }

    private static McpTool voiceAppendTool(final CompositionContextProvider provider) {
        return tool(provider,
            "voice.append",
            "Append additional notes to an existing voice. " +
                "Uses the same note format as voice.create: 'pitch/duration' tokens, chords '<C4 E4 G4>/q', and rests 'r/q'. " +
                "Example: build a melody bar-by-bar — create with voice.create, then voice.append each new bar.",
            McpTools.schema(MAPPER,
                Map.of(
                    "name", "Name of the existing voice to append to",
                    "notes", "Note sequence to append"
                ),
                List.of("name", "notes")),
            (ctx, args) -> VoiceTools.appendToVoice(ctx, str(args, "name"), str(args, "notes"))
        );
    }

    private static McpTool voiceFromMotifTool(final CompositionContextProvider provider) {
        return tool(provider,
            "voice.from_motif",
            "Advanced: Create a voice by applying a transform to a saved motif. " +
                "Example: take 'theme_a', transpose up P5, and use it as a new voice. " +
                "transform values: 'transpose' (requires interval arg, e.g. 'P5'), " +
                "'invert' (requires axis arg, e.g. 'C4'), 'retrograde', " +
                "'augment' (requires factor arg, e.g. '2/1'). " +
                "Omit transform to copy the motif unchanged.",
            buildObjectSchema(
                Map.of(
                    "voice_name", strProp("Name for the new voice"),
                    "motif_name", strProp("Name of the motif to copy"),
                    "transform", strProp("Optional: transpose, invert, retrograde, or augment"),
                    "interval", strProp("For transpose: interval string, e.g. 'P5', 'm3', 'M2'"),
                    "direction", enumProp("For transpose: direction", List.of("up", "down")),
                    "axis", strProp("For invert: axis pitch, e.g. 'C4'"),
                    "factor", strProp("For augment: fraction string, e.g. '2/1', '1/2', '3/2'")
                ),
                List.of("voice_name", "motif_name")),
            (ctx, args) -> {
                final String transform = optStr(args, "transform");
                Map<String, String> transformArgs = null;
                if (transform != null) {
                    transformArgs = new HashMap<>();
                    if (args.has("interval")) {
                        transformArgs.put("interval", str(args, "interval"));
                    }
                    if (args.has("direction")) {
                        transformArgs.put("direction", str(args, "direction"));
                    }
                    if (args.has("axis")) {
                        transformArgs.put("axis", str(args, "axis"));
                    }
                    if (args.has("factor")) {
                        transformArgs.put("factor", str(args, "factor"));
                    }
                }
                return VoiceTools.createVoiceFromMotif(ctx, str(args, "voice_name"), str(args, "motif_name"),
                    transform, transformArgs);
            }
        );
    }

    private static McpTool voiceListTool(final CompositionContextProvider provider) {
        return tool(provider,
            "voice.list",
            "List all voices in the current composition with bar counts, event counts, and total duration.",
            emptySchema(),
            (ctx, args) -> VoiceTools.listVoices(ctx)
        );
    }

    private static McpTool voiceDeleteTool(final CompositionContextProvider provider) {
        return tool(provider,
            "voice.delete",
            "Remove a voice from the composition. Use this to clean up scratch or scaffolding voices " +
                "that should not appear in the final score or form sections.",
            McpTools.schema(MAPPER,
                Map.of("name", "Name of the voice to delete"),
                List.of("name")),
            (ctx, args) -> VoiceOpTools.deleteVoice(ctx, str(args, "name"))
        );
    }

    private static McpTool scoreSetMetadataTool(final CompositionContextProvider provider) {
        return tool(provider,
            "score.set_metadata",
            "Set the title, tempo, and/or time signature of the composition. " +
                "All parameters are optional — only provided values are updated. " +
                "Time signature format: '4/4', '3/4', '6/8', etc.",
            buildObjectSchema(
                Map.of(
                    "title", strProp("Composition title"),
                    "tempo", intProp("Tempo in BPM (1-400)"),
                    "time_signature", strProp("Time signature, e.g. '4/4', '3/4', '6/8'")
                ),
                List.of()),
            (ctx, args) -> ScoreTools.setMetadata(ctx, optStr(args, "title"), optInt(args, "tempo"), optStr(args, "time_signature"))
        );
    }

    private static McpTool scoreAssignInstrumentTool(final CompositionContextProvider provider) {
        return tool(provider,
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
                "Example: assign 'melody' to 'synth_lead', 'chords' to 'electric_piano', 'kick' to 'drums'.",
            McpTools.schema(MAPPER,
                Map.of(
                    "voice", "Name of the voice to assign",
                    "instrument", "Instrument name (e.g. 'piano', 'strings', 'flute')"
                ),
                List.of("voice", "instrument")),
            (ctx, args) -> ScoreTools.assignInstrument(ctx, str(args, "voice"), str(args, "instrument"))
        );
    }

    private static McpTool scoreDescribeTool(final CompositionContextProvider provider) {
        return tool(provider,
            "score.describe",
            "Get a complete description of the current composition state: " +
                "title, tempo, time signature, all voices with note counts, motifs, and instrument assignments. " +
                "Call this to understand the current state before deciding what to do next.",
            emptySchema(),
            (ctx, args) -> ScoreTools.describeScore(ctx)
        );
    }

    private static McpTool scoreClearTool(final CompositionContextProvider provider) {
        return tool(provider,
            "score.clear",
            "Clear the entire composition and start fresh. " +
                "Removes all voices, motifs, and instrument assignments. Resets title and tempo to defaults.",
            emptySchema(),
            (ctx, args) -> ScoreTools.clearScore(ctx)
        );
    }

    private static McpTool transformTransposeTool(final CompositionContextProvider provider) {
        return tool(provider,
            "transform.transpose",
            "Transpose all notes in a voice by a chromatic interval. " +
                "Interval format: quality+size — 'P5' (perfect fifth), 'm3' (minor third), " +
                "'M2' (major second), 'P8' (octave), 'A4' (augmented fourth/tritone). " +
                "Creates a new voice named '{voice}_transposed' unless target_voice is specified. " +
                "Example: transpose 'melody' up by M3 to create a parallel third harmony voice.",
            buildObjectSchema(
                Map.of(
                    "voice", strProp("Name of the voice to transpose"),
                    "interval", strProp("Interval string, e.g. 'P5', 'm3', 'M2'"),
                    "direction", enumProp("Direction of transposition", List.of("up", "down")),
                    "target_voice", strProp("Optional: name for the resulting voice")
                ),
                List.of("voice", "interval")),
            (ctx, args) -> TransformTools.transpose(ctx, str(args, "voice"), str(args, "interval"),
                optStr(args, "direction") != null ? optStr(args, "direction") : "up",
                optStr(args, "target_voice"))
        );
    }

    private static McpTool transformInvertTool(final CompositionContextProvider provider) {
        return tool(provider,
            "transform.invert",
            "Advanced: Invert a voice around an axis pitch (mirror melodic intervals). " +
                "Each note is reflected to the same interval below the axis that it was above it. " +
                "Creates a new voice named '{voice}_inverted' unless target_voice is specified.",
            buildObjectSchema(
                Map.of(
                    "voice", strProp("Name of the voice to invert"),
                    "axis", strProp("Axis pitch to invert around, e.g. 'C4', 'E4'"),
                    "target_voice", strProp("Optional: name for the resulting voice")
                ),
                List.of("voice", "axis")),
            (ctx, args) -> TransformTools.invert(ctx, str(args, "voice"), str(args, "axis"), optStr(args, "target_voice"))
        );
    }

    private static McpTool transformRetrogradeTool(final CompositionContextProvider provider) {
        return tool(provider,
            "transform.retrograde",
            "Advanced: Reverse the temporal order of all events in a voice (play it backwards). " +
                "Creates a new voice named '{voice}_retrograde' unless target_voice is specified.",
            buildObjectSchema(
                Map.of(
                    "voice", strProp("Name of the voice to reverse"),
                    "target_voice", strProp("Optional: name for the resulting voice")
                ),
                List.of("voice")),
            (ctx, args) -> TransformTools.retrograde(ctx, str(args, "voice"), optStr(args, "target_voice"))
        );
    }

    private static McpTool transformAugmentTool(final CompositionContextProvider provider) {
        return tool(provider,
            "transform.augment",
            "Advanced: Scale all durations in a voice by a rational factor. " +
                "Factor '2/1' = augmentation (double duration), '1/2' = diminution (halve), '3/2' = dotted feel. " +
                "Creates a new voice named '{voice}_augmented' unless target_voice is specified.",
            buildObjectSchema(
                Map.of(
                    "voice", strProp("Name of the voice to scale"),
                    "factor", strProp("Duration scale factor as fraction, e.g. '2/1', '1/2', '3/2'"),
                    "target_voice", strProp("Optional: name for the resulting voice")
                ),
                List.of("voice", "factor")),
            (ctx, args) -> TransformTools.augment(ctx, str(args, "voice"), str(args, "factor"), optStr(args, "target_voice"))
        );
    }

    private static McpTool motifSaveTool(final CompositionContextProvider provider) {
        return tool(provider,
            "motif.save",
            "Save a slice of a voice as a named motif for later reuse with voice.from_motif. " +
                "start_note and end_note are 0-based indices (end_note is exclusive). " +
                "Omit both to save the entire voice. " +
                "Example: save the first 4 notes of 'melody' as 'opening_theme', then use voice.from_motif to create variations.",
            buildObjectSchema(
                Map.of(
                    "voice", strProp("Name of the voice to extract from"),
                    "motif_name", strProp("Name to give the saved motif"),
                    "start_note", intProp("0-based start index (inclusive), default 0"),
                    "end_note", intProp("0-based end index (exclusive), default = end of voice")
                ),
                List.of("voice", "motif_name")),
            (ctx, args) -> TransformTools.saveMotif(ctx, str(args, "voice"), str(args, "motif_name"),
                optInt(args, "start_note"), optInt(args, "end_note"))
        );
    }

    private static McpTool queryVoiceTool(final CompositionContextProvider provider) {
        return tool(provider,
            "query.voice",
            "Display the notes of a specific voice in a human-readable format with bar lines. " +
                "Use this to inspect a voice before transforming or exporting.",
            McpTools.schema(MAPPER,
                Map.of("voice", "Name of the voice to display"),
                List.of("voice")),
            (ctx, args) -> QueryTools.queryVoice(ctx, str(args, "voice"))
        );
    }

    private static McpTool queryMotifTool(final CompositionContextProvider provider) {
        return tool(provider,
            "query.motif",
            "Display the notes of a saved motif.",
            McpTools.schema(MAPPER,
                Map.of("motif", "Name of the motif to display"),
                List.of("motif")),
            (ctx, args) -> QueryTools.queryMotif(ctx, str(args, "motif"))
        );
    }

    private static McpTool exportAllTool(final CompositionContextProvider provider,
                                         final ExportOptions exportOptions) {
        return tool(provider,
            "export.all",
            "Export the current composition to a folder containing both a MIDI file (.mid) and " +
                "LilyPond source (.ly). If LilyPond is installed, also engraves a PDF. " +
                "The folder is created in the current working directory. " +
                "Folder name defaults to the composition title if not specified.",
            buildObjectSchema(
                Map.of("folder", strProp("Output folder name (optional, defaults to composition title)")),
                List.of()),
            (ctx, args) -> ExportTools.exportAll(ctx, optStr(args, "folder"), exportOptions)
        );
    }

    private static McpTool exportMidiTool(final CompositionContextProvider provider,
                                          final ExportOptions exportOptions) {
        return tool(provider,
            "export.midi",
            "Render the current composition to a Standard MIDI File (.mid). " +
                "Filename is optional — defaults to '{title}.mid'. " +
                "Returns the path to the written file.",
            buildObjectSchema(
                Map.of("filename", strProp("Output filename, e.g. 'my_piece.mid' (optional)")),
                List.of()),
            (ctx, args) -> ExportTools.exportMidi(ctx, optStr(args, "filename"), exportOptions)
        );
    }

    private static McpTool exportLilypondTool(final CompositionContextProvider provider,
                                              final ExportOptions exportOptions) {
        return tool(provider,
            "export.lilypond",
            "Render the current composition to LilyPond source (.ly file). " +
                "If LilyPond is installed on PATH, also engraves to PDF. " +
                "Returns the LilyPond source text in the data field.",
            buildObjectSchema(
                Map.of("filename", strProp("Output base filename without extension (optional)")),
                List.of()),
            (ctx, args) -> ExportTools.exportLilypond(ctx, optStr(args, "filename"), exportOptions)
        );
    }

    private static McpTool exportMusicXmlTool(final CompositionContextProvider provider,
                                               final ExportOptions exportOptions) {
        return tool(provider,
            "export.musicxml",
            "Export the current composition to MusicXML 4.0 (.musicxml). " +
                "MusicXML is the standard interchange format for notation software — " +
                "opens in MuseScore, Dorico, Sibelius, Finale, and most DAWs. " +
                "Filename is optional — defaults to '{title}.musicxml'.",
            buildObjectSchema(
                Map.of("filename", strProp("Output base filename without extension (optional)")),
                List.of()),
            (ctx, args) -> ExportTools.exportMusicXml(ctx, optStr(args, "filename"), exportOptions)
        );
    }

    private static McpTool harmonySetKeyTool(final CompositionContextProvider provider) {
        return tool(provider, "harmony.set_key",
            "Set the musical key for the composition. " +
                "Format: 'Tonic Mode' — e.g. 'C major', 'A minor', 'F# minor', 'Bb major'. " +
                "The key is used for harmonization, diatonic transposition, and chord progressions.",
            McpTools.schema(MAPPER, Map.of("key", "Key description, e.g. 'C major', 'G major', 'D minor'"),
                List.of("key")),
            (ctx, args) -> HarmonyTools.setKey(ctx, str(args, "key")));
    }

    private static McpTool harmonyChordProgressionTool(final CompositionContextProvider provider) {
        return tool(provider, "harmony.chord_progression",
            "Set a chord progression using Roman numerals. " +
                "Format: space or dash-separated Roman numerals, e.g. 'I IV V I', 'ii V I', 'I V vi IV'. " +
                "Upper case = major chord, lower case = minor, 'o' suffix = diminished, '7' = seventh. " +
                "Requires harmony.set_key to be called first for full resolution.",
            McpTools.schema(MAPPER,
                Map.of("progression", "Roman numeral progression, e.g. 'I IV V I', 'ii V I'"),
                List.of("progression")),
            (ctx, args) -> HarmonyTools.setChordProgression(ctx, str(args, "progression")));
    }

    private static McpTool harmonySetBarsTool(final CompositionContextProvider provider) {
        return tool(provider, "harmony.set_bars",
            "Declare chord changes per measure as concrete chord symbols (not Roman numerals). " +
                "Format: '1:Cm7 2:F7 3:BbM7 4:Eb' — bar number, colon, chord symbol. " +
                "Chord qualities: (bare root)=major, m=minor, 7=dom7, maj7, m7, m7b5, dim, dim7, aug, sus2, sus4. " +
                "Examples: 'Gm7', 'C7', 'Fm7b5', 'Bb7', 'EbM7'. " +
                "Use for jazz chord sheets, ii-V-I sequences, blues changes. " +
                "These chords are used by harmony.walking_bass and override harmony.chord_progression for those tools. " +
                "Example: harmony.set_bars '1:Gm7 2:C7 3:Fm7 4:Bb7 5:Ebmaj7 6:Ebmaj7 7:Cm7 8:D7'",
            McpTools.schema(MAPPER,
                Map.of("bars", "Bar chord string, e.g. '1:Cm7 2:F7 3:Bb7 4:Eb'"),
                List.of("bars")),
            (ctx, args) -> HarmonyTools.setBarChords(ctx, str(args, "bars")));
    }

    private static McpTool harmonyHarmonizeTool(final CompositionContextProvider provider) {
        return tool(provider, "harmony.harmonize",
            "Add chord root accompaniment to a melody voice based on the current chord progression. " +
                "Requires harmony.set_key and harmony.chord_progression to be called first. " +
                "Creates a new voice with chord roots (one per chord in the progression). " +
                "Example: key=C major, progression=I IV V I, melody='bass' → creates bass line of C F G C roots.",
            buildObjectSchema(
                Map.of("voice", strProp("Melody voice to harmonize"),
                    "target_voice", strProp("Name for the harmony voice (optional)"),
                    "octave", intProp("Octave for chord roots, e.g. 3 for bass")),
                List.of("voice")),
            (ctx, args) -> HarmonyTools.harmonize(ctx, str(args, "voice"), optStr(args, "target_voice"),
                args.has("octave") ? args.get("octave").asInt() : 3));
    }

    private static McpTool harmonySuggestHarmonyTool(final CompositionContextProvider provider) {
        return tool(provider, "harmony.suggest_harmony",
            "Analyze a melody voice and suggest a chord progression based on its pitch content. " +
                "Finds the best-fitting diatonic chord for each measure, saves as current progression. " +
                "Example result: 'I IV I V' for a simple melody in C major. " +
                "Requires harmony.set_key to be called first.",
            McpTools.schema(MAPPER,
                Map.of("voice", "Melody voice to analyze"),
                List.of("voice")),
            (ctx, args) -> HarmonyTools.suggestHarmony(ctx, str(args, "voice")));
    }

    private static McpTool harmonyDetectKeyTool(final CompositionContextProvider provider) {
        return tool(provider, "harmony.detect_key",
            "Advanced: Detect the likely key of a voice using pitch class distribution analysis. " +
                "Sets the detected key as the current key. Use harmony.set_key when you know the key.",
            McpTools.schema(MAPPER,
                Map.of("voice", "Voice to analyze for key detection"),
                List.of("voice")),
            (ctx, args) -> HarmonyTools.detectKey(ctx, str(args, "voice")));
    }

    private static McpTool harmonyDiatonicTransposeTool(final CompositionContextProvider provider) {
        return tool(provider, "harmony.diatonic_transpose",
            "Transpose a voice by scale steps within the current key, preserving key membership. " +
                "Example: steps=2 in C major moves C→E, D→F (a diatonic third, not always a major third). " +
                "Steps=-1 moves down one scale step. " +
                "Useful for harmonizing in thirds or sixths. Requires harmony.set_key first.",
            buildObjectSchema(
                Map.of("voice", strProp("Voice to transpose"),
                    "steps", intProp("Number of scale steps (positive=up, negative=down)"),
                    "target_voice", strProp("Name for result voice (optional)")),
                List.of("voice", "steps")),
            (ctx, args) -> HarmonyTools.diatonicTranspose(ctx, str(args, "voice"),
                args.get("steps").asInt(), optStr(args, "target_voice")));
    }

    private static McpTool harmonyWalkingBassTool(final CompositionContextProvider provider) {
        return tool(provider, "harmony.walking_bass",
            "Generate a walking bass line over chord changes, adapted to the current time signature. " +
                "4/4: root-fifth-third-approach (4 quarter notes). 3/4: root-fifth-fifth. " +
                "6/8: root-fifth (2 dotted-quarters). 2/4: root-approach. " +
                "Requires harmony.set_bars (preferred) or harmony.set_key + harmony.chord_progression. " +
                "Set score.set_swing to add shuffle feel after generating. " +
                "Example usage: set_bars → walking_bass target_voice='bass' octave=2 → assign to electric_bass.",
            buildObjectSchema(
                Map.of(
                    "target_voice", strProp("Name for the bass voice (default: 'bass')"),
                    "octave", intProp("Bass octave (default: 2 — C2 to B2 range)"),
                    "bars", intProp("Number of bars (default: use all defined chords)"),
                    "velocity", strProp("Velocity/dynamics: ppp, pp, p, mp, mf (default), f, ff, fff"),
                    "approach", strProp("Approach note style: chromatic (default, 1–2 semitones below), diatonic (scale step below), none (use chord root)")
                ),
                List.of()),
            (ctx, args) -> HarmonyTools.walkingBass(ctx, optStr(args, "target_voice"),
                args.has("octave") ? args.get("octave").asInt() : 2,
                args.has("bars") ? args.get("bars").asInt() : null,
                optStr(args, "velocity"),
                optStr(args, "approach")));
    }

    private static McpTool harmonyCompTool(final CompositionContextProvider provider) {
        return tool(provider, "harmony.comp",
            "Generate a comping (chord accompaniment) voice over bar-level chord changes. " +
                "Styles: " +
                "'quarter_stabs' — block chord on beats 2 and 4 (default, jazz comping); " +
                "'on_beat' — block chord on every beat; " +
                "'eighth_pump' — block chord on every eighth note (funk/rock); " +
                "'shell_voicings' — root + 7th on beats 2 and 4 (sparse jazz voicing); " +
                "'charleston' — dotted quarter + eighth + quarter + rest (classic jazz Charleston rhythm). " +
                "Requires harmony.set_bars (preferred) or harmony.set_key + harmony.chord_progression. " +
                "Example: set_bars → comp style='charleston' octave=3 → assign to piano.",
            buildObjectSchema(
                Map.of(
                    "target_voice", strProp("Name for the comping voice (default: 'comp')"),
                    "octave", intProp("Voicing octave (default: 3 — mid-range)"),
                    "style", strProp("Comping style: quarter_stabs, on_beat, eighth_pump, shell_voicings, charleston (default: quarter_stabs)"),
                    "bars", intProp("Number of bars (default: use all defined chords)"),
                    "velocity", strProp("Velocity/dynamics: ppp, pp, p, mp, mf (default), f, ff, fff")
                ),
                List.of()),
            (ctx, args) -> HarmonyTools.comp(ctx, optStr(args, "target_voice"),
                args.has("octave") ? args.get("octave").asInt() : 3,
                optStr(args, "style"),
                args.has("bars") ? args.get("bars").asInt() : null,
                optStr(args, "velocity")));
    }

    private static McpTool voiceSetDynamicsTool(final CompositionContextProvider provider) {
        return tool(provider,
            "voice.set_dynamics",
            "Set the dynamics (volume) for a voice. " +
                "Applies to all notes in the voice when rendering to MIDI. " +
                "Example: set 'bass' to forte for a driving bass line, 'pad' to piano for background chords. " +
                "Dynamics values: ppp, pp, p, mp, mf (default), f, ff, fff.",
            McpTools.schema(MAPPER,
                Map.of(
                    "voice", "Name of the voice to set dynamics for",
                    "dynamics", "Dynamic level: ppp, pp, p, mp, mf, f, ff, fff"
                ),
                List.of("voice", "dynamics")),
            (ctx, args) -> VoiceTools.setDynamics(ctx, str(args, "voice"), str(args, "dynamics"))
        );
    }

    private static McpTool voiceSetArticulationTool(final CompositionContextProvider provider) {
        return tool(provider,
            "voice.set_articulation",
            "Set articulation for a voice, optionally scoped to a bar range. " +
                "Without from_bar/to_bar, applies to the whole voice. " +
                "With from_bar/to_bar, only those bars are affected — e.g. make just the final phrase tenuto. " +
                "Affects both MIDI playback (note duration) and LilyPond notation marks. " +
                "Articulations: normal (default), staccato, accent, tenuto, marcato, legato, portato.",
            buildObjectSchema(
                Map.of(
                    "voice", strProp("Name of the voice to set articulation for"),
                    "articulation", strProp("Articulation: normal, staccato, accent, tenuto, marcato, legato, portato"),
                    "from_bar", intProp("First bar of the range (1-based, optional)"),
                    "to_bar", intProp("Last bar of the range (inclusive, optional)")
                ),
                List.of("voice", "articulation")),
            (ctx, args) -> VoiceTools.setArticulation(ctx, str(args, "voice"), str(args, "articulation"),
                args.has("from_bar") ? args.get("from_bar").asInt() : null,
                args.has("to_bar") ? args.get("to_bar").asInt() : null)
        );
    }

    private static McpTool voiceConcatTool(final CompositionContextProvider provider) {
        return tool(provider, "voice.concat",
            "Concatenate multiple voices into a single voice sequentially. " +
                "Voice B starts exactly where voice A ends — essential for ABA form, verse-chorus, etc. " +
                "voice_names: comma-separated list of voice names in order. " +
                "Example: concat 'verse1, chorus, verse2, chorus' into 'full_melody'.",
            buildObjectSchema(
                Map.of("voice_names", strProp("Comma-separated voice names, e.g. 'verse1, chorus, verse2'"),
                    "target_voice", strProp("Name for the resulting concatenated voice")),
                List.of("voice_names", "target_voice")),
            (ctx, args) -> VoiceOpTools.concat(ctx, str(args, "voice_names"), str(args, "target_voice")));
    }

    private static McpTool voiceRepeatTool(final CompositionContextProvider provider) {
        return tool(provider, "voice.repeat",
            "Repeat a voice N times, creating a new concatenated voice. " +
                "Useful for ostinatos, repeated motifs, and riff-based composition. " +
                "Example: create a 1-bar bass riff, then voice.repeat it 8 times for an 8-bar groove.",
            buildObjectSchema(
                Map.of("voice", strProp("Voice to repeat"),
                    "times", intProp("Number of repetitions (>= 1)"),
                    "target_voice", strProp("Name for the repeated voice (optional)")),
                List.of("voice", "times")),
            (ctx, args) -> VoiceOpTools.repeat(ctx, str(args, "voice"),
                args.get("times").asInt(), optStr(args, "target_voice")));
    }

    private static McpTool voiceSliceTool(final CompositionContextProvider provider) {
        return tool(provider, "voice.slice",
            "Advanced: Extract a range of measures from a voice. " +
                "start_measure and end_measure are 1-based; end_measure is exclusive. " +
                "E.g., start=1, end=3 extracts measures 1 and 2. " +
                "For most form-building use cases, prefer form.create_section and form.repeat_section.",
            buildObjectSchema(
                Map.of("voice", strProp("Voice to slice"),
                    "start_measure", intProp("First measure to include (1-based)"),
                    "end_measure", intProp("Measure to stop before (exclusive)"),
                    "target_voice", strProp("Name for the sliced voice (optional)")),
                List.of("voice", "start_measure", "end_measure")),
            (ctx, args) -> VoiceOpTools.sliceMeasures(ctx, str(args, "voice"),
                args.get("start_measure").asInt(), args.get("end_measure").asInt(),
                optStr(args, "target_voice")));
    }

    private static McpTool voicePadToMeasureTool(final CompositionContextProvider provider) {
        return tool(provider, "voice.pad_to_measure",
            "Advanced: Add leading rests to a voice so it starts at a given measure. " +
                "start_measure=3 prepends 2 measures of rests before the voice. " +
                "Replaces the need for manual 'r/w r/w' padding. " +
                "For most use cases, prefer form.create_section which handles alignment automatically.",
            buildObjectSchema(
                Map.of("voice", strProp("Voice to pad"),
                    "start_measure", intProp("Measure number where the voice should begin (1-based)"),
                    "target_voice", strProp("Name for the padded voice (optional, replaces original if omitted)")),
                List.of("voice", "start_measure")),
            (ctx, args) -> VoiceOpTools.padToMeasure(ctx, str(args, "voice"),
                args.get("start_measure").asInt(), optStr(args, "target_voice")));
    }

    private static McpTool voiceTrimTool(final CompositionContextProvider provider) {
        return tool(provider, "voice.trim",
            "Truncate a voice to N bars, discarding everything after. Writes back in place.",
            McpTools.schema(MAPPER,
                Map.of("voice", "Name of the voice to trim", "bars", "Number of bars to keep"),
                List.of("voice", "bars")),
            (ctx, args) -> VoiceOpTools.trimVoice(ctx, str(args, "voice"), args.get("bars").asInt()));
    }

    private static McpTool voiceSetBarTool(final CompositionContextProvider provider) {
        return tool(provider, "voice.set_bar",
            "Replace a single bar's content with new notes. " +
                "Uses the same note syntax as voice.create. " +
                "Useful for fixing a wrong bar without recreating the whole voice.",
            buildObjectSchema(
                Map.of("voice", strProp("Voice name"),
                    "bar", intProp("Bar number to replace (1-based)"),
                    "notes", strProp("New note sequence for the bar, e.g. 'C4/q D4/q E4/q F4/q'")),
                List.of("voice", "bar", "notes")),
            (ctx, args) -> VoiceOpTools.setBar(ctx, str(args, "voice"), args.get("bar").asInt(), str(args, "notes")));
    }

    private static McpTool voiceReplaceRangeTool(final CompositionContextProvider provider) {
        return tool(provider, "voice.replace_range",
            "Replace a range of bars with new notes. " +
                "from_bar and to_bar are both inclusive. " +
                "The new notes replace the entire span — they don't have to fill the same number of bars.",
            buildObjectSchema(
                Map.of("voice", strProp("Voice name"),
                    "from_bar", intProp("First bar to replace (1-based, inclusive)"),
                    "to_bar", intProp("Last bar to replace (1-based, inclusive)"),
                    "notes", strProp("Replacement note sequence")),
                List.of("voice", "from_bar", "to_bar", "notes")),
            (ctx, args) -> VoiceOpTools.replaceRange(ctx, str(args, "voice"),
                args.get("from_bar").asInt(), args.get("to_bar").asInt(), str(args, "notes")));
    }

    private static McpTool voiceReplaceNoteTool(final CompositionContextProvider provider) {
        return tool(provider, "voice.replace_note",
            "Surgically replace a single note in a bar. " +
                "Finds the first note in the given bar whose pitch matches 'old', replaces it with 'new'. " +
                "old and new are full note tokens including duration, e.g. 'C4/q' or 'F#4/e'. " +
                "Use query.voice to inspect bar content before calling this.",
            buildObjectSchema(
                Map.of("voice", strProp("Voice name"),
                    "bar", intProp("Bar number (1-based)"),
                    "old", strProp("Note to find by pitch, e.g. 'C4/q'"),
                    "new", strProp("Replacement note(s), e.g. 'D4/q'")),
                List.of("voice", "bar", "old", "new")),
            (ctx, args) -> VoiceOpTools.replaceNote(ctx, str(args, "voice"),
                args.get("bar").asInt(), str(args, "old"), str(args, "new")));
    }

    private static McpTool voiceMeasureCountTool(final CompositionContextProvider provider) {
        return tool(provider, "voice.measure_count",
            "Return the number of complete bars in a voice. " +
                "Also shown in voice.list — use this when you only need the count for one voice.",
            McpTools.schema(MAPPER,
                Map.of("voice", "Voice name"),
                List.of("voice")),
            (ctx, args) -> VoiceOpTools.measureCount(ctx, str(args, "voice")));
    }

    private static McpTool formCreateSectionTool(final CompositionContextProvider provider) {
        return tool(provider, "form.create_section",
            "Create a named section from the current voices (or specified voices). " +
                "Sections are the building blocks of formal plans: A, B, Verse, Chorus, etc. " +
                "measures is optional — if omitted, inferred from the longest provided voice. " +
                "Explicit measures overrides inference; a warning is returned if they disagree. " +
                "voice_names: optional comma-separated list; uses all voices if omitted.",
            buildObjectSchema(
                Map.of("name", strProp("Section name, e.g. 'A', 'B', 'Verse', 'Chorus'"),
                    "measures", intProp("How many measures this section spans (optional — inferred from voices if omitted)"),
                    "voice_names", strProp("Comma-separated voice names (optional, defaults to all)")),
                List.of("name")),
            (ctx, args) -> FormTools.createSection(ctx, str(args, "name"),
                optStr(args, "voice_names"), optInt(args, "measures")));
    }

    private static McpTool formRepeatSectionTool(final CompositionContextProvider provider) {
        return tool(provider, "form.repeat_section",
            "Repeat a previously defined section in the formal plan. " +
                "Creates ABA, AABA, rondo forms, etc. " +
                "new_label: optional display label (e.g. 'A\\'' for the return of A).",
            buildObjectSchema(
                Map.of("section", strProp("Name of section to repeat (must exist)"),
                    "new_label", strProp("Optional display label for this occurrence")),
                List.of("section")),
            (ctx, args) -> FormTools.repeatSection(ctx, str(args, "section"), optStr(args, "new_label")));
    }

    private static McpTool formSetEndingTool(final CompositionContextProvider provider) {
        return tool(provider, "form.set_ending",
            "Register a per-pass ending for a section — standard 1st/2nd ending volta brackets. " +
                "Create the ending bars as a separate section first (e.g. 'A_end_1', 'A_end_2'), " +
                "then call form.set_ending to attach them. " +
                "The ending section's measure count determines how many tail bars are replaced. " +
                "Example: 8-bar section A with 1-bar endings → bar 8 differs between pass 1 and pass 2. " +
                "form.build will render \\repeat volta brackets in LilyPond and correct MIDI per pass.",
            buildObjectSchema(
                Map.of(
                    "section", strProp("Section to attach the ending to"),
                    "pass", intProp("Which repetition this ending plays on (1-based)"),
                    "ending_section", strProp("Name of a previously created section providing the ending bars")
                ),
                List.of("section", "pass", "ending_section")),
            (ctx, args) -> FormTools.setEnding(ctx, str(args, "section"),
                args.get("pass").asInt(), str(args, "ending_section")));
    }

    private static McpTool formBuildTool(final CompositionContextProvider provider) {
        return tool(provider, "form.build",
            "Assemble the score from the formal plan. " +
                "Concatenates sections in order, filling missing voices with rests. " +
                "Creates assembled voices in the composition context.",
            emptySchema(),
            (ctx, args) -> FormTools.buildScore(ctx));
    }

    private static McpTool formDescribeTool(final CompositionContextProvider provider) {
        return tool(provider, "form.describe",
            "Show the current formal plan structure and section count.",
            emptySchema(),
            (ctx, args) -> FormTools.describeForm(ctx));
    }

    private static McpTool rulesCheckTool(final CompositionContextProvider provider) {
        return tool(provider, "rules.check",
            "Run composition rules against all voices and report any violations. " +
                "Checks voice leading (large leaps, repetition) and meter consistency. " +
                "Violations are advisory — they do not prevent composition.",
            emptySchema(),
            (ctx, args) -> RulesTools.check(ctx));
    }

    private static McpTool rulesCheckRangeTool(final CompositionContextProvider provider) {
        return tool(provider, "rules.check_range",
            "Check whether a voice fits within an instrument's written range. " +
                "Reports notes that are too high, too low, or in extreme registers. " +
                "Instrument names: Piano, Flute, Violin, Cello, Trumpet, French Horn, Clarinet, etc.",
            buildObjectSchema(
                Map.of("voice", strProp("Voice to check"),
                    "instrument", strProp("Instrument name, e.g. 'Flute', 'Violin'")),
                List.of("voice", "instrument")),
            (ctx, args) -> RulesTools.checkRange(ctx, str(args, "voice"), str(args, "instrument")));
    }

    private static McpTool instrumentInfoTool() {
        return toolNoCtx(
            "instrument.info",
            "Get information about an instrument: range, family, MIDI program, clef. " +
                "Instrument names: Piano, Flute, Oboe, Clarinet, Bassoon, Trumpet, French Horn, " +
                "Trombone, Tuba, Violin, Viola, Cello, Double Bass, Harp, Organ, etc.",
            McpTools.schema(MAPPER,
                Map.of("instrument", "Instrument name, e.g. 'Flute', 'Violin', 'Piano'"),
                List.of("instrument")),
            args -> InstrumentTools.info(str(args, "instrument"))
        );
    }

    private static McpTool drumsPresetTool(final CompositionContextProvider provider) {
        return tool(provider, "drums.preset",
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
                "afrohouse (syncopated kick on 1/and-of-2/3, open hi-hat accents, conga layer, maracas eighths).",
            McpTools.schema(MAPPER,
                Map.of(
                    "preset", "Preset name: house_4on4, rock_8th, rock_basic, bossa_nova, waltz, waltz_jazz, swing, afrohouse",
                    "bars", "Number of bars to generate (1-64)"
                ),
                List.of("preset", "bars")),
            (ctx, args) -> DrumPresets.loadPreset(ctx, str(args, "preset"), args.get("bars").asInt()));
    }

    private static McpTool scoreSetSwingTool(final CompositionContextProvider provider) {
        return tool(provider, "score.set_swing",
            "Enable swing quantization for MIDI playback. " +
                "Swing delays off-beat eighth notes to create the jazz/funk/blues lilt. " +
                "Standard jazz swing: ratio=2/3 (long-short 2:1). Light shuffle: 3/5. " +
                "Only affects eighth notes; quarter notes, dotted values, and triplets are unchanged. " +
                "Set ratio to '0' to disable swing and return to straight timing. " +
                "Example: score.set_swing 2/3 → jazz standard swing feel.",
            McpTools.schema(MAPPER,
                Map.of("ratio", "Swing ratio between 0.5 and 1.0 — e.g. '2/3' (standard), '3/5' (light), '0' to disable"),
                List.of("ratio")),
            (ctx, args) -> ScoreTools.setSwing(ctx, str(args, "ratio")));
    }

    private static McpTool scoreSetTempoChangeTool(final CompositionContextProvider provider) {
        return tool(provider, "score.set_tempo_change",
            "Declare a gradual tempo change (ritardando or accelerando) across a bar span. " +
                "The change starts at start_bar and reaches to_bpm by end_bar. " +
                "Affects both MIDI playback (interpolated tempo events per bar) and LilyPond notation (rit./accel. mark). " +
                "curve: 'linear' = equal BPM steps; 'exponential' = change accelerates toward end (more natural for rit.). " +
                "Example: start_bar=22 end_bar=24 to_bpm=60 curve=exponential for a 3-bar ritardando.",
            buildObjectSchema(
                Map.of(
                    "start_bar", intProp("First bar of the tempo change (1-based)"),
                    "end_bar", intProp("Last bar — target tempo reached here"),
                    "to_bpm", intProp("Target BPM at end_bar"),
                    "curve", strProp("Interpolation curve: 'linear' (default) or 'exponential'")
                ),
                List.of("start_bar", "end_bar", "to_bpm")),
            (ctx, args) -> ScoreTools.setTempoChange(ctx,
                args.get("start_bar").asInt(), args.get("end_bar").asInt(),
                args.get("to_bpm").asInt(), optStr(args, "curve")));
    }

    private static McpTool scoreSaveTool(final CompositionContextProvider provider) {
        return tool(provider, "score.save",
            "Save the current composition to a JSON snapshot file under generated_tracks/. " +
                "The snapshot captures all voices, motifs, metadata, key, swing, bar chords, and tempo changes. " +
                "Use score.load to restore the session later. " +
                "Example: score.save filename='my_song' — writes generated_tracks/my_song.json",
            buildObjectSchema(
                Map.of("filename", strProp("Filename (without path) to save, e.g. 'my_song' or 'my_song.json'")),
                List.of("filename")),
            (ctx, args) -> SaveLoadTools.save(ctx, str(args, "filename")));
    }

    private static McpTool scoreLoadTool(final CompositionContextProvider provider) {
        return tool(provider, "score.load",
            "Load a composition from a JSON snapshot file under generated_tracks/. " +
                "Replaces all current state with the saved session. " +
                "Example: score.load filename='my_song' — reads generated_tracks/my_song.json",
            buildObjectSchema(
                Map.of("filename", strProp("Filename (without path) to load, e.g. 'my_song' or 'my_song.json'")),
                List.of("filename")),
            (ctx, args) -> SaveLoadTools.load(ctx, str(args, "filename")));
    }

    private static McpTool scoreLoadMidiTool(final CompositionContextProvider provider) {
        return tool(provider, "score.load_midi",
            "Import a MIDI file as voices into the current composition. " +
                "Creates one voice per non-empty MIDI track, named 'voice-0', 'voice-1', etc. " +
                "Also sets the tempo from the MIDI file. " +
                "Existing voices are not cleared — call score.clear first if you want a fresh start. " +
                "Note: MIDI loses enharmonic spelling; all black keys come back as sharps (C#, F#, etc.). " +
                "path can be absolute or relative to the server's working directory.",
            buildObjectSchema(
                Map.of("path", strProp("Path to the MIDI file, e.g. 'generated_tracks/1_my_song/my_song.mid'")),
                List.of("path")),
            (ctx, args) -> SaveLoadTools.loadMidi(ctx, str(args, "path")));
    }

    // --- Tool plumbing ---

    @FunctionalInterface
    private interface ContextToolHandler {
        ToolResult handle(CompositionContext ctx, JsonNode args) throws Exception;
    }

    @FunctionalInterface
    private interface NoContextToolHandler {
        ToolResult handle(JsonNode args) throws Exception;
    }

    private static McpTool tool(final CompositionContextProvider provider,
                                final String name,
                                final String description,
                                final ObjectNode schema,
                                final ContextToolHandler handler) {
        return new McpTool() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return description;
            }

            @Override
            public ObjectNode inputSchema() {
                return schema;
            }

            @Override
            public McpToolResult call(final JsonNode arguments) {
                try {
                    final JsonNode args = arguments != null ? arguments : MAPPER.createObjectNode();
                    final CompositionContext ctx = provider.get();
                    final ToolResult result = ScopedValue.where(MusicCodeModel.CURRENT, ctx.codeModel())
                        .call(() -> handler.handle(ctx, args));
                    return toMcpResult(result);
                } catch (final IllegalArgumentException e) {
                    return McpToolResult.error(e.getMessage());
                } catch (final Exception e) {
                    return McpToolResult.error("Unexpected error: " + e.getMessage());
                }
            }
        };
    }

    private static McpTool toolNoCtx(final String name,
                                     final String description,
                                     final ObjectNode schema,
                                     final NoContextToolHandler handler) {
        return new McpTool() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return description;
            }

            @Override
            public ObjectNode inputSchema() {
                return schema;
            }

            @Override
            public McpToolResult call(final JsonNode arguments) {
                try {
                    final JsonNode args = arguments != null ? arguments : MAPPER.createObjectNode();
                    final ToolResult result = handler.handle(args);
                    return toMcpResult(result);
                } catch (final IllegalArgumentException e) {
                    return McpToolResult.error(e.getMessage());
                } catch (final Exception e) {
                    return McpToolResult.error("Unexpected error: " + e.getMessage());
                }
            }
        };
    }

    private static McpToolResult toMcpResult(final ToolResult result) {
        if (!result.success()) {
            return McpToolResult.error(result.message());
        }
        final String text = result.data() != null
            ? result.message() + "\n\n" + result.data()
            : result.message();
        if (result.artifacts().isEmpty()) {
            return McpToolResult.text(text);
        }
        final List<McpContent.Resource> resources = result.artifacts().stream()
            .map(a -> new McpContent.Resource(a.name(), a.mimeType(),
                Base64.getEncoder().encodeToString(a.data())))
            .toList();
        return McpToolResult.withResources(text, resources);
    }

    // --- Schema builders ---

    private static ObjectNode buildObjectSchema(final Map<String, ObjectNode> properties,
                                                final List<String> required) {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = MAPPER.createObjectNode();
        properties.forEach(props::set);
        schema.set("properties", props);
        final var requiredArray = MAPPER.createArrayNode();
        required.forEach(requiredArray::add);
        schema.set("required", requiredArray);
        return schema;
    }

    private static ObjectNode emptySchema() {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        schema.set("properties", MAPPER.createObjectNode());
        return schema;
    }

    private static ObjectNode strProp(final String description) {
        final ObjectNode node = MAPPER.createObjectNode();
        node.put("type", "string");
        node.put("description", description);
        return node;
    }

    private static ObjectNode intProp(final String description) {
        final ObjectNode node = MAPPER.createObjectNode();
        node.put("type", "integer");
        node.put("description", description);
        return node;
    }

    private static ObjectNode enumProp(final String description, final List<String> values) {
        final ObjectNode node = MAPPER.createObjectNode();
        node.put("type", "string");
        node.put("description", description);
        final var enumArray = MAPPER.createArrayNode();
        values.forEach(enumArray::add);
        node.set("enum", enumArray);
        return node;
    }

    // --- Arg extractors ---

    private static String str(final JsonNode args, final String key) {
        final JsonNode node = args.get(key);
        if (node == null || node.isNull()) {
            throw new IllegalArgumentException("Missing required argument '" + key + "'.");
        }
        return node.asText();
    }

    private static String optStr(final JsonNode args, final String key) {
        final JsonNode node = args.get(key);
        return (node == null || node.isNull() || node.asText().isBlank()) ? null : node.asText();
    }

    private static Integer optInt(final JsonNode args, final String key) {
        final JsonNode node = args.get(key);
        return (node == null || node.isNull()) ? null : node.asInt();
    }
}
