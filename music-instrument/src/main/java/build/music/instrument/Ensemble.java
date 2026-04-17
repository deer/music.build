package build.music.instrument;

import build.base.marshalling.Bound;
import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.AbstractTraitable;
import build.codemodel.foundation.descriptor.Trait;
import build.music.pitch.typesystem.MusicCodeModel;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A named group of instruments.
 */
public final class Ensemble
    extends AbstractTraitable {

    private final List<Instrument> instruments;

    private Ensemble(final MusicCodeModel codeModel, final List<Instrument> instruments) {
        super(codeModel);
        this.instruments = instruments;
    }

    @Unmarshal
    public Ensemble(@Bound final MusicCodeModel codeModel,
                    final Marshaller marshaller,
                    final Stream<Marshalled<Trait>> traits,
                    final Stream<Marshalled<Instrument>> instruments) {
        super(codeModel, marshaller, traits);
        this.instruments = instruments.map(marshaller::unmarshal).toList();
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<Stream<Marshalled<Trait>>> traits,
                           final Out<Stream<Marshalled<Instrument>>> instruments) {
        super.destructor(marshaller, traits);
        instruments.set(this.instruments.stream().map(marshaller::marshal));
    }

    public static Ensemble of(final String name, final List<Instrument> instruments) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(instruments, "instruments must not be null");
        final List<Instrument> immutable = List.copyOf(instruments);
        final Ensemble e = new Ensemble(MusicCodeModel.current(), immutable);
        e.addTrait(EnsembleNameTrait.of(name));
        return e;
    }

    // ── accessors ────────────────────────────────────────────────────────────

    public String name() {
        return getTrait(EnsembleNameTrait.class).orElseThrow().name();
    }

    public List<Instrument> instruments() {
        return instruments;
    }

    static {
        Marshalling.register(Ensemble.class, MethodHandles.lookup());
    }

    // ── catalog constants ─────────────────────────────────────────────────────

    public static final Ensemble STRING_QUARTET = Ensemble.of("String Quartet",
        List.of(Instruments.VIOLIN, Instruments.VIOLIN, Instruments.VIOLA, Instruments.CELLO));

    public static final Ensemble WOODWIND_QUINTET = Ensemble.of("Woodwind Quintet",
        List.of(Instruments.FLUTE, Instruments.OBOE, Instruments.CLARINET,
            Instruments.FRENCH_HORN, Instruments.BASSOON));

    public static final Ensemble BRASS_QUINTET = Ensemble.of("Brass Quintet",
        List.of(Instruments.TRUMPET, Instruments.TRUMPET, Instruments.FRENCH_HORN,
            Instruments.TROMBONE, Instruments.TUBA));

    public static final Ensemble PIANO_TRIO = Ensemble.of("Piano Trio",
        List.of(Instruments.PIANO, Instruments.VIOLIN, Instruments.CELLO));

    public static final Ensemble STRING_ORCHESTRA = Ensemble.of("String Orchestra",
        List.of(Instruments.VIOLIN, Instruments.VIOLIN, Instruments.VIOLA,
            Instruments.CELLO, Instruments.DOUBLE_BASS));
}
