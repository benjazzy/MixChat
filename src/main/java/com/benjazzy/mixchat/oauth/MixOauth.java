package com.benjazzy.mixchat.oauth;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * The MixOauth class handles obtaining the Oauth2 tokens from Mixer.
 */
public class MixOauth {
	private static final File DATA_STORE_DIR = new File(System.getProperty("user.home") + File.separator + ".store" + File.separator + "mixchat");

	/**
	 * Global instance of the data store. The best practice is to make
	 * it a single globally shared instance across your application.
	 */
	private static FileDataStoreFactory DATA_STORE_FACTORY;

	/** OAuth 2 scope. */
	private static final String SCOPE = "user:act_as chat:chat chat:bypass_slowchat chat:connect chat:remove_message chat:purge";

	/** Global instance of the HTTP transport. */
	private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

	/** Global instance of the JSON factory. */
	static final JsonFactory JSON_FACTORY = new JacksonFactory();

	/** Token and  authorization endpoints for Mixer. */
	private static final String TOKEN_SERVER_URL = "https://mixer.com/api/v1/oauth/token";
	private static final String AUTHORIZATION_SERVER_URL = "https://mixer.com/oauth/authorize";

	/** Credential holds the oauth credentials */
	private Credential credential;

	/**
	 * Set's up the DATA_STORE_FACTORY and get tokens.
	 */
	public MixOauth() {
		try {
			DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
			credential = authorize();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * GetAccessToken tries to refresh token and returns the access token.
	 *
	 * @return
	 */
	public String getAccessToken() {
		String token = credential.getAccessToken();
		System.out.println(credential.getExpiresInSeconds());
		if (credential.getExpiresInSeconds() != null && credential.getExpiresInSeconds() < 300) {
			try {
				credential.refreshToken();
				token = credential.getAccessToken();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return token;
	}

	public boolean logout() {
		String[]entries = DATA_STORE_DIR.list();
		for(String s: entries){
			File currentFile = new File(DATA_STORE_DIR.getPath(),s);
			currentFile.delete();
		}
		credential.setExpiresInSeconds(0l);
		return DATA_STORE_DIR.delete();
	}

	/**
	 * Authorizes MixChat application to access user's account.
	 *
	 * @return New authorization.
	 */
	private static Credential authorize() throws Exception {
		OAuth2ClientCredentials.errorIfNotSpecified();
		// set up authorization code flow
		AuthorizationCodeFlow flow = new AuthorizationCodeFlow.Builder(BearerToken.authorizationHeaderAccessMethod(),
				HTTP_TRANSPORT, JSON_FACTORY, new GenericUrl(TOKEN_SERVER_URL),
				new ClientParametersAuthentication(OAuth2ClientCredentials.API_KEY, OAuth2ClientCredentials.API_SECRET),
				OAuth2ClientCredentials.API_KEY, AUTHORIZATION_SERVER_URL).setScopes(Arrays.asList(SCOPE))
						.setDataStoreFactory(DATA_STORE_FACTORY).build();
		// authorize
		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setHost(OAuth2ClientCredentials.DOMAIN)
				.setPort(OAuth2ClientCredentials.PORT).build();
		return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
	}
}
