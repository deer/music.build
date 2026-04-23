package build.music.server.console;

public record LogDisplayEntry(String text, boolean ok, boolean separator) {

    public static LogDisplayEntry parse(final String encoded) {
        if (encoded.startsWith("sep\t")) {
            return new LogDisplayEntry(encoded.substring(4), true, true);
        }
        if (encoded.startsWith("ok\t")) {
            return new LogDisplayEntry(encoded.substring(3), true, false);
        }
        if (encoded.startsWith("err\t")) {
            return new LogDisplayEntry(encoded.substring(4), false, false);
        }
        return new LogDisplayEntry(encoded, true, false);
    }
}
