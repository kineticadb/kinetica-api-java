package com.gisfederal.gpudb;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

/**
 * Abstract base class for objects that contain {@link Record} data with a
 * schema defined at compile time. Derived classes should correspond to a GPUdb
 * type and contain public fields, annotated with {@link RecordObject.Column},
 * that correspond to columns in that type. The {@link RecordObject.Type}
 * annotation may also be applied to derived classes to provide additional
 * information about the type.
 */
public abstract class RecordObject implements Record {
    /**
     * Indicates that a public field is a GPUdb type column.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Column {
        /**
         * The name of the column. If not specified, the field name will be
         * used.
         */
        public String name() default "";

        /**
         * The order of the column relative to other columns. If two columns
         * have the same value for order, they will be sorted by name.
         */
        public int order();

        /**
         * A list of properties that are applicable to the column.
         *
         * @see ColumnProperty
         */
        public String[] properties() default {};
    }

    /**
     * Provides additional information about a GPUdb type.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Type {
        /**
         * The label of the GPUdb type.
         */
        public String label();
    }

    private static class Index {
        public final com.gisfederal.gpudb.Type type;
        public final Schema schema;
        public final Field[] fields;
        public final HashMap<String, Field> fieldMap;

        public Index(Class<?> c) {
            if (Modifier.isAbstract(c.getModifiers())) {
                throw new GPUdbRuntimeException("Class " + c.getName() + " must not be abstract.");
            }

            if (!Modifier.isPublic(c.getModifiers())) {
                throw new GPUdbRuntimeException("Class " + c.getName() + " must be public.");
            }

            String label;
            Type typeAnnotation = c.getAnnotation(Type.class);

            if (typeAnnotation != null) {
                label = typeAnnotation.label();
            } else {
                label = c.getSimpleName();
            }

            ArrayList<Field> fieldList = new ArrayList<>();
            fieldMap = new HashMap<>();

            for (Field field : c.getFields()) {
                if (field.getAnnotation(Column.class) == null) {
                    continue;
                }

                if (Modifier.isFinal(field.getModifiers())) {
                    throw new GPUdbRuntimeException("Field " + c.getName() + "." + field.getName() + " must not be final.");
                }

                fieldList.add(field);
                fieldMap.put(field.getName(), field);
            }

            if (fieldList.isEmpty()) {
                throw new GPUdbRuntimeException("Class " + c.getName() + " must have at least one column.");
            }

            Collections.sort(fieldList, new Comparator<Field>() {
                @Override
                public int compare(Field a, Field b) {
                    Column aAnnotation = a.getAnnotation(Column.class);
                    Column bAnnotation = b.getAnnotation(Column.class);
                    int aOrder = aAnnotation.order();
                    int bOrder = bAnnotation.order();

                    if (aOrder != bOrder) {
                        return aOrder - bOrder;
                    } else {
                        String aName = aAnnotation.name();

                        if (aName.isEmpty()) {
                            aName = a.getName();
                        }

                        String bName = bAnnotation.name();

                        if (bName.isEmpty()) {
                            bName = b.getName();
                        }

                        return aName.compareTo(bName);
                    }
                }
            });

            ArrayList<com.gisfederal.gpudb.Type.Column> columns = new ArrayList<>();

            try {
                for (Field field : fieldList) {
                    Column columnAnnotation = field.getAnnotation(Column.class);
                    Class<?> columnType = field.getType();

                    if (columnType == Double.TYPE) {
                        columnType = Double.class;
                    } else if (columnType == Float.TYPE) {
                        columnType = Float.class;
                    } else if (columnType == Integer.TYPE) {
                        columnType = Integer.class;
                    } else if (columnType == Long.TYPE) {
                        columnType = Long.class;
                    }

                    columns.add(new com.gisfederal.gpudb.Type.Column(
                            columnAnnotation.name().isEmpty() ? field.getName() : columnAnnotation.name(),
                            columnType, columnAnnotation.properties()));
                }

                type = new com.gisfederal.gpudb.Type(label, columns);
            } catch (IllegalArgumentException ex) {
                throw new GPUdbRuntimeException(ex.getMessage());
            }

            schema = type.getSchema();
            fields = new Field[fieldList.size()];
            fieldList.toArray(fields);
        }
    }

    private static final Map<Class<?>, Index> indexes = new ConcurrentHashMap<>(16, 0.75f, 1);

    private static <T extends RecordObject> Index getIndex(Class<T> type) {
        Index index = indexes.get(type);

        if (index != null) {
            return index;
        }

        index = new Index(type);
        indexes.put(type, index);
        return index;
    }

    public static <T extends RecordObject> Schema getSchema(Class<T> type) {
        return getIndex(type).schema;
    }

    public static <T extends RecordObject> com.gisfederal.gpudb.Type getType(Class<T> type) {
        return getIndex(type).type;
    }

    public static <T extends RecordObject> String createType(Class<T> type, GPUdb gpudb) throws GPUdbException {
        String typeId = getIndex(type).type.create(gpudb);
        gpudb.addKnownType(typeId, type);
        return typeId;
    }

    private final Index index;

    /**
     * Creates a new record object instance.
     */
    protected RecordObject() {
        index = getIndex(getClass());
    }

    /**
     * Returns the Avro schema of the object.
     *
     * @return  the Avro schema
     */
    @Override
    public Schema getSchema() {
        return index.schema;
    }

    /**
     * Returns the GPUdb type of the object.
     *
     * @return  the GPUdb type
     */
    public com.gisfederal.gpudb.Type getType() {
        return index.type;
    }

    /**
     * Returns the value of the specified field.
     *
     * @param index  the index of the field
     * @return       the value of the field
     *
     * @throws IndexOutOfBoundsException if the specified index is not valid
     */
    @Override
    public Object get(int index) {
        try {
            return this.index.fields[index].get(this);
        } catch (IllegalAccessException ex) {
            throw new GPUdbRuntimeException("Could not get field value for " + this.index.fields[index].getName() + ".", ex);
        }
    }

    /**
     * Returns the value of the specified field.
     *
     * @param name  the name of the field
     * @return      the value of the field
     *
     * @throws GPUdbRuntimeException if no field exists with the specified name
     */
    @Override
    public Object get(String name) {
        Field field = index.fieldMap.get(name);

        if (field == null) {
            throw new GPUdbRuntimeException("Field " + name + " does not exist.");
        }

        try {
            return field.get(this);
        } catch (IllegalAccessException ex) {
            throw new GPUdbRuntimeException("Could not get field value for " + name + ".", ex);
        }
    }

    /**
     * Returns the value of the specified field cast to a {@link ByteBuffer}.
     * If the field is not of the correct type an exception will be thrown.
     *
     * @param index  the index of the field
     * @return       the value of the field
     *
     * @throws ClassCastException if the field is not of the correct type
     *
     * @throws IndexOutOfBoundsException if the specified index is not valid
     */
    @Override
    public ByteBuffer getBytes(int index)
    {
        return (ByteBuffer)get(index);
    }

    /**
     * Returns the value of the specified field cast to a {@link ByteBuffer}.
     * If the field is not of the correct type an exception will be thrown.
     *
     * @param name  the name of the field
     * @return      the value of the field
     *
     * @throws ClassCastException if the field is not of the correct type
     *
     * @throws GPUdbRuntimeException if no field exists with the specified name
     */
    @Override
    public ByteBuffer getBytes(String name)
    {
        return (ByteBuffer)get(name);
    }

    /**
     * Returns the value of the specified field cast to a {@link Double}.
     * If the field is not of the correct type an exception will be thrown.
     *
     * @param index  the index of the field
     * @return       the value of the field
     *
     * @throws ClassCastException if the field is not of the correct type
     *
     * @throws IndexOutOfBoundsException if the specified index is not valid
     */
    @Override
    public Double getDouble(int index)
    {
        return (Double)get(index);
    }

    /**
     * Returns the value of the specified field cast to a {@link Double}.
     * If the field is not of the correct type an exception will be thrown.
     *
     * @param name  the name of the field
     * @return      the value of the field
     *
     * @throws ClassCastException if the field is not of the correct type
     *
     * @throws GPUdbRuntimeException if no field exists with the specified name
     */
    @Override
    public Double getDouble(String name)
    {
        return (Double)get(name);
    }

    /**
     * Returns the value of the specified field cast to a {@link Float}.
     * If the field is not of the correct type an exception will be thrown.
     *
     * @param index  the index of the field
     * @return       the value of the field
     *
     * @throws ClassCastException if the field is not of the correct type
     *
     * @throws IndexOutOfBoundsException if the specified index is not valid
     */
    @Override
    public Float getFloat(int index)
    {
        return (Float)get(index);
    }

    /**
     * Returns the value of the specified field cast to a {@link Float}.
     * If the field is not of the correct type an exception will be thrown.
     *
     * @param name  the name of the field
     * @return      the value of the field
     *
     * @throws ClassCastException if the field is not of the correct type
     *
     * @throws GPUdbRuntimeException if no field exists with the specified name
     */
    @Override
    public Float getFloat(String name)
    {
        return (Float)get(name);
    }

    /**
     * Returns the value of the specified field cast to a {@link Integer}.
     * If the field is not of the correct type an exception will be thrown.
     *
     * @param index  the index of the field
     * @return       the value of the field
     *
     * @throws ClassCastException if the field is not of the correct type
     *
     * @throws IndexOutOfBoundsException if the specified index is not valid
     */
    @Override
    public Integer getInt(int index)
    {
        return (Integer)get(index);
    }

    /**
     * Returns the value of the specified field cast to a {@link Integer}.
     * If the field is not of the correct type an exception will be thrown.
     *
     * @param name  the name of the field
     * @return      the value of the field
     *
     * @throws ClassCastException if the field is not of the correct type
     *
     * @throws GPUdbRuntimeException if no field exists with the specified name
     */
    @Override
    public Integer getInt(String name)
    {
        return (Integer)get(name);
    }

    /**
     * Returns the value of the specified field cast to a {@link Long}.
     * If the field is not of the correct type an exception will be thrown.
     *
     * @param index  the index of the field
     * @return       the value of the field
     *
     * @throws ClassCastException if the field is not of the correct type
     *
     * @throws IndexOutOfBoundsException if the specified index is not valid
     */
    @Override
    public Long getLong(int index)
    {
        return (Long)get(index);
    }

    /**
     * Returns the value of the specified field cast to a {@link Long}.
     * If the field is not of the correct type an exception will be thrown.
     *
     * @param name  the name of the field
     * @return      the value of the field
     *
     * @throws ClassCastException if the field is not of the correct type
     *
     * @throws GPUdbRuntimeException if no field exists with the specified name
     */
    @Override
    public Long getLong(String name)
    {
        return (Long)get(name);
    }

    /**
     * Returns the value of the specified field cast to a {@link String}.
     * If the field is not of the correct type an exception will be thrown.
     *
     * @param index  the index of the field
     * @return       the value of the field
     *
     * @throws ClassCastException if the field is not of the correct type
     *
     * @throws IndexOutOfBoundsException if the specified index is not valid
     */
    @Override
    public String getString(int index)
    {
        return (String)get(index);
    }

    /**
     * Returns the value of the specified field cast to a {@link String}.
     * If the field is not of the correct type an exception will be thrown.
     *
     * @param name  the name of the field
     * @return      the value of the field
     *
     * @throws ClassCastException if the field is not of the correct type
     *
     * @throws GPUdbRuntimeException if no field exists with the specified name
     */
    @Override
    public String getString(String name)
    {
        return (String)get(name);
    }

    /**
     * Sets the value of the specified field.
     *
     * @param index  the index of the field
     * @param value  the new value
     *
     * @throws IllegalArgumentException if the value is not of the correct type
     *
     * @throws IndexOutOfBoundsException if the specified index is not valid
     */
    @Override
    public void put(int index, Object value) {
        try {
            this.index.fields[index].set(this, value);
        } catch (IllegalAccessException ex) {
            throw new GPUdbRuntimeException("Could not set field value for " + this.index.fields[index].getName() + ".", ex);
        }
    }

    /**
     * Sets the value of the specified field.
     *
     * @param name   the name of the field
     * @param value  the new value
     *
     * @throws GPUdbRuntimeException if no field exists with the specified name
     *
     * @throws IllegalArgumentException if the value is not of the correct type
     */
    @Override
    public void put(String name, Object value) {
        Field field = index.fieldMap.get(name);

        if (field == null) {
            throw new GPUdbRuntimeException("Field " + name + " does not exist.");
        }

        try {
            field.set(this, value);
        } catch (IllegalAccessException ex) {
            throw new GPUdbRuntimeException("Could not set field value for " + name + ".", ex);
        }
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param obj  the reference object with which to compare
     * @return     {@code true} if this object is the same as {@code obj};
     *             {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        RecordObject that = (RecordObject)obj;

        for (int i = index.type.getColumns().size() - 1; i >= 0; i--) {
            if (!this.get(i).equals(that.get(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return  the hash code value
     */
    @Override
    public int hashCode() {
        return GenericData.get().hashCode(this, index.schema);
    }

    /**
     * Returns a string representation of the object.
     *
     * @return  the string representation
     */
    @Override
    public String toString() {
        return GenericData.get().toString(this);
    }
}