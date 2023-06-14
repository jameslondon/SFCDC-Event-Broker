
package com.jil;

import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.google.auth.oauth2.GoogleCredentials;
import com.jil.BigqueryClient.GoogleCredentialsProvider;
import com.jil.Processors.CDCEventGQProcessor;
import com.jil.SFconnector.nCinoEmpConnector;
import com.jil.SFconnector.TopicSubscription;
import com.jil.config.Config;
import com.jil.util.BayeuxParameters;
import com.jil.util.BayeuxParametersImpl;
import com.jil.util.nCinoAccess;
import io.netty.handler.timeout.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.sisu.launch.Main;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

@Slf4j
public class App {
    // More than one thread can be used in the thread pool which leads to parallel processing of events which may be acceptable by the application
    // The main purpose of asynchronous event processing is to make sure that client is able to perform /meta/connect requests which keeps the session alive on the server side
    private static final ExecutorService workerThreadPool = Executors.newFixedThreadPool(10);
    public static void main(String[] argv) throws Exception {

        //Config.get(argv[0]) must be called at with argv parameter (only once) at the top of the main program
        if (argv.length < 1) {
            log.error("You must provide configuration file (path/fileName) as a parameter");
            return;
        }
        Config config = Config.get(argv[0]);
        // shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            workerThreadPool.shutdown(); // Disable new tasks from being submitted
            try {
                // Wait a while for existing tasks to terminate
                if (!workerThreadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    workerThreadPool.shutdownNow(); // Cancel currently executing tasks
                    // Wait a while for tasks to respond to being cancelled
                    if (!workerThreadPool.awaitTermination(60, TimeUnit.SECONDS))
                        log.error("Pool did not terminate");
                }
            } catch (InterruptedException ie) {
                // (Re-)Cancel if current thread also interrupted
                workerThreadPool.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
        }));

        long replayFrom = config.getRelayFrom();

        BayeuxParameters params = new BayeuxParametersImpl(new nCinoAccess(), config);

        GoogleCredentials credentials = GoogleCredentialsProvider.getInstance().getCredentials();
        if (credentials == null) {
            log.error("Failed to get Google credentials.");
        }

        nCinoEmpConnector connector = new nCinoEmpConnector(params);
        if (connector == null) {
            log.error("connector == null");
        }

        connector.start().get(5, TimeUnit.SECONDS);

        Stream<String> eventStream = Arrays.stream(config.getSubscribedChangeEvents().split(","));

        Consumer<Map<String, Object>> consumer = new CDCEventGQProcessor(credentials, config, workerThreadPool);

        eventStream.forEach(topic -> {
                    try {
                        String topicServiceUri = "/data/" + topic.trim();
                        TopicSubscription subscription = connector.subscribe(topicServiceUri, replayFrom, consumer).get(5, TimeUnit.SECONDS);
                        log.debug(String.format("Subscribed: %s", subscription));
                    } catch (InterruptedException | ExecutionException | TimeoutException |
                             java.util.concurrent.TimeoutException e) {
                        e.printStackTrace();
                    }
                }
                );
    }
}
