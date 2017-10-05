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

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * A {@link Job} for executing some background computation that produces a result using the Eclipse
 * Jobs framework. Although the <em>computation result</em> is made available via a
 * {@link #getFuture() future}, most users will prefer to use the <code>onSuccess()</code> and
 * <code>onError()</code> callbacks.
 * <p>
 * A frequent pattern of use is to background some work and then update UI in response. There is a
 * danger that a new job is started before the UI-update has occurred, such that the UI update is
 * now stale and should be abandoned. Checking for this situation can be painful and can be
 * simplified providing callers use the following pattern:
 * 
 * <pre>
 *   currentJob.abandon();  // note: not #cancel()
 *   currentJob = new FuturisticJobSubclass(...);
 *   currentJob.onSuccess(displayExecutor, new Runnable() { ... update UI ... });
 *   currentJob.schedule();
 * </pre>
 * 
 * This example uses {@link #abandon()} as calling {@link #cancel()} on a job that has completed is
 * ignored by the Eclipse Jobs framework, such that any pending runnables cannot check if they are
 * operating against stale data.
 * <p>
 * The job checks for whether the {@link #cancel() job} or {@link Future#cancel(boolean) future}
 * have been cancelled/abandoned. The job also checks whether it has become {@link #isStale()
 * stale}, meaning that the original configuration parameters for the computation have become out of
 * date. The job checks for cancellation or staleness before setting the future result.
 * <p>
 * By default, exceptions are treated as an expected outcome and reported as a <em>computation
 * result</em> {@link SettableFuture#setException(Throwable) on the future} and via the
 * <code>onError()</code>, but the {@link #getResult() job result} will be {@link IStatus#OK OK}.
 * Subclasses may change this exception handling by overriding
 * {@link #handleException(SettableFuture, Exception)}. Note that a {@link #getResult() job result}
 * of anything other than {@link IStatus#OK} may result in some kind of UI shown to the user.
 */
public abstract class FuturisticJob<T> extends Job {
  private final SettableFuture<T> future = SettableFuture.create();

  /**
   * If true, then this job has been abandoned and any pending success and error listeners will not
   * be notified.
   */
  private volatile boolean abandoned = false;

  /**
   * Create a default instance; intended to be used by subclasses that must override
   * {@link #compute(IProgressMonitor)} and optionally {@link #isStale()}.
   * 
   * @param name the job name, surfaced in UI; never {@code null}
   */
  protected FuturisticJob(String name) {
    super(name);
  }

  /*** API ****/

  /**
   * Mark this job as abandoned: like {{@link #cancel()} but applies to completed jobs and prevents
   * any pending <code>onSuccess</code> and <code>onError</code> listeners from being notified.
   */
  public void abandon() {
    abandoned = true;
    cancel();
    if (!future.isCancelled()) {
      future.cancel(true);
    }
  }

  /**
   * Request that <code>callback</code> be executed with the computation value once complete,
   * providing that this job has not been abandoned or cancelled. The callback is executed using the
   * provided executor.
   */
  public void onSuccess(Executor executor, final Consumer<? super T> callback) {
    Runnable dispatch = new Runnable() {
      @Override
      public void run() {
        if (!abandoned && !future.isCancelled()) {
          try {
            callback.accept(future.get());
          } catch (InterruptedException | ExecutionException ex) {
            // ignore
          }
        }
      }
    };
    if (isCurrent()) {
      future.addListener(dispatch, executor);
    }
  }

  /**
   * Request that <code>runnable</code> be executed when the computation is complete, providing that
   * this job has not been abandoned or cancelled. The runnable is executed using the provided
   * executor.
   */
  public void onSuccess(Executor executor, final Runnable runnable) {
    onSuccess(executor, new Consumer<T>() {
      @Override
      public void accept(T result) {
        runnable.run();
      }
    });
  }

  /**
   * Request that <code>runnable</code> be executed should an exception be reported as a result of
   * the computation, providing that this job has not been abandoned or cancelled. The callback is
   * executed using the provided executor.
   */
  public void onError(Executor executor, final Consumer<? super Exception> callback) {
    Runnable dispatch = new Runnable() {
      @Override
      public void run() {
        if (!abandoned && !future.isCancelled()) {
          try {
            future.get();
          } catch (ExecutionException ex) {
            // #compute() only throws Exception
            Verify.verify(ex.getCause() instanceof Exception);
            callback.accept((Exception) ex.getCause());
          } catch (InterruptedException ex) {
            // ignored
          }
        }
      }
    };
    if (isCurrent()) {
      future.addListener(dispatch, executor);
    }
  }

  /**
   * Return true if this job appears to be current (i.e., not abandoned, not cancelled, and not
   * stale).
   */
  public boolean isCurrent() {
    return !abandoned && !future.isCancelled() && !isStale();
  }

  /**
   * Return true if this job's computation completed. The computation state is independent of
   * whether the job {@link #isCurrent() is current}.
   * 
   * @see #getComputationResult()
   * @see #getComputationError()
   */
  public boolean isComputationComplete() {
    return future.isDone() && !future.isCancelled();
  }

  /**
   * Return the result of the computation, if complete. The result will be an exception if an error
   * occurred, or the computation result. The computation state is independent of whether the job
   * {@link #isCurrent() is current}.
   */
  public Optional<Object> getComputation() {
    if (isComputationComplete()) {
      try {
        return Optional.of((Object) future.get());
      } catch (ExecutionException ex) {
        Verify.verify(ex.getCause() instanceof Exception);
        return Optional.of((Object) ex.getCause());
      } catch (InterruptedException ex) {
        // ignored and fallthrough
      }
    }
    return Optional.absent();
  }

  /**
   * If completed and successful, return the result of the computation. The computation state is
   * independent of whether the job {@link #isCurrent() is current}
   */
  public Optional<T> getComputationResult() {
    if (isComputationComplete()) {
      try {
        return Optional.of(future.get());
      } catch (InterruptedException | ExecutionException ex) {
        // ignored and fallthrough
      }
    }
    return Optional.absent();
  }

  /**
   * If completed and an error occurred, return the result of the computation. The computation state
   * is independent of whether the job {@link #isCurrent() is current}
   */
  public Optional<Exception> getComputationError() {
    if (isComputationComplete()) {
      try {
        future.get();
      } catch (ExecutionException ex) {
        Verify.verify(ex.getCause() instanceof Exception);
        return Optional.of((Exception) ex.getCause());
      } catch (InterruptedException ex) {
        // ignored and fallthrough
      }
    }
    return Optional.absent();
  }

  /**
   * Returns the computation result via the future.
   * 
   * @see #getComputationResult()
   * @see #onSuccess(Executor, Consumer)
   * @see #onError(Executor, Consumer)
   */
  public ListenableFuture<T> getFuture() {
    return future;
  }


  /*** OVERRIDES ***/

  /**
   * Compute and return the result. Subclasses can call {@link #checkCancelled(IProgressMonitor)} at
   * checkpoints to see if the job has been cancelled.
   * <p>
   * This default implementation executes the <code>computeTask</code>.
   */
  protected abstract T compute(IProgressMonitor monitor) throws Exception;

  /**
   * Return true if this job is stale: the provided starting parameters have been changed since the
   * job was started. This method should be fast and must be able to be executed from any thread.
   */
  protected boolean isStale() {
    return false;
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


  /*** IMPLEMENTATION ***/

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
}
