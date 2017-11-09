/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ivan Popov - Bug 184211: JDI connectors throw NullPointerException if used separately
 *     			from Eclipse
 *     Google Inc - add support for accepting multiple connections
 *******************************************************************************/
package com.google.cloud.tools.eclipse.jdi.internal.connect;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.ListeningConnector;
import com.sun.jdi.connect.Transport;

import org.eclipse.jdi.internal.VirtualMachineManagerImpl;
import org.eclipse.jdi.internal.connect.ConnectMessages;
import org.eclipse.jdi.internal.connect.ConnectorImpl;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * A fork of org.eclipse.jdi.internal.connect.SocketListeningConnectorImpl that
 * uses our modified SocketTransportImpl.
 */
@SuppressWarnings("restriction")
public class SocketListeningMultiConnectorImpl extends ConnectorImpl implements ListeningConnector {
	/** Port to which is attached. */
	private int fPort;
	/** Timeout before accept returns. */
	private int fTimeout;

	/**
	 * Creates new SocketAttachingConnectorImpl.
	 */
	public SocketListeningMultiConnectorImpl(VirtualMachineManagerImpl virtualMachineManager) {
		super(virtualMachineManager);

		// Create communication protocol specific transport.
		SocketTransportImpl transport = new SocketTransportImpl();
		internalSetTransport(transport);
	}

	private void internalSetTransport(Transport transport) {
		try {
			Method setter = ConnectorImpl.class.getDeclaredMethod("setTransport", Transport.class);
			setter.setAccessible(true);
			setter.invoke(this, transport);
		} catch (Exception ex) {
			// probably should log this
			ex.printStackTrace();
		}
	}

	/**
	 * @return Returns the default arguments.
	 */
	@Override
	public Map<String, Connector.Argument> defaultArguments() {
		HashMap<String, Connector.Argument> arguments = new HashMap<String, Connector.Argument>(1);

		// Port
		IntegerArgument intArg = new _IntegerArgumentImpl("port", //$NON-NLS-1$
				ConnectMessages.SocketListeningConnectorImpl_Port_number_at_which_to_listen_for_VM_connections_1,
				ConnectMessages.SocketListeningConnectorImpl_Port_2, true, SocketTransportImpl.MIN_PORTNR,
				SocketTransportImpl.MAX_PORTNR);
		arguments.put(intArg.name(), intArg);

		// Timeout
		intArg = new _IntegerArgumentImpl("timeout", //$NON-NLS-1$
				ConnectMessages.SocketListeningConnectorImpl_Timeout_before_accept_returns_3,
				ConnectMessages.SocketListeningConnectorImpl_Timeout_4, false, 0, Integer.MAX_VALUE);
		arguments.put(intArg.name(), intArg);

		// FIXME: this doesn't feel like the right place, but
		// IntegerArgumentImpl is package restricted
		intArg = new _IntegerArgumentImpl("connectionLimit", //$NON-NLS-1$
				"Limit incoming connections (0 = no limit)", "Connection limit:", false, 0,
				Integer.MAX_VALUE);
		intArg.setValue(1); // mimics previous behaviour
		arguments.put(intArg.name(), intArg);

		return arguments;
	}

	/**
	 * @return Returns a short identifier for the connector.
	 */
	@Override
	public String name() {
		return "com.sun.jdi.SocketListen"; //$NON-NLS-1$
	}

	/**
	 * @return Returns a human-readable description of this connector and its
	 *         purpose.
	 */
	@Override
	public String description() {
		return ConnectMessages.SocketListeningConnectorImpl_Accepts_socket_connections_initiated_by_other_VMs_5;
	}

	/**
	 * Retrieves connection arguments.
	 */
	private void getConnectionArguments(Map<String, ? extends Connector.Argument> connectionArgs)
			throws IllegalConnectorArgumentsException {
		String attribute = "port"; //$NON-NLS-1$
		try {
			// If listening port is not specified, use port 0
			IntegerArgument argument = (IntegerArgument) connectionArgs.get(attribute);
			if (argument != null && argument.value() != null) {
				fPort = argument.intValue();
			} else {
				fPort = 0;
			}
			// Note that timeout is not used in SUN's ListeningConnector, but is
			// used by our
			// LaunchingConnector.
			attribute = "timeout"; //$NON-NLS-1$
			argument = (IntegerArgument) connectionArgs.get(attribute);
			if (argument != null && argument.value() != null) {
				fTimeout = argument.intValue();
			} else {
				fTimeout = 0;
			}
		} catch (ClassCastException e) {
			throw new IllegalConnectorArgumentsException(
					ConnectMessages.SocketListeningConnectorImpl_Connection_argument_is_not_of_the_right_type_6,
					attribute);
		} catch (NullPointerException e) {
			throw new IllegalConnectorArgumentsException(
					ConnectMessages.SocketListeningConnectorImpl_Necessary_connection_argument_is_null_7, attribute);
		} catch (NumberFormatException e) {
			throw new IllegalConnectorArgumentsException(
					ConnectMessages.SocketListeningConnectorImpl_Connection_argument_is_not_a_number_8, attribute);
		}
	}

	/**
	 * Listens for one or more connections initiated by target VMs.
	 * 
	 * @return Returns the address at which the connector is listening for a
	 *         connection.
	 */
	@Override
	public String startListening(Map<String, ? extends Connector.Argument> connectionArgs)
			throws IOException, IllegalConnectorArgumentsException {
		getConnectionArguments(connectionArgs);
		String result = null;
		try {
			result = ((SocketTransportImpl) fTransport).startListening(fPort);
		} catch (IllegalArgumentException e) {
			throw new IllegalConnectorArgumentsException(
					ConnectMessages.SocketListeningConnectorImpl_ListeningConnector_Socket_Port, "port"); //$NON-NLS-1$
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sun.jdi.connect.ListeningConnector#stopListening(java.util.Map)
	 */
	@Override
	public void stopListening(Map<String, ? extends Connector.Argument> connectionArgs) throws IOException {
		((SocketTransportImpl) fTransport).stopListening();
	}

	/**
	 * Waits for a target VM to attach to this connector.
	 * 
	 * @return Returns a connected Virtual Machine.
	 */
	@Override
	public VirtualMachine accept(Map<String, ? extends Connector.Argument> connectionArgs)
			throws IOException, IllegalConnectorArgumentsException {
		getConnectionArguments(connectionArgs);
		SocketConnection connection = (SocketConnection) ((SocketTransportImpl) fTransport).accept(fTimeout, 0);
		return establishedConnection(connection);
	}

	/**
	 * @return Returns whether this listening connector supports multiple
	 *         connections for a single argument map.
	 */
	@Override
	public boolean supportsMultipleConnections() {
		return true;
	}

	/**
	 * @return Returns port number that is listened to.
	 */
	public int listeningPort() {
		return fPort;
	}
}
