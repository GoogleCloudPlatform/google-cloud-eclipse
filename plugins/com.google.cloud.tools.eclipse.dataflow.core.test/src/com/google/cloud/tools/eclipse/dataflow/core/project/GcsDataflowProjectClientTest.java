/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.core.project;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.MockitoAnnotations;

/**
 * Tests for {@link GcsDataflowProjectClient}.
 */
@RunWith(JUnit4.class)
public class GcsDataflowProjectClientTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private GcsDataflowProjectClient client;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);

    client = new GcsDataflowProjectClient(null);
  }

  @Test
  public void testToGcsLocationUriWithFullUriReturnsUri() {
    String location = "gs://foo-bar/baz";
    assertEquals(location, client.toGcsLocationUri(location));
  }

  @Test
  public void testToGcsLocationUriWithNullReturnsNull() {
    assertEquals(null, client.toGcsLocationUri(null));
  }

  @Test
  public void testToGcsLocationUriWithEmptyInputReturnsEmpty() {
    assertEquals("", client.toGcsLocationUri(""));
  }

  @Test
  public void testToGcsLocationUriWithoutPrefixReturnsWithPrefix() {
    String location = "foo-bar/baz";
    assertEquals("gs://" + location, client.toGcsLocationUri(location));
  }

  @Test
  public void testToGcsLocationUriWithBucketNameOnlyReturnsWithPrefix() {
    String location = "foo-bar";
    assertEquals("gs://foo-bar", client.toGcsLocationUri(location));
  }
}

