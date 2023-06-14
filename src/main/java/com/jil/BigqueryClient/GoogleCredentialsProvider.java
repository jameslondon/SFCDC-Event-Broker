package com.jil.BigqueryClient;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.jil.config.Config;

import java.io.FileInputStream;
import java.io.IOException;


public class GoogleCredentialsProvider {

    private static GoogleCredentialsProvider instance = null;
    private GoogleCredentials credentials;
    private Config config = Config.get();

    private GoogleCredentialsProvider() {

        String googleKeyPath = config.getGoogleCredentialKeyPath();
        if (!(googleKeyPath == null || googleKeyPath.isEmpty())) {
            try {
                credentials = ServiceAccountCredentials.fromStream(new FileInputStream(googleKeyPath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                //user GOOGLE_APPLICTION_CREDENTIAL environment variable for key path as default
                credentials = ServiceAccountCredentials.getApplicationDefault();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static synchronized GoogleCredentialsProvider getInstance() {
        if (instance == null) {
            instance = new GoogleCredentialsProvider();
        }
        return instance;
    }

    public GoogleCredentials getCredentials() {
        return this.credentials;
    }
}
