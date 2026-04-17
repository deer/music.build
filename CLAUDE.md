# music.build

## Codebase Overview

music.build is a Java 25, JPMS-modular library and MCP server that treats musical composition as typed, immutable, AI-accessible data. It exposes MCP tools allowing AI agents to compose music by building typed structures (notes, voices, harmony, form) and exporting to MIDI and LilyPond notation.

Music value types are `class extends AbstractTraitable` from `codemodel-foundation`. Construction uses `Foo.of(...)` static factories that read a `MusicCodeModel` from `ScopedValue<MusicCodeModel>` (Java 25 preview). Scalars (`Fraction`, `Velocity`, `Tempo`, …) stay as records; enums implement `Trait` directly. Composition state serializes through `codemodel-foundation` marshalling.

**Stack**: Java 25 (preview features enabled for `ScopedValue`), JPMS modules, Maven multi-module, JUnit Jupiter 5.11.3, javax.sound.midi, LilyPond CLI (optional). Sibling project deps: `codemodel.build/codemodel-foundation` (load-bearing post-conversion), `base.build` (marshalling, mereology, query, flow), `serve.build` (MCP transport, htmx console).
**Structure**: 14 modules from `music-pitch`/`music-time` (foundation) up through `music-mcp` (tool surface) and `music-server` (HTTP adapter). See [docs/CODEBASE_MAP.md](docs/CODEBASE_MAP.md) for full architecture.

## Build

Always use `./mvnw` (never bare `mvn`).

```bash
./mvnw test                          # all tests
./mvnw test -pl music-mcp            # just MCP tests
./mvnw exec:java -pl music-server    # run the MCP server (port 3000)
```

## Key Files

- `docs/ROADMAP.md` — **current execution plan — start here for "what's next"**
- `skills/music-composition/SKILL.md` — primary AI agent reference: note DSL, all 47 tools, patterns
- `docs/NOTES.md` — known limitations (live only; fixed items pruned)
- `docs/master-plan.md` — original project thesis and strategic vision (historical; execution plan has diverged, see ROADMAP.md)
- `docs/prompts/` — historical Claude Code prompts that bootstrapped each milestone (do not edit)
- `music-mcp/src/main/java/build/music/mcp/CompositionContext.java` — single mutable session state
- `music-server/src/main/java/build/music/server/MusicMcpServer.java` — all 47 tool registrations

## Architecture Notes

- All MCP tool methods are `static (CompositionContext ctx, ...) -> ToolResult` in `build.music.mcp.tools`
- `CompositionContext` is one instance per server process (no multi-user support)
- `form.build` (MCP tool) replaces voices in-place by name — originals are lost
- `Score` depends on `music-harmony.Key` — these modules cannot be separated
- `Chord` events pass through pitch transforms (invert/transpose) unchanged
- `music-score/Voice` and `music-score/Score` are the central exchange types between all output modules

For detailed architecture, module guide, gotchas, and navigation guide, see [docs/CODEBASE_MAP.md](docs/CODEBASE_MAP.md).
