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

package com.google.cloud.tools.eclipse.dataflow.ui.util;

import com.google.common.collect.ImmutableSortedSet;
import java.util.SortedSet;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;

/**
 * Select the first string that contains the current input as a prefix whenever input is provided
 * to the provided Combo.
 */
public class SelectFirstMatchingPrefixListener implements ModifyListener {
  private final Combo targetCombo;
  private String lastString;

  private SortedSet<String> contents;

  public SelectFirstMatchingPrefixListener(Combo targetCombo) {
    this.targetCombo = targetCombo;
    lastString = "";
    contents = ImmutableSortedSet.of();
  }

  public void setContents(SortedSet<String> contents) {
    this.contents = ImmutableSortedSet.copyOf(contents);
  }

  @Override
  public void modifyText(ModifyEvent event) {
    String lastAsOfEvent = lastString;
    String prefix = targetCombo.getText();

    lastString = prefix;
    // If content was deleted, don't immediately replace it
    if (prefix.length() < lastAsOfEvent.length()) {
      return;
    }
    // Don't autofill if the user isn't at the end of the string
    if (targetCombo.getCaretPosition() < prefix.length() || prefix.length() < 6) {
      return;
    }
    SortedSet<String> matchesAndLater = contents.tailSet(prefix);
    if (matchesAndLater.isEmpty()) {
      return;
    }
    String firstMatch = matchesAndLater.first();
    if (!firstMatch.startsWith(prefix) || firstMatch.equals(prefix)) {
      return;
    }
    int comboIndex = contents.size() - matchesAndLater.size();
    // Don't fire on this modification of the combo
    targetCombo.removeModifyListener(this);
    targetCombo.select(comboIndex);

    // Select the text that was added
    targetCombo.setSelection(new Point(prefix.length(), firstMatch.length()));

    // Fire on later user modifications of the combo
    targetCombo.addModifyListener(this);
  }
}
