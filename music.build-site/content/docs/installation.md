---
title: Installation
description: Prerequisites, build instructions, and Claude Desktop setup for music.build.
ai-summary: "Full installation guide for music.build. Covers Java 25 requirement, LilyPond as an optional dependency for PDF export, cloning and building with the Maven wrapper, running the server, and connecting to Claude Desktop on macOS and Windows. Also covers port customization."
ai-keywords: [
  installation,
  Java 25,
  LilyPond,
  Maven,
  Claude Desktop,
  prerequisites,
  setup,
]
---

# Installation

## Prerequisites

**Required: Java 25**

```bash
java -version   # should report 25.x
```

Java 25 can be installed via [SDKMAN](https://sdkman.io)
(`sdk install java 25-open`) or downloaded from
[jdk.java.net](https://jdk.java.net/25/).

**Optional: LilyPond 2.24+**

LilyPond is only needed for PDF sheet music export. MIDI export works without
it.

```bash
lilypond --version   # should report 2.24 or later
```

Install via your system package manager or from
[lilypond.org](https://lilypond.org/download.html).

## Clone and build

```bash
git clone https://github.com/deer/music.build
cd music.build
./mvnw test          # optional: verify everything compiles and passes
```

Use `./mvnw` — never bare `mvn`. The Maven wrapper downloads the correct Maven
version automatically; no separate Maven install is needed.

All sibling library dependencies (`base.build`, `codemodel.build`,
`serve.build`) are on Maven Central — no local installs required.

## Run the server

```bash
./mvnw exec:java -pl music-server
```

The server listens on `http://localhost:3000/mcp`. To use a different port:

```bash
PORT=4000 ./mvnw exec:java -pl music-server
```

## Connect to Claude Desktop

**macOS** — edit
`~/Library/Application Support/Claude/claude_desktop_config.json`

**Windows** — edit `%APPDATA%\Claude\claude_desktop_config.json`

```json
{
  "mcpServers": {
    "music": {
      "url": "http://localhost:3000/mcp"
    }
  }
}
```

If you changed the port, update the URL accordingly. Restart Claude Desktop
after saving the config. The server must be running before Claude opens — start
it first.

## Single-user only

music.build is designed for local, single-user use. All composition state lives
in one in-process session — concurrent requests share the same state. Do not run
it as a hosted or multi-user service.
