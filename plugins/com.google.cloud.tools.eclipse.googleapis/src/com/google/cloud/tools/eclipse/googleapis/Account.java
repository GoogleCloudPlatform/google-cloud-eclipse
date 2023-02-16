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

package com.google.cloud.tools.eclipse.googleapis;

import com.google.api.client.auth.oauth2.Credential;
import com.google.common.base.Preconditions;
import java.util.Optional;

/**
 * Represents a single logged-in account.
 */
public class Account {

  private final String email;
  private final Credential oAuth2Credential;
  private final String name;
  private final String avatarUrl;

  public Account(String email, Credential oAuth2Credential, String name, String avatarUrl) {
    Preconditions.checkNotNull(email);
    Preconditions.checkNotNull(oAuth2Credential);

    this.email = email;
    this.oAuth2Credential = oAuth2Credential;
    this.name = name;
    this.avatarUrl = avatarUrl;
  }

  public String getEmail() {
    return email;
  }

  public Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  public Optional<String> getAvatarUrl() {
    return Optional.ofNullable(avatarUrl);
  }

  public Credential getOAuth2Credential() {
    return oAuth2Credential;
  }

  /**
   * Identical to {@code getOAuth2Credential().getAccessToken()}.
   */
  public Optional<String> getAccessToken() {
    return Optional.ofNullable(oAuth2Credential.getAccessToken());
  }

  /**
   * Identical to {@code getOAuth2Credential().getRefreshToken()}.
   */
  public Optional<String> getRefreshToken() {
    return Optional.ofNullable(oAuth2Credential.getRefreshToken());
  }

  long getAccessTokenExpiryTime() {
    return oAuth2Credential.getExpirationTimeMilliseconds();
  }

  @Override
  public boolean equals(Object account) {
    if (!(account instanceof Account)) {
      return false;
    }
    return email.equals(((Account) account).getEmail());
  }

  @Override
  public int hashCode() {
    return email.hashCode();
  }
}
