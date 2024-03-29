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
 * com.gpudb.GPUdb#aggregateStatisticsByRange(AggregateStatisticsByRangeRequest)
 * GPUdb.aggregateStatisticsByRange}.
 * <p>
 * Divides the given set into bins and calculates statistics of the values of a
 * value-column in each bin.  The bins are based on the values of a given
 * binning-column.  The statistics that may be requested are mean, stdv
 * (standard deviation), variance, skew, kurtosis, sum, min, max, first, last
 * and weighted average. In addition to the requested statistics the count of
 * total samples in each bin is returned. This counts vector is just the
 * histogram of the column used to divide the set members into bins. The
 * weighted average statistic requires a weight column to be specified in
 * {@link Options#WEIGHT_COLUMN_NAME WEIGHT_COLUMN_NAME}. The weighted average
 * is then defined as the sum of the products of the value column times the
 * weight column divided by the sum of the weight column.
 * <p>
 * There are two methods for binning the set members. In the first, which can
 * be used for numeric valued binning-columns, a min, max and interval are
 * specified. The number of bins, nbins, is the integer upper bound of
 * (max-min)/interval. Values that fall in the range
 * [min+n*interval,min+(n+1)*interval) are placed in the nth bin where n ranges
 * from 0..nbin-2. The final bin is [min+(nbin-1)*interval,max]. In the second
 * method, {@link Options#BIN_VALUES BIN_VALUES} specifies a list of binning
 * column values. Binning-columns whose value matches the nth member of the
 * {@link Options#BIN_VALUES BIN_VALUES} list are placed in the nth bin. When a
 * list is provided, the binning-column must be of type string or int.
 * <p>
 * NOTE:  The Kinetica instance being accessed must be running a CUDA
 * (GPU-based) build to service this request.
 */
public class AggregateStatisticsByRangeRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("AggregateStatisticsByRangeRequest")
            .namespace("com.gpudb")
            .fields()
                .name("tableName").type().stringType().noDefault()
                .name("selectExpression").type().stringType().noDefault()
                .name("columnName").type().stringType().noDefault()
                .name("valueColumnName").type().stringType().noDefault()
                .name("stats").type().stringType().noDefault()
                .name("start").type().doubleType().noDefault()
                .name("end").type().doubleType().noDefault()
                .name("interval").type().doubleType().noDefault()
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
     * A set of string constants for the {@link
     * AggregateStatisticsByRangeRequest} parameter {@link #getOptions()
     * options}.
     * <p>
     * Map of optional parameters:
     */
    public static final class Options {
        /**
         * A list of comma separated value-column names over which statistics
         * can be accumulated along with the primary value_column.
         */
        public static final String ADDITIONAL_COLUMN_NAMES = "additional_column_names";

        /**
         * A list of comma separated binning-column values. Values that match
         * the nth bin_values value are placed in the nth bin.
         */
        public static final String BIN_VALUES = "bin_values";

        /**
         * Name of the column used as weighting column for the weighted_average
         * statistic.
         */
        public static final String WEIGHT_COLUMN_NAME = "weight_column_name";

        /**
         * Name of the column used for candlestick charting techniques.
         */
        public static final String ORDER_COLUMN_NAME = "order_column_name";

        private Options() {  }
    }

    private String tableName;
    private String selectExpression;
    private String columnName;
    private String valueColumnName;
    private String stats;
    private double start;
    private double end;
    private double interval;
    private Map<String, String> options;

    /**
     * Constructs an AggregateStatisticsByRangeRequest object with default
     * parameters.
     */
    public AggregateStatisticsByRangeRequest() {
        tableName = "";
        selectExpression = "";
        columnName = "";
        valueColumnName = "";
        stats = "";
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs an AggregateStatisticsByRangeRequest object with the
     * specified parameters.
     *
     * @param tableName  Name of the table on which the ranged-statistics
     *                   operation will be performed, in
     *                   [schema_name.]table_name format, using standard <a
     *                   href="../../../../../../concepts/tables/#table-name-resolution"
     *                   target="_top">name resolution rules</a>.
     * @param selectExpression  For a non-empty expression statistics are
     *                          calculated for those records for which the
     *                          expression is true. The default value is ''.
     * @param columnName  Name of the binning-column used to divide the set
     *                    samples into bins.
     * @param valueColumnName  Name of the value-column for which statistics
     *                         are to be computed.
     * @param stats  A string of comma separated list of the statistics to
     *               calculate, e.g. 'sum,mean'. Available statistics: mean,
     *               stdv (standard deviation), variance, skew, kurtosis, sum.
     * @param start  The lower bound of the binning-column.
     * @param end  The upper bound of the binning-column.
     * @param interval  The interval of a bin. Set members fall into bin i if
     *                  the binning-column falls in the range
     *                  [start+interval*i, start+interval*(i+1)).
     * @param options  Map of optional parameters:
     *                 <ul>
     *                     <li>{@link Options#ADDITIONAL_COLUMN_NAMES
     *                         ADDITIONAL_COLUMN_NAMES}: A list of comma
     *                         separated value-column names over which
     *                         statistics can be accumulated along with the
     *                         primary value_column.
     *                     <li>{@link Options#BIN_VALUES BIN_VALUES}: A list of
     *                         comma separated binning-column values. Values
     *                         that match the nth bin_values value are placed
     *                         in the nth bin.
     *                     <li>{@link Options#WEIGHT_COLUMN_NAME
     *                         WEIGHT_COLUMN_NAME}: Name of the column used as
     *                         weighting column for the weighted_average
     *                         statistic.
     *                     <li>{@link Options#ORDER_COLUMN_NAME
     *                         ORDER_COLUMN_NAME}: Name of the column used for
     *                         candlestick charting techniques.
     *                 </ul>
     *                 The default value is an empty {@link Map}.
     */
    public AggregateStatisticsByRangeRequest(String tableName, String selectExpression, String columnName, String valueColumnName, String stats, double start, double end, double interval, Map<String, String> options) {
        this.tableName = (tableName == null) ? "" : tableName;
        this.selectExpression = (selectExpression == null) ? "" : selectExpression;
        this.columnName = (columnName == null) ? "" : columnName;
        this.valueColumnName = (valueColumnName == null) ? "" : valueColumnName;
        this.stats = (stats == null) ? "" : stats;
        this.start = start;
        this.end = end;
        this.interval = interval;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * Name of the table on which the ranged-statistics operation will be
     * performed, in [schema_name.]table_name format, using standard <a
     * href="../../../../../../concepts/tables/#table-name-resolution"
     * target="_top">name resolution rules</a>.
     *
     * @return The current value of {@code tableName}.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Name of the table on which the ranged-statistics operation will be
     * performed, in [schema_name.]table_name format, using standard <a
     * href="../../../../../../concepts/tables/#table-name-resolution"
     * target="_top">name resolution rules</a>.
     *
     * @param tableName  The new value for {@code tableName}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AggregateStatisticsByRangeRequest setTableName(String tableName) {
        this.tableName = (tableName == null) ? "" : tableName;
        return this;
    }

    /**
     * For a non-empty expression statistics are calculated for those records
     * for which the expression is true. The default value is ''.
     *
     * @return The current value of {@code selectExpression}.
     */
    public String getSelectExpression() {
        return selectExpression;
    }

    /**
     * For a non-empty expression statistics are calculated for those records
     * for which the expression is true. The default value is ''.
     *
     * @param selectExpression  The new value for {@code selectExpression}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AggregateStatisticsByRangeRequest setSelectExpression(String selectExpression) {
        this.selectExpression = (selectExpression == null) ? "" : selectExpression;
        return this;
    }

    /**
     * Name of the binning-column used to divide the set samples into bins.
     *
     * @return The current value of {@code columnName}.
     */
    public String getColumnName() {
        return columnName;
    }

    /**
     * Name of the binning-column used to divide the set samples into bins.
     *
     * @param columnName  The new value for {@code columnName}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AggregateStatisticsByRangeRequest setColumnName(String columnName) {
        this.columnName = (columnName == null) ? "" : columnName;
        return this;
    }

    /**
     * Name of the value-column for which statistics are to be computed.
     *
     * @return The current value of {@code valueColumnName}.
     */
    public String getValueColumnName() {
        return valueColumnName;
    }

    /**
     * Name of the value-column for which statistics are to be computed.
     *
     * @param valueColumnName  The new value for {@code valueColumnName}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AggregateStatisticsByRangeRequest setValueColumnName(String valueColumnName) {
        this.valueColumnName = (valueColumnName == null) ? "" : valueColumnName;
        return this;
    }

    /**
     * A string of comma separated list of the statistics to calculate,
     * e.g.&nbsp;'sum,mean'. Available statistics: mean, stdv (standard
     * deviation), variance, skew, kurtosis, sum.
     *
     * @return The current value of {@code stats}.
     */
    public String getStats() {
        return stats;
    }

    /**
     * A string of comma separated list of the statistics to calculate,
     * e.g.&nbsp;'sum,mean'. Available statistics: mean, stdv (standard
     * deviation), variance, skew, kurtosis, sum.
     *
     * @param stats  The new value for {@code stats}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AggregateStatisticsByRangeRequest setStats(String stats) {
        this.stats = (stats == null) ? "" : stats;
        return this;
    }

    /**
     * The lower bound of the binning-column.
     *
     * @return The current value of {@code start}.
     */
    public double getStart() {
        return start;
    }

    /**
     * The lower bound of the binning-column.
     *
     * @param start  The new value for {@code start}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AggregateStatisticsByRangeRequest setStart(double start) {
        this.start = start;
        return this;
    }

    /**
     * The upper bound of the binning-column.
     *
     * @return The current value of {@code end}.
     */
    public double getEnd() {
        return end;
    }

    /**
     * The upper bound of the binning-column.
     *
     * @param end  The new value for {@code end}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AggregateStatisticsByRangeRequest setEnd(double end) {
        this.end = end;
        return this;
    }

    /**
     * The interval of a bin. Set members fall into bin i if the binning-column
     * falls in the range [start+interval*i, start+interval*(i+1)).
     *
     * @return The current value of {@code interval}.
     */
    public double getInterval() {
        return interval;
    }

    /**
     * The interval of a bin. Set members fall into bin i if the binning-column
     * falls in the range [start+interval*i, start+interval*(i+1)).
     *
     * @param interval  The new value for {@code interval}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AggregateStatisticsByRangeRequest setInterval(double interval) {
        this.interval = interval;
        return this;
    }

    /**
     * Map of optional parameters:
     * <ul>
     *     <li>{@link Options#ADDITIONAL_COLUMN_NAMES ADDITIONAL_COLUMN_NAMES}:
     *         A list of comma separated value-column names over which
     *         statistics can be accumulated along with the primary
     *         value_column.
     *     <li>{@link Options#BIN_VALUES BIN_VALUES}: A list of comma separated
     *         binning-column values. Values that match the nth bin_values
     *         value are placed in the nth bin.
     *     <li>{@link Options#WEIGHT_COLUMN_NAME WEIGHT_COLUMN_NAME}: Name of
     *         the column used as weighting column for the weighted_average
     *         statistic.
     *     <li>{@link Options#ORDER_COLUMN_NAME ORDER_COLUMN_NAME}: Name of the
     *         column used for candlestick charting techniques.
     * </ul>
     * The default value is an empty {@link Map}.
     *
     * @return The current value of {@code options}.
     */
    public Map<String, String> getOptions() {
        return options;
    }

    /**
     * Map of optional parameters:
     * <ul>
     *     <li>{@link Options#ADDITIONAL_COLUMN_NAMES ADDITIONAL_COLUMN_NAMES}:
     *         A list of comma separated value-column names over which
     *         statistics can be accumulated along with the primary
     *         value_column.
     *     <li>{@link Options#BIN_VALUES BIN_VALUES}: A list of comma separated
     *         binning-column values. Values that match the nth bin_values
     *         value are placed in the nth bin.
     *     <li>{@link Options#WEIGHT_COLUMN_NAME WEIGHT_COLUMN_NAME}: Name of
     *         the column used as weighting column for the weighted_average
     *         statistic.
     *     <li>{@link Options#ORDER_COLUMN_NAME ORDER_COLUMN_NAME}: Name of the
     *         column used for candlestick charting techniques.
     * </ul>
     * The default value is an empty {@link Map}.
     *
     * @param options  The new value for {@code options}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AggregateStatisticsByRangeRequest setOptions(Map<String, String> options) {
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
                return this.selectExpression;

            case 2:
                return this.columnName;

            case 3:
                return this.valueColumnName;

            case 4:
                return this.stats;

            case 5:
                return this.start;

            case 6:
                return this.end;

            case 7:
                return this.interval;

            case 8:
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
                this.selectExpression = (String)value;
                break;

            case 2:
                this.columnName = (String)value;
                break;

            case 3:
                this.valueColumnName = (String)value;
                break;

            case 4:
                this.stats = (String)value;
                break;

            case 5:
                this.start = (Double)value;
                break;

            case 6:
                this.end = (Double)value;
                break;

            case 7:
                this.interval = (Double)value;
                break;

            case 8:
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

        AggregateStatisticsByRangeRequest that = (AggregateStatisticsByRangeRequest)obj;

        return ( this.tableName.equals( that.tableName )
                 && this.selectExpression.equals( that.selectExpression )
                 && this.columnName.equals( that.columnName )
                 && this.valueColumnName.equals( that.valueColumnName )
                 && this.stats.equals( that.stats )
                 && ( (Double)this.start ).equals( (Double)that.start )
                 && ( (Double)this.end ).equals( (Double)that.end )
                 && ( (Double)this.interval ).equals( (Double)that.interval )
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
        builder.append( gd.toString( "selectExpression" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.selectExpression ) );
        builder.append( ", " );
        builder.append( gd.toString( "columnName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.columnName ) );
        builder.append( ", " );
        builder.append( gd.toString( "valueColumnName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.valueColumnName ) );
        builder.append( ", " );
        builder.append( gd.toString( "stats" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.stats ) );
        builder.append( ", " );
        builder.append( gd.toString( "start" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.start ) );
        builder.append( ", " );
        builder.append( gd.toString( "end" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.end ) );
        builder.append( ", " );
        builder.append( gd.toString( "interval" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.interval ) );
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
        hashCode = (31 * hashCode) + this.selectExpression.hashCode();
        hashCode = (31 * hashCode) + this.columnName.hashCode();
        hashCode = (31 * hashCode) + this.valueColumnName.hashCode();
        hashCode = (31 * hashCode) + this.stats.hashCode();
        hashCode = (31 * hashCode) + ((Double)this.start).hashCode();
        hashCode = (31 * hashCode) + ((Double)this.end).hashCode();
        hashCode = (31 * hashCode) + ((Double)this.interval).hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }
}
