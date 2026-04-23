package build.music.mcp;

/**
 * Controls how export tools handle their output.
 *
 * @param writeToDisk  if true, artifacts are written to the local filesystem (generated_tracks/)
 * @param returnBytes  if true, artifact bytes are returned in the ToolResult for MCP delivery
 */
public record ExportOptions(boolean writeToDisk, boolean returnBytes) {

    /** Write to disk and return bytes — for the local dev server with console UI. */
    public static ExportOptions diskAndBytes() {
        return new ExportOptions(true, true);
    }

    /** Return bytes only — for hosted deployments where disk writes would accumulate. */
    public static ExportOptions bytesOnly() {
        return new ExportOptions(false, true);
    }
}
