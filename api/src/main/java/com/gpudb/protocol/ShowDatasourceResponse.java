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
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;


/**
 * A set of results returned by {@link
 * com.gpudb.GPUdb#showDatasource(ShowDatasourceRequest)}.
 */
public class ShowDatasourceResponse implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("ShowDatasourceResponse")
            .namespace("com.gpudb")
            .fields()
                .name("datasourceNames").type().array().items().stringType().noDefault()
                .name("storageProviderTypes").type().array().items().stringType().noDefault()
                .name("additionalInfo").type().array().items().map().values().stringType().noDefault()
                .name("info").type().map().values().stringType().noDefault()
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
     * The storage provider type of the data sources named in {@code
     * datasourceNames}.
     * Supported values:
     * <ul>
     *         <li> {@link
     * com.gpudb.protocol.ShowDatasourceResponse.StorageProviderTypes#HDFS
     * HDFS}: Apache Hadoop Distributed File System
     *         <li> {@link
     * com.gpudb.protocol.ShowDatasourceResponse.StorageProviderTypes#S3 S3}:
     * Amazon S3 bucket
     * </ul>
     * A set of string constants for the parameter {@code
     * storageProviderTypes}.
     */
    public static final class StorageProviderTypes {

        /**
         * Apache Hadoop Distributed File System
         */
        public static final String HDFS = "hdfs";

        /**
         * Amazon S3 bucket
         */
        public static final String S3 = "s3";

        private StorageProviderTypes() {  }
    }


    /**
     * Additional information about the respective data sources in {@code
     * datasourceNames}.
     * Supported values:
     * <ul>
     * </ul>
     * A set of string constants for the parameter {@code additionalInfo}.
     */
    public static final class AdditionalInfo {

        /**
         * Location of the remote storage in
         * 'storage_provider_type://[storage_path[:storage_port]]' format
         */
        public static final String LOCATION = "location";

        /**
         * Name of the Amazon S3 bucket used as the data source
         */
        public static final String S3_BUCKET_NAME = "s3_bucket_name";

        /**
         * Name of the Amazon S3 region where the bucket is located
         */
        public static final String S3_REGION = "s3_region";

        /**
         * Kerberos key for the given HDFS user
         */
        public static final String HDFS_KERBEROS_KEYTAB = "hdfs_kerberos_keytab";

        /**
         * Name of the remote system user
         */
        public static final String USER_NAME = "user_name";

        private AdditionalInfo() {  }
    }

    private List<String> datasourceNames;
    private List<String> storageProviderTypes;
    private List<Map<String, String>> additionalInfo;
    private Map<String, String> info;


    /**
     * Constructs a ShowDatasourceResponse object with default parameters.
     */
    public ShowDatasourceResponse() {
    }

    /**
     * 
     * @return The data source names.
     * 
     */
    public List<String> getDatasourceNames() {
        return datasourceNames;
    }

    /**
     * 
     * @param datasourceNames  The data source names.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public ShowDatasourceResponse setDatasourceNames(List<String> datasourceNames) {
        this.datasourceNames = (datasourceNames == null) ? new ArrayList<String>() : datasourceNames;
        return this;
    }

    /**
     * 
     * @return The storage provider type of the data sources named in {@code
     *         datasourceNames}.
     *         Supported values:
     *         <ul>
     *                 <li> {@link
     *         com.gpudb.protocol.ShowDatasourceResponse.StorageProviderTypes#HDFS
     *         HDFS}: Apache Hadoop Distributed File System
     *                 <li> {@link
     *         com.gpudb.protocol.ShowDatasourceResponse.StorageProviderTypes#S3
     *         S3}: Amazon S3 bucket
     *         </ul>
     * 
     */
    public List<String> getStorageProviderTypes() {
        return storageProviderTypes;
    }

    /**
     * 
     * @param storageProviderTypes  The storage provider type of the data
     *                              sources named in {@code datasourceNames}.
     *                              Supported values:
     *                              <ul>
     *                                      <li> {@link
     *                              com.gpudb.protocol.ShowDatasourceResponse.StorageProviderTypes#HDFS
     *                              HDFS}: Apache Hadoop Distributed File
     *                              System
     *                                      <li> {@link
     *                              com.gpudb.protocol.ShowDatasourceResponse.StorageProviderTypes#S3
     *                              S3}: Amazon S3 bucket
     *                              </ul>
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public ShowDatasourceResponse setStorageProviderTypes(List<String> storageProviderTypes) {
        this.storageProviderTypes = (storageProviderTypes == null) ? new ArrayList<String>() : storageProviderTypes;
        return this;
    }

    /**
     * 
     * @return Additional information about the respective data sources in
     *         {@code datasourceNames}.
     *         Supported values:
     *         <ul>
     *         </ul>
     * 
     */
    public List<Map<String, String>> getAdditionalInfo() {
        return additionalInfo;
    }

    /**
     * 
     * @param additionalInfo  Additional information about the respective data
     *                        sources in {@code datasourceNames}.
     *                        Supported values:
     *                        <ul>
     *                        </ul>
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public ShowDatasourceResponse setAdditionalInfo(List<Map<String, String>> additionalInfo) {
        this.additionalInfo = (additionalInfo == null) ? new ArrayList<Map<String, String>>() : additionalInfo;
        return this;
    }

    /**
     * 
     * @return Additional information.
     * 
     */
    public Map<String, String> getInfo() {
        return info;
    }

    /**
     * 
     * @param info  Additional information.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public ShowDatasourceResponse setInfo(Map<String, String> info) {
        this.info = (info == null) ? new LinkedHashMap<String, String>() : info;
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
                return this.datasourceNames;

            case 1:
                return this.storageProviderTypes;

            case 2:
                return this.additionalInfo;

            case 3:
                return this.info;

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
                this.datasourceNames = (List<String>)value;
                break;

            case 1:
                this.storageProviderTypes = (List<String>)value;
                break;

            case 2:
                this.additionalInfo = (List<Map<String, String>>)value;
                break;

            case 3:
                this.info = (Map<String, String>)value;
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

        ShowDatasourceResponse that = (ShowDatasourceResponse)obj;

        return ( this.datasourceNames.equals( that.datasourceNames )
                 && this.storageProviderTypes.equals( that.storageProviderTypes )
                 && this.additionalInfo.equals( that.additionalInfo )
                 && this.info.equals( that.info ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "datasourceNames" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.datasourceNames ) );
        builder.append( ", " );
        builder.append( gd.toString( "storageProviderTypes" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.storageProviderTypes ) );
        builder.append( ", " );
        builder.append( gd.toString( "additionalInfo" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.additionalInfo ) );
        builder.append( ", " );
        builder.append( gd.toString( "info" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.info ) );
        builder.append( "}" );

        return builder.toString();
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = (31 * hashCode) + this.datasourceNames.hashCode();
        hashCode = (31 * hashCode) + this.storageProviderTypes.hashCode();
        hashCode = (31 * hashCode) + this.additionalInfo.hashCode();
        hashCode = (31 * hashCode) + this.info.hashCode();
        return hashCode;
    }

}
