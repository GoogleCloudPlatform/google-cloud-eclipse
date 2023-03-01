/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.googleapis.internal;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessControlException;
import java.util.Locale;

/**
 * Lightweight version of DefaultCredentialProvider
 * @see <a href="https://github.dev/googleapis/google-api-java-client/blob/3cb09eb2dd5ed5f3eb5b9d18b7e4413d2f3634f1/google-api-client/src/main/java/com/google/api/client/googleapis/auth/oauth2/DefaultCredentialProvider.java#L45">com.google.api.client.googleapis.auth.oauth2.DefaultCredentialProvider</a>  
 */
public class NonCachedDefaultCredentialsProvider {

    static final String CREDENTIAL_ENV_VAR = "GOOGLE_APPLICATION_CREDENTIALS";

    static final String WELL_KNOWN_CREDENTIALS_FILE = "application_default_credentials.json";

    static final String CLOUDSDK_CONFIG_DIRECTORY = "gcloud";

    static final String CLOUD_SHELL_ENV_VAR = "DEVSHELL_CLIENT_PORT";

    private static enum Environment {
      UNKNOWN,
      ENVIRONMENT_VARIABLE,
      WELL_KNOWN_FILE,
    }
    
    Environment detectedEnvironment;
    
    NonCachedDefaultCredentialsProvider() {}

    /**
     * {@link Beta} <br>
     * Returns the Application Default Credentials.
     *
     * Lightweight version of DefaultCredentialProvider.getDefaultCredential
     * that does not cache the credentials (i.e. user can revoke or 
     * log in with a different account in the gcloud CLI and this will be detected)
     * @param transport the transport for Http calls.
     * @param jsonFactory the factory for Json parsing and formatting.
     * @return the credential instance.
     * @throws IOException if the credential cannot be created in the current environment.
     */
    final GoogleCredential getDefaultCredential(HttpTransport transport, JsonFactory jsonFactory)
        throws IOException {
      return getDefaultCredentialUnsynchronized(transport, jsonFactory);
    }

    private final GoogleCredential getDefaultCredentialUnsynchronized(
        HttpTransport transport, JsonFactory jsonFactory) throws IOException {

      if (detectedEnvironment == null) {
        detectedEnvironment = detectEnvironment(transport);
      }

      switch (detectedEnvironment) {
        case ENVIRONMENT_VARIABLE:
          return getCredentialUsingEnvironmentVariable(transport, jsonFactory);
        case WELL_KNOWN_FILE:
          return getCredentialUsingWellKnownFile(transport, jsonFactory);
        default:
          return null;
      }
    }

    private GoogleCredential getCredentialUsingWellKnownFile(
        HttpTransport transport, JsonFactory jsonFactory) throws IOException {
      File wellKnownFileLocation = getWellKnownCredentialsFile();
      InputStream credentialsStream = null;
      try {
        credentialsStream = new FileInputStream(wellKnownFileLocation);
        return GoogleCredential.fromStream(credentialsStream, transport, jsonFactory);
      } catch (IOException e) {
        throw new IOException(
            String.format(
                "Error reading credential file from location %s: %s",
                wellKnownFileLocation, e.getMessage()));
      } finally {
        if (credentialsStream != null) {
          credentialsStream.close();
        }
      }
    }
    
    private GoogleCredential getCredentialUsingEnvironmentVariable(
        HttpTransport transport, JsonFactory jsonFactory) throws IOException {
      String credentialsPath = System.getenv(CREDENTIAL_ENV_VAR);

      InputStream credentialsStream = null;
      try {
        credentialsStream = new FileInputStream(credentialsPath);
        return GoogleCredential.fromStream(credentialsStream, transport, jsonFactory);
      } catch (IOException e) {
        // Although it is also the cause, the message of the caught exception can have very
        // important information for diagnosing errors, so include its message in the
        // outer exception message also
        IOException toThrow = new IOException(
            String.format(
                "Error reading credential file from environment variable %s, value '%s': %s",
                CREDENTIAL_ENV_VAR, credentialsPath, e.getMessage()));
        toThrow.initCause(e);
        throw toThrow;
      } finally {
        if (credentialsStream != null) {
          credentialsStream.close();
        }
      }
    }
    
    private final File getWellKnownCredentialsFile() {
      File cloudConfigPath = null;
      String os = getProperty("os.name", "").toLowerCase(Locale.US);
      if (os.indexOf("windows") >= 0) {
        File appDataPath = new File(System.getenv("APPDATA"));
        cloudConfigPath = new File(appDataPath, CLOUDSDK_CONFIG_DIRECTORY);
      } else {
        File configPath = new File(getProperty("user.home", ""), ".config");
        cloudConfigPath = new File(configPath, CLOUDSDK_CONFIG_DIRECTORY);
      }
      File credentialFilePath = new File(cloudConfigPath, WELL_KNOWN_CREDENTIALS_FILE);
      return credentialFilePath;
    }

    /** Override in test code to isolate from environment. */
    boolean fileExists(File file) {
      return file.exists() && !file.isDirectory();
    }

    /** Override in test code to isolate from environment. */
    String getProperty(String property, String def) {
      return System.getProperty(property, def);
    }

    /** Override in test code to isolate from environment. */
    Class<?> forName(String className) throws ClassNotFoundException {
      return Class.forName(className);
    }

    private final Environment detectEnvironment(HttpTransport transport) throws IOException {
      // First try the environment variable
      if (runningUsingEnvironmentVariable()) {
        return Environment.ENVIRONMENT_VARIABLE;

        // Then try the well-known file
      } else if (runningUsingWellKnownFile()) {
        return Environment.WELL_KNOWN_FILE;
      }

      return Environment.UNKNOWN;
    }
    
    private boolean runningUsingEnvironmentVariable() throws IOException {
      String credentialsPath = System.getenv(CREDENTIAL_ENV_VAR);
      if (credentialsPath == null || credentialsPath.length() == 0) {
        return false;
      }

      try {
        File credentialsFile = new File(credentialsPath);
        if (!credentialsFile.exists() || credentialsFile.isDirectory()) {
          throw new IOException(
              String.format(
                  "Error reading credential file from environment variable %s, value '%s': "
                      + "File does not exist.",
                  CREDENTIAL_ENV_VAR, credentialsPath));
        }
        return true;
      } catch (AccessControlException expected) {
        return false;
      }
    }
    
    private boolean runningUsingWellKnownFile() {
      File wellKnownFileLocation = getWellKnownCredentialsFile();
      try {
        return fileExists(wellKnownFileLocation);
      } catch (AccessControlException expected) {
        return false;
      }
    }
    
}
