package build.music.mcp;

/**
 * Standardized result type for MCP tool outputs.
 * {@code message} is the agent-facing response text.
 * {@code data} carries structured content when present (e.g. LilyPond source).
 */
public record ToolResult(boolean success, String message, String data) {

    public static ToolResult success(final String message) {
        return new ToolResult(true, message, null);
    }

    public static ToolResult success(final String message, final String data) {
        return new ToolResult(true, message, data);
    }

    public static ToolResult error(final String message) {
        return new ToolResult(false, message, null);
    }
}
