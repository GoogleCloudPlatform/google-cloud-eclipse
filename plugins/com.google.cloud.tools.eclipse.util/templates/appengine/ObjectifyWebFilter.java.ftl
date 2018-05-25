<#if package != "">package ${package};

</#if>import javax.servlet.annotation.WebFilter;

import com.googlecode.objectify.ObjectifyFilter;

/**
 * Filter required by Objectify to clean up any thread-local transaction contexts and pending
 * asynchronous operations that remain at the end of a request.
 */
@WebFilter(urlPatterns = {"/*"})
public class ObjectifyWebFilter extends ObjectifyFilter {}