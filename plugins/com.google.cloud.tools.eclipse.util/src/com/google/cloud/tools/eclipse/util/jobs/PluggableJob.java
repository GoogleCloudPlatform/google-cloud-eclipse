/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.util.jobs;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import java.util.concurrent.Callable;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A {@link FuturisticJob} where the computation and staleness-checks are pluggable.
 */
public class PluggableJob<T> extends FuturisticJob<T> {
  /**
   * A pluggable check for whether this instance is stale; extenders can override
   * {@link #isStale()}. This check should be fast and must be able to be executed from any thread.
   */
  private final Predicate<? super FuturisticJob<T>> stalenessCheck;

  /**
   * Pluggable runnable for computing the result of the job; extenders can instead override
   * {@link #compute()}.
   */
  private final Callable<? extends T> computeTask;

  /**
   * Create a new instance with pluggable runnable. This instance is never stale.
   * 
   * @param name the job name, surfaced in UI; never {@code null}
   * @param computeTask the actual runnable to compute a result; never {@code null}
   */
  public PluggableJob(String name, Callable<? extends T> computeTask) {
    this(name, computeTask, Predicates.alwaysFalse());
  }

  /**
   * Create a new instance with pluggable runnable and stale check. The staleness check should be
   * fast and must be able to be executed from any thread.
   * 
   * @param name the job name, surfaced in UI; never {@code null}
   * @param computeTask the actual runnable to compute a result; never {@code null}
   * @param stalenessCheck a predicate to check if this job is stale; never {@code null},
   */
  public PluggableJob(String name, Callable<? extends T> computeTask,
      Predicate<? super FuturisticJob<T>> stalenessCheck) {
    super(name);
    Preconditions.checkNotNull(computeTask);
    Preconditions.checkNotNull(stalenessCheck);
    this.computeTask = computeTask;
    this.stalenessCheck = stalenessCheck;
  }

  @Override
  protected T compute(IProgressMonitor monitor) throws Exception {
    return computeTask.call();
  }

  @Override
  protected boolean isStale() {
    return stalenessCheck.apply(this);
  }



}
