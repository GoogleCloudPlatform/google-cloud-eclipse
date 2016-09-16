<#if package != "">package ${package};

</#if>import java.io.IOException;

import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Test;

public class HelloAppEngineTest {

  @Test
  public void test() throws IOException {
    MockHttpServletResponse response = new MockHttpServletResponse();
    new HelloAppEngine().doGet(null, response);
    Assert.assertThat(response.getContentType(), Is.is("text/plain"));
    Assert.assertThat(response.getWriterContent().toString(), Is.is("Hello App Engine!\n"));
  }
}
