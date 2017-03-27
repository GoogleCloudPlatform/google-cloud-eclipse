
package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import com.google.cloud.tools.eclipse.appengine.localserver.Messages;
import com.google.cloud.tools.eclipse.appengine.localserver.server.LocalAppEngineServerLaunchConfigurationDelegate;
import com.google.cloud.tools.eclipse.ui.util.images.SharedImages;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;

/**
 * A Platform/Debug Presentation to provide an icon for our
 * {@link LocalAppEngineServerLaunchConfigurationDelegate.CloudSdkDebugTarget} instances.
 */
public class CloudSdkDebugTargetPresentation extends LabelProvider
    implements IDebugModelPresentation {
  private static final Logger logger =
      Logger.getLogger(CloudSdkDebugTargetPresentation.class.getName());
  private Image image;

  @Override
  public String getText(Object element) {
    if (element instanceof IDebugTarget) {
      try {
        IDebugTarget target = (IDebugTarget) element;
        String text = target.getName();
        return target.isTerminated() ? Messages.getString("target.terminated", text) : text;
      } catch (DebugException ex) {
        logger.log(Level.FINE, "Unexpected execption", ex);
        /* FALLTHROUGH */
      }
    }
    return super.getText(element);
  }

  @Override
  public Image getImage(Object element) {
    if (element instanceof IDebugTarget) {
      if (image == null) {
        image = SharedImages.CLOUDSDK_IMAGE_DESCRIPTOR.createImage();
      }
      return image;
    }
    return super.getImage(element);
  }

  @Override
  public void dispose() {
    if (image != null) {
      image.dispose();
    }
    image = null;
    super.dispose();
  }

  @Override
  public IEditorInput getEditorInput(Object element) {
    return null;
  }

  @Override
  public String getEditorId(IEditorInput input, Object element) {
    return null;
  }

  @Override
  public void setAttribute(String attribute, Object value) {
  }

  @Override
  public void computeDetail(IValue value, IValueDetailListener listener) {
  }
}
