package com.benjazzy.mixchat;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

public class MixOauth {
	// private static final File DATA_STORE_DIR = new
	// File(System.getProperty("user.home"), ".store/dailymotion_sample");
	private static final File DATA_STORE_DIR = new File("/home/benjazzy/.store/dailymotion_sample");

	/**
	 * Global instance of the {@link DataStoreFactory}. The best practice is to make
	 * it a single globally shared instance across your application.
	 */
	private static FileDataStoreFactory DATA_STORE_FACTORY;

	/** OAuth 2 scope. */
	private static final String SCOPE = "user:act_as chat:chat chat:bypass_slowchat chat:connect";

	/** Global instance of the HTTP transport. */
	private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

	/** Global instance of the JSON factory. */
	static final JsonFactory JSON_FACTORY = new JacksonFactory();

	private static final String TOKEN_SERVER_URL = "https://mixer.com/api/v1/oauth/token";
	private static final String AUTHORIZATION_SERVER_URL = "https://mixer.com/oauth/authorize";

	private Credential credential;

	public MixOauth() {
		try {
			DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
			credential = authorize();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String getAccessToken() {
		return credential.getAccessToken();
	}

	public static HttpResponse executeGet(HttpTransport transport, JsonFactory jsonFactory, String accessToken,
			GenericUrl url) throws IOException {
		Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod())
				.setAccessToken(accessToken);
		HttpRequestFactory requestFactory = transport.createRequestFactory(credential);
		return requestFactory.buildGetRequest(url).execute();
	}

	/** Authorizes the installed application to access user's protected data. */
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
