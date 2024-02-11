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
 * com.gpudb.GPUdb#alterCredential(AlterCredentialRequest)
 * GPUdb.alterCredential}.
 * <p>
 * Alter the properties of an existing <a
 * href="../../../../../../concepts/credentials/" target="_top">credential</a>.
 */
public class AlterCredentialRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("AlterCredentialRequest")
            .namespace("com.gpudb")
            .fields()
                .name("credentialName").type().stringType().noDefault()
                .name("credentialUpdatesMap").type().map().values().stringType().noDefault()
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
     * A set of string constants for the {@link AlterCredentialRequest}
     * parameter {@link #getCredentialUpdatesMap() credentialUpdatesMap}.
     * <p>
     * Map containing the properties of the credential to be updated. Error if
     * empty.
     */
    public static final class CredentialUpdatesMap {
        /**
         * New type for the credential.
         * Supported values:
         * <ul>
         *     <li>{@link CredentialUpdatesMap#AWS_ACCESS_KEY AWS_ACCESS_KEY}
         *     <li>{@link CredentialUpdatesMap#AWS_IAM_ROLE AWS_IAM_ROLE}
         *     <li>{@link CredentialUpdatesMap#AZURE_AD AZURE_AD}
         *     <li>{@link CredentialUpdatesMap#AZURE_OAUTH AZURE_OAUTH}
         *     <li>{@link CredentialUpdatesMap#AZURE_SAS AZURE_SAS}
         *     <li>{@link CredentialUpdatesMap#AZURE_STORAGE_KEY
         *         AZURE_STORAGE_KEY}
         *     <li>{@link CredentialUpdatesMap#DOCKER DOCKER}
         *     <li>{@link CredentialUpdatesMap#GCS_SERVICE_ACCOUNT_ID
         *         GCS_SERVICE_ACCOUNT_ID}
         *     <li>{@link CredentialUpdatesMap#GCS_SERVICE_ACCOUNT_KEYS
         *         GCS_SERVICE_ACCOUNT_KEYS}
         *     <li>{@link CredentialUpdatesMap#HDFS HDFS}
         *     <li>{@link CredentialUpdatesMap#KAFKA KAFKA}
         * </ul>
         */
        public static final String TYPE = "type";

        public static final String AWS_ACCESS_KEY = "aws_access_key";
        public static final String AWS_IAM_ROLE = "aws_iam_role";
        public static final String AZURE_AD = "azure_ad";
        public static final String AZURE_OAUTH = "azure_oauth";
        public static final String AZURE_SAS = "azure_sas";
        public static final String AZURE_STORAGE_KEY = "azure_storage_key";
        public static final String DOCKER = "docker";
        public static final String GCS_SERVICE_ACCOUNT_ID = "gcs_service_account_id";
        public static final String GCS_SERVICE_ACCOUNT_KEYS = "gcs_service_account_keys";
        public static final String HDFS = "hdfs";
        public static final String KAFKA = "kafka";

        /**
         * New user for the credential
         */
        public static final String IDENTITY = "identity";

        /**
         * New password for the credential
         */
        public static final String SECRET = "secret";

        /**
         * Updates the schema name.  If {@link CredentialUpdatesMap#SCHEMA_NAME
         * SCHEMA_NAME} doesn't exist, an error will be thrown. If {@link
         * CredentialUpdatesMap#SCHEMA_NAME SCHEMA_NAME} is empty, then the
         * user's default schema will be used.
         */
        public static final String SCHEMA_NAME = "schema_name";

        private CredentialUpdatesMap() {  }
    }

    private String credentialName;
    private Map<String, String> credentialUpdatesMap;
    private Map<String, String> options;

    /**
     * Constructs an AlterCredentialRequest object with default parameters.
     */
    public AlterCredentialRequest() {
        credentialName = "";
        credentialUpdatesMap = new LinkedHashMap<>();
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs an AlterCredentialRequest object with the specified
     * parameters.
     *
     * @param credentialName  Name of the credential to be altered. Must be an
     *                        existing credential.
     * @param credentialUpdatesMap  Map containing the properties of the
     *                              credential to be updated. Error if empty.
     *                              <ul>
     *                                  <li>{@link CredentialUpdatesMap#TYPE
     *                                      TYPE}: New type for the credential.
     *                                      Supported values:
     *                                      <ul>
     *                                          <li>{@link
     *                                              CredentialUpdatesMap#AWS_ACCESS_KEY
     *                                              AWS_ACCESS_KEY}
     *                                          <li>{@link
     *                                              CredentialUpdatesMap#AWS_IAM_ROLE
     *                                              AWS_IAM_ROLE}
     *                                          <li>{@link
     *                                              CredentialUpdatesMap#AZURE_AD
     *                                              AZURE_AD}
     *                                          <li>{@link
     *                                              CredentialUpdatesMap#AZURE_OAUTH
     *                                              AZURE_OAUTH}
     *                                          <li>{@link
     *                                              CredentialUpdatesMap#AZURE_SAS
     *                                              AZURE_SAS}
     *                                          <li>{@link
     *                                              CredentialUpdatesMap#AZURE_STORAGE_KEY
     *                                              AZURE_STORAGE_KEY}
     *                                          <li>{@link
     *                                              CredentialUpdatesMap#DOCKER
     *                                              DOCKER}
     *                                          <li>{@link
     *                                              CredentialUpdatesMap#GCS_SERVICE_ACCOUNT_ID
     *                                              GCS_SERVICE_ACCOUNT_ID}
     *                                          <li>{@link
     *                                              CredentialUpdatesMap#GCS_SERVICE_ACCOUNT_KEYS
     *                                              GCS_SERVICE_ACCOUNT_KEYS}
     *                                          <li>{@link
     *                                              CredentialUpdatesMap#HDFS
     *                                              HDFS}
     *                                          <li>{@link
     *                                              CredentialUpdatesMap#KAFKA
     *                                              KAFKA}
     *                                      </ul>
     *                                  <li>{@link
     *                                      CredentialUpdatesMap#IDENTITY
     *                                      IDENTITY}: New user for the
     *                                      credential
     *                                  <li>{@link CredentialUpdatesMap#SECRET
     *                                      SECRET}: New password for the
     *                                      credential
     *                                  <li>{@link
     *                                      CredentialUpdatesMap#SCHEMA_NAME
     *                                      SCHEMA_NAME}: Updates the schema
     *                                      name.  If {@link
     *                                      CredentialUpdatesMap#SCHEMA_NAME
     *                                      SCHEMA_NAME} doesn't exist, an
     *                                      error will be thrown. If {@link
     *                                      CredentialUpdatesMap#SCHEMA_NAME
     *                                      SCHEMA_NAME} is empty, then the
     *                                      user's default schema will be used.
     *                              </ul>
     * @param options  Optional parameters.
     */
    public AlterCredentialRequest(String credentialName, Map<String, String> credentialUpdatesMap, Map<String, String> options) {
        this.credentialName = (credentialName == null) ? "" : credentialName;
        this.credentialUpdatesMap = (credentialUpdatesMap == null) ? new LinkedHashMap<String, String>() : credentialUpdatesMap;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * Name of the credential to be altered. Must be an existing credential.
     *
     * @return The current value of {@code credentialName}.
     */
    public String getCredentialName() {
        return credentialName;
    }

    /**
     * Name of the credential to be altered. Must be an existing credential.
     *
     * @param credentialName  The new value for {@code credentialName}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AlterCredentialRequest setCredentialName(String credentialName) {
        this.credentialName = (credentialName == null) ? "" : credentialName;
        return this;
    }

    /**
     * Map containing the properties of the credential to be updated. Error if
     * empty.
     * <ul>
     *     <li>{@link CredentialUpdatesMap#TYPE TYPE}: New type for the
     *         credential.
     *         Supported values:
     *         <ul>
     *             <li>{@link CredentialUpdatesMap#AWS_ACCESS_KEY
     *                 AWS_ACCESS_KEY}
     *             <li>{@link CredentialUpdatesMap#AWS_IAM_ROLE AWS_IAM_ROLE}
     *             <li>{@link CredentialUpdatesMap#AZURE_AD AZURE_AD}
     *             <li>{@link CredentialUpdatesMap#AZURE_OAUTH AZURE_OAUTH}
     *             <li>{@link CredentialUpdatesMap#AZURE_SAS AZURE_SAS}
     *             <li>{@link CredentialUpdatesMap#AZURE_STORAGE_KEY
     *                 AZURE_STORAGE_KEY}
     *             <li>{@link CredentialUpdatesMap#DOCKER DOCKER}
     *             <li>{@link CredentialUpdatesMap#GCS_SERVICE_ACCOUNT_ID
     *                 GCS_SERVICE_ACCOUNT_ID}
     *             <li>{@link CredentialUpdatesMap#GCS_SERVICE_ACCOUNT_KEYS
     *                 GCS_SERVICE_ACCOUNT_KEYS}
     *             <li>{@link CredentialUpdatesMap#HDFS HDFS}
     *             <li>{@link CredentialUpdatesMap#KAFKA KAFKA}
     *         </ul>
     *     <li>{@link CredentialUpdatesMap#IDENTITY IDENTITY}: New user for the
     *         credential
     *     <li>{@link CredentialUpdatesMap#SECRET SECRET}: New password for the
     *         credential
     *     <li>{@link CredentialUpdatesMap#SCHEMA_NAME SCHEMA_NAME}: Updates
     *         the schema name.  If {@link CredentialUpdatesMap#SCHEMA_NAME
     *         SCHEMA_NAME} doesn't exist, an error will be thrown. If {@link
     *         CredentialUpdatesMap#SCHEMA_NAME SCHEMA_NAME} is empty, then the
     *         user's default schema will be used.
     * </ul>
     *
     * @return The current value of {@code credentialUpdatesMap}.
     */
    public Map<String, String> getCredentialUpdatesMap() {
        return credentialUpdatesMap;
    }

    /**
     * Map containing the properties of the credential to be updated. Error if
     * empty.
     * <ul>
     *     <li>{@link CredentialUpdatesMap#TYPE TYPE}: New type for the
     *         credential.
     *         Supported values:
     *         <ul>
     *             <li>{@link CredentialUpdatesMap#AWS_ACCESS_KEY
     *                 AWS_ACCESS_KEY}
     *             <li>{@link CredentialUpdatesMap#AWS_IAM_ROLE AWS_IAM_ROLE}
     *             <li>{@link CredentialUpdatesMap#AZURE_AD AZURE_AD}
     *             <li>{@link CredentialUpdatesMap#AZURE_OAUTH AZURE_OAUTH}
     *             <li>{@link CredentialUpdatesMap#AZURE_SAS AZURE_SAS}
     *             <li>{@link CredentialUpdatesMap#AZURE_STORAGE_KEY
     *                 AZURE_STORAGE_KEY}
     *             <li>{@link CredentialUpdatesMap#DOCKER DOCKER}
     *             <li>{@link CredentialUpdatesMap#GCS_SERVICE_ACCOUNT_ID
     *                 GCS_SERVICE_ACCOUNT_ID}
     *             <li>{@link CredentialUpdatesMap#GCS_SERVICE_ACCOUNT_KEYS
     *                 GCS_SERVICE_ACCOUNT_KEYS}
     *             <li>{@link CredentialUpdatesMap#HDFS HDFS}
     *             <li>{@link CredentialUpdatesMap#KAFKA KAFKA}
     *         </ul>
     *     <li>{@link CredentialUpdatesMap#IDENTITY IDENTITY}: New user for the
     *         credential
     *     <li>{@link CredentialUpdatesMap#SECRET SECRET}: New password for the
     *         credential
     *     <li>{@link CredentialUpdatesMap#SCHEMA_NAME SCHEMA_NAME}: Updates
     *         the schema name.  If {@link CredentialUpdatesMap#SCHEMA_NAME
     *         SCHEMA_NAME} doesn't exist, an error will be thrown. If {@link
     *         CredentialUpdatesMap#SCHEMA_NAME SCHEMA_NAME} is empty, then the
     *         user's default schema will be used.
     * </ul>
     *
     * @param credentialUpdatesMap  The new value for {@code
     *                              credentialUpdatesMap}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AlterCredentialRequest setCredentialUpdatesMap(Map<String, String> credentialUpdatesMap) {
        this.credentialUpdatesMap = (credentialUpdatesMap == null) ? new LinkedHashMap<String, String>() : credentialUpdatesMap;
        return this;
    }

    /**
     * Optional parameters.
     *
     * @return The current value of {@code options}.
     */
    public Map<String, String> getOptions() {
        return options;
    }

    /**
     * Optional parameters.
     *
     * @param options  The new value for {@code options}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AlterCredentialRequest setOptions(Map<String, String> options) {
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
                return this.credentialName;

            case 1:
                return this.credentialUpdatesMap;

            case 2:
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
                this.credentialName = (String)value;
                break;

            case 1:
                this.credentialUpdatesMap = (Map<String, String>)value;
                break;

            case 2:
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

        AlterCredentialRequest that = (AlterCredentialRequest)obj;

        return ( this.credentialName.equals( that.credentialName )
                 && this.credentialUpdatesMap.equals( that.credentialUpdatesMap )
                 && this.options.equals( that.options ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "credentialName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.credentialName ) );
        builder.append( ", " );
        builder.append( gd.toString( "credentialUpdatesMap" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.credentialUpdatesMap ) );
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
        hashCode = (31 * hashCode) + this.credentialName.hashCode();
        hashCode = (31 * hashCode) + this.credentialUpdatesMap.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }
}
