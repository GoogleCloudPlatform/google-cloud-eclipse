package com.google.cloud.tools.eclipse.util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;

public class ResourceUtils {

  /**
   * Get the file being edited by a given editor. Note that this method can
   * return null if the specified editor isn't an IFileEditor.
   */
  public static IFile getEditorInput(IEditorPart editor) {
    IFileEditorInput fileEditorInput = AdapterUtilities.getAdapter(editor.getEditorInput(),
                                                                   IFileEditorInput.class);
    if (fileEditorInput != null) {
      return fileEditorInput.getFile();
    }
    return null;
  }

  /**
   * Get the resource pointed at by the specified selection. Note that this
   * method can return null in the case that the selection is empty, or it isn't
   * an IStructuredSelection.
   */
  public static IResource getSelectionResource(ISelection selection) {
    if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
      Object element = ((IStructuredSelection) selection).getFirstElement();
      return AdapterUtilities.getAdapter(element, IResource.class);
    }
    return null;
  }
}
