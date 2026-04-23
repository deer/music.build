package build.music.server;

import build.serve.sse.SseEmitter;
import build.serve.sse.SseEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

final class ConsoleEventBus {

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    void waitUntilClosed(final SseEmitter emitter) throws InterruptedException {
        emitters.add(emitter);
        try {
            while (emitter.isOpen()) {
                Thread.sleep(500);
            }
        } finally {
            emitters.remove(emitter);
        }
    }

    void publish(final String event, final String data) {
        final List<SseEmitter> dead = new ArrayList<>();
        for (final SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEvent.of(event, data));
            } catch (final IOException e) {
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }
}
