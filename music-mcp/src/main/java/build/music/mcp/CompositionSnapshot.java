package build.music.mcp;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.base.version.Version;
import build.music.score.Score;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * An immutable snapshot of a {@link CompositionContext} that can be marshalled to JSON
 * and restored to an equivalent context.
 *
 * <p>The snapshot holds a complete {@link Score} (which contains all voice/part data,
 * key, swing, bar chords, tempo changes, etc.) plus a list of named motifs that exist
 * outside the score.
 *
 * <p>Schema version {@value #SCHEMA_VERSION_STRING} — bump this constant when the
 * snapshot format changes in a backwards-incompatible way.
 */
public final class CompositionSnapshot {

    /**
     * Current schema version string.
     */
    public static final String SCHEMA_VERSION_STRING = "2.0.0";

    private final String schemaVersion;
    private final Score score;
    private final List<MotifSnapshot> motifs;

    /**
     * Construct directly (used by {@link CompositionContext#snapshot()}).
     */
    public CompositionSnapshot(final String schemaVersion, final Score score, final List<MotifSnapshot> motifs) {
        this.schemaVersion = Objects.requireNonNull(schemaVersion);
        this.score = Objects.requireNonNull(score);
        this.motifs = List.copyOf(Objects.requireNonNull(motifs));
    }

    @Unmarshal
    public CompositionSnapshot(final Marshaller marshaller,
                               final String schemaVersion,
                               final Marshalled<Score> score,
                               final Stream<Marshalled<MotifSnapshot>> motifs) {
        this.schemaVersion = schemaVersion;
        this.score = marshaller.unmarshal(score);
        this.motifs = motifs.map(marshaller::unmarshal).toList();
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<String> schemaVersion,
                           final Out<Marshalled<Score>> score,
                           final Out<Stream<Marshalled<MotifSnapshot>>> motifs) {
        schemaVersion.set(this.schemaVersion);
        score.set(marshaller.marshal(this.score));
        motifs.set(this.motifs.stream().map(marshaller::marshal));
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public Version schemaVersion() {
        return Version.parse(schemaVersion);
    }

    public Score score() {
        return score;
    }

    public List<MotifSnapshot> motifs() {
        return motifs;
    }

    // ── Object ────────────────────────────────────────────────────────────────

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CompositionSnapshot other)) {
            return false;
        }
        return Objects.equals(schemaVersion, other.schemaVersion)
            && Objects.equals(score, other.score)
            && Objects.equals(motifs, other.motifs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schemaVersion, score, motifs);
    }

    static {
        Marshalling.register(CompositionSnapshot.class, MethodHandles.lookup());
    }
}
