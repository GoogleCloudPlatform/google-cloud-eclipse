/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Google Inc - modify to support multiple incoming connections
 *******************************************************************************/

package com.google.cloud.tools.eclipse.jdt.internal.launching;

import com.google.cloud.tools.eclipse.jdi.internal.connect.SocketMultiListeningConnectorImpl;

import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.ListeningConnector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.jdi.Bootstrap;
import org.eclipse.jdi.internal.VirtualMachineManagerImpl;
import org.eclipse.jdt.internal.launching.LaunchingMessages;
import org.eclipse.jdt.internal.launching.LaunchingPlugin;
import org.eclipse.jdt.internal.launching.SocketListenConnectorProcess;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMConnector;
import org.eclipse.osgi.util.NLS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A standard socket listening connector. Starts a launch that waits for a VM to
 * connect at a specific port.
 * 
 * @since 3.4
 * @see SocketListenConnectorProcess
 */
public class SocketListenMultiConnector implements IVMConnector {

	/**
	 * Return the socket transport listening connector
	 * 
	 * @return the new {@link ListeningConnector}
	 * @exception CoreException
	 *                if unable to locate the connector
	 */
	private static ListeningConnector getListeningConnector() {
		return new SocketMultiListeningConnectorImpl((VirtualMachineManagerImpl) Bootstrap.virtualMachineManager());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.launching.IVMConnector#getIdentifier()
	 */
	@Override
	public String getIdentifier() {
		// return
		// IJavaLaunchConfigurationConstants.ID_SOCKET_LISTEN_VM_CONNECTOR;
		return "com.google.cloud.tools.eclipse.launching.jdt.socketListenerMultipleConnector";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.launching.IVMConnector#getName()
	 */
	@Override
	public String getName() {
		return "Standard (Listens for Multiple Connections)"; // LaunchingMessages.SocketListenConnector_1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.launching.IVMConnector#connect(java.util.Map,
	 * org.eclipse.core.runtime.IProgressMonitor,
	 * org.eclipse.debug.core.ILaunch)
	 */
	@Override
	public void connect(Map<String, String> arguments, IProgressMonitor monitor, ILaunch launch) throws CoreException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}

		monitor.subTask(LaunchingMessages.SocketListenConnector_2);

		ListeningConnector connector = getListeningConnector();

		String portNumberString = arguments.get("port"); //$NON-NLS-1$
		if (portNumberString == null) {
			abort(LaunchingMessages.SocketAttachConnector_Port_unspecified_for_remote_connection__2, null,
					IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_PORT);
		}
		boolean multiple = Boolean.valueOf(arguments.get("multiple"));

		Map<String, Connector.Argument> acceptArguments = connector.defaultArguments();

		Connector.Argument param = acceptArguments.get("port"); //$NON-NLS-1$
		param.setValue(portNumberString);

		try {
			monitor.subTask(NLS.bind(LaunchingMessages.SocketListenConnector_3, new String[] { portNumberString }));
			connector.startListening(acceptArguments);
			SocketListenMultiConnectorProcess process = new SocketListenMultiConnectorProcess(launch, portNumberString,
					multiple);
			process.waitForConnection(connector, acceptArguments);
		} catch (IOException e) {
			abort(LaunchingMessages.SocketListenConnector_4, e,
					IJavaLaunchConfigurationConstants.ERR_REMOTE_VM_CONNECTION_FAILED);
		} catch (IllegalConnectorArgumentsException e) {
			abort(LaunchingMessages.SocketListenConnector_4, e,
					IJavaLaunchConfigurationConstants.ERR_REMOTE_VM_CONNECTION_FAILED);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.launching.IVMConnector#getDefaultArguments()
	 */
	@Override
	public Map<String, Connector.Argument> getDefaultArguments() throws CoreException {
		Map<String, Connector.Argument> def = getListeningConnector().defaultArguments();
		Connector.IntegerArgument arg = (Connector.IntegerArgument) def.get("port"); //$NON-NLS-1$
		arg.setValue(8000);
		Connector.BooleanArgument multipleArg = (Connector.BooleanArgument) def.get("multiple"); //$NON-NLS-1$
		multipleArg.setValue(false); // perform as previous versions
		return def;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.launching.IVMConnector#getArgumentOrder()
	 */
	@Override
	public List<String> getArgumentOrder() {
		List<String> list = new ArrayList<String>(1);
		list.add("port"); //$NON-NLS-1$
		list.add("multiple"); //$NON-NLS-1$
		return list;
	}

	/**
	 * Throws a core exception with an error status object built from the given
	 * message, lower level exception, and error code.
	 * 
	 * @param message
	 *            the status message
	 * @param exception
	 *            lower level exception associated with the error, or
	 *            <code>null</code> if none
	 * @param code
	 *            error code
	 * @throws CoreException
	 *             if an error occurs
	 */
	protected static void abort(String message, Throwable exception, int code) throws CoreException {
		throw new CoreException(
				new Status(IStatus.ERROR, LaunchingPlugin.getUniqueIdentifier(), code, message, exception));
	}
}
