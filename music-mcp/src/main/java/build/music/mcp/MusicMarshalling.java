package build.music.mcp;

import build.base.marshalling.Marshaller;
import build.base.marshalling.Marshalling;
import build.base.transport.json.JsonTransport;
import build.codemodel.foundation.naming.NameProvider;
import build.codemodel.foundation.transport.IrreducibleNameTransformer;
import build.codemodel.foundation.transport.ModuleNameTransformer;
import build.codemodel.foundation.transport.NamespaceTransformer;
import build.codemodel.foundation.transport.TypeNameTransformer;

/**
 * Factory methods for the JSON marshalling stack used by
 * {@code score.save} / {@code score.load} and CompositionMarshallingTests.
 */
public final class MusicMarshalling {

    private MusicMarshalling() {
    }

    /**
     * Returns a new {@link JsonTransport} with all four codemodel name transformers registered.
     *
     * @param nameProvider the {@link NameProvider} from the active {@link build.music.pitch.typesystem.MusicCodeModel}
     * @return a configured {@link JsonTransport}
     */
    public static JsonTransport configuredTransport(final NameProvider nameProvider) {
        final JsonTransport transport = new JsonTransport();
        transport.register(new IrreducibleNameTransformer(nameProvider));
        transport.register(new ModuleNameTransformer(nameProvider));
        transport.register(new NamespaceTransformer(nameProvider));
        transport.register(new TypeNameTransformer(nameProvider));
        return transport;
    }

    /**
     * Returns a new {@link Marshaller} from the global {@link build.base.marshalling.SchemaFactory}.
     */
    public static Marshaller newMarshaller() {
        return Marshalling.newMarshaller();
    }
}
