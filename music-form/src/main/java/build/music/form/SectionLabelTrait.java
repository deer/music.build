package build.music.form;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

/**
 * Display label for a {@link Section} as rendered in notation (e.g. "A", "B", "Chorus").
 * Distinct from {@link SectionNameTrait} which is the structural identity key. Singular.
 */
@Singular
public record SectionLabelTrait(String label) implements Trait {

    @Unmarshal
    public SectionLabelTrait {
        Objects.requireNonNull(label, "label must not be null");
    }

    @Marshal
    public void destructor(final Out<String> label) {
        label.set(this.label);
    }

    public static SectionLabelTrait of(final String label) {
        return new SectionLabelTrait(label);
    }

    static {
        Marshalling.register(SectionLabelTrait.class, MethodHandles.lookup());
    }
}
