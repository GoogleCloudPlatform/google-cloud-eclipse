<?xml version="1.0" encoding="UTF-8"?>
<!--
	This stylesheet removes an <application/> element from appengine-web.xml
-->
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:appengine="http://appengine.google.com/ns/1.0">

  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="appengine:application"/>

</xsl:stylesheet>