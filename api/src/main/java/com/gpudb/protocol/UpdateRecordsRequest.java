/*
 *  This file was autogenerated by the GPUdb schema processor.
 *
 *  DO NOT EDIT DIRECTLY.
 */
package com.gpudb.protocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.avro.generic.GenericData;


/**
 * A set of parameters for {@link
 * com.gpudb.GPUdb#updateRecords(UpdateRecordsRequest)}.
 * <p>
 * Runs multiple predicate-based updates in a single call.  With the list of
 * given expressions, any matching record's column values will be updated as
 * provided in {@code newValuesMaps}.  There is also an optional 'upsert'
 * capability where if a particular predicate doesn't match any existing
 * record, then a new record can be inserted.
 * <p>
 * Note that this operation can only be run on an original table and not on a
 * collection or a result view.
 * <p>
 * This operation can update primary key values.  By default only 'pure primary
 * key' predicates are allowed when updating primary key values. If the primary
 * key for a table is the column 'attr1', then the operation will only accept
 * predicates of the form: "attr1 == 'foo'" if the attr1 column is being
 * updated.  For a composite primary key (e.g. columns 'attr1' and 'attr2')
 * then this operation will only accept predicates of the form: "(attr1 ==
 * 'foo') and (attr2 == 'bar')".  Meaning, all primary key columns must appear
 * in an equality predicate in the expressions.  Furthermore each 'pure primary
 * key' predicate must be unique within a given request.  These restrictions
 * can be removed by utilizing some available options through {@code options}.
 * 
 * @param <T>  The type of object being processed.
 * 
 */
public class UpdateRecordsRequest<T> {

    /**
     * Optional parameters.
     * <ul>
     *         <li> {@link
     * com.gpudb.protocol.RawUpdateRecordsRequest.Options#GLOBAL_EXPRESSION
     * GLOBAL_EXPRESSION}: An optional global expression to reduce the search
     * space of the predicates listed in {@code expressions}.  The default
     * value is ''.
     *         <li> {@link
     * com.gpudb.protocol.RawUpdateRecordsRequest.Options#BYPASS_SAFETY_CHECKS
     * BYPASS_SAFETY_CHECKS}: When set to {@code true}, all predicates are
     * available for primary key updates.  Keep in mind that it is possible to
     * destroy data in this case, since a single predicate may match multiple
     * objects (potentially all of records of a table), and then updating all
     * of those records to have the same primary key will, due to the primary
     * key uniqueness constraints, effectively delete all but one of those
     * updated records.
     * Supported values:
     * <ul>
     *         <li> {@link
     * com.gpudb.protocol.RawUpdateRecordsRequest.Options#TRUE TRUE}
     *         <li> {@link
     * com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE FALSE}
     * </ul>
     * The default value is {@link
     * com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE FALSE}.
     *         <li> {@link
     * com.gpudb.protocol.RawUpdateRecordsRequest.Options#UPDATE_ON_EXISTING_PK
     * UPDATE_ON_EXISTING_PK}: Can be used to customize behavior when the
     * updated primary key value already exists as described in {@link
     * com.gpudb.GPUdb#insertRecords(InsertRecordsRequest)}.
     * Supported values:
     * <ul>
     *         <li> {@link
     * com.gpudb.protocol.RawUpdateRecordsRequest.Options#TRUE TRUE}
     *         <li> {@link
     * com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE FALSE}
     * </ul>
     * The default value is {@link
     * com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE FALSE}.
     *         <li> {@link
     * com.gpudb.protocol.RawUpdateRecordsRequest.Options#UPDATE_PARTITION
     * UPDATE_PARTITION}: Force qualifying records to be deleted and reinserted
     * so their partition membership will be reevaluated.
     * Supported values:
     * <ul>
     *         <li> {@link
     * com.gpudb.protocol.RawUpdateRecordsRequest.Options#TRUE TRUE}
     *         <li> {@link
     * com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE FALSE}
     * </ul>
     * The default value is {@link
     * com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE FALSE}.
     *         <li> {@link
     * com.gpudb.protocol.RawUpdateRecordsRequest.Options#TRUNCATE_STRINGS
     * TRUNCATE_STRINGS}: If set to {true}@{, any strings which are too long
     * for their charN string fields will be truncated to fit.  The default
     * value is false.
     * Supported values:
     * <ul>
     *         <li> {@link
     * com.gpudb.protocol.RawUpdateRecordsRequest.Options#TRUE TRUE}
     *         <li> {@link
     * com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE FALSE}
     * </ul>
     * The default value is {@link
     * com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE FALSE}.
     *         <li> {@link
     * com.gpudb.protocol.RawUpdateRecordsRequest.Options#USE_EXPRESSIONS_IN_NEW_VALUES_MAPS
     * USE_EXPRESSIONS_IN_NEW_VALUES_MAPS}: When set to {@code true}, all new
     * values in {@code newValuesMaps} are considered as expression values.
     * When set to {@code false}, all new values in {@code newValuesMaps} are
     * considered as constants.  NOTE:  When {@code true}, string constants
     * will need to be quoted to avoid being evaluated as expressions.
     * Supported values:
     * <ul>
     *         <li> {@link
     * com.gpudb.protocol.RawUpdateRecordsRequest.Options#TRUE TRUE}
     *         <li> {@link
     * com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE FALSE}
     * </ul>
     * The default value is {@link
     * com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE FALSE}.
     *         <li> {@link
     * com.gpudb.protocol.RawUpdateRecordsRequest.Options#RECORD_ID RECORD_ID}:
     * ID of a single record to be updated (returned in the call to {@link
     * com.gpudb.GPUdb#insertRecords(InsertRecordsRequest)} or {@link
     * com.gpudb.GPUdb#getRecordsFromCollection(Object,
     * GetRecordsFromCollectionRequest)}).
     * </ul>
     * The default value is an empty {@link Map}.
     * A set of string constants for the parameter {@code options}.
     */
    public static final class Options {

        /**
         * An optional global expression to reduce the search space of the
         * predicates listed in {@code expressions}.  The default value is ''.
         */
        public static final String GLOBAL_EXPRESSION = "global_expression";

        /**
         * When set to {@code true}, all predicates are available for primary
         * key updates.  Keep in mind that it is possible to destroy data in
         * this case, since a single predicate may match multiple objects
         * (potentially all of records of a table), and then updating all of
         * those records to have the same primary key will, due to the primary
         * key uniqueness constraints, effectively delete all but one of those
         * updated records.
         * Supported values:
         * <ul>
         *         <li> {@link
         * com.gpudb.protocol.RawUpdateRecordsRequest.Options#TRUE TRUE}
         *         <li> {@link
         * com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE FALSE}
         * </ul>
         * The default value is {@link
         * com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE FALSE}.
         */
        public static final String BYPASS_SAFETY_CHECKS = "bypass_safety_checks";
        public static final String TRUE = "true";
        public static final String FALSE = "false";

        /**
         * Can be used to customize behavior when the updated primary key value
         * already exists as described in {@link
         * com.gpudb.GPUdb#insertRecords(InsertRecordsRequest)}.
         * Supported values:
         * <ul>
         *         <li> {@link
         * com.gpudb.protocol.RawUpdateRecordsRequest.Options#TRUE TRUE}
         *         <li> {@link
         * com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE FALSE}
         * </ul>
         * The default value is {@link
         * com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE FALSE}.
         */
        public static final String UPDATE_ON_EXISTING_PK = "update_on_existing_pk";

        /**
         * Force qualifying records to be deleted and reinserted so their
         * partition membership will be reevaluated.
         * Supported values:
         * <ul>
         *         <li> {@link
         * com.gpudb.protocol.RawUpdateRecordsRequest.Options#TRUE TRUE}
         *         <li> {@link
         * com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE FALSE}
         * </ul>
         * The default value is {@link
         * com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE FALSE}.
         */
        public static final String UPDATE_PARTITION = "update_partition";

        /**
         * If set to {true}@{, any strings which are too long for their charN
         * string fields will be truncated to fit.  The default value is false.
         * Supported values:
         * <ul>
         *         <li> {@link
         * com.gpudb.protocol.RawUpdateRecordsRequest.Options#TRUE TRUE}
         *         <li> {@link
         * com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE FALSE}
         * </ul>
         * The default value is {@link
         * com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE FALSE}.
         */
        public static final String TRUNCATE_STRINGS = "truncate_strings";

        /**
         * When set to {@code true}, all new values in {@code newValuesMaps}
         * are considered as expression values. When set to {@code false}, all
         * new values in {@code newValuesMaps} are considered as constants.
         * NOTE:  When {@code true}, string constants will need to be quoted to
         * avoid being evaluated as expressions.
         * Supported values:
         * <ul>
         *         <li> {@link
         * com.gpudb.protocol.RawUpdateRecordsRequest.Options#TRUE TRUE}
         *         <li> {@link
         * com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE FALSE}
         * </ul>
         * The default value is {@link
         * com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE FALSE}.
         */
        public static final String USE_EXPRESSIONS_IN_NEW_VALUES_MAPS = "use_expressions_in_new_values_maps";

        /**
         * ID of a single record to be updated (returned in the call to {@link
         * com.gpudb.GPUdb#insertRecords(InsertRecordsRequest)} or {@link
         * com.gpudb.GPUdb#getRecordsFromCollection(Object,
         * GetRecordsFromCollectionRequest)}).
         */
        public static final String RECORD_ID = "record_id";

        private Options() {  }
    }

    private String tableName;
    private List<String> expressions;
    private List<Map<String, String>> newValuesMaps;
    private List<T> data;
    private Map<String, String> options;


    /**
     * Constructs an UpdateRecordsRequest object with default parameters.
     */
    public UpdateRecordsRequest() {
        tableName = "";
        expressions = new ArrayList<>();
        newValuesMaps = new ArrayList<>();
        data = new ArrayList<>();
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs an UpdateRecordsRequest object with the specified parameters.
     * 
     * @param tableName  Table to be updated. Must be a currently existing
     *                   table and not a collection or view.
     * @param expressions  A list of the actual predicates, one for each
     *                     update; format should follow the guidelines {@link
     *                     com.gpudb.GPUdb#filter(FilterRequest) here}.
     * @param newValuesMaps  List of new values for the matching records.  Each
     *                       element is a map with (key, value) pairs where the
     *                       keys are the names of the columns whose values are
     *                       to be updated; the values are the new values.  The
     *                       number of elements in the list should match the
     *                       length of {@code expressions}.
     * @param data  An *optional* list of new binary-avro encoded records to
     *              insert, one for each update.  If one of {@code expressions}
     *              does not yield a matching record to be updated, then the
     *              corresponding element from this list will be added to the
     *              table.  The default value is an empty {@link List}.
     * @param options  Optional parameters.
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#GLOBAL_EXPRESSION
     *                 GLOBAL_EXPRESSION}: An optional global expression to
     *                 reduce the search space of the predicates listed in
     *                 {@code expressions}.  The default value is ''.
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#BYPASS_SAFETY_CHECKS
     *                 BYPASS_SAFETY_CHECKS}: When set to {@code true}, all
     *                 predicates are available for primary key updates.  Keep
     *                 in mind that it is possible to destroy data in this
     *                 case, since a single predicate may match multiple
     *                 objects (potentially all of records of a table), and
     *                 then updating all of those records to have the same
     *                 primary key will, due to the primary key uniqueness
     *                 constraints, effectively delete all but one of those
     *                 updated records.
     *                 Supported values:
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#TRUE
     *                 TRUE}
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE
     *                 FALSE}
     *                 </ul>
     *                 The default value is {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE
     *                 FALSE}.
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#UPDATE_ON_EXISTING_PK
     *                 UPDATE_ON_EXISTING_PK}: Can be used to customize
     *                 behavior when the updated primary key value already
     *                 exists as described in {@link
     *                 com.gpudb.GPUdb#insertRecords(InsertRecordsRequest)}.
     *                 Supported values:
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#TRUE
     *                 TRUE}
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE
     *                 FALSE}
     *                 </ul>
     *                 The default value is {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE
     *                 FALSE}.
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#UPDATE_PARTITION
     *                 UPDATE_PARTITION}: Force qualifying records to be
     *                 deleted and reinserted so their partition membership
     *                 will be reevaluated.
     *                 Supported values:
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#TRUE
     *                 TRUE}
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE
     *                 FALSE}
     *                 </ul>
     *                 The default value is {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE
     *                 FALSE}.
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#TRUNCATE_STRINGS
     *                 TRUNCATE_STRINGS}: If set to {true}@{, any strings which
     *                 are too long for their charN string fields will be
     *                 truncated to fit.  The default value is false.
     *                 Supported values:
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#TRUE
     *                 TRUE}
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE
     *                 FALSE}
     *                 </ul>
     *                 The default value is {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE
     *                 FALSE}.
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#USE_EXPRESSIONS_IN_NEW_VALUES_MAPS
     *                 USE_EXPRESSIONS_IN_NEW_VALUES_MAPS}: When set to {@code
     *                 true}, all new values in {@code newValuesMaps} are
     *                 considered as expression values. When set to {@code
     *                 false}, all new values in {@code newValuesMaps} are
     *                 considered as constants.  NOTE:  When {@code true},
     *                 string constants will need to be quoted to avoid being
     *                 evaluated as expressions.
     *                 Supported values:
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#TRUE
     *                 TRUE}
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE
     *                 FALSE}
     *                 </ul>
     *                 The default value is {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE
     *                 FALSE}.
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#RECORD_ID
     *                 RECORD_ID}: ID of a single record to be updated
     *                 (returned in the call to {@link
     *                 com.gpudb.GPUdb#insertRecords(InsertRecordsRequest)} or
     *                 {@link com.gpudb.GPUdb#getRecordsFromCollection(Object,
     *                 GetRecordsFromCollectionRequest)}).
     *                 </ul>
     *                 The default value is an empty {@link Map}.
     * 
     */
    public UpdateRecordsRequest(String tableName, List<String> expressions, List<Map<String, String>> newValuesMaps, List<T> data, Map<String, String> options) {
        this.tableName = (tableName == null) ? "" : tableName;
        this.expressions = (expressions == null) ? new ArrayList<String>() : expressions;
        this.newValuesMaps = (newValuesMaps == null) ? new ArrayList<Map<String, String>>() : newValuesMaps;
        this.data = (data == null) ? new ArrayList<T>() : data;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * 
     * @return Table to be updated. Must be a currently existing table and not
     *         a collection or view.
     * 
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * 
     * @param tableName  Table to be updated. Must be a currently existing
     *                   table and not a collection or view.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public UpdateRecordsRequest<T> setTableName(String tableName) {
        this.tableName = (tableName == null) ? "" : tableName;
        return this;
    }

    /**
     * 
     * @return A list of the actual predicates, one for each update; format
     *         should follow the guidelines {@link
     *         com.gpudb.GPUdb#filter(FilterRequest) here}.
     * 
     */
    public List<String> getExpressions() {
        return expressions;
    }

    /**
     * 
     * @param expressions  A list of the actual predicates, one for each
     *                     update; format should follow the guidelines {@link
     *                     com.gpudb.GPUdb#filter(FilterRequest) here}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public UpdateRecordsRequest<T> setExpressions(List<String> expressions) {
        this.expressions = (expressions == null) ? new ArrayList<String>() : expressions;
        return this;
    }

    /**
     * 
     * @return List of new values for the matching records.  Each element is a
     *         map with (key, value) pairs where the keys are the names of the
     *         columns whose values are to be updated; the values are the new
     *         values.  The number of elements in the list should match the
     *         length of {@code expressions}.
     * 
     */
    public List<Map<String, String>> getNewValuesMaps() {
        return newValuesMaps;
    }

    /**
     * 
     * @param newValuesMaps  List of new values for the matching records.  Each
     *                       element is a map with (key, value) pairs where the
     *                       keys are the names of the columns whose values are
     *                       to be updated; the values are the new values.  The
     *                       number of elements in the list should match the
     *                       length of {@code expressions}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public UpdateRecordsRequest<T> setNewValuesMaps(List<Map<String, String>> newValuesMaps) {
        this.newValuesMaps = (newValuesMaps == null) ? new ArrayList<Map<String, String>>() : newValuesMaps;
        return this;
    }

    /**
     * 
     * @return An *optional* list of new binary-avro encoded records to insert,
     *         one for each update.  If one of {@code expressions} does not
     *         yield a matching record to be updated, then the corresponding
     *         element from this list will be added to the table.  The default
     *         value is an empty {@link List}.
     * 
     */
    public List<T> getData() {
        return data;
    }

    /**
     * 
     * @param data  An *optional* list of new binary-avro encoded records to
     *              insert, one for each update.  If one of {@code expressions}
     *              does not yield a matching record to be updated, then the
     *              corresponding element from this list will be added to the
     *              table.  The default value is an empty {@link List}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public UpdateRecordsRequest<T> setData(List<T> data) {
        this.data = (data == null) ? new ArrayList<T>() : data;
        return this;
    }

    /**
     * 
     * @return Optional parameters.
     *         <ul>
     *                 <li> {@link
     *         com.gpudb.protocol.RawUpdateRecordsRequest.Options#GLOBAL_EXPRESSION
     *         GLOBAL_EXPRESSION}: An optional global expression to reduce the
     *         search space of the predicates listed in {@code expressions}.
     *         The default value is ''.
     *                 <li> {@link
     *         com.gpudb.protocol.RawUpdateRecordsRequest.Options#BYPASS_SAFETY_CHECKS
     *         BYPASS_SAFETY_CHECKS}: When set to {@code true}, all predicates
     *         are available for primary key updates.  Keep in mind that it is
     *         possible to destroy data in this case, since a single predicate
     *         may match multiple objects (potentially all of records of a
     *         table), and then updating all of those records to have the same
     *         primary key will, due to the primary key uniqueness constraints,
     *         effectively delete all but one of those updated records.
     *         Supported values:
     *         <ul>
     *                 <li> {@link
     *         com.gpudb.protocol.RawUpdateRecordsRequest.Options#TRUE TRUE}
     *                 <li> {@link
     *         com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link
     *         com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE FALSE}.
     *                 <li> {@link
     *         com.gpudb.protocol.RawUpdateRecordsRequest.Options#UPDATE_ON_EXISTING_PK
     *         UPDATE_ON_EXISTING_PK}: Can be used to customize behavior when
     *         the updated primary key value already exists as described in
     *         {@link com.gpudb.GPUdb#insertRecords(InsertRecordsRequest)}.
     *         Supported values:
     *         <ul>
     *                 <li> {@link
     *         com.gpudb.protocol.RawUpdateRecordsRequest.Options#TRUE TRUE}
     *                 <li> {@link
     *         com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link
     *         com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE FALSE}.
     *                 <li> {@link
     *         com.gpudb.protocol.RawUpdateRecordsRequest.Options#UPDATE_PARTITION
     *         UPDATE_PARTITION}: Force qualifying records to be deleted and
     *         reinserted so their partition membership will be reevaluated.
     *         Supported values:
     *         <ul>
     *                 <li> {@link
     *         com.gpudb.protocol.RawUpdateRecordsRequest.Options#TRUE TRUE}
     *                 <li> {@link
     *         com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link
     *         com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE FALSE}.
     *                 <li> {@link
     *         com.gpudb.protocol.RawUpdateRecordsRequest.Options#TRUNCATE_STRINGS
     *         TRUNCATE_STRINGS}: If set to {true}@{, any strings which are too
     *         long for their charN string fields will be truncated to fit.
     *         The default value is false.
     *         Supported values:
     *         <ul>
     *                 <li> {@link
     *         com.gpudb.protocol.RawUpdateRecordsRequest.Options#TRUE TRUE}
     *                 <li> {@link
     *         com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link
     *         com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE FALSE}.
     *                 <li> {@link
     *         com.gpudb.protocol.RawUpdateRecordsRequest.Options#USE_EXPRESSIONS_IN_NEW_VALUES_MAPS
     *         USE_EXPRESSIONS_IN_NEW_VALUES_MAPS}: When set to {@code true},
     *         all new values in {@code newValuesMaps} are considered as
     *         expression values. When set to {@code false}, all new values in
     *         {@code newValuesMaps} are considered as constants.  NOTE:  When
     *         {@code true}, string constants will need to be quoted to avoid
     *         being evaluated as expressions.
     *         Supported values:
     *         <ul>
     *                 <li> {@link
     *         com.gpudb.protocol.RawUpdateRecordsRequest.Options#TRUE TRUE}
     *                 <li> {@link
     *         com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link
     *         com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE FALSE}.
     *                 <li> {@link
     *         com.gpudb.protocol.RawUpdateRecordsRequest.Options#RECORD_ID
     *         RECORD_ID}: ID of a single record to be updated (returned in the
     *         call to {@link
     *         com.gpudb.GPUdb#insertRecords(InsertRecordsRequest)} or {@link
     *         com.gpudb.GPUdb#getRecordsFromCollection(Object,
     *         GetRecordsFromCollectionRequest)}).
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
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#GLOBAL_EXPRESSION
     *                 GLOBAL_EXPRESSION}: An optional global expression to
     *                 reduce the search space of the predicates listed in
     *                 {@code expressions}.  The default value is ''.
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#BYPASS_SAFETY_CHECKS
     *                 BYPASS_SAFETY_CHECKS}: When set to {@code true}, all
     *                 predicates are available for primary key updates.  Keep
     *                 in mind that it is possible to destroy data in this
     *                 case, since a single predicate may match multiple
     *                 objects (potentially all of records of a table), and
     *                 then updating all of those records to have the same
     *                 primary key will, due to the primary key uniqueness
     *                 constraints, effectively delete all but one of those
     *                 updated records.
     *                 Supported values:
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#TRUE
     *                 TRUE}
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE
     *                 FALSE}
     *                 </ul>
     *                 The default value is {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE
     *                 FALSE}.
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#UPDATE_ON_EXISTING_PK
     *                 UPDATE_ON_EXISTING_PK}: Can be used to customize
     *                 behavior when the updated primary key value already
     *                 exists as described in {@link
     *                 com.gpudb.GPUdb#insertRecords(InsertRecordsRequest)}.
     *                 Supported values:
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#TRUE
     *                 TRUE}
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE
     *                 FALSE}
     *                 </ul>
     *                 The default value is {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE
     *                 FALSE}.
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#UPDATE_PARTITION
     *                 UPDATE_PARTITION}: Force qualifying records to be
     *                 deleted and reinserted so their partition membership
     *                 will be reevaluated.
     *                 Supported values:
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#TRUE
     *                 TRUE}
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE
     *                 FALSE}
     *                 </ul>
     *                 The default value is {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE
     *                 FALSE}.
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#TRUNCATE_STRINGS
     *                 TRUNCATE_STRINGS}: If set to {true}@{, any strings which
     *                 are too long for their charN string fields will be
     *                 truncated to fit.  The default value is false.
     *                 Supported values:
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#TRUE
     *                 TRUE}
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE
     *                 FALSE}
     *                 </ul>
     *                 The default value is {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE
     *                 FALSE}.
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#USE_EXPRESSIONS_IN_NEW_VALUES_MAPS
     *                 USE_EXPRESSIONS_IN_NEW_VALUES_MAPS}: When set to {@code
     *                 true}, all new values in {@code newValuesMaps} are
     *                 considered as expression values. When set to {@code
     *                 false}, all new values in {@code newValuesMaps} are
     *                 considered as constants.  NOTE:  When {@code true},
     *                 string constants will need to be quoted to avoid being
     *                 evaluated as expressions.
     *                 Supported values:
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#TRUE
     *                 TRUE}
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE
     *                 FALSE}
     *                 </ul>
     *                 The default value is {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#FALSE
     *                 FALSE}.
     *                         <li> {@link
     *                 com.gpudb.protocol.RawUpdateRecordsRequest.Options#RECORD_ID
     *                 RECORD_ID}: ID of a single record to be updated
     *                 (returned in the call to {@link
     *                 com.gpudb.GPUdb#insertRecords(InsertRecordsRequest)} or
     *                 {@link com.gpudb.GPUdb#getRecordsFromCollection(Object,
     *                 GetRecordsFromCollectionRequest)}).
     *                 </ul>
     *                 The default value is an empty {@link Map}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public UpdateRecordsRequest<T> setOptions(Map<String, String> options) {
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
        return this;
    }
    @Override
    public boolean equals(Object obj) {
        if( obj == this ) {
            return true;
        }

        if( (obj == null) || (obj.getClass() != this.getClass()) ) {
            return false;
        }

        UpdateRecordsRequest that = (UpdateRecordsRequest)obj;

        return ( this.tableName.equals( that.tableName )
                 && this.expressions.equals( that.expressions )
                 && this.newValuesMaps.equals( that.newValuesMaps )
                 && this.data.equals( that.data )
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
        builder.append( gd.toString( "expressions" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.expressions ) );
        builder.append( ", " );
        builder.append( gd.toString( "newValuesMaps" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.newValuesMaps ) );
        builder.append( ", " );
        builder.append( gd.toString( "data" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.data ) );
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
        hashCode = (31 * hashCode) + this.expressions.hashCode();
        hashCode = (31 * hashCode) + this.newValuesMaps.hashCode();
        hashCode = (31 * hashCode) + this.data.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }

}
