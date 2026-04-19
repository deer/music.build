---
title: Quickstart
description: Start the server, connect Claude Desktop, and ask Claude to compose a piece in under five minutes.
ai-summary: "Step-by-step quickstart: clone the repo, start the MCP server, add it to Claude Desktop config, and prompt Claude to compose a piece. Includes the exact Claude Desktop JSON config and a sample composition prompt. Read this after the introduction."
ai-keywords: [quickstart, Claude Desktop, MCP server, setup, compose, MIDI]
---

# Quickstart

This gets you from zero to a composed MIDI file in about five minutes.

## 1. Clone and start the server

```bash
git clone https://github.com/deer/music.build
cd music.build
./mvnw exec:java -pl music-server
```

The server starts on `http://localhost:3000/mcp`. Leave it running.

The first run downloads Maven dependencies — subsequent starts are instant.

## 2. Connect Claude Desktop

Add music.build to your Claude Desktop configuration:

**macOS** — `~/Library/Application Support/Claude/claude_desktop_config.json`

**Windows** — `%APPDATA%\Claude\claude_desktop_config.json`

```json
{
  "mcpServers": {
    "music": {
      "url": "http://localhost:3000/mcp"
    }
  }
}
```

Restart Claude Desktop. The server must be running before Claude connects —
start it first, then open Claude.

## 3. Ask Claude to compose something

In Claude Desktop, try a prompt like:

> Compose a 16-bar jazz waltz in F major. Use a lyrical melody in the right
> hand, a walking bass in the left hand, and light brushed drums. Export to MIDI
> when done.

Claude will make a series of tool calls — creating voices, setting harmony,
loading a drum preset — and finish with `export.all`. The composed files appear
in `generated_tracks/` inside your music.build directory.

## What to expect

A typical composition session looks like this in the tool call log:

```
score.set_metadata   → title, tempo, 3/4 time
harmony.set_key      → F major
voice.create         → melody (8 bars)
voice.append         → melody (bars 9–16)
drums.preset         → waltz_jazz pattern
harmony.walking_bass → bass voice from chord progression
score.assign_instrument → melody → Flute, bass → Acoustic Bass
rules.check          → validate meter and voice leading
export.all           → write MIDI + session log
```

The MIDI file plays back with all voices, instruments, and tempo exactly as the
agent specified.

## Use a different port

If port 3000 is taken:

```bash
PORT=4000 ./mvnw exec:java -pl music-server
```

Update the `url` in your Claude Desktop config to match.

## Next steps

- [Installation](/docs/installation) — full prerequisites and build instructions
- [Note Syntax](/docs/note-syntax) — the DSL agents use to write notes
- [Tools](/docs/tools) — complete reference for all 47 MCP tools
