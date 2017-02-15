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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.jdt.internal.launching.SocketListenMultiConnector;

import com.sun.jdi.connect.Connector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.jdt.launching.SocketUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Test the SocketListen*Multi*Connector*
 */
@SuppressWarnings("restriction")
public class MultipleConnectionsTest {

	private ILaunch launch = new MockLaunch();

	private SocketListenMultiConnector connector;

	private int port;

	@Before
	public void setUp() {
		port = SocketUtil.findFreePort();
	}

	@Test
	public void testDefaultSettings() throws CoreException {
		connector = new SocketListenMultiConnector();
		Map<String, Connector.Argument> defaults = connector.getDefaultArguments();
		assertTrue(defaults.containsKey("connectionLimit"));
		assertEquals(1, ((Connector.IntegerArgument) defaults.get("connectionLimit")).intValue());
	}

	/**
	 * Ensure out-of-the-box settings mimics previous behaviour of accepting a
	 * single connection
	 * 
	 * @throws IOException
	 */
	@Test
	public void testDefaultBehaviour() throws CoreException, InterruptedException {
		connector = new SocketListenMultiConnector();
		Map<String, String> arguments = new HashMap<>();
		arguments.put("port", Integer.toString(port));
		connector.connect(arguments, new NullProgressMonitor(), launch);
		Thread.sleep(200);

		assertTrue("first connect should succeed", connect());
		assertFalse("second connect should fail", connect());
	}

	/**
	 * Ensure connector accepts a single connection
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testSingleConnectionBehaviour() throws CoreException, InterruptedException {
		connector = new SocketListenMultiConnector();
		Map<String, String> arguments = new HashMap<>();
		arguments.put("port", Integer.toString(port));
		arguments.put("connectionLimit", "1");
		connector.connect(arguments, new NullProgressMonitor(), launch);
		Thread.sleep(200);

		assertTrue("first connect should succeed", connect());
		assertFalse("second connect should fail", connect());
	}

	/**
	 * Ensure out-of-the-box settings mimics previous behaviour of accepting a
	 * single connection
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testTwoConnectionsBehaviour() throws CoreException, InterruptedException {
		connector = new SocketListenMultiConnector();
		Map<String, String> arguments = new HashMap<>();
		arguments.put("port", Integer.toString(port));
		arguments.put("connectionLimit", "2");
		connector.connect(arguments, new NullProgressMonitor(), launch);
		Thread.sleep(200);

		assertTrue("first connect should succeed", connect());
		assertTrue("second connect should succeed", connect());
	}

	/**
	 * Ensure out-of-the-box settings mimics previous behaviour of accepting a
	 * single connection
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testUnlimitedConnectionsBehaviour() throws CoreException, InterruptedException {
		connector = new SocketListenMultiConnector();
		Map<String, String> arguments = new HashMap<>();
		arguments.put("port", Integer.toString(port));
		arguments.put("connectionLimit", "0");
		connector.connect(arguments, new NullProgressMonitor(), launch);
		Thread.sleep(200);

		for (int i = 0; i < 10; i++) {
			assertTrue("connection " + i + " should succeed", connect());
		}
	}

	@After
	public void tearDown() throws DebugException {
		launch.terminate();
	}

	private boolean connect() {
		boolean result = true;
		// Two try blocks to distinguish between exceptions from socket close
		// (ignorable) and from dealing with the remote (errors)
		try (Socket s = new Socket()) {
			try {
				s.connect(new InetSocketAddress(InetAddress.getLocalHost(), port));
				byte[] buffer = new byte[14];
				s.getInputStream().read(buffer);
				assertEquals("JDWP-Handshake", new String(buffer));
				s.getOutputStream().write("JDWP-Handshake".getBytes());
				s.getOutputStream().flush();
				// Closing gracelessly like this produces
				// com.sun.jdi.VMDisconnectedExceptions on the log. Could
				// respond to JDWP to try to bring down the connections
				// gracefully, but it's a bit involved.
			} catch (IOException e) {
				result = false;
			}
		} catch(IOException e) {
		}
		try {
			// sleep to allow the remote side to setup the connection
			Thread.sleep(1000);
		} catch (InterruptedException ex) {
			// ignore
		}
		return result;
	}
}
