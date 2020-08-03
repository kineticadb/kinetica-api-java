/*
 *  This file was autogenerated by the GPUdb schema processor.
 *
 *  DO NOT EDIT DIRECTLY.
 */
package com.gpudb.protocol;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;


/**
 * A set of parameters for {@link
 * com.gpudb.GPUdb#executeProc(ExecuteProcRequest)}.
 * <p>
 * Executes a proc. This endpoint is asynchronous and does not wait for the
 * proc to complete before returning.
 */
public class ExecuteProcRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("ExecuteProcRequest")
            .namespace("com.gpudb")
            .fields()
                .name("procName").type().stringType().noDefault()
                .name("params").type().map().values().stringType().noDefault()
                .name("binParams").type().map().values().bytesType().noDefault()
                .name("inputTableNames").type().array().items().stringType().noDefault()
                .name("inputColumnNames").type().map().values().array().items().stringType().noDefault()
                .name("outputTableNames").type().array().items().stringType().noDefault()
                .name("options").type().map().values().stringType().noDefault()
            .endRecord();


    /**
     * This method supports the Avro framework and is not intended to be called
     * directly by the user.
     * 
     * @return  the schema for the class.
     * 
     */
    public static Schema getClassSchema() {
        return schema$;
    }


    /**
     * Optional parameters.
     * <ul>
     *         <li> {@link
     * com.gpudb.protocol.ExecuteProcRequest.Options#CACHE_INPUT CACHE_INPUT}:
     * A comma-delimited list of table names from {@code inputTableNames} from
     * which input data will be cached for use in subsequent calls to {@link
     * com.gpudb.GPUdb#executeProc(ExecuteProcRequest)} with the {@code
     * use_cached_input} option. Cached input data will be retained until the
     * proc status is cleared with the {@link
     * com.gpudb.GPUdb#showProcStatus(ShowProcStatusRequest) clear_complete}
     * option of {@link com.gpudb.GPUdb#showProcStatus(ShowProcStatusRequest)}
     * and all proc instances using the cached data have completed.  The
     * default value is ''.
     *         <li> {@link
     * com.gpudb.protocol.ExecuteProcRequest.Options#USE_CACHED_INPUT
     * USE_CACHED_INPUT}: A comma-delimited list of run IDs (as returned from
     * prior calls to {@link com.gpudb.GPUdb#executeProc(ExecuteProcRequest)})
     * of running or completed proc instances from which input data cached
     * using the {@code cache_input} option will be used. Cached input data
     * will not be used for any tables specified in {@code inputTableNames},
     * but data from all other tables cached for the specified run IDs will be
     * passed to the proc. If the same table was cached for multiple specified
     * run IDs, the cached data from the first run ID specified in the list
     * that includes that table will be used.  The default value is ''.
     *         <li> {@link
     * com.gpudb.protocol.ExecuteProcRequest.Options#KIFS_INPUT_DIRS
     * KIFS_INPUT_DIRS}: A comma-delimited list of KiFS directories whose local
     * files will be made directly accessible to the proc through the API. (All
     * KiFS files, local or not, are also accessible through the file system
     * below the KiFS mount point.) Each name specified must the name of an
     * existing KiFS directory.  The default value is ''.
     *         <li> {@link
     * com.gpudb.protocol.ExecuteProcRequest.Options#RUN_TAG RUN_TAG}: A string
     * that, if not empty, can be used in subsequent calls to {@link
     * com.gpudb.GPUdb#showProcStatus(ShowProcStatusRequest)} or {@link
     * com.gpudb.GPUdb#killProc(KillProcRequest)} to identify the proc
     * instance.  The default value is ''.
     * </ul>
     * The default value is an empty {@link Map}.
     * A set of string constants for the parameter {@code options}.
     */
    public static final class Options {

        /**
         * A comma-delimited list of table names from {@code inputTableNames}
         * from which input data will be cached for use in subsequent calls to
         * {@link com.gpudb.GPUdb#executeProc(ExecuteProcRequest)} with the
         * {@code use_cached_input} option. Cached input data will be retained
         * until the proc status is cleared with the {@link
         * com.gpudb.GPUdb#showProcStatus(ShowProcStatusRequest)
         * clear_complete} option of {@link
         * com.gpudb.GPUdb#showProcStatus(ShowProcStatusRequest)} and all proc
         * instances using the cached data have completed.  The default value
         * is ''.
         */
        public static final String CACHE_INPUT = "cache_input";

        /**
         * A comma-delimited list of run IDs (as returned from prior calls to
         * {@link com.gpudb.GPUdb#executeProc(ExecuteProcRequest)}) of running
         * or completed proc instances from which input data cached using the
         * {@code cache_input} option will be used. Cached input data will not
         * be used for any tables specified in {@code inputTableNames}, but
         * data from all other tables cached for the specified run IDs will be
         * passed to the proc. If the same table was cached for multiple
         * specified run IDs, the cached data from the first run ID specified
         * in the list that includes that table will be used.  The default
         * value is ''.
         */
        public static final String USE_CACHED_INPUT = "use_cached_input";

        /**
         * A comma-delimited list of KiFS directories whose local files will be
         * made directly accessible to the proc through the API. (All KiFS
         * files, local or not, are also accessible through the file system
         * below the KiFS mount point.) Each name specified must the name of an
         * existing KiFS directory.  The default value is ''.
         */
        public static final String KIFS_INPUT_DIRS = "kifs_input_dirs";

        /**
         * A string that, if not empty, can be used in subsequent calls to
         * {@link com.gpudb.GPUdb#showProcStatus(ShowProcStatusRequest)} or
         * {@link com.gpudb.GPUdb#killProc(KillProcRequest)} to identify the
         * proc instance.  The default value is ''.
         */
        public static final String RUN_TAG = "run_tag";

        private Options() {  }
    }

    private String procName;
    private Map<String, String> params;
    private Map<String, ByteBuffer> binParams;
    private List<String> inputTableNames;
    private Map<String, List<String>> inputColumnNames;
    private List<String> outputTableNames;
    private Map<String, String> options;


    /**
     * Constructs an ExecuteProcRequest object with default parameters.
     */
    public ExecuteProcRequest() {
        procName = "";
        params = new LinkedHashMap<>();
        binParams = new LinkedHashMap<>();
        inputTableNames = new ArrayList<>();
        inputColumnNames = new LinkedHashMap<>();
        outputTableNames = new ArrayList<>();
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs an ExecuteProcRequest object with the specified parameters.
     * 
     * @param procName  Name of the proc to execute. Must be the name of a
     *                  currently existing proc.
     * @param params  A map containing named parameters to pass to the proc.
     *                Each key/value pair specifies the name of a parameter and
     *                its value.  The default value is an empty {@link Map}.
     * @param binParams  A map containing named binary parameters to pass to
     *                   the proc. Each key/value pair specifies the name of a
     *                   parameter and its value.  The default value is an
     *                   empty {@link Map}.
     * @param inputTableNames  Names of the tables containing data to be passed
     *                         to the proc. Each name specified must be the
     *                         name of a currently existing table, in
     *                         [schema_name.]table_name format, using standard
     *                         <a
     *                         href="../../../../../concepts/tables.html#table-name-resolution"
     *                         target="_top">name resolution rules</a>.  If no
     *                         table names are specified, no data will be
     *                         passed to the proc.  The default value is an
     *                         empty {@link List}.
     * @param inputColumnNames  Map of table names from {@code inputTableNames}
     *                          to lists of names of columns from those tables
     *                          that will be passed to the proc. Each column
     *                          name specified must be the name of an existing
     *                          column in the corresponding table. If a table
     *                          name from {@code inputTableNames} is not
     *                          included, all columns from that table will be
     *                          passed to the proc.  The default value is an
     *                          empty {@link Map}.
     * @param outputTableNames  Names of the tables to which output data from
     *                          the proc will be written, each in
     *                          [schema_name.]table_name format, using standard
     *                          <a
     *                          href="../../../../../concepts/tables.html#table-name-resolution"
     *                          target="_top">name resolution rules</a> and
     *                          meeting <a
     *                          href="../../../../../concepts/tables.html#table-naming-criteria"
     *                          target="_top">table naming criteria</a>. If a
     *                          specified table does not exist, it will
     *                          automatically be created with the same schema
     *                          as the corresponding table (by order) from
     *                          {@code inputTableNames}, excluding any primary
     *                          and shard keys. If a specified table is a
     *                          non-persistent result table, it must not have
     *                          primary or shard keys. If no table names are
     *                          specified, no output data can be returned from
     *                          the proc.  The default value is an empty {@link
     *                          List}.
     * @param options  Optional parameters.
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.ExecuteProcRequest.Options#CACHE_INPUT
     *                 CACHE_INPUT}: A comma-delimited list of table names from
     *                 {@code inputTableNames} from which input data will be
     *                 cached for use in subsequent calls to {@link
     *                 com.gpudb.GPUdb#executeProc(ExecuteProcRequest)} with
     *                 the {@code use_cached_input} option. Cached input data
     *                 will be retained until the proc status is cleared with
     *                 the {@link
     *                 com.gpudb.GPUdb#showProcStatus(ShowProcStatusRequest)
     *                 clear_complete} option of {@link
     *                 com.gpudb.GPUdb#showProcStatus(ShowProcStatusRequest)}
     *                 and all proc instances using the cached data have
     *                 completed.  The default value is ''.
     *                         <li> {@link
     *                 com.gpudb.protocol.ExecuteProcRequest.Options#USE_CACHED_INPUT
     *                 USE_CACHED_INPUT}: A comma-delimited list of run IDs (as
     *                 returned from prior calls to {@link
     *                 com.gpudb.GPUdb#executeProc(ExecuteProcRequest)}) of
     *                 running or completed proc instances from which input
     *                 data cached using the {@code cache_input} option will be
     *                 used. Cached input data will not be used for any tables
     *                 specified in {@code inputTableNames}, but data from all
     *                 other tables cached for the specified run IDs will be
     *                 passed to the proc. If the same table was cached for
     *                 multiple specified run IDs, the cached data from the
     *                 first run ID specified in the list that includes that
     *                 table will be used.  The default value is ''.
     *                         <li> {@link
     *                 com.gpudb.protocol.ExecuteProcRequest.Options#KIFS_INPUT_DIRS
     *                 KIFS_INPUT_DIRS}: A comma-delimited list of KiFS
     *                 directories whose local files will be made directly
     *                 accessible to the proc through the API. (All KiFS files,
     *                 local or not, are also accessible through the file
     *                 system below the KiFS mount point.) Each name specified
     *                 must the name of an existing KiFS directory.  The
     *                 default value is ''.
     *                         <li> {@link
     *                 com.gpudb.protocol.ExecuteProcRequest.Options#RUN_TAG
     *                 RUN_TAG}: A string that, if not empty, can be used in
     *                 subsequent calls to {@link
     *                 com.gpudb.GPUdb#showProcStatus(ShowProcStatusRequest)}
     *                 or {@link com.gpudb.GPUdb#killProc(KillProcRequest)} to
     *                 identify the proc instance.  The default value is ''.
     *                 </ul>
     *                 The default value is an empty {@link Map}.
     * 
     */
    public ExecuteProcRequest(String procName, Map<String, String> params, Map<String, ByteBuffer> binParams, List<String> inputTableNames, Map<String, List<String>> inputColumnNames, List<String> outputTableNames, Map<String, String> options) {
        this.procName = (procName == null) ? "" : procName;
        this.params = (params == null) ? new LinkedHashMap<String, String>() : params;
        this.binParams = (binParams == null) ? new LinkedHashMap<String, ByteBuffer>() : binParams;
        this.inputTableNames = (inputTableNames == null) ? new ArrayList<String>() : inputTableNames;
        this.inputColumnNames = (inputColumnNames == null) ? new LinkedHashMap<String, List<String>>() : inputColumnNames;
        this.outputTableNames = (outputTableNames == null) ? new ArrayList<String>() : outputTableNames;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * 
     * @return Name of the proc to execute. Must be the name of a currently
     *         existing proc.
     * 
     */
    public String getProcName() {
        return procName;
    }

    /**
     * 
     * @param procName  Name of the proc to execute. Must be the name of a
     *                  currently existing proc.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public ExecuteProcRequest setProcName(String procName) {
        this.procName = (procName == null) ? "" : procName;
        return this;
    }

    /**
     * 
     * @return A map containing named parameters to pass to the proc. Each
     *         key/value pair specifies the name of a parameter and its value.
     *         The default value is an empty {@link Map}.
     * 
     */
    public Map<String, String> getParams() {
        return params;
    }

    /**
     * 
     * @param params  A map containing named parameters to pass to the proc.
     *                Each key/value pair specifies the name of a parameter and
     *                its value.  The default value is an empty {@link Map}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public ExecuteProcRequest setParams(Map<String, String> params) {
        this.params = (params == null) ? new LinkedHashMap<String, String>() : params;
        return this;
    }

    /**
     * 
     * @return A map containing named binary parameters to pass to the proc.
     *         Each key/value pair specifies the name of a parameter and its
     *         value.  The default value is an empty {@link Map}.
     * 
     */
    public Map<String, ByteBuffer> getBinParams() {
        return binParams;
    }

    /**
     * 
     * @param binParams  A map containing named binary parameters to pass to
     *                   the proc. Each key/value pair specifies the name of a
     *                   parameter and its value.  The default value is an
     *                   empty {@link Map}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public ExecuteProcRequest setBinParams(Map<String, ByteBuffer> binParams) {
        this.binParams = (binParams == null) ? new LinkedHashMap<String, ByteBuffer>() : binParams;
        return this;
    }

    /**
     * 
     * @return Names of the tables containing data to be passed to the proc.
     *         Each name specified must be the name of a currently existing
     *         table, in [schema_name.]table_name format, using standard <a
     *         href="../../../../../concepts/tables.html#table-name-resolution"
     *         target="_top">name resolution rules</a>.  If no table names are
     *         specified, no data will be passed to the proc.  The default
     *         value is an empty {@link List}.
     * 
     */
    public List<String> getInputTableNames() {
        return inputTableNames;
    }

    /**
     * 
     * @param inputTableNames  Names of the tables containing data to be passed
     *                         to the proc. Each name specified must be the
     *                         name of a currently existing table, in
     *                         [schema_name.]table_name format, using standard
     *                         <a
     *                         href="../../../../../concepts/tables.html#table-name-resolution"
     *                         target="_top">name resolution rules</a>.  If no
     *                         table names are specified, no data will be
     *                         passed to the proc.  The default value is an
     *                         empty {@link List}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public ExecuteProcRequest setInputTableNames(List<String> inputTableNames) {
        this.inputTableNames = (inputTableNames == null) ? new ArrayList<String>() : inputTableNames;
        return this;
    }

    /**
     * 
     * @return Map of table names from {@code inputTableNames} to lists of
     *         names of columns from those tables that will be passed to the
     *         proc. Each column name specified must be the name of an existing
     *         column in the corresponding table. If a table name from {@code
     *         inputTableNames} is not included, all columns from that table
     *         will be passed to the proc.  The default value is an empty
     *         {@link Map}.
     * 
     */
    public Map<String, List<String>> getInputColumnNames() {
        return inputColumnNames;
    }

    /**
     * 
     * @param inputColumnNames  Map of table names from {@code inputTableNames}
     *                          to lists of names of columns from those tables
     *                          that will be passed to the proc. Each column
     *                          name specified must be the name of an existing
     *                          column in the corresponding table. If a table
     *                          name from {@code inputTableNames} is not
     *                          included, all columns from that table will be
     *                          passed to the proc.  The default value is an
     *                          empty {@link Map}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public ExecuteProcRequest setInputColumnNames(Map<String, List<String>> inputColumnNames) {
        this.inputColumnNames = (inputColumnNames == null) ? new LinkedHashMap<String, List<String>>() : inputColumnNames;
        return this;
    }

    /**
     * 
     * @return Names of the tables to which output data from the proc will be
     *         written, each in [schema_name.]table_name format, using standard
     *         <a
     *         href="../../../../../concepts/tables.html#table-name-resolution"
     *         target="_top">name resolution rules</a> and meeting <a
     *         href="../../../../../concepts/tables.html#table-naming-criteria"
     *         target="_top">table naming criteria</a>. If a specified table
     *         does not exist, it will automatically be created with the same
     *         schema as the corresponding table (by order) from {@code
     *         inputTableNames}, excluding any primary and shard keys. If a
     *         specified table is a non-persistent result table, it must not
     *         have primary or shard keys. If no table names are specified, no
     *         output data can be returned from the proc.  The default value is
     *         an empty {@link List}.
     * 
     */
    public List<String> getOutputTableNames() {
        return outputTableNames;
    }

    /**
     * 
     * @param outputTableNames  Names of the tables to which output data from
     *                          the proc will be written, each in
     *                          [schema_name.]table_name format, using standard
     *                          <a
     *                          href="../../../../../concepts/tables.html#table-name-resolution"
     *                          target="_top">name resolution rules</a> and
     *                          meeting <a
     *                          href="../../../../../concepts/tables.html#table-naming-criteria"
     *                          target="_top">table naming criteria</a>. If a
     *                          specified table does not exist, it will
     *                          automatically be created with the same schema
     *                          as the corresponding table (by order) from
     *                          {@code inputTableNames}, excluding any primary
     *                          and shard keys. If a specified table is a
     *                          non-persistent result table, it must not have
     *                          primary or shard keys. If no table names are
     *                          specified, no output data can be returned from
     *                          the proc.  The default value is an empty {@link
     *                          List}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public ExecuteProcRequest setOutputTableNames(List<String> outputTableNames) {
        this.outputTableNames = (outputTableNames == null) ? new ArrayList<String>() : outputTableNames;
        return this;
    }

    /**
     * 
     * @return Optional parameters.
     *         <ul>
     *                 <li> {@link
     *         com.gpudb.protocol.ExecuteProcRequest.Options#CACHE_INPUT
     *         CACHE_INPUT}: A comma-delimited list of table names from {@code
     *         inputTableNames} from which input data will be cached for use in
     *         subsequent calls to {@link
     *         com.gpudb.GPUdb#executeProc(ExecuteProcRequest)} with the {@code
     *         use_cached_input} option. Cached input data will be retained
     *         until the proc status is cleared with the {@link
     *         com.gpudb.GPUdb#showProcStatus(ShowProcStatusRequest)
     *         clear_complete} option of {@link
     *         com.gpudb.GPUdb#showProcStatus(ShowProcStatusRequest)} and all
     *         proc instances using the cached data have completed.  The
     *         default value is ''.
     *                 <li> {@link
     *         com.gpudb.protocol.ExecuteProcRequest.Options#USE_CACHED_INPUT
     *         USE_CACHED_INPUT}: A comma-delimited list of run IDs (as
     *         returned from prior calls to {@link
     *         com.gpudb.GPUdb#executeProc(ExecuteProcRequest)}) of running or
     *         completed proc instances from which input data cached using the
     *         {@code cache_input} option will be used. Cached input data will
     *         not be used for any tables specified in {@code inputTableNames},
     *         but data from all other tables cached for the specified run IDs
     *         will be passed to the proc. If the same table was cached for
     *         multiple specified run IDs, the cached data from the first run
     *         ID specified in the list that includes that table will be used.
     *         The default value is ''.
     *                 <li> {@link
     *         com.gpudb.protocol.ExecuteProcRequest.Options#KIFS_INPUT_DIRS
     *         KIFS_INPUT_DIRS}: A comma-delimited list of KiFS directories
     *         whose local files will be made directly accessible to the proc
     *         through the API. (All KiFS files, local or not, are also
     *         accessible through the file system below the KiFS mount point.)
     *         Each name specified must the name of an existing KiFS directory.
     *         The default value is ''.
     *                 <li> {@link
     *         com.gpudb.protocol.ExecuteProcRequest.Options#RUN_TAG RUN_TAG}:
     *         A string that, if not empty, can be used in subsequent calls to
     *         {@link com.gpudb.GPUdb#showProcStatus(ShowProcStatusRequest)} or
     *         {@link com.gpudb.GPUdb#killProc(KillProcRequest)} to identify
     *         the proc instance.  The default value is ''.
     *         </ul>
     *         The default value is an empty {@link Map}.
     * 
     */
    public Map<String, String> getOptions() {
        return options;
    }

    /**
     * 
     * @param options  Optional parameters.
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.ExecuteProcRequest.Options#CACHE_INPUT
     *                 CACHE_INPUT}: A comma-delimited list of table names from
     *                 {@code inputTableNames} from which input data will be
     *                 cached for use in subsequent calls to {@link
     *                 com.gpudb.GPUdb#executeProc(ExecuteProcRequest)} with
     *                 the {@code use_cached_input} option. Cached input data
     *                 will be retained until the proc status is cleared with
     *                 the {@link
     *                 com.gpudb.GPUdb#showProcStatus(ShowProcStatusRequest)
     *                 clear_complete} option of {@link
     *                 com.gpudb.GPUdb#showProcStatus(ShowProcStatusRequest)}
     *                 and all proc instances using the cached data have
     *                 completed.  The default value is ''.
     *                         <li> {@link
     *                 com.gpudb.protocol.ExecuteProcRequest.Options#USE_CACHED_INPUT
     *                 USE_CACHED_INPUT}: A comma-delimited list of run IDs (as
     *                 returned from prior calls to {@link
     *                 com.gpudb.GPUdb#executeProc(ExecuteProcRequest)}) of
     *                 running or completed proc instances from which input
     *                 data cached using the {@code cache_input} option will be
     *                 used. Cached input data will not be used for any tables
     *                 specified in {@code inputTableNames}, but data from all
     *                 other tables cached for the specified run IDs will be
     *                 passed to the proc. If the same table was cached for
     *                 multiple specified run IDs, the cached data from the
     *                 first run ID specified in the list that includes that
     *                 table will be used.  The default value is ''.
     *                         <li> {@link
     *                 com.gpudb.protocol.ExecuteProcRequest.Options#KIFS_INPUT_DIRS
     *                 KIFS_INPUT_DIRS}: A comma-delimited list of KiFS
     *                 directories whose local files will be made directly
     *                 accessible to the proc through the API. (All KiFS files,
     *                 local or not, are also accessible through the file
     *                 system below the KiFS mount point.) Each name specified
     *                 must the name of an existing KiFS directory.  The
     *                 default value is ''.
     *                         <li> {@link
     *                 com.gpudb.protocol.ExecuteProcRequest.Options#RUN_TAG
     *                 RUN_TAG}: A string that, if not empty, can be used in
     *                 subsequent calls to {@link
     *                 com.gpudb.GPUdb#showProcStatus(ShowProcStatusRequest)}
     *                 or {@link com.gpudb.GPUdb#killProc(KillProcRequest)} to
     *                 identify the proc instance.  The default value is ''.
     *                 </ul>
     *                 The default value is an empty {@link Map}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public ExecuteProcRequest setOptions(Map<String, String> options) {
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
        return this;
    }

    /**
     * This method supports the Avro framework and is not intended to be called
     * directly by the user.
     * 
     * @return the schema object describing this class.
     * 
     */
    @Override
    public Schema getSchema() {
        return schema$;
    }

    /**
     * This method supports the Avro framework and is not intended to be called
     * directly by the user.
     * 
     * @param index  the position of the field to get
     * 
     * @return value of the field with the given index.
     * 
     * @throws IndexOutOfBoundsException
     * 
     */
    @Override
    public Object get(int index) {
        switch (index) {
            case 0:
                return this.procName;

            case 1:
                return this.params;

            case 2:
                return this.binParams;

            case 3:
                return this.inputTableNames;

            case 4:
                return this.inputColumnNames;

            case 5:
                return this.outputTableNames;

            case 6:
                return this.options;

            default:
                throw new IndexOutOfBoundsException("Invalid index specified.");
        }
    }

    /**
     * This method supports the Avro framework and is not intended to be called
     * directly by the user.
     * 
     * @param index  the position of the field to set
     * @param value  the value to set
     * 
     * @throws IndexOutOfBoundsException
     * 
     */
    @Override
    @SuppressWarnings("unchecked")
    public void put(int index, Object value) {
        switch (index) {
            case 0:
                this.procName = (String)value;
                break;

            case 1:
                this.params = (Map<String, String>)value;
                break;

            case 2:
                this.binParams = (Map<String, ByteBuffer>)value;
                break;

            case 3:
                this.inputTableNames = (List<String>)value;
                break;

            case 4:
                this.inputColumnNames = (Map<String, List<String>>)value;
                break;

            case 5:
                this.outputTableNames = (List<String>)value;
                break;

            case 6:
                this.options = (Map<String, String>)value;
                break;

            default:
                throw new IndexOutOfBoundsException("Invalid index specified.");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if( obj == this ) {
            return true;
        }

        if( (obj == null) || (obj.getClass() != this.getClass()) ) {
            return false;
        }

        ExecuteProcRequest that = (ExecuteProcRequest)obj;

        return ( this.procName.equals( that.procName )
                 && this.params.equals( that.params )
                 && this.binParams.equals( that.binParams )
                 && this.inputTableNames.equals( that.inputTableNames )
                 && this.inputColumnNames.equals( that.inputColumnNames )
                 && this.outputTableNames.equals( that.outputTableNames )
                 && this.options.equals( that.options ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "procName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.procName ) );
        builder.append( ", " );
        builder.append( gd.toString( "params" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.params ) );
        builder.append( ", " );
        builder.append( gd.toString( "binParams" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.binParams ) );
        builder.append( ", " );
        builder.append( gd.toString( "inputTableNames" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.inputTableNames ) );
        builder.append( ", " );
        builder.append( gd.toString( "inputColumnNames" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.inputColumnNames ) );
        builder.append( ", " );
        builder.append( gd.toString( "outputTableNames" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.outputTableNames ) );
        builder.append( ", " );
        builder.append( gd.toString( "options" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.options ) );
        builder.append( "}" );

        return builder.toString();
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = (31 * hashCode) + this.procName.hashCode();
        hashCode = (31 * hashCode) + this.params.hashCode();
        hashCode = (31 * hashCode) + this.binParams.hashCode();
        hashCode = (31 * hashCode) + this.inputTableNames.hashCode();
        hashCode = (31 * hashCode) + this.inputColumnNames.hashCode();
        hashCode = (31 * hashCode) + this.outputTableNames.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }

}
