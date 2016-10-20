/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Google Inc - add support for accepting multiple connections
 *******************************************************************************/
package com.google.cloud.tools.eclipse.jdt.internal.launching;

import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.ListeningConnector;
import com.sun.jdi.connect.TransportTimeoutException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.jdi.TimeoutException;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.launching.LaunchingMessages;
import org.eclipse.jdt.internal.launching.LaunchingPlugin;
import org.eclipse.jdt.internal.launching.SocketListenConnector;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.osgi.util.NLS;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

/**
 * Fork of
 * {@link org.eclipse.jdt.internal.launching.SocketListenConnectorProcess}.
 * Knows how to handle "connectionLimit" count.
 * 
 * A process that represents a VM listening connector that is waiting for a VM
 * to remotely connect. Allows the user to see the status of the connection and
 * terminate it. If a successful connection occurs, the debug target is added to
 * the launch and this process is removed. Supports receiving multiple
 * connections.
 * 
 * @since 3.4
 * @see SocketListenConnector
 */
@SuppressWarnings("restriction")
public class SocketListenMultiConnectorProcess implements IProcess {
    /**
	 * The number of incoming connections to accept (0 = unlimited). Setting to
	 * 1 mimics previous behaviour.
	 */
	private int fConnectionLimit;

	/** The number of connections accepted so far. */
	private int fAccepted = 0;

	/** Time when this instance was created (milliseconds) */
	private long fStartTime;

    /**
     * Whether this process has been terminated.
     */
    private boolean fTerminated = false;
    /**
     * The launch this process belongs to
     */
    private ILaunch fLaunch;
    /**
     * The port this connector will listen on.
     */
    private String fPort;
    /**
     * The system job that will wait for incoming VM connections.
     */
    private WaitForConnectionJob fWaitForConnectionJob;

    /**
     * Creates this process. The label for this process will state the port the
     * connector is listening at.
     * 
     * @param launch
     *            the launch this process belongs to
     * @param port
     *            the port the connector will wait on
     * @param connectionLimit
     *            the number of incoming connections to accept (0 = unlimited)
     */
    public SocketListenMultiConnectorProcess(ILaunch launch, String port, int connectionLimit) {
        fLaunch = launch;
        fPort = port;
		fConnectionLimit = connectionLimit;
    }
    
    /**
     * Starts a job that will accept a VM remotely connecting to the 
     * given connector.  The #startListening() method must have been
     * called on the connector with the same arguments before calling
     * this method.  The 'port' argument in the map should have the same
     * value as the port specified in this process' constructor.
     * 
     * @param connector the connector that will accept incoming connections
     * @param arguments map of arguments that are used by the connector
     * @throws CoreException if a problem occurs trying to accept a connection
     * @see SocketListenConnector
     */
	public void waitForConnection(ListeningConnector connector, Map<String, Connector.Argument> arguments)
			throws CoreException {
        if (isTerminated()){
            throw new CoreException(getStatus(LaunchingMessages.SocketListenConnectorProcess_0, null, IJavaLaunchConfigurationConstants.ERR_REMOTE_VM_CONNECTION_FAILED));
        }
		fStartTime = System.currentTimeMillis();
		fAccepted = 0;
        // If the connector does not support multiple connections, accept a single connection
        try {
            if (!connector.supportsMultipleConnections()) {
				fConnectionLimit = 1;
            }
        } catch (IOException | IllegalConnectorArgumentsException ex) {
			fConnectionLimit = 1;
        }
        fLaunch.addProcess(this);
        fWaitForConnectionJob = new WaitForConnectionJob(connector, arguments);
		fWaitForConnectionJob.setPriority(Job.SHORT);
		fWaitForConnectionJob.setSystem(true);
        fWaitForConnectionJob.addJobChangeListener(new JobChangeAdapter(){
            @Override
            public void running(IJobChangeEvent event) {
                fireReadyToAcceptEvent();
            }
            @Override
            public void done(IJobChangeEvent event) {
				if (event.getResult().isOK() && continueListening()) {
                    fWaitForConnectionJob.schedule();
				} else {
                    try{
                        terminate();
                    } catch (DebugException e){}
                }
            }
        });
        fWaitForConnectionJob.schedule();
    }

    /**
	 * Return true if this connector should continue listening for further
	 * connections.
	 */
	protected boolean continueListening() {
		return !isTerminated() && (fConnectionLimit <= 0 || fConnectionLimit - fAccepted > 0);
	}

	/**
	 * Returns an error status using the passed parameters.
	 * 
	 * @param message
	 *            the status message
	 * @param exception
	 *            lower level exception associated with the error, or
	 *            <code>null</code> if none
	 * @param code
	 *            error code
	 * @return the new {@link IStatus}
	 */
    protected static IStatus getStatus(String message, Throwable exception, int code) {
        return new Status(IStatus.ERROR, LaunchingPlugin.getUniqueIdentifier(), code, message, exception);
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.debug.core.model.IProcess#getExitValue()
     */
    @Override
    public int getExitValue() throws DebugException {
        return 0;
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.core.model.IProcess#getLabel()
     */
    @Override
    public String getLabel() {
        return NLS.bind(LaunchingMessages.SocketListenConnectorProcess_1, new String[]{fPort});
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.core.model.IProcess#getLaunch()
     */
    @Override
    public ILaunch getLaunch() {
        return fLaunch;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.debug.core.model.ITerminate#canTerminate()
     */
    @Override
    public boolean canTerminate() {
        return !fTerminated;
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.core.model.ITerminate#isTerminated()
     */
    @Override
    public boolean isTerminated() {
        return fTerminated;
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.core.model.ITerminate#terminate()
     */
    @Override
    public void terminate() throws DebugException {
        if (!fTerminated){
            fTerminated = true;
            fLaunch.removeProcess(this);
            if (fWaitForConnectionJob != null){
                fWaitForConnectionJob.cancel();
                fWaitForConnectionJob.stopListening();
                fWaitForConnectionJob = null;
            }
            fireTerminateEvent();
        }
    }
    
    /**
     * Fires a terminate event.
     */
    protected void fireTerminateEvent() {
        DebugPlugin manager= DebugPlugin.getDefault();
        if (manager != null) {
            manager.fireDebugEventSet(new DebugEvent[]{new DebugEvent(this, DebugEvent.TERMINATE)});
        }
    }
    
    /**
     * Fires a custom model specific event when this connector is ready to accept incoming
     * connections from a remote VM.
     */
    protected void fireReadyToAcceptEvent(){
        DebugPlugin manager= DebugPlugin.getDefault();
        if (manager != null) {
            manager.fireDebugEventSet(new DebugEvent[]{new DebugEvent(this, DebugEvent.MODEL_SPECIFIC, IJavaLaunchConfigurationConstants.DETAIL_CONFIG_READY_TO_ACCEPT_REMOTE_VM_CONNECTION)});
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.core.model.IProcess#getStreamsProxy()
     */
    @Override
    public IStreamsProxy getStreamsProxy() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.core.model.IProcess#getAttribute(java.lang.String)
     */
    @Override
    public String getAttribute(String key) {
        return null;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.debug.core.model.IProcess#setAttribute(java.lang.String, java.lang.String)
     */
    @Override
    public void setAttribute(String key, String value) {
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
     */
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return null;
    }

	/**
	 * Return the time since this connector started.
	 */
	private String getRunningTime() {
		long total = System.currentTimeMillis() - fStartTime;
		StringWriter result = new StringWriter();
		PrintWriter writer = new PrintWriter(result);
		int minutes = (int) (total / 60 / 1000);
		int seconds = (int) (total / 1000) % 60;
		int milliseconds = (int) (total / 1000) % 1000;
		writer.printf("%02d:%02d.%03d", minutes, seconds, milliseconds).close();
		return result.toString();
	}


    /**
     * Job that waits for incoming VM connections.  When a remote
     * VM connection is accepted, a debug target is created and 
     * the process that created this job is removed.
     */
    class WaitForConnectionJob extends Job{

        private ListeningConnector fConnector;
        private Map<String, Connector.Argument> fArguments;
        /**
         * Flag that can be set to tell this job that waiting
         * for incoming connections has been cancelled.  If true,
         * IOExceptions will be ignored, allowing other threads
         * to close the socket without generating an error.
         */
        private boolean fListeningStopped = false;
        
        public WaitForConnectionJob(ListeningConnector connector, Map<String, Connector.Argument> arguments) {
            super(getLabel());
            fConnector = connector;
            fArguments = arguments;
        }
        
        @Override
        protected IStatus run(IProgressMonitor monitor) {
            try{
                // The following code sets a timeout (not officially supported in Sun's spec).
                // Allows polling for job cancellation. If the implementation does not support timeout
                // the job cannot be cancelled (but the launch can still be terminated).
                Connector.Argument timeout = fArguments.get("timeout"); //$NON-NLS-1$
                if (timeout != null){
                    timeout.setValue("3000"); //$NON-NLS-1$
                }
                
                VirtualMachine vm = null;
                while (vm == null && !monitor.isCanceled()){
                    try {
                        vm = fConnector.accept(fArguments);
                    } catch (TransportTimeoutException e){
                    }
                }
                
                if (monitor.isCanceled()){
                    fConnector.stopListening(fArguments);
                    return Status.CANCEL_STATUS;
                }
                
                ILaunchConfiguration configuration = fLaunch.getLaunchConfiguration();
                boolean allowTerminate = false;
                if (configuration != null) {
                    try{
                        allowTerminate = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_ALLOW_TERMINATE, false);
                    } catch (CoreException e) {
                        LaunchingPlugin.log(e);
                    }
                }
                Connector.Argument portArg= fArguments.get("port"); //$NON-NLS-1$
                String vmLabel = constructVMLabel(vm, portArg.value(), fLaunch.getLaunchConfiguration());
                IDebugTarget debugTarget= JDIDebugModel.newDebugTarget(fLaunch, vm, vmLabel, null, allowTerminate, true);
                fLaunch.addDebugTarget(debugTarget);
				fAccepted++;
                return Status.OK_STATUS;
            } catch (IOException e) {
                if (fListeningStopped){
                    return Status.CANCEL_STATUS;
                }
                return getStatus(LaunchingMessages.SocketListenConnectorProcess_4, e, IJavaLaunchConfigurationConstants.ERR_REMOTE_VM_CONNECTION_FAILED);
            } catch (IllegalConnectorArgumentsException e) {
                return getStatus(LaunchingMessages.SocketListenConnectorProcess_4, e, IJavaLaunchConfigurationConstants.ERR_REMOTE_VM_CONNECTION_FAILED); 
            }
        }
        
        /* (non-Javadoc)
         * @see org.eclipse.core.runtime.jobs.Job#canceling()
         */
        @Override
        protected void canceling() {
            stopListening();
        }
            
        /**
         * Tells the listening connector to stop listening.  Ensures
         * that the socket is closed and the port released.  Sets a flag
         * so that the IOException thrown by the connector's accept method
         * will be ignored.
         */
        protected void stopListening() {
            if (!fListeningStopped){
                try{
                    fListeningStopped = true;
                    fConnector.stopListening(fArguments);
                } catch (IOException e) {
                    done(getStatus(LaunchingMessages.SocketListenConnectorProcess_5, e, IJavaLaunchConfigurationConstants.ERR_REMOTE_VM_CONNECTION_FAILED)); 
                } catch (IllegalConnectorArgumentsException e) {
                    done(getStatus(LaunchingMessages.SocketListenConnectorProcess_5, e, IJavaLaunchConfigurationConstants.ERR_REMOTE_VM_CONNECTION_FAILED)); 
                }
            }
        }
        
        /**
         * Helper method that constructs a human-readable label for a remote VM.
         * @param vm the VM
         * @param port the port
         * @param configuration the configuration 
         * @return the new VM label
         */
        protected String constructVMLabel(VirtualMachine vm, String port, ILaunchConfiguration configuration) {
            String name = null;
            try {
                name = vm.name();
            } catch (TimeoutException e) {
                // do nothing
            } catch (VMDisconnectedException e) {
                // do nothing
            }
            if (name == null) {
                if (configuration == null) {
                    name = ""; //$NON-NLS-1$
                } else {
                    name = configuration.getName();
                }
            }
            StringBuffer buffer = new StringBuffer(name);
			if (fConnectionLimit != 1) {
				// if we're accepting multiple incoming connections,
				// append the time when each connection was accepted
				buffer.append("<").append(getRunningTime()).append(">");
			}
            buffer.append('['); 
            buffer.append(port);
            buffer.append(']'); 
            return buffer.toString();
        }
    }
}
