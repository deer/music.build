# music.build

[![CI](https://github.com/deer/music.build/actions/workflows/main-pull-request.yml/badge.svg)](https://github.com/deer/music.build/actions/workflows/main-pull-request.yml)
[![Maven Central](https://img.shields.io/maven-central/v/build.music/music-mcp)](https://central.sonatype.com/artifact/build.music/music-mcp)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

An MCP server that lets AI agents compose music. Built on a typed, immutable music theory library — notes, voices, harmony, form, and transforms are first-class values that agents build up incrementally and export to MIDI and LilyPond notation.

Apache 2.0 · Java 25 · JPMS · Maven

## What it does

Connect music.build to Claude Desktop or any MCP-capable agent. The agent can:

- Build note sequences with full pitch spelling, rhythm, and articulation
- Layer voices into a score, assign instruments, set key and tempo
- Generate walking bass lines, chord voicings, and diatonic harmonizations
- Assemble multi-section forms with volta endings
- Check voice leading and range rules
- Export to MIDI, LilyPond PDF, and a replayable session log

No piano roll. No GUI. The composition emerges from tool calls.

## Setup

**Prerequisites**

- Java 25 (`java -version` should report `25.x`)
- LilyPond 2.24+ (optional — required only for PDF export; MIDI works without it)

**Clone and run**

```bash
git clone https://github.com/deer/music.build
cd music.build
./mvnw exec:java -pl music-server
```

All dependencies (`base.build`, `codemodel.build`, `serve.build`) are on Maven Central — no local installs required.

The MCP server listens on `http://localhost:3000/mcp`. To use a different port:

```bash
PORT=4000 ./mvnw exec:java -pl music-server
```

**Connect to Claude Desktop**

Add to `~/Library/Application Support/Claude/claude_desktop_config.json` (macOS) or `%APPDATA%\Claude\claude_desktop_config.json` (Windows), adjusting the port if you changed it:

```json
{
  "mcpServers": {
    "music": {
      "url": "http://localhost:3000/mcp"
    }
  }
}
```

Restart Claude Desktop. The server must be running before Claude connects — start it first, then open Claude. Ask Claude to compose something.

## Module stack

| Module | What it provides                                             |
|---|--------------------------------------------------------------|
| `music-pitch` | Pitches, intervals, enharmonics, tuning                      |
| `music-time` | Durations, time signatures, tempo, metric positions          |
| `music-core` | Notes, rests, chords, chord symbols, velocity, articulation  |
| `music-transform` | Transpose, invert, retrograde, augment                       |
| `music-score` | Voice, Part, Score — the central exchange types              |
| `music-voice` | Voice operations: slice, concat, pad, merge                  |
| `music-harmony` | Keys, scales, Roman numerals, chord progressions, harmonizer |
| `music-instrument` | Instrument catalog with ranges and GM program numbers        |
| `music-rules` | Voice leading, range, meter, parallel motion checks          |
| `music-form` | Sections, formal plans, volta endings                        |
| `music-midi` | MIDI render and read, General MIDI constants                 |
| `music-lilypond` | LilyPond source generation and PDF engraving                 |
| `music-mcp` | 47 MCP tools, composition context, save/load                 |
| `music-server` | HTTP adapter, MCP dispatch, session event log                |

## Build and test

```bash
./mvnw test                   # all modules
./mvnw test -pl music-mcp     # just MCP layer
```

## Limitations

music.build is designed for **single-user local use**. All composition state lives in a single in-process `CompositionContext` — concurrent requests share the same session. Don't run it as a hosted or multi-user service.

## Design

Music types are built on [`codemodel.build`](https://github.com/deer/codemodel.build) — the same typed attribute system used across the `*.build` family. Each type carries a set of `Trait`s queryable by the framework. Construction uses static factories (`Note.of(...)`, `Voice.of(...)`); spelling and arithmetic stay exact throughout.

- **Exact rational arithmetic** — durations are `Fraction`, never `double`
- **Null-free** — `Optional` for genuinely absent values
- **Immutable** — transforms return new instances
- **Sealed hierarchies** — `NoteEvent permits Note, Rest, Chord`; pattern matching is exhaustive by construction
- **Parse ↔ print** — `toString()` and `parse()` round-trip losslessly for pitches, intervals, durations

## Group ID

`build.music` · `0.1.0-SNAPSHOT`
