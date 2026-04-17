module build.music.score {
    requires build.codemodel.foundation;
    requires build.base.marshalling;
    requires build.music.pitch;
    requires build.music.time;
    requires build.music.core;
    requires build.music.harmony;

    exports build.music.score;
}
