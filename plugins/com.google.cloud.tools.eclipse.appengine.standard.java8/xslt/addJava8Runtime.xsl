<?xml version="1.0" encoding="UTF-8"?>
<!--
  This stylesheet adds or updates a <runtime> element to java8 in an appengine-web.xml
-->
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:appengine="http://appengine.google.com/ns/1.0"
  xmlns="http://appengine.google.com/ns/1.0">

  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>

  <!-- if no <runtime> elements found, add one -->
  <xsl:template match="/*[not(//appengine:runtime)]">
    <xsl:copy>
      <xsl:element name="runtime">java8</xsl:element>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>

  <!-- rewrite any existing <runtime> elements to java8 -->
  <xsl:template match="appengine:runtime">
    <xsl:element name="runtime">java8</xsl:element>
  </xsl:template>

</xsl:stylesheet>
