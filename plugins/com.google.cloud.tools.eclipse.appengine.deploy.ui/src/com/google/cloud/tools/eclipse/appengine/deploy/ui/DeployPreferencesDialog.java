package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.databinding.dialog.TitleAreaDialogSupport;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.google.cloud.tools.eclipse.appengine.ui.AppEngineImages;

public class DeployPreferencesDialog extends TitleAreaDialog {

  // if the image is smaller (e.g. 32x32, it will break the layout of the TitleAreaDialog)
  // seems like an Eclipse/JFace bug
  private Image titleImage = AppEngineImages.appEngine(64).createImage();

  private DeployPreferencesPanel content;
  private IProject project;

  public DeployPreferencesDialog(Shell parentShell, IProject project) {
    super(parentShell);
    this.project = project;
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText(getTitleString());
  }

  @Override
  protected Control createContents(Composite parent) {
    Control contents = super.createContents(parent);

    setTitle(getTitleString());
    if (titleImage != null) {
      setTitleImage(titleImage);
    }

    return contents;
  }
  
  @Override
  protected Control createDialogArea(final Composite parent) {
    Composite dialogArea = (Composite) super.createDialogArea(parent);
    content = new DeployPreferencesPanel(dialogArea, project);
    content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    TitleAreaDialogSupport.create(this, content.getDataBindingContext());
    return dialogArea;
  }
  
  private String getTitleString() {
    return Messages.getString("deploy.preferences.dialog.title");
  }

  @Override
  protected void okPressed() {
    content.savePreferences();
    super.okPressed();
  }

  @Override
  public boolean close() {
    titleImage.dispose();
    return super.close();
  }
}
