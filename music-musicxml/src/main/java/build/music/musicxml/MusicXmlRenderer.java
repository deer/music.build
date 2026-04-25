package build.music.musicxml;

import build.music.core.Chord;
import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.core.Rest;
import build.music.harmony.Key;
import build.music.pitch.SpelledPitch;
import build.music.score.Part;
import build.music.score.Score;
import build.music.time.Fraction;
import build.music.time.TimeSignature;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Converts a {@link Score} to MusicXML 4.0 (score-partwise) source.
 *
 * <p>Uses 12 divisions per quarter note, which yields integer division counts for
 * whole/half/quarter/eighth/sixteenth and all their dotted and triplet variants.
 */
public final class MusicXmlRenderer {

    private MusicXmlRenderer() {
    }

    private static final int DIVISIONS_PER_QUARTER = 12;
    private static final int DIVISIONS_PER_WHOLE = DIVISIONS_PER_QUARTER * 4; // 48

    public static String render(final Score score) {
        try {
            final StringWriter out = new StringWriter();
            final XMLStreamWriter xml = XMLOutputFactory.newInstance().createXMLStreamWriter(out);

            xml.writeStartDocument("UTF-8", "1.0");
            xml.writeDTD("\n<!DOCTYPE score-partwise PUBLIC" +
                " \"-//Recordare//DTD MusicXML 4.0 Partwise//EN\"" +
                " \"http://www.musicxml.org/dtds/partwise.dtd\">");
            xml.writeCharacters("\n");

            xml.writeStartElement("score-partwise");
            xml.writeAttribute("version", "4.0");

            elem(xml, "movement-title", score.title());

            final List<Part> parts = score.scoreParts();

            xml.writeStartElement("part-list");
            for (int i = 0; i < parts.size(); i++) {
                writeScorePart(xml, parts.get(i), "P" + (i + 1));
            }
            xml.writeEndElement(); // part-list

            final TimeSignature ts = score.timeSignature();
            final Key key = score.key();

            for (int i = 0; i < parts.size(); i++) {
                xml.writeStartElement("part");
                xml.writeAttribute("id", "P" + (i + 1));
                writeMeasures(xml, parts.get(i).voice().events(), ts, key);
                xml.writeEndElement(); // part
            }

            xml.writeEndElement(); // score-partwise
            xml.writeEndDocument();
            xml.close();

            return out.toString();
        } catch (final XMLStreamException e) {
            throw new IllegalStateException("MusicXML rendering failed", e);
        }
    }

    private static void writeScorePart(final XMLStreamWriter xml,
                                       final Part part,
                                       final String id) throws XMLStreamException {
        xml.writeStartElement("score-part");
        xml.writeAttribute("id", id);
        elem(xml, "part-name", part.name());
        xml.writeStartElement("score-instrument");
        xml.writeAttribute("id", id + "-I1");
        elem(xml, "instrument-name", part.name());
        xml.writeEndElement();
        xml.writeStartElement("midi-instrument");
        xml.writeAttribute("id", id + "-I1");
        elem(xml, "midi-channel", String.valueOf(part.midiChannel() + 1));
        elem(xml, "midi-program", String.valueOf(part.midiProgram() + 1));
        xml.writeEndElement();
        xml.writeEndElement(); // score-part
    }

    private static void writeMeasures(final XMLStreamWriter xml,
                                      final List<NoteEvent> events,
                                      final TimeSignature ts,
                                      final Key key) throws XMLStreamException {
        final List<List<NoteEvent>> measures = splitIntoMeasures(events, ts.measureDuration());
        for (int m = 0; m < measures.size(); m++) {
            xml.writeStartElement("measure");
            xml.writeAttribute("number", String.valueOf(m + 1));
            if (m == 0) {
                writeAttributes(xml, ts, key);
            }
            for (final NoteEvent event : measures.get(m)) {
                writeEvent(xml, event);
            }
            xml.writeEndElement(); // measure
        }
    }

    private static void writeAttributes(final XMLStreamWriter xml,
                                        final TimeSignature ts,
                                        final Key key) throws XMLStreamException {
        xml.writeStartElement("attributes");
        elem(xml, "divisions", String.valueOf(DIVISIONS_PER_QUARTER));
        xml.writeStartElement("key");
        if (key != null) {
            elem(xml, "fifths", String.valueOf(key.signatureAccidentals()));
            elem(xml, "mode", key.minor() ? "minor" : "major");
        } else {
            elem(xml, "fifths", "0");
        }
        xml.writeEndElement(); // key
        xml.writeStartElement("time");
        elem(xml, "beats", String.valueOf(ts.beats()));
        elem(xml, "beat-type", String.valueOf(ts.beatUnit()));
        xml.writeEndElement(); // time
        xml.writeStartElement("clef");
        elem(xml, "sign", "G");
        elem(xml, "line", "2");
        xml.writeEndElement(); // clef
        xml.writeEndElement(); // attributes
    }

    private static void writeEvent(final XMLStreamWriter xml,
                                   final NoteEvent event) throws XMLStreamException {
        switch (event) {
            case Note n -> writeNote(xml, n.pitch().spelled(), n.duration().fraction(), false);
            case Rest r -> writeRest(xml, r.duration().fraction());
            case Chord c -> {
                boolean first = true;
                for (final var pitch : c.pitches()) {
                    if (pitch instanceof SpelledPitch sp) {
                        writeNote(xml, sp, c.duration().fraction(), !first);
                        first = false;
                    }
                }
            }
        }
    }

    private static void writeNote(final XMLStreamWriter xml,
                                  final SpelledPitch pitch,
                                  final Fraction duration,
                                  final boolean chord) throws XMLStreamException {
        final int divs = toDivisions(duration);
        final String type = noteType(duration);
        final boolean dotted = isDotted(duration);
        final boolean triplet = isTriplet(duration);

        xml.writeStartElement("note");
        if (chord) {
            xml.writeEmptyElement("chord");
        }
        xml.writeStartElement("pitch");
        elem(xml, "step", pitch.name().name());
        final int alter = pitch.accidental().semitoneOffset();
        if (alter != 0) {
            elem(xml, "alter", String.valueOf(alter));
        }
        elem(xml, "octave", String.valueOf(pitch.octave()));
        xml.writeEndElement(); // pitch
        elem(xml, "duration", String.valueOf(divs));
        if (type != null) {
            elem(xml, "type", type);
            if (dotted) {
                xml.writeEmptyElement("dot");
            }
            if (triplet) {
                xml.writeStartElement("time-modification");
                elem(xml, "actual-notes", "3");
                elem(xml, "normal-notes", "2");
                xml.writeEndElement();
            }
        }
        xml.writeEndElement(); // note
    }

    private static void writeRest(final XMLStreamWriter xml,
                                  final Fraction duration) throws XMLStreamException {
        final int divs = toDivisions(duration);
        final String type = noteType(duration);
        final boolean dotted = isDotted(duration);

        xml.writeStartElement("note");
        xml.writeEmptyElement("rest");
        elem(xml, "duration", String.valueOf(divs));
        if (type != null) {
            elem(xml, "type", type);
            if (dotted) {
                xml.writeEmptyElement("dot");
            }
        }
        xml.writeEndElement(); // note
    }

    // --- Duration helpers ---

    private static int toDivisions(final Fraction f) {
        return (f.numerator() * DIVISIONS_PER_WHOLE) / f.denominator();
    }

    private static String noteType(final Fraction f) {
        final int n = f.numerator();
        final int d = f.denominator();
        if (n == 1 && d == 1) {
            return "whole";
        }
        if (n == 1 && d == 2) {
            return "half";
        }
        if (n == 3 && d == 4) {
            return "half";       // dotted half
        }
        if (n == 1 && d == 4) {
            return "quarter";
        }
        if (n == 3 && d == 8) {
            return "quarter";    // dotted quarter
        }
        if (n == 1 && d == 6) {
            return "quarter";    // quarter triplet
        }
        if (n == 1 && d == 8) {
            return "eighth";
        }
        if (n == 3 && d == 16) {
            return "eighth";     // dotted eighth
        }
        if (n == 1 && d == 12) {
            return "eighth";     // eighth triplet
        }
        if (n == 1 && d == 16) {
            return "16th";
        }
        if (n == 3 && d == 32) {
            return "16th";       // dotted 16th
        }
        if (n == 1 && d == 24) {
            return "16th";       // 16th triplet
        }
        if (n == 1 && d == 32) {
            return "32nd";
        }
        if (n == 1 && d == 64) {
            return "64th";
        }
        return null;
    }

    private static boolean isDotted(final Fraction f) {
        return f.numerator() == 3 && (f.denominator() == 4 || f.denominator() == 8
            || f.denominator() == 16 || f.denominator() == 32);
    }

    private static boolean isTriplet(final Fraction f) {
        return f.numerator() == 1 && (f.denominator() == 6 || f.denominator() == 12
            || f.denominator() == 24);
    }

    // --- Measure splitting ---

    private static List<List<NoteEvent>> splitIntoMeasures(final List<NoteEvent> events,
                                                           final Fraction measureDur) {
        final List<List<NoteEvent>> result = new ArrayList<>();
        List<NoteEvent> current = new ArrayList<>();
        Fraction accumulated = Fraction.ZERO;

        for (final NoteEvent event : events) {
            current.add(event);
            accumulated = accumulated.add(event.duration().fraction());
            if (accumulated.compareTo(measureDur) >= 0) {
                result.add(current);
                current = new ArrayList<>();
                accumulated = accumulated.subtract(measureDur);
            }
        }
        if (!current.isEmpty()) {
            result.add(current);
        }
        if (result.isEmpty()) {
            result.add(new ArrayList<>());
        }
        return result;
    }

    // --- XML helper ---

    private static void elem(final XMLStreamWriter xml,
                             final String tag,
                             final String text) throws XMLStreamException {
        xml.writeStartElement(tag);
        xml.writeCharacters(text);
        xml.writeEndElement();
    }
}
