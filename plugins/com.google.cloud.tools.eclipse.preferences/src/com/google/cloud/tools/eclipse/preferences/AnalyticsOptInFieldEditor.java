package com.google.cloud.tools.eclipse.preferences;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

public class AnalyticsOptInFieldEditor extends FieldEditor {

  private static final Logger logger = Logger.getLogger(AnalyticsOptInFieldEditor.class.getName());

  private Group group;
  private BooleanFieldEditor optInStatusEditor;

  /**
   * @param name the name of the preference this field editor works on
   * @param parent the parent of the field editor's control
   */
  public AnalyticsOptInFieldEditor(String name, Composite parent) {
    group = new Group(parent, SWT.SHADOW_OUT);
    group.setText(Messages.ANALYTICS_PREFERENCE_GROUP_TITLE);

    // Opt-in checkbox with a label
    optInStatusEditor = new BooleanFieldEditor(name, Messages.ANALYTICS_OPT_IN_TEXT, group);

    // The privacy policy disclaimer with a clickable link
    Link link = new Link(group, SWT.NONE);
    link.setText(Messages.ANALYTICS_DISCLAIMER);
    link.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        // Open a privacy policy web page when the link is clicked.
        try {
          URL url = new URL(Messages.GOOGLE_PRIVACY_POLICY_URL);
          IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
          browserSupport.createBrowser(null).openURL(url);
        } catch (MalformedURLException mue) {
          logger.log(Level.WARNING, "URL malformed", mue);
        } catch (PartInitException pie) {
          logger.log(Level.WARNING, "Cannot launch a browser", pie);
        }
      }
    });

    // Initialize and set up this object.
    init(name, "labelless field editor");
    createControl(parent);
  }

  @Override
  public void setPreferenceStore(IPreferenceStore store) {
    super.setPreferenceStore(store);
    optInStatusEditor.setPreferenceStore(store);
  }

  @Override
  public int getNumberOfControls() {
    return 1;
  }

  @Override
  protected void adjustForNumColumns(int numColumns) {
    ((GridData) group.getLayoutData()).horizontalSpan = numColumns;
  }

  @Override
  protected void doFillIntoGrid(Composite parent, int numColumns) {
    GridData gridData = new GridData();
    gridData.horizontalSpan = numColumns;
    group.setLayoutData(gridData);

    GridLayoutFactory.swtDefaults().applyTo(group);
  }

  @Override
  protected void doLoad() {
    optInStatusEditor.load();
  }

  @Override
  protected void doLoadDefault() {
    optInStatusEditor.loadDefault();
  }

  @Override
  protected void doStore() {
    optInStatusEditor.store();
  }

}
