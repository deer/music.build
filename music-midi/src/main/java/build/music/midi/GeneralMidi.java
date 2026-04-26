package build.music.midi;

/**
 * General MIDI constants: program numbers (0-indexed), percussion note numbers, and CC numbers.
 */
public final class GeneralMidi {

    // -----------------------------------------------------------------------
    // Programs — Piano (0–7)
    // -----------------------------------------------------------------------
    public static final int ACOUSTIC_GRAND_PIANO    = 0;
    public static final int BRIGHT_ACOUSTIC_PIANO   = 1;
    public static final int ELECTRIC_GRAND_PIANO    = 2;
    public static final int HONKYTONK_PIANO         = 3;
    public static final int ELECTRIC_PIANO_1        = 4;
    public static final int ELECTRIC_PIANO_2        = 5;
    public static final int HARPSICHORD             = 6;
    public static final int CLAVI                   = 7;

    // -----------------------------------------------------------------------
    // Programs — Chromatic Percussion (8–15)
    // -----------------------------------------------------------------------
    public static final int CELESTA                 = 8;
    public static final int GLOCKENSPIEL            = 9;
    public static final int MUSIC_BOX               = 10;
    public static final int VIBRAPHONE              = 11;
    public static final int MARIMBA                 = 12;
    public static final int XYLOPHONE               = 13;
    public static final int TUBULAR_BELLS           = 14;
    public static final int DULCIMER                = 15;

    // -----------------------------------------------------------------------
    // Programs — Organ (16–23)
    // -----------------------------------------------------------------------
    public static final int DRAWBAR_ORGAN           = 16;
    public static final int PERCUSSIVE_ORGAN        = 17;
    public static final int ROCK_ORGAN              = 18;
    public static final int CHURCH_ORGAN            = 19;
    public static final int REED_ORGAN              = 20;
    public static final int ACCORDION               = 21;
    public static final int HARMONICA               = 22;
    public static final int TANGO_ACCORDION         = 23;

    // -----------------------------------------------------------------------
    // Programs — Guitar (24–31)
    // -----------------------------------------------------------------------
    public static final int ACOUSTIC_GUITAR_NYLON   = 24;
    public static final int ACOUSTIC_GUITAR_STEEL   = 25;
    public static final int ELECTRIC_GUITAR_JAZZ    = 26;
    public static final int ELECTRIC_GUITAR_CLEAN   = 27;
    public static final int ELECTRIC_GUITAR_MUTED   = 28;
    public static final int OVERDRIVEN_GUITAR        = 29;
    public static final int DISTORTION_GUITAR       = 30;
    public static final int GUITAR_HARMONICS        = 31;

    // -----------------------------------------------------------------------
    // Programs — Bass (32–39)
    // -----------------------------------------------------------------------
    public static final int ACOUSTIC_BASS           = 32;
    public static final int ELECTRIC_BASS_FINGER    = 33;
    public static final int ELECTRIC_BASS_PICK      = 34;
    public static final int FRETLESS_BASS           = 35;
    public static final int SLAP_BASS_1             = 36;
    public static final int SLAP_BASS_2             = 37;
    public static final int SYNTH_BASS_1            = 38;
    public static final int SYNTH_BASS_2            = 39;

    // -----------------------------------------------------------------------
    // Programs — Strings (40–47)
    // -----------------------------------------------------------------------
    public static final int VIOLIN                  = 40;
    public static final int VIOLA                   = 41;
    public static final int CELLO                   = 42;
    public static final int CONTRABASS              = 43;
    public static final int TREMOLO_STRINGS         = 44;
    public static final int PIZZICATO_STRINGS       = 45;
    public static final int ORCHESTRAL_HARP         = 46;
    public static final int TIMPANI                 = 47;

    // -----------------------------------------------------------------------
    // Programs — Ensemble (48–55)
    // -----------------------------------------------------------------------
    public static final int STRING_ENSEMBLE_1       = 48;
    public static final int STRING_ENSEMBLE_2       = 49;
    public static final int SYNTH_STRINGS_1         = 50;
    public static final int SYNTH_STRINGS_2         = 51;
    public static final int CHOIR_AAHS              = 52;
    public static final int VOICE_OOHS              = 53;
    public static final int SYNTH_VOICE             = 54;
    public static final int ORCHESTRA_HIT           = 55;

    // -----------------------------------------------------------------------
    // Programs — Brass (56–63)
    // -----------------------------------------------------------------------
    public static final int TRUMPET                 = 56;
    public static final int TROMBONE                = 57;
    public static final int TUBA                    = 58;
    public static final int MUTED_TRUMPET           = 59;
    public static final int FRENCH_HORN             = 60;
    public static final int BRASS_SECTION           = 61;
    public static final int SYNTH_BRASS_1           = 62;
    public static final int SYNTH_BRASS_2           = 63;

    // -----------------------------------------------------------------------
    // Programs — Reed (64–71)
    // -----------------------------------------------------------------------
    public static final int SOPRANO_SAX             = 64;
    public static final int ALTO_SAX                = 65;
    public static final int TENOR_SAX               = 66;
    public static final int BARITONE_SAX            = 67;
    public static final int OBOE                    = 68;
    public static final int ENGLISH_HORN            = 69;
    public static final int BASSOON                 = 70;
    public static final int CLARINET               = 71;

    // -----------------------------------------------------------------------
    // Programs — Pipe (72–79)
    // -----------------------------------------------------------------------
    public static final int PICCOLO                 = 72;
    public static final int FLUTE                   = 73;
    public static final int RECORDER               = 74;
    public static final int PAN_FLUTE               = 75;
    public static final int BLOWN_BOTTLE            = 76;
    public static final int SHAKUHACHI              = 77;
    public static final int WHISTLE                 = 78;
    public static final int OCARINA                 = 79;

    // -----------------------------------------------------------------------
    // Programs — Synth Lead (80–87)
    // -----------------------------------------------------------------------
    public static final int LEAD_SQUARE             = 80;
    public static final int LEAD_SAWTOOTH           = 81;
    public static final int LEAD_CALLIOPE           = 82;
    public static final int LEAD_CHIFF              = 83;
    public static final int LEAD_CHARANG            = 84;
    public static final int LEAD_VOICE              = 85;
    public static final int LEAD_FIFTHS             = 86;
    public static final int LEAD_BASS_LEAD          = 87;

    // -----------------------------------------------------------------------
    // Programs — Synth Pad (88–95)
    // -----------------------------------------------------------------------
    public static final int PAD_NEW_AGE             = 88;
    public static final int PAD_WARM                = 89;
    public static final int PAD_POLYSYNTH           = 90;
    public static final int PAD_CHOIR               = 91;
    public static final int PAD_BOWED               = 92;
    public static final int PAD_METALLIC            = 93;
    public static final int PAD_HALO                = 94;
    public static final int PAD_SWEEP               = 95;

    // -----------------------------------------------------------------------
    // Programs — Synth Effects (96–103)
    // -----------------------------------------------------------------------
    public static final int FX_RAIN                 = 96;
    public static final int FX_SOUNDTRACK           = 97;
    public static final int FX_CRYSTAL              = 98;
    public static final int FX_ATMOSPHERE           = 99;
    public static final int FX_BRIGHTNESS           = 100;
    public static final int FX_GOBLINS              = 101;
    public static final int FX_ECHOES               = 102;
    public static final int FX_SCI_FI               = 103;

    // -----------------------------------------------------------------------
    // Programs — Ethnic (104–111)
    // -----------------------------------------------------------------------
    public static final int SITAR                   = 104;
    public static final int BANJO                   = 105;
    public static final int SHAMISEN                = 106;
    public static final int KOTO                    = 107;
    public static final int KALIMBA                 = 108;
    public static final int BAG_PIPE                = 109;
    public static final int FIDDLE                  = 110;
    public static final int SHANAI                  = 111;

    // -----------------------------------------------------------------------
    // Programs — Percussive (112–119)
    // -----------------------------------------------------------------------
    public static final int TINKLE_BELL             = 112;
    public static final int AGOGO                   = 113;
    public static final int STEEL_DRUMS             = 114;
    public static final int WOODBLOCK               = 115;
    public static final int TAIKO_DRUM              = 116;
    public static final int MELODIC_TOM             = 117;
    public static final int SYNTH_DRUM              = 118;
    public static final int REVERSE_CYMBAL          = 119;

    // -----------------------------------------------------------------------
    // Programs — Sound Effects (120–127)
    // -----------------------------------------------------------------------
    public static final int GUITAR_FRET_NOISE       = 120;
    public static final int BREATH_NOISE            = 121;
    public static final int SEASHORE                = 122;
    public static final int BIRD_TWEET              = 123;
    public static final int TELEPHONE_RING          = 124;
    public static final int HELICOPTER              = 125;
    public static final int APPLAUSE                = 126;
    public static final int GUNSHOT                 = 127;

    // -----------------------------------------------------------------------
    // Percussion note numbers (GM channel 10, 0-indexed = channel 9)
    // -----------------------------------------------------------------------
    public static final int DRUM_CHANNEL            = 9;

    public static final int PERC_ACOUSTIC_BASS_DRUM = 35;
    public static final int PERC_BASS_DRUM_1        = 36;
    public static final int PERC_SIDE_STICK         = 37;
    public static final int PERC_ACOUSTIC_SNARE     = 38;
    public static final int PERC_HAND_CLAP          = 39;
    public static final int PERC_ELECTRIC_SNARE     = 40;
    public static final int PERC_LOW_FLOOR_TOM      = 41;
    public static final int PERC_CLOSED_HI_HAT      = 42;
    public static final int PERC_HIGH_FLOOR_TOM     = 43;
    public static final int PERC_PEDAL_HI_HAT       = 44;
    public static final int PERC_LOW_TOM            = 45;
    public static final int PERC_OPEN_HI_HAT        = 46;
    public static final int PERC_LOW_MID_TOM        = 47;
    public static final int PERC_HI_MID_TOM         = 48;
    public static final int PERC_CRASH_CYMBAL_1     = 49;
    public static final int PERC_HIGH_TOM           = 50;
    public static final int PERC_RIDE_CYMBAL_1      = 51;
    public static final int PERC_CHINESE_CYMBAL      = 52;
    public static final int PERC_RIDE_BELL          = 53;
    public static final int PERC_TAMBOURINE         = 54;
    public static final int PERC_SPLASH_CYMBAL      = 55;
    public static final int PERC_COWBELL            = 56;
    public static final int PERC_CRASH_CYMBAL_2     = 57;
    public static final int PERC_VIBRASLAP          = 58;
    public static final int PERC_RIDE_CYMBAL_2      = 59;
    public static final int PERC_HI_BONGO           = 60;
    public static final int PERC_LOW_BONGO          = 61;
    public static final int PERC_MUTE_HI_CONGA      = 62;
    public static final int PERC_OPEN_HI_CONGA      = 63;
    public static final int PERC_LOW_CONGA          = 64;
    public static final int PERC_HIGH_TIMBALE       = 65;
    public static final int PERC_LOW_TIMBALE        = 66;
    public static final int PERC_HIGH_AGOGO         = 67;
    public static final int PERC_LOW_AGOGO          = 68;
    public static final int PERC_CABASA             = 69;
    public static final int PERC_MARACAS            = 70;
    public static final int PERC_SHORT_WHISTLE      = 71;
    public static final int PERC_LONG_WHISTLE       = 72;
    public static final int PERC_SHORT_GUIRO        = 73;
    public static final int PERC_LONG_GUIRO         = 74;
    public static final int PERC_CLAVES             = 75;
    public static final int PERC_HI_WOOD_BLOCK      = 76;
    public static final int PERC_LOW_WOOD_BLOCK     = 77;
    public static final int PERC_MUTE_CUICA         = 78;
    public static final int PERC_OPEN_CUICA         = 79;
    public static final int PERC_MUTE_TRIANGLE      = 80;
    public static final int PERC_OPEN_TRIANGLE      = 81;

    // -----------------------------------------------------------------------
    // MIDI CC constants
    // -----------------------------------------------------------------------
    public static final int CC_MODULATION           = 1;
    public static final int CC_VOLUME               = 7;
    public static final int CC_PAN                  = 10;
    public static final int CC_EXPRESSION           = 11;
    public static final int CC_SUSTAIN              = 64;
    public static final int CC_REVERB               = 91;
    public static final int CC_CHORUS               = 93;

    private GeneralMidi() {
    }
}
