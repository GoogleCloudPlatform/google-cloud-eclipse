package @ServletPackageName@;
import java.io.IOException;
import javax.servlet.http.*;

@SuppressWarnings("serial")
public class HelloAppEngine extends HttpServlet {

  public void doGet(HttpServletRequest request, HttpServletResponse response) 
      throws IOException {
      
    response.setContentType("text/plain");
    response.getWriter().println("Hello App Engine!");
    
  }
}