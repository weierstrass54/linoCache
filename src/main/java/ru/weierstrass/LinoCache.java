package ru.weierstrass;

import java.util.ArrayList;
import java.util.Collection;
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
    private final ConcurrentHashMap<Object, ScheduledFuture> _cleanerTasks;

    private final long _ttl;
    private final ScheduledExecutorService _cleaner;

    LinoCache(int capacity) {
        this(capacity, 0);
    }

    LinoCache(int capacity, long ttl) {
        super(capacity);
        _ttl = ttl;
        _map = new ConcurrentHashMap<>(capacity);
        _cleaner = Executors.newScheduledThreadPool(1, r -> new Thread(r, "LinoCache-TTL"));
        _cleanerTasks = new ConcurrentHashMap<>();
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

    @Override
    public Object remove(Object key) {
        _map.keySet().forEach(strings -> _log.info("Cache keys: {}", strings));
        _log.debug("Received key: {}", key);
        Map<List<String>, Object> removes = _map.entrySet().stream()
                .filter(e -> e.getKey().contains(key))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        removes.forEach((innerKey, realKey) -> {
            _map.remove(innerKey);
            super.remove(realKey);
            _log.debug("Remove object with resolved key {} -> {} -> {}.", key, innerKey, realKey);
        });
        return true;
    }

    private void putMapKey(Object key, Object value) {
        List<String> mapKey = generateKey(value);
        _log.debug("Cache map key generated {} -> {}.", key, mapKey);
        _map.put(mapKey, key);
        setTtl(mapKey);
    }

    private List<String> generateKey(Object value) throws IllegalArgumentException {
        List<String> key = new ArrayList<>();
        if (value instanceof Collection) {
            ((Collection) value).forEach(o -> key.add(generateSingleKey(o)));
        } else {
            key.add(generateSingleKey(value));
        }
        return key;
    }

    private String generateSingleKey(Object value) throws IllegalArgumentException {
        if (value instanceof LinoIdentifiable) {
            return ((LinoIdentifiable) value).getKey();
        }
        throw new IllegalArgumentException(
                "Value should implements " + LinoIdentifiable.class.getName() + " interface.");
    }

    private void setTtl(List<String> key) {
        if (_ttl > 0) {
            if (_cleanerTasks.containsKey(key)) {
                _log.debug("Cancel TTL task for object with key: {}.", key);
                _cleanerTasks.get(key).cancel(true);
            }
            _log.debug("New TTL task for object with key {} and ttl {} ms.", key, _ttl);
            _cleanerTasks.put(key, _cleaner.schedule(() -> {
                Object realKey = _map.get(key);
                if (realKey != null) {
                    super.remove(realKey);
                }
            }, _ttl, TimeUnit.MILLISECONDS));
        }
    }

}
