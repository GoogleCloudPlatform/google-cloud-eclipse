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

package com.google.cloud.tools.eclipse.appengine.facets.ui.navigator;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineFlexJarFacet;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineFlexWarFacet;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.AppEngineProjectElement;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.CronDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.DatastoreIndexesDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.DenialOfServiceDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.DispatchRoutingDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.TaskQueuesDescriptor;
import com.google.cloud.tools.eclipse.test.util.BasePluginXmlTest;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.ui.IPageLayout;
import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Element;

/** Specific tests for the Common Navigator configuration. */
public class PluginXmlTest extends BasePluginXmlTest {
  @Rule public TestProjectCreator projectCreator = new TestProjectCreator();

  @Test
  public void testNavigatorContentDefinition() {
    Element definition =
        findElement(
            "//plugin/extension[@point='org.eclipse.ui.navigator.navigatorContent']"
                + "/navigatorContent[@id='com.google.cloud.tools.eclipse.appengine.navigator']");
    assertEquals("true", definition.getAttribute("activeByDefault"));
    assertEquals("org.eclipse.jst.jee.ui.web", definition.getAttribute("appearsBefore"));
    assertEquals(
        AppEngineContentProvider.class.getName(), definition.getAttribute("contentProvider"));
    assertEquals(
        "platform:/plugin/com.google.cloud.tools.eclipse.appengine.ui/icons/obj16/appengine.png",
        definition.getAttribute("icon"));
    assertEquals(
        "com.google.cloud.tools.eclipse.appengine.navigator", definition.getAttribute("id"));
    assertEquals(
        "com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.AppEngineLabelProvider",
        definition.getAttribute("labelProvider"));
    assertEquals("%appengineNavigatorContentProvider", definition.getAttribute("name"));
    assertEquals("high", definition.getAttribute("priority"));
  }

  @Test
  public void testNavigatorContentTriggerExpression_normalProject() throws CoreException {
    Element triggerPoints =
        findElement(
            "//plugin/extension[@point='org.eclipse.ui.navigator.navigatorContent']"
                + "/navigatorContent[@id='com.google.cloud.tools.eclipse.appengine.navigator']"
                + "/triggerPoints/*");
    Expression triggerExpression = checkExpression(triggerPoints);
    assertEquals(
        EvaluationResult.FALSE,
        triggerExpression.evaluate(new EvaluationContext(null, projectCreator.getProject())));
  }

  @Test
  public void testNavigatorContentTriggerExpression_appEngineStandardJavaProject()
      throws CoreException {
    Element triggerPoints =
        findElement(
            "//plugin/extension[@point='org.eclipse.ui.navigator.navigatorContent']"
                + "/navigatorContent[@id='com.google.cloud.tools.eclipse.appengine.navigator']"
                + "/triggerPoints/*");
    Expression triggerExpression = checkExpression(triggerPoints);
    projectCreator.withFacets(
        AppEngineStandardFacet.JRE7, JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25);
    assertEquals(
        EvaluationResult.TRUE,
        triggerExpression.evaluate(new EvaluationContext(null, projectCreator.getProject())));
  }

  @Test
  public void testNavigatorContentTriggerExpression_appEngineFlexibleWarJavaProject()
      throws CoreException {
    Element triggerPoints =
        findElement(
            "//plugin/extension[@point='org.eclipse.ui.navigator.navigatorContent']"
                + "/navigatorContent[@id='com.google.cloud.tools.eclipse.appengine.navigator']"
                + "/triggerPoints/*");
    Expression triggerExpression = checkExpression(triggerPoints);
    projectCreator.withFacets(
        AppEngineFlexWarFacet.FACET_VERSION, JavaFacet.VERSION_1_8, WebFacetUtils.WEB_31);
    assertEquals(
        EvaluationResult.TRUE,
        triggerExpression.evaluate(new EvaluationContext(null, projectCreator.getProject())));
  }

  @Test
  public void testNavigatorContentTriggerExpression_appEngineFlexibleJarJavaProject()
      throws CoreException {
    Element triggerPoints =
        findElement(
            "//plugin/extension[@point='org.eclipse.ui.navigator.navigatorContent']"
                + "/navigatorContent[@id='com.google.cloud.tools.eclipse.appengine.navigator']"
                + "/triggerPoints/*");
    Expression triggerExpression = checkExpression(triggerPoints);
    projectCreator.withFacets(AppEngineFlexJarFacet.FACET_VERSION, JavaFacet.VERSION_1_8);
    assertEquals(
        EvaluationResult.TRUE,
        triggerExpression.evaluate(new EvaluationContext(null, projectCreator.getProject())));
  }

  @Test
  public void testNavigatorContentActionProvider() throws CoreException {
    Element actionProvider =
        findElement(
            "//plugin/extension[@point='org.eclipse.ui.navigator.navigatorContent']"
                + "/navigatorContent[@id='com.google.cloud.tools.eclipse.appengine.navigator']"
                + "/actionProvider");
    assertEquals(AppEngineActionProvider.class.getName(), actionProvider.getAttribute("class"));
    assertEquals(
        "com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.actions",
        actionProvider.getAttribute("id"));
    Element actionProviderEnablement = findElement("enablement/*", actionProvider);
    Expression enablementExpression = checkExpression(actionProviderEnablement);
    assertEquals(
        EvaluationResult.TRUE,
        enablementExpression.evaluate(
            new EvaluationContext(null, mock(AppEngineProjectElement.class))));
    assertEquals(
        EvaluationResult.TRUE,
        enablementExpression.evaluate(new EvaluationContext(null, mock(CronDescriptor.class))));
    assertEquals(
        EvaluationResult.TRUE,
        enablementExpression.evaluate(
            new EvaluationContext(null, mock(DenialOfServiceDescriptor.class))));
    assertEquals(
        EvaluationResult.TRUE,
        enablementExpression.evaluate(
            new EvaluationContext(null, mock(DispatchRoutingDescriptor.class))));
    assertEquals(
        EvaluationResult.TRUE,
        enablementExpression.evaluate(
            new EvaluationContext(null, mock(DatastoreIndexesDescriptor.class))));
    assertEquals(
        EvaluationResult.TRUE,
        enablementExpression.evaluate(
            new EvaluationContext(null, mock(TaskQueuesDescriptor.class))));
  }

  @Test
  public void testAssociationWithProjectExplorer() {
    Element viewerContentBinding =
        findElement(
            "//plugin/extension[@point='org.eclipse.ui.navigator.viewer']/viewerContentBinding");
    assertEquals(IPageLayout.ID_PROJECT_EXPLORER, viewerContentBinding.getAttribute("viewerId"));
    findElement("includes", viewerContentBinding);
    Element contentExtension = findElement("includes/contentExtension", viewerContentBinding);
    assertEquals(
        "com.google.cloud.tools.eclipse.appengine.navigator",
        contentExtension.getAttribute("pattern"));
  }
}
