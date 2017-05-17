/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.ui.launcher;

import com.google.cloud.tools.eclipse.dataflow.core.launcher.options.PipelineOptionsType;
import com.google.cloud.tools.eclipse.dataflow.ui.DataflowUiPlugin;
import com.google.common.collect.Ordering;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
import org.eclipse.ui.dialogs.SearchPattern;
import java.util.Comparator;
import java.util.Map;

/**
 * A Dialog to select a {@link PipelineOptionsType}.
 */
public class PipelineOptionsSelectionDialog extends FilteredItemsSelectionDialog {
  private final Map<String, PipelineOptionsType> optionsTypes;

  public PipelineOptionsSelectionDialog(
      Shell shell, Map<String, PipelineOptionsType> optionsTypes) {
    super(shell, false);
    this.optionsTypes = optionsTypes;
    setListLabelProvider(new PipelineOptionsLabelProvider());
  }

  @Override
  protected Control createExtendedContentArea(Composite parent) {
    return null;
  }

  @Override
  protected IDialogSettings getDialogSettings() {
    return DataflowUiPlugin.getDialogSettingsSection(getClass().getName());
  }

  @Override
  protected IStatus validateItem(Object item) {
    return Status.OK_STATUS;
  }

  @Override
  protected ItemsFilter createFilter() {
    return new PipelineOptionsSelectionFilter();
  }

  @SuppressWarnings("rawtypes")
  @Override
  protected Comparator getItemsComparator() {
    return Ordering.natural();
  }

  @Override
  protected void fillContentProvider(AbstractContentProvider contentProvider,
      ItemsFilter itemsFilter, IProgressMonitor progressMonitor) throws CoreException {
    SubMonitor submonitor = SubMonitor.convert(progressMonitor, optionsTypes.size());
    for (String optionsTypeName : optionsTypes.keySet()) {
      if (itemsFilter.isConsistentItem(optionsTypeName) && itemsFilter.matchItem(optionsTypeName)) {
        contentProvider.add(optionsTypeName, itemsFilter);
      }
      submonitor.worked(1);
    }
  }

  @Override
  public String getElementName(Object item) {
    return item.toString();
  }


  private class PipelineOptionsSelectionFilter extends ItemsFilter {
    public PipelineOptionsSelectionFilter() {
      super(
          new SearchPattern(SearchPattern.RULE_CAMELCASE_MATCH | SearchPattern.RULE_PATTERN_MATCH));
    }

    @Override
    public boolean matchItem(Object item) {
      if (!isConsistentItem(item)) {
        return false;
      }
      return matches(item.toString()) || item.toString().contains(getPattern());
    }

    @Override
    public boolean isConsistentItem(Object item) {
      return optionsTypes.containsKey(item);
    }
  }

  private class PipelineOptionsLabelProvider implements ILabelProvider {
    @Override
    public void addListener(ILabelProviderListener listener) {}

    @Override
    public void dispose() {}

    @Override
    public boolean isLabelProperty(Object element, String property) {
      return false;
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {}

    @Override
    public Image getImage(Object element) {
      return null;
    }

    @Override
    public String getText(Object element) {
      if (element == null) {
        return null;
      }
      String simpleName = Signature.getSimpleName(element.toString());
      return String.format("%s - %s", simpleName, element.toString());
    }
  }
}
