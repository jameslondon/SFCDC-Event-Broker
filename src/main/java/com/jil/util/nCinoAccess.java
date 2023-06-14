package com.jil.util;

import com.jil.config.Config;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
public class nCinoAccess {
    static Config config = Config.get();

    public static Supplier<Map<String, String>> getAccessTokenSupplier() {
        return () -> {
            try {
                Map<String, String> tokenResp = callTokeEndpoint(generateJwt());
                //log.debug("tokenResp", tokenResp);
                return tokenResp;
            } catch (Exception e) {
                log.error("Error performing access", e);
            }
            return null;
        };
    }

    private static String generateJwt() throws Exception {
        PrivateKey privateKey;

        //for testing purpose, user may use nCino Private Key directly (which is not secure)
        String nCinoPrivateKeyPath = config.getnCinoPrivateKeyPath();
        if (!(nCinoPrivateKeyPath == null || nCinoPrivateKeyPath.isEmpty())) {
            byte[] privateKeyBytes = Files.readAllBytes(Paths.get(nCinoPrivateKeyPath));
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            privateKey = keyFactory.generatePrivate(keySpec);
        } else {
           // Load key from keystore
            String keyStorePassword = System.getenv("KEYSTORE_PASSWORD");
            String keyPassword = System.getenv("KEY_PASSWORD");
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream keystoreStream = new FileInputStream(config.getKeystorePath())) {
                keystore.load(keystoreStream, keyPassword.toCharArray());
            }
            // Retrieve the private key
            Key key = keystore.getKey(config.getKeystoreAlias(), keyStorePassword.toCharArray());
            if (!(key instanceof PrivateKey)) {
                throw new KeyStoreException("Loaded key is not a private key");
            }
            privateKey = (PrivateKey) key;
        }

        JwtBuilder jwtBuilder = Jwts.builder()
                .setIssuer(config.getClientId())
                .setSubject(config.getUsername())
                .setAudience(config.getTokenEndpoint())
                .setExpiration(Date.from(Instant.now().plusSeconds(1200)));
        String jwt = jwtBuilder
                .signWith(SignatureAlgorithm.RS256, privateKey)
                .compact();
        log.debug("jwt: {}", jwt);
        return jwt;
    }

    private static Map<String, String> callTokeEndpoint(String jwt) {
        WebClient client = WebClient.builder().build();
        String requestBody = "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=" + jwt;
        Mono<String> responseMono = client.post()
                .uri(config.getTokenEndpoint())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(String.class);

        String response = responseMono.block();
        log.debug("response: {}", response);

        Map<String, String> tokenResp = new HashMap<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode responseJson = mapper.readTree(response);
            tokenResp.put("access_token", responseJson.get("access_token").asText());
            tokenResp.put("instance_url", responseJson.get("instance_url").asText());
            tokenResp.put("id", responseJson.get("id").asText());
            tokenResp.put("token_type", responseJson.get("token_type").asText());

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON response", e);
        }

        return tokenResp;
    }

}
