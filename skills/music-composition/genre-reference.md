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

