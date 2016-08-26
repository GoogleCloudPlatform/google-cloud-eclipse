This fragment provides a variant of the Eclipse JDT/Debug
SocketListenConnector that supports listening for multiple incoming
connections.  Google Cloud's `dev_appserver`, a web container for
local development and debugging of App Engine projects, may launch
multiple JVMs and the same `--jvm_flags` are provided to each JVM.
Thus when debugging an App Engine application, we may have multiple
JVMs trying to connect to the Eclipse JDT Debugger.

These patches have been submitted upstream and are tracked as:

    Bug 499385 - Socket Listen mode should allow multiple connections
    https://bugs.eclipse.org/bugs/show_bug.cgi?id=499385

This fragment is currently conditioned on Eclipse 4.5 (Mars) and
Eclipse 4.6 (Neon) as we assume the functionality will be merged for
Eclipse 4.7 (Oxygen).
