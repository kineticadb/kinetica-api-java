package com.gpudb.filesystem.utils;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This is an internal class and not meant to be used by the end users of the
 * filesystem API. The consequences of using this class directly in client code
 * is not guaranteed and maybe undesirable.
 *
 */
public class GPUdbFileHandlerUtils {
    /**
     * Merge a list of buffers into a single buffer.
     *
     * @param byteBuffers list of ByteBuffer objects
     * @return merged ByteBuffer
     */
    public static ByteBuffer merge( List<ByteBuffer> byteBuffers ) {
        if ( byteBuffers == null || byteBuffers.size() == 0 ) {
            return ByteBuffer.allocate(0);
        } else if ( byteBuffers.size() == 1 ) {
            return byteBuffers.get(0);
        } else {
            int capacity = 0;
            for ( ByteBuffer buffer: byteBuffers ) {
                capacity += buffer.capacity();

            }
            ByteBuffer fullContent = ByteBuffer.allocate( capacity );

            for ( ByteBuffer buffer: byteBuffers ) {
                fullContent.put( buffer );
            }

            fullContent.flip();

            return fullContent;
        }
    }

    /**
     * Terminate a thread pool after waiting for a given vaue of 'timeout'
     * @param threadPool
     */
    public static void awaitTerminationAfterShutdown(ExecutorService threadPool,
                                                     long timeout) {
        threadPool.shutdown();
        try {
            if ( !threadPool.awaitTermination( timeout, TimeUnit.SECONDS ) ) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException ex) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static String joinStrings( List<String> stringList ) {
        return StringUtils.join( stringList, ',');
    }

    public static <T> Set<T> setDifference(Set<T> setA, Set<T> setB) {
        Set<T> result = new HashSet<T>( setA );
        result.removeAll( setB );
        return result;
    }

    public static <T> Set<T> setIntersection(Set<T> setA, Set<T> setB) {
        Set<T> result = new HashSet<T>( setA );
        result.retainAll( setB );
        return result;
    }

    public static String joinStrings(List<String> stringList, char separator ) {
        return StringUtils.join( stringList, separator );
    }

    public static ObjectMapper getJacksonObjectMapper() {
        SimpleModule module = new SimpleModule("BooleanAsString", new Version(1, 0, 0, null, null, null));

        module.addSerializer(Boolean.class,new BooleanSerializer());
        module.addSerializer(boolean.class,new BooleanSerializer());

        module.addSerializer(Integer.class,new IntegerSerializer());
        module.addSerializer(int.class,new IntegerSerializer());

        module.addSerializer(Long.class,new LongSerializer());
        module.addSerializer(long.class,new LongSerializer());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule( module );

        return mapper;
    }


}

class BooleanSerializer extends JsonSerializer<Boolean> {
    @Override
    public void serialize(Boolean value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
        jgen.writeString(value.toString());
    }
}

class LongSerializer extends JsonSerializer<Long> {
    @Override
    public void serialize(Long value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
        jgen.writeString(value.toString());
    }
}

class IntegerSerializer extends JsonSerializer<Integer> {
    @Override
    public void serialize(Integer value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
        jgen.writeString(value.toString());
    }
}


