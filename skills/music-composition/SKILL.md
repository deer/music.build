---
name: music-composition
description: "Use when composing music with the music-server MCP tools. Covers note syntax, tool workflow, and composition patterns."
---

# Music Composition with music-server

You have 47 MCP tools for building compositions from scratch. This skill covers the syntax, tool flow, and patterns.

## Note Sequence Syntax

Notes are whitespace-separated tokens: `pitch/duration`

**Pitch:** standard name + optional accidental + octave: `C4`, `F#3`, `Bb5`, `Eb4`

**Rests:** `r/q`, `r/h`, `r/e` etc.

**Chords:** angle brackets around space-separated pitches, followed by `/duration`:
```
<C4 E4 G4>/q       → C major triad, quarter note
<Eb3 G3 Bb3>/h     → Eb major triad, half note
<D3 F3 A3 C4>/q    → Dm7 chord, quarter note
```
Use chords for pad stabs, gospel organ, jazz voicings — any simultaneous pitches.

**Duration codes:**
| Code | Value        |
|------|--------------|
| `w`  | whole        |
| `h`  | half         |
| `q`  | quarter      |
| `e`  | eighth       |
| `s`  | sixteenth    |
| `dh` | dotted half  |
| `dq` | dotted quarter |
| `de` | dotted eighth |
| `h3` | half-note triplet (= 1/3 whole) |
| `q3` | quarter-note triplet (= 1/6 whole) |
| `e3` | eighth-note triplet (= 1/12 whole) |
| `s3` | sixteenth-note triplet (= 1/24 whole) |

**Ties** — append `~tie` to hold a note into the next note of the same pitch (extends sound across bar lines):
```
C4/h~tie C4/q     → C4 sounds for a dotted half total
G4/q~tie G4/e     → G4 quarter + eighth tied together
```
Ties are rendered as `~` in LilyPond and extend NOTE_OFF in MIDI. Use for notes that cross bar lines or need durations not expressible as a single rhythmic value.

**Per-note velocity** — append `~vel:N` (0–127) to override the voice-level dynamic for a single note:
```
C4/q~vel:100 D4/q E4/q~vel:40   # loud C, default D, quiet E
G4/h~vel:120~tie G4/q            # very loud tied note
```
Useful for shaping phrases: push peak notes, pull back resolutions, accent approach tones. Takes precedence over `voice.set_dynamics`.

**Articulations** — append `~code` after the duration:
| Suffix  | Articulation | LilyPond mark | MIDI effect          |
|---------|--------------|---------------|----------------------|
| `~stac` | staccato     | `-.`          | 50% duration         |
| `~acc`  | accent       | `->`          | normal duration      |
| `~ten`  | tenuto       | `--`          | full duration        |
| `~marc` | marcato      | `-^`          | 75% duration         |
| `~leg`  | legato       | (none)        | full duration        |

```
C4/q~stac D4/q E4/h     # staccato C, normal D and E
<C4 E4 G4>/q~acc         # accented chord stab
```

Use `voice.set_articulation` to apply an articulation to an entire voice at once.

**Examples:**
```
C4/q D4/q E4/q F4/q              # four quarter notes
E4/e F#4/e G4/q r/q              # eighths, quarter, rest
C4/dq D4/e E4/h                  # dotted quarter + eighth + half
<Eb3 G3 Bb3>/q <Ab3 C4 Eb4>/q   # two chord stabs
C4/q~stac D4/q~stac r/e E4/e F4/q  # staccato melody
```

## Tools by Category

### Composition state
- `score.set_metadata` — set title, tempo (BPM), time signature ("4/4", "3/4")
- `score.set_swing` — enable swing quantization: `ratio="2/3"` (standard jazz), `"3/5"` (light), `"0"` to disable
- `score.set_tempo_change` — gradual ritardando or accelerando across a bar span: `start_bar`, `end_bar`, `to_bpm`, optional `curve="linear"` (default) or `"exponential"` (more natural for rit.). Affects both MIDI playback and LilyPond notation.
- `score.describe` — inspect full composition state (voices, dynamics, articulations, instruments)
- `score.assign_instrument` — assign a voice to a GM instrument (see list below)
- `score.clear` — wipe everything and start fresh

### Voices
- `voice.create` — create a named voice from a note sequence
- `voice.append` — append more notes to an existing voice
- `voice.list` — list all voices with event counts and duration
- `voice.set_dynamics` — set volume for a voice: `ppp pp p mp mf f ff fff` (default: mf)
- `voice.set_articulation` — set articulation for a voice: `staccato accent tenuto marcato legato`. Optional `from_bar` / `to_bar` to scope to a bar range (e.g. make only the final phrase tenuto).
- `voice.from_motif` — Advanced: create a voice from a saved motif, optionally transformed
- `voice.delete` — remove a voice from the composition

### Query
- `query.voice` — show the note sequence of a voice
- `query.motif` — show the note sequence of a motif

### Transforms (all create a new voice, leaving the original intact)
- `transform.transpose` — transpose by interval (e.g. "P5", "m3", "M2", "P8"), direction "up"/"down"
- `transform.invert` — Advanced: mirror pitches around an axis pitch (e.g. "C4")
- `transform.retrograde` — Advanced: reverse temporal order
- `transform.augment` — Advanced: scale all durations by a factor ("2/1"=double, "1/2"=halve)

### Motifs
- `motif.save` — save a slice of a voice as a named motif (start/end are 0-based note indices)

### Voice operations (all produce a new voice)
- `voice.concat` — concatenate multiple voices sequentially (`voice_names`: comma list, `target_voice`)
- `voice.repeat` — repeat a voice N times — great for ostinatos and drum loops
- `voice.slice` — Advanced: extract measures startMeasure–endMeasure (exclusive) from a voice
- `voice.pad_to_measure` — Advanced: prepend rests so the voice starts at the given measure number

### Harmony
- `harmony.set_key` — set the composition key, e.g. "F major", "D minor", "Bb major"
- `harmony.chord_progression` — set a Roman-numeral progression, e.g. "I IV V I", "ii V I", "I V vi IV"
- `harmony.set_bars` — declare per-bar chord changes as concrete chord symbols: `"1:Cm7 2:F7 3:Bbmaj7 4:Eb"`
- `harmony.harmonize` — generate a chord-root voice from the current key + progression (`target_voice`, optional `octave`)
- `harmony.walking_bass` — generate a walking bass line over chords; adapts to time signature: 4/4 = root-fifth-third-approach, 3/4 = root-fifth-fifth, 6/8 = root-fifth (dotted-quarters), 2/4 = root-approach. Uses `harmony.set_bars` if set, else falls back to progression.
- `harmony.comp` — generate a comping (chord accompaniment) voice; adapts to time signature. Style options: `quarter_stabs` (default), `on_beat`, `eighth_pump`, `shell_voicings`, `charleston` (4/4 only)
- `harmony.suggest_harmony` — suggest chord harmonizations measure-by-measure for a given voice (advisory)
- `harmony.detect_key` — Advanced: run pitch-class analysis on a voice and report the most likely key
- `harmony.diatonic_transpose` — transpose a voice by N diatonic scale steps within the current key

### Drums
- `drums.preset` — load a named drum pattern for N bars; creates `drums_kick`, `drums_snare`, etc. voices on channel 9
  - Available presets: `house_4on4`, `rock_8th`, `rock_basic`, `bossa_nova`, `waltz`, `waltz_jazz`, `swing`, `afrohouse`
  - `waltz_jazz` (3/4): kick + hi-hat on beat 1, soft brushed snare (MP) on beats 2 & 3 — more idiomatic than `waltz` for jazz contexts
  - `bossa_nova` creates: `drums_kick`, `drums_rim`, `drums_hihat` (eighth hi-hat), `drums_clave` (tresillo: dq+dq+q)
  - `afrohouse` creates: `drums_kick` (beat 1, and-of-2, beat 3 — syncopated, not 4-on-the-floor), `drums_clap` (2&4), `drums_hihat` (closed eighths with open hat on and-of-2 and and-of-4), `drums_congas` (hi/low conga alternating), `drums_maracas` (running eighths, velocity P). Set `drums_maracas` to `p` or `pp` and `drums_congas` to `mp` after loading.

### Rules / validation
- `rules.check` — run voice-leading and meter rules; percussion voices (channel 9) are automatically skipped
- `rules.check_range` — check all notes in a voice against a named instrument's written range

### Instruments
- `instrument.info` — get written range, comfortable range, MIDI program, clef, and articulations for a named instrument

### Form
- `form.create_section` — capture the current voices (or a subset) as a named section with a measure count
- `form.repeat_section` — append a repeat of a previously defined section (optionally with a new label)
- `form.set_ending` — attach a 1st/2nd volta ending to a section: `section` (the repeated section), `pass` (1 or 2), `ending_section` (a separately created section with the alternate tail bars). LilyPond renders `\repeat volta` + `\alternative` blocks; MIDI plays the correct ending per pass.
- `form.build` — assemble sections into a full score; replaces per-section voices with the full assembled versions
- `form.describe` — show the current formal plan (e.g. "A(8) → B(8) → A(8), total: 24 measures")

### Export
- `export.all` — **preferred** — creates a folder with .mid, .ly, .json, and PDF (if LilyPond installed). The server auto-prepends a sequential number to the folder name (e.g. `folder="sunlit_yard"` → `25_sunlit_yard`). Do NOT include a number yourself — it will double-prefix (e.g. `25_25_sunlit_yard`).
- `export.midi` — write a .mid file to the current directory
- `export.lilypond` — write a .ly file (and engrave PDF if LilyPond is installed)

### Session persistence
- `score.save` — save the full composition state to a JSON snapshot under `generated_tracks/` (voices, motifs, metadata, key, swing, bar chords, tempo changes). Use to checkpoint work in progress.
- `score.load` — restore a previously saved snapshot, replacing all current state.

## Available Instruments

**Keyboards:** `piano`, `bright_piano`, `honky_tonk` (detuned upright), `electric_piano` (Rhodes), `rhodes`, `harpsichord`, `organ`, `accordion`

**Tuned percussion:** `vibraphone`, `marimba`

**Guitar / bass:** `guitar`, `electric_guitar`, `electric_bass`, `synth_bass`

**Strings:** `violin`, `viola`, `cello`, `strings`, `synth_strings`

**Choir:** `choir`

**Brass / winds:** `trumpet`, `trombone`, `tuba`, `french_horn`, `brass`, `saxophone`, `clarinet`, `flute`, `oboe`, `bassoon`

**Synth:** `synth_lead` (sawtooth — classic house/trance lead), `synth_pad` (warm pad — chord stabs)

**Drums:** `drums` — automatically assigned to MIDI channel 9 (GM percussion). Notes in the voice are treated as GM drum sound numbers. Use the note names below to select sounds:

| Note | MIDI | GM Drum Sound       | Use                        |
|------|------|---------------------|----------------------------|
| C2   | 36   | Bass Drum 1         | four-on-the-floor kick     |
| C#2  | 37   | Side Stick          | rim shot                   |
| D2   | 38   | Snare Drum 1        | snare / backbeat           |
| Eb2  | 39   | Hand Clap           | clap on 2 & 4              |
| F#2  | 42   | Closed Hi-Hat       | 8th-note groove            |
| Ab2  | 44   | Pedal Hi-Hat        | open 16th feel             |
| Bb2  | 46   | Open Hi-Hat         | accent on the "and"        |
| C#3  | 49   | Crash Cymbal 1      | section transitions        |
| Eb3  | 51   | Ride Cymbal 1       | ride groove                |
| Bb2  | 46   | Open Hi-Hat         | breathable "and" accent    |
| D#4  | 63   | Open Hi Conga       | Afrobeats hand percussion  |
| E4   | 64   | Low Conga           | Afrobeats hand percussion  |
| Bb4  | 70   | Maracas             | 16th/8th texture layer     |

**Drum preset shortcut — instead of building patterns manually, use `drums.preset`:**
```
drums.preset preset="house_4on4" bars=8
# Creates drums_kick, drums_clap, drums_hihat — 8 bars, all on channel 9
```

**Manual drum voice example (1 bar, 4/4):**
```
voice.create "kick"  "C2/q C2/q C2/q C2/q"
voice.create "snare" "r/q D2/q r/q D2/q"
voice.create "hats"  "F#2/e F#2/e F#2/e F#2/e F#2/e F#2/e F#2/e F#2/e"
score.assign_instrument "kick"  "drums"
score.assign_instrument "snare" "drums"
score.assign_instrument "hats"  "drums"
# Then voice.repeat each voice for the desired number of bars
```

## Typical Workflow

```
1. score.set_metadata       — title, tempo, time sig
2. harmony.set_key          — optional; required for harmony.* and diatonic_transpose
3. drums.preset             — optional; quick drum pattern for N bars
4. voice.create             — one call per voice, with full note sequence
   voice.append             — add more bars if needed
   voice.set_dynamics       — set volume per voice (f for lead, mp for pads, etc.)
   voice.set_articulation   — set articulation per voice (staccato for stabs, etc.)
5. transform.transpose      — derive harmony voices (parallel thirds, octave doublings)
   voice.repeat             — extend loops to full length
   voice.concat             — join sections manually if not using form tools
6. harmony.harmonize        — generate an accompaniment voice (needs harmony.set_key first)
7. rules.check              — validate voice leading and meter (percussion skipped automatically)
8. form.create_section      — snapshot current voices as a named section (A, B, etc.)
   form.repeat_section      — plan repeats (ABA, AABA, etc.)
   form.build               — assemble sections; voices replaced with full-length assembled versions
9. score.assign_instrument  — one call per voice (instrument assignments survive form.build, so either order works)
10. score.describe          — sanity check before export
11. export.all              — or export.midi / export.lilypond
```

## Patterns

**House track with chord stabs:**
```
harmony.set_key "Eb major"
voice.create "chords" "<Eb3 G3 Bb3>/q r/q <Ab3 C4 Eb4>/q r/q"
voice.set_dynamics "chords" "mp"
voice.set_articulation "chords" "staccato"
score.assign_instrument "chords" "synth_pad"
```

**Walking bass line (manual):**
```
harmony.set_key "F major"
voice.create "bass" "F2/q A2/q C3/q E3/q | D2/q F2/q A2/q C3/q"
voice.set_dynamics "bass" "f"
score.assign_instrument "bass" "electric_bass"
```

**Walking bass (auto-generated from chord changes):**
```
harmony.set_key "G minor"
harmony.set_bars "1:Gm7 2:C7 3:Fm7 4:Bb7 5:Ebmaj7 6:Ebmaj7 7:Am7b5 8:D7"
harmony.walking_bass target_voice="bass" octave=2
score.set_swing ratio="2/3"          # add jazz swing to eighths
score.assign_instrument "bass" "electric_bass"
```

**Jazz swing feel:**
```
# Write melody in straight eighths, then add swing for playback
score.set_swing ratio="2/3"          # standard 2:1 jazz swing
# score.set_swing ratio="3/5"        # lighter shuffle
# score.set_swing ratio="0"          # disable
```

**Jazz comping from chord changes (no manual note entry):**
```
harmony.set_key "Bb major"
harmony.set_bars "1:Bb7 2:Bb7 3:Eb7 4:Eb7 5:Bb7 6:F7"
harmony.comp target_voice="comp" style="charleston" octave=3
score.set_swing ratio="2/3"
score.assign_instrument "comp" "rhodes"
# style options: quarter_stabs (2&4), on_beat, eighth_pump, shell_voicings, charleston
```

**Diatonic harmony in thirds:**
```
harmony.set_key "G major"
transform.transpose "melody" "M3" "up" target_voice="melody_thirds"
# Now melody_thirds is a parallel major-third above melody (chromatic, not diatonic)
# For diatonic thirds use harmony.diatonic_transpose "melody" steps=2
```

**Structural form (ABA):**
```
# Build A section voices, then:
form.create_section "A" measures=8
# Build B section voices (can be completely different), then:
form.create_section "B" measures=8
form.repeat_section "A"
form.build          # assembles A(8) + B(8) + A(8) = 24 measures
# After form.build, all voices are replaced with full 24-bar assembled versions
```

**Volta endings (1st/2nd ending brackets):**
```
# Build main A section (7 bars), then create two 1-bar endings:
form.create_section "A" measures=8       # full 8-bar snapshot
# Build "A_end1" voices (1-bar turnaround back to A):
form.create_section "A_end1" measures=1
# Build "A_end2" voices (1-bar final cadence):
form.create_section "A_end2" measures=1
# Attach endings — pass 1 uses A_end1, pass 2 uses A_end2:
form.set_ending section="A" pass=1 ending_section="A_end1"
form.set_ending section="A" pass=2 ending_section="A_end2"
form.repeat_section "A"
form.build   # LilyPond: \repeat volta 2 + \alternative; MIDI: correct ending per pass
```

**Ritardando on final bars:**
```
# Assume 24-bar piece at 120 BPM — slow to 60 over the last 3 bars:
score.set_tempo_change start_bar=22 end_bar=24 to_bpm=60 curve="exponential"
# exponential feels more natural than linear for a rit.
```

**Canon at the octave:**
```
voice.create "subject" "C4/q D4/q E4/q G4/q E4/q F4/q G4/q C5/q"
voice.pad_to_measure "subject" start_measure=3 target_voice="answer"
transform.transpose "answer" "P8" "down" target_voice="answer_low"
```

**Motivic development (build a piece from a short cell):**
```
# 1. Define a 4-note cell and save it as a motif
voice.create "cell" "C4/q E4/q G4/q B4/q"
motif.save "cell" startNote=0 endNote=4

# 2. Derive transformed variants — each becomes its own voice
voice.from_motif "cell_inv"   motif="cell" transform="invert"     axis="C4"
voice.from_motif "cell_retro" motif="cell" transform="retrograde"
voice.from_motif "cell_up5"   motif="cell" transform="transpose"  interval="P5"

# 3. Augment for a slower countermelody (double all durations)
transform.augment "cell" factor="2/1" target_voice="cell_slow"

# 4. Build voices by repeating cells — stagger entry for canon/imitation effect
voice.repeat "cell"     times=4 target_voice="subject"
voice.repeat "cell_inv" times=4 target_voice="counter"
voice.pad_to_measure "counter" start_measure=2   # answer enters 1 bar later

# 5. B section uses retrograde or transposed variant as new material
voice.repeat "cell_retro" times=4 target_voice="b_melody"
voice.repeat "cell_up5"   times=4 target_voice="b_counter"

# 6. Assemble into form
form.create_section "A" measures=4
# (swap in B voices here)
form.create_section "B" measures=4
form.repeat_section "A"
form.build   # A(4) + B(4) + A(4) = 12 bars, all derived from the original cell

# Clean up ALL scratch voices before form.create_section or export
# Any voice still in scope will become a staff in the PDF
voice.delete "cell"
voice.delete "cell_inv"
voice.delete "cell_retro"
voice.delete "cell_up5"
voice.delete "cell_aug"
voice.delete "cell_slow"
```
The key idea: **one cell generates all the material**. The transforms (invert, retrograde, transpose, augment) are the compositional engine — not just decoration.

## Tips

- **Swing** only affects eighth notes in MIDI output. Quarter notes, dotted values, and triplets are unswung. Write swing melodies using straight `e` eighths — the renderer adds the lilt.
- **Walking bass** uses `harmony.set_bars` chords if set, otherwise falls back to `harmony.chord_progression`. Always set the key before calling `walking_bass` (needed for approach notes).
- **Chord names in LilyPond PDF** — if `harmony.set_bars` has been called, chord symbols are automatically rendered above the staves as a `\new ChordNames` staff. No extra step needed; just export after setting bar chords.
- **Per-section chord maps with `form.build`** — call `harmony.set_bars` separately for each section before calling `form.create_section`. `form.build` automatically merges and offsets each section's chord map to the correct absolute bar numbers in the assembled score. You do not need to manually re-number bars across sections.
- **`harmony.comp`** styles: `quarter_stabs` (beats 2&4 — jazz default), `on_beat` (all beats — rock/gospel), `eighth_pump` (8 per bar — funk), `shell_voicings` (root+7th on 2&4 — sparse jazz), `charleston` (dotted-quarter+eighth+quarter+rest — classic jazz). Use octave 3 for piano/Rhodes, octave 4 for guitar.
- **Ties** (`~tie`) are for notes that span bar lines or durations not expressible as a single value. The tied notes must have the same pitch; MIDI renders them as a single sustained note.
- **`harmony.set_bars`** chord symbols use the same quality syntax as `ChordSymbol.parse`: `Cm7`, `F7`, `Ebmaj7`, `Am7b5`, `Ddim7`. Bare root (e.g. `Eb`) means major.
- **`target_voice`** on all transforms is optional — omit to get `{name}_transposed` etc.
- Rests pass through all transforms unchanged (no pitch to operate on).
- Chords: `transform.transpose` transposes all pitches in the chord; `transform.augment` scales the chord's duration. `transform.invert` does not invert chord pitches — chords pass through unchanged.
- `score.assign_instrument "kick" "drums"` forces channel 9 automatically.
- `harmony.set_key` must be called before `harmony.harmonize`, `harmony.diatonic_transpose`.
- `form.build` rest-fills missing voices automatically — if "bass" exists in A but not B, B gets whole-note rests.
- After `form.build`, voices are replaced in-place — no `_form` suffix, no stale originals.
- `rules.check` is advisory — violations are warnings/suggestions, never blockers. Percussion voices are skipped.
- Key signature is now reflected in LilyPond output — call `harmony.set_key` before exporting for correct notation.
