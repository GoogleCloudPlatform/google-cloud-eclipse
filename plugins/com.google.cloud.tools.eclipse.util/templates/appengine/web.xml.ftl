<?xml version="1.0" encoding="utf-8"?>
<web-app xmlns="${namespace}"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="${namespace} ${schemaUrl}"
         version="${servletVersion}">
<#if servletVersion == "2.5">
  <servlet>
    <servlet-name>HelloAppEngine</servlet-name>
    <servlet-class>${package}HelloAppEngine</servlet-class>
  </servlet>
  <servlet-mapping>
    <servlet-name>HelloAppEngine</servlet-name>
    <url-pattern>/hello</url-pattern>
  </servlet-mapping>
</#if>
  <welcome-file-list>
    <welcome-file>index.html</welcome-file>
    <welcome-file>index.jsp</welcome-file>
  </welcome-file-list>
</web-app>