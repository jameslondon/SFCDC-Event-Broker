package com.jil.util;

import com.jil.config.Config;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class BayeuxParametersImpl implements BayeuxParameters {

    private nCinoAccess access;
    private Config config;

    public BayeuxParametersImpl(nCinoAccess access, Config config) {
        this.access = access;
        this.config = config;
    }

    @Override
    public String bearerToken() {
        Map<String, String> token_resp_map = access.getAccessTokenSupplier().get();
        if (token_resp_map == null) {
            throw new RuntimeException("token_resp_map is null");
        }
        return token_resp_map.get("access_token");
    }

    @Override
    public URL host() {
        try {
            return new URL(config.getNCinoInstanceUrl());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(String.format("Unable to create url: %s", config.getNCinoInstanceUrl()), e);
        }
    }
}
