package ru.weierstrass;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinoCache extends ConcurrentHashMap<Object, Object> {

    private static final Logger _log = LoggerFactory.getLogger(LinoCache.class);

    private final ConcurrentHashMap<List<String>, Object> _map;
    private final ConcurrentHashMap<Object, ScheduledFuture> _ttlTasks;
    private final KeyGenerator _generator;

    private final long _ttl;
    private final TimeUnit _unit;
    private final ScheduledExecutorService _cleaner;

    LinoCache(int capacity) {
        this(capacity, 0, TimeUnit.HOURS);
    }

    LinoCache(int capacity, long ttl, TimeUnit unit) {
        super(capacity);
        _ttl = ttl;
        _unit = unit;

        _map = new ConcurrentHashMap<>(capacity);
        _generator = new KeyGenerator();
        _cleaner = Executors.newScheduledThreadPool(1, r -> new Thread(r, "LinoCache-TTL"));
        _ttlTasks = new ConcurrentHashMap<>();
    }

    @Override
    public Object put(Object key, Object value) {
        putMapKey(key, value);
        return super.put(key, value);
    }

    @Override
    public Object putIfAbsent(Object key, Object value) {
        putMapKey(key, value);
        return super.putIfAbsent(key, value);
    }

    public void evict(String key) {
        Map<List<String>, Object> evictions = _map.entrySet().stream()
            .filter(entry -> entry.getKey().contains(key))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        evictions.forEach((innerKey, realKey) -> {
            _map.remove(innerKey);
            remove(realKey);
        });
    }

    private void putMapKey(Object key, Object value) {
        List<String> mapKey = _generator.generate(value);
        _log.debug("Cache map key generated {} -> {}.", key, mapKey);
        _map.put(mapKey, key);
        setTtl(key);
    }

    private void setTtl(Object key) {
        if (_ttl > 0) {
            if (_ttlTasks.containsKey(key)) {
                _log.debug("Cancel TTL task for object with key: {}.", key);
                _ttlTasks.get(key).cancel(true);
            }
            _log.debug("New TTL task for object with key {} and ttl {} {}", key, _ttl,
                _unit.toString());
            _ttlTasks.put(key, _cleaner.scheduleAtFixedRate(() -> remove(key), _ttl, _ttl, _unit));
        }
    }

}
