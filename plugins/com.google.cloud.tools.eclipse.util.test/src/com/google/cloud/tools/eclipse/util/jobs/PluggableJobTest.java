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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.util.concurrent.Callables;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.Test;

/**
 * Test PluggableJob and, by extension, FuturisticJob.
 */
public class PluggableJobTest {

  @Test
  public void testConstructor_nullCallable() {
    try {
      new PluggableJob<Void>("name", null);
      fail("Expected NPE");
    } catch (NullPointerException ex) {
    }
  }

  @Test
  public void testConstructor_nullStalenessCheck() {
    try {
      new PluggableJob<>("name", Callables.returning((Void) null), null);
      fail("Expected NPE");
    } catch (NullPointerException ex) {
    }
  }

  @Test
  public void testScheduled() throws InterruptedException, ExecutionException {
    Object obj = new Object();
    PluggableJob<Object> job = new PluggableJob<>("name", Callables.returning(obj));
    assertFalse(job.getFuture().isDone());
    assertFalse(job.getFuture().isCancelled());
    job.schedule();
    job.join();
    assertTrue(job.getFuture().isDone());
    assertFalse(job.getFuture().isCancelled());
    assertSame(obj, job.getFuture().get());
    assertEquals("Should be OK", IStatus.OK, job.getResult().getSeverity());
  }

  @Test
  public void testJobCancelingCancelsFuture() throws InterruptedException, BrokenBarrierException {
    final CyclicBarrier barrier = new CyclicBarrier(2);
    PluggableJob<Object> job = new PluggableJob<>("name", new Callable<Object>() {
      @Override
      public Object call() {
        try {
          barrier.await(); // job started: should release main thread
          barrier.await(); // wait for job cancel
        } catch (InterruptedException | BrokenBarrierException ex) {
        }
        return barrier;
      }
    });
    job.schedule();
    barrier.await();
    job.cancel();
    assertTrue("future should be cancelled by canceling()", job.getFuture().isCancelled());
    barrier.await(); // job should now finish but be cancelled
    job.join();
    assertNotNull("Job should be finished", job.getResult());
    assertEquals("Should be CANCEL", IStatus.CANCEL, job.getResult().getSeverity());
  }

  @Test
  public void testFutureCancelingCancelsJob() throws InterruptedException, BrokenBarrierException {
    final CyclicBarrier barrier = new CyclicBarrier(2);
    PluggableJob<Object> job = new PluggableJob<>("name", new Callable<Object>() {
      @Override
      public Object call() {
        try {
          barrier.await(); // job started: should release main thread
          barrier.await(); // wait for future cancel
        } catch (InterruptedException | BrokenBarrierException ex) {
        }
        return barrier;
      }
    });
    job.schedule();
    barrier.await(); // wait until job started
    assertEquals("Should be RUNNING", Job.RUNNING, job.getState());
    job.getFuture().cancel(true);
    barrier.await(); // job should now finish but report as cancelled
    job.join();
    assertNotNull("Job should be finished", job.getResult());
    assertEquals("Should be CANCEL", IStatus.CANCEL, job.getResult().getSeverity());
  }

  @Test
  public void testStaleCancelsFuture() throws InterruptedException {
    Object obj = new Object();
    PluggableJob<Object> job =
        new PluggableJob<>("name", Callables.returning(obj), Predicates.alwaysTrue());
    job.schedule();
    job.join();
    assertEquals("Should be CANCEL", IStatus.CANCEL, job.getResult().getSeverity());
    assertTrue(job.getFuture().isCancelled());
  }

  @Test
  public void testStaleFiresFutureListener() throws InterruptedException {
    Object obj = new Object();
    final PluggableJob<Object> job =
        new PluggableJob<>("name", Callables.returning(obj), Predicates.alwaysTrue());
    assertFalse(job.getFuture().isDone());
    final boolean[] listenerRun = new boolean[] {false};
    job.getFuture().addListener(new Runnable() {
      @Override
      public void run() {
        listenerRun[0] = true;
        assertTrue(job.getFuture().isCancelled());
      }
    }, MoreExecutors.directExecutor());
    assertFalse(listenerRun[0]);
    job.schedule();
    job.join();
    assertTrue(listenerRun[0]);
    assertEquals("Should be CANCEL", IStatus.CANCEL, job.getResult().getSeverity());
  }

  @Test
  public void testCompleteness_normal() throws InterruptedException {
    Object obj = new Object();
    PluggableJob<Object> job = new PluggableJob<>("name", Callables.returning(obj));
    assertFalse(job.isComputationComplete());
    job.schedule();
    job.join();
    assertTrue(job.isComputationComplete());
    assertFalse(job.getComputationError().isPresent());
    assertTrue(job.getComputationResult().isPresent());
    assertEquals(obj, job.getComputationResult().get());
    assertEquals(obj, job.getComputation().get());
  }

  @Test
  public void testCompleteness_error() throws InterruptedException {
    final Exception exception = new Exception("test");
    PluggableJob<Object> job = new PluggableJob<>("name", new Callable<Object>() {
      @Override
      public Object call() throws Exception {
        throw exception;
      }
    });
    assertFalse(job.isComputationComplete());
    job.schedule();
    job.join();
    assertTrue(job.isComputationComplete());
    assertTrue(job.getComputationError().isPresent());
    assertEquals(exception, job.getComputationError().get());
    assertFalse(job.getComputationResult().isPresent());
    assertEquals(exception, job.getComputation().get());
  }

  @Test
  public void testOnSuccess_normal() throws InterruptedException {
    Object obj = new Object();
    PluggableJob<Object> job = new PluggableJob<>("name", Callables.returning(obj));
    final boolean[] listenerRun = new boolean[] {false};
    job.onSuccess(MoreExecutors.directExecutor(), new Runnable() {
      @Override
      public void run() {
        listenerRun[0] = true;
      }
    });
    assertFalse(listenerRun[0]);
    job.schedule();
    job.join();
    assertTrue(listenerRun[0]);
    assertTrue(job.isComputationComplete());
  }

  @Test
  public void testOnSuccess_abandon() throws InterruptedException {
    Object obj = new Object();
    PluggableJob<Object> job =
        new PluggableJob<>("name", Callables.returning(obj), Predicates.alwaysTrue());
    final boolean[] listenerRun = new boolean[] {false};
    job.onSuccess(MoreExecutors.directExecutor(), new Runnable() {
      @Override
      public void run() {
        listenerRun[0] = true;
      }
    });
    assertFalse(listenerRun[0]);
    job.schedule(); // should be stale and cancelled
    job.join();
    assertFalse("onSuccess should not have been called", listenerRun[0]);
  }

  @Test
  public void testOnError() throws InterruptedException {
    PluggableJob<Object> job = new PluggableJob<>("name", new Callable<Object>() {
      @Override
      public Object call() throws Exception {
        throw new Exception("test");
      }
    });
    final boolean[] listenerRun = new boolean[] {false};
    job.onError(MoreExecutors.directExecutor(), new Consumer<Exception>() {
      @Override
      public void accept(Exception result) {
        listenerRun[0] = true;
      }
    });
    assertFalse(listenerRun[0]);
    job.schedule();
    job.join();
    assertTrue("onError should have been called", listenerRun[0]);
  }

  @Test
  public void testIsCurrent_abandon() throws InterruptedException {
    Object obj = new Object();
    PluggableJob<Object> job = new PluggableJob<>("name", Callables.returning(obj));
    assertTrue(job.isCurrent());
    job.schedule(); // should be stale and cancelled
    job.join();
    assertTrue(job.isCurrent());
    job.abandon();
    assertFalse("Abandoned jobs should not be current", job.isCurrent());
  }

  @Test
  public void testIsCurrent_stale() throws InterruptedException {
    Object obj = new Object();
    final boolean[] isStale = new boolean[] { false };
    PluggableJob<Object> job = new PluggableJob<>("name", Callables.returning(obj),
        new Predicate<FuturisticJob<?>>() {
          @Override
          public boolean apply(FuturisticJob<?> job) {
            return isStale[0];
          }
        });
    assertTrue(job.isCurrent());
    job.schedule(); // should self-cancel
    job.join();
    assertTrue(job.isCurrent());
    isStale[0] = true;
    // should now be stale 
    assertFalse("Stale jobs should not be current", job.isCurrent());
  }

}
