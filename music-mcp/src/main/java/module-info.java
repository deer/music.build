module build.music.mcp {
    requires build.music.pitch;
    requires build.music.time;
    requires build.music.core;
    requires build.music.transform;
    requires build.music.score;
    requires build.music.midi;
    requires build.music.lilypond;
    requires build.music.musicxml;
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
    requires build.serve.mcp;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;

    exports build.music.mcp;
    exports build.music.mcp.tools;
    opens build.music.mcp to com.fasterxml.jackson.databind;
}
