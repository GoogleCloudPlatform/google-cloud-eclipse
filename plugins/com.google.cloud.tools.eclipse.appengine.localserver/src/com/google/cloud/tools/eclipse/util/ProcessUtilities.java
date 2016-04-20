package com.google.cloud.tools.eclipse.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.google.cloud.tools.eclipse.appengine.localserver.Activator;
import com.google.common.io.ByteStreams;

public class ProcessUtilities {

  /**
   * Launch the process specified in the commands and wait for it to terminate.
   * 
   * @param commands commands to pass to the {@link ProcessBuilder}
   * @param workingDir directory to use as the working directory
   * @param outputStream output stream to receive process output
   * @return process exit code
   */
  public static int launchProcessAndWaitFor(List<String> commands,
                                            File workingDir,
                                            final OutputStream outputStream) throws InterruptedException, IOException {
    return launchProcessAndWaitFor(commands, workingDir, null, outputStream);
  }

  /**
   * Launch the process specified in the commands and wait for it to terminate.
   * 
   * @param commands commands to pass to the {@link ProcessBuilder}
   * @param workingDir directory to use as the working directory
   * @param additionalPaths list of additional directories to be appended to the
   *          PATH environment variable
   * @param outputStream output stream to receive process output
   * @return process exit code
   */
  public static int launchProcessAndWaitFor(List<String> commands,
                                            File workingDir,
                                            final List<String> additionalPaths,
                                            final OutputStream outputStream) throws InterruptedException, IOException {
    ProcessBuilder pb = new ProcessBuilder(commands);
    pb.directory(workingDir);
    pb.redirectErrorStream(true);
    moveClasspathArgToEnvironmentVariable(commands, pb);

    // if given a non-null, non-empty list of paths, then append to the PATH
    // environment variable
    if (additionalPaths != null && additionalPaths.size() >= 1) {
      StringBuilder newPathEnvVar = new StringBuilder(pb.environment().get("PATH"));
      // for each additional path, add it- if it isn't already in the path list
      for (String path : additionalPaths) {
        // if this additional path isn't already in the new path environment
        // variable
        String[] existingPaths = newPathEnvVar.toString()
                                              .split(java.io.File.pathSeparatorChar + "");
        boolean pathAlreadyInPATH = false;
        for (String existingPath : existingPaths) {
          if (path.equals(existingPath)) {
            pathAlreadyInPATH = true;
          }
        }
        if (!pathAlreadyInPATH) {
          // append the new path onto newPathEnvVar
          newPathEnvVar.append(java.io.File.pathSeparatorChar);
          newPathEnvVar.append(path);
        }
      }
      // finally, set the environment variable on the process builder
      pb.environment().put("PATH", newPathEnvVar.toString());
    }

    Process process = null;
    Thread t = null;
    int processExitCode = -1;

    try {
      process = pb.start();

      // Local class to read the output of the process and write it to the
      // output stream
      class OutputPump implements Runnable {

        Process process;

        OutputPump(Process p) {
          process = p;
        }

        @Override
        public void run() {
          InputStream inputStream = process.getInputStream();
          try {
            int bytesRead = 0;
            byte[] buf = new byte[1024];
            while ((bytesRead = inputStream.read(buf)) != -1) {
              outputStream.write(buf, 0, bytesRead);
            }
          } catch (IOException e) {
            // The "Stream closed" exception is common when we destroy the
            // process (e.g. when the user requests to terminate a GWT compile)
            if (!e.getMessage().contains("Stream closed")) {
              Activator.getDefault()
                       .getLog()
                       .log(new Status(IStatus.ERROR,
                                       Activator.PLUGIN_ID,
                                       e.getLocalizedMessage(),
                                       e));
            }
          }
        }
      }
      t = new Thread(new OutputPump(process), "Process Output Pump");
      t.start();

      // Wait for process to complete
      processExitCode = process.waitFor();
    } catch (InterruptedException ie) {
      /*
       * If the thread that called this method is interrupted while waiting for
       * process.waitFor to return, ensure that the process is terminated and
       * that its file handles have been cleaned up.
       */
      cleanupProcess(process);

      // Rethrow the original exception
      throw ie;
    } finally {
      if (t != null) {
        // Wait for this thread to complete before returning from the method.
        try {
          t.join();
          // Close all of the process' streams, and destroy the process
          cleanupProcess(process);
        } catch (InterruptedException e) {
          Activator.getDefault()
                   .getLog()
                   .log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getLocalizedMessage(), e));
        }
      }
    }

    return processExitCode;
  }

  /**
   * Closes the process' input stream, output stream, and error stream, and
   * finally destroys the process by calling {@code destroy()}.
   * 
   * @param p the process to cleanup
   */
  private static void cleanupProcess(Process p) {
    if (p == null) {
      return;
    }

    try {
      if (p.getInputStream() != null) {
        p.getInputStream().close();
      }
    } catch (IOException e) {
      Activator.getDefault()
               .getLog()
               .log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getLocalizedMessage(), e));
    }

    try {
      if (p.getOutputStream() != null) {
        p.getOutputStream().close();
      }
    } catch (IOException e) {
      Activator.getDefault()
               .getLog()
               .log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getLocalizedMessage(), e));
    }

    try {
      if (p.getErrorStream() != null) {
        p.getErrorStream().close();
      }
    } catch (IOException e) {
      Activator.getDefault()
               .getLog()
               .log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getLocalizedMessage(), e));
    }

    p.destroy();
  }

  /*
   * Put classpath argument in an environment variable so we don't overflow the
   * process command-line buffer on Windows.
   */
  private static void moveClasspathArgToEnvironmentVariable(List<String> commandArgs,
                                                            ProcessBuilder pb) {
    int cpFlagIndex = commandArgs.indexOf("-cp");
    if (cpFlagIndex == -1) {
      cpFlagIndex = commandArgs.indexOf("-classpath");
    }

    if (cpFlagIndex > -1 && cpFlagIndex < commandArgs.size() - 1) {
      Map<String, String> env = pb.environment();
      String classpath = commandArgs.get(cpFlagIndex + 1);
      env.put("CLASSPATH", classpath);

      commandArgs.remove(cpFlagIndex);
      commandArgs.remove(cpFlagIndex);
    }
  }

  /**
   * Note: this method waits until the completion of the process.
   */
  public static String getProcessOutput(Process process) throws IOException, InterruptedException {
    process.waitFor();
    return copyInputStreamToString(process.getInputStream());
  }

  public static String getProcessErrorOutput(Process process) throws IOException,
                                                              InterruptedException {
    process.waitFor();
    return copyInputStreamToString(process.getErrorStream());
  }

  private static String copyInputStreamToString(InputStream inputStream) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ByteStreams.copy(inputStream, bos);
    return new String(bos.toByteArray(), StandardCharsets.UTF_8);
  }
}
