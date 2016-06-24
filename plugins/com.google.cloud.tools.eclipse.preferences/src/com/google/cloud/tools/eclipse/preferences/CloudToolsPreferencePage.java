package com.google.cloud.tools.eclipse.preferences;

import org.eclipse.jface.preference.*;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;

/**
 * A preference page that is contributed to the
 * Preferences dialog. By subclassing <code>FieldEditorPreferencePage</code>, we
 * use the field support built into JFace to create a page
 * that knows how to save, restore, and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 */
public class CloudToolsPreferencePage extends FieldEditorPreferencePage
    implements IWorkbenchPreferencePage {

  public static final String ANALYTICS_OPT_IN = "ANALYTICS_OPT_IN";
  public static final String ANALYTICS_OPT_IN_REGISTERED = "ANALYTICS_OPT_IN_REGISTERED";
  public static final String ANALYTICS_CLIENT_ID = "ANALYTICS_CLIENT_ID";

  public CloudToolsPreferencePage() {
    super(GRID);
    setPreferenceStore(Activator.getDefault().getPreferenceStore());
  }

  @Override
  public void createFieldEditors() {
    addField(new AnalyticsOptInFieldEditor(ANALYTICS_OPT_IN, getFieldEditorParent()));
  }

  @Override
  public void init(IWorkbench workbench) {
  }

}