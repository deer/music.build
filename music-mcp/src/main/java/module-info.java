module build.music.mcp {
    requires build.music.pitch;
    requires build.music.time;
    requires build.music.core;
    requires build.music.transform;
    requires build.music.score;
    requires build.music.midi;
    requires build.music.lilypond;
    requires build.music.harmony;
    requires build.music.voice;
    requires build.music.instrument;
    requires build.music.rules;
    requires build.music.form;
    requires java.desktop;
    requires build.codemodel.foundation;
    requires build.base.marshalling;
    requires build.base.transport;
    requires build.base.transport.json;
    requires build.base.version;
    requires com.fasterxml.jackson.core;

    exports build.music.mcp;
    exports build.music.mcp.tools;
}
