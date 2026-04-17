---
title: Note Syntax
description: The note sequence syntax used by voice.create and voice.append to describe pitches, durations, chords, ties, articulations, and per-note velocity.
ai-summary: "Complete reference for the note sequence DSL used by voice.create and voice.append. Covers pitch names, octave numbers, accidentals, duration codes (w/h/q/e/s and dotted/triplet variants), rests, chord syntax with angle brackets, ties, articulation suffixes (stac/acc/ten/marc/leg), and per-note velocity overrides. Includes runnable examples."
ai-keywords: [
  note syntax,
  DSL,
  pitch,
  duration,
  chord,
  rest,
  tie,
  articulation,
  velocity,
  voice.create,
]
---

# Note Syntax

`voice.create` and `voice.append` accept a note sequence: whitespace-separated
tokens, one per note or chord.

```
C4/q D4/q E4/h
```

Each token is `pitch/duration`, with optional suffixes for ties, articulations,
and per-note velocity.

## Pitches

Standard scientific pitch notation: note name, optional accidental, octave
number.

```
C4    D#3    Bb5    Eb4    F#2
```

Middle C is `C4` (MIDI 60). Octave numbers follow
[Scientific Pitch Notation](https://en.wikipedia.org/wiki/Scientific_pitch_notation).

## Rests

Replace the pitch with `r`:

```
r/q    r/h    r/e    r/w
```

## Duration codes

| Code | Value                               |
| ---- | ----------------------------------- |
| `w`  | whole                               |
| `h`  | half                                |
| `q`  | quarter                             |
| `e`  | eighth                              |
| `s`  | sixteenth                           |
| `dh` | dotted half                         |
| `dq` | dotted quarter                      |
| `de` | dotted eighth                       |
| `h3` | half-note triplet (⅓ whole)         |
| `q3` | quarter-note triplet (⅙ whole)      |
| `e3` | eighth-note triplet (1/12 whole)    |
| `s3` | sixteenth-note triplet (1/24 whole) |

## Chords

Wrap simultaneous pitches in angle brackets, followed by `/duration`:

```
<C4 E4 G4>/q          → C major triad, quarter note
<Eb3 G3 Bb3>/h        → Eb major triad, half note
<D3 F3 A3 C4>/q       → Dm7 chord, quarter note
```

Use chords for pad stabs, gospel organ, jazz voicings, or any simultaneous
pitches.

## Ties

Append `~tie` to extend a note into the next note of the same pitch:

```
C4/h~tie C4/q          → C4 sounds for a dotted half total
G4/q~tie G4/e          → G4 quarter + eighth tied together
```

Ties are useful for durations that cross bar lines or that can't be expressed as
a single rhythmic value.

## Articulations

Append `~code` after the duration:

| Suffix  | Articulation | MIDI effect     |
| ------- | ------------ | --------------- |
| `~stac` | staccato     | 50% duration    |
| `~acc`  | accent       | normal duration |
| `~ten`  | tenuto       | full duration   |
| `~marc` | marcato      | 75% duration    |
| `~leg`  | legato       | full duration   |

```
C4/q~stac D4/q E4/h       → staccato C, normal D and E
<C4 E4 G4>/q~acc           → accented chord stab
```

To apply an articulation to an entire voice at once, use
`voice.set_articulation`.

## Per-note velocity

Append `~vel:N` (0–127) to override the voice-level dynamic for a single note:

```
C4/q~vel:100 D4/q E4/q~vel:40     → loud C, default D, quiet E
G4/h~vel:120~tie G4/q              → very loud tied note
```

This takes precedence over `voice.set_dynamics`. Use it to shape phrase peaks,
pull back resolutions, or accent approach tones.

## Examples

```
C4/q D4/q E4/q F4/q                     → four quarter notes
E4/e F#4/e G4/q r/q                     → eighths + quarter + rest
C4/dq D4/e E4/h                         → dotted quarter + eighth + half
<Eb3 G3 Bb3>/q <Ab3 C4 Eb4>/q          → two chord stabs
C4/q~stac D4/q~stac r/e E4/e F4/q      → staccato melody
C4/e3 D4/e3 E4/e3                       → eighth-note triplet
```
