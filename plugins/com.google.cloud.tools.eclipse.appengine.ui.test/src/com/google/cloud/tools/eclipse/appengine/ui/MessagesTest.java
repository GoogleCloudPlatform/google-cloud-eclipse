package com.google.cloud.tools.eclipse.appengine.ui;

import org.junit.Assert;
import org.junit.Test;

public class MessagesTest {

  @Test
  public void test() {
    Assert.assertEquals(
        "Cannot create an App Engine Eclipse project because the Cloud SDK App Engine Java"
        + " component is not installed. Fix by running "
        + "'gcloud components install app-engine-java' on the command-line.",
        Messages.getString("fix.appengine.java.component"));
  }

}
