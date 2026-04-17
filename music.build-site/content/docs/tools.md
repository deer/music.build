---
title: Tools
description: All 55 MCP tools organized by category, with descriptions and usage notes.
ai-summary: "Complete reference for all 55 music.build MCP tools, organized by category: voices, voice operations, score configuration, transforms, motifs, harmony, drums, query, form, rules, instruments, and export. Each entry describes what the tool does and when to use it. Cross-references note-syntax for the note DSL."
ai-keywords: [
  MCP tools,
  voice,
  harmony,
  drums,
  export,
  MIDI,
  form,
  transform,
  rules,
  instrument,
  score,
]
---

# Tools

music.build exposes 55 MCP tools. They are all stateless functions over
`CompositionContext` — the session state that accumulates between calls.

## Voices

Voices are the basic building block. Each is a named sequence of note events.

- `voice.create` — create a named voice from a note sequence (see
  [Note Syntax](/docs/note-syntax))
- `voice.append` — append more notes to an existing voice
- `voice.list` — list all voices with event counts and total duration
- `voice.set_dynamics` — set volume: `ppp pp p mp mf f ff fff` (default: `mf`)
- `voice.set_articulation` — set articulation for a voice:
  `staccato accent tenuto marcato legato`. Accepts optional `from_bar` /
  `to_bar` to scope to a bar range
- `voice.from_motif` — create a voice from a saved motif, optionally with a
  transform applied
- `voice.delete` — remove a voice from the session

## Voice operations

These produce new voices; originals are untouched unless you pass the same name
as the target.

- `voice.concat` — join multiple voices end-to-end (`voice_names`:
  comma-separated list, `target_voice`)
- `voice.repeat` — repeat a voice N times — useful for ostinatos and drum loops
- `voice.slice` — extract a measure range from a voice (start/end are measure
  numbers, end exclusive)
- `voice.pad_to_measure` — prepend rests so the voice starts at a given measure
  number

## Score configuration

- `score.set_metadata` — set title, tempo (BPM), and time signature (`"4/4"`,
  `"3/4"`, `"6/8"`, etc.)
- `score.assign_instrument` — assign a voice to a General MIDI instrument by
  name
- `score.set_swing` — enable swing quantization: `ratio="2/3"` (standard jazz),
  `"3/5"` (light), `"0"` to disable. Applies only to single eighth notes, not
  chords.
- `score.set_tempo_change` — gradual ritardando or accelerando over a bar span:
  `start_bar`, `end_bar`, `to_bpm`, optional `curve="linear"` (default) or
  `"exponential"`
- `score.describe` — inspect the full composition state: voices, dynamics,
  articulations, instruments
- `score.save` — save the current session to a JSON snapshot file
- `score.load` — restore a previously saved session from a JSON snapshot
- `score.clear` — wipe everything and start fresh

## Transforms

All transforms create a **new voice**. Pass the same name as `target_voice` to
overwrite the original.

- `transform.transpose` — transpose by interval (`"P5"`, `"m3"`, `"M2"`,
  `"P8"`), direction `"up"` or `"down"`
- `transform.invert` — mirror pitches around an axis pitch (e.g. `"C4"`)
- `transform.retrograde` — reverse the temporal order of events
- `transform.augment` — scale all durations by a rational factor (`"2/1"` =
  double, `"1/2"` = halve). After augmentation all durations become
  `ScaledDuration`, not standard rhythmic values.

> Chord events pass through transpose and invert unchanged — pitch transforms
> only apply to Note events.

## Motifs

- `motif.save` — save a slice of a voice as a named motif. `start` and `end` are
  0-based note indices.

Saved motifs can be recalled with `voice.from_motif` or inspected with
`query.motif`.

## Harmony

- `harmony.set_key` — set the composition key, e.g. `"F major"`, `"D minor"`,
  `"Bb major"`
- `harmony.chord_progression` — set a Roman-numeral progression, e.g.
  `"I IV V I"`, `"ii V I"`
- `harmony.set_bars` — declare per-bar chord symbols: `"1:Cm7 2:F7 3:Bbmaj7"`.
  Call this before `form.create_section` if using form tools.
- `harmony.harmonize` — generate a chord-root voice from the current key and
  progression. Always emits whole notes.
- `harmony.walking_bass` — generate a walking bass line over the chord changes.
  Adapts to time signature: 4/4 uses root-fifth-third-approach; 3/4 uses
  root-fifth-fifth; 6/8 uses root-fifth (dotted quarters).
- `harmony.comp` — generate a comping voice. Style options: `quarter_stabs`
  (default), `on_beat`, `eighth_pump`, `shell_voicings`, `charleston` (4/4 only)
- `harmony.suggest_harmony` — suggest chord harmonizations measure-by-measure
  for a given melody (advisory, does not modify state)
- `harmony.detect_key` — run pitch-class analysis on a voice and report the most
  likely key
- `harmony.diatonic_transpose` — transpose a voice by N diatonic scale steps
  within the current key

## Drums

Drum voices land on MIDI channel 9 (General MIDI percussion).

- `drums.preset` — load a named drum pattern for N bars. Creates named
  sub-voices: `drums_kick`, `drums_snare`, etc.

Available presets:

| Preset       | Character                                             |
| ------------ | ----------------------------------------------------- |
| `house_4on4` | Four-on-the-floor kick, closed hi-hats                |
| `rock_8th`   | Rock with eighth-note hi-hat pattern                  |
| `rock_basic` | Simple rock beat                                      |
| `bossa_nova` | Kick + rim + hi-hat + clave (tresillo)                |
| `waltz`      | 3/4 basic waltz                                       |
| `waltz_jazz` | 3/4 jazz waltz — brushed snare on 2 & 3               |
| `swing`      | Jazz swing                                            |
| `afrohouse`  | Syncopated kick, congas, open hi-hat accents, maracas |

After loading `afrohouse`, set `drums_maracas` to `p` or `pp` and `drums_congas`
to `mp`.

## Query

- `query.voice` — show the note sequence of a voice with bar markers
- `query.motif` — show the note sequence of a saved motif

## Form

Use form tools to assemble multi-section pieces.

- `form.create_section` — capture the current voices (or a named subset) as a
  section with a measure count
- `form.repeat_section` — append a repeat of a previously defined section
- `form.set_ending` — attach a 1st/2nd volta ending to a section. The ending
  section is consumed (removed from the play sequence) and attached as an
  alternate tail.
- `form.build` — assemble all sections into a final score. **Replaces voice
  event lists in-place** — per-section voice data is gone after this call.
- `form.describe` — show the current formal plan, e.g.
  `"A(8) → B(8) → A(8), total: 24 measures"`

> `harmony.set_bars` must be called **before** `form.create_section` for bar
> chords to be captured per section.

## Rules

Rules never throw — they accumulate `Violation` records with severity levels
(error / warning / suggestion).

- `rules.check` — validate voice leading and meter for all voices. Percussion
  voices (channel 9) are automatically skipped.
- `rules.check_range` — check all notes in a voice against a named instrument's
  written range

## Instruments

- `instrument.info` — get the written range, comfortable range, MIDI program
  number, clef, and supported articulations for a named instrument

The catalog covers 23 orchestral instruments. Use `instrument.info` before
writing a voice to stay within range.

## Export

- `export.midi` — write a MIDI file to `generated_tracks/`
- `export.lilypond` — write a LilyPond `.ly` source file and engrave to PDF
  (requires LilyPond installed). Always produces a `.midi` side-effect file
  alongside the PDF.
- `export.all` — write MIDI + LilyPond + a JSON composition snapshot + the
  session log (`session.jsonl`) to a numbered folder under `generated_tracks/`

Use `export.all` as the standard finish — it's the most complete output and the
session log enables replay.
