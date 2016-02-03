package com.gisfederal.gpudb;

import com.gisfederal.gpudb.Type.Column;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.apache.avro.Schema;

public final class TypeObjectMap {
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

    public interface Factory {
        Object newInstance();
    }

    public static final class ClassFactory implements Factory {
        private final Class<?> objectClass;

        public ClassFactory(Class<?> objectClass) {
            this.objectClass = objectClass;
        }

        @Override
        public Object newInstance() {
            try {
                return objectClass.newInstance();
            } catch (IllegalAccessException | InstantiationException ex) {
                throw new GPUdbRuntimeException("Could not create " + objectClass.getName() + " instance.", ex);
            }
        }
    }

    public interface Accessor {
        Object get(Object o);
    }

    public interface Mutator {
        void set(Object o, Object value);
    }

    public static final class FieldHandler implements Accessor, Mutator {
        private final Field field;

        public FieldHandler(Field field) {
            this.field = field;
        }

        @Override
        public Object get(Object o) {
            try {
                return field.get(o);
            } catch (IllegalAccessException ex) {
                throw new GPUdbRuntimeException("Could not set field value for " + field.getName() + ".", ex);
            }
        }

        @Override
        public void set(Object o, Object value) {
            try {
                field.set(o, value);
            } catch (IllegalAccessException ex) {
                throw new GPUdbRuntimeException("Could not set field value for " + field.getName() + ".", ex);
            }
        }
    }

    public static final class ConvertingFieldHandler implements Accessor, Mutator {
        private final Field field;
        private final DataType fieldType;
        private final DataType columnType;

        public ConvertingFieldHandler(Field field, Column column) {
            this.field = field;
            fieldType = DataType.fromClass(field.getType());

            if (fieldType == null) {
                throw new IllegalArgumentException("Field " + field.getName() + " has unsupported type " + field.getType().getName() + ".");
            }

            columnType = DataType.fromClass(column.getType());
        }

        @Override
        public Object get(Object o) {
            try {
                return convert(field.get(o), fieldType, columnType);
            } catch (IllegalAccessException ex) {
                throw new GPUdbRuntimeException("Could not set field value for " + field.getName() + ".", ex);
            }
        }

        @Override
        public void set(Object o, Object value) {
            try {
                field.set(o, convert(value, columnType, fieldType));
            } catch (IllegalAccessException ex) {
                throw new GPUdbRuntimeException("Could not set field value for " + field.getName() + ".", ex);
            }
        }
    }

    public static final class MethodHandler implements Accessor, Mutator {
        private final Method getMethod;
        private final Method setMethod;

        public MethodHandler(Method getMethod, Method setMethod) {
            this.getMethod = getMethod;
            this.setMethod = setMethod;
        }

        @Override
        public Object get(Object o) {
            try {
                return getMethod.invoke(o);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new GPUdbRuntimeException("Could not call " + getMethod.getName() + ".", ex);
            }
        }

        @Override
        public void set(Object o, Object value) {
            try {
                setMethod.invoke(o, value);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new GPUdbRuntimeException("Could not call " + setMethod.getName() + ".", ex);
            }
        }
    }

    public static final class ConvertingMethodHandler implements Accessor, Mutator {
        private final Method getMethod;
        private final Method setMethod;
        private final DataType fieldType;
        private final DataType columnType;

        public ConvertingMethodHandler(Method getMethod, Method setMethod, Column column) {
            this.getMethod = getMethod;
            this.setMethod = setMethod;
            fieldType = DataType.fromClass(getMethod.getReturnType());
            columnType = DataType.fromClass(column.getType());
        }

        @Override
        public Object get(Object o) {
            try {
                return convert(getMethod.invoke(o), fieldType, columnType);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new GPUdbRuntimeException("Could not call " + getMethod.getName() + ".", ex);
            }
        }

        @Override
        public void set(Object o, Object value) {
            try {
                setMethod.invoke(o, convert(value, columnType, fieldType));
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new GPUdbRuntimeException("Could not call " + setMethod.getName() + ".", ex);
            }
        }
    }

    public static TypeObjectMap fromType(Type type, Class<?> objectClass) {
        if (!Modifier.isPublic(objectClass.getModifiers())) {
            throw new IllegalArgumentException("Class " + objectClass.getName() + " must be public.");
        }

        List<Accessor> accessors = new ArrayList<>();
        List<Mutator> mutators = new ArrayList<>();
        Field[] objectClassFields = objectClass.getFields();
        Method[] objectClassMethods = objectClass.getMethods();

        for (Column column : type.getColumns()) {
            String columnName = column.getName().toLowerCase();
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
                throw new GPUdbRuntimeException("Column " + column.getName() + " has no match in " + objectClass.getName() + ".");
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

        return new TypeObjectMap(type, new ClassFactory(objectClass), accessors, mutators);
    }

    private final Type type;
    private final Schema schema;
    private final Factory factory;
    private final Accessor[] accessors;
    private final Mutator[] mutators;

    public TypeObjectMap(Type type, Factory factory, List<Accessor> accessors, List<Mutator> mutators) {
        this.type = type;
        this.schema = type.getSchema();
        this.factory = factory;
        this.accessors = accessors.toArray(new Accessor[0]);
        this.mutators = mutators.toArray(new Mutator[0]);
    }

    public Type getType() {
        return type;
    }

    public Schema getSchema() {
        return schema;
    }

    public Object get(Object o, int index) {
        return accessors[index].get(o);
    }

    public void put(Object o, int index, Object value) {
        mutators[index].set(o, value);
    }

    public Object newInstance() {
        return factory.newInstance();
    }
}