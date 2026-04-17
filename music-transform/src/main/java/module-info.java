module build.music.transform {
    requires build.music.pitch;
    requires build.music.time;
    requires build.music.core;
    requires build.base.marshalling;

    exports build.music.transform;
    opens build.music.transform to build.base.marshalling;
}
