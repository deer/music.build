module build.music.abc {
    requires build.music.pitch;
    requires build.music.time;
    requires build.music.core;
    requires build.music.score;
    requires build.base.marshalling;
    requires build.base.parsing;

    exports build.music.abc;
    opens build.music.abc to build.base.marshalling;
}
