package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.databinding.dialog.TitleAreaDialogSupport;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;

import com.google.cloud.tools.eclipse.appengine.deploy.ui.DeployPreferencesPanel.AdvancedSectionExpansionHandler;
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
  protected Control createContents(Composite parent) {
    Control contents = super.createContents(parent);

    String title = getTitleString();
    setTitle(title);
    getShell().setText(title);
    if (titleImage != null) {
      setTitleImage(titleImage);
    }

    return contents;
  }
  
  @Override
  protected Control createDialogArea(final Composite parent) {
    Composite dialogArea = (Composite) super.createDialogArea(parent);
    content = new DeployPreferencesPanel(dialogArea, project, getExpansionHandler());
    GridDataFactory.fillDefaults().grab(true, false).applyTo(content);
    TitleAreaDialogSupport.create(this, content.getDataBindingContext());
    return dialogArea;
  }
  
  private AdvancedSectionExpansionHandler getExpansionHandler() {
    return new AdvancedSectionExpansionHandler() {

      @Override
      public void handleExpansionEvent(ExpansionEvent e) {
        Shell shell = getShell();
        shell.setMinimumSize(shell.getSize().x, 0);
        shell.pack();
        ((ExpandableComposite) e.getSource()).getParent().layout();
        shell.setMinimumSize(shell.getSize());
      }
    };
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

  @Override
  public boolean isHelpAvailable() {
    return false;
  }
}
