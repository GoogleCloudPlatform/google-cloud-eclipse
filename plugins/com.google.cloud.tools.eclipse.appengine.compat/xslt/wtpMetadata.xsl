<?xml version="1.0" encoding="UTF-8"?>
<!--
  This stylesheet removes the following GPE runtime and facets from the WTP facet metadata file:

    <runtime name="Google App Engine"/>
    <installed facet="com.google.appengine.facet" version="1"/>
    <installed facet="com.google.appengine.facet.ear" version="1"/>

  The metadata file is ".settings/org.eclipse.wst.common.project.facet.core.xml".
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="runtime[@name='Google App Engine']"/>
  <xsl:template match="installed[@facet='com.google.appengine.facet']"/>
  <xsl:template match="installed[@facet='com.google.appengine.facet.ear']"/>

</xsl:stylesheet>
