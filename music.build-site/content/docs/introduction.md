---
title: Introduction
description: "music.build is an MCP server that lets AI agents compose music as typed, immutable data."
ai-summary: "Overview of music.build: what it is, how it works, and the core concepts of typed music composition. Covers the note-to-score data model, the 55-tool MCP surface, and the two export targets (MIDI and LilyPond PDF). Read this before the quickstart."
ai-keywords: [
  music.build,
  MCP,
  AI composition,
  typed music,
  immutable,
  MIDI,
  LilyPond,
  Java,
]
---

# Introduction

music.build is an MCP server that lets AI agents compose music by building
typed, immutable structures — notes, voices, harmony, and formal sections — and
exporting them to MIDI and LilyPond notation.

There is no piano roll, no GUI, and no drag-and-drop. Compositions emerge
entirely from tool calls.

## Why typed music data?

Music is hierarchical: a note belongs to a phrase, a phrase to a section, a
section to a piece. When those relationships are explicit and typed, an AI agent
can reason about them — transposing a voice, checking voice leading, assembling
an AABA form — without ever losing precision.

In music.build, every value is exact:

- Durations are rational fractions (`3/8`, not `0.375`) — no floating-point
  drift
- Pitches carry their spelling (`F#4` is distinct from `Gb4`) — enharmonics are
  preserved
- Transforms return new values; originals are untouched — no side effects

This makes compositions replayable, queryable, and structurally editable at any
point in the session.

## How it works

The server exposes **55 MCP tools** across seven areas:

| Area       | What it does                                                             |
| ---------- | ------------------------------------------------------------------------ |
| Voices     | Build and edit named note sequences                                      |
| Harmony    | Set keys, chord progressions, generate walking bass and comp voices      |
| Transforms | Transpose, invert, retrograde, augment — all produce new voices          |
| Form       | Define sections, repeats, and volta endings; assemble into a final score |
| Drums      | Load named drum patterns onto MIDI channel 9                             |
| Rules      | Check voice leading, meter, and instrument ranges                        |
| Export     | Write MIDI, LilyPond PDF, and a session log to disk                      |

An AI agent connects to the server, builds up a composition voice by voice, and
calls `export.all` when done. The output lands in a numbered folder under
`generated_tracks/`.

## The data model

The central unit is a **Voice** — an ordered sequence of note events. Events are
typed: `Note`, `Rest`, or `Chord`. Voices accumulate in a **CompositionContext**
(one per server process), along with instrument assignments, dynamics, and form
state.

When the agent calls export, the context assembles a **Score** — the complete
composition — and hands it to the MIDI and LilyPond renderers.

```
Note / Rest / Chord   ← note events
       ↓
     Voice            ← named sequence of events
       ↓
  CompositionContext  ← session state (voices, parts, form)
       ↓
     Score            ← assembled composition
       ↓
  MIDI + LilyPond     ← output files
```

## What agents can compose

With music.build connected to Claude or another MCP-capable agent, the agent
can:

- Build note sequences with full pitch spelling, rhythm, and articulation
- Layer voices into a score and assign GM instruments
- Generate walking bass lines, chord voicings, and diatonic harmonizations
- Assemble multi-section AABA or verse-chorus forms with volta endings
- Check voice leading and instrument range rules before export
- Export to MIDI and, if LilyPond is installed, engraved PDF sheet music
