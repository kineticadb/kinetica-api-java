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
 * com.gpudb.GPUdb#createDatasink(CreateDatasinkRequest) GPUdb.createDatasink}.
 * <p>
 * Creates a <a href="../../../../../../concepts/data_sinks/"
 * target="_top">data sink</a>, which contains the destination information for
 * a data sink that is external to the database.
 */
public class CreateDatasinkRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("CreateDatasinkRequest")
            .namespace("com.gpudb")
            .fields()
                .name("name").type().stringType().noDefault()
                .name("destination").type().stringType().noDefault()
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
     * A set of string constants for the {@link CreateDatasinkRequest}
     * parameter {@link #getOptions() options}.
     * <p>
     * Optional parameters.
     */
    public static final class Options {
        /**
         * Timeout in seconds for connecting to this data sink
         */
        public static final String CONNECTION_TIMEOUT = "connection_timeout";

        /**
         * Timeout in seconds for waiting for a response from this data sink
         */
        public static final String WAIT_TIMEOUT = "wait_timeout";

        /**
         * Name of the <a href="../../../../../../concepts/credentials/"
         * target="_top">credential</a> object to be used in this data sink
         */
        public static final String CREDENTIAL = "credential";

        /**
         * Name of the Amazon S3 bucket to use as the data sink
         */
        public static final String S3_BUCKET_NAME = "s3_bucket_name";

        /**
         * Name of the Amazon S3 region where the given bucket is located
         */
        public static final String S3_REGION = "s3_region";

        /**
         * When true (default), the requests URI should be specified in
         * virtual-hosted-style format where the bucket name is part of the
         * domain name in the URL.
         * <p>
         * Otherwise set to false to use path-style URI for requests.
         * Supported values:
         * <ul>
         *     <li>{@link Options#TRUE TRUE}
         *     <li>{@link Options#FALSE FALSE}
         * </ul>
         * The default value is {@link Options#TRUE TRUE}.
         */
        public static final String S3_USE_VIRTUAL_ADDRESSING = "s3_use_virtual_addressing";

        public static final String TRUE = "true";
        public static final String FALSE = "false";

        /**
         * Amazon IAM Role ARN which has required S3 permissions that can be
         * assumed for the given S3 IAM user
         */
        public static final String S3_AWS_ROLE_ARN = "s3_aws_role_arn";

        /**
         * Customer encryption algorithm used encrypting data
         */
        public static final String S3_ENCRYPTION_CUSTOMER_ALGORITHM = "s3_encryption_customer_algorithm";

        /**
         * Customer encryption key to encrypt or decrypt data
         */
        public static final String S3_ENCRYPTION_CUSTOMER_KEY = "s3_encryption_customer_key";

        /**
         * Kerberos keytab file location for the given HDFS user.  This may be
         * a KIFS file.
         */
        public static final String HDFS_KERBEROS_KEYTAB = "hdfs_kerberos_keytab";

        /**
         * Delegation token for the given HDFS user
         */
        public static final String HDFS_DELEGATION_TOKEN = "hdfs_delegation_token";

        /**
         * Use kerberos authentication for the given HDFS cluster.
         * Supported values:
         * <ul>
         *     <li>{@link Options#TRUE TRUE}
         *     <li>{@link Options#FALSE FALSE}
         * </ul>
         * The default value is {@link Options#FALSE FALSE}.
         */
        public static final String HDFS_USE_KERBEROS = "hdfs_use_kerberos";

        /**
         * Name of the Azure storage account to use as the data sink, this is
         * valid only if tenant_id is specified
         */
        public static final String AZURE_STORAGE_ACCOUNT_NAME = "azure_storage_account_name";

        /**
         * Name of the Azure storage container to use as the data sink
         */
        public static final String AZURE_CONTAINER_NAME = "azure_container_name";

        /**
         * Active Directory tenant ID (or directory ID)
         */
        public static final String AZURE_TENANT_ID = "azure_tenant_id";

        /**
         * Shared access signature token for Azure storage account to use as
         * the data sink
         */
        public static final String AZURE_SAS_TOKEN = "azure_sas_token";

        /**
         * Oauth token to access given storage container
         */
        public static final String AZURE_OAUTH_TOKEN = "azure_oauth_token";

        /**
         * Name of the Google Cloud Storage bucket to use as the data sink
         */
        public static final String GCS_BUCKET_NAME = "gcs_bucket_name";

        /**
         * Name of the Google Cloud project to use as the data sink
         */
        public static final String GCS_PROJECT_ID = "gcs_project_id";

        /**
         * Google Cloud service account keys to use for authenticating the data
         * sink
         */
        public static final String GCS_SERVICE_ACCOUNT_KEYS = "gcs_service_account_keys";

        /**
         * JDBC driver jar file location
         */
        public static final String JDBC_DRIVER_JAR_PATH = "jdbc_driver_jar_path";

        /**
         * Name of the JDBC driver class
         */
        public static final String JDBC_DRIVER_CLASS_NAME = "jdbc_driver_class_name";

        /**
         * Name of the Kafka topic to publish to if {@link #getDestination()
         * destination} is a Kafka broker
         */
        public static final String KAFKA_TOPIC_NAME = "kafka_topic_name";

        /**
         * Maximum number of records per notification message. The default
         * value is '1'.
         */
        public static final String MAX_BATCH_SIZE = "max_batch_size";

        /**
         * Maximum size in bytes of each notification message. The default
         * value is '1000000'.
         */
        public static final String MAX_MESSAGE_SIZE = "max_message_size";

        /**
         * The desired format of JSON encoded notifications message.
         * <p>
         * If {@link Options#NESTED NESTED}, records are returned as an array.
         * Otherwise, only a single record per messages is returned.
         * Supported values:
         * <ul>
         *     <li>{@link Options#FLAT FLAT}
         *     <li>{@link Options#NESTED NESTED}
         * </ul>
         * The default value is {@link Options#FLAT FLAT}.
         */
        public static final String JSON_FORMAT = "json_format";

        public static final String FLAT = "flat";
        public static final String NESTED = "nested";

        /**
         * When no credentials are supplied, we use anonymous access by
         * default.  If this is set, we will use cloud provider user settings.
         * Supported values:
         * <ul>
         *     <li>{@link Options#TRUE TRUE}
         *     <li>{@link Options#FALSE FALSE}
         * </ul>
         * The default value is {@link Options#FALSE FALSE}.
         */
        public static final String USE_MANAGED_CREDENTIALS = "use_managed_credentials";

        /**
         * Use https to connect to datasink if true, otherwise use http.
         * Supported values:
         * <ul>
         *     <li>{@link Options#TRUE TRUE}
         *     <li>{@link Options#FALSE FALSE}
         * </ul>
         * The default value is {@link Options#TRUE TRUE}.
         */
        public static final String USE_HTTPS = "use_https";

        /**
         * Bypass validation of connection to this data sink.
         * Supported values:
         * <ul>
         *     <li>{@link Options#TRUE TRUE}
         *     <li>{@link Options#FALSE FALSE}
         * </ul>
         * The default value is {@link Options#FALSE FALSE}.
         */
        public static final String SKIP_VALIDATION = "skip_validation";

        private Options() {  }
    }

    private String name;
    private String destination;
    private Map<String, String> options;

    /**
     * Constructs a CreateDatasinkRequest object with default parameters.
     */
    public CreateDatasinkRequest() {
        name = "";
        destination = "";
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs a CreateDatasinkRequest object with the specified parameters.
     *
     * @param name  Name of the data sink to be created.
     * @param destination  Destination for the output data in format
     *                     'storage_provider_type://path[:port]'.  Supported
     *                     storage provider types are 'azure', 'gcs', 'hdfs',
     *                     'http', 'https', 'jdbc', 'kafka' and 's3'.
     * @param options  Optional parameters.
     *                 <ul>
     *                     <li>{@link Options#CONNECTION_TIMEOUT
     *                         CONNECTION_TIMEOUT}: Timeout in seconds for
     *                         connecting to this data sink
     *                     <li>{@link Options#WAIT_TIMEOUT WAIT_TIMEOUT}:
     *                         Timeout in seconds for waiting for a response
     *                         from this data sink
     *                     <li>{@link Options#CREDENTIAL CREDENTIAL}: Name of
     *                         the <a
     *                         href="../../../../../../concepts/credentials/"
     *                         target="_top">credential</a> object to be used
     *                         in this data sink
     *                     <li>{@link Options#S3_BUCKET_NAME S3_BUCKET_NAME}:
     *                         Name of the Amazon S3 bucket to use as the data
     *                         sink
     *                     <li>{@link Options#S3_REGION S3_REGION}: Name of the
     *                         Amazon S3 region where the given bucket is
     *                         located
     *                     <li>{@link Options#S3_USE_VIRTUAL_ADDRESSING
     *                         S3_USE_VIRTUAL_ADDRESSING}: When true (default),
     *                         the requests URI should be specified in
     *                         virtual-hosted-style format where the bucket
     *                         name is part of the domain name in the URL.
     *                         Otherwise set to false to use path-style URI for
     *                         requests.
     *                         Supported values:
     *                         <ul>
     *                             <li>{@link Options#TRUE TRUE}
     *                             <li>{@link Options#FALSE FALSE}
     *                         </ul>
     *                         The default value is {@link Options#TRUE TRUE}.
     *                     <li>{@link Options#S3_AWS_ROLE_ARN S3_AWS_ROLE_ARN}:
     *                         Amazon IAM Role ARN which has required S3
     *                         permissions that can be assumed for the given S3
     *                         IAM user
     *                     <li>{@link Options#S3_ENCRYPTION_CUSTOMER_ALGORITHM
     *                         S3_ENCRYPTION_CUSTOMER_ALGORITHM}: Customer
     *                         encryption algorithm used encrypting data
     *                     <li>{@link Options#S3_ENCRYPTION_CUSTOMER_KEY
     *                         S3_ENCRYPTION_CUSTOMER_KEY}: Customer encryption
     *                         key to encrypt or decrypt data
     *                     <li>{@link Options#HDFS_KERBEROS_KEYTAB
     *                         HDFS_KERBEROS_KEYTAB}: Kerberos keytab file
     *                         location for the given HDFS user.  This may be a
     *                         KIFS file.
     *                     <li>{@link Options#HDFS_DELEGATION_TOKEN
     *                         HDFS_DELEGATION_TOKEN}: Delegation token for the
     *                         given HDFS user
     *                     <li>{@link Options#HDFS_USE_KERBEROS
     *                         HDFS_USE_KERBEROS}: Use kerberos authentication
     *                         for the given HDFS cluster.
     *                         Supported values:
     *                         <ul>
     *                             <li>{@link Options#TRUE TRUE}
     *                             <li>{@link Options#FALSE FALSE}
     *                         </ul>
     *                         The default value is {@link Options#FALSE
     *                         FALSE}.
     *                     <li>{@link Options#AZURE_STORAGE_ACCOUNT_NAME
     *                         AZURE_STORAGE_ACCOUNT_NAME}: Name of the Azure
     *                         storage account to use as the data sink, this is
     *                         valid only if tenant_id is specified
     *                     <li>{@link Options#AZURE_CONTAINER_NAME
     *                         AZURE_CONTAINER_NAME}: Name of the Azure storage
     *                         container to use as the data sink
     *                     <li>{@link Options#AZURE_TENANT_ID AZURE_TENANT_ID}:
     *                         Active Directory tenant ID (or directory ID)
     *                     <li>{@link Options#AZURE_SAS_TOKEN AZURE_SAS_TOKEN}:
     *                         Shared access signature token for Azure storage
     *                         account to use as the data sink
     *                     <li>{@link Options#AZURE_OAUTH_TOKEN
     *                         AZURE_OAUTH_TOKEN}: Oauth token to access given
     *                         storage container
     *                     <li>{@link Options#GCS_BUCKET_NAME GCS_BUCKET_NAME}:
     *                         Name of the Google Cloud Storage bucket to use
     *                         as the data sink
     *                     <li>{@link Options#GCS_PROJECT_ID GCS_PROJECT_ID}:
     *                         Name of the Google Cloud project to use as the
     *                         data sink
     *                     <li>{@link Options#GCS_SERVICE_ACCOUNT_KEYS
     *                         GCS_SERVICE_ACCOUNT_KEYS}: Google Cloud service
     *                         account keys to use for authenticating the data
     *                         sink
     *                     <li>{@link Options#JDBC_DRIVER_JAR_PATH
     *                         JDBC_DRIVER_JAR_PATH}: JDBC driver jar file
     *                         location
     *                     <li>{@link Options#JDBC_DRIVER_CLASS_NAME
     *                         JDBC_DRIVER_CLASS_NAME}: Name of the JDBC driver
     *                         class
     *                     <li>{@link Options#KAFKA_TOPIC_NAME
     *                         KAFKA_TOPIC_NAME}: Name of the Kafka topic to
     *                         publish to if {@code destination} is a Kafka
     *                         broker
     *                     <li>{@link Options#MAX_BATCH_SIZE MAX_BATCH_SIZE}:
     *                         Maximum number of records per notification
     *                         message. The default value is '1'.
     *                     <li>{@link Options#MAX_MESSAGE_SIZE
     *                         MAX_MESSAGE_SIZE}: Maximum size in bytes of each
     *                         notification message. The default value is
     *                         '1000000'.
     *                     <li>{@link Options#JSON_FORMAT JSON_FORMAT}: The
     *                         desired format of JSON encoded notifications
     *                         message.   If {@link Options#NESTED NESTED},
     *                         records are returned as an array. Otherwise,
     *                         only a single record per messages is returned.
     *                         Supported values:
     *                         <ul>
     *                             <li>{@link Options#FLAT FLAT}
     *                             <li>{@link Options#NESTED NESTED}
     *                         </ul>
     *                         The default value is {@link Options#FLAT FLAT}.
     *                     <li>{@link Options#USE_MANAGED_CREDENTIALS
     *                         USE_MANAGED_CREDENTIALS}: When no credentials
     *                         are supplied, we use anonymous access by
     *                         default.  If this is set, we will use cloud
     *                         provider user settings.
     *                         Supported values:
     *                         <ul>
     *                             <li>{@link Options#TRUE TRUE}
     *                             <li>{@link Options#FALSE FALSE}
     *                         </ul>
     *                         The default value is {@link Options#FALSE
     *                         FALSE}.
     *                     <li>{@link Options#USE_HTTPS USE_HTTPS}: Use https
     *                         to connect to datasink if true, otherwise use
     *                         http.
     *                         Supported values:
     *                         <ul>
     *                             <li>{@link Options#TRUE TRUE}
     *                             <li>{@link Options#FALSE FALSE}
     *                         </ul>
     *                         The default value is {@link Options#TRUE TRUE}.
     *                     <li>{@link Options#SKIP_VALIDATION SKIP_VALIDATION}:
     *                         Bypass validation of connection to this data
     *                         sink.
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
    public CreateDatasinkRequest(String name, String destination, Map<String, String> options) {
        this.name = (name == null) ? "" : name;
        this.destination = (destination == null) ? "" : destination;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * Name of the data sink to be created.
     *
     * @return The current value of {@code name}.
     */
    public String getName() {
        return name;
    }

    /**
     * Name of the data sink to be created.
     *
     * @param name  The new value for {@code name}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public CreateDatasinkRequest setName(String name) {
        this.name = (name == null) ? "" : name;
        return this;
    }

    /**
     * Destination for the output data in format
     * 'storage_provider_type://path[:port]'.
     * <p>
     * Supported storage provider types are 'azure', 'gcs', 'hdfs', 'http',
     * 'https', 'jdbc', 'kafka' and 's3'.
     *
     * @return The current value of {@code destination}.
     */
    public String getDestination() {
        return destination;
    }

    /**
     * Destination for the output data in format
     * 'storage_provider_type://path[:port]'.
     * <p>
     * Supported storage provider types are 'azure', 'gcs', 'hdfs', 'http',
     * 'https', 'jdbc', 'kafka' and 's3'.
     *
     * @param destination  The new value for {@code destination}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public CreateDatasinkRequest setDestination(String destination) {
        this.destination = (destination == null) ? "" : destination;
        return this;
    }

    /**
     * Optional parameters.
     * <ul>
     *     <li>{@link Options#CONNECTION_TIMEOUT CONNECTION_TIMEOUT}: Timeout
     *         in seconds for connecting to this data sink
     *     <li>{@link Options#WAIT_TIMEOUT WAIT_TIMEOUT}: Timeout in seconds
     *         for waiting for a response from this data sink
     *     <li>{@link Options#CREDENTIAL CREDENTIAL}: Name of the <a
     *         href="../../../../../../concepts/credentials/"
     *         target="_top">credential</a> object to be used in this data sink
     *     <li>{@link Options#S3_BUCKET_NAME S3_BUCKET_NAME}: Name of the
     *         Amazon S3 bucket to use as the data sink
     *     <li>{@link Options#S3_REGION S3_REGION}: Name of the Amazon S3
     *         region where the given bucket is located
     *     <li>{@link Options#S3_USE_VIRTUAL_ADDRESSING
     *         S3_USE_VIRTUAL_ADDRESSING}: When true (default), the requests
     *         URI should be specified in virtual-hosted-style format where the
     *         bucket name is part of the domain name in the URL.   Otherwise
     *         set to false to use path-style URI for requests.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#TRUE TRUE}.
     *     <li>{@link Options#S3_AWS_ROLE_ARN S3_AWS_ROLE_ARN}: Amazon IAM Role
     *         ARN which has required S3 permissions that can be assumed for
     *         the given S3 IAM user
     *     <li>{@link Options#S3_ENCRYPTION_CUSTOMER_ALGORITHM
     *         S3_ENCRYPTION_CUSTOMER_ALGORITHM}: Customer encryption algorithm
     *         used encrypting data
     *     <li>{@link Options#S3_ENCRYPTION_CUSTOMER_KEY
     *         S3_ENCRYPTION_CUSTOMER_KEY}: Customer encryption key to encrypt
     *         or decrypt data
     *     <li>{@link Options#HDFS_KERBEROS_KEYTAB HDFS_KERBEROS_KEYTAB}:
     *         Kerberos keytab file location for the given HDFS user.  This may
     *         be a KIFS file.
     *     <li>{@link Options#HDFS_DELEGATION_TOKEN HDFS_DELEGATION_TOKEN}:
     *         Delegation token for the given HDFS user
     *     <li>{@link Options#HDFS_USE_KERBEROS HDFS_USE_KERBEROS}: Use
     *         kerberos authentication for the given HDFS cluster.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#FALSE FALSE}.
     *     <li>{@link Options#AZURE_STORAGE_ACCOUNT_NAME
     *         AZURE_STORAGE_ACCOUNT_NAME}: Name of the Azure storage account
     *         to use as the data sink, this is valid only if tenant_id is
     *         specified
     *     <li>{@link Options#AZURE_CONTAINER_NAME AZURE_CONTAINER_NAME}: Name
     *         of the Azure storage container to use as the data sink
     *     <li>{@link Options#AZURE_TENANT_ID AZURE_TENANT_ID}: Active
     *         Directory tenant ID (or directory ID)
     *     <li>{@link Options#AZURE_SAS_TOKEN AZURE_SAS_TOKEN}: Shared access
     *         signature token for Azure storage account to use as the data
     *         sink
     *     <li>{@link Options#AZURE_OAUTH_TOKEN AZURE_OAUTH_TOKEN}: Oauth token
     *         to access given storage container
     *     <li>{@link Options#GCS_BUCKET_NAME GCS_BUCKET_NAME}: Name of the
     *         Google Cloud Storage bucket to use as the data sink
     *     <li>{@link Options#GCS_PROJECT_ID GCS_PROJECT_ID}: Name of the
     *         Google Cloud project to use as the data sink
     *     <li>{@link Options#GCS_SERVICE_ACCOUNT_KEYS
     *         GCS_SERVICE_ACCOUNT_KEYS}: Google Cloud service account keys to
     *         use for authenticating the data sink
     *     <li>{@link Options#JDBC_DRIVER_JAR_PATH JDBC_DRIVER_JAR_PATH}: JDBC
     *         driver jar file location
     *     <li>{@link Options#JDBC_DRIVER_CLASS_NAME JDBC_DRIVER_CLASS_NAME}:
     *         Name of the JDBC driver class
     *     <li>{@link Options#KAFKA_TOPIC_NAME KAFKA_TOPIC_NAME}: Name of the
     *         Kafka topic to publish to if {@link #getDestination()
     *         destination} is a Kafka broker
     *     <li>{@link Options#MAX_BATCH_SIZE MAX_BATCH_SIZE}: Maximum number of
     *         records per notification message. The default value is '1'.
     *     <li>{@link Options#MAX_MESSAGE_SIZE MAX_MESSAGE_SIZE}: Maximum size
     *         in bytes of each notification message. The default value is
     *         '1000000'.
     *     <li>{@link Options#JSON_FORMAT JSON_FORMAT}: The desired format of
     *         JSON encoded notifications message.   If {@link Options#NESTED
     *         NESTED}, records are returned as an array. Otherwise, only a
     *         single record per messages is returned.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#FLAT FLAT}
     *             <li>{@link Options#NESTED NESTED}
     *         </ul>
     *         The default value is {@link Options#FLAT FLAT}.
     *     <li>{@link Options#USE_MANAGED_CREDENTIALS USE_MANAGED_CREDENTIALS}:
     *         When no credentials are supplied, we use anonymous access by
     *         default.  If this is set, we will use cloud provider user
     *         settings.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#FALSE FALSE}.
     *     <li>{@link Options#USE_HTTPS USE_HTTPS}: Use https to connect to
     *         datasink if true, otherwise use http.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#TRUE TRUE}.
     *     <li>{@link Options#SKIP_VALIDATION SKIP_VALIDATION}: Bypass
     *         validation of connection to this data sink.
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
     *     <li>{@link Options#CONNECTION_TIMEOUT CONNECTION_TIMEOUT}: Timeout
     *         in seconds for connecting to this data sink
     *     <li>{@link Options#WAIT_TIMEOUT WAIT_TIMEOUT}: Timeout in seconds
     *         for waiting for a response from this data sink
     *     <li>{@link Options#CREDENTIAL CREDENTIAL}: Name of the <a
     *         href="../../../../../../concepts/credentials/"
     *         target="_top">credential</a> object to be used in this data sink
     *     <li>{@link Options#S3_BUCKET_NAME S3_BUCKET_NAME}: Name of the
     *         Amazon S3 bucket to use as the data sink
     *     <li>{@link Options#S3_REGION S3_REGION}: Name of the Amazon S3
     *         region where the given bucket is located
     *     <li>{@link Options#S3_USE_VIRTUAL_ADDRESSING
     *         S3_USE_VIRTUAL_ADDRESSING}: When true (default), the requests
     *         URI should be specified in virtual-hosted-style format where the
     *         bucket name is part of the domain name in the URL.   Otherwise
     *         set to false to use path-style URI for requests.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#TRUE TRUE}.
     *     <li>{@link Options#S3_AWS_ROLE_ARN S3_AWS_ROLE_ARN}: Amazon IAM Role
     *         ARN which has required S3 permissions that can be assumed for
     *         the given S3 IAM user
     *     <li>{@link Options#S3_ENCRYPTION_CUSTOMER_ALGORITHM
     *         S3_ENCRYPTION_CUSTOMER_ALGORITHM}: Customer encryption algorithm
     *         used encrypting data
     *     <li>{@link Options#S3_ENCRYPTION_CUSTOMER_KEY
     *         S3_ENCRYPTION_CUSTOMER_KEY}: Customer encryption key to encrypt
     *         or decrypt data
     *     <li>{@link Options#HDFS_KERBEROS_KEYTAB HDFS_KERBEROS_KEYTAB}:
     *         Kerberos keytab file location for the given HDFS user.  This may
     *         be a KIFS file.
     *     <li>{@link Options#HDFS_DELEGATION_TOKEN HDFS_DELEGATION_TOKEN}:
     *         Delegation token for the given HDFS user
     *     <li>{@link Options#HDFS_USE_KERBEROS HDFS_USE_KERBEROS}: Use
     *         kerberos authentication for the given HDFS cluster.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#FALSE FALSE}.
     *     <li>{@link Options#AZURE_STORAGE_ACCOUNT_NAME
     *         AZURE_STORAGE_ACCOUNT_NAME}: Name of the Azure storage account
     *         to use as the data sink, this is valid only if tenant_id is
     *         specified
     *     <li>{@link Options#AZURE_CONTAINER_NAME AZURE_CONTAINER_NAME}: Name
     *         of the Azure storage container to use as the data sink
     *     <li>{@link Options#AZURE_TENANT_ID AZURE_TENANT_ID}: Active
     *         Directory tenant ID (or directory ID)
     *     <li>{@link Options#AZURE_SAS_TOKEN AZURE_SAS_TOKEN}: Shared access
     *         signature token for Azure storage account to use as the data
     *         sink
     *     <li>{@link Options#AZURE_OAUTH_TOKEN AZURE_OAUTH_TOKEN}: Oauth token
     *         to access given storage container
     *     <li>{@link Options#GCS_BUCKET_NAME GCS_BUCKET_NAME}: Name of the
     *         Google Cloud Storage bucket to use as the data sink
     *     <li>{@link Options#GCS_PROJECT_ID GCS_PROJECT_ID}: Name of the
     *         Google Cloud project to use as the data sink
     *     <li>{@link Options#GCS_SERVICE_ACCOUNT_KEYS
     *         GCS_SERVICE_ACCOUNT_KEYS}: Google Cloud service account keys to
     *         use for authenticating the data sink
     *     <li>{@link Options#JDBC_DRIVER_JAR_PATH JDBC_DRIVER_JAR_PATH}: JDBC
     *         driver jar file location
     *     <li>{@link Options#JDBC_DRIVER_CLASS_NAME JDBC_DRIVER_CLASS_NAME}:
     *         Name of the JDBC driver class
     *     <li>{@link Options#KAFKA_TOPIC_NAME KAFKA_TOPIC_NAME}: Name of the
     *         Kafka topic to publish to if {@link #getDestination()
     *         destination} is a Kafka broker
     *     <li>{@link Options#MAX_BATCH_SIZE MAX_BATCH_SIZE}: Maximum number of
     *         records per notification message. The default value is '1'.
     *     <li>{@link Options#MAX_MESSAGE_SIZE MAX_MESSAGE_SIZE}: Maximum size
     *         in bytes of each notification message. The default value is
     *         '1000000'.
     *     <li>{@link Options#JSON_FORMAT JSON_FORMAT}: The desired format of
     *         JSON encoded notifications message.   If {@link Options#NESTED
     *         NESTED}, records are returned as an array. Otherwise, only a
     *         single record per messages is returned.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#FLAT FLAT}
     *             <li>{@link Options#NESTED NESTED}
     *         </ul>
     *         The default value is {@link Options#FLAT FLAT}.
     *     <li>{@link Options#USE_MANAGED_CREDENTIALS USE_MANAGED_CREDENTIALS}:
     *         When no credentials are supplied, we use anonymous access by
     *         default.  If this is set, we will use cloud provider user
     *         settings.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#FALSE FALSE}.
     *     <li>{@link Options#USE_HTTPS USE_HTTPS}: Use https to connect to
     *         datasink if true, otherwise use http.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#TRUE TRUE}.
     *     <li>{@link Options#SKIP_VALIDATION SKIP_VALIDATION}: Bypass
     *         validation of connection to this data sink.
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
    public CreateDatasinkRequest setOptions(Map<String, String> options) {
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
                return this.destination;

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
                this.name = (String)value;
                break;

            case 1:
                this.destination = (String)value;
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

        CreateDatasinkRequest that = (CreateDatasinkRequest)obj;

        return ( this.name.equals( that.name )
                 && this.destination.equals( that.destination )
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
        builder.append( gd.toString( "destination" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.destination ) );
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
        hashCode = (31 * hashCode) + this.destination.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }
}