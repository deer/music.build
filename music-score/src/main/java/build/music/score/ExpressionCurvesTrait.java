package build.music.score;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;
import build.music.time.ExpressionCurve;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Ordered list of expression (CC 11) curves for a {@link Voice}.
 * Singular: a voice has at most one expression-curve timeline.
 */
@Singular
public record ExpressionCurvesTrait(List<ExpressionCurve> curves) implements Trait {

    @Unmarshal
    public ExpressionCurvesTrait(final Marshaller marshaller, final Stream<Marshalled<ExpressionCurve>> curves) {
        this(curves.map(marshaller::unmarshal).toList());
    }

    public ExpressionCurvesTrait {
        curves = List.copyOf(Objects.requireNonNull(curves, "curves must not be null"));
    }

    @Marshal
    public void destructor(final Marshaller marshaller, final Out<Stream<Marshalled<ExpressionCurve>>> curves) {
        curves.set(this.curves.stream().map(marshaller::marshal));
    }

    public static ExpressionCurvesTrait of(final List<ExpressionCurve> curves) {
        return new ExpressionCurvesTrait(curves);
    }

    static {
        Marshalling.register(ExpressionCurvesTrait.class, MethodHandles.lookup());
    }
}
