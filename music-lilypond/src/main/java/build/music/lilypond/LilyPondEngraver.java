package build.music.lilypond;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Invokes LilyPond to produce PDF/PNG from LilyPond source. Requires lilypond on PATH.
 */
public final class LilyPondEngraver {

    private LilyPondEngraver() {
    }

    /**
     * Write LilyPond source to a .ly file and invoke lilypond to produce a PDF.
     *
     * @param source    LilyPond source text
     * @param outputDir directory to write .ly and .pdf files
     * @param baseName  base filename without extension
     * @return Path to the generated PDF
     * @throws IOException if writing or invoking lilypond fails
     */
    public static Path engravePdf(final String source, final Path outputDir, final String baseName) throws IOException {
        final Path lyFile = outputDir.resolve(baseName + ".ly");
        Files.writeString(lyFile, source);

        runLilyPond(outputDir, baseName, lyFile, "--pdf");

        return outputDir.resolve(baseName + ".pdf");
    }

    /**
     * Write LilyPond source to a .ly file and invoke lilypond to produce a PNG.
     *
     * @param source    LilyPond source text
     * @param outputDir directory to write .ly and .png files
     * @param baseName  base filename without extension
     * @return Path to the generated PNG
     * @throws IOException if writing or invoking lilypond fails
     */
    public static Path engravePng(final String source, final Path outputDir, final String baseName) throws IOException {
        final Path lyFile = outputDir.resolve(baseName + ".ly");
        Files.writeString(lyFile, source);

        runLilyPond(outputDir, baseName, lyFile, "--png");

        return outputDir.resolve(baseName + ".png");
    }

    /**
     * Check whether lilypond is installed and available.
     */
    public static boolean isAvailable() {
        return findExecutable() != null;
    }

    /**
     * Find the lilypond executable: tries "lilypond" on PATH first,
     * then common absolute locations as a fallback.
     */
    private static String findExecutable() {
        for (final String candidate : new String[]{"lilypond", "/usr/bin/lilypond", "/usr/local/bin/lilypond"}) {
            try {
                final Process proc = new ProcessBuilder(candidate, "--version")
                    .redirectErrorStream(true)
                    .start();
                proc.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
                if (proc.waitFor() == 0) {
                    return candidate;
                }
            } catch (final IOException | InterruptedException ignored) {
                // try next candidate
            }
        }
        return null;
    }

    private static void runLilyPond(final Path outputDir, final String baseName, final Path lyFile, final String formatFlag)
        throws IOException {
        final String executable = findExecutable();
        if (executable == null) {
            throw new IOException("lilypond not found on PATH or in /usr/bin, /usr/local/bin");
        }
        final String outBase = outputDir.toAbsolutePath().resolve(baseName).toString();
        final ProcessBuilder pb = new ProcessBuilder(
            executable, formatFlag, "-o", outBase, lyFile.toAbsolutePath().toString()
        );
        pb.redirectErrorStream(true);

        final Process proc;
        try {
            proc = pb.start();
        } catch (final IOException e) {
            throw new IOException("Could not start lilypond. Is it installed and on PATH?", e);
        }

        final String output = new String(proc.getInputStream().readAllBytes());

        final int exitCode;
        try {
            exitCode = proc.waitFor();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for lilypond", e);
        }

        if (exitCode != 0) {
            throw new IOException("lilypond exited with code " + exitCode + ":\n" + output);
        }
    }
}
