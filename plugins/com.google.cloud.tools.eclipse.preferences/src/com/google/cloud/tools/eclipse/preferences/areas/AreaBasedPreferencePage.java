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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A host preference page that hosts multiple <em>preference areas</em>. These
 * {@link PreferenceArea} are like embedded preference pages, like more complex JFace
 * {@link org.eclipse.jface.preference.FieldEditor FieldEditor.
 */
public class AreaBasedPreferencePage extends PreferencePage
    implements IWorkbenchPreferencePage, IExecutableExtension {
  /**
   * Responsible for ordering preference areas by their rank.
   */
  public static class AreaOrdering implements Comparator<PreferenceArea> {
    @Override
    public int compare(PreferenceArea o1, PreferenceArea o2) {
      return o1.getRank() - o2.getRank();
    }
  }

  /**
   * Builds the contents from an extension point. Could consider making this a standalone
   * IExtensionFactory that constructs and populates an {@linkplain AreaBasedPreferencePage}.
   */
  private class ExtensionBuilder {
    private static final String PREFAREA_EXTENSION_POINT =
        "com.google.cloud.tools.eclipse.preferences.areas";
    private static final String NAME_AREA = "area";
    private static final String ATTR_HOST_PAGE_ID = "host";
    private static final String ATTR_TITLE = "title";
    private static final String ATTR_PREF_PATH = "preferences";
    private static final String ATTR_CLASS = "class";
    private static final String ATTR_RANK = "rank";

    private static final String ATTR_ID = "id";

    private void build() {
      for (IConfigurationElement element : getRegistry()
          .getConfigurationElementsFor(PREFAREA_EXTENSION_POINT)) {
        if (element.getName().equals(NAME_AREA)
            && pageId.equals(element.getAttribute(ATTR_HOST_PAGE_ID))) {
          try {
            PreferenceArea area = (PreferenceArea) element.createExecutableExtension(ATTR_CLASS);
            IPreferenceStore store = resolvePreferenceStore(element.getAttribute(ATTR_PREF_PATH));
            if (element.getAttribute(ATTR_TITLE) != null) {
              area.setTitle(element.getAttribute(ATTR_TITLE));
            }
            if (element.getAttribute(ATTR_RANK) != null) {
              area.setRank(Integer.parseInt(element.getAttribute(ATTR_RANK)));
            }
            if (store != null) {
              area.setPreferenceStore(store);
            }
            areas.add(area);
          } catch (CoreException ex) {
            logger.log(Level.SEVERE,
                "Unable to create " + element.getAttribute(ATTR_CLASS) + " for page " + pageId, ex);
          } catch (ClassCastException ex) {
            logger.log(Level.SEVERE, "Class " + element.getAttribute(ATTR_CLASS) + " must extend "
                + PreferenceArea.class, ex);
          } catch (NumberFormatException ex) {
            logger.log(Level.SEVERE,
                "Preference area rank '" + element.getAttribute(ATTR_RANK) + "' is not an integer",
                ex);
          }
        }
      }
    }
  }

  private static final Logger logger = Logger.getLogger(AreaBasedPreferencePage.class.getName());

  private String pageId;

  private IWorkbench workbench;

  private List<PreferenceArea> areas = new ArrayList<>();

  private IPropertyChangeListener propertyChangeListener = new IPropertyChangeListener() {
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      update();
    }
  };

  /**
   * Create new instance. {@linkplain AreaBasedPreferencePage}s require a pageId to be referenced by
   * different {@link PreferenceArea}s.
   * 
   * @param pageId the identifier of this page
   */
  public AreaBasedPreferencePage(String pageId) {
    this.pageId = pageId;
  }

  /**
   * 0-argument constructor required by the Eclipse Extension Registry. Not intended for normal use.
   * 
   * @see AreaBasedPreferencePage(String)
   * @noreference use {@link AreaBasedPreferencePage(String)} instead
   */
  public AreaBasedPreferencePage() {}

  @Override
  public void init(IWorkbench workbench) {
    this.workbench = workbench;
  }

  @Override
  public void setInitializationData(IConfigurationElement configElement, String propertyName,
      Object data) throws CoreException {
    if (configElement.getAttribute(ExtensionBuilder.ATTR_ID) == null) {
      throw new CoreException(new Status(IStatus.ERROR, configElement.getNamespaceIdentifier(),
          "Missing " + ExtensionBuilder.ATTR_HOST_PAGE_ID));
    }
    pageId = configElement.getAttribute(ExtensionBuilder.ATTR_ID);
    new ExtensionBuilder().build();
  }

  public void addArea(PreferenceArea area) {
    areas.add(area);
  }

  public void removeArea(PreferenceArea area) {
    areas.remove(area);
  }

  /** Return the ordered set of preference areas. */
  public List<PreferenceArea> getAreas() {
    List<PreferenceArea> copy = new ArrayList<>(areas);
    Collections.sort(copy, new AreaOrdering());
    return copy;
  }

  @Override
  protected final Control createContents(Composite parent) {
    if (pageId == null) {
      throw new IllegalStateException("No page id");
    }
    Collections.sort(areas, new AreaOrdering());
    Composite container = new Composite(parent, SWT.NONE);
    for (PreferenceArea area : areas) {
      // configure the area
      if (workbench != null) {
        area.setWorkbench(workbench);
      }

      // render the contents
      Composite contents;
      if (area.getTitle() == null) {
        contents = new Composite(container, SWT.BORDER);
      } else {
        contents = new Group(container, SWT.NONE);
        ((Group) contents).setText(area.getTitle());
      }
      area.createContents(contents);
      GridLayoutFactory.swtDefaults().generateLayout(contents);

      // load the preferences
      area.setPropertyChangeListener(propertyChangeListener);
      area.load();
    }
    // apply extra space around areas
    GridLayoutFactory.swtDefaults().spacing(5, 10).generateLayout(container);
    parent.layout(true, true);
    return container;
  }

  /**
   * Update the page messaging based on the provided status.
   * 
   * @param status the status to be shown
   */
  private void show(IStatus status) {
    switch (status.getSeverity()) {
      case IStatus.ERROR:
        setMessage(status.getMessage(), ERROR);
        return;
      case IStatus.WARNING:
        setMessage(status.getMessage(), WARNING);
        return;
      case IStatus.INFO:
        setMessage(status.getMessage(), INFORMATION);
        return;
      default:
        setMessage(null);
        return;
    }
  }

  @Override
  public boolean performOk() {
    for (PreferenceArea area : areas) {
      area.performApply();
      if (area.getPreferenceStore() instanceof IPersistentPreferenceStore) {
        try {
          ((IPersistentPreferenceStore) area.getPreferenceStore()).save();
        } catch (IOException ex) {
          logger.log(Level.SEVERE, "Unable to persist preferences for " + area, ex);
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public boolean performCancel() {
    for (PreferenceArea area : areas) {
      if (!area.performCancel()) {
        return false;
      }
    }
    return super.performCancel();
  }

  @Override
  protected void performDefaults() {
    for (PreferenceArea area : areas) {
      area.loadDefault();
    }
    super.performDefaults();
  }

  @Override
  public void dispose() {
    for (PreferenceArea area : areas) {
      area.dispose();
    }
    super.dispose();
  }

  private IPreferenceStore resolvePreferenceStore(String preferencePath) {
    try {
      URI uri = new URI(preferencePath);
      return PreferenceResolver.resolve(uri);
    } catch (URISyntaxException | IllegalArgumentException ex) {
      logger.severe("Invalid preference specification: " + preferencePath);
      return null;
    }
  }

  private IExtensionRegistry getRegistry() {
    return RegistryFactory.getRegistry();
  }

  protected void update() {
    IStatus severest = Status.OK_STATUS;
    for (PreferenceArea area : areas) {
      IStatus status = area.getStatus();
      if (status.getSeverity() > severest.getSeverity()) {
        severest = status;
      }
    }
    setValid(severest.getSeverity() != IStatus.ERROR);
    show(severest);
  }
}
