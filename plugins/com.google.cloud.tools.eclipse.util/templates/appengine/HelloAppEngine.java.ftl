<#if package != "">package ${package};

</#if>import java.io.IOException;

<#if servletVersion == "3.1">
import javax.servlet.annotation.WebServlet;
</#if>
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

<#if servletVersion == "3.1">
@WebServlet(
    name = "HelloAppEngine",
    urlPatterns = {"/hello"}
)
</#if>
public class HelloAppEngine extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) 
      throws IOException {

    response.setContentType("text/plain");
    response.setCharacterEncoding("UTF-8");

    response.getWriter().print("Hello App Engine!\r\n");

  }
}