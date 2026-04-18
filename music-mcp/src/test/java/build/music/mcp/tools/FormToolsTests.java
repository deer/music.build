package build.music.mcp.tools;

import build.music.mcp.CompositionContext;
import build.music.mcp.ToolResult;
import build.music.score.Score;
import build.music.score.StructuredVoice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FormTools MCP tool behaviour, focusing on form.set_ending correctness.
 */
class FormToolsTests {

    private CompositionContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new CompositionContext();
        ScoreTools.setMetadata(ctx, "Test", 120, "4/4");
    }

    /**
     * Bug: After form.build, ctx.buildScore() must include StructuredVoices so the LilyPond
     * renderer can emit \repeat volta / \alternative notation.
     */
    @Test
    void formBuild_withEnding_propagatesStructuredVoicesToContext() {
        // Compose an 8-bar main section with a 1-bar ending
        VoiceTools.createVoice(ctx, "melody", "C4/q C4/q C4/q C4/q " +
            "C4/q C4/q C4/q C4/q " +
            "C4/q C4/q C4/q C4/q " +
            "C4/q C4/q C4/q C4/q " +
            "C4/q C4/q C4/q C4/q " +
            "C4/q C4/q C4/q C4/q " +
            "C4/q C4/q C4/q C4/q " +
            "C4/q C4/q C4/q C4/q");  // 32 quarters = 8 bars of 4/4

        // Create ending (1 bar)
        VoiceTools.createVoice(ctx, "melody", "D4/q D4/q D4/q D4/q");
        FormTools.createSection(ctx, "A_end", "melody", 1);

        // Re-create melody as the 8-bar main body
        VoiceTools.createVoice(ctx, "melody", "C4/q C4/q C4/q C4/q " +
            "C4/q C4/q C4/q C4/q " +
            "C4/q C4/q C4/q C4/q " +
            "C4/q C4/q C4/q C4/q " +
            "C4/q C4/q C4/q C4/q " +
            "C4/q C4/q C4/q C4/q " +
            "C4/q C4/q C4/q C4/q " +
            "C4/q C4/q C4/q C4/q");
        FormTools.createSection(ctx, "A", "melody", 8);

        FormTools.repeatSection(ctx, "A", null);
        FormTools.setEnding(ctx, "A", 1, "A_end");

        ToolResult result = FormTools.buildScore(ctx);
        assertTrue(result.success(), result.message());

        Score score = ctx.buildScore();
        assertFalse(score.structuredVoices().isEmpty(),
            "form.build must propagate StructuredVoices so LilyPond can emit \\repeat volta");
    }

    /**
     * Bug: After form.build, ctx.barChords must reflect each section's chords
     * at the correct absolute bar positions (not just the last harmony.set_bars call).
     */
    @Test
    void formBuild_mergesPerSectionBarChordsWithBarOffset() {
        // Section A: 2 bars, Cm7 at bar 1, F7 at bar 2
        VoiceTools.createVoice(ctx, "melody", "C4/h C4/h C4/h C4/h"); // 2 bars of 4/4
        HarmonyTools.setBarChords(ctx, "1:Cm7 2:F7");
        FormTools.createSection(ctx, "A", "melody", 2);

        // Section B: 2 bars, BbM7 at bar 1, Eb at bar 2
        VoiceTools.createVoice(ctx, "melody", "D4/h D4/h D4/h D4/h");
        HarmonyTools.setBarChords(ctx, "1:Bbmaj7 2:Eb");
        FormTools.createSection(ctx, "B", "melody", 2);

        ToolResult result = FormTools.buildScore(ctx);
        assertTrue(result.success(), result.message());

        var chords = ctx.getBarChords();
        // A section occupies bars 1-2, B section occupies bars 3-4
        assertNotNull(chords.get(1), "bar 1 (A section Cm7) must be present");
        assertNotNull(chords.get(2), "bar 2 (A section F7) must be present");
        assertNotNull(chords.get(3), "bar 3 (B section BbM7, offset from bar 1) must be present");
        assertNotNull(chords.get(4), "bar 4 (B section Eb, offset from bar 2) must be present");

        assertTrue(chords.get(1).toString().contains("Cm"),
            "bar 1 should be Cm7, got: " + chords.get(1));
        assertTrue(chords.get(3).toString().contains("Bb"),
            "bar 3 should be Bbmaj7 (B section offset by 2), got: " + chords.get(3));
    }

    @Test
    void setBarChords_afterCreateSection_stillCaptured() {
        // harmony.set_bars called AFTER form.create_section — must still work
        VoiceTools.createVoice(ctx, "melody", "C4/h C4/h C4/h C4/h");
        FormTools.createSection(ctx, "A", "melody", 2);
        HarmonyTools.setBarChords(ctx, "1:Dm7 2:G7");

        ToolResult result = FormTools.buildScore(ctx);
        assertTrue(result.success(), result.message());

        var chords = ctx.getBarChords();
        assertNotNull(chords.get(1), "bar 1 must be present even when set_bars called after create_section");
        assertTrue(chords.get(1).toString().contains("Dm"),
            "bar 1 should be Dm7, got: " + chords.get(1));
    }
}
