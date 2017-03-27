<?xml version="1.0" encoding="utf-8"?>
<!--
Copyright 2017 Foxysoft GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema">
  <xsl:output method="text" />
  <xsl:param name="iv_output_dir" select="."/>
  <xsl:variable name="BR">
    <!-- Carriage return -->
    <xsl:text>&#xd;</xsl:text>
    <!-- Line feed -->
    <xsl:text>&#xa;</xsl:text>
  </xsl:variable>
  <xsl:template match="@*|node()">
    <xsl:apply-templates select="@*|node()" />
  </xsl:template>
  <xsl:template match="functions">
    <xsl:apply-templates select="function" />
  </xsl:template>
  <xsl:template match="function">
    <xsl:result-document href="{$iv_output_dir}/{@name}">
      <xsl:text># name: </xsl:text>
      <xsl:value-of select="@name" />
      <xsl:value-of select="$BR"/>
      <xsl:text># --</xsl:text>
      <xsl:value-of select="$BR"/>
      <xsl:value-of select="@name"/>
      <xsl:text>(</xsl:text>
      <xsl:value-of select="$BR"/>
      <xsl:copy>
        <xsl:apply-templates select="*" />
      </xsl:copy>
      <xsl:text>)$0</xsl:text>
      <xsl:value-of select="$BR"/>
    </xsl:result-document>
  </xsl:template>
</xsl:stylesheet>
