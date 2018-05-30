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

package com.google.cloud.tools.eclipse.appengine.localserver.server;

import com.google.cloud.tools.eclipse.appengine.facets.WebProjectUtil;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/** A simple value class to capture updates to a module's {@code datastore-indexes.xml}. */
public class DatastoreIndexUpdateData {
  private static final Logger logger = Logger.getLogger(DatastoreIndexUpdateData.class.getName());

  /**
   * Check the server configuration and detect if a {@code datastore-indexes-auto.xml} was found.
   * Return {@code null} if not found.
   */
  public static DatastoreIndexUpdateData detect(ILaunchConfiguration configuration, IServer server) {
    // The datastore-indexes.xml file should be in the default module's WEB-INF/ directory.
    IModule defaultService = ModuleUtils.findService(server, "default");
    if (defaultService == null) {
      return null;
    }
    return detect(configuration, server, defaultService);
  }

  @VisibleForTesting
  static DatastoreIndexUpdateData detect(ILaunchConfiguration configuration, IServer server,
      IModule defaultService) {
    LocalAppEngineServerBehaviour serverBehaviour = (LocalAppEngineServerBehaviour) server
        .loadAdapter(LocalAppEngineServerBehaviour.class, null);
    IPath deployPath = serverBehaviour.getModuleDeployDirectory(defaultService);
    IPath datastoreIndexesAutoXml =
        deployPath.append("WEB-INF/appengine-generated/datastore-indexes-auto.xml");
    if (!indexGenerated(datastoreIndexesAutoXml)) {
      return null;
    }
    // datastore-indexes-auto.xml may be generated even if datastore-indexes.xml does not exist
    IFile datastoreIndexesXml = WebProjectUtil.findInWebInf(defaultService.getProject(),
        new org.eclipse.core.runtime.Path("datastore-indexes.xml"));

    if (datastoreIndexesXml != null && datastoreIndexesXml.exists()) {
      long sourceTimestamp = datastoreIndexesXml.getLocalTimeStamp();
      long generatedTimestamp = datastoreIndexesAutoXml.toFile().lastModified();
      if (sourceTimestamp > generatedTimestamp) {
        logger.log(Level.FINE, "no change based on datastore-indexes timestamps");
        return null;
      }
    }

    return new DatastoreIndexUpdateData(server, configuration, defaultService, datastoreIndexesXml,
        datastoreIndexesAutoXml);
  }

  private static boolean indexGenerated(IPath datastoreIndexesAutoXml) {
    if (!datastoreIndexesAutoXml.toFile().exists()) { 
      return false;
    }
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    try {
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = builder.parse(datastoreIndexesAutoXml.toFile());
      return doc.getElementsByTagName("datastore-index").getLength() > 0;
    } catch (ParserConfigurationException ex) {
      return true;
    } catch (SAXException | IOException ex) {
      return false;
    }
  }

  public final IServer server;
  public final ILaunchConfiguration configuration;

  /** Should be the default module. */
  public final IModule module;

  /** The original {@code datastore-indexes.xml} file; may be {@code null} if it doesn't exist. */
  public final IFile datastoreIndexesXml;

  /** The generated {@code datastore-indexes-auto.xml} with updates. */
  public final IPath datastoreIndexesAutoXml;

  private DatastoreIndexUpdateData(IServer server, ILaunchConfiguration configuration,
      IModule module, IFile datastoreIndexesXml, IPath datastoreIndexesAutoXml) {
    this.server = server;
    this.configuration = configuration;
    this.module = module;
    this.datastoreIndexesXml = datastoreIndexesXml;
    this.datastoreIndexesAutoXml = datastoreIndexesAutoXml;
  }
}