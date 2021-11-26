package com.gpudb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class GPUdbLoggerTest {

    private static final Path TEST_LOG_FILE_PATH = Paths.get("build/logs/gpudb-api.log");

    @BeforeEach
    public void beforeEach() {
        try {
            Files.deleteIfExists(TEST_LOG_FILE_PATH);
        } catch (IOException e) {
            System.out.printf("Could not delete %s due to %s%n", TEST_LOG_FILE_PATH, e);
        }
    }

    @Test
    public void testLogMessagesAreAppendedToLogFile() throws Exception {
        GPUdbLogger.trace("Test log message");
        GPUdbLogger.debug("Test log message");
        GPUdbLogger.info("Test log message");
        GPUdbLogger.warn("Test log message");
        GPUdbLogger.error("Test log message");

        // give some time for the logged messages to be flushed to the file
        Thread.sleep(5000);
        List<String> traceMessages = Files.lines(TEST_LOG_FILE_PATH)
                .filter(line -> line.contains("TRACE"))
                .collect(Collectors.<String>toList());
        assertTrue(traceMessages.isEmpty());
        List<String> nonTraceMessages = Files.lines(TEST_LOG_FILE_PATH)
                .filter(line -> !line.contains("TRACE"))
                .collect(Collectors.toList());
        assertEquals(4, nonTraceMessages.size());
    }

}