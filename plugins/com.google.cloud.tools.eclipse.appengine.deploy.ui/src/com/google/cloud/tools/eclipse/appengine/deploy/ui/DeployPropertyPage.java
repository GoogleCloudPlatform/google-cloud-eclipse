package com.google.cloud.tools.eclipse.appengine.deploy.ui;


import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

public class DeployPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {

  private Text version;
  private Text projectId;
  private Text bucket;
  private Button defaultVersionButton;
  private Button customVersionButton;
  private Button autoPromoteButton;
  private Button manualPromoteButton;
  private Button defaultBucketButton;
  private Button customBucketButton;

  private RadioSelectionListener versionSelectionListener;
  private RadioSelectionListener bucketSelectionListener;

  public class RadioSelectionListener implements SelectionListener {

    private Text text;
    private Button button;

    public RadioSelectionListener(Text text, Button button) {
      this.text = text;
      this.button = button;
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
      doSelection(e);
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
      doSelection(e);
    }

    private void doSelection(SelectionEvent e) {
      text.setEnabled(e.getSource() == button);
    }
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(new GridLayout(1, false));

    // project ID
    Composite projectIdComp = new Composite(container, SWT.NONE);
    projectIdComp.setLayout(new GridLayout(3, false));
    projectIdComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Label projectIdLabel = new Label(projectIdComp, SWT.LEFT);
    projectIdLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
    projectIdLabel.setText(Messages.getString("project.id"));

    projectId = new Text(projectIdComp, SWT.LEFT | SWT.SINGLE | SWT.BORDER);
    projectId.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Button browseProjectButton = new Button(projectIdComp, SWT.PUSH);
    browseProjectButton.setText("...");
    browseProjectButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

    // version
    Composite versionComp = new Composite(container, SWT.NONE);
    versionComp.setLayout(new GridLayout(3, false));
    versionComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Label versionLabel = new Label(versionComp, SWT.LEFT);
    versionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
    versionLabel.setText(Messages.getString("project.version"));

    defaultVersionButton = new Button(versionComp, SWT.RADIO);
    defaultVersionButton.setText(Messages.getString("use.default.versioning"));
    defaultVersionButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));

    // custom version
    Label emptyLabel = new Label(versionComp, SWT.LEFT);
    emptyLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

    customVersionButton = new Button(versionComp, SWT.RADIO);
    customVersionButton.setText(Messages.getString("use.custom.versioning"));
    customVersionButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

    version = new Text(versionComp, SWT.LEFT | SWT.SINGLE | SWT.BORDER);
    version.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    versionSelectionListener = new RadioSelectionListener(version, customVersionButton);
    defaultVersionButton.addSelectionListener(versionSelectionListener);
    customVersionButton.addSelectionListener(versionSelectionListener);

    selectVersionButton();

    // promote
    Composite promoteComp = new Composite(container, SWT.NONE);
    promoteComp.setLayout(new GridLayout(3, false));
    promoteComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Label promoteLabel = new Label(promoteComp, SWT.LEFT);
    promoteLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
    promoteLabel.setText(Messages.getString("promote.policy"));

    autoPromoteButton = new Button(promoteComp, SWT.RADIO);
    autoPromoteButton.setText(Messages.getString("auto.promote"));
    autoPromoteButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

    manualPromoteButton = new Button(promoteComp, SWT.RADIO);
    manualPromoteButton.setText(Messages.getString("manual.promote"));
    manualPromoteButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

    selectPromoteButton();

    // bucket
    Composite bucketComp = new Composite(container, SWT.NONE);
    bucketComp.setLayout(new GridLayout(3, false));
    bucketComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Label bucketLabel = new Label(bucketComp, SWT.LEFT);
    bucketLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
    bucketLabel.setText(Messages.getString("bucket.name"));

    defaultBucketButton = new Button(bucketComp, SWT.RADIO);
    defaultBucketButton.setText(Messages.getString("use.default.bucket"));
    defaultBucketButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));

    // custom version
    Label emptyLabel2 = new Label(bucketComp, SWT.LEFT);
    emptyLabel2.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

    customBucketButton = new Button(bucketComp, SWT.RADIO);
    customBucketButton.setText(Messages.getString("use.custom.bucket"));
    customBucketButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

    Composite bucketTextComp = new Composite(bucketComp, SWT.NONE);
    bucketTextComp.setLayout(new GridLayout(2, false));
    bucketTextComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    bucket = new Text(bucketTextComp, SWT.LEFT | SWT.SINGLE | SWT.BORDER);
    bucket.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Button browseBucketButton = new Button(bucketTextComp, SWT.PUSH);
    browseBucketButton.setText("...");
    browseBucketButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

    bucketSelectionListener = new RadioSelectionListener(bucket, customBucketButton);
    defaultBucketButton.addSelectionListener(bucketSelectionListener);
    customBucketButton.addSelectionListener(bucketSelectionListener);

    selectBucketButton();

    return container;
  }

  private void selectBucketButton() {
    defaultBucketButton.setSelection(true);
    bucket.setEnabled(false);
  }

  private void selectPromoteButton() {
    autoPromoteButton.setSelection(true);
  }

  private void selectVersionButton() {
    defaultVersionButton.setSelection(true);
    version.setEnabled(false);
  }

}
