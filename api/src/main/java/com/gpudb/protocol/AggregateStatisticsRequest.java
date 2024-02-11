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
 * com.gpudb.GPUdb#aggregateStatistics(AggregateStatisticsRequest)
 * GPUdb.aggregateStatistics}.
 * <p>
 * Calculates the requested statistics of the given column(s) in a given table.
 * <p>
 * The available statistics are: {@link Stats#COUNT COUNT} (number of total
 * objects), {@link Stats#MEAN MEAN}, {@link Stats#STDV STDV} (standard
 * deviation), {@link Stats#VARIANCE VARIANCE}, {@link Stats#SKEW SKEW}, {@link
 * Stats#KURTOSIS KURTOSIS}, {@link Stats#SUM SUM}, {@link Stats#MIN MIN},
 * {@link Stats#MAX MAX}, {@link Stats#WEIGHTED_AVERAGE WEIGHTED_AVERAGE},
 * {@link Stats#CARDINALITY CARDINALITY} (unique count), {@link
 * Stats#ESTIMATED_CARDINALITY ESTIMATED_CARDINALITY}, {@link Stats#PERCENTILE
 * PERCENTILE}, and {@link Stats#PERCENTILE_RANK PERCENTILE_RANK}.
 * <p>
 * Estimated cardinality is calculated by using the hyperloglog approximation
 * technique.
 * <p>
 * Percentiles and percentile ranks are approximate and are calculated using
 * the t-digest algorithm. They must include the desired {@link
 * Stats#PERCENTILE PERCENTILE}/{@link Stats#PERCENTILE_RANK PERCENTILE_RANK}.
 * To compute multiple percentiles each value must be specified separately
 * (i.e.
 * 'percentile(75.0),percentile(99.0),percentile_rank(1234.56),percentile_rank(-5)').
 * <p>
 * A second, comma-separated value can be added to the {@link Stats#PERCENTILE
 * PERCENTILE} statistic to calculate percentile resolution, e.g., a 50th
 * percentile with 200 resolution would be 'percentile(50,200)'.
 * <p>
 * The weighted average statistic requires a weight column to be specified in
 * {@link Options#WEIGHT_COLUMN_NAME WEIGHT_COLUMN_NAME}.  The weighted average
 * is then defined as the sum of the products of {@link #getColumnName()
 * columnName} times the {@link Options#WEIGHT_COLUMN_NAME WEIGHT_COLUMN_NAME}
 * values divided by the sum of the {@link Options#WEIGHT_COLUMN_NAME
 * WEIGHT_COLUMN_NAME} values.
 * <p>
 * Additional columns can be used in the calculation of statistics via {@link
 * Options#ADDITIONAL_COLUMN_NAMES ADDITIONAL_COLUMN_NAMES}.  Values in these
 * columns will be included in the overall aggregate calculation--individual
 * aggregates will not be calculated per additional column.  For instance,
 * requesting the {@link Stats#COUNT COUNT} & {@link Stats#MEAN MEAN} of {@link
 * #getColumnName() columnName} x and {@link Options#ADDITIONAL_COLUMN_NAMES
 * ADDITIONAL_COLUMN_NAMES} y & z, where x holds the numbers 1-10, y holds
 * 11-20, and z holds 21-30, would return the total number of x, y, & z values
 * (30), and the single average value across all x, y, & z values (15.5).
 * <p>
 * The response includes a list of key/value pairs of each statistic requested
 * and its corresponding value.
 */
public class AggregateStatisticsRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("AggregateStatisticsRequest")
            .namespace("com.gpudb")
            .fields()
                .name("tableName").type().stringType().noDefault()
                .name("columnName").type().stringType().noDefault()
                .name("stats").type().stringType().noDefault()
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
     * A set of string constants for the {@link AggregateStatisticsRequest}
     * parameter {@link #getStats() stats}.
     * <p>
     * Comma separated list of the statistics to calculate, e.g. "sum,mean".
     */
    public static final class Stats {
        /**
         * Number of objects (independent of the given column(s)).
         */
        public static final String COUNT = "count";

        /**
         * Arithmetic mean (average), equivalent to sum/count.
         */
        public static final String MEAN = "mean";

        /**
         * Sample standard deviation (denominator is count-1).
         */
        public static final String STDV = "stdv";

        /**
         * Unbiased sample variance (denominator is count-1).
         */
        public static final String VARIANCE = "variance";

        /**
         * Skewness (third standardized moment).
         */
        public static final String SKEW = "skew";

        /**
         * Kurtosis (fourth standardized moment).
         */
        public static final String KURTOSIS = "kurtosis";

        /**
         * Sum of all values in the column(s).
         */
        public static final String SUM = "sum";

        /**
         * Minimum value of the column(s).
         */
        public static final String MIN = "min";

        /**
         * Maximum value of the column(s).
         */
        public static final String MAX = "max";

        /**
         * Weighted arithmetic mean (using the option {@link
         * Options#WEIGHT_COLUMN_NAME WEIGHT_COLUMN_NAME} as the weighting
         * column).
         */
        public static final String WEIGHTED_AVERAGE = "weighted_average";

        /**
         * Number of unique values in the column(s).
         */
        public static final String CARDINALITY = "cardinality";

        /**
         * Estimate (via hyperloglog technique) of the number of unique values
         * in the column(s).
         */
        public static final String ESTIMATED_CARDINALITY = "estimated_cardinality";

        /**
         * Estimate (via t-digest) of the given percentile of the column(s)
         * (percentile(50.0) will be an approximation of the median). Add a
         * second, comma-separated value to calculate percentile resolution,
         * e.g., 'percentile(75,150)'
         */
        public static final String PERCENTILE = "percentile";

        /**
         * Estimate (via t-digest) of the percentile rank of the given value in
         * the column(s) (if the given value is the median of the column(s),
         * percentile_rank(&lt;median&gt;) will return approximately 50.0).
         */
        public static final String PERCENTILE_RANK = "percentile_rank";

        private Stats() {  }
    }

    /**
     * A set of string constants for the {@link AggregateStatisticsRequest}
     * parameter {@link #getOptions() options}.
     * <p>
     * Optional parameters.
     */
    public static final class Options {
        /**
         * A list of comma separated column names over which statistics can be
         * accumulated along with the primary column.  All columns listed and
         * {@link #getColumnName() columnName} must be of the same type.  Must
         * not include the column specified in {@link #getColumnName()
         * columnName} and no column can be listed twice.
         */
        public static final String ADDITIONAL_COLUMN_NAMES = "additional_column_names";

        /**
         * Name of column used as weighting attribute for the weighted average
         * statistic.
         */
        public static final String WEIGHT_COLUMN_NAME = "weight_column_name";

        private Options() {  }
    }

    private String tableName;
    private String columnName;
    private String stats;
    private Map<String, String> options;

    /**
     * Constructs an AggregateStatisticsRequest object with default parameters.
     */
    public AggregateStatisticsRequest() {
        tableName = "";
        columnName = "";
        stats = "";
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs an AggregateStatisticsRequest object with the specified
     * parameters.
     *
     * @param tableName  Name of the table on which the statistics operation
     *                   will be performed, in [schema_name.]table_name format,
     *                   using standard <a
     *                   href="../../../../../../concepts/tables/#table-name-resolution"
     *                   target="_top">name resolution rules</a>.
     * @param columnName  Name of the primary column for which the statistics
     *                    are to be calculated.
     * @param stats  Comma separated list of the statistics to calculate, e.g.
     *               "sum,mean".
     *               Supported values:
     *               <ul>
     *                   <li>{@link Stats#COUNT COUNT}: Number of objects
     *                       (independent of the given column(s)).
     *                   <li>{@link Stats#MEAN MEAN}: Arithmetic mean
     *                       (average), equivalent to sum/count.
     *                   <li>{@link Stats#STDV STDV}: Sample standard deviation
     *                       (denominator is count-1).
     *                   <li>{@link Stats#VARIANCE VARIANCE}: Unbiased sample
     *                       variance (denominator is count-1).
     *                   <li>{@link Stats#SKEW SKEW}: Skewness (third
     *                       standardized moment).
     *                   <li>{@link Stats#KURTOSIS KURTOSIS}: Kurtosis (fourth
     *                       standardized moment).
     *                   <li>{@link Stats#SUM SUM}: Sum of all values in the
     *                       column(s).
     *                   <li>{@link Stats#MIN MIN}: Minimum value of the
     *                       column(s).
     *                   <li>{@link Stats#MAX MAX}: Maximum value of the
     *                       column(s).
     *                   <li>{@link Stats#WEIGHTED_AVERAGE WEIGHTED_AVERAGE}:
     *                       Weighted arithmetic mean (using the option {@link
     *                       Options#WEIGHT_COLUMN_NAME WEIGHT_COLUMN_NAME} as
     *                       the weighting column).
     *                   <li>{@link Stats#CARDINALITY CARDINALITY}: Number of
     *                       unique values in the column(s).
     *                   <li>{@link Stats#ESTIMATED_CARDINALITY
     *                       ESTIMATED_CARDINALITY}: Estimate (via hyperloglog
     *                       technique) of the number of unique values in the
     *                       column(s).
     *                   <li>{@link Stats#PERCENTILE PERCENTILE}: Estimate (via
     *                       t-digest) of the given percentile of the column(s)
     *                       (percentile(50.0) will be an approximation of the
     *                       median). Add a second, comma-separated value to
     *                       calculate percentile resolution, e.g.,
     *                       'percentile(75,150)'
     *                   <li>{@link Stats#PERCENTILE_RANK PERCENTILE_RANK}:
     *                       Estimate (via t-digest) of the percentile rank of
     *                       the given value in the column(s) (if the given
     *                       value is the median of the column(s),
     *                       percentile_rank(&lt;median&gt;) will return
     *                       approximately 50.0).
     *               </ul>
     * @param options  Optional parameters.
     *                 <ul>
     *                     <li>{@link Options#ADDITIONAL_COLUMN_NAMES
     *                         ADDITIONAL_COLUMN_NAMES}: A list of comma
     *                         separated column names over which statistics can
     *                         be accumulated along with the primary column.
     *                         All columns listed and {@code columnName} must
     *                         be of the same type.  Must not include the
     *                         column specified in {@code columnName} and no
     *                         column can be listed twice.
     *                     <li>{@link Options#WEIGHT_COLUMN_NAME
     *                         WEIGHT_COLUMN_NAME}: Name of column used as
     *                         weighting attribute for the weighted average
     *                         statistic.
     *                 </ul>
     *                 The default value is an empty {@link Map}.
     */
    public AggregateStatisticsRequest(String tableName, String columnName, String stats, Map<String, String> options) {
        this.tableName = (tableName == null) ? "" : tableName;
        this.columnName = (columnName == null) ? "" : columnName;
        this.stats = (stats == null) ? "" : stats;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * Name of the table on which the statistics operation will be performed,
     * in [schema_name.]table_name format, using standard <a
     * href="../../../../../../concepts/tables/#table-name-resolution"
     * target="_top">name resolution rules</a>.
     *
     * @return The current value of {@code tableName}.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Name of the table on which the statistics operation will be performed,
     * in [schema_name.]table_name format, using standard <a
     * href="../../../../../../concepts/tables/#table-name-resolution"
     * target="_top">name resolution rules</a>.
     *
     * @param tableName  The new value for {@code tableName}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AggregateStatisticsRequest setTableName(String tableName) {
        this.tableName = (tableName == null) ? "" : tableName;
        return this;
    }

    /**
     * Name of the primary column for which the statistics are to be
     * calculated.
     *
     * @return The current value of {@code columnName}.
     */
    public String getColumnName() {
        return columnName;
    }

    /**
     * Name of the primary column for which the statistics are to be
     * calculated.
     *
     * @param columnName  The new value for {@code columnName}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AggregateStatisticsRequest setColumnName(String columnName) {
        this.columnName = (columnName == null) ? "" : columnName;
        return this;
    }

    /**
     * Comma separated list of the statistics to calculate,
     * e.g.&nbsp;"sum,mean".
     * Supported values:
     * <ul>
     *     <li>{@link Stats#COUNT COUNT}: Number of objects (independent of the
     *         given column(s)).
     *     <li>{@link Stats#MEAN MEAN}: Arithmetic mean (average), equivalent
     *         to sum/count.
     *     <li>{@link Stats#STDV STDV}: Sample standard deviation (denominator
     *         is count-1).
     *     <li>{@link Stats#VARIANCE VARIANCE}: Unbiased sample variance
     *         (denominator is count-1).
     *     <li>{@link Stats#SKEW SKEW}: Skewness (third standardized moment).
     *     <li>{@link Stats#KURTOSIS KURTOSIS}: Kurtosis (fourth standardized
     *         moment).
     *     <li>{@link Stats#SUM SUM}: Sum of all values in the column(s).
     *     <li>{@link Stats#MIN MIN}: Minimum value of the column(s).
     *     <li>{@link Stats#MAX MAX}: Maximum value of the column(s).
     *     <li>{@link Stats#WEIGHTED_AVERAGE WEIGHTED_AVERAGE}: Weighted
     *         arithmetic mean (using the option {@link
     *         Options#WEIGHT_COLUMN_NAME WEIGHT_COLUMN_NAME} as the weighting
     *         column).
     *     <li>{@link Stats#CARDINALITY CARDINALITY}: Number of unique values
     *         in the column(s).
     *     <li>{@link Stats#ESTIMATED_CARDINALITY ESTIMATED_CARDINALITY}:
     *         Estimate (via hyperloglog technique) of the number of unique
     *         values in the column(s).
     *     <li>{@link Stats#PERCENTILE PERCENTILE}: Estimate (via t-digest) of
     *         the given percentile of the column(s) (percentile(50.0) will be
     *         an approximation of the median). Add a second, comma-separated
     *         value to calculate percentile resolution, e.g.,
     *         'percentile(75,150)'
     *     <li>{@link Stats#PERCENTILE_RANK PERCENTILE_RANK}: Estimate (via
     *         t-digest) of the percentile rank of the given value in the
     *         column(s) (if the given value is the median of the column(s),
     *         percentile_rank(&lt;median&gt;) will return approximately 50.0).
     * </ul>
     *
     * @return The current value of {@code stats}.
     */
    public String getStats() {
        return stats;
    }

    /**
     * Comma separated list of the statistics to calculate,
     * e.g.&nbsp;"sum,mean".
     * Supported values:
     * <ul>
     *     <li>{@link Stats#COUNT COUNT}: Number of objects (independent of the
     *         given column(s)).
     *     <li>{@link Stats#MEAN MEAN}: Arithmetic mean (average), equivalent
     *         to sum/count.
     *     <li>{@link Stats#STDV STDV}: Sample standard deviation (denominator
     *         is count-1).
     *     <li>{@link Stats#VARIANCE VARIANCE}: Unbiased sample variance
     *         (denominator is count-1).
     *     <li>{@link Stats#SKEW SKEW}: Skewness (third standardized moment).
     *     <li>{@link Stats#KURTOSIS KURTOSIS}: Kurtosis (fourth standardized
     *         moment).
     *     <li>{@link Stats#SUM SUM}: Sum of all values in the column(s).
     *     <li>{@link Stats#MIN MIN}: Minimum value of the column(s).
     *     <li>{@link Stats#MAX MAX}: Maximum value of the column(s).
     *     <li>{@link Stats#WEIGHTED_AVERAGE WEIGHTED_AVERAGE}: Weighted
     *         arithmetic mean (using the option {@link
     *         Options#WEIGHT_COLUMN_NAME WEIGHT_COLUMN_NAME} as the weighting
     *         column).
     *     <li>{@link Stats#CARDINALITY CARDINALITY}: Number of unique values
     *         in the column(s).
     *     <li>{@link Stats#ESTIMATED_CARDINALITY ESTIMATED_CARDINALITY}:
     *         Estimate (via hyperloglog technique) of the number of unique
     *         values in the column(s).
     *     <li>{@link Stats#PERCENTILE PERCENTILE}: Estimate (via t-digest) of
     *         the given percentile of the column(s) (percentile(50.0) will be
     *         an approximation of the median). Add a second, comma-separated
     *         value to calculate percentile resolution, e.g.,
     *         'percentile(75,150)'
     *     <li>{@link Stats#PERCENTILE_RANK PERCENTILE_RANK}: Estimate (via
     *         t-digest) of the percentile rank of the given value in the
     *         column(s) (if the given value is the median of the column(s),
     *         percentile_rank(&lt;median&gt;) will return approximately 50.0).
     * </ul>
     *
     * @param stats  The new value for {@code stats}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AggregateStatisticsRequest setStats(String stats) {
        this.stats = (stats == null) ? "" : stats;
        return this;
    }

    /**
     * Optional parameters.
     * <ul>
     *     <li>{@link Options#ADDITIONAL_COLUMN_NAMES ADDITIONAL_COLUMN_NAMES}:
     *         A list of comma separated column names over which statistics can
     *         be accumulated along with the primary column.  All columns
     *         listed and {@link #getColumnName() columnName} must be of the
     *         same type.  Must not include the column specified in {@link
     *         #getColumnName() columnName} and no column can be listed twice.
     *     <li>{@link Options#WEIGHT_COLUMN_NAME WEIGHT_COLUMN_NAME}: Name of
     *         column used as weighting attribute for the weighted average
     *         statistic.
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
     *     <li>{@link Options#ADDITIONAL_COLUMN_NAMES ADDITIONAL_COLUMN_NAMES}:
     *         A list of comma separated column names over which statistics can
     *         be accumulated along with the primary column.  All columns
     *         listed and {@link #getColumnName() columnName} must be of the
     *         same type.  Must not include the column specified in {@link
     *         #getColumnName() columnName} and no column can be listed twice.
     *     <li>{@link Options#WEIGHT_COLUMN_NAME WEIGHT_COLUMN_NAME}: Name of
     *         column used as weighting attribute for the weighted average
     *         statistic.
     * </ul>
     * The default value is an empty {@link Map}.
     *
     * @param options  The new value for {@code options}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AggregateStatisticsRequest setOptions(Map<String, String> options) {
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
                return this.columnName;

            case 2:
                return this.stats;

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
                this.columnName = (String)value;
                break;

            case 2:
                this.stats = (String)value;
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

        AggregateStatisticsRequest that = (AggregateStatisticsRequest)obj;

        return ( this.tableName.equals( that.tableName )
                 && this.columnName.equals( that.columnName )
                 && this.stats.equals( that.stats )
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
        builder.append( gd.toString( "columnName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.columnName ) );
        builder.append( ", " );
        builder.append( gd.toString( "stats" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.stats ) );
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
        hashCode = (31 * hashCode) + this.columnName.hashCode();
        hashCode = (31 * hashCode) + this.stats.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }
}
