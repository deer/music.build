package build.music.mcp;

import build.base.json.JsonValue;
import build.music.pitch.typesystem.MusicCodeModel;
import build.serve.mcp.McpContent;
import build.serve.mcp.McpResourceContent;
import build.serve.mcp.McpToolAnnotations;
import build.serve.mcp.McpToolResult;
import build.serve.mcp.ToolDef;
import build.serve.mcp.ToolParam;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Adapter that bridges {@link ToolDef} (which produces {@link McpToolResult}) to music domain handlers
 * (which produce {@link ToolResult}), injecting {@link CompositionContext} and the
 * {@link MusicCodeModel} ScopedValue.
 */
public final class MusicToolDef {

    private MusicToolDef() {
    }

    @FunctionalInterface
    public interface ContextHandler {
        ToolResult handle(CompositionContext ctx, JsonValue args) throws Exception;
    }

    @FunctionalInterface
    public interface SimpleHandler {
        ToolResult handle(JsonValue args) throws Exception;
    }

    public static ContextBuilder of(final CompositionContextProvider provider,
                                    final String name,
                                    final String description) {
        return new ContextBuilder(provider, name, description);
    }

    public static SimpleBuilder ofNoCtx(final String name, final String description) {
        return new SimpleBuilder(name, description);
    }

    public static final class ContextBuilder {

        private final CompositionContextProvider provider;
        private final String name;
        private final String description;
        private final List<ToolParam<?>> params = new ArrayList<>();
        private McpToolAnnotations annotations = null;

        private ContextBuilder(final CompositionContextProvider provider,
                               final String name,
                               final String description) {
            this.provider = provider;
            this.name = name;
            this.description = description;
        }

        public ContextBuilder param(final ToolParam<?> param) {
            params.add(param);
            return this;
        }

        public ContextBuilder annotations(final McpToolAnnotations annotations) {
            this.annotations = annotations;
            return this;
        }

        public ToolDef handle(final ContextHandler handler) {
            final CompositionContextProvider p = provider;
            final List<ToolParam<?>> paramsCopy = List.copyOf(params);
            final ToolDef.Builder builder = ToolDef.of(name, description);
            paramsCopy.forEach(builder::param);
            builder.annotations(annotations);
            return builder.handle(args -> {
                final CompositionContext ctx = p.get();
                final ToolResult result = ScopedValue.where(MusicCodeModel.CURRENT, ctx.codeModel())
                    .call(() -> handler.handle(ctx, args));
                return toMcpResult(result);
            });
        }
    }

    public static final class SimpleBuilder {

        private final String name;
        private final String description;
        private final List<ToolParam<?>> params = new ArrayList<>();
        private McpToolAnnotations annotations = null;

        private SimpleBuilder(final String name, final String description) {
            this.name = name;
            this.description = description;
        }

        public SimpleBuilder param(final ToolParam<?> param) {
            params.add(param);
            return this;
        }

        public SimpleBuilder annotations(final McpToolAnnotations annotations) {
            this.annotations = annotations;
            return this;
        }

        public ToolDef handle(final SimpleHandler handler) {
            final List<ToolParam<?>> paramsCopy = List.copyOf(params);
            final ToolDef.Builder builder = ToolDef.of(name, description);
            paramsCopy.forEach(builder::param);
            builder.annotations(annotations);
            return builder.handle(args -> toMcpResult(handler.handle(args)));
        }
    }

    static McpToolResult toMcpResult(final ToolResult result) {
        if (!result.success()) {
            return McpToolResult.error(result.message());
        }
        final String text = result.data() != null
            ? result.message() + "\n\n" + result.data()
            : result.message();
        if (result.artifacts().isEmpty()) {
            return McpToolResult.text(text);
        }
        final List<McpContent.Resource> resources = result.artifacts().stream()
            .map(a -> new McpResourceContent.Text(a.name(), a.mimeType(),
                Base64.getEncoder().encodeToString(a.data())))
            .map(McpContent.Resource::new)
            .toList();
        return McpToolResult.withResources(text, resources);
    }
}
