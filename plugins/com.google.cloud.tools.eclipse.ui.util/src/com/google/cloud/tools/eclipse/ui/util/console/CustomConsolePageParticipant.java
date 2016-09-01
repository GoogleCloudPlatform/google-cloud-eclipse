package com.google.cloud.tools.eclipse.ui.util.console;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.part.IPageBookViewPage;

import com.google.cloud.tools.eclipse.ui.util.Messages;

/**
 * Adds a stop button for the App Engine runtime to the {@link TaggedMessageConsole}
 */
public abstract class CustomConsolePageParticipant<T> implements IConsolePageParticipant {
  
  private static final Logger logger = Logger.getLogger(CustomConsolePageParticipant.class.getName());
  
  private TaggedMessageConsole<T> console;
  private Action terminateAction;
  private Action closeAction;
  protected boolean closeVisible = false;
  
  @Override
  public <C> C getAdapter(Class<C> required) {
    return null;
  }

  @Override
  public void init(IPageBookViewPage page, IConsole console) {
    if (console instanceof TaggedMessageConsole) {
      this.console = (TaggedMessageConsole) console;
    } else {
      logger.log(Level.SEVERE, "console is instance of "
                               + console.getClass().getName()
                               + ", expected was "
                               + TaggedMessageConsole.class.getName());
    }
    // contribute to toolbar
    IActionBars actionBars = page.getSite().getActionBars();
    configureToolBar(actionBars.getToolBarManager());
    initInternal();
    update();
  }

  protected void initInternal() {
  };

  @Override
  public void dispose() {
    terminateAction = null;
    closeAction = null;
  }

  @Override
  public void activated() {
    update();
  }

  @Override
  public void deactivated() {
    update();
  }
  
  private void configureToolBar(IToolBarManager toolbarManager) {
    terminateAction = new Action(terminateActionText()) {
      @Override
      public void run() {
        runTerminateAction();
        update();
      }
    };
    terminateAction.setToolTipText(terminateActionTooltip());
    terminateAction.setImageDescriptor(getSharedImage(ISharedImages.IMG_ELCL_STOP));
    terminateAction.setHoverImageDescriptor(getSharedImage(ISharedImages.IMG_ELCL_STOP));
    terminateAction.setDisabledImageDescriptor(getSharedImage(ISharedImages.IMG_ELCL_STOP_DISABLED));
    toolbarManager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, terminateAction);
    
    if (closeVisible) {
      closeAction = new Action(closeActionText()) {
        @Override
        public void run() {
          runCloseAction();
        }
      };
      closeAction.setToolTipText(closeActionTooltip());
      closeAction.setImageDescriptor(getSharedImage(ISharedImages.IMG_ELCL_REMOVE));
      closeAction.setHoverImageDescriptor(getSharedImage(ISharedImages.IMG_ELCL_REMOVE));
      closeAction.setDisabledImageDescriptor(getSharedImage(ISharedImages.IMG_ELCL_REMOVE_DISABLED));
      toolbarManager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, closeAction);
    }
  }

  private ImageDescriptor getSharedImage(String image) {
    return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(image);
  }

  protected String terminateActionText() {
    return Messages.getString("action.stop");
  }
  
  protected String terminateActionTooltip() {
    return Messages.getString("action.stop");
  }

  protected String closeActionText() {
    return Messages.getString("action.close");
  }
  
  protected String closeActionTooltip() {
    return Messages.getString("action.close");
  }

  protected void update() {
    T tag = console.getTag();
    
    if (terminateAction != null) {
      terminateAction.setEnabled(terminateEnabled(tag));
    }
    
    if (closeAction != null) {
      closeAction.setEnabled(closeEnabled(tag));
    }
  }

  protected abstract void runTerminateAction();
  
  protected void runCloseAction() {
    ConsolePlugin.getDefault().getConsoleManager().removeConsoles(new IConsole[] { console });
  };

  protected boolean terminateEnabled(T serverBehaviour) {
    return false;
  };
  
  protected boolean closeEnabled(T serverBehaviour) {
    return false;
  }

  protected TaggedMessageConsole<T> getConsole() {
    return console;
  }
 
}

