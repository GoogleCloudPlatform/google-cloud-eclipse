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

package com.google.cloud.tools.eclipse.swtbot;

import com.google.common.base.Preconditions;
import java.util.Arrays;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.swtbot.swt.finder.widgets.TimeoutException;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.StringDescription;

/**
 * Utilities for manipulating trees.
 */
public class SwtBotTreeUtilities {

  /**
   * Wait until the given tree has items, then return the first item.
   *
   * @throws TimeoutException if no items appear within the default timeout
   */
  public static SWTBotTreeItem waitUntilTreeHasItems(SWTWorkbenchBot bot, SWTBotTree tree) {
    bot.waitUntil(new DefaultCondition() {
      @Override
      public String getFailureMessage() {
        return "Tree items never appeared";
      }

      @Override
      public boolean test() throws Exception {
        return tree.hasItems();
      }
    });
    return tree.getAllItems()[0];
  }

  /**
   * Wait until the given tree item has items, and return the first item.
   *
   * @throws TimeoutException if no items appear within the default timeout
   */
  public static SWTBotTreeItem waitUntilTreeHasItems(SWTWorkbenchBot bot, SWTBotTreeItem treeItem) {
    bot.waitUntil(
        new DefaultCondition() {
          @Override
          public String getFailureMessage() {
            return "Tree items never appeared";
          }

          @Override
          public boolean test() throws Exception {
            SWTBotTreeItem[] children = treeItem.getItems();
            if (children.length == 1 && "".equals(children[0].getText())) {
              // Work around odd bug seen only on Windows and Linux.
              // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/2569
              treeItem.collapse();
              treeItem.expand();
              children = treeItem.getItems();
            }
            return children.length > 0;
          }
        });
    return treeItem.getItems()[0];
  }

  /**
   * Wait until the given tree has not items.
   * 
   * @throws TimeoutException if no items appear within the default timeout
   */
  public static void waitUntilTreeHasNoItems(SWTWorkbenchBot bot, final SWTBotTree tree) {
    bot.waitUntil(new DefaultCondition() {
      @Override
      public String getFailureMessage() {
        return "Tree items never disappeared";
      }

      @Override
      public boolean test() throws Exception {
        return !tree.hasItems();
      }
    });
  }

  /**
   * Wait until the tree item contains the given text with the timeout {@link
   * SWTBotPreferences#TIMEOUT}.
   */
  public static void waitUntilTreeContainsText(
      SWTWorkbenchBot bot, SWTBotTreeItem treeItem, String text) {
    waitUntilTreeTextMatches(bot, treeItem, Matchers.containsString(text));
  }

  /** Wait until the tree item text matches with the timeout {@link SWTBotPreferences#TIMEOUT}. */
  public static void waitUntilTreeTextMatches(
      SWTWorkbenchBot bot, SWTBotTreeItem treeItem, Matcher<String> textMatcher) {
    waitUntilTreeTextMatches(bot, treeItem, textMatcher, SWTBotPreferences.TIMEOUT);
  }

  /** Wait until the tree item contains the given text with the timeout specified. */
  public static void waitUntilTreeTextMatches(
      SWTWorkbenchBot bot,
      final SWTBotTreeItem treeItem,
      final Matcher<String> textMatcher,
      long timeout) {
    bot.waitUntil(
        new DefaultCondition() {
          @Override
          public boolean test() throws Exception {
            return textMatcher.matches(treeItem.getText());
          }

          @Override
          public String getFailureMessage() {
            Description description = new StringDescription();
            description.appendText("Text never matched: ");
            textMatcher.describeMismatch(treeItem.getText(), description);
            return description.toString();
          }
        },
        timeout);
  }

  /**
   * Expand and select the nodes in the given tree. This is equivalent to
   *
   * <pre>
   * tree
   *   .expandNode(nodeNames[0])
   *   .expandNode(nodeNames[1])
   *   ...
   *   .select(nodeNames[N-1]);
   * </pre>
   *
   * except that it will collapse and re-expand intermediate nodes on timeout.
   *
   * @return the tree item
   * @throws WidgetNotFoundException if the tree item could not be found
   * @see <a href="https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/2569">issue
   *     2569</a>
   */
  public static SWTBotTreeItem select(SWTWorkbenchBot bot, SWTBotTree tree, String... nodeNames) {
    Preconditions.checkArgument(nodeNames.length > 0, "no children to navigate");
    int leafIndex = nodeNames.length - 1;
    waitUntilTreeHasItems(bot, tree);

    // special case: no intermediate nodes
    if (nodeNames.length == 1) {
      return tree.getTreeItem(nodeNames[leafIndex]).select(); // throws WNFE if not found
    }

    // try expanding the intermediate nodes at once and selecting the leaf node
    try {
      String[] intermediates = Arrays.copyOf(nodeNames, leafIndex);
      SWTBotTreeItem item = tree.expandNode(intermediates); // throws WNFE
      if (item != null) {
        return item.getNode(nodeNames[leafIndex]).select(); // throws WNFE if not found
      }
    } catch (WidgetNotFoundException ex) {
      // ignore: we now collapse and re-expand items
    }
    // now proceed down the node path, 1 element at a time, collapsing and re-expanding in the hope
    // that the child-nodes will appear properly
    SWTBotTreeItem item = tree.collapseNode(nodeNames[0]); // throws WNFE
    item.expand();
    // re-expand remaining intermediate nodes
    for (int i = 1; i < leafIndex; i++) {
      item.collapseNode(nodeNames[i]); // throws WNFE
      item = item.expandNode(nodeNames[i]);
    }
    return item.getNode(nodeNames[leafIndex]).select(); // throws WNFE
  }
}
