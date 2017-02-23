<?xml version="1.0" encoding="UTF-8"?>
<!--
	This stylesheet changes the servlet in web.xml to Servlet 2.5.
-->
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:ns1="http://xmlns.jcp.org/xml/ns/javaee"
	xmlns:ns2="http://java.sun.com/xml/ns/javaee"
	exclude-result-prefixes="ns1 ns2">
  
  <xsl:output method="xml" indent="yes"/>

  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>
   
  <xsl:template match="/ns1:web-app" >
  	<web-app xmlns="http://java.sun.com/xml/ns/javaee"
	      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	      xsi:schemaLocation="http://java.sun.com/xml/ns/javaee
	      http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd"
	      version="2.5">
      <xsl:apply-templates select="*" />
    </web-app>
  </xsl:template>

  <xsl:template match="/ns2:web-app[@version='3.0']" >
  	<web-app xmlns="http://java.sun.com/xml/ns/javaee"
	      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	      xsi:schemaLocation="http://java.sun.com/xml/ns/javaee
	      http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd"
	      version="2.5">
      <xsl:apply-templates select="*" />
    </web-app>
  </xsl:template>

  <xsl:variable name="ns" select=
      "document('')/*/namespace"/>
 
  <xsl:template match="*">
    <xsl:choose>
      <xsl:when test="namespace-uri()=$ns">
        <xsl:element name="{name()}" namespace="{namespace-uri()}">
          <xsl:apply-templates select="@*|node()"/>
        </xsl:element>
      </xsl:when>
      <xsl:otherwise>
        <xsl:element name="{name()}" namespace="{$ns}">
          <xsl:apply-templates select="@*|node()"/>
        </xsl:element>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template> 
 
</xsl:stylesheet>