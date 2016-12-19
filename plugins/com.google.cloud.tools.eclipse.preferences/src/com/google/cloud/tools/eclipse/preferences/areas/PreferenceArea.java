/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.preferences.areas;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;

/**
 * Abstract base class for preference areas. A preference area is somewhat like a
 * {@link org.eclipse.jface.preference.FieldEditor FieldEditor} except that it is expected to be
 * configured from an extension point.
 * <p>
 * Preference areas should notify its listeners of changes using property change events.
 */
public abstract class PreferenceArea {
  /* property values */
  public static final String IS_VALID = "area_is_valid";
  public static final String VALUE = "area_value";

  private IPreferenceStore preferences;

  /**
   * Listener, or <code>null</code> if none.
   */
  private IPropertyChangeListener propertyChangeListener;

  /** The associated workbench. */
  private IWorkbench workbench;

  /** The area title; may be {@code null}. */
  private String title;

  /** The area rank, used to sort/order areas within a page. */
  private int rank = Integer.MAX_VALUE;

  protected PreferenceArea() {}

  /**
   * Create the contents of the preference area.
   * 
   * @param parent the container
   * @return the central control
   */
  public abstract Control createContents(Composite parent);

  /** Destroy any resources held onto by this instance. */
  public void dispose() {}

  public IPropertyChangeListener getPropertyChangeListener() {
    return propertyChangeListener;
  }

  public void setPropertyChangeListener(IPropertyChangeListener propertyChangeListener) {
    this.propertyChangeListener = propertyChangeListener;
  }

  /** Provides the specified preferences. */
  public IPreferenceStore getPreferenceStore() {
    return preferences;
  }

  /** Provides the specified preferences node. */
  public void setPreferenceStore(IPreferenceStore preferences) {
    this.preferences = preferences;
  }

  /**
   * Return a status representing the validity of the area contents. The status message may be shown
   * to the user in UI.
   * 
   * @return the status
   */
  public abstract IStatus getStatus();

  /**
   * Initialize the UI based on values loaded from the preference store.
   */
  public abstract void load();

  /**
   * Initialize the UI based on default values.
   */
  public abstract void loadDefault();

  public abstract void performApply();

  /**
   * Undo any changes that might have been made.
   * 
   * @return true if changes are undone, false if cancel is not possible
   */
  public boolean performCancel() {
    return true;
  }

  /**
   * Fire an event to notify of a property change.
   * 
   * @see #IS_VALID
   * @see #VALUE
   */
  protected void fireValueChanged(String property, Object oldValue, Object newValue) {
    if (propertyChangeListener != null) {
      propertyChangeListener
          .propertyChange(new PropertyChangeEvent(this, property, oldValue, newValue));
    }
  }

  /** Return the associated workbench. */
  public IWorkbench getWorkbench() {
    return workbench;
  }

  /**
   * Set the associated workbench.
   * 
   * @param workbench the current workbench
   */
  public void setWorkbench(IWorkbench workbench) {
    this.workbench = workbench;
  }

  /**
   * Return the area title.
   * 
   * @return the area title or {@code null} if none
   */
  public String getTitle() {
    return title;
  }

  /**
   * Set the area title. Changing the value has no effect once the area has been rendered.
   * 
   * @param title the new title or {@code null} if none
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /** Return this area's rank, used to order areas on a page. */
  public int getRank() {
    return rank;
  }

  /** Set this area's rank, used to order areas on a page. */
  public void setRank(int rank) {
    this.rank = rank;
  }

}
