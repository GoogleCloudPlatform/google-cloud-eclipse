package com.google.cloud.tools.eclipse.usagetracker;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

/**
 * A one-time dialog to suggest opt-in for sending client-side usage metrics.
 */
class OptInDialog extends Dialog {

  public OptInDialog(Shell parentShell) {
    super(parentShell);
    setShellStyle(SWT.MODELESS);
    setBlockOnOpen(false);
  }

  /**
   * Show this dialog at the top-right corner.
   */
  @Override
  protected Point getInitialLocation(Point initialSize) {
    Rectangle parentBounds = getParentShell().getBounds();
    Rectangle parentClientArea = getParentShell().getClientArea();

    int heightCaptionAndUpperBorder =
        parentBounds.height - parentClientArea.height - getParentShell().getBorderWidth();
    return new Point(parentBounds.x + parentClientArea.width - initialSize.x,
        parentBounds.y + heightCaptionAndUpperBorder);
  }

  /**
   * Overridden in order to remove the button bar area (and the OK and CANCEL buttons) that the
   * base class creates by default. As a result, most of the code has been taken directly from
   * the base method except for the code to create the button bar area.
   * <p/>
   * @see Dialog#createContents(Composite)
   */
  @Override
  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, 0);
    GridLayout layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.verticalSpacing = 0;
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(GridData.FILL_BOTH));
    applyDialogFont(composite);
    initializeDialogUnits(composite);
    dialogArea = createDialogArea(composite);

    return composite;
  }

  /**
   * Creates one {@link #Label} and one {@link #Link} inside the dialog. Users can click on
   * either "I agree" or "I don't agree" through the {@link #Link}.
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    Composite container = (Composite) super.createDialogArea(parent);

    Label label = new Label(container, 0);
    label.setText(Messages.OPT_IN_NOTIFICATION_TEXT);

    // Use a smaller font that the default.
    FontData[] fontData = label.getFont().getFontData();
    for (int i = 0; i < fontData.length; i++) {
      fontData[i].setHeight(fontData[i].getHeight() - 1);
    }
    Font smallerFont = new Font(label.getDisplay(), fontData);
    label.setFont(smallerFont);

    Link link = new Link(container, 0);
    link.setText(Messages.OPT_IN_NOTIFICATION_LINK);
    link.setFont(smallerFont);

    // Register the opt-in status depending on user selection.
    link.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        if ("opted-in".equals(event.text)) {
          AnalyticsPingManager.registerOptInStatus(true);
        } else if ("opted-out".equals(event.text)) {
          AnalyticsPingManager.registerOptInStatus(false);
        }
        close();
      }
    });

    return container;
  }
}
