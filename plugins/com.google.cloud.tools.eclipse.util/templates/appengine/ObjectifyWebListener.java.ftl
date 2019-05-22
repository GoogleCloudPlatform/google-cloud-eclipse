<#if package != "">package ${package};

</#if>import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
<#if servletVersion != "2.5">import javax.servlet.annotation.WebListener;
</#if>

import com.googlecode.objectify.ObjectifyService;

<#if servletVersion != "2.5">@WebListener
</#if>public class ObjectifyWebListener implements ServletContextListener {

  @Override
  public void contextInitialized(ServletContextEvent event) {
    ObjectifyService.init();
    // This is a good place to register your POJO entity classes.
    // ObjectifyService.register(YourEntity.class);
  }

  @Override
  public void contextDestroyed(ServletContextEvent event) {
  }
}