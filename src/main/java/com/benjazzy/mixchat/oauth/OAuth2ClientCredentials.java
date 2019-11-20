/*
 * Copyright (c) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.benjazzy.mixchat.oauth;

/**
 * Contains the Oauth2 credentials.
 */
public class OAuth2ClientCredentials {

  /** Value of the "API Key". */
  public static final String API_KEY = "3721d6b1332a6db44a22ab5b71ae8e34ae187ee995b38f1a";

  /** Value of the "API Secret". */
  public static final String API_SECRET = "3077c3dbc12f46c8343953a300a350736e0ae9e65864dea83f5259682a4c39ca";

  /** Port in the "Callback URL". */
  public static final int PORT = 8080;

  /** Domain name in the "Callback URL". */
  public static final String DOMAIN = "127.0.0.1";

  /**
   * Notifies the user if API_KEY and API_SECRET are not specified.
   */
  public static void errorIfNotSpecified() {
    if (API_KEY.startsWith("Enter ") || API_SECRET.startsWith("Enter ")) {
      System.out.println(
          "Enter API Key and API Secret" + " into API_KEY and API_SECRET in " + OAuth2ClientCredentials.class);
      System.exit(1);
    }
  }
}