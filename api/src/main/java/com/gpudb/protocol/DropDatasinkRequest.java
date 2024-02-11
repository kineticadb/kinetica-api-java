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
 * com.gpudb.GPUdb#dropDatasink(DropDatasinkRequest) GPUdb.dropDatasink}.
 * <p>
 * Drops an existing <a href="../../../../../../concepts/data_sinks/"
 * target="_top">data sink</a>.
 * <p>
 * By default, if any <a href="../../../../../../concepts/table_monitors"
 * target="_top">table monitors</a> use this sink as a destination, the request
 * will be blocked unless option {@link Options#CLEAR_TABLE_MONITORS
 * CLEAR_TABLE_MONITORS} is {@link Options#TRUE TRUE}.
 */
public class DropDatasinkRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("DropDatasinkRequest")
            .namespace("com.gpudb")
            .fields()
                .name("name").type().stringType().noDefault()
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
     * A set of string constants for the {@link DropDatasinkRequest} parameter
     * {@link #getOptions() options}.
     * <p>
     * Optional parameters.
     */
    public static final class Options {
        /**
         * If {@link Options#TRUE TRUE}, any <a
         * href="../../../../../../concepts/table_monitors/"
         * target="_top">table monitors</a> that use this data sink will be
         * cleared.
         * Supported values:
         * <ul>
         *     <li>{@link Options#TRUE TRUE}
         *     <li>{@link Options#FALSE FALSE}
         * </ul>
         * The default value is {@link Options#FALSE FALSE}.
         */
        public static final String CLEAR_TABLE_MONITORS = "clear_table_monitors";

        public static final String TRUE = "true";
        public static final String FALSE = "false";

        private Options() {  }
    }

    private String name;
    private Map<String, String> options;

    /**
     * Constructs a DropDatasinkRequest object with default parameters.
     */
    public DropDatasinkRequest() {
        name = "";
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs a DropDatasinkRequest object with the specified parameters.
     *
     * @param name  Name of the data sink to be dropped. Must be an existing
     *              data sink.
     * @param options  Optional parameters.
     *                 <ul>
     *                     <li>{@link Options#CLEAR_TABLE_MONITORS
     *                         CLEAR_TABLE_MONITORS}: If {@link Options#TRUE
     *                         TRUE}, any <a
     *                         href="../../../../../../concepts/table_monitors/"
     *                         target="_top">table monitors</a> that use this
     *                         data sink will be cleared.
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
    public DropDatasinkRequest(String name, Map<String, String> options) {
        this.name = (name == null) ? "" : name;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * Name of the data sink to be dropped. Must be an existing data sink.
     *
     * @return The current value of {@code name}.
     */
    public String getName() {
        return name;
    }

    /**
     * Name of the data sink to be dropped. Must be an existing data sink.
     *
     * @param name  The new value for {@code name}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public DropDatasinkRequest setName(String name) {
        this.name = (name == null) ? "" : name;
        return this;
    }

    /**
     * Optional parameters.
     * <ul>
     *     <li>{@link Options#CLEAR_TABLE_MONITORS CLEAR_TABLE_MONITORS}: If
     *         {@link Options#TRUE TRUE}, any <a
     *         href="../../../../../../concepts/table_monitors/"
     *         target="_top">table monitors</a> that use this data sink will be
     *         cleared.
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
     *     <li>{@link Options#CLEAR_TABLE_MONITORS CLEAR_TABLE_MONITORS}: If
     *         {@link Options#TRUE TRUE}, any <a
     *         href="../../../../../../concepts/table_monitors/"
     *         target="_top">table monitors</a> that use this data sink will be
     *         cleared.
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
    public DropDatasinkRequest setOptions(Map<String, String> options) {
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
                return this.name;

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
                this.name = (String)value;
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

        DropDatasinkRequest that = (DropDatasinkRequest)obj;

        return ( this.name.equals( that.name )
                 && this.options.equals( that.options ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "name" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.name ) );
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
        hashCode = (31 * hashCode) + this.name.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }
}
