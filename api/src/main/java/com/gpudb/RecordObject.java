package com.gpudb;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.avro.Schema;

/**
 * Abstract base class for objects that contain {@link Record} data with a
 * schema defined at compile time. Derived classes should correspond to a GPUdb
 * type and contain public fields, annotated with {@link RecordObject.Column},
 * that correspond to columns in that type. The {@link RecordObject.Type}
 * annotation may also be applied to derived classes to provide additional
 * information about the type.
 */
public abstract class RecordObject extends RecordBase {
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

    private static final class Index {
        public final com.gpudb.Type type;
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

            ArrayList<com.gpudb.Type.Column> columns = new ArrayList<>();

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

                    columns.add(new com.gpudb.Type.Column(
                            columnAnnotation.name().isEmpty() ? field.getName() : columnAnnotation.name(),
                            columnType, columnAnnotation.properties()));
                }

                type = new com.gpudb.Type(label, columns);
            } catch (IllegalArgumentException ex) {
                throw new GPUdbRuntimeException(ex.getMessage());
            }

            fields = new Field[fieldList.size()];
            fieldList.toArray(fields);
        }
    }

    private static final Map<Class<?>, Index> INDEXES = new ConcurrentHashMap<>(16, 0.75f, 1);

    private static Index getIndex(Class<? extends RecordObject> type) {
        Index index = INDEXES.get(type);

        if (index != null) {
            return index;
        }

        index = new Index(type);
        INDEXES.put(type, index);
        return index;
    }

    /**
     * Gets the {@link Type} object corresponding to the metadata in the
     * specified {@link RecordObject} class.
     *
     * @param type  the {@link RecordObject} class from which to obtain metadata
     * @return      the corresponding {@link Type} object
     */
    public static com.gpudb.Type getType(Class<? extends RecordObject> type) {
        return getIndex(type).type;
    }

    /**
     * Gets the Avro record schema corresponding to the metadata in the
     * specified {@link RecordObject} class.
     *
     * @param type  the {@link RecordObject} class from which to obtain metadata
     * @return      the corresponding Avro record schema
     */
    public static Schema getSchema(Class<? extends RecordObject> type) {
        return getIndex(type).type.getSchema();
    }

    /**
     * Creates a type in GPUdb based on the metadata in the specified
     * {@link RecordObject} class and returns the type ID for reference. If an
     * identical type already exists in GPUdb, the type ID of the existing type
     * will be returned and no new type will be created. The specified class
     * will also automatically be added as a {@link GPUdbBase#addKnownType(
     * String, Object) known type} in the specified {@link GPUdb} instance.
     *
     * @param type   the {@link RecordObject} class from which to obtain
     *               metadata
     * @param gpudb  the {@link GPUdb} instance in which to create the type
     * @return       the type ID of the type in GPUdb
     *
     * @throws GPUdbException if an error occurs while creating the type
     */
    public static String createType(Class<? extends RecordObject> type, GPUdb gpudb) throws GPUdbException {
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

    @Override
    public com.gpudb.Type getType() {
        return index.type;
    }

    @Override
    public Object get(int index) {
        try {
            return this.index.fields[index].get(this);
        } catch (IllegalAccessException ex) {
            throw new GPUdbRuntimeException("Could not get field value for " + this.index.fields[index].getName() + ".", ex);
        }
    }

    @Override
    public void put(int index, Object value) {
        try {
            this.index.fields[index].set(this, value);
        } catch (IllegalAccessException ex) {
            throw new GPUdbRuntimeException("Could not set field value for " + this.index.fields[index].getName() + ".", ex);
        }
    }
}