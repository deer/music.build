import type { DenoteConfig } from "@denote/core";

export const config: DenoteConfig = {
  name: "music.build",
  logo: {
    text: "music", // Lowercase in header
    suffix: ".build", // Rendered in primary color
  },
  colors: {
    primary: "#b45309",
    background: "#faf7f2",
    surface: "#f0e9dd",
    text: "#1c1008",
    border: "#d4c0a0",
    dark: {
      primary: "#f59e0b",
      background: "#0f0d0a",
      surface: "#1c1712",
      text: "#f0e9dd",
      border: "#3a2f22",
    },
  },
  fonts: {
    heading: 'Georgia, "Times New Roman", serif',
  },
  style: {
    //     darkMode: "auto",
    roundedness: "none",
  },
  landing: {
    hero: {
      title: "Compose music with",
      titleHighlight: "AI agents",
      description:
        "music.build is an MCP server that lets AI agents compose music as typed, immutable data — notes, voices, harmony, and form — and export to MIDI and LilyPond notation.",
    },
    cta: {
      primary: { text: "Get Started", href: "/docs/quickstart" },
      secondary: {
        text: "GitHub",
        href: "https://github.com/deer/music.build",
      },
    },
    install: "./mvnw exec:java -pl music-server",
    features: [
      {
        icon: "🎵",
        title: "Typed Music Data",
        description:
          "Notes, voices, and chords are first-class immutable values. No piano roll — compositions emerge from tool calls.",
      },
      {
        icon: "🤖",
        title: "47 MCP Tools",
        description:
          "Pitch, harmony, form, drums, transforms, and export. Connect to Claude Desktop or any MCP-capable agent.",
      },
      {
        icon: "🎼",
        title: "MIDI + LilyPond",
        description:
          "Export to playable MIDI and engraved PDF sheet music via LilyPond.",
      },
    ],
  },
  navigation: [
    {
      title: "Getting Started",
      children: [
        { title: "Introduction", href: "/docs/introduction" },
        { title: "Quickstart", href: "/docs/quickstart" },
        { title: "Installation", href: "/docs/installation" },
      ],
    },
    {
      title: "Reference",
      children: [
        { title: "Note Syntax", href: "/docs/note-syntax" },
        { title: "Tools", href: "/docs/tools" },
      ],
    },
  ],
  topNav: [
    { title: "Documentation", href: "/docs/introduction" },
    { title: "GitHub", href: "https://github.com/deer/music.build" },
  ],
  social: {
    github: "https://github.com/deer/music.build",
  },
  footer: {
    copyright: "© 2026 music.build · Apache 2.0",
  },
  search: {
    enabled: true,
  },
  ai: {
    mcp: true,
  },
};
