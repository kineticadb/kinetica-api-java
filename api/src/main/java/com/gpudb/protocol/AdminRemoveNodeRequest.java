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
 * com.gpudb.GPUdb#adminRemoveNode(AdminRemoveNodeRequest)}.
 * <p>
 * Remove a node from the cluster.  Note that this operation could take a long
 * time to complete for big clusters.  The data is transferred to other nodes
 * in the cluster before the node is removed.
 */
public class AdminRemoveNodeRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("AdminRemoveNodeRequest")
            .namespace("com.gpudb")
            .fields()
                .name("rank").type().intType().noDefault()
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
     * com.gpudb.protocol.AdminRemoveNodeRequest.Options#RESHARD RESHARD}: When
     * {@code true}, then the shards from nodes will be moved to the other
     * nodes in the cluster. When false, then the node will only be removed
     * from the cluster if the node does not contain any data shards, otherwise
     * an error is returned.  Note that for big clusters, this data transfer
     * could be time consuming and also result in delay in responding to
     * queries for busy clusters.
     * Supported values:
     * <ul>
     *         <li> {@link
     * com.gpudb.protocol.AdminRemoveNodeRequest.Options#TRUE TRUE}
     *         <li> {@link
     * com.gpudb.protocol.AdminRemoveNodeRequest.Options#FALSE FALSE}
     * </ul>
     * The default value is {@link
     * com.gpudb.protocol.AdminRemoveNodeRequest.Options#TRUE TRUE}.
     *         <li> {@link
     * com.gpudb.protocol.AdminRemoveNodeRequest.Options#FORCE FORCE}: When
     * {@code true}, the rank is immediately shutdown and removed from the
     * cluster.  This will result in loss of any data that is present in the
     * node at the time of the request.
     * Supported values:
     * <ul>
     *         <li> {@link
     * com.gpudb.protocol.AdminRemoveNodeRequest.Options#TRUE TRUE}
     *         <li> {@link
     * com.gpudb.protocol.AdminRemoveNodeRequest.Options#FALSE FALSE}
     * </ul>
     * The default value is {@link
     * com.gpudb.protocol.AdminRemoveNodeRequest.Options#FALSE FALSE}.
     * </ul>
     * A set of string constants for the parameter {@code options}.
     */
    public static final class Options {

        /**
         * When {@code true}, then the shards from nodes will be moved to the
         * other nodes in the cluster. When false, then the node will only be
         * removed from the cluster if the node does not contain any data
         * shards, otherwise an error is returned.  Note that for big clusters,
         * this data transfer could be time consuming and also result in delay
         * in responding to queries for busy clusters.
         * Supported values:
         * <ul>
         *         <li> {@link
         * com.gpudb.protocol.AdminRemoveNodeRequest.Options#TRUE TRUE}
         *         <li> {@link
         * com.gpudb.protocol.AdminRemoveNodeRequest.Options#FALSE FALSE}
         * </ul>
         * The default value is {@link
         * com.gpudb.protocol.AdminRemoveNodeRequest.Options#TRUE TRUE}.
         */
        public static final String RESHARD = "reshard";
        public static final String TRUE = "true";
        public static final String FALSE = "false";

        /**
         * When {@code true}, the rank is immediately shutdown and removed from
         * the cluster.  This will result in loss of any data that is present
         * in the node at the time of the request.
         * Supported values:
         * <ul>
         *         <li> {@link
         * com.gpudb.protocol.AdminRemoveNodeRequest.Options#TRUE TRUE}
         *         <li> {@link
         * com.gpudb.protocol.AdminRemoveNodeRequest.Options#FALSE FALSE}
         * </ul>
         * The default value is {@link
         * com.gpudb.protocol.AdminRemoveNodeRequest.Options#FALSE FALSE}.
         */
        public static final String FORCE = "force";

        private Options() {  }
    }

    private int rank;
    private Map<String, String> options;


    /**
     * Constructs an AdminRemoveNodeRequest object with default parameters.
     */
    public AdminRemoveNodeRequest() {
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs an AdminRemoveNodeRequest object with the specified
     * parameters.
     * 
     * @param rank  Rank number of the node being removed from the cluster.
     * @param options  Optional parameters.
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminRemoveNodeRequest.Options#RESHARD
     *                 RESHARD}: When {@code true}, then the shards from nodes
     *                 will be moved to the other nodes in the cluster. When
     *                 false, then the node will only be removed from the
     *                 cluster if the node does not contain any data shards,
     *                 otherwise an error is returned.  Note that for big
     *                 clusters, this data transfer could be time consuming and
     *                 also result in delay in responding to queries for busy
     *                 clusters.
     *                 Supported values:
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminRemoveNodeRequest.Options#TRUE
     *                 TRUE}
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminRemoveNodeRequest.Options#FALSE
     *                 FALSE}
     *                 </ul>
     *                 The default value is {@link
     *                 com.gpudb.protocol.AdminRemoveNodeRequest.Options#TRUE
     *                 TRUE}.
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminRemoveNodeRequest.Options#FORCE
     *                 FORCE}: When {@code true}, the rank is immediately
     *                 shutdown and removed from the cluster.  This will result
     *                 in loss of any data that is present in the node at the
     *                 time of the request.
     *                 Supported values:
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminRemoveNodeRequest.Options#TRUE
     *                 TRUE}
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminRemoveNodeRequest.Options#FALSE
     *                 FALSE}
     *                 </ul>
     *                 The default value is {@link
     *                 com.gpudb.protocol.AdminRemoveNodeRequest.Options#FALSE
     *                 FALSE}.
     *                 </ul>
     * 
     */
    public AdminRemoveNodeRequest(int rank, Map<String, String> options) {
        this.rank = rank;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * 
     * @return Rank number of the node being removed from the cluster.
     * 
     */
    public int getRank() {
        return rank;
    }

    /**
     * 
     * @param rank  Rank number of the node being removed from the cluster.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public AdminRemoveNodeRequest setRank(int rank) {
        this.rank = rank;
        return this;
    }

    /**
     * 
     * @return Optional parameters.
     *         <ul>
     *                 <li> {@link
     *         com.gpudb.protocol.AdminRemoveNodeRequest.Options#RESHARD
     *         RESHARD}: When {@code true}, then the shards from nodes will be
     *         moved to the other nodes in the cluster. When false, then the
     *         node will only be removed from the cluster if the node does not
     *         contain any data shards, otherwise an error is returned.  Note
     *         that for big clusters, this data transfer could be time
     *         consuming and also result in delay in responding to queries for
     *         busy clusters.
     *         Supported values:
     *         <ul>
     *                 <li> {@link
     *         com.gpudb.protocol.AdminRemoveNodeRequest.Options#TRUE TRUE}
     *                 <li> {@link
     *         com.gpudb.protocol.AdminRemoveNodeRequest.Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link
     *         com.gpudb.protocol.AdminRemoveNodeRequest.Options#TRUE TRUE}.
     *                 <li> {@link
     *         com.gpudb.protocol.AdminRemoveNodeRequest.Options#FORCE FORCE}:
     *         When {@code true}, the rank is immediately shutdown and removed
     *         from the cluster.  This will result in loss of any data that is
     *         present in the node at the time of the request.
     *         Supported values:
     *         <ul>
     *                 <li> {@link
     *         com.gpudb.protocol.AdminRemoveNodeRequest.Options#TRUE TRUE}
     *                 <li> {@link
     *         com.gpudb.protocol.AdminRemoveNodeRequest.Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link
     *         com.gpudb.protocol.AdminRemoveNodeRequest.Options#FALSE FALSE}.
     *         </ul>
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
     *                 com.gpudb.protocol.AdminRemoveNodeRequest.Options#RESHARD
     *                 RESHARD}: When {@code true}, then the shards from nodes
     *                 will be moved to the other nodes in the cluster. When
     *                 false, then the node will only be removed from the
     *                 cluster if the node does not contain any data shards,
     *                 otherwise an error is returned.  Note that for big
     *                 clusters, this data transfer could be time consuming and
     *                 also result in delay in responding to queries for busy
     *                 clusters.
     *                 Supported values:
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminRemoveNodeRequest.Options#TRUE
     *                 TRUE}
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminRemoveNodeRequest.Options#FALSE
     *                 FALSE}
     *                 </ul>
     *                 The default value is {@link
     *                 com.gpudb.protocol.AdminRemoveNodeRequest.Options#TRUE
     *                 TRUE}.
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminRemoveNodeRequest.Options#FORCE
     *                 FORCE}: When {@code true}, the rank is immediately
     *                 shutdown and removed from the cluster.  This will result
     *                 in loss of any data that is present in the node at the
     *                 time of the request.
     *                 Supported values:
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminRemoveNodeRequest.Options#TRUE
     *                 TRUE}
     *                         <li> {@link
     *                 com.gpudb.protocol.AdminRemoveNodeRequest.Options#FALSE
     *                 FALSE}
     *                 </ul>
     *                 The default value is {@link
     *                 com.gpudb.protocol.AdminRemoveNodeRequest.Options#FALSE
     *                 FALSE}.
     *                 </ul>
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public AdminRemoveNodeRequest setOptions(Map<String, String> options) {
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
                return this.rank;

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
     * 
     */
    @Override
    @SuppressWarnings("unchecked")
    public void put(int index, Object value) {
        switch (index) {
            case 0:
                this.rank = (Integer)value;
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

        AdminRemoveNodeRequest that = (AdminRemoveNodeRequest)obj;

        return ( ( this.rank == that.rank )
                 && this.options.equals( that.options ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "rank" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.rank ) );
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
        hashCode = (31 * hashCode) + this.rank;
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }

}