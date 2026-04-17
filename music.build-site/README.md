# music.build-site

Built with [Denote](https://denote.sh) — an AI-native documentation framework.

## Development

```bash
deno task dev
```

Open [http://localhost:8000](http://localhost:8000) to view your site.

## Build

```bash
deno task build
```

## Start

Serve the production build:

```bash
deno task start
```

## Deploy

### Deno Deploy

Link your GitHub repo at [dash.deno.com](https://dash.deno.com). Set the build
command to `deno task build` and the entry point to `_fresh/server.js`.

### Docker

A `Dockerfile` is included. Build and run:

```bash
docker build -t music.build-site .
docker run -p 8000:8000 music.build-site
```
