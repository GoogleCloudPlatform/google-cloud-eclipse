/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.ui.util;

import com.google.common.base.Strings;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IMenuListener2;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.menus.IMenuService;

/**
 * A handler for submenu-style tool bar command contributions that are intended to be a placeholder
 * to open a drop-down menu. This handler is required as Eclipse otherwise only opens the drop-down
 * menu when clicking on the small drop-down arrow. The popup menu is populated using normal menu
 * contributions found via the Eclipse {@link IMenuService}, and the menu ID is taken from the
 * command contribution's {@code id}.
 * <p>
 * <b>Example</b>
 * 
 * <pre>
 * &lt;extension point=&quot;org.eclipse.ui.menus&quot;&gt;
 *    &lt;menuContribution locationURI=&quot;toolbar:org.eclipse.ui.main.toolbar?after=additions&quot;&gt;
 *       &lt;toolbar id=&quot;com.google.cloud.tools.eclipse.appengine.toolbar&quot;&gt;
 *          &lt;command
 *                style=&quot;pulldown&quot;
 *                commandId=&quot;com.google.cloud.tools.eclipse.ui.util.showPopup&quot;
 *                id=&quot;com.google.cloud.tools.eclipse.appengine.actions&quot;
 *                icon=&quot;xxx.png&quot; /&gt;
 *       &lt;/toolbar&gt;
 *    &lt;/menuContribution&gt;
 *    &lt;menuContribution locationURI=&quot;menu:com.google.cloud.tools.eclipse.appengine.actions&quot;&gt;
 *       &lt;command commandId=&quot;...&quot;/&gt;
 *    &lt;/menuContribution&gt;
 * &lt;/extension&gt;
 * </pre>
 */
public final class OpenDropDownMenuHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    ToolItem toolItem = findToolItem(event);
    String menuId = getMenuId(event, toolItem);
    IMenuService menuService = ServiceUtils.getService(event, IMenuService.class);
    openDropDownMenu(menuId, toolItem, menuService);
    return null;
  }

  /**
   * Ensure we're being executed as a command tool item with style {@code DROP_DOWN}.
   * 
   * @return the {@link ToolItem}
   */
  private static ToolItem findToolItem(ExecutionEvent event) throws ExecutionException {
    if (event.getTrigger() instanceof Event) {
      Event swtEvent = (Event) event.getTrigger();
      if (swtEvent.widget instanceof ToolItem) {
        ToolItem toolItem = (ToolItem) swtEvent.widget;
        int style = toolItem.getStyle();
        if ((style & SWT.DROP_DOWN) != 0) {
          return toolItem;
        }
      }
    }
    throw new ExecutionException("Invalid tool item");
  }

  /**
   * Retrieve the menu id to show from the contribution item's ID (following the documented approach
   * for DROP_DOWN items).
   */
  private static String getMenuId(ExecutionEvent event, ToolItem toolItem)
      throws ExecutionException {
    if (toolItem.getData() instanceof ContributionItem) {
      ContributionItem data = (ContributionItem) toolItem.getData();
      if (!Strings.isNullOrEmpty(data.getId())) {
        return data.getId();
      }
    }
    throw new ExecutionException("Unable to determine menu ID");
  }

  /**
   * Opens drop-down menu.
   */
  private static void openDropDownMenu(final String menuId, final ToolItem toolItem,
      final IMenuService menuService) {
    final MenuManager menuManager = new MenuManager();
    Menu menu = menuManager.createContextMenu(toolItem.getParent());
    menuManager.addMenuListener(new IMenuListener2() {
      @Override
      public void menuAboutToHide(IMenuManager manager) {
        toolItem.getDisplay().asyncExec(new Runnable() {
          @Override
          public void run() {
            menuService.releaseContributions(menuManager);
            menuManager.dispose();
          }
        });
      }

      @Override
      public void menuAboutToShow(IMenuManager manager) {
        menuService.populateContributionManager(menuManager, "menu:" + menuId);
      }
    });
    // place the menu below the drop-down item
    Rectangle itemBounds = toolItem.getBounds();
    Point point =
        toolItem.getParent().toDisplay(new Point(itemBounds.x, itemBounds.y + itemBounds.height));
    menu.setLocation(point.x, point.y);
    menu.setVisible(true);
  }
}
