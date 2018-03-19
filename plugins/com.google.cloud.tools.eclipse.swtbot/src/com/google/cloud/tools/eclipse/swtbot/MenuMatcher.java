/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.swtbot;

import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.stream.Stream;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.matchers.AbstractMatcher;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotRootMenu;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

/**
 * Matches menu item with matching menu text. Supports {@link SWTBotMenu}, {@link SWTBotRootMenu},
 * and {@link Menu}.
 */
public class MenuMatcher<T> extends AbstractMatcher<T> {
  /** Match the menu with the given label. */
  public static <T> Matcher<T> hasMenuItem(String label) {
    return hasMenuItem(is(label));
  }

  /** Match a menu with a matching label. */
  public static <T> Matcher<T> hasMenuItem(Matcher<String> menuLabelMatcher) {
    return new MenuMatcher<>(menuLabelMatcher);
  }

  private Matcher<String> menuLabelMatcher;

  private MenuMatcher(Matcher<String> menuLabelMatcher) {
    this.menuLabelMatcher = menuLabelMatcher;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("has menu item that ").appendDescriptionOf(menuLabelMatcher);
  }

  @Override
  protected boolean doMatch(Object item) {
    if (item instanceof SWTBotMenu) {
      List<String> menuTexts = ((SWTBotMenu) item).menuItems();
      return menuTexts.stream().anyMatch(text -> menuLabelMatcher.matches(text));
    } else if (item instanceof SWTBotRootMenu) {
      List<String> menuTexts = ((SWTBotRootMenu) item).menuItems();
      return menuTexts.stream().anyMatch(text -> menuLabelMatcher.matches(text));
    } else if (item instanceof Menu) {
      return UIThreadRunnable.syncExec(
          () ->
              Stream.of(((Menu) item).getItems())
                  .anyMatch(child -> menuLabelMatcher.matches(child.getText())));
    }
    return false;
  }
}
