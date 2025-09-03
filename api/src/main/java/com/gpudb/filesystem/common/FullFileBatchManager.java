package com.gpudb.filesystem.common;

import com.gpudb.filesystem.GPUdbFileHandler;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This is an internal class and not meant to be used by the end users of the
 * {@code filesystem} API. The consequences of using this class directly in
 * client code is not guaranteed and maybe undesirable.

 * This class manages the task of creating batches of files where the files
 * are candidates for one-shot uploads.
 */
public class FullFileBatchManager {

    private final GPUdbFileHandler.Options fileHandlerOptions;
    /**
     * This variable is used to maintain a map of file names in the local
     * file system to the pair of values of target file name on the KIFS and
     * the file size on the local file system. This is used by the method
     * {@link FileOperation#sortFilesIntoFullAndMultipartLists(List, List)}. This method,
     * after detecting a file to be a candidate for one shot upload will
     * insert an entry into this map. This map is further split up into a
     * list of maps and stored in the variable {@link #listOfFullFileNameToRemoteFileNameMap}
     * by the method {@link #createBatches()}.
     *
     * The purpose of splitting the map into the list of maps is to arrive at
     * batches of full file uploads so that each batch has a cumulative
     * size whose limit is given by the method getFileSizeToSplit() of the class
     * GPUdbFileHandler.Options.
     */
    protected Map<String, Pair<String, Long>> fullFileNameToSizeMap;


    /**
     * This variable is used to store the maps in a list so that each map has
     * files whose sizes add up to a maximum of the value returned by the method
     * getFileSizeToSplit() of the class GPUdbFileHandler.Options.
     * Each entry in this list is a map of local file name to its corresponding
     * KIFS file name.
     * 
     * This is used by the method 'FileUploader#uploadFullFiles()'
     */
    protected List<Map<String, String>> listOfFullFileNameToRemoteFileNameMap;


    FullFileBatchManager(GPUdbFileHandler.Options fileHandlerOptions) {
        this.fullFileNameToSizeMap = new LinkedHashMap<>();
        this.listOfFullFileNameToRemoteFileNameMap = new ArrayList<>();
        this.fileHandlerOptions = fileHandlerOptions;
    }


    /**
     * Resets the internal data structures maintained for the batches
     */
    public void clearBatches() {
        this.fullFileNameToSizeMap.clear();
        this.listOfFullFileNameToRemoteFileNameMap.clear();
    }


    /**
     * Creates batches of files which could be uploaded one shot.
     */
    public void createBatches() {

        if( this.fullFileNameToSizeMap.size() == 0 )
            return;

        int sum = 0;
        Map<String, String> bucket = new LinkedHashMap<>();

        List<String> keySet = new ArrayList<>( this.fullFileNameToSizeMap.keySet() );

        for( int i=0, s = this.fullFileNameToSizeMap.size(); i < s; i++ ) {
            // Key to the map is the local file name
            String fileName = keySet.get( i );

            // Get the remote file name which is the first value in the Pair
            String remoteFileName = this.fullFileNameToSizeMap.get( fileName ).getKey();

            // Get the file size which is the second value in the Pair
            Long size = this.fullFileNameToSizeMap.get( fileName ).getValue();

            bucket.put( fileName, remoteFileName );
            sum += size;

            if( sum > this.fileHandlerOptions.getFileSizeToSplit() ) {
                // This must go into the next bucket as it overshoots the
                // partitionSum value
                bucket.remove( fileName );

                // Re-adjust the index so that current value is re-read in the
                // next iteration
                i--;
            }

            if( sum >= this.fileHandlerOptions.getFileSizeToSplit() ) {
                this.listOfFullFileNameToRemoteFileNameMap.add( new LinkedHashMap<>(bucket) );
                bucket.clear();
                sum = 0;
            }

        }

        // If after the last iteration of the loop the bucket has any leftovers
        // add them to the list of partitions. This would be always less than
        // the 'partitionSum'.
        if( !bucket.isEmpty() ) {
            this.listOfFullFileNameToRemoteFileNameMap.add(new LinkedHashMap<>(bucket));
        }

    }


    /**
     * Add the details for a file to be uploaded one shot.
     * 
     * @param localFileName  Name of the file on the local file system.
     * @param remoteFileName  Name of the file on KIFS.
     * @param size  The size of the file, needed to partition the batches on.
     */
    public void addFile( String localFileName, String remoteFileName, Long size ) {
        this.fullFileNameToSizeMap.put( localFileName, Pair.of( remoteFileName, size) );
    }


    /**
     * Gets the number of batches of files which could be uploaded one shot.
     * 
     * @return  The number of batches
     */
    public int getNumberOfBatches() {
        return this.listOfFullFileNameToRemoteFileNameMap.size();
    }


    /**
     * Gets a particular batch of files which could be uploaded one shot using
     * a single call to the endpoint '/upload/files'.
     * 
     * @param n  The index of the batch to retrieve.
     * @return  The Nth batch; this batch is a set of entries in a map where
     *        each entry has the local file name as the key and the name of the
     *        file on KIFS as the value.
     */
    public Map<String, String> getNthBatch( int n ) {
        if( n >= 0 && n < this.listOfFullFileNameToRemoteFileNameMap.size() )
            return this.listOfFullFileNameToRemoteFileNameMap.get( n );
        
        return null;
    }

}
