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
import com.google.cloud.tools.eclipse.appengine.localserver.Messages;
import com.google.cloud.tools.eclipse.appengine.localserver.server.DatastoreIndexUpdateData;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.compare.BufferedContent;
import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.ui.synchronize.SaveableCompareEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

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
      new Status(IStatus.INFO, "com.google.cloud.tools.eclipse.appengine.localserver", //$NON-NLS-1$
          DATASTORE_INDEXES_AUTO_CODE, "", null); //$NON-NLS-1$

  private static final String EMPTY_DATASTORE_INDEXES_XML =
      "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<datastore-indexes autoGenerate=\"true\">\n" //$NON-NLS-1$ //$NON-NLS-2$
          + "</datastore-indexes>"; //$NON-NLS-1$

  @Override
  public Object handleStatus(IStatus status, Object source) throws CoreException {
    Preconditions.checkArgument(source instanceof DatastoreIndexUpdateData);
    DatastoreIndexUpdateData update = (DatastoreIndexUpdateData) source;
    Preconditions.checkState(update.datastoreIndexesAutoXml != null);

    if (DebugUITools.isPrivate(update.configuration)) {
      return null;
    }

    IWorkbenchPage page = getActivePage();
    Shell shell = page.getWorkbenchWindow().getShell();
    String[] buttonLabels =
        new String[] {Messages.getString("REVIEW_CHANGES"), Messages.getString("IGNORE_CHANGES")};
    MessageDialog dialog =
        new MessageDialog(shell, Messages.getString("REVIEW_DATASTORE_INDEXES_UPDATE_TITLE"), //$NON-NLS-1$
            null, Messages.getString("REVIEW_DATASTORE_INDEXES_UPDATE_MESSAGE"), //$NON-NLS-1$
            MessageDialog.QUESTION, 0, buttonLabels);
    if (dialog.open() != 0) {
      return null;
    }

    // create an empty source file if it doesn't exist
    IFile datastoreIndexesXml = update.datastoreIndexesXml;
    if (datastoreIndexesXml == null) {
      IProject project = update.module.getProject();
      try {
        datastoreIndexesXml = createNewDatastoreIndexesXml(project, null /* monitor */);
      } catch (CoreException ex) {
        logger.log(Level.SEVERE, "could not create empty datastore-indexes.xml in " + project, ex); //$NON-NLS-1$
      }
    }

    CompareEditorInput input = new GeneratedDatastoreIndexesUpdateEditorInput(page,
        datastoreIndexesXml, update.datastoreIndexesAutoXml.toFile());
    CompareUI.openCompareEditor(input);
    return null;
  }


  /** Create a new empty {@code datastore-indexes.xml} for the given project. */
  private IFile createNewDatastoreIndexesXml(IProject project, IProgressMonitor monitor)
      throws CoreException {
    InputStream contents =
        new ByteArrayInputStream(EMPTY_DATASTORE_INDEXES_XML.getBytes(StandardCharsets.UTF_8));
    IFile datastoreIndexesXml =
        WebProjectUtil.createFileInWebInf(
            project,
            new Path("datastore-indexes.xml"), //$NON-NLS-1$
            contents,
            false /* overwrite */,
            monitor);
    return datastoreIndexesXml;
  }

  /** Find the current window page; should never be {@code null}. */
  private IWorkbenchPage getActivePage() {
    IWorkbench workbench = PlatformUI.getWorkbench();
    Preconditions.checkState(workbench.getWorkbenchWindowCount() > 0);
    IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
    if (window == null) {
      window = workbench.getWorkbenchWindows()[0];
    }
    return window.getActivePage();
  }

  /**
   * Compares a {@code datastore-indexes.xml} file to the devappserver-generated
   * {@code datastore-indexes-auto.xml} file, that may have been updated with additional entities
   * and properties.
   * 
   * TODO: configure a structured comparator for copying the new entities and properties?
   */
  static class GeneratedDatastoreIndexesUpdateEditorInput extends SaveableCompareEditorInput {
    private IFile source;
    private File generatedUpdate;

    public GeneratedDatastoreIndexesUpdateEditorInput(IWorkbenchPage page, IFile source,
        File generatedUpdate) {
      super(new CompareConfiguration(), page);
      this.source = source;
      this.generatedUpdate = generatedUpdate;
    }

    @Override
    protected ICompareInput prepareCompareInput(IProgressMonitor monitor)
        throws InvocationTargetException, InterruptedException {
      CompareConfiguration cc = getCompareConfiguration();
      cc.setLeftEditable(true);
      cc.setRightEditable(false);
      cc.setLeftLabel(source.getName());
      cc.setLeftImage(CompareUI.getImage(source));
      cc.setRightLabel(generatedUpdate.getName());
      cc.setRightImage(CompareUI.getImage(source)); // same type
      ITypedElement left = SaveableCompareEditorInput.createFileElement(source);
      ITypedElement right = new LocalDiskContent(generatedUpdate);
      return new DiffNode(left, right);
    }

    @Override
    public boolean canRunAsJob() {
      return true;
    }

    @Override
    protected void fireInputChange() {}

    /**
     * Represents fixed on-disk file content.
     */
    static class LocalDiskContent extends BufferedContent implements ITypedElement {
      private final File source;

      public LocalDiskContent(File source) {
        this.source = source;
      }

      @Override
      protected InputStream createStream() throws CoreException {
        try {
          return new FileInputStream(source);
        } catch (FileNotFoundException ex) {
          throw new CoreException(StatusUtil.error(this, ex.getMessage()));
        }
      }

      @Override
      public String getName() {
        return source.getName();
      }

      @Override
      public Image getImage() {
        return null;
      }

      @Override
      public String getType() {
        return ITypedElement.TEXT_TYPE;
      }
    }
  }
}
