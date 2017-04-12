<#if version == "2.5">
  <#assign namespace = "http://java.sun.com/xml/ns/javaee"
           schema = "web-app_2_5.xsd">
<#elseif version == "3.1">
  <#assign namespace = "http://xmlns.jcp.org/xml/ns/javaee"
           schema = "web-app_3_1.xsd">
</#if>
<?xml version="1.0" encoding="utf-8"?>
<web-app xmlns="${namespace}"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="${namespace} ${namespace}/${schema}"
         version="${version}">
  <servlet>
    <servlet-name>HelloAppEngine</servlet-name>
    <servlet-class>${package}HelloAppEngine</servlet-class>
  </servlet>
  <servlet-mapping>
    <servlet-name>HelloAppEngine</servlet-name>
    <url-pattern>/hello</url-pattern>
  </servlet-mapping>
  <welcome-file-list>
    <welcome-file>index.html</welcome-file>
  </welcome-file-list>
</web-app>