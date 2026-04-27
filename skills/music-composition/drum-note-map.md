# GM Drum Note Map

Full General MIDI percussion map for channel 9 voices. Use these note names when building manual drum patterns with `voice.create`.

| Note | MIDI | GM Drum Sound        | Use                           |
|------|------|----------------------|-------------------------------|
| B1   | 35   | Acoustic Bass Drum   | deep kick variant             |
| C2   | 36   | Bass Drum 1          | four-on-the-floor kick        |
| C#2  | 37   | Side Stick           | rim shot                      |
| D2   | 38   | Acoustic Snare       | snare / backbeat              |
| Eb2  | 39   | Hand Clap            | clap on 2 & 4                 |
| E2   | 40   | Electric Snare       | tighter snare sound           |
| F2   | 41   | Low Floor Tom        | floor tom low                 |
| F#2  | 42   | Closed Hi-Hat        | 8th-note groove               |
| G2   | 43   | High Floor Tom       | floor tom high                |
| Ab2  | 44   | Pedal Hi-Hat         | foot hi-hat                   |
| A2   | 45   | Low Tom              | tom fill                      |
| Bb2  | 46   | Open Hi-Hat          | accent on the "and"           |
| B2   | 47   | Low-Mid Tom          | tom fill                      |
| C3   | 48   | Hi-Mid Tom           | tom fill                      |
| C#3  | 49   | Crash Cymbal 1       | section transitions           |
| D3   | 50   | High Tom             | highest tom                   |
| Eb3  | 51   | Ride Cymbal 1        | ride groove                   |
| E3   | 52   | Chinese Cymbal       | aggressive accent             |
| F3   | 53   | Ride Bell            | ride bell ping                |
| F#3  | 54   | Tambourine           | 8th/16th texture              |
| G3   | 55   | Splash Cymbal        | quick accent                  |
| Ab3  | 56   | Cowbell              | funk/disco accent             |
| A3   | 57   | Crash Cymbal 2       | second crash                  |
| Bb3  | 58   | Vibraslap            | Latin accent                  |
| B3   | 59   | Ride Cymbal 2        | second ride                   |
| C4   | 60   | Hi Bongo             | Latin/Afro hand percussion    |
| C#4  | 61   | Low Bongo            | Latin/Afro hand percussion    |
| D4   | 62   | Mute Hi Conga        | Afrohouse/Latin percussion    |
| D#4  | 63   | Open Hi Conga        | Afrobeats hand percussion     |
| E4   | 64   | Low Conga            | Afrobeats hand percussion     |
| F4   | 65   | High Timbale         | Latin percussion              |
| F#4  | 66   | Low Timbale          | Latin percussion              |
| G4   | 67   | High Agogo           | Latin bell tone               |
| Ab4  | 68   | Low Agogo            | Latin bell tone               |
| A4   | 69   | Cabasa               | 16th-note shaker texture      |
| Bb4  | 70   | Maracas              | 16th/8th texture layer        |
| B4   | 71   | Short Whistle        | novelty/accents               |
| C5   | 72   | Long Whistle         | novelty/accents               |
| C#5  | 73   | Short Guiro          | Latin scrape short            |
| D5   | 74   | Long Guiro           | Latin scrape long             |
| Eb5  | 75   | Claves               | tresillo / clave pattern      |
| E5   | 76   | Hi Wood Block        | percussive click high         |
| F5   | 77   | Low Wood Block       | percussive click low          |
| F#5  | 78   | Mute Cuica           | Brazilian cuica               |
| G5   | 79   | Open Cuica           | Brazilian cuica               |
| Ab5  | 80   | Mute Triangle        | delicate accent               |
| A5   | 81   | Open Triangle        | bright delicate accent        |

## Manual Drum Voice Example (1 bar, 4/4)

```
voice.create "kick"  "C2/q C2/q C2/q C2/q"
voice.create "snare" "r/q D2/q r/q D2/q"
voice.create "hats"  "F#2/e F#2/e F#2/e F#2/e F#2/e F#2/e F#2/e F#2/e"
score.assign_instrument "kick"  "drums"
score.assign_instrument "snare" "drums"
score.assign_instrument "hats"  "drums"
# Then voice.repeat each voice for the desired number of bars
```
