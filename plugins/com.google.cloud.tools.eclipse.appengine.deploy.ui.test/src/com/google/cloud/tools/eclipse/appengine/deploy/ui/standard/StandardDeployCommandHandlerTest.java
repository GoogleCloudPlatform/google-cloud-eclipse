package com.google.cloud.tools.eclipse.appengine.deploy.ui.standard;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISources;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.cloud.tools.eclipse.appengine.deploy.ui.standard.StandardDeployCommandHandler;
import com.google.cloud.tools.eclipse.util.FacetedProjectHelper;

@RunWith(MockitoJUnitRunner.class)
public class StandardDeployCommandHandlerTest {

  @Mock private FacetedProjectHelper facetedProjectHelper;

  @Test(expected = ExecutionException.class)
  public void testExecute_facetedProjectCreationThrowsException() throws ExecutionException, CoreException {
    StandardDeployCommandHandler handler = new StandardDeployCommandHandler(facetedProjectHelper);

    when(facetedProjectHelper.getFacetedProject(isA(IProject.class))).thenThrow(getFakeCoreException());
    ExecutionEvent event = getTestExecutionEvent(mock(IProject.class));

    handler.execute(event);
  }

  private CoreException getFakeCoreException() {
    return new CoreException(getFakeErrorStatus());
  }

  private Status getFakeErrorStatus() {
    return new Status(IStatus.ERROR, "fakePluginId", "test exception");
  }

  private ExecutionEvent getTestExecutionEvent(Object project) {
    IEvaluationContext context = mock(IEvaluationContext.class);
    IStructuredSelection selection = mock(IStructuredSelection.class);
    when(selection.size()).thenReturn(1);
    when(selection.getFirstElement()).thenReturn(project);
    when(context.getVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME)).thenReturn(selection);
    when(context.getVariable(ISources.ACTIVE_SHELL_NAME)).thenReturn(mock(Shell.class));
    return new ExecutionEvent(null /*command */, Collections.EMPTY_MAP, null /* trigger */, context);
  }
}
