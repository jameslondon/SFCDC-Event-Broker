package com.jil.config;

import com.jil.SFconnector.nCinoEmpConnector;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
	private static Config config;
	private String  clientId;
	private String  username;
	private String  nCinoPrivateKeyPath;
	private String tokenEndpoint;
	private String nCinoInstanceUrl;
	private Long relayFrom;
	private String gcsBuckName;
	private String bigQueryDatasetName;
	private String subscribedChangeEvents;

	private String keystoreType;
	private String keystorePath;
	private String keystoreAlias;
	private String googleCredentialKeyPath;

	public String getClientId() {
		return clientId;
	}
	public String getUsername() {
		return username;
	}
	public String getnCinoPrivateKeyPath() { return nCinoPrivateKeyPath; }
	public String getTokenEndpoint() {
		return tokenEndpoint;
	}
	public long getRelayFrom() { return relayFrom; }
	public String getSubscribedChangeEvents( ) { return subscribedChangeEvents; }

	public String getGcsBuckName() {return gcsBuckName;}
	public String getBigQueryDatasetName() {return bigQueryDatasetName;}
	public String getNCinoInstanceUrl() {return nCinoInstanceUrl;}
	public String getKeystoreType() {return keystoreType;}
	public String getKeystorePath() {return keystorePath;}
	public String getKeystoreAlias() {return keystoreAlias;}
	public String getGoogleCredentialKeyPath() {return googleCredentialKeyPath;}
	public static Config get() {
		return config;
	}
	public static Config get(String propertyFile) {
		if (config == null) {
			try (InputStream input = new FileInputStream(propertyFile)) {
				Properties prop = new Properties();
				prop.load(input);
				config = new Config();
				config.clientId = readMandatoryProp(prop, "clientId");
				config.username = readMandatoryProp(prop, "username");
				config.nCinoPrivateKeyPath = readOptionalProp(prop, "nCinoPrivateKeyPath");
				config.tokenEndpoint = readMandatoryProp(prop, "tokenEndpoint");
				config.nCinoInstanceUrl = readMandatoryProp(prop, "nCinoInstanceUrl");
				config.subscribedChangeEvents = readMandatoryProp(prop, "subscribedChangeEvents");
				config.gcsBuckName = readMandatoryProp(prop, "gcsBuckName");
				config.bigQueryDatasetName = readMandatoryProp(prop, "bigQueryDatasetName");
				config.keystoreType = readMandatoryProp(prop, "keystoreType");
				config.keystorePath = readMandatoryProp(prop, "keystorePath");
				config.keystoreAlias = readMandatoryProp(prop, "keystoreAlias");
				config.googleCredentialKeyPath = readOptionalProp(prop, "googleCredentialKeyPath");
				config.relayFrom = readOptionalLongProp(prop, "relayFrom");
			} catch (IOException e) {
				throw new RuntimeException("Failed to load configuration: " + e.getMessage(), e);
			}
		}
		return config;
	}

	private static String readMandatoryProp(Properties prop, String key) throws IOException {
		String value = prop.getProperty(key);
		if (value == null || value.trim().equals("")) {
			throw new IOException("Missing mandatory property: " + key);
		}
		return value;
	}
	
	private static int readMantoryIntProp(Properties prop, String key) throws IOException {
		String stringValue = readMandatoryProp(prop, key);
		return Integer.valueOf(stringValue);
	}
	private static long readOptionalLongProp(Properties prop, String key) throws IOException {
		String stringValue = prop.getProperty(key);
		if (stringValue == null || stringValue.trim().equals("")) {
			return nCinoEmpConnector.REPLAY_FROM_TIP;
		} else {
			return Long.valueOf(stringValue);
		}
	}
	private static String readOptionalProp(Properties prop, String key) throws IOException {
		return prop.getProperty(key);
	}
}
