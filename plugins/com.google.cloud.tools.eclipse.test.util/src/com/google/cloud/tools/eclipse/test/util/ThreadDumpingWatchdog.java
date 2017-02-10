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

package com.google.cloud.tools.eclipse.test.util;

import com.google.cloud.tools.eclipse.test.util.reflection.ReflectionUtil;
import com.google.common.base.Stopwatch;
import java.lang.Thread.State;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.internal.jobs.JobManager;
import org.eclipse.core.internal.jobs.LockManager;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A JUnit test helper that periodically performs stack dumps for the current threads.
 */
public class ThreadDumpingWatchdog extends TimerTask implements TestRule {
  private final long period;
  private final TimeUnit unit;
  private final boolean ignoreUselessThreads;

  private Description description;
  private Timer timer;
  private Stopwatch stopwatch;

  public ThreadDumpingWatchdog(long period, TimeUnit unit) {
    this(period, unit, true);
  }

  public ThreadDumpingWatchdog(long period, TimeUnit unit, boolean ignoreUselessThreads) {
    this.period = period;
    this.unit = unit;
    this.ignoreUselessThreads = ignoreUselessThreads;
  }

  @Override
  public Statement apply(final Statement base, Description description) {
    this.description = description;
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        install();
        try {
          base.evaluate();
        } finally {
          remove();
        }
      }
    };
  }

  protected void install() {
    // Surefire doesn't output anything until the test is complete,
    // so it's hard to tell what test we're associated with
    System.out.println("[Watchdog] > " + description);
    timer = new Timer("Thread Dumping Watchdog");
    timer.scheduleAtFixedRate(this, unit.toMillis(period), unit.toMillis(period));
    stopwatch = Stopwatch.createStarted();
  }

  protected void remove() {
    timer.cancel();
  }

  @Override
  public void run() {
    Stopwatch dumpingTime = Stopwatch.createStarted();
    ThreadMXBean bean = ManagementFactory.getThreadMXBean();
    ThreadInfo[] infos = bean.dumpAllThreads(true, true);
    Arrays.sort(infos, new Comparator<ThreadInfo>() {
      @Override
      public int compare(ThreadInfo o1, ThreadInfo o2) {
        return Long.compare(o1.getThreadId(), o2.getThreadId());
      }
    });

    StringBuilder sb = new StringBuilder();
    sb.append("\n+-------------------------------------------------------------------------------");
    sb.append("\n| STACK DUMP @ ").append(stopwatch).append(": ").append(description);
    sb.append("\n|");
    dumpEclipseLocks(sb, "| ");

    int uselessThreadsCount = 0;
    for (ThreadInfo tinfo : infos) {
      // Unfortunately ThreadInfo#toString() only dumps up to 8 stackframes, and
      // this value is not configurable :-(
      if (!isUselessThread(tinfo)) {
        dumpThreadInfo(sb, "| ", tinfo);
      } else {
        uselessThreadsCount++;
      }
    }
    if (uselessThreadsCount > 0) {
      sb.append("\n| Ignored threads:");
      for (ThreadInfo tinfo : infos) {
        if (isUselessThread(tinfo)) {
          sb.append("\n|   ");
          dumpThreadHeader(sb, tinfo);
        }
      }
    }
    sb.append("\n| ELAPSED TIME: ").append(dumpingTime);
    sb.append("\n+-------------------------------------------------------------------------------");
    System.err.println(sb.toString());
  }

  /**
   * Attempt to obtain the Eclipse Lock Manager debug output. The output represents a matrix where
   * the columns are the allocated locks and the rows correspond to the threads and their interest
   * in the locks.
   * 
   * <pre>
   * Eclipse Locks:
   *  :: 
   *  R/, OrderedLock (4),
   *  ModalContext :  1, 0,
   *  Worker-4 :  0, 1,
   *  Worker-3 :  -1, 0,
   *  -------
   * </pre>
   * 
   * The first row following "::" lists the locks. A lock is either an
   * {@link org.eclipse.core.runtime.jobs.ISchedulingRule ISchedulingRule} or an explicit
   * {@link org.eclipse.core.runtime.jobs.ILock ILock}. Each subsequent row lists the relationships
   * between a thread and its acquired locks (&gt; 0), the locks it is waiting to acquire (-1), or
   * has no relationship (0). In the above, the workspace root ({@code R/}) has been acquired by
   * <em>ModalContext</em> but <em>Worker-3</em> would like to acquire it, and <em>Worker-4</em> has
   * acquired <em>ILock #4</em>.
   * 
   * @see org.eclipse.core.internal.jobs.DeadlockDetector
   */
  private void dumpEclipseLocks(StringBuilder sb, String linePrefix) {
    try {
      // Unfortunately this is not exposed in a nice manner
      LockManager manager = ((JobManager) Job.getJobManager()).getLockManager();
      if (manager.isEmpty()) {
        // don't output anything if no locks are held
        return;
      }
      // locks is an instance of DeadlockDetector
      Object locks = ReflectionUtil.getField(manager, "locks", Object.class);
      String debugOutput = ReflectionUtil.invoke(locks, "toDebugString", String.class);
      sb.append("\n").append(linePrefix);
      sb.append("\n").append(linePrefix).append("Eclipse Locks:");
      sb.append("\n").append(linePrefix).append(debugOutput.replace("\n", "\n" + linePrefix));
    } catch (SecurityException | IllegalArgumentException | ReflectiveOperationException ex) {
      sb.append("\n").append(linePrefix).append("Eclipse Lock information not available");
    }
  }

  /**
   * Identify useless threads, like idle worker pool threads.
   */
  private boolean isUselessThread(ThreadInfo tinfo) {
    if (!ignoreUselessThreads) {
      return false;
    }
    String threadName = tinfo.getThreadName();
    if (tinfo.getThreadState() == State.TIMED_WAITING && tinfo.getLockInfo() != null) {
      String lockClassName = tinfo.getLockInfo().getClassName();
      // Eclipse Jobs worker:
      // "Worker-9" [107] TIMED_WAITING on org.eclipse.core.internal.jobs.WorkerPool@5f4b99c7
      if (threadName.startsWith("Worker-")
          && "org.eclipse.core.internal.jobs.WorkerPool".equals(lockClassName)) {
        return true;
      }
      // "Timer-0" [43] TIMED_WAITING on java.util.TaskQueue@6ac9af9e
      if (threadName.startsWith("Timer-") && "java.util.TaskQueue".equals(lockClassName)) {
        return true;
      }
      // "Active Thread: Equinox Container: 0a1c2f36-c9b6-4aea-8192-af1c5847a0f2" [16] TIMED_WAITING
      // on java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject@38a2a717
      if (threadName.startsWith("Active Thread: Equinox Container: ")
          && "java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject"
              .equals(lockClassName)) {
        return true;
      }
    }
    if (tinfo.getThreadState() == State.WAITING && tinfo.getLockInfo() != null) {
      String lockClassName = tinfo.getLockInfo().getClassName();
      // "Reference Handler" [2] WAITING on java.lang.ref.Reference$Lock@3a28b268
      if ("Reference Handler".equals(threadName)
          && "java.lang.ref.Reference$Lock".equals(lockClassName)) {
        return true;
      }
      // "Finalizer" [3] WAITING on java.lang.ref.ReferenceQueue$Lock@2902d3d5
      // "EMF Reference Cleaner" [35] WAITING on java.lang.ref.ReferenceQueue$Lock@a0853f9
      if (("Finalizer".equals(threadName) || "EMF Reference Cleaner".equals(threadName))
          && "java.lang.ref.ReferenceQueue$Lock".equals(lockClassName)) {
        return true;
      }
      // "Worker-JM" [28] WAITING on java.util.ArrayList@46ce31f9
      if ("Worker-JM".equals(threadName)
          && "java.util.ArrayList".equals(lockClassName)) {
        return true;
      }
      // "SCR Component Actor" [27] WAITING on java.util.LinkedList@787d08c3
      if ("SCR Component Actor".equals(threadName)
          && "java.util.LinkedList".equals(lockClassName)) {
        return true;
      }
      // "Bundle File Closer" [21] WAITING on org.eclipse.osgi.framework.eventmgr.EventManager$EventThread@76faf029
      // "Refresh Thread: Equinox Container: 0a1c2f36-c9b6-4aea-8192-af1c5847a0f2" [19] WAITING on
      // org.eclipse.osgi.framework.eventmgr.EventManager$EventThread@9d34d50
      // "Start Level: Equinox Container: 0a1c2f36-c9b6-4aea-8192-af1c5847a0f2" [20] WAITING on
      // org.eclipse.osgi.framework.eventmgr.EventManager$EventThread@3e25e294
      // "Framework Event Dispatcher:
      // org.eclipse.osgi.internal.framework.EquinoxEventPublisher@2aa5fe93" [18] WAITING on
      // org.eclipse.osgi.framework.eventmgr.EventManager$EventThread@19f867b9
      if (("Bundle File Closer".equals(threadName)
          || threadName.startsWith("Refresh Thread: Equinox Container: ")
          || threadName.startsWith("Start Level: Equinox Container: ")
          || threadName.startsWith(
              "Framework Event Dispatcher: org.eclipse.osgi.internal.framework.EquinoxEventPublisher"))
          && "org.eclipse.osgi.framework.eventmgr.EventManager$EventThread".equals(lockClassName)) {
        return true;
      }
    }
    // "Signal Dispatcher" [4] RUNNABLE
    // "JDWP Transport Listener: dt_socket" [5] RUNNABLE
    // "JDWP Event Helper Thread" [6] RUNNABLE
    // "JDWP Command Reader" [7] RUNNABLE (in native code)
    if (tinfo.getThreadState() == State.RUNNABLE
        && (threadName.startsWith("JDWP ") || "Signal Dispatcher".equals(threadName))) {
      return true;
    }

    return false;
  }

  @SuppressWarnings("incomplete-switch")
  private void dumpThreadInfo(StringBuilder sb, String prefix, ThreadInfo tinfo) {
    sb.append('\n').append(prefix);
    dumpThreadHeader(sb, tinfo);

    StackTraceElement[] trace = tinfo.getStackTrace();
    if (trace.length > 0) {
      sb.append('\n').append(prefix).append("    at ").append(trace[0]);
      if (tinfo.getLockInfo() != null) {
        sb.append('\n').append(prefix).append("    - ");
        switch (tinfo.getThreadState()) {
          case BLOCKED:
            sb.append("blocked on ");
            break;
          case TIMED_WAITING:
          case WAITING:
            sb.append("waiting on ");
            break;
        }
        sb.append(tinfo.getLockInfo());
      }
      MonitorInfo[] lockedMonitors = tinfo.getLockedMonitors();
      for (int i = 1; i < trace.length; i++) {
        sb.append("\n").append(prefix).append("    at ").append(trace[i]);
        for (MonitorInfo minfo : lockedMonitors) {
          if (minfo.getLockedStackDepth() == i) {
            sb.append("\n").append(prefix).append("    - locked ").append(minfo);
          }
        }
      }
    }
    LockInfo[] lockedSynchronizers = tinfo.getLockedSynchronizers();
    if (lockedSynchronizers.length > 0) {
      sb.append("\n").append(prefix).append("    Locked synchronizers:");
      for (int i = 0; i < lockedSynchronizers.length; i++) {
        sb.append("\n").append(prefix).append("      ").append(i).append(". ")
            .append(lockedSynchronizers[i]);
      }
    }
    sb.append("\n").append(prefix);
  }

  private void dumpThreadHeader(StringBuilder sb, ThreadInfo tinfo) {
    sb.append('"').append(tinfo.getThreadName()).append("\" [").append(tinfo.getThreadId())
        .append("] ").append(tinfo.getThreadState());
    if (tinfo.getLockName() != null) {
      sb.append(" on ").append(tinfo.getLockName());
    }
    if (tinfo.getLockOwnerName() != null) {
      sb.append(" owned by '").append(tinfo.getLockOwnerName()).append(" [id:")
          .append(tinfo.getLockOwnerId()).append(']');
    }
    if (tinfo.isSuspended()) {
      sb.append(" (suspended)");
    }
    if (tinfo.isInNative()) {
      sb.append(" (in native code)");
    }
  }

}
