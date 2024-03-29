/*
 *  This file was autogenerated by the Kinetica schema processor.
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
 * com.gpudb.GPUdb#appendRecords(AppendRecordsRequest) GPUdb.appendRecords}.
 * <p>
 * Append (or insert) all records from a source table (specified by {@link
 * #getSourceTableName() sourceTableName}) to a particular target table
 * (specified by {@link #getTableName() tableName}). The field map (specified
 * by {@link #getFieldMap() fieldMap}) holds the user specified map of target
 * table column names with their mapped source column names.
 */
public class AppendRecordsRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("AppendRecordsRequest")
            .namespace("com.gpudb")
            .fields()
                .name("tableName").type().stringType().noDefault()
                .name("sourceTableName").type().stringType().noDefault()
                .name("fieldMap").type().map().values().stringType().noDefault()
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
     * A set of string constants for the {@link AppendRecordsRequest} parameter
     * {@link #getOptions() options}.
     * <p>
     * Optional parameters.
     */
    public static final class Options {
        /**
         * A positive integer indicating the number of initial results to skip
         * from {@link #getSourceTableName() sourceTableName}. Default is 0.
         * The minimum allowed value is 0. The maximum allowed value is
         * MAX_INT. The default value is '0'.
         */
        public static final String OFFSET = "offset";

        /**
         * A positive integer indicating the maximum number of results to be
         * returned from {@link #getSourceTableName() sourceTableName}. Or
         * END_OF_SET (-9999) to indicate that the max number of results should
         * be returned. The default value is '-9999'.
         */
        public static final String LIMIT = "limit";

        /**
         * Optional filter expression to apply to the {@link
         * #getSourceTableName() sourceTableName}. The default value is ''.
         */
        public static final String EXPRESSION = "expression";

        /**
         * Comma-separated list of the columns to be sorted by from source
         * table (specified by {@link #getSourceTableName() sourceTableName}),
         * e.g., 'timestamp asc, x desc'. The {@link Options#ORDER_BY ORDER_BY}
         * columns do not have to be present in {@link #getFieldMap()
         * fieldMap}. The default value is ''.
         */
        public static final String ORDER_BY = "order_by";

        /**
         * Specifies the record collision policy for inserting source table
         * records (specified by {@link #getSourceTableName() sourceTableName})
         * into a target table (specified by {@link #getTableName() tableName})
         * with a <a href="../../../../../../concepts/tables/#primary-keys"
         * target="_top">primary key</a>. If set to {@link Options#TRUE TRUE},
         * any existing table record with primary key values that match those
         * of a source table record being inserted will be replaced by that new
         * record (the new data will be "upserted"). If set to {@link
         * Options#FALSE FALSE}, any existing table record with primary key
         * values that match those of a source table record being inserted will
         * remain unchanged, while the source record will be rejected and an
         * error handled as determined by {@link Options#IGNORE_EXISTING_PK
         * IGNORE_EXISTING_PK}.  If the specified table does not have a primary
         * key, then this option has no effect.
         * Supported values:
         * <ul>
         *     <li>{@link Options#TRUE TRUE}: Upsert new records when primary
         *         keys match existing records
         *     <li>{@link Options#FALSE FALSE}: Reject new records when primary
         *         keys match existing records
         * </ul>
         * The default value is {@link Options#FALSE FALSE}.
         */
        public static final String UPDATE_ON_EXISTING_PK = "update_on_existing_pk";

        public static final String TRUE = "true";
        public static final String FALSE = "false";

        /**
         * Specifies the record collision error-suppression policy for
         * inserting source table records (specified by {@link
         * #getSourceTableName() sourceTableName}) into a target table
         * (specified by {@link #getTableName() tableName}) with a <a
         * href="../../../../../../concepts/tables/#primary-keys"
         * target="_top">primary key</a>, only used when not in upsert mode
         * (upsert mode is disabled when {@link Options#UPDATE_ON_EXISTING_PK
         * UPDATE_ON_EXISTING_PK} is {@link Options#FALSE FALSE}).  If set to
         * {@link Options#TRUE TRUE}, any source table record being inserted
         * that is rejected for having primary key values that match those of
         * an existing target table record will be ignored with no error
         * generated.  If {@link Options#FALSE FALSE}, the rejection of any
         * source table record for having primary key values matching an
         * existing target table record will result in an error being raised.
         * If the specified table does not have a primary key or if upsert mode
         * is in effect ({@link Options#UPDATE_ON_EXISTING_PK
         * UPDATE_ON_EXISTING_PK} is {@link Options#TRUE TRUE}), then this
         * option has no effect.
         * Supported values:
         * <ul>
         *     <li>{@link Options#TRUE TRUE}: Ignore source table records whose
         *         primary key values collide with those of target table
         *         records
         *     <li>{@link Options#FALSE FALSE}: Raise an error for any source
         *         table record whose primary key values collide with those of
         *         a target table record
         * </ul>
         * The default value is {@link Options#FALSE FALSE}.
         */
        public static final String IGNORE_EXISTING_PK = "ignore_existing_pk";

        /**
         * If set to {@link Options#TRUE TRUE}, it allows inserting longer
         * strings into smaller charN string columns by truncating the longer
         * strings to fit.
         * Supported values:
         * <ul>
         *     <li>{@link Options#TRUE TRUE}
         *     <li>{@link Options#FALSE FALSE}
         * </ul>
         * The default value is {@link Options#FALSE FALSE}.
         */
        public static final String TRUNCATE_STRINGS = "truncate_strings";

        private Options() {  }
    }

    private String tableName;
    private String sourceTableName;
    private Map<String, String> fieldMap;
    private Map<String, String> options;

    /**
     * Constructs an AppendRecordsRequest object with default parameters.
     */
    public AppendRecordsRequest() {
        tableName = "";
        sourceTableName = "";
        fieldMap = new LinkedHashMap<>();
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs an AppendRecordsRequest object with the specified parameters.
     *
     * @param tableName  The table name for the records to be appended, in
     *                   [schema_name.]table_name format, using standard <a
     *                   href="../../../../../../concepts/tables/#table-name-resolution"
     *                   target="_top">name resolution rules</a>.  Must be an
     *                   existing table.
     * @param sourceTableName  The source table name to get records from, in
     *                         [schema_name.]table_name format, using standard
     *                         <a
     *                         href="../../../../../../concepts/tables/#table-name-resolution"
     *                         target="_top">name resolution rules</a>.  Must
     *                         be an existing table name.
     * @param fieldMap  Contains the mapping of column names from the target
     *                  table (specified by {@code tableName}) as the keys, and
     *                  corresponding column names or expressions (e.g.,
     *                  'col_name+1') from the source table (specified by
     *                  {@code sourceTableName}). Must be existing column names
     *                  in source table and target table, and their types must
     *                  be matched. For details on using expressions, see <a
     *                  href="../../../../../../concepts/expressions/"
     *                  target="_top">Expressions</a>.
     * @param options  Optional parameters.
     *                 <ul>
     *                     <li>{@link Options#OFFSET OFFSET}: A positive
     *                         integer indicating the number of initial results
     *                         to skip from {@code sourceTableName}. Default is
     *                         0. The minimum allowed value is 0. The maximum
     *                         allowed value is MAX_INT. The default value is
     *                         '0'.
     *                     <li>{@link Options#LIMIT LIMIT}: A positive integer
     *                         indicating the maximum number of results to be
     *                         returned from {@code sourceTableName}. Or
     *                         END_OF_SET (-9999) to indicate that the max
     *                         number of results should be returned. The
     *                         default value is '-9999'.
     *                     <li>{@link Options#EXPRESSION EXPRESSION}: Optional
     *                         filter expression to apply to the {@code
     *                         sourceTableName}. The default value is ''.
     *                     <li>{@link Options#ORDER_BY ORDER_BY}:
     *                         Comma-separated list of the columns to be sorted
     *                         by from source table (specified by {@code
     *                         sourceTableName}), e.g., 'timestamp asc, x
     *                         desc'. The {@link Options#ORDER_BY ORDER_BY}
     *                         columns do not have to be present in {@code
     *                         fieldMap}. The default value is ''.
     *                     <li>{@link Options#UPDATE_ON_EXISTING_PK
     *                         UPDATE_ON_EXISTING_PK}: Specifies the record
     *                         collision policy for inserting source table
     *                         records (specified by {@code sourceTableName})
     *                         into a target table (specified by {@code
     *                         tableName}) with a <a
     *                         href="../../../../../../concepts/tables/#primary-keys"
     *                         target="_top">primary key</a>. If set to {@link
     *                         Options#TRUE TRUE}, any existing table record
     *                         with primary key values that match those of a
     *                         source table record being inserted will be
     *                         replaced by that new record (the new data will
     *                         be "upserted"). If set to {@link Options#FALSE
     *                         FALSE}, any existing table record with primary
     *                         key values that match those of a source table
     *                         record being inserted will remain unchanged,
     *                         while the source record will be rejected and an
     *                         error handled as determined by {@link
     *                         Options#IGNORE_EXISTING_PK IGNORE_EXISTING_PK}.
     *                         If the specified table does not have a primary
     *                         key, then this option has no effect.
     *                         Supported values:
     *                         <ul>
     *                             <li>{@link Options#TRUE TRUE}: Upsert new
     *                                 records when primary keys match existing
     *                                 records
     *                             <li>{@link Options#FALSE FALSE}: Reject new
     *                                 records when primary keys match existing
     *                                 records
     *                         </ul>
     *                         The default value is {@link Options#FALSE
     *                         FALSE}.
     *                     <li>{@link Options#IGNORE_EXISTING_PK
     *                         IGNORE_EXISTING_PK}: Specifies the record
     *                         collision error-suppression policy for inserting
     *                         source table records (specified by {@code
     *                         sourceTableName}) into a target table (specified
     *                         by {@code tableName}) with a <a
     *                         href="../../../../../../concepts/tables/#primary-keys"
     *                         target="_top">primary key</a>, only used when
     *                         not in upsert mode (upsert mode is disabled when
     *                         {@link Options#UPDATE_ON_EXISTING_PK
     *                         UPDATE_ON_EXISTING_PK} is {@link Options#FALSE
     *                         FALSE}).  If set to {@link Options#TRUE TRUE},
     *                         any source table record being inserted that is
     *                         rejected for having primary key values that
     *                         match those of an existing target table record
     *                         will be ignored with no error generated.  If
     *                         {@link Options#FALSE FALSE}, the rejection of
     *                         any source table record for having primary key
     *                         values matching an existing target table record
     *                         will result in an error being raised.  If the
     *                         specified table does not have a primary key or
     *                         if upsert mode is in effect ({@link
     *                         Options#UPDATE_ON_EXISTING_PK
     *                         UPDATE_ON_EXISTING_PK} is {@link Options#TRUE
     *                         TRUE}), then this option has no effect.
     *                         Supported values:
     *                         <ul>
     *                             <li>{@link Options#TRUE TRUE}: Ignore source
     *                                 table records whose primary key values
     *                                 collide with those of target table
     *                                 records
     *                             <li>{@link Options#FALSE FALSE}: Raise an
     *                                 error for any source table record whose
     *                                 primary key values collide with those of
     *                                 a target table record
     *                         </ul>
     *                         The default value is {@link Options#FALSE
     *                         FALSE}.
     *                     <li>{@link Options#TRUNCATE_STRINGS
     *                         TRUNCATE_STRINGS}: If set to {@link Options#TRUE
     *                         TRUE}, it allows inserting longer strings into
     *                         smaller charN string columns by truncating the
     *                         longer strings to fit.
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
    public AppendRecordsRequest(String tableName, String sourceTableName, Map<String, String> fieldMap, Map<String, String> options) {
        this.tableName = (tableName == null) ? "" : tableName;
        this.sourceTableName = (sourceTableName == null) ? "" : sourceTableName;
        this.fieldMap = (fieldMap == null) ? new LinkedHashMap<String, String>() : fieldMap;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * The table name for the records to be appended, in
     * [schema_name.]table_name format, using standard <a
     * href="../../../../../../concepts/tables/#table-name-resolution"
     * target="_top">name resolution rules</a>.  Must be an existing table.
     *
     * @return The current value of {@code tableName}.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * The table name for the records to be appended, in
     * [schema_name.]table_name format, using standard <a
     * href="../../../../../../concepts/tables/#table-name-resolution"
     * target="_top">name resolution rules</a>.  Must be an existing table.
     *
     * @param tableName  The new value for {@code tableName}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AppendRecordsRequest setTableName(String tableName) {
        this.tableName = (tableName == null) ? "" : tableName;
        return this;
    }

    /**
     * The source table name to get records from, in [schema_name.]table_name
     * format, using standard <a
     * href="../../../../../../concepts/tables/#table-name-resolution"
     * target="_top">name resolution rules</a>.  Must be an existing table
     * name.
     *
     * @return The current value of {@code sourceTableName}.
     */
    public String getSourceTableName() {
        return sourceTableName;
    }

    /**
     * The source table name to get records from, in [schema_name.]table_name
     * format, using standard <a
     * href="../../../../../../concepts/tables/#table-name-resolution"
     * target="_top">name resolution rules</a>.  Must be an existing table
     * name.
     *
     * @param sourceTableName  The new value for {@code sourceTableName}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AppendRecordsRequest setSourceTableName(String sourceTableName) {
        this.sourceTableName = (sourceTableName == null) ? "" : sourceTableName;
        return this;
    }

    /**
     * Contains the mapping of column names from the target table (specified by
     * {@link #getTableName() tableName}) as the keys, and corresponding column
     * names or expressions (e.g., 'col_name+1') from the source table
     * (specified by {@link #getSourceTableName() sourceTableName}). Must be
     * existing column names in source table and target table, and their types
     * must be matched. For details on using expressions, see <a
     * href="../../../../../../concepts/expressions/"
     * target="_top">Expressions</a>.
     *
     * @return The current value of {@code fieldMap}.
     */
    public Map<String, String> getFieldMap() {
        return fieldMap;
    }

    /**
     * Contains the mapping of column names from the target table (specified by
     * {@link #getTableName() tableName}) as the keys, and corresponding column
     * names or expressions (e.g., 'col_name+1') from the source table
     * (specified by {@link #getSourceTableName() sourceTableName}). Must be
     * existing column names in source table and target table, and their types
     * must be matched. For details on using expressions, see <a
     * href="../../../../../../concepts/expressions/"
     * target="_top">Expressions</a>.
     *
     * @param fieldMap  The new value for {@code fieldMap}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AppendRecordsRequest setFieldMap(Map<String, String> fieldMap) {
        this.fieldMap = (fieldMap == null) ? new LinkedHashMap<String, String>() : fieldMap;
        return this;
    }

    /**
     * Optional parameters.
     * <ul>
     *     <li>{@link Options#OFFSET OFFSET}: A positive integer indicating the
     *         number of initial results to skip from {@link
     *         #getSourceTableName() sourceTableName}. Default is 0. The
     *         minimum allowed value is 0. The maximum allowed value is
     *         MAX_INT. The default value is '0'.
     *     <li>{@link Options#LIMIT LIMIT}: A positive integer indicating the
     *         maximum number of results to be returned from {@link
     *         #getSourceTableName() sourceTableName}. Or END_OF_SET (-9999) to
     *         indicate that the max number of results should be returned. The
     *         default value is '-9999'.
     *     <li>{@link Options#EXPRESSION EXPRESSION}: Optional filter
     *         expression to apply to the {@link #getSourceTableName()
     *         sourceTableName}. The default value is ''.
     *     <li>{@link Options#ORDER_BY ORDER_BY}: Comma-separated list of the
     *         columns to be sorted by from source table (specified by {@link
     *         #getSourceTableName() sourceTableName}), e.g., 'timestamp asc, x
     *         desc'. The {@link Options#ORDER_BY ORDER_BY} columns do not have
     *         to be present in {@link #getFieldMap() fieldMap}. The default
     *         value is ''.
     *     <li>{@link Options#UPDATE_ON_EXISTING_PK UPDATE_ON_EXISTING_PK}:
     *         Specifies the record collision policy for inserting source table
     *         records (specified by {@link #getSourceTableName()
     *         sourceTableName}) into a target table (specified by {@link
     *         #getTableName() tableName}) with a <a
     *         href="../../../../../../concepts/tables/#primary-keys"
     *         target="_top">primary key</a>. If set to {@link Options#TRUE
     *         TRUE}, any existing table record with primary key values that
     *         match those of a source table record being inserted will be
     *         replaced by that new record (the new data will be "upserted").
     *         If set to {@link Options#FALSE FALSE}, any existing table record
     *         with primary key values that match those of a source table
     *         record being inserted will remain unchanged, while the source
     *         record will be rejected and an error handled as determined by
     *         {@link Options#IGNORE_EXISTING_PK IGNORE_EXISTING_PK}.  If the
     *         specified table does not have a primary key, then this option
     *         has no effect.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}: Upsert new records when
     *                 primary keys match existing records
     *             <li>{@link Options#FALSE FALSE}: Reject new records when
     *                 primary keys match existing records
     *         </ul>
     *         The default value is {@link Options#FALSE FALSE}.
     *     <li>{@link Options#IGNORE_EXISTING_PK IGNORE_EXISTING_PK}: Specifies
     *         the record collision error-suppression policy for inserting
     *         source table records (specified by {@link #getSourceTableName()
     *         sourceTableName}) into a target table (specified by {@link
     *         #getTableName() tableName}) with a <a
     *         href="../../../../../../concepts/tables/#primary-keys"
     *         target="_top">primary key</a>, only used when not in upsert mode
     *         (upsert mode is disabled when {@link
     *         Options#UPDATE_ON_EXISTING_PK UPDATE_ON_EXISTING_PK} is {@link
     *         Options#FALSE FALSE}).  If set to {@link Options#TRUE TRUE}, any
     *         source table record being inserted that is rejected for having
     *         primary key values that match those of an existing target table
     *         record will be ignored with no error generated.  If {@link
     *         Options#FALSE FALSE}, the rejection of any source table record
     *         for having primary key values matching an existing target table
     *         record will result in an error being raised.  If the specified
     *         table does not have a primary key or if upsert mode is in effect
     *         ({@link Options#UPDATE_ON_EXISTING_PK UPDATE_ON_EXISTING_PK} is
     *         {@link Options#TRUE TRUE}), then this option has no effect.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}: Ignore source table records
     *                 whose primary key values collide with those of target
     *                 table records
     *             <li>{@link Options#FALSE FALSE}: Raise an error for any
     *                 source table record whose primary key values collide
     *                 with those of a target table record
     *         </ul>
     *         The default value is {@link Options#FALSE FALSE}.
     *     <li>{@link Options#TRUNCATE_STRINGS TRUNCATE_STRINGS}: If set to
     *         {@link Options#TRUE TRUE}, it allows inserting longer strings
     *         into smaller charN string columns by truncating the longer
     *         strings to fit.
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
     *     <li>{@link Options#OFFSET OFFSET}: A positive integer indicating the
     *         number of initial results to skip from {@link
     *         #getSourceTableName() sourceTableName}. Default is 0. The
     *         minimum allowed value is 0. The maximum allowed value is
     *         MAX_INT. The default value is '0'.
     *     <li>{@link Options#LIMIT LIMIT}: A positive integer indicating the
     *         maximum number of results to be returned from {@link
     *         #getSourceTableName() sourceTableName}. Or END_OF_SET (-9999) to
     *         indicate that the max number of results should be returned. The
     *         default value is '-9999'.
     *     <li>{@link Options#EXPRESSION EXPRESSION}: Optional filter
     *         expression to apply to the {@link #getSourceTableName()
     *         sourceTableName}. The default value is ''.
     *     <li>{@link Options#ORDER_BY ORDER_BY}: Comma-separated list of the
     *         columns to be sorted by from source table (specified by {@link
     *         #getSourceTableName() sourceTableName}), e.g., 'timestamp asc, x
     *         desc'. The {@link Options#ORDER_BY ORDER_BY} columns do not have
     *         to be present in {@link #getFieldMap() fieldMap}. The default
     *         value is ''.
     *     <li>{@link Options#UPDATE_ON_EXISTING_PK UPDATE_ON_EXISTING_PK}:
     *         Specifies the record collision policy for inserting source table
     *         records (specified by {@link #getSourceTableName()
     *         sourceTableName}) into a target table (specified by {@link
     *         #getTableName() tableName}) with a <a
     *         href="../../../../../../concepts/tables/#primary-keys"
     *         target="_top">primary key</a>. If set to {@link Options#TRUE
     *         TRUE}, any existing table record with primary key values that
     *         match those of a source table record being inserted will be
     *         replaced by that new record (the new data will be "upserted").
     *         If set to {@link Options#FALSE FALSE}, any existing table record
     *         with primary key values that match those of a source table
     *         record being inserted will remain unchanged, while the source
     *         record will be rejected and an error handled as determined by
     *         {@link Options#IGNORE_EXISTING_PK IGNORE_EXISTING_PK}.  If the
     *         specified table does not have a primary key, then this option
     *         has no effect.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}: Upsert new records when
     *                 primary keys match existing records
     *             <li>{@link Options#FALSE FALSE}: Reject new records when
     *                 primary keys match existing records
     *         </ul>
     *         The default value is {@link Options#FALSE FALSE}.
     *     <li>{@link Options#IGNORE_EXISTING_PK IGNORE_EXISTING_PK}: Specifies
     *         the record collision error-suppression policy for inserting
     *         source table records (specified by {@link #getSourceTableName()
     *         sourceTableName}) into a target table (specified by {@link
     *         #getTableName() tableName}) with a <a
     *         href="../../../../../../concepts/tables/#primary-keys"
     *         target="_top">primary key</a>, only used when not in upsert mode
     *         (upsert mode is disabled when {@link
     *         Options#UPDATE_ON_EXISTING_PK UPDATE_ON_EXISTING_PK} is {@link
     *         Options#FALSE FALSE}).  If set to {@link Options#TRUE TRUE}, any
     *         source table record being inserted that is rejected for having
     *         primary key values that match those of an existing target table
     *         record will be ignored with no error generated.  If {@link
     *         Options#FALSE FALSE}, the rejection of any source table record
     *         for having primary key values matching an existing target table
     *         record will result in an error being raised.  If the specified
     *         table does not have a primary key or if upsert mode is in effect
     *         ({@link Options#UPDATE_ON_EXISTING_PK UPDATE_ON_EXISTING_PK} is
     *         {@link Options#TRUE TRUE}), then this option has no effect.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}: Ignore source table records
     *                 whose primary key values collide with those of target
     *                 table records
     *             <li>{@link Options#FALSE FALSE}: Raise an error for any
     *                 source table record whose primary key values collide
     *                 with those of a target table record
     *         </ul>
     *         The default value is {@link Options#FALSE FALSE}.
     *     <li>{@link Options#TRUNCATE_STRINGS TRUNCATE_STRINGS}: If set to
     *         {@link Options#TRUE TRUE}, it allows inserting longer strings
     *         into smaller charN string columns by truncating the longer
     *         strings to fit.
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
    public AppendRecordsRequest setOptions(Map<String, String> options) {
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
                return this.tableName;

            case 1:
                return this.sourceTableName;

            case 2:
                return this.fieldMap;

            case 3:
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
                this.tableName = (String)value;
                break;

            case 1:
                this.sourceTableName = (String)value;
                break;

            case 2:
                this.fieldMap = (Map<String, String>)value;
                break;

            case 3:
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

        AppendRecordsRequest that = (AppendRecordsRequest)obj;

        return ( this.tableName.equals( that.tableName )
                 && this.sourceTableName.equals( that.sourceTableName )
                 && this.fieldMap.equals( that.fieldMap )
                 && this.options.equals( that.options ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "tableName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.tableName ) );
        builder.append( ", " );
        builder.append( gd.toString( "sourceTableName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.sourceTableName ) );
        builder.append( ", " );
        builder.append( gd.toString( "fieldMap" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.fieldMap ) );
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
        hashCode = (31 * hashCode) + this.tableName.hashCode();
        hashCode = (31 * hashCode) + this.sourceTableName.hashCode();
        hashCode = (31 * hashCode) + this.fieldMap.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }
}
