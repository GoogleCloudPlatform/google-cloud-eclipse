<#if package != "">package ${package};

</#if>import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class HelloAppEngineSpringBoot {

  @RequestMapping("/")
  public String home() {
    return "Hello App Engine!";
  }

  public static void main(String[] args) {
    SpringApplication.run(HelloAppEngineSpringBoot.class, args);
  }
}