# Genre Reference

Quick-start scaffolding for common genres. Each entry: tempo range, time signature, key tendencies, drum preset, instrument stack, and the tool calls that establish the style.

---

### Jazz (Swing)

**Tempo:** 120–220 BPM | **Sig:** 4/4 | **Swing:** `"2/3"` | **Drums:** `swing`
**Keys:** Bb, F, Eb, Ab, C | **Progressions:** `ii V I`, `I vi ii V`, 12-bar blues
**Stack:** `rhodes`/`piano` + `electric_bass` + `saxophone`/`trumpet`

```
score.set_metadata title="Jazz Tune" tempo=160 time_signature="4/4"
harmony.set_key "Bb major"
harmony.set_bars "1:Bbmaj7 2:Gm7 3:Cm7 4:F7 5:Dm7 6:G7 7:Cm7 8:F7"
harmony.walking_bass target_voice="bass" octave=2
harmony.comp target_voice="comp" style="charleston" octave=3
drums.preset preset="swing" bars=8
score.set_swing ratio="2/3"
score.assign_instrument "bass" "electric_bass"
score.assign_instrument "comp" "rhodes"
```

---

### Jazz Waltz

**Tempo:** 140–200 BPM | **Sig:** 3/4 | **Swing:** `"2/3"` | **Drums:** `waltz_jazz`
**Stack:** `piano` + `electric_bass`

```
score.set_metadata title="Jazz Waltz" tempo=160 time_signature="3/4"
harmony.set_key "F minor"
harmony.set_bars "1:Fm7 2:Bb7 3:Ebmaj7 4:Abmaj7 5:Dm7b5 6:G7 7:Cm7 8:C7"
harmony.comp target_voice="comp" style="on_beat" octave=3
harmony.walking_bass target_voice="bass" octave=2
drums.preset preset="waltz_jazz" bars=8
score.set_swing ratio="2/3"
score.assign_instrument "comp" "piano"
score.assign_instrument "bass" "electric_bass"
```

---

### Bossa Nova

**Tempo:** 120–140 BPM | **Sig:** 4/4 | **Swing:** none — the clave replaces swing feel | **Drums:** `bossa_nova`
**Keys:** major 7ths — Cmaj7, Bbmaj7, Fmaj7 | **Progressions:** `ii V I` with major-7 voicings, cycle-of-5ths
**Stack:** `guitar` + `electric_bass`

```
score.set_metadata title="Bossa Nova" tempo=128 time_signature="4/4"
harmony.set_key "C major"
harmony.set_bars "1:Cmaj7 2:Am7 3:Dm7 4:G7 5:Cmaj7 6:Fmaj7 7:Dm7b5 8:G7"
drums.preset preset="bossa_nova" bars=8
harmony.comp target_voice="comp" style="quarter_stabs" octave=3
harmony.walking_bass target_voice="bass" octave=2 approach="diatonic"
score.assign_instrument "comp" "guitar"
score.assign_instrument "bass" "electric_bass"
```

---

### House / EDM

**Tempo:** 120–130 BPM | **Sig:** 4/4 | **Swing:** none | **Drums:** `house_4on4`
**Keys:** minor (Dorian, natural minor) | **Progressions:** `i bVII bVI bVII`, `i iv`, single-chord vamp
**Stack:** `synth_lead` + `synth_pad` + `synth_bass`

The defining texture is **off-beat chord stabs** — chords on the "and" of the beat, not the downbeat:

```
score.set_metadata title="House Track" tempo=124 time_signature="4/4"
harmony.set_key "F minor"
drums.preset preset="house_4on4" bars=8
voice.create "stabs" "r/e <F3 Ab3 C4>/e r/e <F3 Ab3 C4>/e r/e <Eb3 G3 Bb3>/e r/e <Eb3 G3 Bb3>/e"
voice.set_dynamics "stabs" "mp"
voice.set_articulation "stabs" "staccato"
score.assign_instrument "stabs" "synth_pad"
voice.create "bass" "F2/e F2/e r/e C2/e F2/e r/e C2/e r/e"
voice.set_dynamics "bass" "f"
score.assign_instrument "bass" "synth_bass"
```

---

### Reggae

**Tempo:** 70–100 BPM | **Sig:** 4/4 | **Swing:** none | **Drums:** manual (no preset)
**Keys:** Bb, F, G, D, A | **Progressions:** `i bVII bVI bVII`, `I IV V`
**Stack:** `electric_guitar` (skank) + `organ` + `electric_bass`

Two defining rhythms: the **skank** (staccato chords on every 8th upbeat) and the **one-drop** (kick + snare on beat 3 only):

```
score.set_metadata title="Reggae Groove" tempo=85 time_signature="4/4"
harmony.set_key "G minor"
voice.create "skank" "r/e <G3 Bb3 D4>/e r/e <G3 Bb3 D4>/e r/e <G3 Bb3 D4>/e r/e <G3 Bb3 D4>/e"
voice.set_dynamics "skank" "mf"
voice.set_articulation "skank" "staccato"
score.assign_instrument "skank" "electric_guitar"
voice.create "bass" "G2/q r/q G2/e r/e r/q"
voice.set_dynamics "bass" "f"
score.assign_instrument "bass" "electric_bass"
voice.create "kick"  "r/h C2/q r/q"
voice.create "snare" "r/h D2/q r/q"
voice.create "hihat" "F#2/e F#2/e F#2/e F#2/e F#2/e F#2/e F#2/e F#2/e"
score.assign_instrument "kick"  "drums"
score.assign_instrument "snare" "drums"
score.assign_instrument "hihat" "drums"
```

---

### Blues (12-Bar)

**Tempo:** 80–130 BPM | **Sig:** 4/4 | **Swing:** `"2/3"` for shuffle | **Drums:** `swing` (shuffle) or `rock_basic` (straight)
**Keys:** A, E, G, D, Bb | **Progression:** I7–I7–I7–I7–IV7–IV7–I7–I7–V7–IV7–I7–V7
**Stack:** `electric_guitar` + `electric_bass` + `piano`

```
score.set_metadata title="12-Bar Blues in A" tempo=108 time_signature="4/4"
harmony.set_key "A major"
harmony.set_bars "1:A7 2:A7 3:A7 4:A7 5:D7 6:D7 7:A7 8:A7 9:E7 10:D7 11:A7 12:E7"
harmony.walking_bass target_voice="bass" octave=2
harmony.comp target_voice="comp" style="on_beat" octave=3
drums.preset preset="swing" bars=12
score.set_swing ratio="2/3"
score.assign_instrument "bass" "electric_bass"
score.assign_instrument "comp" "piano"
```

---

### Hip-Hop / Lo-Fi

**Tempo:** 70–95 BPM | **Sig:** 4/4 | **Swing:** `"3/5"` (light bounce) | **Drums:** `rock_basic`
**Keys:** minor — D, A, C, F | **Progressions:** `i bVII bVI bVII`, `im7 IVm7`, `ii V i`
**Stack:** `rhodes`/`piano` + `electric_bass`/`synth_bass` + `vibraphone`; optionally `strings`/`choir`

```
score.set_metadata title="Lo-Fi Beat" tempo=82 time_signature="4/4"
harmony.set_key "D minor"
harmony.set_bars "1:Dm7 2:Bbmaj7 3:Gm7 4:Am7"
harmony.comp target_voice="keys" style="shell_voicings" octave=3
harmony.walking_bass target_voice="bass" octave=2
drums.preset preset="rock_basic" bars=4
score.set_swing ratio="3/5"
score.assign_instrument "keys" "rhodes"
score.assign_instrument "bass" "electric_bass"
```

---

### Afrohouse

**Tempo:** 115–125 BPM | **Sig:** 4/4 | **Swing:** none | **Drums:** `afrohouse`
**Keys:** minor (Dorian, pentatonic) | **Progressions:** `i bVII`, `i iv`, single-chord groove
**Stack:** `synth_bass` + `synth_lead` + `synth_pad` + hand percussion from preset

After loading `afrohouse`: set `drums_maracas` to `p` and `drums_congas` to `mp`.

```
score.set_metadata title="Afrohouse" tempo=120 time_signature="4/4"
harmony.set_key "A minor"
drums.preset preset="afrohouse" bars=8
voice.set_dynamics "drums_maracas" "p"
voice.set_dynamics "drums_congas"  "mp"
voice.create "pads" "<A3 C4 E4>/h <G3 B3 D4>/h"
voice.set_dynamics "pads" "mp"
voice.set_articulation "pads" "legato"
score.assign_instrument "pads" "synth_pad"
voice.create "bass" "A2/e r/e r/e A2/e A2/e r/q r/e"
voice.set_dynamics "bass" "f"
score.assign_instrument "bass" "synth_bass"
```

---

### Funk

**Tempo:** 88–108 BPM | **Sig:** 4/4 | **Swing:** none (or very light `"3/5"`) | **Drums:** `rock_basic`
**Keys:** minor (Dorian) — E, A, G, D | **Progressions:** single-chord vamp (`i7`), `I7 IV7`, `i bVII`
**Stack:** `electric_guitar` or `organ` + `electric_bass`

The two defining textures: **`eighth_pump`** comping (tight staccato 8th-chord stabs on every subdivision) and a **syncopated 16th-note bass line** written manually. Keep the bass line on or around the beat-1 root with syncopation on the "e" and "ah" subdivisions.

```
score.set_metadata title="Funk Groove" tempo=100 time_signature="4/4"
harmony.set_key "E minor"
harmony.set_bars "1:Em7 2:Em7 3:Em7 4:Em7"
drums.preset preset="rock_basic" bars=4
harmony.comp target_voice="chords" style="eighth_pump" octave=3
voice.set_dynamics "chords" "mf"
voice.set_articulation "chords" "staccato"
voice.create "bass" "E2/s r/s E2/s r/s G2/s A2/s E2/s r/s E2/s r/s E2/s r/s D2/s r/s B1/s r/s"
voice.set_dynamics "bass" "f"
score.assign_instrument "chords" "organ"
score.assign_instrument "bass" "electric_bass"
```

---

### R&B / Soul

**Tempo:** 65–95 BPM | **Sig:** 4/4 | **Swing:** `"3/5"` light bounce | **Drums:** `rock_basic`
**Keys:** minor — D, G, A, Bb | warm 7th and 9th voicings | **Progressions:** `i7 iv7 bVII bVI`, `ii7 V7 Imaj7`
**Stack:** `rhodes` + `electric_bass` + optional `strings` or `choir` for backing

Legato bass, `shell_voicings` comp for warmth, laid-back pocket feel. The groove sits slightly behind the beat — model this by keeping the bass line simple and sparse.

```
score.set_metadata title="R&B Groove" tempo=80 time_signature="4/4"
harmony.set_key "D minor"
harmony.set_bars "1:Dm7 2:Gm7 3:Bbmaj7 4:Am7"
drums.preset preset="rock_basic" bars=4
harmony.comp target_voice="keys" style="shell_voicings" octave=3
voice.set_dynamics "keys" "mf"
score.set_swing ratio="3/5"
voice.create "bass" "D2/q r/e D2/e A1/q r/q"
voice.set_dynamics "bass" "mf"
score.assign_instrument "keys" "rhodes"
score.assign_instrument "bass" "electric_bass"
```

---

### Gospel

**Tempo:** 80–120 BPM | **Sig:** 4/4 | **Swing:** none | **Drums:** `rock_basic`
**Keys:** major — Bb, Eb, F, C | **Progressions:** `I IV V I`, `I ii V I`, `I bVII IV I`
**Stack:** `organ` + `piano` + optional `choir`

The defining texture is full `on_beat` chord stabs on all four beats — dense, energetic, and loud. Organ carries the harmonic weight; piano doubles or adds runs. Call-and-response between melody and comping is idiomatic.

```
score.set_metadata title="Gospel" tempo=96 time_signature="4/4"
harmony.set_key "Bb major"
harmony.set_bars "1:Bbmaj7 2:Eb7 3:Bb 4:F7 5:Bbmaj7 6:Eb7 7:Bb 8:Bb"
harmony.comp target_voice="organ" style="on_beat" octave=3
voice.set_dynamics "organ" "ff"
voice.set_articulation "organ" "staccato"
harmony.walking_bass target_voice="bass" octave=2
drums.preset preset="rock_basic" bars=8
score.assign_instrument "organ" "organ"
score.assign_instrument "bass" "electric_bass"
```

---

### Disco

**Tempo:** 110–130 BPM | **Sig:** 4/4 | **Swing:** none | **Drums:** `house_4on4`
**Keys:** minor or major — Cm, Fm, Am, C major | lush 7th and 9th voicings | **Progressions:** `i bVII bVI bVII`, `I IV ii V`
**Stack:** `strings` + `electric_bass` + `piano` or `synth_pad`

The `house_4on4` kick is the disco backbone. The characteristic harmonic rhythm is `charleston` style — the dotted-quarter + eighth figure (long-short) that gives disco its momentum. Strings play the chord stabs; bass is melodic and prominent.

```
score.set_metadata title="Disco" tempo=120 time_signature="4/4"
harmony.set_key "C minor"
harmony.set_bars "1:Cm 2:Bb 3:Ab 4:Bb"
drums.preset preset="house_4on4" bars=4
harmony.comp target_voice="strings" style="charleston" octave=4
voice.set_dynamics "strings" "mf"
score.assign_instrument "strings" "strings"
harmony.walking_bass target_voice="bass" octave=2
voice.set_dynamics "bass" "f"
score.assign_instrument "bass" "electric_bass"
```

---

### Ska

**Tempo:** 130–160 BPM | **Sig:** 4/4 | **Swing:** none | **Drums:** `rock_basic`
**Keys:** major — G, D, A, Bb | bright, upbeat | **Progressions:** `I IV V I`, `I VI IV V`
**Stack:** `electric_guitar` (skank) + `trumpet` + `trombone` + `electric_bass`

The ska skank lands **on the upbeat** (the "and" of each beat) — the guitar chops on the off-beat while the kick and snare mark the downbeats. Reggae's one-drop kick pattern shifts the emphasis further, but both genres are fundamentally off-beat. Horns play staccato melodic stabs. Bass walks steadily.

```
score.set_metadata title="Ska" tempo=148 time_signature="4/4"
harmony.set_key "G major"
harmony.set_bars "1:G 2:C 3:D 4:G"
drums.preset preset="rock_basic" bars=4
voice.create "skank" "r/e <G3 B3 D4>/e r/e <G3 B3 D4>/e r/e <G3 B3 D4>/e r/e <G3 B3 D4>/e"
voice.set_dynamics "skank" "mf"
voice.set_articulation "skank" "staccato"
score.assign_instrument "skank" "electric_guitar"
harmony.walking_bass target_voice="bass" octave=2 approach="diatonic"
voice.set_dynamics "bass" "f"
score.assign_instrument "bass" "electric_bass"
```

---

### Samba

**Tempo:** 180–200 BPM | **Sig:** 4/4 | **Swing:** none — clave drives the feel | **Drums:** `bossa_nova`
**Keys:** major — G, C, F, Bb | bright, fast | **Progressions:** `I VI7 ii V I`, `I IV V I`
**Stack:** `guitar` + `electric_bass`

Use the `bossa_nova` preset at 180–200 BPM — the tresillo clave pattern works for samba but the higher tempo gives it the characteristic urgency. The bass plays an active "samba bass" pattern: root on beat 1, an upbeat fill on the "and" of beat 2.

```
score.set_metadata title="Samba" tempo=190 time_signature="4/4"
harmony.set_key "G major"
harmony.set_bars "1:Gmaj7 2:E7 3:Am7 4:D7 5:Gmaj7 6:Cmaj7 7:Am7 8:D7"
drums.preset preset="bossa_nova" bars=8
harmony.comp target_voice="comp" style="quarter_stabs" octave=3
voice.set_dynamics "comp" "mf"
voice.create "bass" "G2/e r/e G2/e A2/e G2/q r/q"
voice.set_dynamics "bass" "f"
score.assign_instrument "comp" "guitar"
score.assign_instrument "bass" "electric_bass"
```

---

### Salsa / Latin Jazz

**Tempo:** 160–210 BPM | **Sig:** 4/4 | **Swing:** none | **Drums:** manual clave
**Keys:** minor — Cm, Am, Dm | **Progressions:** `i ii7b5 V7 i`, `i iv V7 i`
**Stack:** `trumpet` + `trombone` + `piano` + `electric_bass`

The clave is the rhythmic spine of salsa. The **3-2 son clave** has 3 hits in bar 1 (beat 1, and-of-2, beat 4) and 2 hits in bar 2 (beat 2, and-of-3). Build it manually on channel 9. Piano montuno uses `quarter_stabs`; bass plays tumbao (root on 2 and 4).

```
score.set_metadata title="Salsa" tempo=185 time_signature="4/4"
harmony.set_key "C minor"
harmony.set_bars "1:Cm7 2:Fm7 3:G7 4:Cm7"
harmony.comp target_voice="piano" style="quarter_stabs" octave=3
voice.set_dynamics "piano" "mf"
harmony.walking_bass target_voice="bass" octave=2 approach="chromatic"
voice.set_dynamics "bass" "f"
score.assign_instrument "piano" "piano"
score.assign_instrument "bass" "electric_bass"
# 3-2 son clave (bar 1: beat 1, and-of-2, beat 4 / bar 2: beat 2, and-of-3)
voice.create "clave" "C5/q r/e C5/e r/q C5/q r/q C5/q r/e C5/e r/q"
voice.set_dynamics "clave" "mf"
score.assign_instrument "clave" "drums"
```

---

### Country

**Tempo:** 100–140 BPM | **Sig:** 4/4 | **Swing:** none | **Drums:** `rock_basic`
**Keys:** major — G, D, A, C, E | diatonic, straightforward | **Progressions:** `I IV V I`, `I V vi IV`, `I IV I V`
**Stack:** `electric_guitar` + `electric_bass`

Simple diatonic harmony, clear backbeat. `on_beat` comp or `quarter_stabs`; walking bass with `diatonic` approach. Keep it clean — country avoids chromaticism and extended chords. Pedal steel character can be approximated with a legato voice.

```
score.set_metadata title="Country" tempo=120 time_signature="4/4"
harmony.set_key "G major"
harmony.set_bars "1:G 2:C 3:G 4:D 5:G 6:C 7:D 8:G"
harmony.comp target_voice="guitar" style="on_beat" octave=3
voice.set_dynamics "guitar" "mf"
harmony.walking_bass target_voice="bass" octave=2 approach="diatonic"
voice.set_dynamics "bass" "f"
drums.preset preset="rock_basic" bars=8
score.assign_instrument "guitar" "electric_guitar"
score.assign_instrument "bass" "electric_bass"
```

---

### Pop

**Tempo:** 100–130 BPM | **Sig:** 4/4 | **Swing:** none | **Drums:** `rock_basic`
**Keys:** major or minor — C, G, D, A, F | **Progressions:** `I V vi IV`, `vi IV I V`, `I IV V I`
**Stack:** `synth_pad` + `synth_bass` + optional `piano`

The I–V–vi–IV loop (C–G–Am–F in C major) underlies hundreds of pop songs. `on_beat` or `quarter_stabs` for pads; legato articulation for atmosphere. Keep the arrangement sparse — pop mixes have space.

```
score.set_metadata title="Pop" tempo=116 time_signature="4/4"
harmony.set_key "C major"
harmony.set_bars "1:C 2:G 3:Am 4:F"
harmony.comp target_voice="pads" style="quarter_stabs" octave=4
voice.set_dynamics "pads" "mp"
voice.set_articulation "pads" "legato"
harmony.walking_bass target_voice="bass" octave=2 approach="diatonic"
voice.set_dynamics "bass" "mf"
drums.preset preset="rock_basic" bars=4
score.assign_instrument "pads" "synth_pad"
score.assign_instrument "bass" "synth_bass"
```

---

### Ambient / Drone

**Tempo:** 60–80 BPM (determines note duration only) | **Sig:** 4/4 | **Swing:** none | **Drums:** none
**Keys:** Dorian or Lydian feel — use `"X minor"` and build the harmony manually; pentatonic is safe for any mood
**Stack:** `synth_pad` + optional `strings` or `choir`

No drums, no bass line, no rhythmic pulse. Use whole and half notes only. Legato articulation throughout. `voice.add_expression_curve` is the primary dynamic tool — add slow crescendo/decrescendo curves on each pad voice rather than setting fixed dynamics.

```
score.set_metadata title="Ambient" tempo=70 time_signature="4/4"
voice.create "pad_1" "<D3 A3 C4 F4>/w <C3 G3 Bb3 E4>/w <D3 A3 C4 F4>/w <F3 A3 C4>/w"
voice.set_articulation "pad_1" "legato"
voice.add_expression_curve voice="pad_1" start_bar=1 end_bar=4 from_value=30 to_value=90 curve="linear"
score.assign_instrument "pad_1" "synth_pad"
voice.create "pad_2" "<A2 E3 G3>/w <G2 D3 F3>/w <A2 E3 G3>/w <A2 C3 E3>/w"
voice.set_articulation "pad_2" "legato"
voice.add_expression_curve voice="pad_2" start_bar=1 end_bar=4 from_value=20 to_value=70 curve="exponential"
score.assign_instrument "pad_2" "strings"
```

---

### Klezmer

**Tempo:** 126–160 BPM (freylekhs / festive dance) | **Sig:** 4/4 | **Swing:** light `"3/5"` optional | **Drums:** `rock_basic` (modern) or none (traditional)
**Keys:** harmonic minor — D, A, E | **Progressions:** `i bVII bVI V7 i`, `i iv V7 i`
**Stack:** `clarinet` + optional `piano` + `electric_bass`

The signature sound is the **augmented second** — the interval between the b6 and #7 of harmonic minor (e.g., Bb→C# in D harmonic minor). Spell these leaps explicitly in the melody voice. For the `V7` chord in D minor, the A7 requires C# — the raised 7th. Use `harmony.set_bars "...:A7"` and spell C# in the melody manually.

```
score.set_metadata title="Klezmer Freylekhs" tempo=144 time_signature="4/4"
harmony.set_key "D minor"
harmony.set_bars "1:Dm 2:Gm 3:A7 4:Dm"
voice.create "melody" "D4/q F4/q A4/q D5/q D5/q Bb4/q G4/q F4/q C#5/q A4/q E5/q C#5/q D5/h r/h"
voice.set_dynamics "melody" "mf"
score.assign_instrument "melody" "clarinet"
harmony.comp target_voice="comp" style="on_beat" octave=3
voice.set_dynamics "comp" "mp"
harmony.walking_bass target_voice="bass" octave=2
score.assign_instrument "comp" "piano"
score.assign_instrument "bass" "electric_bass"
```

---

### Flamenco

**Tempo:** 140–180 BPM (rumba flamenca) | **Sig:** 4/4 | **Swing:** none | **Drums:** none or manual cajon
**Keys:** Phrygian — use `"A minor"` and build the cadence Am→G→F→E manually | **Progressions:** `i bVII bVI bII` (Phrygian cadence), `i iv V i`
**Stack:** `guitar`

The Phrygian cadence `Am–G–F–E` (or `Dm–C–Bb–A`) is the defining harmonic motion. Note: the resolution chord is **E major** (not Em) — spell it as `E` in `harmony.set_bars`. `quarter_stabs` with staccato approximates rasgueado strumming. No walking bass — a simple root pedal or bass-chord alternation is more idiomatic.

```
score.set_metadata title="Flamenco Rumba" tempo=160 time_signature="4/4"
harmony.set_key "A minor"
harmony.set_bars "1:Am 2:G 3:F 4:E"
harmony.comp target_voice="guitar" style="quarter_stabs" octave=3
voice.set_dynamics "guitar" "mf"
voice.set_articulation "guitar" "staccato"
voice.create "bass" "A2/q A2/q A2/q A2/q G2/q G2/q G2/q G2/q F2/q F2/q F2/q F2/q E2/q E2/q E2/q E2/q"
voice.set_dynamics "bass" "f"
score.assign_instrument "guitar" "guitar"
score.assign_instrument "bass" "electric_bass"
```

---

### Traditional Irish

**Tempo:** 160–180 BPM (reel, 4/4), 110–120 BPM (jig, 6/8) | **Swing:** none
**Keys:** D major, G major, A major, E minor, D Dorian — modal flat-7 is idiomatic (D Mixolydian = D major with C♮)
**Progressions:** simple diatonic — `I bVII I`, `I IV I V`, `i bVII bVI bVII`; changes are sparse, melody-driven
**Drums:** none traditionally; optional bodhrán (quarter-note frame drum — use `G2` High Floor Tom on channel 9)
**Stack:** `flute` or `violin` (melody), `accordion` (chord fill), `guitar` (rhythm strums)

Structure is almost always **AABB** — two 8-bar strains, each repeated. Melody is all 8th notes at speed; chord accompaniment is sparse (one chord per bar or two). No walking bass — a held root or simple root/5th alternation is more idiomatic.

```
score.set_metadata title="The Morning Dew" tempo=168 time_signature="4/4"
harmony.set_key "D major"
voice.create "melody_A" "D4/e F#4/e A4/e D5/e A4/e F#4/e E4/e D4/e F#4/e A4/e B4/e A4/e G4/e F#4/e E4/e D4/e"
voice.set_dynamics "melody_A" "mf"
score.assign_instrument "melody_A" "flute"
harmony.set_bars "1:D 2:G 3:D 4:A 5:D 6:G 7:A 8:D"
harmony.comp target_voice="chords" style="on_beat" octave=3
voice.set_dynamics "chords" "mp"
score.assign_instrument "chords" "accordion"
# Optional bodhrán — quarter notes, channel 9
voice.create "bodhran" "G2/q G2/q G2/q G2/q"
voice.set_dynamics "bodhran" "mp"
score.assign_instrument "bodhran" "drums"
```

