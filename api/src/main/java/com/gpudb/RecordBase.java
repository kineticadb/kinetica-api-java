package com.gpudb;

import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

/**
 * Abstract class that provides default implementations of most methods of
 * {@link Record}. Derived classes must at a minimum implement the
 * {@link #getType()}, {@link #get(int)}, and {@link #put(int, Object)} methods.
 */
public abstract class RecordBase implements Record {
    private final class RecordEntry implements Map.Entry<String, Object> {
        private final int index;

        public RecordEntry(int index) {
            this.index = index;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }

            Map.Entry<?, ?> e = (Map.Entry)o;

            Object k1 = getKey();
            Object k2 = e.getKey();

            if (k1 == k2 || (k1 != null && k1.equals(k2))) {
                Object v1 = getValue();
                Object v2 = e.getValue();

                if (v1 == v2 || (v1 != null && v1.equals(v2))) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public String getKey() {
            return RecordBase.this.getType().getColumn(index).getName();
        }

        @Override
        public Object getValue() {
            return RecordBase.this.get(index);
        }

        @Override
        public int hashCode() {
            Object value = getValue();
            return (getKey().hashCode() ^ (value == null ? 0 : value.hashCode()));
        }

        @Override
        public Object setValue(Object value) {
            Object oldValue = RecordBase.this.get(index);
            RecordBase.this.put(index, value);
            return oldValue;
        }

        @Override
        public String toString() {
            GenericData gd = GenericData.get();
            return gd.toString(getKey()) + ":" + gd.toString(getValue());
        }
    }

    private final class RecordEntryIterator implements Iterator<Map.Entry<String, Object>> {
        private int pos = 0;

        @Override
        public boolean hasNext() {
            return pos < RecordBase.this.getType().getColumnCount();
        }

        @Override
        public Map.Entry<String, Object> next() {
            return new RecordEntry(pos++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private final class RecordEntrySet extends AbstractSet<Map.Entry<String, Object>> {
        @Override
        public boolean add(Map.Entry<String, Object> o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends Map.Entry<String, Object>> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Iterator<Map.Entry<String, Object>> iterator() {
            return new RecordEntryIterator();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return RecordBase.this.getType().getColumnCount();
        }
    }

    private final class RecordMap extends AbstractMap<String, Object> {
        private final RecordEntrySet entrySet = new RecordEntrySet();

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsKey(Object key) {
            if (key == null) {
                throw new NullPointerException("Key cannot be null.");
            }

            if (!(key instanceof String)) {
                throw new ClassCastException("Key must be a string.");
            }

            return RecordBase.this.getType().getColumn((String)key) != null;
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return entrySet;
        }

        @Override
        public Object get(Object key) {
            if (key == null) {
                throw new NullPointerException("Key cannot be null.");
            }

            if (!(key instanceof String)) {
                throw new ClassCastException("Key must be a string.");
            }

            int columnIndex = RecordBase.this.getType().getColumnIndex((String)key);

            if (columnIndex == -1) {
                return null;
            }

            return RecordBase.this.get(columnIndex);
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Object put(String key, Object value) {
            if (key == null) {
                throw new NullPointerException("Key cannot be null.");
            }

            int columnIndex = RecordBase.this.getType().getColumnIndex(key);

            if (columnIndex == -1) {
                throw new IllegalArgumentException("Field " + key + " does not exist.");
            }

            Object oldValue = RecordBase.this.get(columnIndex);
            RecordBase.this.put(columnIndex, value);
            return oldValue;
        }

        @Override
        public Object remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return RecordBase.this.getType().getColumnCount();
        }

        @Override
        public String toString() {
            return RecordBase.this.toString();
        }
    }

    @Override
    public Schema getSchema() {
        return getType().getSchema();
    }

    @Override
    public Object get(String name) {
        int index = getType().getColumnIndex(name);

        if (index == -1) {
            return null;
        }

        return get(index);
    }

    @Override
    public ByteBuffer getBytes(int index)
    {
        return (ByteBuffer)get(index);
    }

    @Override
    public ByteBuffer getBytes(String name)
    {
        return (ByteBuffer)get(name);
    }

    @Override
    public Double getDouble(int index)
    {
        return (Double)get(index);
    }

    @Override
    public Double getDouble(String name)
    {
        return (Double)get(name);
    }

    @Override
    public Float getFloat(int index)
    {
        return (Float)get(index);
    }

    @Override
    public Float getFloat(String name)
    {
        return (Float)get(name);
    }

    @Override
    public Integer getInt(int index)
    {
        return (Integer)get(index);
    }

    @Override
    public Integer getInt(String name)
    {
        return (Integer)get(name);
    }

    @Override
    public Long getLong(int index)
    {
        return (Long)get(index);
    }

    @Override
    public Long getLong(String name)
    {
        return (Long)get(name);
    }

    @Override
    public String getString(int index)
    {
        return (String)get(index);
    }

    @Override
    public String getString(String name)
    {
        return (String)get(name);
    }

    @Override
    public void put(String name, Object value) {
        int index = getType().getColumnIndex(name);

        if (index == -1) {
            throw new IllegalArgumentException("Field " + name + " does not exist.");
        }

        put(index, value);
    }

    @Override
    public Map<String, Object> getDataMap() {
        return new RecordMap();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        DynamicTableRecord that = (DynamicTableRecord)obj;

        if (!that.getType().equals(this.getType())) {
            return false;
        }

        int columnCount = this.getType().getColumnCount();

        for (int i = 0; i < columnCount; i++) {
            if (!Objects.equals(that.get(i), this.get(i))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        int columnCount = getType().getColumnCount();

        for (int i = 0; i < columnCount; i++) {
            hashCode = 31 * hashCode;
            Object value = get(i);

            if (value != null) {
                hashCode += value.hashCode();
            }
        }

        return hashCode;
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        Type type = getType();
        int columnCount = type.getColumnCount();

        for (int i = 0; i < columnCount; i++) {
            if (i > 0) {
                builder.append(",");
            }

            builder.append(gd.toString(type.getColumn(i).getName()));
            builder.append(":");
            builder.append(gd.toString(get(i)));
        }

        builder.append("}");
        return builder.toString();
    }
}