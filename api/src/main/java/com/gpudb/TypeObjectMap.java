package com.gpudb;

import com.gpudb.Type.Column;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.avro.Schema;

public final class TypeObjectMap<T> {
    private enum DataType {
        BOOLEAN, BYTE, BYTEARRAY, BYTEBUFFER, DOUBLE, FLOAT, INTEGER, LONG, SHORT, STRING;

        public static DataType fromClass(Class<?> c) {
            if (c == Boolean.class || c == Boolean.TYPE) {
                return DataType.BOOLEAN;
            } else if (c == Byte.class || c == Byte.TYPE) {
                return DataType.BYTE;
            } else if (c == Byte[].class || c == byte[].class) {
                return DataType.BYTEARRAY;
            } else if (c == ByteBuffer.class) {
                return DataType.BYTEBUFFER;
            } else if (c == Double.class || c == Double.TYPE) {
                return DataType.DOUBLE;
            } else if (c == Float.class || c == Float.TYPE) {
                return DataType.FLOAT;
            } else if (c == Integer.class || c == Integer.TYPE) {
                return DataType.INTEGER;
            } else if (c == Long.class || c == Long.TYPE) {
                return DataType.LONG;
            } else if (c == Short.class || c == Short.TYPE) {
                return DataType.SHORT;
            } else if (c == String.class) {
                return DataType.STRING;
            } else {
                return null;
            }
        }

        public Type.Column toColumn(String name) {
            switch (this) {
                case BOOLEAN:
                case BYTE:
                    return new Type.Column(name, Integer.class, ColumnProperty.INT8);

                case BYTEARRAY:
                case BYTEBUFFER:
                    return new Type.Column(name, ByteBuffer.class);

                case INTEGER:
                    return new Type.Column(name, Integer.class);

                case SHORT:
                    return new Type.Column(name, Integer.class, ColumnProperty.INT16);

                case DOUBLE:
                    return new Type.Column(name, Double.class);

                case FLOAT:
                    return new Type.Column(name, Float.class);

                case LONG:
                    return new Type.Column(name, Long.class);

                case STRING:
                    return new Type.Column(name, String.class);

                default:
                    return null;
            }
        }
    }

    private static Object convert(Object value, DataType fromType, DataType toType) {
        if (value != null) {
            switch (fromType) {
                case BOOLEAN:
                    switch (toType) {
                        case BYTE:
                            value = (byte)((boolean)value ? 1 : 0);
                            break;

                        case DOUBLE:
                            value = (boolean)value ? 1.0 : 0.0;
                            break;

                        case FLOAT:
                            value = (boolean)value ? 1.0f : 0.0f;
                            break;

                        case INTEGER:
                            value = (boolean)value ? 1 : 0;
                            break;

                        case LONG:
                            value = (boolean)value ? 1l : 0l;
                            break;

                        case SHORT:
                            value = (short)((boolean)value ? 1 : 0);
                            break;
                    }

                    break;

                case BYTE:
                    switch (toType) {
                        case BOOLEAN:
                            value = (byte)value != 0;
                            break;

                        case DOUBLE:
                            value = (double)(byte)value;
                            break;

                        case FLOAT:
                            value = (float)(byte)value;
                            break;

                        case INTEGER:
                            value = (int)(byte)value;
                            break;

                        case LONG:
                            value = (long)(byte)value;
                            break;

                        case SHORT:
                            value = (short)(byte)value;
                            break;
                    }

                    break;

                case BYTEARRAY:
                    switch (toType) {
                        case BYTEBUFFER:
                            value = ByteBuffer.wrap((byte[])value);
                            break;
                    }

                    break;

                case BYTEBUFFER:
                    switch (toType) {
                        case BYTEARRAY:
                            value = ((ByteBuffer)value).array();
                            break;
                    }

                    break;

                case DOUBLE:
                    switch (toType) {
                        case BOOLEAN:
                            value = ((double)value) != 0.0;
                            break;

                        case BYTE:
                            value = (byte)(double)value;
                            break;

                        case FLOAT:
                            value = (float)(double)value;
                            break;

                        case INTEGER:
                            value = (int)(double)value;
                            break;

                        case LONG:
                            value = (long)(double)value;
                            break;

                        case SHORT:
                            value = (short)(double)value;
                            break;
                    }

                    break;

                case FLOAT:
                    switch (toType) {
                        case BOOLEAN:
                            value = ((float)value) != 0.0f;
                            break;

                        case BYTE:
                            value = (byte)(float)value;
                            break;

                        case DOUBLE:
                            value = (double)(float)value;
                            break;

                        case INTEGER:
                            value = (int)(float)value;
                            break;

                        case LONG:
                            value = (long)(float)value;
                            break;

                        case SHORT:
                            value = (short)(float)value;
                            break;
                    }

                    break;

                case INTEGER:
                    switch (toType) {
                        case BOOLEAN:
                            value = ((int)value) != 0;
                            break;

                        case BYTE:
                            value = (byte)(int)value;
                            break;

                        case DOUBLE:
                            value = (double)(int)value;
                            break;

                        case FLOAT:
                            value = (float)(int)value;
                            break;

                        case LONG:
                            value = (long)(int)value;
                            break;

                        case SHORT:
                            value = (short)(int)value;
                            break;
                    }

                    break;

                case LONG:
                    switch (toType) {
                        case BOOLEAN:
                            value = ((long)value) != 0l;
                            break;

                        case BYTE:
                            value = (byte)(long)value;
                            break;

                        case DOUBLE:
                            value = (double)(long)value;
                            break;

                        case FLOAT:
                            value = (float)(long)value;
                            break;

                        case INTEGER:
                            value = (int)(long)value;
                            break;

                        case SHORT:
                            value = (short)(long)value;
                            break;
                    }

                    break;

                case SHORT:
                    switch (toType) {
                        case BOOLEAN:
                            value = ((short)value) != 0;
                            break;

                        case BYTE:
                            value = (byte)(short)value;
                            break;

                        case DOUBLE:
                            value = (double)(short)value;
                            break;

                        case FLOAT:
                            value = (float)(short)value;
                            break;

                        case INTEGER:
                            value = (int)(short)value;
                            break;

                        case LONG:
                            value = (long)(short)value;
                            break;
                    }

                    break;
            }
        }

        return value;
    }

    /**
     * A factory that creates new instances of the specified class.
     *
     * @param <T>  the class that the factory creates instances of
     */
    public interface Factory<T> {
        /**
         * Creates a new object instance.
         *
         * @return  the new instance
         */
        T newInstance();
    }

    /**
     * A factory that uses reflection to create new instances of the specified
     * class. The class must have a public, parameterless constructor.
     *
     * @param <T>  the class that the factory creates instances of
     */
    public static final class ClassFactory<T> implements Factory<T> {
        private final Class<?> objectClass;

        /**
         * Creates a {@link ClassFactory} for the specified class. The class
         * must have a public, parameterless constructor.
         *
         * @param objectClass  the class that the factory creates instances of
         */
        public ClassFactory(Class<?> objectClass) {
            this.objectClass = objectClass;
        }

        /**
         * Creates a new object instance.
         *
         * @return  the new instance
         *
         * @throws GPUdbRuntimeException if an instance could not be created
         */
        @Override
        @SuppressWarnings("unchecked")
        public T newInstance() {
            try {
                return (T)objectClass.newInstance();
            } catch (IllegalAccessException | InstantiationException ex) {
                throw new GPUdbRuntimeException("Could not create " + objectClass.getName() + " instance.", ex);
            }
        }
    }

    /**
     * An accessor that returns a value from an object.
     */
    public interface Accessor {
        /**
         * Returns a value from an object.
         *
         * @param o  the object from which to return the value
         * @return   the value
         */
        Object get(Object o);
    }

    /**
     * A mutator that sets a value in an object.
     */
    public interface Mutator {
        /**
         * Sets a value in an object.
         *
         * @param o      the object in which to set the value
         * @param value  the value
         */
        void set(Object o, Object value);
    }

    /**
     * A combined {@link Accessor} and {@link Mutator} that provides access to a
     * specified field using reflection. The field must be public.
     */
    public static final class FieldHandler implements Accessor, Mutator {
        private final Field field;

        /**
         * Creates a {@link FieldHandler} for the specified field.
         *
         * @param field  the field
         */
        public FieldHandler(Field field) {
            this.field = field;
        }

        /**
         * Returns the value of the field specified in the constructor from the
         * specified object. The object must belong to the class containing the
         * field.
         *
         * @param o  the object
         * @return   the value of the field
         *
         * @throws GPUdbRuntimeException if the value could not be retrieved
         */
        @Override
        public Object get(Object o) {
            try {
                return field.get(o);
            } catch (IllegalAccessException ex) {
                throw new GPUdbRuntimeException("Could not get field value for " + field.getName() + ".", ex);
            }
        }

        /**
         * Sets the value of the field specified in the constructor in the
         * specified object. The object must belong to the class containing the
         * field.
         *
         * @param o      the object
         * @param value  the new value of the field
         *
         * @throws GPUdbRuntimeException if the value could not be set
         */
        @Override
        public void set(Object o, Object value) {
            try {
                field.set(o, value);
            } catch (IllegalAccessException ex) {
                throw new GPUdbRuntimeException("Could not set field value for " + field.getName() + ".", ex);
            }
        }
    }

    /**
     * A combined {@link Accessor} and {@link Mutator} that provides access to a
     * specified field using reflection, converting the value to and from the
     * data type of a specified GPUdb {@link Type.Column column}.
     */
    public static final class ConvertingFieldHandler implements Accessor, Mutator {
        private final Field field;
        private final DataType fieldType;
        private final DataType columnType;

        /**
         * Creates a {@link ConvertingFieldHandler} for the specified field and
         * GPUdb {@link Type.Column column}. Field values will be converted to
         * and from the data type of the column.
         *
         * @param field   the field
         * @param column  the column
         *
         * @throws IllegalArgumentException if the field does not have a data
         * type that is supported for conversion
         */
        public ConvertingFieldHandler(Field field, Column column) {
            this.field = field;
            fieldType = DataType.fromClass(field.getType());

            if (fieldType == null) {
                throw new IllegalArgumentException("Field " + field.getName()
                        + " has unsupported type " + field.getType().getName() + ".");
            }

            columnType = DataType.fromClass(column.getType());
        }

        /**
         * Returns the value of the field specified in the constructor from the
         * specified object, converting it to the data type of the GPUdb
         * {@link Type.Column column} specified in the constructor. The object
         * must belong to the class containing the field.
         *
         * @param o  the object
         * @return   the converted value of the field
         *
         * @throws GPUdbRuntimeException if the value could not be retrieved
         */
        @Override
        public Object get(Object o) {
            try {
                return convert(field.get(o), fieldType, columnType);
            } catch (IllegalAccessException ex) {
                throw new GPUdbRuntimeException("Could not get field value for " + field.getName() + ".", ex);
            }
        }

        /**
         * Sets the value of the field specified in the constructor in the
         * specified object after converting it from the data type of the GPUdb
         * {@link Type.Column column} specified in the constructor. The object
         * must belong to the class containing the field.
         *
         * @param o      the object
         * @param value  the unconverted new value of the field
         *
         * @throws GPUdbRuntimeException if the value could not be set
         */
        @Override
        public void set(Object o, Object value) {
            try {
                field.set(o, convert(value, columnType, fieldType));
            } catch (IllegalAccessException ex) {
                throw new GPUdbRuntimeException("Could not set field value for " + field.getName() + ".", ex);
            }
        }
    }

    /**
     * A combined {@link Accessor} and {@link Mutator} that provides access to a
     * field via specified get and set methods using reflection. The get method
     * must be public and take no parameters, and the set method must be public
     * and take one parameter of the same type returned by the get method.
     */
    public static final class MethodHandler implements Accessor, Mutator {
        private final Method getMethod;
        private final Method setMethod;

        /**
         * Creates a {@link MethodHandler} for the specified get and set
         * methods. The get method must be public and take no parameters, and
         * the set method must be public and take one parameter of the same type
         * returned by the get method.
         *
         * @param getMethod  the get method
         * @param setMethod  the set method
         *
         * @throws IllegalArgumentException if the specified methods are not of
         * the correct form
         */
        public MethodHandler(Method getMethod, Method setMethod) {
            if (getMethod.getParameterTypes().length != 0
                    || getMethod.getReturnType() == Void.TYPE) {
                throw new IllegalArgumentException("Get method " + getMethod.getName()
                        + " must take no parameters and return a value.");
            }

            if (setMethod.getParameterTypes().length != 1) {
                throw new IllegalArgumentException("Set method " + setMethod.getName()
                        + " must take one parameter.");
            }

            if (getMethod.getReturnType() != setMethod.getParameterTypes()[0]) {
                throw new IllegalArgumentException("Set method " + setMethod.getName()
                        + " must take one parameter of the same type returned by get method "
                        + getMethod.getName() + ".");
            }

            this.getMethod = getMethod;
            this.setMethod = setMethod;
        }

        /**
         * Returns a value from the specified object via the get method
         * specified in the constructor. The object must belong to the class
         * containing the method.
         *
         * @param o  the object
         * @return   the value returned by the get method
         *
         * @throws GPUdbRuntimeException if the value could not be retrieved
         */
        @Override
        public Object get(Object o) {
            try {
                return getMethod.invoke(o);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new GPUdbRuntimeException("Could not call " + getMethod.getName() + ".", ex);
            }
        }

        /**
         * Sets a value in the specified object via the set method specified in
         * the constructor. The object must belong to the class containing the
         * method.
         *
         * @param o      the object
         * @param value  the new value of the field
         *
         * @throws GPUdbRuntimeException if the value could not be set
         */
        @Override
        public void set(Object o, Object value) {
            try {
                setMethod.invoke(o, value);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new GPUdbRuntimeException("Could not call " + setMethod.getName() + ".", ex);
            }
        }
    }

    /**
     * A combined {@link Accessor} and {@link Mutator} that provides access to a
     * field via specified get and set methods using reflection, converting the
     * value to and from the data type of a specified GPUdb {@link Type.Column
     * column}. The get method must be public and take no parameters, and the
     * set method must be public and take one parameter of the same type
     * returned by the get method.
     */
    public static final class ConvertingMethodHandler implements Accessor, Mutator {
        private final Method getMethod;
        private final Method setMethod;
        private final DataType fieldType;
        private final DataType columnType;

        /**
         * Creates a {@link ConvertingMethodHandler} for the specified get and
         * set methods and GPUdb {@link Type.Column column}. Field values will
         * be converted to and from the data type of the column.
         *
         * @param getMethod  the get method
         * @param setMethod  the set method
         * @param column     the column
         *
         * @throws IllegalArgumentException if the specified methods are not of
         * the correct form or the field does not have a data type that is
         * supported for conversion
         */
        public ConvertingMethodHandler(Method getMethod, Method setMethod, Column column) {
            if (getMethod.getParameterTypes().length != 0
                    || getMethod.getReturnType() == Void.TYPE) {
                throw new IllegalArgumentException("Get method " + getMethod.getName()
                        + " must take no parameters and return a value.");
            }

            if (setMethod.getParameterTypes().length != 1) {
                throw new IllegalArgumentException("Set method " + setMethod.getName()
                        + " must take one parameter.");
            }

            if (getMethod.getReturnType() != setMethod.getParameterTypes()[0]) {
                throw new IllegalArgumentException("Set method " + setMethod.getName()
                        + " must take one parameter of the same type returned by get method "
                        + getMethod.getName() + ".");
            }

            this.getMethod = getMethod;
            this.setMethod = setMethod;
            fieldType = DataType.fromClass(getMethod.getReturnType());

            if (fieldType == null) {
                throw new IllegalArgumentException("Get method " + getMethod.getName()
                        + " returns unsupported type " + getMethod.getReturnType().getName() + ".");
            }

            columnType = DataType.fromClass(column.getType());
        }

        /**
         * Returns a value from the specified object via the get method
         * specified in the constructor, converting it to the data type of the
         * GPUdb {@link Type.Column column} specified in the constructor. The
         * object must belong to the class containing the method.
         *
         * @param o  the object
         * @return   the value returned by the get method
         *
         * @throws GPUdbRuntimeException if the value could not be retrieved
         */
        @Override
        public Object get(Object o) {
            try {
                return convert(getMethod.invoke(o), fieldType, columnType);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new GPUdbRuntimeException("Could not call " + getMethod.getName() + ".", ex);
            }
        }

        /**
         * Sets a value in the specified object via the set method specified in
         * the constructor after converting it from the data type of the GPUdb
         * {@link Type.Column column} specified in the constructor. The object
         * must belong to the class containing the method.
         *
         * @param o      the object
         * @param value  the new value of the field
         *
         * @throws GPUdbRuntimeException if the value could not be set
         */
        @Override
        public void set(Object o, Object value) {
            try {
                setMethod.invoke(o, convert(value, columnType, fieldType));
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new GPUdbRuntimeException("Could not call " + setMethod.getName() + ".", ex);
            }
        }
    }

    /**
     * Creates a {@link TypeObjectMap} based on the specified class. All public
     * fields and get/set method pairs with supported data types will be used as
     * columns and ordered by name.
     *
     * @param <T>          the class
     * @param objectClass  the class
     * @return             the type object map
     *
     * @throws IllegalArgumentException if the specified class is not public
     */
    public static <T> TypeObjectMap<T> fromClass(Class<T> objectClass) {
        return fromClass(objectClass, null, null);
    }

    /**
     * Creates a {@link TypeObjectMap} based on the specified class with the
     * specified type label and overrides. By default, all public fields and
     * get/set method pairs with supported data types will be used as columns
     * and ordered by name, but column names, data types and properties can be
     * overridden with entries in the {@code columnOverrides} map. Each entry in
     * the map must consist of the name of a field in the specified class (or
     * for method pairs, the name following "get" and "set") as a key, and a
     * {@link Type.Column} object providing the name, data type, and properties
     * to use for the corresponding column as a value. To prevent a column from
     * being created for a field or method pair, use a value of {@code null}.
     *
     * @param <T>              the class
     * @param objectClass      the class
     * @param label            the type label ({@code null} to use class name)
     * @param columnOverrides  the map of overrides ({@code null} for none)
     * @return                 the type object map
     *
     * @throws IllegalArgumentException if the specified class is not public
     */
    public static <T> TypeObjectMap<T> fromClass(Class<T> objectClass, String label, Map<String, Type.Column> columnOverrides) {
        if (!Modifier.isPublic(objectClass.getModifiers())) {
            throw new IllegalArgumentException("Class " + objectClass.getName() + " must be public.");
        }

        Map<String, Type.Column> columns = new HashMap<>();
        Map<String, Accessor> accessors = new HashMap<>();
        Map<String, Mutator> mutators = new HashMap<>();
        Field[] objectClassFields = objectClass.getFields();
        Method[] objectClassMethods = objectClass.getMethods();

        for (Field field : objectClassFields) {
            String fieldName = field.getName();
            DataType dataType = DataType.fromClass(field.getType());

            if (dataType == null) {
                continue;
            }

            Type.Column column;

            if (columnOverrides != null && columnOverrides.containsKey(fieldName)) {
                column = columnOverrides.get(fieldName);

                if (column == null) {
                    continue;
                }
            } else {
                column = dataType.toColumn(fieldName);
            }

            fieldName = column.getName();
            columns.put(fieldName, column);

            if (DataType.fromClass(field.getType()) == DataType.fromClass(column.getType())) {
                FieldHandler handler = new FieldHandler(field);
                accessors.put(fieldName, handler);
                mutators.put(fieldName, handler);
            } else {
                ConvertingFieldHandler handler = new ConvertingFieldHandler(field, column);
                accessors.put(fieldName, handler);
                mutators.put(fieldName, handler);
            }
        }

        for (Method getMethod : objectClassMethods) {
            String methodName = getMethod.getName();

            if (!methodName.startsWith("get") || getMethod.getParameterTypes().length != 0) {
                continue;
            }

            String fieldName = methodName.substring(3);
            Method setMethod;

            try {
                setMethod = objectClass.getMethod("set" + fieldName, getMethod.getReturnType());
            } catch (NoSuchMethodException | SecurityException ex) {
                continue;
            }

            DataType dataType = DataType.fromClass(getMethod.getReturnType());

            if (dataType == null) {
                continue;
            }

            Type.Column column;

            if (columnOverrides != null && columnOverrides.containsKey(fieldName)) {
                column = columnOverrides.get(fieldName);

                if (column == null) {
                    continue;
                }
            } else {
                column = dataType.toColumn(fieldName);
            }

            fieldName = column.getName();
            columns.put(fieldName, column);

            if (DataType.fromClass(getMethod.getReturnType()) == DataType.fromClass(column.getType())) {
                MethodHandler handler = new MethodHandler(getMethod, setMethod);
                accessors.put(fieldName, handler);
                mutators.put(fieldName, handler);
            } else {
                ConvertingMethodHandler handler = new ConvertingMethodHandler(getMethod, setMethod, column);
                accessors.put(fieldName, handler);
                mutators.put(fieldName, handler);
            }
        }

        List<Type.Column> columnList = new ArrayList<>(columns.values());
        List<Accessor> accessorList = new ArrayList<>();
        List<Mutator> mutatorList = new ArrayList<>();

        Collections.sort(columnList, new Comparator<Type.Column>() {
            @Override
            public int compare(Column o1, Column o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        for (Type.Column column : columnList) {
            accessorList.add(accessors.get(column.getName()));
            mutatorList.add(mutators.get(column.getName()));
        }

        return new TypeObjectMap<>(new Type(label != null ? label : objectClass.getSimpleName(), columnList),
                objectClass, new ClassFactory<T>(objectClass), accessorList, mutatorList);
    }

    /**
     * Creates a {@link TypeObjectMap} based on the specified {@link Type} and
     * class. Each column in the type is mapped to a public field of the same
     * name, or in the absence of such a field, a get/set method pair where the
     * name following "get" and "set" is the same.
     *
     * @param <T>          the class
     * @param type         the type
     * @param objectClass  the class
     * @return             the type object map
     *
     * @throws IllegalArgumentException if the specified class is not public,
     * a column exists in the specified type that has no matching field or
     * method pair in the class, or a column has a type that is incompatible
     * with the matching field or method pair
     */
    public static <T> TypeObjectMap<T> fromType(Type type, Class<T> objectClass) {
        return fromType(type, objectClass, null);
    }

    /**
     * Creates a {@link TypeObjectMap} based on the specified {@link Type} and
     * class with the specified overrides. By default, each column is mapped to
     * a public field of the same name, or in the absence of such a field, a
     * get/set method pair where the name following "get" and "set" is the same,
     * but field and method pair names can be overridden with entries in the
     * {@code columnOverrides} map. Each entry in the map must consist of the
     * name of a column in the specified type as a key, and the name of the
     * corresponding field (or for method pairs, the name following "get" and
     * "set") as a value.
     *
     * @param <T>          the class
     * @param type         the type
     * @param objectClass  the class
     * @param columnOverrides
     * @return             the type object map
     *
     * @throws IllegalArgumentException if the specified class is not public,
     * a column exists in the specified type that has no matching field or
     * method pair in the class, or a column has a type that is incompatible
     * with the matching field or method pair
     */
    public static <T> TypeObjectMap<T> fromType(Type type, Class<T> objectClass, Map<String, String> columnOverrides) {
        if (!Modifier.isPublic(objectClass.getModifiers())) {
            throw new IllegalArgumentException("Class " + objectClass.getName() + " must be public.");
        }

        List<Accessor> accessors = new ArrayList<>();
        List<Mutator> mutators = new ArrayList<>();
        Field[] objectClassFields = objectClass.getFields();
        Method[] objectClassMethods = objectClass.getMethods();

        for (Column column : type.getColumns()) {
            String columnName = column.getName();

            if (columnOverrides != null && columnOverrides.containsKey(columnName)) {
                columnName = columnOverrides.get(columnName);
            }

            columnName = columnName.toLowerCase();
            boolean foundField = false;

            for (Field field : objectClassFields) {
                if (field.getName().toLowerCase().equals(columnName)) {
                    if (DataType.fromClass(field.getType()) == DataType.fromClass(column.getType())) {
                        FieldHandler handler = new FieldHandler(field);
                        accessors.add(handler);
                        mutators.add(handler);
                    } else {
                        ConvertingFieldHandler handler = new ConvertingFieldHandler(field, column);
                        accessors.add(handler);
                        mutators.add(handler);
                    }

                    foundField = true;
                    break;
                }
            }

            if (foundField) {
               continue;
            }

            Method getMethod = null;
            Method setMethod = null;

            for (Method method : objectClassMethods) {
                String methodName = method.getName().toLowerCase();

                if (getMethod == null
                        && methodName.equals("get" + columnName)
                        && method.getParameterTypes().length == 0) {
                    getMethod = method;
                } else if (setMethod == null
                        && methodName.equals("set" + columnName)
                        && method.getParameterTypes().length == 1) {
                    setMethod = method;
                }

                if (getMethod != null && setMethod != null) {
                    break;
                }
            }

            if (getMethod == null || setMethod == null) {
                throw new IllegalArgumentException("Column " + column.getName() + " has no match in " + objectClass.getName() + ".");
            }

            if (DataType.fromClass(getMethod.getReturnType()) == DataType.fromClass(column.getType())) {
                MethodHandler handler = new MethodHandler(getMethod, setMethod);
                accessors.add(handler);
                mutators.add(handler);
            } else {
                ConvertingMethodHandler handler = new ConvertingMethodHandler(getMethod, setMethod, column);
                accessors.add(handler);
                mutators.add(handler);
            }
        }

        return new TypeObjectMap<>(type, objectClass, new ClassFactory<T>(objectClass), accessors, mutators);
    }

    private final Type type;
    private final Class<T> objectClass;
    private final Schema schema;
    private final Factory<T> factory;
    private final Accessor[] accessors;
    private final Mutator[] mutators;

    /**
     * Creates a {@link TypeObjectMap} for the specified {@link Type} and class.
     * The specified {@code factory} must create new instances of the class,
     * and the specified {@code accessors} and {@code mutators} must get and set
     * the values in instances of the class corresponding to the columns in the
     * type (the lists must be in column order and use the data types of the
     * columns rather than any underlying fields in the class).
     *
     * @param type         the type
     * @param objectClass  the class
     * @param factory      factory that creates instances of the class
     * @param accessors    accessors that get column values from the class
     * @param mutators     mutators that set column values in the class
     */
    public TypeObjectMap(Type type, Class<T> objectClass, Factory<T> factory, List<Accessor> accessors, List<Mutator> mutators) {
        this.type = type;
        this.objectClass = objectClass;
        this.schema = type.getSchema();
        this.factory = factory;
        this.accessors = accessors.toArray(new Accessor[0]);
        this.mutators = mutators.toArray(new Mutator[0]);
    }

    /**
     * Returns the GPUdb {@link Type} of the type object map.
     *
     * @return  the type
     */
    public Type getType() {
        return type;
    }


    /**
     * Returns the class of the type object map.
     *
     * @return  the class
     */
    public Class<?> getObjectClass() {
        return objectClass;
    }

    /**
     * Returns the Avro {@link Schema} of the type object map.
     *
     * @return  the schema
     */
    public Schema getSchema() {
        return schema;
    }

    /**
     * Returns the value of the column at the specified index from the specified
     * object. The object must belong to the class specified when the type
     * object map was created.
     *
     * @param o      the object
     * @param index  the column index
     * @return       the value of the column
     */
    public Object get(Object o, int index) {
        return accessors[index].get(o);
    }

    /**
     * Sets the value of the column at the specified index in the specified
     * object. The object must belong to the class specified when the type
     * object map was created.
     *
     * @param o      the object
     * @param index  the column index
     * @param value  the new value of the column
     */
    public void put(Object o, int index, Object value) {
        mutators[index].set(o, value);
    }

    /**
     * Creates a new instance of the class specified when the type object map
     * was created.
     *
     * @return  the new instance
     */
    public T newInstance() {
        return factory.newInstance();
    }

    /**
     * Creates a type in GPUdb based on the metadata in the specified
     * {@link TypeObjectMap} object and returns the type ID for reference. If an
     * identical type already exists in GPUdb, the type ID of the existing type
     * will be returned and no new type will be created. The specified type
     * object map will also automatically be added as a
     * {@link GPUdbBase#addKnownType(String, Class, TypeObjectMap) known type
     * and known type object map} in the specified {@link GPUdb} instance.
     *
     * @param gpudb  the {@link GPUdb} instance in which to create the type
     * @return       the type ID of the type in GPUdb
     *
     * @throws GPUdbException if an error occurs while creating the type
     */
    public String createType(GPUdb gpudb) throws GPUdbException {
        String typeId = type.create(gpudb);
        gpudb.addKnownType(typeId, objectClass, this);
        return typeId;
    }
}