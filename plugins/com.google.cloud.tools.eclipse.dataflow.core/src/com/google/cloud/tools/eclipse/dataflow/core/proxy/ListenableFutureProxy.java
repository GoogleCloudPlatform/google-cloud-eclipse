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

package com.google.cloud.tools.eclipse.dataflow.core.proxy;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A class that forwards all of its method calls to a contained {@code ListenableFuture}.
 */
public class ListenableFutureProxy<T> implements Future<T> {
  private final ListenableFuture<T> target;

  public ListenableFutureProxy(ListenableFuture<T> target) {
    this.target = target;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return target.cancel(mayInterruptIfRunning);
  }

  @Override
  public boolean isCancelled() {
    return target.isCancelled();
  }

  @Override
  public boolean isDone() {
    return target.isDone();
  }

  @Override
  public T get() throws InterruptedException, ExecutionException {
    return target.get();
  }

  @Override
  public T get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return target.get(timeout, unit);
  }

  public void addListener(Runnable arg0, Executor arg1) {
    target.addListener(arg0, arg1);
  }

}

