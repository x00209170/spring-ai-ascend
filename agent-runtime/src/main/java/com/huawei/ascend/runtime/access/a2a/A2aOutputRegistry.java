package com.huawei.ascend.runtime.access.a2a;

import com.huawei.ascend.runtime.common.AgentResponseEvent;
import com.huawei.ascend.runtime.common.RuntimeIdentity;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import reactor.core.Disposable;
import reactor.core.publisher.Sinks;

public final class A2aOutputRegistry {

    private final ConcurrentMap<Key, Entry> entries = new ConcurrentHashMap<>();

    public void append(RuntimeIdentity id, A2aOutput output) {
        Key key = key(id);
        Entry entry = entries.computeIfAbsent(key, ignored -> new Entry());
        synchronized (entry) {
            if (entry.terminal) return;
            entry.outputs.add(output);
            entry.sink.tryEmitNext(output).orThrow();
            if (output.terminal()) {
                entry.terminal = true;
                entry.sink.tryEmitComplete().orThrow();
            }
        }
    }

    public List<A2aOutput> list(RuntimeIdentity id) {
        Entry entry = entries.get(key(id));
        return entry == null ? List.of() : List.copyOf(entry.outputs);
    }

    public Runnable subscribe(RuntimeIdentity id, Consumer<A2aOutput> subscriber) {
        return entries.computeIfAbsent(key(id), ignored -> new Entry())
                .sink.asFlux().subscribe(subscriber::accept)::dispose;
    }

    private static Key key(RuntimeIdentity id) {
        return new Key(id.tenantId(), id.sessionId(), id.taskId());
    }

    private record Key(String tenantId, String sessionId, String taskId) {}

    private static class Entry {
        final CopyOnWriteArrayList<A2aOutput> outputs = new CopyOnWriteArrayList<>();
        final Sinks.Many<A2aOutput> sink = Sinks.many().replay().all();
        volatile boolean terminal;
    }
}
