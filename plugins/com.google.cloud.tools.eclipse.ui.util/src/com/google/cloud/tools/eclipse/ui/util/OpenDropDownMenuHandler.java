
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
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.IMenuService;

/**
 * A command handler for commands representing drop-down Menu contribution item. This handler just
 * opens drop-down menu, since Eclipse behavior for such items is to open drop-down menu only by
 * clicking on small arrow. Drop-down Menu contribution items without any handler shown as disabled.
 */
public final class OpenDropDownMenuHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    ToolItem toolItem = checkToolItem(event);
    String menuId = getMenuId(event, toolItem);
    IMenuService menuService = getService(event, IMenuService.class);
    openDropDownMenu(menuId, toolItem, menuService);
    return null;
  }

  /**
   * Ensure we're being executed as a command tool item with style {@code DROP_DOWN}.
   */
  private ToolItem checkToolItem(ExecutionEvent event) throws ExecutionException {
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
    throw new ExecutionException("Invalid toolitem");
  }

  /**
   * Retrieve the menu id to show either from the event's {@code menuId} parameter or from the
   * contribution item's ID (following the documented approach for DROP_DOWN items).
   */
  private String getMenuId(ExecutionEvent event, ToolItem toolItem) throws ExecutionException {
    String menuId = event.getParameter("menuId");
    if (!Strings.isNullOrEmpty(menuId)) {
      return menuId;
    }
    if (toolItem.getData() instanceof ContributionItem) {
      ContributionItem data = (ContributionItem) toolItem.getData();
      if (!Strings.isNullOrEmpty(data.getId())) {
        return data.getId();
      }
    }
    throw new ExecutionException("Unable to determine menu ID");
  }

  private <T> T getService(ExecutionEvent event, Class<T> clazz) {
    return PlatformUI.getWorkbench().getService(clazz);
  }

  /**
   * Opens drop-down menu.
   */
  private void openDropDownMenu(final String menuId, final ToolItem toolItem,
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
