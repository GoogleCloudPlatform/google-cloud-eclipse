/*
 * Copyright 2018 Google LLC
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

package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import com.google.api.client.util.Preconditions;
import com.google.cloud.tools.eclipse.appengine.facets.WebProjectUtil;
import com.google.cloud.tools.eclipse.appengine.localserver.server.DatastoreIndexUpdateData;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

/**
 * Notify the user that a {@code datastore-indexes-auto.xml} was found on termination of the server.
 */
public class DatastoreIndexesUpdatedStatusHandler implements IStatusHandler {
  private static final Logger logger =
      Logger.getLogger(DatastoreIndexesUpdatedStatusHandler.class.getName());

  /**
   * The error code indicating that the {@code datastore-indexes-auto.xml} file was present. Used
   * with the plugin ID to uniquely identify this prompter.
   */
  static final int DATASTORE_INDEXES_AUTO_CODE = 256;

  /**
   * A specially crafted status message that is passed into the Debug Prompter class to obtain our
   * {@code datastore-indexes} notification prompter.
   */
  public static final IStatus DATASTORE_INDEXES_UPDATED =
      new Status(IStatus.INFO, "com.google.cloud.tools.eclipse.appengine.localserver",
          DATASTORE_INDEXES_AUTO_CODE, "", null);

  private static final String EMPTY_DATASTORE_INDEXES_XML =
      "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<datastore-indexes autoGenerate=\"true\">\n"
          + "</datastore-indexes>";

  @Override
  public Object handleStatus(IStatus status, Object source) throws CoreException {
    Preconditions.checkArgument(source instanceof DatastoreIndexUpdateData);
    DatastoreIndexUpdateData update = (DatastoreIndexUpdateData) source;
    Preconditions.checkState(update.datastoreIndexesAutoXml != null);

    if (DebugUITools.isPrivate(update.configuration)) {
      return null;
    }

    Shell activeShell = DebugUIPlugin.getShell();
    // should consider using MessageDialogWithToggle? if original doesn't exist, should we copy into
    // place?
    boolean review = MessageDialog.openQuestion(activeShell, "Detected Datastore Indexes Update",
        "The Development App Server has generated an update for the Datastore Indexes (datastore-indexes.xml). "
            + "Would you like to review and merge the changes?");
    if (review) {
      IWorkbenchPage page = getActivePage();
      IFile datastoreIndexesXml = update.datastoreIndexesXml;

      // create an empty source file if it doesn't exist
      if(datastoreIndexesXml == null) {
        IProject project = update.module.getProject();
        try (InputStream contents =
            new ByteArrayInputStream(
                EMPTY_DATASTORE_INDEXES_XML.getBytes(StandardCharsets.UTF_8))) {
          datastoreIndexesXml = WebProjectUtil.createFileInWebInf(project,
              new Path("datastore-indexes.xml"), contents, null /* monitor */);
        } catch (IOException ex) {
          logger.log(Level.SEVERE, "could not create empty datastore-indexes.xml in " + project,
              ex);
        }
      }
      // todo: open a compare editor
      IDE.openEditor(page, update.datastoreIndexesXml);
      IFileStore autoXml =
          EFS.getLocalFileSystem().fromLocalFile(update.datastoreIndexesAutoXml.toFile());
      IDE.openEditorOnFileStore(page, autoXml);
    }
    return null;
  }


  private IWorkbenchPage getActivePage() {
    IWorkbench workbench = PlatformUI.getWorkbench();
    Preconditions.checkState(workbench.getWorkbenchWindowCount() > 0);
    IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
    if (window == null) {
      window = workbench.getWorkbenchWindows()[0];
    }
    return window.getActivePage();
  }

}
