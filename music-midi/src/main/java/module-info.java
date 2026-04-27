module build.music.midi {
    requires build.music.pitch;
    requires build.music.time;
    requires build.music.core;
    requires build.music.score;
    requires build.music.harmony;
    requires java.desktop;
    requires build.base.marshalling;

    exports build.music.midi;
    opens build.music.midi to build.base.marshalling;
}
