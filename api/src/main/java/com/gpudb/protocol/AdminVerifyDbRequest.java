/*
 *  This file was autogenerated by the GPUdb schema processor.
 *
 *  DO NOT EDIT DIRECTLY.
 */
package com.gpudb.protocol;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;


/**
 * A set of parameters for {@link
 * com.gpudb.GPUdb#adminVerifyDb(AdminVerifyDbRequest)}.
 * <p>
 * Verify database is in a consistent state.  When inconsistencies or errors
 * are found, the verified_ok flag in the response is set to false and the list
 * of errors found is provided in the error_list.
 */
public class AdminVerifyDbRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("AdminVerifyDbRequest")
            .namespace("com.gpudb")
            .fields()
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
     * com.gpudb.protocol.AdminVerifyDbRequest.Options#REBUILD_ON_ERROR
     * REBUILD_ON_ERROR}:
     * Supported values:
     * <ul>
     *         <li> {@link com.gpudb.protocol.AdminVerifyDbRequest.Options#TRUE
     * TRUE}
     *         <li> {@link
     * com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE FALSE}
     * </ul>
     * The default value is {@link
     * com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE FALSE}.
     *         <li> {@link
     * com.gpudb.protocol.AdminVerifyDbRequest.Options#VERIFY_NULLS
     * VERIFY_NULLS}: When enabled, verifies that null values are set to zero
     * Supported values:
     * <ul>
     *         <li> {@link com.gpudb.protocol.AdminVerifyDbRequest.Options#TRUE
     * TRUE}
     *         <li> {@link
     * com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE FALSE}
     * </ul>
     * The default value is {@link
     * com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE FALSE}.
     *         <li> {@link
     * com.gpudb.protocol.AdminVerifyDbRequest.Options#VERIFY_PERSIST
     * VERIFY_PERSIST}:
     * Supported values:
     * <ul>
     *         <li> {@link com.gpudb.protocol.AdminVerifyDbRequest.Options#TRUE
     * TRUE}
     *         <li> {@link
     * com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE FALSE}
     * </ul>
     * The default value is {@link
     * com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE FALSE}.
     *         <li> {@link
     * com.gpudb.protocol.AdminVerifyDbRequest.Options#CONCURRENT_SAFE
     * CONCURRENT_SAFE}: When enabled, allows this endpoint to be run safely
     * with other concurrent database operations. Other operations may be
     * slower while this is running.
     * Supported values:
     * <ul>
     *         <li> {@link com.gpudb.protocol.AdminVerifyDbRequest.Options#TRUE
     * TRUE}
     *         <li> {@link
     * com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE FALSE}
     * </ul>
     * The default value is {@link
     * com.gpudb.protocol.AdminVerifyDbRequest.Options#TRUE TRUE}.
     *         <li> {@link
     * com.gpudb.protocol.AdminVerifyDbRequest.Options#VERIFY_RANK0
     * VERIFY_RANK0}: When enabled, compares rank0 table meta-data against
     * workers meta-data
     * Supported values:
     * <ul>
     *         <li> {@link com.gpudb.protocol.AdminVerifyDbRequest.Options#TRUE
     * TRUE}
     *         <li> {@link
     * com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE FALSE}
     * </ul>
     * The default value is {@link
     * com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE FALSE}.
     * </ul>
     * The default value is an empty {@link Map}.
     * A set of string constants for the parameter {@code options}.
     */
    public static final class Options {

        /**
         * Supported values:
         * <ul>
         *         <li> {@link
         * com.gpudb.protocol.AdminVerifyDbRequest.Options#TRUE TRUE}
         *         <li> {@link
         * com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE FALSE}
         * </ul>
         * The default value is {@link
         * com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE FALSE}.
         */
        public static final String REBUILD_ON_ERROR = "rebuild_on_error";
        public static final String TRUE = "true";
        public static final String FALSE = "false";

        /**
         * When enabled, verifies that null values are set to zero
         * Supported values:
         * <ul>
         *         <li> {@link
         * com.gpudb.protocol.AdminVerifyDbRequest.Options#TRUE TRUE}
         *         <li> {@link
         * com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE FALSE}
         * </ul>
         * The default value is {@link
         * com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE FALSE}.
         */
        public static final String VERIFY_NULLS = "verify_nulls";

        /**
         * Supported values:
         * <ul>
         *         <li> {@link
         * com.gpudb.protocol.AdminVerifyDbRequest.Options#TRUE TRUE}
         *         <li> {@link
         * com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE FALSE}
         * </ul>
         * The default value is {@link
         * com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE FALSE}.
         */
        public static final String VERIFY_PERSIST = "verify_persist";

        /**
         * When enabled, allows this endpoint to be run safely with other
         * concurrent database operations. Other operations may be slower while
         * this is running.
         * Supported values:
         * <ul>
         *         <li> {@link
         * com.gpudb.protocol.AdminVerifyDbRequest.Options#TRUE TRUE}
         *         <li> {@link
         * com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE FALSE}
         * </ul>
         * The default value is {@link
         * com.gpudb.protocol.AdminVerifyDbRequest.Options#TRUE TRUE}.
         */
        public static final String CONCURRENT_SAFE = "concurrent_safe";

        /**
         * When enabled, compares rank0 table meta-data against workers
         * meta-data
         * Supported values:
         * <ul>
         *         <li> {@link
         * com.gpudb.protocol.AdminVerifyDbRequest.Options#TRUE TRUE}
         *         <li> {@link
         * com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE FALSE}
         * </ul>
         * The default value is {@link
         * com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE FALSE}.
         */
        public static final String VERIFY_RANK0 = "verify_rank0";

        private Options() {  }
    }

    private Map<String, String> options;


    /**
     * Constructs an AdminVerifyDbRequest object with default parameters.
     */
    public AdminVerifyDbRequest() {
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs an AdminVerifyDbRequest object with the specified parameters.
     * 
     * @param options  Optional parameters.
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#REBUILD_ON_ERROR
     *                 REBUILD_ON_ERROR}:
     *                 Supported values:
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#TRUE
     *                 TRUE}
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE
     *                 FALSE}
     *                 </ul>
     *                 The default value is {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE
     *                 FALSE}.
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#VERIFY_NULLS
     *                 VERIFY_NULLS}: When enabled, verifies that null values
     *                 are set to zero
     *                 Supported values:
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#TRUE
     *                 TRUE}
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE
     *                 FALSE}
     *                 </ul>
     *                 The default value is {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE
     *                 FALSE}.
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#VERIFY_PERSIST
     *                 VERIFY_PERSIST}:
     *                 Supported values:
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#TRUE
     *                 TRUE}
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE
     *                 FALSE}
     *                 </ul>
     *                 The default value is {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE
     *                 FALSE}.
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#CONCURRENT_SAFE
     *                 CONCURRENT_SAFE}: When enabled, allows this endpoint to
     *                 be run safely with other concurrent database operations.
     *                 Other operations may be slower while this is running.
     *                 Supported values:
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#TRUE
     *                 TRUE}
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE
     *                 FALSE}
     *                 </ul>
     *                 The default value is {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#TRUE
     *                 TRUE}.
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#VERIFY_RANK0
     *                 VERIFY_RANK0}: When enabled, compares rank0 table
     *                 meta-data against workers meta-data
     *                 Supported values:
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#TRUE
     *                 TRUE}
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE
     *                 FALSE}
     *                 </ul>
     *                 The default value is {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE
     *                 FALSE}.
     *                 </ul>
     *                 The default value is an empty {@link Map}.
     * 
     */
    public AdminVerifyDbRequest(Map<String, String> options) {
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * 
     * @return Optional parameters.
     *         <ul>
     *                 <li> {@link
     *         com.gpudb.protocol.AdminVerifyDbRequest.Options#REBUILD_ON_ERROR
     *         REBUILD_ON_ERROR}:
     *         Supported values:
     *         <ul>
     *                 <li> {@link
     *         com.gpudb.protocol.AdminVerifyDbRequest.Options#TRUE TRUE}
     *                 <li> {@link
     *         com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link
     *         com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE FALSE}.
     *                 <li> {@link
     *         com.gpudb.protocol.AdminVerifyDbRequest.Options#VERIFY_NULLS
     *         VERIFY_NULLS}: When enabled, verifies that null values are set
     *         to zero
     *         Supported values:
     *         <ul>
     *                 <li> {@link
     *         com.gpudb.protocol.AdminVerifyDbRequest.Options#TRUE TRUE}
     *                 <li> {@link
     *         com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link
     *         com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE FALSE}.
     *                 <li> {@link
     *         com.gpudb.protocol.AdminVerifyDbRequest.Options#VERIFY_PERSIST
     *         VERIFY_PERSIST}:
     *         Supported values:
     *         <ul>
     *                 <li> {@link
     *         com.gpudb.protocol.AdminVerifyDbRequest.Options#TRUE TRUE}
     *                 <li> {@link
     *         com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link
     *         com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE FALSE}.
     *                 <li> {@link
     *         com.gpudb.protocol.AdminVerifyDbRequest.Options#CONCURRENT_SAFE
     *         CONCURRENT_SAFE}: When enabled, allows this endpoint to be run
     *         safely with other concurrent database operations. Other
     *         operations may be slower while this is running.
     *         Supported values:
     *         <ul>
     *                 <li> {@link
     *         com.gpudb.protocol.AdminVerifyDbRequest.Options#TRUE TRUE}
     *                 <li> {@link
     *         com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link
     *         com.gpudb.protocol.AdminVerifyDbRequest.Options#TRUE TRUE}.
     *                 <li> {@link
     *         com.gpudb.protocol.AdminVerifyDbRequest.Options#VERIFY_RANK0
     *         VERIFY_RANK0}: When enabled, compares rank0 table meta-data
     *         against workers meta-data
     *         Supported values:
     *         <ul>
     *                 <li> {@link
     *         com.gpudb.protocol.AdminVerifyDbRequest.Options#TRUE TRUE}
     *                 <li> {@link
     *         com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link
     *         com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE FALSE}.
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
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#REBUILD_ON_ERROR
     *                 REBUILD_ON_ERROR}:
     *                 Supported values:
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#TRUE
     *                 TRUE}
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE
     *                 FALSE}
     *                 </ul>
     *                 The default value is {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE
     *                 FALSE}.
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#VERIFY_NULLS
     *                 VERIFY_NULLS}: When enabled, verifies that null values
     *                 are set to zero
     *                 Supported values:
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#TRUE
     *                 TRUE}
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE
     *                 FALSE}
     *                 </ul>
     *                 The default value is {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE
     *                 FALSE}.
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#VERIFY_PERSIST
     *                 VERIFY_PERSIST}:
     *                 Supported values:
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#TRUE
     *                 TRUE}
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE
     *                 FALSE}
     *                 </ul>
     *                 The default value is {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE
     *                 FALSE}.
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#CONCURRENT_SAFE
     *                 CONCURRENT_SAFE}: When enabled, allows this endpoint to
     *                 be run safely with other concurrent database operations.
     *                 Other operations may be slower while this is running.
     *                 Supported values:
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#TRUE
     *                 TRUE}
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE
     *                 FALSE}
     *                 </ul>
     *                 The default value is {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#TRUE
     *                 TRUE}.
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#VERIFY_RANK0
     *                 VERIFY_RANK0}: When enabled, compares rank0 table
     *                 meta-data against workers meta-data
     *                 Supported values:
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#TRUE
     *                 TRUE}
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE
     *                 FALSE}
     *                 </ul>
     *                 The default value is {@link
     *                 com.gpudb.protocol.AdminVerifyDbRequest.Options#FALSE
     *                 FALSE}.
     *                 </ul>
     *                 The default value is an empty {@link Map}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public AdminVerifyDbRequest setOptions(Map<String, String> options) {
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

        AdminVerifyDbRequest that = (AdminVerifyDbRequest)obj;

        return ( this.options.equals( that.options ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "options" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.options ) );
        builder.append( "}" );

        return builder.toString();
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }

}
