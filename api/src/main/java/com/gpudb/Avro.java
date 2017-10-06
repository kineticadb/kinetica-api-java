package com.gpudb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.avro.Schema;
import org.apache.avro.UnresolvedUnionException;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;

/**
 * Utility class containing static methods for encoding and decoding Avro binary
 * objects.
 */
public final class Avro {
    /**
     * Proxy for decoding Avro binary objects into non-Avro-compatible Java
     * objects.
     */
    private static final class AvroDecodeProxy<T> implements IndexedRecord {
        private final TypeObjectMap<T> typeObjectMap;
        private final Schema schema;
        public T object;

        public AvroDecodeProxy(TypeObjectMap<T> typeObjectMap) {
            this.typeObjectMap = typeObjectMap;
            this.schema = typeObjectMap.getSchema();
        }

        @Override
        public Schema getSchema() {
            return schema;
        }

        @Override
        public Object get(int index) {
            // When decoding a field, Avro will call get(...) to check the
            // previous value (to reuse lists, maps, etc., instead of creating
            // new ones). However, TypeObjectMap does not support field types
            // that are reusable, so this step can be skipped to avoid
            // reflection costs.
            return null;
        }

        @Override
        public void put(int index, Object o) {
            typeObjectMap.put(object, index, o);
        }
    }

    /**
     * Proxy for encoding non-Avro-compatible Java objects into Avro binary
     * objects.
     */
    private static final class AvroEncodeProxy<T> implements IndexedRecord {
        private final TypeObjectMap<T> typeObjectMap;
        private final Schema schema;
        public T object;

        public AvroEncodeProxy(TypeObjectMap<T> typeObjectMap) {
            this.typeObjectMap = typeObjectMap;
            this.schema = typeObjectMap.getSchema();
        }

        @Override
        public Schema getSchema() {
            return schema;
        }

        @Override
        public Object get(int index) {
            return typeObjectMap.get(object, index);
        }

        @Override
        public void put(int index, Object o) {
            typeObjectMap.put(object, index, o);
        }
    }

    /**
     * Avro reader for generic objects that uses String for string values.
     */
    static final class DatumReader<T> extends GenericDatumReader<T> {
        public DatumReader(Schema schema) {
            super(schema);
        }

        @Override
        protected Class<?> findStringClass(Schema schema) {
            return String.class;
        }
    }

    /**
     * Default thread pool for multi-threaded operations.
     */
    private static final ExecutorService defaultThreadPool =
            new ThreadPoolExecutor(0, Integer.MAX_VALUE, 100L, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>());

    /**
     * Pattern for parsing Avro NullPointerException messages.
     */
    private static final Pattern nullPointerExceptionPattern =
            Pattern.compile("null of .+ in field (.+) of .+");

    /**
     * Decodes an Avro binary object into a pre-created destination object.
     *
     * @param <T>             the type of object being decoded
     * @param object          the destination object
     * @param encodedObject   the Avro binary object
     * @return                the destination object (same as {@code object}
     *                        parameter)
     *
     * @throws GPUdbException if a decoding error occurs
     */
    public static <T extends IndexedRecord> T decode(T object, ByteBuffer encodedObject) throws GPUdbException {
        try {
            if (object instanceof SpecificRecord) {
                new SpecificDatumReader<>(object.getSchema()).read(object, DecoderFactory.get().binaryDecoder(encodedObject.array(), null));
            } else {
                new DatumReader<>(object.getSchema()).read(object, DecoderFactory.get().binaryDecoder(encodedObject.array(), null));
            }

            return object;
        } catch (IOException ex) {
            if (ex.getMessage() == null) {
                throw new GPUdbException("Could not decode object", ex);
            } else {
                throw new GPUdbException("Could not decode object: " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * Decodes an Avro binary object.
     *
     * @param <T>             the type of object being decoded
     * @param typeDescriptor  type descriptor for the type of object being
     *                        decoded
     * @param encodedObject   the Avro binary object
     * @return                the decoded object
     *
     * @throws IllegalArgumentException if {@code typeDescriptor} is not a
     * {@link Schema}, {@link Type}, {@link TypeObjectMap}, or {@link Class}
     * that implements {@link IndexedRecord}
     *
     * @throws GPUdbException if a decoding error occurs
     *
     * @throws GPUdbRuntimeException if unable to instantiate the class
     * specified by {@code typeDescriptor}
     */
    @SuppressWarnings("unchecked")
    public static <T> T decode(Object typeDescriptor, ByteBuffer encodedObject) throws GPUdbException {
        if (typeDescriptor instanceof Class && IndexedRecord.class.isAssignableFrom((Class)typeDescriptor)) {
            try {
                return (T)decode(((Class<? extends IndexedRecord>)typeDescriptor).newInstance(), encodedObject);
            } catch (IllegalAccessException | InstantiationException ex) {
                throw new GPUdbRuntimeException("Could not create " + ((Class<T>)typeDescriptor).getName() + " instance.", ex);
            }
        } else if (typeDescriptor instanceof Schema) {
            return (T)decode(new GenericData.Record((Schema)typeDescriptor), encodedObject);
        } else if (typeDescriptor instanceof Type) {
            return (T)decode(((Type)typeDescriptor).newInstance(), encodedObject);
        } else if (typeDescriptor instanceof TypeObjectMap) {
            TypeObjectMap<T> typeObjectMap = (TypeObjectMap<T>)typeDescriptor;
            T object = typeObjectMap.newInstance();
            AvroDecodeProxy<T> proxy = new AvroDecodeProxy<>(typeObjectMap);
            proxy.object = object;
            decode(proxy, encodedObject);
            return object;
        } else {
            throw new IllegalArgumentException("Type descriptor must be a Schema, Type, TypeObjectMap, or Class implementing IndexedRecord.");
        }
    }

    /**
     * Decodes a homogeneous portion of a list of Avro binary objects.
     *
     * @param <T>             the type of objects being decoded
     * @param typeDescriptor  type descriptor for the type of objects being
     *                        decoded
     * @param encodedObjects  list of Avro binary objects
     * @param start           index of first object within
     *                        {@code encodedObjects} to decode
     * @param count           number of objects within {@code encodedObjects} to
     *                        decode
     * @return                list of decoded objects
     *
     * @throws IndexOutOfBoundsException if {@code start} is less than zero,
     * {@code count} is less than zero, or {@code start} plus {@code count}
     * exceeds the length of {@code encodedObjects}
     *
     * @throws IllegalArgumentException if {@code typeDescriptor} is not a
     * {@link Schema}, {@link Type}, {@link TypeObjectMap}, or {@link Class}
     * that implements {@link IndexedRecord}
     *
     * @throws GPUdbException if a decoding error occurs
     *
     * @throws GPUdbRuntimeException if unable to instantiate the class
     * specified by {@code typeDescriptor}
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> decode(Object typeDescriptor, List<ByteBuffer> encodedObjects, int start, int count) throws GPUdbException {
        if (start < 0) {
            throw new IndexOutOfBoundsException("Invalid start index specified.");
        }

        if (count < 0) {
            throw new IndexOutOfBoundsException("Invalid count specified.");
        }

        if (start + count > encodedObjects.size()) {
            throw new IndexOutOfBoundsException("Start index plus count exceeds list size.");
        }

        ArrayList<T> objects = new ArrayList<>(count);

        if (count == 0) {
            return objects;
        }

        try {
            if (typeDescriptor instanceof Class && IndexedRecord.class.isAssignableFrom((Class)typeDescriptor)) {
                try {
                    Class<IndexedRecord> typeClass = (Class<IndexedRecord>)typeDescriptor;
                    IndexedRecord object = typeClass.newInstance();
                    GenericDatumReader<IndexedRecord> reader;

                    if (object instanceof SpecificRecord) {
                        reader = new SpecificDatumReader<>(object.getSchema());
                    } else {
                        reader = new DatumReader<>(object.getSchema());
                    }

                    DecoderFactory factory = DecoderFactory.get();
                    BinaryDecoder decoder = factory.binaryDecoder(encodedObjects.get(start).array(), null);
                    reader.read(object, decoder);
                    objects.add((T)object);

                    for (int i = start + 1; i < start + count; i++) {
                        object = typeClass.newInstance();
                        decoder = factory.binaryDecoder(encodedObjects.get(i).array(), decoder);
                        reader.read(object, decoder);
                        objects.add((T)object);
                    }
                } catch (IllegalAccessException | InstantiationException ex) {
                    throw new GPUdbRuntimeException("Could not create " + ((Class)typeDescriptor).getName() + " instance.", ex);
                }
            } else if (typeDescriptor instanceof Schema) {
                Schema schema = (Schema)typeDescriptor;
                GenericDatumReader<org.apache.avro.generic.GenericRecord> reader = new DatumReader<>(schema);
                DecoderFactory factory = DecoderFactory.get();
                BinaryDecoder decoder = null;

                for (int i = start; i < start + count; i++) {
                    org.apache.avro.generic.GenericRecord object = new GenericData.Record(schema);
                    decoder = factory.binaryDecoder(encodedObjects.get(i).array(), decoder);
                    reader.read(object, decoder);
                    objects.add((T)object);
                }
            } else if (typeDescriptor instanceof Type) {
                Type type = (Type)typeDescriptor;
                Schema schema = type.getSchema();
                GenericDatumReader<Record> reader = new DatumReader<>(schema);
                DecoderFactory factory = DecoderFactory.get();
                BinaryDecoder decoder = null;

                for (int i = start; i < start + count; i++) {
                    Record object = type.newInstance();
                    decoder = factory.binaryDecoder(encodedObjects.get(i).array(), decoder);
                    reader.read(object, decoder);
                    objects.add((T)object);
                }
            } else if (typeDescriptor instanceof TypeObjectMap) {
                TypeObjectMap<T> typeObjectMap = (TypeObjectMap<T>)typeDescriptor;
                Schema schema = typeObjectMap.getSchema();
                GenericDatumReader<IndexedRecord> reader = new DatumReader<>(schema);
                DecoderFactory factory = DecoderFactory.get();
                BinaryDecoder decoder = null;
                AvroDecodeProxy<T> proxy = new AvroDecodeProxy<>(typeObjectMap);

                for (int i = start; i < start + count; i++) {
                    T object = typeObjectMap.newInstance();
                    proxy.object = object;
                    decoder = factory.binaryDecoder(encodedObjects.get(i).array(), decoder);
                    reader.read(proxy, decoder);
                    objects.add(object);
                }
            } else {
                throw new IllegalArgumentException("Type descriptor must be a Schema, Type, TypeObjectMap, or Class implementing IndexedRecord.");
            }
        } catch (IOException ex) {
            if (ex.getMessage() == null) {
                throw new GPUdbException("Could not decode object", ex);
            } else {
                throw new GPUdbException("Could not decode object: " + ex.getMessage(), ex);
            }
        }

        return objects;
    }

    /**
     * Decodes a homogeneous portion of a list of Avro binary objects,
     * optionally using multiple threads, with or without a supplied executor.
     *
     * @param <T>             the type of objects being decoded
     * @param typeDescriptor  type descriptor for the type of objects being
     *                        decoded
     * @param encodedObjects  list of Avro binary objects
     * @param start           index of first object within
     *                        {@code encodedObjects} to decode
     * @param count           number of objects within {@code encodedObjects} to
     *                        decode
     * @param threadCount     number of threads to use for decoding.
     * @param executor        optional executor responsible for managing
     *                        threads; <code>null</code> to create threads on
     *                        demand
     * @return                list of decoded objects
     *
     * @throws IndexOutOfBoundsException if {@code start} is less than zero,
     * {@code count} is less than zero, or {@code start} plus {@code count}
     * exceeds the length of {@code encodedObjects}
     *
     * @throws IllegalArgumentException if {@code threadCount} is less than one
     * or {@code typeDescriptor} is not a {@link Schema}, {@link Type}, {@link
     * TypeObjectMap}, or {@link Class} that implements {@link IndexedRecord}
     *
     * @throws GPUdbException if a decoding error occurs
     *
     * @throws GPUdbRuntimeException if unable to instantiate the class
     * specified by {@code typeDescriptor}
     */
    public static <T> List<T> decode(final Object typeDescriptor, final List<ByteBuffer> encodedObjects, int start, int count, int threadCount, ExecutorService executor) throws GPUdbException {
        if (threadCount == 1 || count <= 1) {
            return decode(typeDescriptor, encodedObjects, start, count);
        }

        if (threadCount < 1) {
            throw new IllegalArgumentException("Thread count must be greater than zero.");
        }

        ArrayList<T> objects = new ArrayList<>(count);
        int partitionSize = count / threadCount;
        int partitionExtras = count % threadCount;
        ExecutorService executorService = executor != null ? executor : defaultThreadPool;
        List<Future<List<T>>> futures = new ArrayList<>(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int partitionStart = i * partitionSize + Math.min(i, partitionExtras);
            final int partitionEnd = (i + 1) * partitionSize + Math.min(i + 1, partitionExtras);

            if (partitionStart == partitionEnd) {
                break;
            }

            futures.add(executorService.submit(new Callable<List<T>>() {
                @Override
                public List<T> call() throws GPUdbException {
                    return decode(typeDescriptor, encodedObjects, partitionStart, partitionEnd - partitionStart);
                }
            }));
        }

        for (Future<List<T>> future : futures) {
            try {
                objects.addAll(future.get());
            } catch (ExecutionException ex) {
                if (ex.getCause() instanceof GPUdbException) {
                    throw (GPUdbException)ex.getCause();
                } else if (ex.getCause() instanceof RuntimeException) {
                    throw (RuntimeException)ex.getCause();
                } else {
                    throw new GPUdbException(ex.getMessage(), ex);
                }
            } catch (InterruptedException ex) {
                throw new GPUdbException(ex.getMessage(), ex);
            }
        }

        return objects;
    }

    /**
     * Decodes a homogeneous list of Avro binary objects.
     *
     * @param <T>             the type of objects being decoded
     * @param typeDescriptor  type descriptor for the type of objects being
     *                        decoded
     * @param encodedObjects  list of Avro binary objects
     * @return                list of decoded objects
     *
     * @throws IllegalArgumentException if {@code typeDescriptor} is not a
     * {@link Type}, a {@link TypeObjectMap}, a {@link Schema}, or a {@link
     * Class} that implements {@link IndexedRecord}
     *
     * @throws GPUdbException if a decoding error occurs
     *
     * @throws GPUdbRuntimeException if unable to instantiate the class
     * specified by {@code typeDescriptor}
     */
    public static <T> List<T> decode(Object typeDescriptor, List<ByteBuffer> encodedObjects) throws GPUdbException {
        return decode(typeDescriptor, encodedObjects, 0, encodedObjects.size());
    }

    /**
     * Decodes a homogeneous list of Avro binary objects, optionally using
     * multiple threads and/or a supplied executor.
     *
     * @param <T>             the type of objects being decoded
     * @param typeDescriptor  type descriptor for the type of objects being
     *                        decoded
     * @param encodedObjects  list of Avro binary objects
     * @param threadCount     number of threads to use for decoding.
     * @param executor        optional executor responsible for managing
     *                        threads; <code>null</code> to create threads on
     *                        demand
     * @return                list of decoded objects
     *
     * @throws IllegalArgumentException if {@code threadCount} is less than one
     * or {@code typeDescriptor} is not a {@link Type}, a {@link TypeObjectMap},
     * a {@link Schema}, or a {@link Class} that implements {@link
     * IndexedRecord}
     *
     * @throws GPUdbException if a decoding error occurs
     *
     * @throws GPUdbRuntimeException if unable to instantiate the class
     * specified by {@code typeDescriptor}
     */
    public static <T> List<T> decode(Object typeDescriptor, List<ByteBuffer> encodedObjects, int threadCount, ExecutorService executor) throws GPUdbException {
        return decode(typeDescriptor, encodedObjects, 0, encodedObjects.size(), threadCount, executor);
    }

    /**
     * Encodes an Avro-compatible object into Avro binary format.
     *
     * @param <T>             the type of object being encoded
     * @param object          the object to encode
     * @return                the encoded object
     *
     * @throws GPUdbException if an encoding error occurs
     */
    public static <T extends IndexedRecord> ByteBuffer encode(T object) throws GPUdbException {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(stream, null);

            if (object instanceof SpecificRecord) {
                new SpecificDatumWriter<>(object.getSchema()).write(object, encoder);
            } else {
                new GenericDatumWriter<>(object.getSchema()).write(object, encoder);
            }

            encoder.flush();
            stream.close();
            return ByteBuffer.wrap(stream.toByteArray());
        } catch (ClassCastException | IOException ex) {
            if (ex.getMessage() == null) {
                throw new GPUdbException("Could not encode object", ex);
            } else {
                throw new GPUdbException("Could not encode object: " + ex.getMessage(), ex);
            }
        } catch (NullPointerException ex) {
            Matcher matcher = nullPointerExceptionPattern.matcher(ex.getMessage());

            if (matcher.matches()) {
                throw new GPUdbException("Could not encode object: Non-nullable field " + matcher.group(1) + " cannot be null.", ex);
            } else {
                throw new GPUdbException("Could not encode object: " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * Encodes a non-Avro-compatible object into Avro binary format using the
     * specified type object map.
     *
     * @param <T>             the type of object being encoded
     * @param typeObjectMap   the type object map
     * @param object          the object to encode
     * @return                the encoded object
     *
     * @throws GPUdbException if an encoding error occurs
     */
    public static <T> ByteBuffer encode(TypeObjectMap<T> typeObjectMap, T object) throws GPUdbException {
        AvroEncodeProxy<T> proxy = new AvroEncodeProxy<>(typeObjectMap);
        proxy.object = object;
        return encode(proxy);
    }

    /**
     * Encodes a portion of list of objects into Avro binary format, optionally
     * using a type object map for non-Avro-compatible objects.
     */
    private static <T> List<ByteBuffer> encodeInternal(TypeObjectMap<T> typeObjectMap, List<T> objects, int start, int count) throws GPUdbException {
        if (start < 0) {
            throw new IndexOutOfBoundsException("Invalid start index specified.");
        }

        if (count < 0) {
            throw new IndexOutOfBoundsException("Invalid count specified.");
        }

        if (start + count > objects.size()) {
            throw new IndexOutOfBoundsException("Start index plus count exceeds list size.");
        }

        ArrayList<ByteBuffer> encodedObjects = new ArrayList<>(count);

        if (count == 0) {
            return encodedObjects;
        }

        try {
            T object = objects.get(start);
            GenericDatumWriter<IndexedRecord> writer;

            if (typeObjectMap == null) {
                if (object instanceof SpecificRecord) {
                    writer = new SpecificDatumWriter<>(((IndexedRecord)object).getSchema());
                } else {
                    writer = new GenericDatumWriter<>(((IndexedRecord)object).getSchema());
                }
            } else {
                writer = new GenericDatumWriter<>(typeObjectMap.getSchema());
            }

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            EncoderFactory factory = EncoderFactory.get();
            BinaryEncoder encoder = factory.binaryEncoder(stream, null);
            AvroEncodeProxy<T> proxy = null;

            if (typeObjectMap == null) {
                writer.write((IndexedRecord)object, encoder);
            } else {
                proxy = new AvroEncodeProxy<>(typeObjectMap);
                proxy.object = object;
                writer.write(proxy, encoder);
            }

            encoder.flush();
            stream.close();
            encodedObjects.add(ByteBuffer.wrap(stream.toByteArray()));

            for (int i = start + 1; i < start + count; i++) {
                stream = new ByteArrayOutputStream();
                encoder = factory.binaryEncoder(stream, encoder);

                if (typeObjectMap == null) {
                    writer.write((IndexedRecord)objects.get(i), encoder);
                } else {
                    proxy.object = objects.get(i);
                    writer.write(proxy, encoder);
                }

                encoder.flush();
                stream.close();
                encodedObjects.add(ByteBuffer.wrap(stream.toByteArray()));
            }
        } catch (ClassCastException | UnresolvedUnionException ex) {
            throw new GPUdbException("Could not encode object: Field has incorrect data type.", ex);
        } catch (IOException ex) {
            if (ex.getMessage() == null) {
                throw new GPUdbException("Could not encode object", ex);
            } else {
                throw new GPUdbException("Could not encode object: " + ex.getMessage(), ex);
            }
        } catch (NullPointerException ex) {
            Matcher matcher = nullPointerExceptionPattern.matcher(ex.getMessage());

            if (matcher.matches()) {
                throw new GPUdbException("Could not encode object: Non-nullable field " + matcher.group(1) + " cannot be null.", ex);
            } else {
                throw new GPUdbException("Could not encode object: " + ex.getMessage(), ex);
            }
        }

        return encodedObjects;
    }

    /**
     * Encodes a portion of list of Avro-compatible objects into Avro binary
     * format.
     *
     * @param <T>             the type of objects being encoded
     * @param objects         list of objects to encode
     * @param start           index of first object within {@code objects} to
     *                        encode
     * @param count           number of objects within {@code objects} to encode
     * @return                list of encoded objects
     *
     * @throws IndexOutOfBoundsException if {@code start} is less than zero,
     * {@code count} is less than zero, or {@code start} plus {@code count}
     * exceeds the length of {@code objects}
     *
     * @throws GPUdbException if an encoding error occurs
     */
    public static <T extends IndexedRecord> List<ByteBuffer> encode(List<T> objects, int start, int count) throws GPUdbException {
        return encodeInternal(null, objects, start, count);
    }

    /**
     * Encodes a portion of list of non-Avro-compatible objects into Avro binary
     * format using the specified type object map.
     *
     * @param <T>             the type of objects being encoded
     * @param typeObjectMap   the type object map
     * @param objects         list of objects to encode
     * @param start           index of first object within {@code objects} to
     *                        encode
     * @param count           number of objects within {@code objects} to encode
     * @return                list of encoded objects
     *
     * @throws IndexOutOfBoundsException if {@code start} is less than zero,
     * {@code count} is less than zero, or {@code start} plus {@code count}
     * exceeds the length of {@code objects}
     *
     * @throws GPUdbException if an encoding error occurs
     */
    public static <T> List<ByteBuffer> encode(TypeObjectMap<T> typeObjectMap, List<T> objects, int start, int count) throws GPUdbException {
        return encodeInternal(typeObjectMap, objects, start, count);
    }

    /**
     * Encodes a portion of list of objects into Avro binary format, optionally
     * using a type object map for non-Avro-compatible objects, and optionally
     * using multiple threads, with or without a supplied executor.
     */
    private static <T> List<ByteBuffer> encodeInternal(final TypeObjectMap<T> typeObjectMap, final List<T> objects, int start, int count, int threadCount, ExecutorService executor) throws GPUdbException {
        if (threadCount == 1 || count <= 1) {
            return encodeInternal(typeObjectMap, objects, start, count);
        }

        if (threadCount < 1) {
            throw new IllegalArgumentException("Thread count must be greater than zero.");
        }

        ArrayList<ByteBuffer> encodedObjects = new ArrayList<>(count);
        int partitionSize = count / threadCount;
        int partitionExtras = count % threadCount;
        ExecutorService executorService = executor != null ? executor : defaultThreadPool;
        List<Future<List<ByteBuffer>>> futures = new ArrayList<>(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int partitionStart = i * partitionSize + Math.min(i, partitionExtras);
            final int partitionEnd = (i + 1) * partitionSize + Math.min(i + 1, partitionExtras);

            if (partitionStart == partitionEnd) {
                break;
            }

            futures.add(executorService.submit(new Callable<List<ByteBuffer>>() {
                @Override
                public List<ByteBuffer> call() throws GPUdbException {
                    return encodeInternal(typeObjectMap, objects, partitionStart, partitionEnd - partitionStart);
                }
            }));
        }

        for (Future<List<ByteBuffer>> future : futures) {
            try {
                encodedObjects.addAll(future.get());
            } catch (ExecutionException ex) {
                if (ex.getCause() instanceof GPUdbException) {
                    throw (GPUdbException)ex.getCause();
                } else if (ex.getCause() instanceof RuntimeException) {
                    throw (RuntimeException)ex.getCause();
                } else {
                    throw new GPUdbException(ex.getMessage(), ex);
                }
            } catch (InterruptedException ex) {
                throw new GPUdbException(ex.getMessage(), ex);
            }
        }

        return encodedObjects;
    }

    /**
     * Encodes a portion of list of Avro-compatible objects into Avro binary
     * format, optionally using multiple threads, with or without a supplied
     * executor.
     *
     * @param <T>             the type of objects being encoded
     * @param objects         list of objects to encode
     * @param start           index of first object within {@code objects} to
     *                        encode
     * @param count           number of objects within {@code objects} to encode
     * @param threadCount     number of threads to use for decoding.
     * @param executor        optional executor responsible for managing
     *                        threads; <code>null</code> to create threads on
     *                        demand
     * @return                list of encoded objects
     *
     * @throws IndexOutOfBoundsException if {@code start} is less than zero,
     * {@code count} is less than zero, or {@code start} plus {@code count}
     * exceeds the length of {@code objects}
     *
     * @throws IllegalArgumentException if {@code threadCount} is less than one
     *
     * @throws GPUdbException if an encoding error occurs
     */
    public static <T extends IndexedRecord> List<ByteBuffer> encode(List<T> objects, int start, int count, int threadCount, ExecutorService executor) throws GPUdbException {
        return encodeInternal(null, objects, start, count, threadCount, executor);
    }

    /**
     * Encodes a portion of list of Avro-compatible objects into Avro binary
     * format using the specified type object map, optionally using multiple
     * threads, with or without a supplied executor.
     *
     * @param <T>             the type of objects being encoded
     * @param typeObjectMap   the type object map
     * @param objects         list of objects to encode
     * @param start           index of first object within {@code objects} to
     *                        encode
     * @param count           number of objects within {@code objects} to encode
     * @param threadCount     number of threads to use for decoding.
     * @param executor        optional executor responsible for managing
     *                        threads; <code>null</code> to create threads on
     *                        demand
     * @return                list of encoded objects
     *
     * @throws IndexOutOfBoundsException if {@code start} is less than zero,
     * {@code count} is less than zero, or {@code start} plus {@code count}
     * exceeds the length of {@code objects}
     *
     * @throws IllegalArgumentException if {@code threadCount} is less than one
     *
     * @throws GPUdbException if an encoding error occurs
     */
    public static <T> List<ByteBuffer> encode(TypeObjectMap<T> typeObjectMap, List<T> objects, int start, int count, int threadCount, ExecutorService executor) throws GPUdbException {
        return encodeInternal(typeObjectMap, objects, start, count, threadCount, executor);
    }

    /**
     * Encodes a list of Avro-compatible objects into Avro binary format.
     *
     * @param <T>             the type of objects being encoded
     * @param objects         list of objects to encode
     * @return                list of encoded objects
     *
     * @throws GPUdbException if an encoding error occurs
     */
    public static <T extends IndexedRecord> List<ByteBuffer> encode(List<T> objects) throws GPUdbException {
        return encodeInternal(null, objects, 0, objects.size());
    }

    /**
     * Encodes a list of non-Avro-compatible objects into Avro binary format
     * using the specified type object map.
     *
     * @param <T>             the type of objects being encoded
     * @param typeObjectMap   the type object map
     * @param objects         list of objects to encode
     * @return                list of encoded objects
     *
     * @throws GPUdbException if an encoding error occurs
     */
    public static <T> List<ByteBuffer> encode(TypeObjectMap<T> typeObjectMap, List<T> objects) throws GPUdbException {
        return encodeInternal(typeObjectMap, objects, 0, objects.size());
    }

    /**
     * Encodes a list of Avro-compatible objects into Avro binary format,
     * optionally using multiple threads, with or without a supplied executor.
     *
     * @param <T>             the type of objects being encoded
     * @param objects         list of objects to encode
     * @param threadCount     number of threads to use for decoding.
     * @param executor        optional executor responsible for managing
     *                        threads; <code>null</code> to create threads on
     *                        demand
     * @return                list of encoded objects
     *
     * @throws IllegalArgumentException if {@code threadCount} is less than one
     *
     * @throws GPUdbException if an encoding error occurs
     */
    public static <T extends IndexedRecord> List<ByteBuffer> encode(List<T> objects, int threadCount, ExecutorService executor) throws GPUdbException {
        return encodeInternal(null, objects, 0, objects.size(), threadCount, executor);
    }

    /**
     * Encodes a list of non-Avro-compatible objects into Avro binary format
     * using the specified type object map, optionally using multiple threads,
     * with or without a supplied executor.
     *
     * @param <T>             the type of objects being encoded
     * @param typeObjectMap   the type object map
     * @param objects         list of objects to encode
     * @param threadCount     number of threads to use for decoding.
     * @param executor        optional executor responsible for managing
     *                        threads; <code>null</code> to create threads on
     *                        demand
     * @return                list of encoded objects
     *
     * @throws IllegalArgumentException if {@code threadCount} is less than one
     *
     * @throws GPUdbException if an encoding error occurs
     */
    public static <T> List<ByteBuffer> encode(TypeObjectMap<T> typeObjectMap, List<T> objects, int threadCount, ExecutorService executor) throws GPUdbException {
        return encodeInternal(typeObjectMap, objects, 0, objects.size(), threadCount, executor);
    }

    private Avro() {
    }
}