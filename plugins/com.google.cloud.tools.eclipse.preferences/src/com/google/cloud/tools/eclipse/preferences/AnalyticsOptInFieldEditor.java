package com.google.cloud.tools.eclipse.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Link;

public class AnalyticsOptInFieldEditor extends FieldEditor {

  private Group group;
  private BooleanFieldEditor optInStatusEditor;

  /**
   * @param name the name of the preference this field editor works on
   * @param parent the parent of the field editor's control
   */
  public AnalyticsOptInFieldEditor(String name, Composite parent) {
    group = new Group(parent, SWT.SHADOW_OUT);
    group.setText(Messages.FIELD_EDITOR_ANALYTICS_GROUP_TITLE);

    optInStatusEditor =
        new BooleanFieldEditor(name, Messages.FIELD_EDITOR_ANALYTICS_OPT_IN_TEXT, group);

    Link link = new Link(group, SWT.NONE);
    link.setText(Messages.FIELD_EDITOR_ANALYTICS_DISCLAIMER);

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
