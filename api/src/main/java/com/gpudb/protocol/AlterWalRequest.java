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
 * A set of parameters for {@link com.gpudb.GPUdb#alterWal(AlterWalRequest)
 * GPUdb.alterWal}.
 * <p>
 * Alters table wal settings.
 * Returns information about the requested table wal modifications.
 */
public class AlterWalRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("AlterWalRequest")
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
     * A set of string constants for the {@link AlterWalRequest} parameter
     * {@link #getOptions() options}.
     * <p>
     * Optional parameters.
     */
    public static final class Options {
        /**
         * Maximum size of an individual segment file
         */
        public static final String MAX_SEGMENT_SIZE = "max_segment_size";

        /**
         * Approximate number of segment files to split the wal across. Must be
         * at least two.
         */
        public static final String SEGMENT_COUNT = "segment_count";

        /**
         * Maximum size of an individual segment file.
         * Supported values:
         * <ul>
         *     <li>{@link Options#NONE NONE}: Disables the wal
         *     <li>{@link Options#BACKGROUND BACKGROUND}: Wal entries are
         *         periodically written instead of immediately after each
         *         operation
         *     <li>{@link Options#FLUSH FLUSH}: Protects entries in the event
         *         of a database crash
         *     <li>{@link Options#FSYNC FSYNC}: Protects entries in the event
         *         of an OS crash
         * </ul>
         */
        public static final String SYNC_POLICY = "sync_policy";

        /**
         * Disables the wal
         */
        public static final String NONE = "none";

        /**
         * Wal entries are periodically written instead of immediately after
         * each operation
         */
        public static final String BACKGROUND = "background";

        /**
         * Protects entries in the event of a database crash
         */
        public static final String FLUSH = "flush";

        /**
         * Protects entries in the event of an OS crash
         */
        public static final String FSYNC = "fsync";

        /**
         * Specifies how frequently wal entries are written with background
         * sync. This is a global setting and can only be used with the system
         * {options.table_names} specifier '*'.
         */
        public static final String FLUSH_FREQUENCY = "flush_frequency";

        /**
         * If {@link Options#TRUE TRUE} each entry will be checked against a
         * protective checksum.
         * Supported values:
         * <ul>
         *     <li>{@link Options#TRUE TRUE}
         *     <li>{@link Options#FALSE FALSE}
         * </ul>
         * The default value is {@link Options#TRUE TRUE}.
         */
        public static final String CHECKSUM = "checksum";

        public static final String TRUE = "true";
        public static final String FALSE = "false";

        /**
         * If {@link Options#TRUE TRUE} tables with unique wal settings will be
         * overridden when applying a system level change.
         * Supported values:
         * <ul>
         *     <li>{@link Options#TRUE TRUE}
         *     <li>{@link Options#FALSE FALSE}
         * </ul>
         * The default value is {@link Options#FALSE FALSE}.
         */
        public static final String OVERRIDE_NON_DEFAULT = "override_non_default";

        /**
         * If {@link Options#TRUE TRUE} tables with unique wal settings will be
         * reverted to the current global settings. Cannot be used in
         * conjunction with any other option.
         * Supported values:
         * <ul>
         *     <li>{@link Options#TRUE TRUE}
         *     <li>{@link Options#FALSE FALSE}
         * </ul>
         * The default value is {@link Options#FALSE FALSE}.
         */
        public static final String RESTORE_SYSTEM_SETTINGS = "restore_system_settings";

        /**
         * If {@link Options#TRUE TRUE} and a system-level change was
         * requested, the system configuration will be written to disk upon
         * successful application of this request. This will commit the changes
         * from this request and any additional in-memory modifications.
         * Supported values:
         * <ul>
         *     <li>{@link Options#TRUE TRUE}
         *     <li>{@link Options#FALSE FALSE}
         * </ul>
         * The default value is {@link Options#TRUE TRUE}.
         */
        public static final String PERSIST = "persist";

        private Options() {  }
    }

    private List<String> tableNames;
    private Map<String, String> options;

    /**
     * Constructs an AlterWalRequest object with default parameters.
     */
    public AlterWalRequest() {
        tableNames = new ArrayList<>();
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs an AlterWalRequest object with the specified parameters.
     *
     * @param tableNames  List of tables to modify. An asterisk changes the
     *                    system settings.
     * @param options  Optional parameters.
     *                 <ul>
     *                     <li>{@link Options#MAX_SEGMENT_SIZE
     *                         MAX_SEGMENT_SIZE}: Maximum size of an individual
     *                         segment file
     *                     <li>{@link Options#SEGMENT_COUNT SEGMENT_COUNT}:
     *                         Approximate number of segment files to split the
     *                         wal across. Must be at least two.
     *                     <li>{@link Options#SYNC_POLICY SYNC_POLICY}: Maximum
     *                         size of an individual segment file.
     *                         Supported values:
     *                         <ul>
     *                             <li>{@link Options#NONE NONE}: Disables the
     *                                 wal
     *                             <li>{@link Options#BACKGROUND BACKGROUND}:
     *                                 Wal entries are periodically written
     *                                 instead of immediately after each
     *                                 operation
     *                             <li>{@link Options#FLUSH FLUSH}: Protects
     *                                 entries in the event of a database crash
     *                             <li>{@link Options#FSYNC FSYNC}: Protects
     *                                 entries in the event of an OS crash
     *                         </ul>
     *                     <li>{@link Options#FLUSH_FREQUENCY FLUSH_FREQUENCY}:
     *                         Specifies how frequently wal entries are written
     *                         with background sync. This is a global setting
     *                         and can only be used with the system
     *                         {options.table_names} specifier '*'.
     *                     <li>{@link Options#CHECKSUM CHECKSUM}: If {@link
     *                         Options#TRUE TRUE} each entry will be checked
     *                         against a protective checksum.
     *                         Supported values:
     *                         <ul>
     *                             <li>{@link Options#TRUE TRUE}
     *                             <li>{@link Options#FALSE FALSE}
     *                         </ul>
     *                         The default value is {@link Options#TRUE TRUE}.
     *                     <li>{@link Options#OVERRIDE_NON_DEFAULT
     *                         OVERRIDE_NON_DEFAULT}: If {@link Options#TRUE
     *                         TRUE} tables with unique wal settings will be
     *                         overridden when applying a system level change.
     *                         Supported values:
     *                         <ul>
     *                             <li>{@link Options#TRUE TRUE}
     *                             <li>{@link Options#FALSE FALSE}
     *                         </ul>
     *                         The default value is {@link Options#FALSE
     *                         FALSE}.
     *                     <li>{@link Options#RESTORE_SYSTEM_SETTINGS
     *                         RESTORE_SYSTEM_SETTINGS}: If {@link Options#TRUE
     *                         TRUE} tables with unique wal settings will be
     *                         reverted to the current global settings. Cannot
     *                         be used in conjunction with any other option.
     *                         Supported values:
     *                         <ul>
     *                             <li>{@link Options#TRUE TRUE}
     *                             <li>{@link Options#FALSE FALSE}
     *                         </ul>
     *                         The default value is {@link Options#FALSE
     *                         FALSE}.
     *                     <li>{@link Options#PERSIST PERSIST}: If {@link
     *                         Options#TRUE TRUE} and a system-level change was
     *                         requested, the system configuration will be
     *                         written to disk upon successful application of
     *                         this request. This will commit the changes from
     *                         this request and any additional in-memory
     *                         modifications.
     *                         Supported values:
     *                         <ul>
     *                             <li>{@link Options#TRUE TRUE}
     *                             <li>{@link Options#FALSE FALSE}
     *                         </ul>
     *                         The default value is {@link Options#TRUE TRUE}.
     *                 </ul>
     *                 The default value is an empty {@link Map}.
     */
    public AlterWalRequest(List<String> tableNames, Map<String, String> options) {
        this.tableNames = (tableNames == null) ? new ArrayList<String>() : tableNames;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * List of tables to modify. An asterisk changes the system settings.
     *
     * @return The current value of {@code tableNames}.
     */
    public List<String> getTableNames() {
        return tableNames;
    }

    /**
     * List of tables to modify. An asterisk changes the system settings.
     *
     * @param tableNames  The new value for {@code tableNames}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AlterWalRequest setTableNames(List<String> tableNames) {
        this.tableNames = (tableNames == null) ? new ArrayList<String>() : tableNames;
        return this;
    }

    /**
     * Optional parameters.
     * <ul>
     *     <li>{@link Options#MAX_SEGMENT_SIZE MAX_SEGMENT_SIZE}: Maximum size
     *         of an individual segment file
     *     <li>{@link Options#SEGMENT_COUNT SEGMENT_COUNT}: Approximate number
     *         of segment files to split the wal across. Must be at least two.
     *     <li>{@link Options#SYNC_POLICY SYNC_POLICY}: Maximum size of an
     *         individual segment file.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#NONE NONE}: Disables the wal
     *             <li>{@link Options#BACKGROUND BACKGROUND}: Wal entries are
     *                 periodically written instead of immediately after each
     *                 operation
     *             <li>{@link Options#FLUSH FLUSH}: Protects entries in the
     *                 event of a database crash
     *             <li>{@link Options#FSYNC FSYNC}: Protects entries in the
     *                 event of an OS crash
     *         </ul>
     *     <li>{@link Options#FLUSH_FREQUENCY FLUSH_FREQUENCY}: Specifies how
     *         frequently wal entries are written with background sync. This is
     *         a global setting and can only be used with the system
     *         {options.table_names} specifier '*'.
     *     <li>{@link Options#CHECKSUM CHECKSUM}: If {@link Options#TRUE TRUE}
     *         each entry will be checked against a protective checksum.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#TRUE TRUE}.
     *     <li>{@link Options#OVERRIDE_NON_DEFAULT OVERRIDE_NON_DEFAULT}: If
     *         {@link Options#TRUE TRUE} tables with unique wal settings will
     *         be overridden when applying a system level change.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#FALSE FALSE}.
     *     <li>{@link Options#RESTORE_SYSTEM_SETTINGS RESTORE_SYSTEM_SETTINGS}:
     *         If {@link Options#TRUE TRUE} tables with unique wal settings
     *         will be reverted to the current global settings. Cannot be used
     *         in conjunction with any other option.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#FALSE FALSE}.
     *     <li>{@link Options#PERSIST PERSIST}: If {@link Options#TRUE TRUE}
     *         and a system-level change was requested, the system
     *         configuration will be written to disk upon successful
     *         application of this request. This will commit the changes from
     *         this request and any additional in-memory modifications.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#TRUE TRUE}.
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
     *     <li>{@link Options#MAX_SEGMENT_SIZE MAX_SEGMENT_SIZE}: Maximum size
     *         of an individual segment file
     *     <li>{@link Options#SEGMENT_COUNT SEGMENT_COUNT}: Approximate number
     *         of segment files to split the wal across. Must be at least two.
     *     <li>{@link Options#SYNC_POLICY SYNC_POLICY}: Maximum size of an
     *         individual segment file.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#NONE NONE}: Disables the wal
     *             <li>{@link Options#BACKGROUND BACKGROUND}: Wal entries are
     *                 periodically written instead of immediately after each
     *                 operation
     *             <li>{@link Options#FLUSH FLUSH}: Protects entries in the
     *                 event of a database crash
     *             <li>{@link Options#FSYNC FSYNC}: Protects entries in the
     *                 event of an OS crash
     *         </ul>
     *     <li>{@link Options#FLUSH_FREQUENCY FLUSH_FREQUENCY}: Specifies how
     *         frequently wal entries are written with background sync. This is
     *         a global setting and can only be used with the system
     *         {options.table_names} specifier '*'.
     *     <li>{@link Options#CHECKSUM CHECKSUM}: If {@link Options#TRUE TRUE}
     *         each entry will be checked against a protective checksum.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#TRUE TRUE}.
     *     <li>{@link Options#OVERRIDE_NON_DEFAULT OVERRIDE_NON_DEFAULT}: If
     *         {@link Options#TRUE TRUE} tables with unique wal settings will
     *         be overridden when applying a system level change.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#FALSE FALSE}.
     *     <li>{@link Options#RESTORE_SYSTEM_SETTINGS RESTORE_SYSTEM_SETTINGS}:
     *         If {@link Options#TRUE TRUE} tables with unique wal settings
     *         will be reverted to the current global settings. Cannot be used
     *         in conjunction with any other option.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#FALSE FALSE}.
     *     <li>{@link Options#PERSIST PERSIST}: If {@link Options#TRUE TRUE}
     *         and a system-level change was requested, the system
     *         configuration will be written to disk upon successful
     *         application of this request. This will commit the changes from
     *         this request and any additional in-memory modifications.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#TRUE TRUE}.
     * </ul>
     * The default value is an empty {@link Map}.
     *
     * @param options  The new value for {@code options}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AlterWalRequest setOptions(Map<String, String> options) {
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

        AlterWalRequest that = (AlterWalRequest)obj;

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