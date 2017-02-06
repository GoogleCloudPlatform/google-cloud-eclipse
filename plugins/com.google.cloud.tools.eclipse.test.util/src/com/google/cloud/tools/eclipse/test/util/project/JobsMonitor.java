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

package com.google.cloud.tools.eclipse.test.util.project;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Tracks the state of Eclipse Jobs, and reports outstanding tasks.
 */
public class JobsMonitor implements TestRule {
  public static final JobsMonitor INSTANCE = new JobsMonitor();

  private final AtomicInteger refCount = new AtomicInteger(0);
  private IJobManager jobManager = Job.getJobManager();
  private Map<Job, Stopwatch> readyJobs = Maps.newConcurrentMap();
  private Map<Job, Stopwatch> runningJobs = Maps.newConcurrentMap();
  private Map<Job, Stopwatch> sleepingJobs = Maps.newConcurrentMap();
  private Map<Job, Stopwatch> scheduledJobs = Maps.newConcurrentMap();

  private IJobChangeListener listener = new IJobChangeListener() {
    @Override
    public void sleeping(IJobChangeEvent event) {
      readyJobs.remove(event.getJob());
      runningJobs.remove(event.getJob());
      scheduledJobs.remove(event.getJob());
      sleepingJobs.put(event.getJob(), Stopwatch.createStarted());
    }

    @Override
    public void scheduled(IJobChangeEvent event) {
      readyJobs.remove(event.getJob());
      runningJobs.remove(event.getJob());
      sleepingJobs.remove(event.getJob());
      scheduledJobs.put(event.getJob(), Stopwatch.createStarted());
    }

    @Override
    public void running(IJobChangeEvent event) {
      readyJobs.remove(event.getJob());
      sleepingJobs.remove(event.getJob());
      scheduledJobs.remove(event.getJob());
      runningJobs.put(event.getJob(), Stopwatch.createStarted());
    }

    @Override
    public void done(IJobChangeEvent event) {
      readyJobs.remove(event.getJob());
      sleepingJobs.remove(event.getJob());
      scheduledJobs.remove(event.getJob());
      runningJobs.remove(event.getJob());
    }

    @Override
    public void awake(IJobChangeEvent event) {
      sleepingJobs.remove(event.getJob());
      scheduledJobs.remove(event.getJob());
      runningJobs.remove(event.getJob());
      readyJobs.put(event.getJob(), Stopwatch.createStarted());
    }

    @Override
    public void aboutToRun(IJobChangeEvent event) {}
  };

  public void install() {
    if (refCount.getAndIncrement() == 0) {
      jobManager.addJobChangeListener(listener);
    }
  }

  public void release() {
    if (refCount.decrementAndGet() == 0) {
      jobManager.removeJobChangeListener(listener);
    }
  }

  /** Report {@code running} and {@code ready} jobs. */
  public void report() {
    report(Job.RUNNING & Job.WAITING);
  }

  /** Report job matching the provided status. */
  public void report(int jobStates) {
    if ((jobStates & Job.RUNNING) != 0) {
      for (java.util.Map.Entry<Job, Stopwatch> entry : runningJobs.entrySet()) {
        output("running", entry.getKey(), entry.getValue());
      }
    }
    if ((jobStates & Job.WAITING) != 0) {
      for (java.util.Map.Entry<Job, Stopwatch> entry : readyJobs.entrySet()) {
        output("ready", entry.getKey(), entry.getValue());
      }
    }
    if ((jobStates & Job.SLEEPING) != 0) {
      for (java.util.Map.Entry<Job, Stopwatch> entry : scheduledJobs.entrySet()) {
        output("scheduled", entry.getKey(), entry.getValue());
      }
      for (java.util.Map.Entry<Job, Stopwatch> entry : sleepingJobs.entrySet()) {
        output("sleeping", entry.getKey(), entry.getValue());
      }
    }
  }

  private void output(String description, Job job, Stopwatch timer) {
    System.out.printf("[%s] %s (%s%s%s) [%s]\n", description, job, job.getClass(),
        (job.isSystem() ? ", system" : ""), (job.isUser() ? ", user" : ""), timer);
  }

  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        install();
        try {
          base.evaluate();
        } finally {
          report();
          release();
        }
      }
    };
  }

  // should use the main instance
  private JobsMonitor() {}
}
