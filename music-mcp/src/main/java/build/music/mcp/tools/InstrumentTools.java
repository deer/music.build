package build.music.mcp.tools;

import build.music.instrument.Instrument;
import build.music.instrument.Instruments;
import build.music.mcp.ToolResult;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MCP tools for instrument information and suggestions.
 */
public final class InstrumentTools {

    private InstrumentTools() {}

    /** Tool: instrument.info — get information about a named instrument. */
    public static ToolResult info(final String instrumentName) {
        final Optional<Instrument> opt = findInstrument(instrumentName);
        if (opt.isEmpty()) {
            return ToolResult.error("Unknown instrument: '" + instrumentName +
                "'. Try: Piano, Flute, Violin, Cello, Trumpet, French Horn, Clarinet, etc.");
        }
        final Instrument i = opt.get();
        return ToolResult.success(
            "Instrument: " + i.name() + "\n" +
            "Family: " + i.family() + "\n" +
            "Written range: " + i.writtenRange() + " (MIDI " +
                i.writtenRange().low().midi() + "-" + i.writtenRange().high().midi() + ")\n" +
            "Comfortable range: " + i.comfortableRange() + "\n" +
            "MIDI program: " + i.midiProgram() + "\n" +
            "Transposing: " + (i.transposing() ? "yes" : "no") + "\n" +
            "Suggested clef: " + i.clef() + "\n" +
            "Articulations: " + i.availableArticulations().stream()
                .map(Enum::name).collect(Collectors.joining(", "))
        );
    }

    private static Optional<Instrument> findInstrument(final String name) {
        Optional<Instrument> opt = Instruments.byName(name);
        if (opt.isEmpty()) {
            final String lower = name.toLowerCase();
            opt = Instruments.all().stream()
                .filter(i -> i.name().toLowerCase().contains(lower))
                .findFirst();
        }
        return opt;
    }
}
