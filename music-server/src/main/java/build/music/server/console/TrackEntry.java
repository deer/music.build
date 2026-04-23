package build.music.server.console;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public record TrackEntry(
    String name,
    String midiFile,
    String pdfFile,
    String jsonFile,
    String jsonlFile,
    String lyFile,
    String createdAt,
    String midiSize
) {
    public boolean hasMidi() {
        return midiFile != null;
    }

    public boolean hasPdf() {
        return pdfFile != null;
    }

    public boolean hasJson() {
        return jsonFile != null;
    }

    public boolean hasJsonl() {
        return jsonlFile != null;
    }

    public boolean hasLy() {
        return lyFile != null;
    }

    public static TrackEntry from(final Path folder) {
        final String midi = findFile(folder, ".mid");
        return new TrackEntry(
            folder.getFileName().toString(),
            midi,
            findFile(folder, ".pdf"),
            findFile(folder, ".json"),
            findFile(folder, ".jsonl"),
            findFile(folder, ".ly"),
            readCreatedAt(folder),
            readMidiSize(folder, midi)
        );
    }

    private static String readCreatedAt(final Path folder) {
        try {
            final var mtime = Files.getLastModifiedTime(folder).toInstant()
                .atZone(ZoneId.systemDefault());
            return DateTimeFormatter.ofPattern("MMM d, HH:mm").format(mtime);
        } catch (final IOException e) {
            return "";
        }
    }

    private static String readMidiSize(final Path folder, final String midi) {
        if (midi == null) {
            return "";
        }
        try {
            final long bytes = Files.size(folder.resolve(midi));
            return bytes < 1024 ? bytes + " B" : (bytes / 1024) + " KB";
        } catch (final IOException e) {
            return "";
        }
    }

    private static String findFile(final Path folder, final String ext) {
        try (var files = Files.list(folder)) {
            return files
                .filter(p -> p.getFileName().toString().endsWith(ext))
                .map(p -> p.getFileName().toString())
                .findFirst()
                .orElse(null);
        } catch (final IOException e) {
            return null;
        }
    }
}
