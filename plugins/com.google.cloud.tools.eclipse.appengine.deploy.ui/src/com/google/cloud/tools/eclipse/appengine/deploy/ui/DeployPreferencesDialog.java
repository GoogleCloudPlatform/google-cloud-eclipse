package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import org.eclipse.core.databinding.ValidationStatusProvider;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.databinding.dialog.TitleAreaDialogSupport;
import org.eclipse.jface.databinding.dialog.ValidationMessageProvider;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;
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

    // TitleAreaDialogSupport does not validate initially, let's trigger validation this way
    content.getDataBindingContext().updateTargets();

    return contents;
  }
  
  @Override
  protected Control createDialogArea(final Composite parent) {
    Composite dialogArea = (Composite) super.createDialogArea(parent);
    setMargin(dialogArea);
    content = new DeployPreferencesPanel(dialogArea, project, getExpansionHandler());
    GridDataFactory.fillDefaults().grab(true, false).applyTo(content);
    TitleAreaDialogSupport.create(this, content.getDataBindingContext()).setValidationMessageProvider(new ValidationMessageProvider() {
      @Override
      public int getMessageType(ValidationStatusProvider statusProvider) {
        int type = super.getMessageType(statusProvider);
        Button okButton = getButton(IDialogConstants.OK_ID);
        if (okButton != null) {
          okButton.setEnabled(type != IMessageProvider.ERROR);
        }
        return type;
      }
    });
    return dialogArea;
  }

  private void setMargin(Composite dialogArea) {
    Layout layout = dialogArea.getLayout();
    if (layout instanceof GridLayout) {
      ((GridLayout)layout).marginWidth = 5;
    }
  }
  
  private AdvancedSectionExpansionHandler getExpansionHandler() {
    return new AdvancedSectionExpansionHandler() {

      @Override
      public void handleExpansionEvent(ExpansionEvent event) {
        Shell shell = getShell();
        shell.setMinimumSize(shell.getSize().x, 0);
        shell.pack();
        ((ExpandableComposite) event.getSource()).getParent().layout();
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
