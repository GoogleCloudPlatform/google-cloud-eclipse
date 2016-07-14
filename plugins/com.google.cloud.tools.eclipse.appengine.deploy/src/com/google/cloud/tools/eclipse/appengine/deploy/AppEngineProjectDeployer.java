package com.google.cloud.tools.eclipse.appengine.deploy;

import java.io.File;
import java.util.Collections;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.google.cloud.tools.appengine.api.deploy.DefaultDeployConfiguration;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkAppEngineDeployment;

public class AppEngineProjectDeployer {

  private static final String PLUGIN_ID = "com.google.cloud.tools.eclipse.appengine.localserver"; //$NON-NLS-1$
  private static final String APPENGINE_WEB_XML_PATH = "WEB-INF/appengine-web.xml"; //$NON-NLS-1$

  public void deploy(IPath stagingDir, CloudSdk cloudSdk, IProgressMonitor monitor) throws CoreException {
    SubMonitor progress = SubMonitor.convert(monitor, 1);
    progress.setTaskName(Messages.getString("task.name.deploy.project")); //$NON-NLS-1$
    try  {

      Document doc = parseAppEngineWebXml(new File(stagingDir.append(APPENGINE_WEB_XML_PATH).toOSString()));

      String projectId = getProjectId(doc);
      if (projectId == null) {
        throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, Messages.getString("project.id.missing"))); //$NON-NLS-1$
      }

      String projectVersion = getProjectVersion(doc);
      if (projectVersion == null) {
        throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, Messages.getString("project.version.missing"))); //$NON-NLS-1$
      }

      DefaultDeployConfiguration deployConfig = new DefaultDeployConfiguration();
      deployConfig.setDeployables(Collections.singletonList(stagingDir.append("app.yaml").toFile())); //$NON-NLS-1$
      deployConfig.setProject(projectId);
      deployConfig.setVersion(projectVersion);
      deployConfig.setPromote(true);

      CloudSdkAppEngineDeployment deployment = new CloudSdkAppEngineDeployment(cloudSdk);
      deployment.deploy(deployConfig);
    } catch (Exception e) {
      throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, Messages.getString("deploy.failed.error.message"), e)); //$NON-NLS-1$
    } finally {
      progress.worked(1);
    }
  }

  private Document parseAppEngineWebXml(File appEngineXml) throws Exception {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.parse(appEngineXml);
    return doc;
  }

  private String getProjectId(Document doc) throws CoreException {
    return getTopLevelValue(doc, "appengine-web-app", "application"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  private String getProjectVersion(Document doc) throws CoreException {
    return getTopLevelValue(doc, "appengine-web-app", "version"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  private String getTopLevelValue(Document doc, String parentTagName, String childTagName) throws CoreException {
    try {
      NodeList parentTag = doc.getElementsByTagName(parentTagName);
      NodeList childNodes = parentTag.item(0).getChildNodes();
      for (int i = 0; i < childNodes.getLength(); ++i) {
        if (childNodes.item(i).getNodeName().equals(childTagName)) {
          return childNodes.item(i).getChildNodes().item(0).getNodeValue();
        }
      }
    } catch (Exception e) {
      throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, Messages.getString("missing.appengine.xml.element") + childTagName)); //$NON-NLS-1$
    }
    return null;
  }
}
