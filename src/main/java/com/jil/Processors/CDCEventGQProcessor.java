package com.jil.Processors;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.jil.BigqueryClient.BigQueryJSONLoader;
import com.jil.config.Config;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CDCEventGQProcessor implements Consumer<Map<String, Object>> {

    private final ObjectMapper mapper = new ObjectMapper();
    private final GoogleCredentials  credentials;
    private final Config config;

    private ExecutorService workerThreadPool;

    public CDCEventGQProcessor(GoogleCredentials credentials, Config config, ExecutorService workerThreadPool) {
        this.credentials = credentials;
        this.config = config;
        this.workerThreadPool = workerThreadPool;
    }
    @Override
    public void accept(Map<String, Object> event) {
        workerThreadPool.submit(() -> {
                    try {
                        String jsonStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(event);
                        log.info("Change Data Capture event fired. Data:\n" + jsonStr);
                        ObjectMapper objectMapper = new ObjectMapper();

                        JsonNode rootNode = objectMapper.readTree(jsonStr);
                        JsonNode payloadNode = rootNode.path("payload");
                        JsonNode changeEventHeaderNode = payloadNode.path("ChangeEventHeader");
                        String entityName = changeEventHeaderNode.path("entityName").asText();

                        if (changeEventHeaderNode.isMissingNode()) {
                            log.debug("JSON string does not contain a 'ChangeEventHeader' property.");
                            return;
                        }
                        if (entityName.isEmpty()) {
                            log.debug("JSON string does not contain an 'entityName' property.");
                            return;
                        }
                        String jsonPayloadStr = objectMapper.writeValueAsString(payloadNode);
                        log.info("payload json: " + jsonPayloadStr);

                        String bqTableName = entityName.toLowerCase();
                        String blobName = bqTableName + ".json";

                        if (BigQueryJSONLoader.loadGCSFromJSON(credentials,
                                config.getGcsBuckName(), //"jianliu888-hometest",
                                blobName,
                                jsonPayloadStr)) {
                            BigQueryJSONLoader.loadBQFromGCS(credentials,
                                    config.getGcsBuckName(), //"jianliu888-hometest",
                                    blobName,
                                    config.getBigQueryDatasetName(), //"jianliuhometest",
                                    bqTableName);
                        }

                    } catch (RuntimeException e) {
                        // handle RuntimeException
                        log.error("RuntimeException occurred during task execution", e);
                    } catch (JsonProcessingException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }
}
