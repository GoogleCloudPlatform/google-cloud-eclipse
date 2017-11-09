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

package com.google.cloud.tools.eclipse.jdt.launching;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.ISourceLocator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

public class MockLaunch implements ILaunch {
	private ConcurrentLinkedDeque<IProcess> processes = new ConcurrentLinkedDeque<>();
	private ConcurrentLinkedDeque<IDebugTarget> targets = new ConcurrentLinkedDeque<>();
	private ISourceLocator sourceLocator;
	private Map<String, String> attributes = new HashMap<>();

	@Override
	public boolean canTerminate() {
		for (IProcess p : processes) {
			if (p.canTerminate()) {
				return true;
			}
		}
		for (IDebugTarget t : targets) {
			if (t.canTerminate()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isTerminated() {
		for (IProcess p : processes) {
			if (!p.isTerminated()) {
				return false;
			}
		}
		for (IDebugTarget t : targets) {
			if (!t.isTerminated()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void terminate() throws DebugException {
		for (Iterator<IProcess> iter = processes.iterator(); iter.hasNext(); iter.remove()) {
			IProcess p = iter.next();
			if (p.canTerminate()) {
				p.terminate();
			}
		}
		for (Iterator<IDebugTarget> iter = targets.iterator(); iter.hasNext(); iter.remove()) {
			IDebugTarget t = iter.next();
			if (t.canTerminate()) {
				t.terminate();
			}
		}
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	@Override
	public Object[] getChildren() {
		return new Object[0];
	}

	@Override
	public IDebugTarget getDebugTarget() {
		return null;
	}

	@Override
	public IProcess[] getProcesses() {
		return processes.toArray(new IProcess[processes.size()]);
	}

	@Override
	public IDebugTarget[] getDebugTargets() {
		return targets.toArray(new IDebugTarget[targets.size()]);
	}

	@Override
	public void addDebugTarget(IDebugTarget target) {
		targets.add(target);
	}

	@Override
	public void removeDebugTarget(IDebugTarget target) {
		targets.remove(target);
	}

	@Override
	public void addProcess(IProcess process) {
		processes.add(process);
	}

	@Override
	public void removeProcess(IProcess process) {
		processes.remove(process);
	}

	@Override
	public ISourceLocator getSourceLocator() {
		return sourceLocator;
	}

	@Override
	public void setSourceLocator(ISourceLocator locator) {
		sourceLocator = locator;
	}

	@Override
	public String getLaunchMode() {
		return ILaunchManager.RUN_MODE;
	}

	@Override
	public ILaunchConfiguration getLaunchConfiguration() {
		return null;
	}

	@Override
	public void setAttribute(String key, String value) {
		attributes.put(key, value);
	}

	@Override
	public String getAttribute(String key) {
		return attributes.get(key);
	}

	@Override
	public boolean hasChildren() {
		return getChildren().length > 0;
	}
}
