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
 * A set of results returned by {@link
 * com.gpudb.GPUdb#adminAlterJobs(AdminAlterJobsRequest) GPUdb.adminAlterJobs}.
 */
public class AdminAlterJobsResponse implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("AdminAlterJobsResponse")
            .namespace("com.gpudb")
            .fields()
                .name("jobIds").type().array().items().longType().noDefault()
                .name("action").type().stringType().noDefault()
                .name("status").type().array().items().stringType().noDefault()
                .name("info").type().map().values().stringType().noDefault()
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

    private List<Long> jobIds;
    private String action;
    private List<String> status;
    private Map<String, String> info;

    /**
     * Constructs an AdminAlterJobsResponse object with default parameters.
     */
    public AdminAlterJobsResponse() {
    }

    /**
     * Jobs on which the action was performed.
     *
     * @return The current value of {@code jobIds}.
     */
    public List<Long> getJobIds() {
        return jobIds;
    }

    /**
     * Jobs on which the action was performed.
     *
     * @param jobIds  The new value for {@code jobIds}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AdminAlterJobsResponse setJobIds(List<Long> jobIds) {
        this.jobIds = (jobIds == null) ? new ArrayList<Long>() : jobIds;
        return this;
    }

    /**
     * Action requested on the jobs.
     *
     * @return The current value of {@code action}.
     */
    public String getAction() {
        return action;
    }

    /**
     * Action requested on the jobs.
     *
     * @param action  The new value for {@code action}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AdminAlterJobsResponse setAction(String action) {
        this.action = (action == null) ? "" : action;
        return this;
    }

    /**
     * Status of the requested action for each job.
     *
     * @return The current value of {@code status}.
     */
    public List<String> getStatus() {
        return status;
    }

    /**
     * Status of the requested action for each job.
     *
     * @param status  The new value for {@code status}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AdminAlterJobsResponse setStatus(List<String> status) {
        this.status = (status == null) ? new ArrayList<String>() : status;
        return this;
    }

    /**
     * Additional information.
     *
     * @return The current value of {@code info}.
     */
    public Map<String, String> getInfo() {
        return info;
    }

    /**
     * Additional information.
     *
     * @param info  The new value for {@code info}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AdminAlterJobsResponse setInfo(Map<String, String> info) {
        this.info = (info == null) ? new LinkedHashMap<String, String>() : info;
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
                return this.jobIds;

            case 1:
                return this.action;

            case 2:
                return this.status;

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
     */
    @Override
    @SuppressWarnings("unchecked")
    public void put(int index, Object value) {
        switch (index) {
            case 0:
                this.jobIds = (List<Long>)value;
                break;

            case 1:
                this.action = (String)value;
                break;

            case 2:
                this.status = (List<String>)value;
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

        AdminAlterJobsResponse that = (AdminAlterJobsResponse)obj;

        return ( this.jobIds.equals( that.jobIds )
                 && this.action.equals( that.action )
                 && this.status.equals( that.status )
                 && this.info.equals( that.info ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "jobIds" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.jobIds ) );
        builder.append( ", " );
        builder.append( gd.toString( "action" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.action ) );
        builder.append( ", " );
        builder.append( gd.toString( "status" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.status ) );
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
        hashCode = (31 * hashCode) + this.jobIds.hashCode();
        hashCode = (31 * hashCode) + this.action.hashCode();
        hashCode = (31 * hashCode) + this.status.hashCode();
        hashCode = (31 * hashCode) + this.info.hashCode();
        return hashCode;
    }
}
