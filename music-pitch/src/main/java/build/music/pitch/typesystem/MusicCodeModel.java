package build.music.pitch.typesystem;

import build.codemodel.foundation.ConceptualCodeModel;
import build.codemodel.foundation.naming.NonCachingNameProvider;

/**
 * The music typesystem registry — a {@link ConceptualCodeModel} pre-configured for music domain types.
 *
 * <p>Bound via {@link #CURRENT} so that factory methods in converted music types can read the ambient
 * {@code MusicCodeModel} without threading it through every constructor call.
 *
 * <p>Lives in {@code music-pitch} (the lowest dependency layer) so all music modules can call
 * {@link #current()} from their {@code of(...)} factories without introducing a circular dependency.
 */
public final class MusicCodeModel extends ConceptualCodeModel {

    /**
     * The ambient {@link ScopedValue} binding. Each MCP request is wrapped in
     * {@code ScopedValue.where(MusicCodeModel.CURRENT, ctx.codeModel()).run(...)}.
     */
    public static final ScopedValue<MusicCodeModel> CURRENT = ScopedValue.newInstance();

    /**
     * Constructs a {@link MusicCodeModel} with a fresh {@link NonCachingNameProvider}.
     * One instance is created per build.music.mcp.CompositionContext.
     */
    public MusicCodeModel() {
        super(new NonCachingNameProvider());
    }

    /**
     * A shared fallback instance used during static initializers and tests where no
     * ScopedValue binding is in scope (e.g., the Instruments catalog constants).
     */
    private static final MusicCodeModel STATIC_FALLBACK = new MusicCodeModel();

    /**
     * Returns the {@link MusicCodeModel} bound to the current thread scope, or a shared
     * fallback instance when called outside any binding (e.g., static initializers).
     *
     * <p>Composition session code is always called within a
     * {@code ScopedValue.where(CURRENT, ctx.codeModel()).run(...)} block and therefore uses
     * the per-session instance. Static catalog constants (e.g., {@code Instruments.PIANO})
     * and test helpers use the fallback.
     */
    public static MusicCodeModel current() {
        return CURRENT.isBound() ? CURRENT.get() : STATIC_FALLBACK;
    }
}
