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

import com.google.common.base.Stopwatch;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * 
 */
public class ThreadDumpingWatchdog extends TimerTask implements TestRule {
  private String title;
  private long period;
  private TimeUnit unit;
  private Timer timer;
  private Stopwatch stopwatch;

  public ThreadDumpingWatchdog(String title, long period, TimeUnit unit) {
    this.title = title;
    this.period = period;
    this.unit = unit;
  }

  @Override
  public Statement apply(final Statement base, Description description) {
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
    timer = new Timer("Thread Dumping Watchdog");
    timer.scheduleAtFixedRate(this, unit.toMillis(period), unit.toMillis(period));
    stopwatch = Stopwatch.createStarted();
  }

  protected void remove() {
    timer.cancel();
  }

  @Override
  public void run() {
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
    sb.append("\n| STACK DUMP @ ").append(stopwatch).append(": ").append(title);
    sb.append("\n|");
    for (ThreadInfo tinfo : infos) {
      sb.append("\n| ").append(tinfo.toString().replace("\n", "\n|"));
    }
    sb.append("\n+-------------------------------------------------------------------------------");
    System.err.println(sb.toString());
  }

}
