package com.jil.BigqueryClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.*;
import com.google.cloud.storage.*;
import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BigQueryJSONLoader {
    public static Boolean loadGCSFromJSON(GoogleCredentials credentials,
                                          String bucketName,
                                          String blobName,
                                          String jsonPayloadStr
                                          ) throws JsonProcessingException {
        if (credentials == null) {
            log.error("credentials is null");
            return false;
        }
        log.info("Start to load json response to GCS.");

        Storage storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();

        BlobId blobId = BlobId.of(bucketName, blobName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/json").build();

        try (WritableByteChannel channel = storage.writer(blobInfo)) {
            channel.write(java.nio.ByteBuffer.wrap(jsonPayloadStr.getBytes()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        log.info("JSON string successfully loaded into GCS.");
        return true;
    }

    public static void loadBQFromGCS(GoogleCredentials credentials,
                                     String bucketName,
                                     String blobName,
                                     String datasetName,
                                     String tableName) throws InterruptedException {
        BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();

        TableId tableId = TableId.of(datasetName, tableName);
        String sourceUri = "gs://" + bucketName + "/" + blobName;

        FormatOptions formatOptions = FormatOptions.json();
        LoadJobConfiguration loadJobConfig = LoadJobConfiguration.newBuilder(
                        tableId,
                        sourceUri,
                        formatOptions)
                .setSchemaUpdateOptions(
                  ImmutableList.of(JobInfo.SchemaUpdateOption.ALLOW_FIELD_ADDITION)) // Allow adding new fields
                .setWriteDisposition(JobInfo.WriteDisposition.WRITE_APPEND) // This appends to the table
                .setAutodetect(true)
                .build();
        try {
            Job loadJob = bigquery.create(JobInfo.newBuilder(loadJobConfig).build());
            loadJob = loadJob.waitFor();
            if (loadJob.isDone() && loadJob.getStatus().getError() == null) {
                log.info("JSON string successfully loaded into BigQuery table.");
            } else {
                log.error("Error occurred while loading JSON string into BigQuery table.");
            }
        } catch (JobException e) {
            log.error("JobException was thrown: " + e.getMessage());
        }

    }
}
