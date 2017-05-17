<?xml version="1.0" encoding="UTF-8"?>
<!--
  This stylesheet removes a <runtime>java8</runtime> element from an appengine-web.xml
-->
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:appengine="http://appengine.google.com/ns/1.0">

  <xsl:template match="/">
  <xsl:text>
</xsl:text>
    <xsl:apply-templates select="*"/>
  </xsl:template>
  
  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>

  <!-- remove any <runtime>java8</runtime> elements -->
  <xsl:template match="appengine:runtime[normalize-space(text())='java8']"/>

</xsl:stylesheet>