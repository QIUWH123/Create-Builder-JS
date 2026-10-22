package net.quiwh.cbjs.kinetics;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Keeps Create's public member map mutable while invalidating cached demand on edits. */
public final class StressTrackedMap<K, V> extends AbstractMap<K, V> {
    private final Map<K, V> delegate;
    private final Runnable invalidate;

    public StressTrackedMap(Map<K, V> delegate, Runnable invalidate) {
        this.delegate = Objects.requireNonNull(delegate);
        this.invalidate = Objects.requireNonNull(invalidate);
    }

    @Override public int size() { return delegate.size(); }
    @Override public boolean containsKey(Object key) { return delegate.containsKey(key); }
    @Override public boolean containsValue(Object value) { return delegate.containsValue(value); }
    @Override public V get(Object key) { return delegate.get(key); }

    @Override
    public V put(K key, V value) {
        boolean present = delegate.containsKey(key);
        V previous = delegate.put(key, value);
        if (!present || !Objects.equals(previous, value)) invalidate.run();
        return previous;
    }

    @Override
    public V remove(Object key) {
        boolean present = delegate.containsKey(key);
        V previous = delegate.remove(key);
        if (present) invalidate.run();
        return previous;
    }

    @Override
    public void clear() {
        if (delegate.isEmpty()) return;
        delegate.clear();
        invalidate.run();
    }

    private <T> Iterator<T> trackRemoval(Iterator<T> iterator) {
        return new Iterator<>() {
            @Override public boolean hasNext() { return iterator.hasNext(); }
            @Override public T next() { return iterator.next(); }
            @Override public void remove() {
                iterator.remove();
                invalidate.run();
            }
        };
    }

    // Native stress calculations iterate keys: avoid allocating an Entry wrapper per member.
    @Override
    public Set<K> keySet() {
        return new AbstractSet<>() {
            @Override public int size() { return delegate.size(); }
            @Override public boolean contains(Object key) { return delegate.containsKey(key); }
            @Override public Iterator<K> iterator() { return trackRemoval(delegate.keySet().iterator()); }
            @Override public void clear() { StressTrackedMap.this.clear(); }
        };
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new AbstractSet<>() {
            @Override public int size() { return delegate.size(); }
            @Override public void clear() { StressTrackedMap.this.clear(); }
            @Override public Iterator<Entry<K, V>> iterator() {
                Iterator<Entry<K, V>> iterator = trackRemoval(delegate.entrySet().iterator());
                return new Iterator<>() {
                    @Override public boolean hasNext() { return iterator.hasNext(); }
                    @Override public void remove() { iterator.remove(); }
                    @Override public Entry<K, V> next() {
                        Entry<K, V> entry = iterator.next();
                        return new Entry<>() {
                            @Override public K getKey() { return entry.getKey(); }
                            @Override public V getValue() { return entry.getValue(); }
                            @Override public V setValue(V value) {
                                V previous = entry.setValue(value);
                                if (!Objects.equals(previous, value)) invalidate.run();
                                return previous;
                            }
                            @Override public int hashCode() { return entry.hashCode(); }
                            @Override public boolean equals(Object other) { return entry.equals(other); }
                        };
                    }
                };
            }
        };
    }
}
