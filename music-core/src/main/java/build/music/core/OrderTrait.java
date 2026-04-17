package build.music.core;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Carries a sequence of integer indices recording the ordered arrangement of non-singular traits
 * within a Traitable. Used by {@link Chord} to preserve the sounding order of its pitches:
 * {@code indices.get(i)} is the i-th pitch's position in the chord's trait list.
 *
 * <p>Singular: a chord has exactly one pitch ordering.
 */
@Singular
public record OrderTrait(List<Integer> indices) implements Trait {

    @Unmarshal
    public OrderTrait(final Stream<Integer> indices) {
        this(indices.toList());
    }

    public OrderTrait {
        indices = List.copyOf(Objects.requireNonNull(indices, "indices must not be null"));
    }

    @Marshal
    public void destructor(final Out<Stream<Integer>> indices) {
        indices.set(this.indices.stream());
    }

    public static OrderTrait of(final List<Integer> indices) {
        return new OrderTrait(indices);
    }

    public static OrderTrait ascending(final int size) {
        final List<Integer> idx = new java.util.ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            idx.add(i);
        }
        return new OrderTrait(idx);
    }

    static {
        Marshalling.register(OrderTrait.class, MethodHandles.lookup());
    }
}
