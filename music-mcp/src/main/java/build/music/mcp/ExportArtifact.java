package build.music.mcp;

/**
 * A binary artifact produced by an export tool (MIDI, PDF, LilyPond source, etc.).
 *
 * @param name     filename, used as the resource URI in MCP responses (e.g. "composition.mid")
 * @param mimeType MIME type (e.g. "audio/midi", "application/pdf", "text/x-lilypond")
 * @param data     raw bytes
 */
public record ExportArtifact(String name, String mimeType, byte[] data) {
}
