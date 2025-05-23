/*
 *  This file was autogenerated by the Kinetica schema processor.
 *
 *  DO NOT EDIT DIRECTLY.
 */
package com.gpudb.protocol;

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
 * com.gpudb.GPUdb#adminRepairTable(AdminRepairTableRequest)
 * GPUdb.adminRepairTable}.
 * <p>
 * Manually repair a corrupted table.
 * Returns information about affected tables.
 */
public class AdminRepairTableRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("AdminRepairTableRequest")
            .namespace("com.gpudb")
            .fields()
                .name("tableNames").type().array().items().stringType().noDefault()
                .name("options").type().map().values().stringType().noDefault()
            .endRecord();

    /**
     * This method supports the Avro framework and is not intended to be called
     * directly by the user.
     *
     * @return The schema for the class.
     */
    public static Schema getClassSchema() {
        return schema$;
    }

    /**
     * A set of string constants for the {@link AdminRepairTableRequest}
     * parameter {@link #getOptions() options}.
     * <p>
     * Optional parameters.
     */
    public static final class Options {
        /**
         * Corrective action to take.
         * Supported values:
         * <ul>
         *     <li>{@link Options#DELETE_CHUNKS DELETE_CHUNKS}: Deletes any
         *         corrupted chunks
         *     <li>{@link Options#SHRINK_COLUMNS SHRINK_COLUMNS}: Shrinks
         *         corrupted chunks to the shortest column
         *     <li>{@link Options#REPLAY_WAL REPLAY_WAL}: Manually invokes wal
         *         replay on the table
         * </ul>
         */
        public static final String REPAIR_POLICY = "repair_policy";

        /**
         * Deletes any corrupted chunks
         */
        public static final String DELETE_CHUNKS = "delete_chunks";

        /**
         * Shrinks corrupted chunks to the shortest column
         */
        public static final String SHRINK_COLUMNS = "shrink_columns";

        /**
         * Manually invokes wal replay on the table
         */
        public static final String REPLAY_WAL = "replay_wal";

        /**
         * If {@link Options#FALSE FALSE} only table chunk data already known
         * to be corrupted will be repaired. Otherwise the database will
         * perform a full table scan to check for correctness.
         * Supported values:
         * <ul>
         *     <li>{@link Options#TRUE TRUE}
         *     <li>{@link Options#FALSE FALSE}
         * </ul>
         * The default value is {@link Options#FALSE FALSE}.
         */
        public static final String VERIFY_ALL = "verify_all";

        public static final String TRUE = "true";
        public static final String FALSE = "false";

        private Options() {  }
    }

    private List<String> tableNames;
    private Map<String, String> options;

    /**
     * Constructs an AdminRepairTableRequest object with default parameters.
     */
    public AdminRepairTableRequest() {
        tableNames = new ArrayList<>();
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs an AdminRepairTableRequest object with the specified
     * parameters.
     *
     * @param tableNames  List of tables to query. An asterisk returns all
     *                    tables.
     * @param options  Optional parameters.
     *                 <ul>
     *                     <li>{@link Options#REPAIR_POLICY REPAIR_POLICY}:
     *                         Corrective action to take.
     *                         Supported values:
     *                         <ul>
     *                             <li>{@link Options#DELETE_CHUNKS
     *                                 DELETE_CHUNKS}: Deletes any corrupted
     *                                 chunks
     *                             <li>{@link Options#SHRINK_COLUMNS
     *                                 SHRINK_COLUMNS}: Shrinks corrupted
     *                                 chunks to the shortest column
     *                             <li>{@link Options#REPLAY_WAL REPLAY_WAL}:
     *                                 Manually invokes wal replay on the table
     *                         </ul>
     *                     <li>{@link Options#VERIFY_ALL VERIFY_ALL}: If {@link
     *                         Options#FALSE FALSE} only table chunk data
     *                         already known to be corrupted will be repaired.
     *                         Otherwise the database will perform a full table
     *                         scan to check for correctness.
     *                         Supported values:
     *                         <ul>
     *                             <li>{@link Options#TRUE TRUE}
     *                             <li>{@link Options#FALSE FALSE}
     *                         </ul>
     *                         The default value is {@link Options#FALSE
     *                         FALSE}.
     *                 </ul>
     *                 The default value is an empty {@link Map}.
     */
    public AdminRepairTableRequest(List<String> tableNames, Map<String, String> options) {
        this.tableNames = (tableNames == null) ? new ArrayList<String>() : tableNames;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * List of tables to query. An asterisk returns all tables.
     *
     * @return The current value of {@code tableNames}.
     */
    public List<String> getTableNames() {
        return tableNames;
    }

    /**
     * List of tables to query. An asterisk returns all tables.
     *
     * @param tableNames  The new value for {@code tableNames}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AdminRepairTableRequest setTableNames(List<String> tableNames) {
        this.tableNames = (tableNames == null) ? new ArrayList<String>() : tableNames;
        return this;
    }

    /**
     * Optional parameters.
     * <ul>
     *     <li>{@link Options#REPAIR_POLICY REPAIR_POLICY}: Corrective action
     *         to take.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#DELETE_CHUNKS DELETE_CHUNKS}: Deletes any
     *                 corrupted chunks
     *             <li>{@link Options#SHRINK_COLUMNS SHRINK_COLUMNS}: Shrinks
     *                 corrupted chunks to the shortest column
     *             <li>{@link Options#REPLAY_WAL REPLAY_WAL}: Manually invokes
     *                 wal replay on the table
     *         </ul>
     *     <li>{@link Options#VERIFY_ALL VERIFY_ALL}: If {@link Options#FALSE
     *         FALSE} only table chunk data already known to be corrupted will
     *         be repaired. Otherwise the database will perform a full table
     *         scan to check for correctness.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#FALSE FALSE}.
     * </ul>
     * The default value is an empty {@link Map}.
     *
     * @return The current value of {@code options}.
     */
    public Map<String, String> getOptions() {
        return options;
    }

    /**
     * Optional parameters.
     * <ul>
     *     <li>{@link Options#REPAIR_POLICY REPAIR_POLICY}: Corrective action
     *         to take.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#DELETE_CHUNKS DELETE_CHUNKS}: Deletes any
     *                 corrupted chunks
     *             <li>{@link Options#SHRINK_COLUMNS SHRINK_COLUMNS}: Shrinks
     *                 corrupted chunks to the shortest column
     *             <li>{@link Options#REPLAY_WAL REPLAY_WAL}: Manually invokes
     *                 wal replay on the table
     *         </ul>
     *     <li>{@link Options#VERIFY_ALL VERIFY_ALL}: If {@link Options#FALSE
     *         FALSE} only table chunk data already known to be corrupted will
     *         be repaired. Otherwise the database will perform a full table
     *         scan to check for correctness.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#FALSE FALSE}.
     * </ul>
     * The default value is an empty {@link Map}.
     *
     * @param options  The new value for {@code options}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AdminRepairTableRequest setOptions(Map<String, String> options) {
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
        return this;
    }

    /**
     * This method supports the Avro framework and is not intended to be called
     * directly by the user.
     *
     * @return The schema object describing this class.
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
     */
    @Override
    public Object get(int index) {
        switch (index) {
            case 0:
                return this.tableNames;

            case 1:
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
     */
    @Override
    @SuppressWarnings("unchecked")
    public void put(int index, Object value) {
        switch (index) {
            case 0:
                this.tableNames = (List<String>)value;
                break;

            case 1:
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

        AdminRepairTableRequest that = (AdminRepairTableRequest)obj;

        return ( this.tableNames.equals( that.tableNames )
                 && this.options.equals( that.options ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "tableNames" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.tableNames ) );
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
        hashCode = (31 * hashCode) + this.tableNames.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }
}
