<?xml version="1.0" encoding="UTF-8"?>
<!--
	This stylesheet replaces the groupId element in pom.xml.
-->
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:pom="http://maven.apache.org/POM/4.0.0">

  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="pom:groupId/text()">com.google.cloud.tools</xsl:template>

</xsl:stylesheet>