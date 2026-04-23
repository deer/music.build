@build.base.template.ProcessTemplates
module build.music.server {
    requires build.music.mcp;
    requires build.music.pitch;
    requires build.serve.mcp;
    requires build.serve.application;
    requires build.serve.health;
    requires build.serve.htmx;
    requires build.serve.sse;
    requires build.base.network;
    requires build.base.template;

    requires static build.base.template.processor;

    exports build.music.server.console;
}
