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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.Future;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * A {@link Job} for executing some background computation using the Jobs framework that produces a
 * result. The <em>computation result</em> is made available via a {@link #getFuture() future}. The
 * job checks for whether the {@link #cancel() job} or {@link Future#cancel(boolean) future} have
 * been cancelled. The job also checks whether it has become {@link #isStale() stale}, meaning that
 * the original configuration parameters for the computation have become out of date. The job checks
 * for cancellation or staleness before setting the future result.
 * <p>
 * By default, exceptions are treated as an expected outcome and reported as a <em>computation
 * result</em> on the future, but the {@link #getResult() job result} will be {@link IStatus#OK OK}.
 * Subclasses may change this exception handling by overriding
 * {@link #handleException(SettableFuture, Exception)}. Note that a {@link #getResult() job result}
 * of anything other than {@link IStatus#OK} may result in some kind of UI shown to the user.
 */
public abstract class FuturisticJob<T> extends Job {
  private final SettableFuture<T> future = SettableFuture.create();

  /**
   * Create a default instance; intended to be used by subclasses that must override
   * {@link #compute(IProgressMonitor)} and optionally {@link #isStale()}.
   * 
   * @param name the job name, surfaced in UI; never {@code null}
   */
  protected FuturisticJob(String name) {
    super(name);
  }

  /**
   * Compute and return the result. Subclasses can call {@link #checkCancelled(IProgressMonitor)} at
   * checkpoints to see if the job has been cancelled.
   * <p>
   * This default implementation executes the <code>computeTask</code>.
   */
  protected abstract T compute(IProgressMonitor monitor) throws Exception;

  /** Returns the computation result via the future. */
  public ListenableFuture<T> getFuture() {
    return future;
  }

  @Override
  protected final IStatus run(IProgressMonitor monitor) {
    checkCancelled(monitor);
    T result = null;
    try {
      result = compute(monitor);
      checkCancelled(monitor);
      if (!future.set(result)) {
        // setting the future may fail if the computation returned a future itself,
        // such that is is being set asynchronously, or if it was cancelled.
        if (!future.isDone()) {
          return ASYNC_FINISH;
        }
        if (future.isCancelled()) {
          return Status.CANCEL_STATUS;
        }
      }
      return Status.OK_STATUS;
    } catch (OperationCanceledException ex) {
      future.cancel(true);
      return Status.CANCEL_STATUS;
    } catch (Exception ex) { // throwable?
      checkCancelled(monitor);
      // allow subclasses to override exception handling
      return handleException(future, ex);
    }
  }

  /**
   * Handle the job execution result when an exception occurred during computation. This default
   * implementation treats exceptions as expected outcome, returning an {@link IStatus#OK} as the
   * job execution result and reporting the exception to the job creator via the {@link #getFuture()
   * job's result future} with {@link SettableFuture#setException(Throwable)}. Subclasses may wish
   * to override this handling:
   * <ul>
   * <li>to turn certain exceptions into normal results with {@link SettableFuture#set(Object)},
   * or</li>
   * <li>to report job execution failures by returning an {@link IStatus#ERROR} results as these may
   * be reported to the user.</li>
   * </ul>
   * 
   * @param resultFuture the future that will be returned by {{@link #getFuture()}; subclasses may
   *        change
   * @param ex the exception that occurred during computation
   * @return the job's execution result
   */
  protected IStatus handleException(SettableFuture<T> resultFuture, Exception ex) {
    resultFuture.setException(ex);
    return Status.OK_STATUS;
  }

  @Override
  protected void canceling() {
    future.cancel(true);
    super.canceling();
  }

  /**
   * Check if this job has been cancelled, either explicitly via {@link #cancel()}, by
   * {@link Future#cancel() cancelling the future}, or the job parameters {@link #isStale() becoming
   * stale}. This method may be called within {@link #compute(IProgressMonitor)}.
   * 
   * @throws OperationCanceledException if the job has been cancelled
   */
  protected void checkCancelled(IProgressMonitor monitor) throws OperationCanceledException {
    if (monitor.isCanceled() || future.isCancelled() || isStale()) {
      future.cancel(true);
      throw new OperationCanceledException();
    }
  }

  /**
   * Return true if this job is stale: the provided starting parameters have been changed since the
   * job was started. This method will be called from this job's thread.
   */
  protected boolean isStale() {
    return false;
  }
}
