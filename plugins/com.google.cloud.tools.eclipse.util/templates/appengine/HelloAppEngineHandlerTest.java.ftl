<#if package != "">package ${package};

</#if>import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

public class HelloAppEngineHandlerTest {

  @Test
  public void test() throws IOException {
    MockHttpExchange httpExchange = new MockHttpExchange();
    new HelloAppEngineHandler().handle(httpExchange);
    Assert.assertEquals(200, httpExchange.getResponseCode());
    Assert.assertEquals("Hello App Engine!", httpExchange.getWrittenContents());
  }
}