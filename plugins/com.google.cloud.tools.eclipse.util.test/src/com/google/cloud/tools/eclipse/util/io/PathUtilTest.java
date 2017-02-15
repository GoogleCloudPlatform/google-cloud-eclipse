/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.util.io;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.eclipse.core.runtime.Path;
import org.junit.Test;

public class PathUtilTest {

  @Test
  public void testMakePathAbsolute_nullPathReturnsBasePath() {
    assertThat(PathUtil.makePathAbsolute(null, new Path("/")).toString(), is("/"));
  }

  @Test
  public void testMakePathAbsolute_nullBasePathReturnsPath() {
    assertThat(PathUtil.makePathAbsolute(new Path("path"), null).toString(), is("path"));
  }

  @Test
  public void testMakePathAbsolute_absolutePathReturnsSamePath() {
    assertThat(PathUtil.makePathAbsolute(new Path("/path"), new Path("/foo")).toString(), is("/path"));
  }

  @Test
  public void testMakePathAbsolute_relativePathReturnsAppendedPath() {
    assertThat(PathUtil.makePathAbsolute(new Path("path"), new Path("/foo")).toString(), is("/foo/path"));
  }

  @Test
  public void testMakePathAbsolute_relativeBasePathReturnsRelativeAppendedPath() {
    assertThat(PathUtil.makePathAbsolute(new Path("path"), new Path("foo")).toString(), is("foo/path"));
  }

  @Test
  public void testRelativizePath_nullPathNullBasePathReturnsNull() {
    assertNull(PathUtil.relativizePath(null, null));
  }

  @Test
  public void testRelativizePath_nullPathReturnsBasePath() {
    assertThat(PathUtil.relativizePath(null, new Path("/foo")).toString(), is("/foo"));
  }

  @Test
  public void testRelativizePath_nullBasePathReturnsPath() {
    assertThat(PathUtil.relativizePath(new Path("foo"), null).toString(), is("foo"));
  }

  @Test
  public void testRelativizePath_pathNotPrefixOfBasePathReturnsPath() {
    assertThat(PathUtil.relativizePath(new Path("/foo/bar"), new Path("/baz")).toString(), is("/foo/bar"));
  }

  @Test
  public void testRelativizePath_relativePathNotPrefixReturnsPath() {
    assertThat(PathUtil.relativizePath(new Path("foo/bar"), new Path("/baz")).toString(), is("foo/bar"));
  }

  @Test
  public void testRelativizePath_pathPrefixOfBasePathReturnsRelativePath() {
    assertThat(PathUtil.relativizePath(new Path("/foo/bar"), new Path("/foo")).toString(), is("bar"));
  }

  @Test
  public void testRelativizePath_relativeBasePathReturnsRelativePath() {
    assertThat(PathUtil.relativizePath(new Path("foo/bar"), new Path("foo")).toString(), is("bar"));
  }

  // this test is 1. implementation dependent, 2. environment dependent, but shows that if path is relative, it will
  // likely be resolved to an absolute path that is not prefix of the basePath
  @Test
  public void testRelativizePath_relativePathPotentialyPrefixOfAbsoluteBasePathReturnsRelativePath() {
    assertThat(PathUtil.relativizePath(new Path("foo/bar"), new Path("/foo")).toString(), is("foo/bar"));
  }
}
