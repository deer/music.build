package build.music.mcp;

import java.util.List;

/**
 * Standardized result type for MCP tool outputs.
 * {@code message} is the agent-facing response text.
 * {@code data} carries structured text content when present (e.g. LilyPond source).
 * {@code artifacts} carries binary outputs when {@link ExportOptions#returnBytes()} is true.
 */
public record ToolResult(boolean success, String message, String data, List<ExportArtifact> artifacts) {

    public static ToolResult success(final String message) {
        return new ToolResult(true, message, null, List.of());
    }

    public static ToolResult success(final String message, final String data) {
        return new ToolResult(true, message, data, List.of());
    }

    public static ToolResult success(final String message, final List<ExportArtifact> artifacts) {
        return new ToolResult(true, message, null, artifacts);
    }

    public static ToolResult error(final String message) {
        return new ToolResult(false, message, null, List.of());
    }
}
