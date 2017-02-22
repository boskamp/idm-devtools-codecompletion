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
	<xsl:template match="@*|node()">
		<xsl:apply-templates select="@*|node()" />
	</xsl:template>
	<xsl:template match="/">
		<xsl:text>{"!name":"idm",</xsl:text>
		<xsl:copy>
			<!-- Context position of the CURRENT function -->
			<!-- within ALL function elements will be required -->
			<!-- INSIDE the function template rule, so we must -->
			<!-- set up a context sequence that only contains -->
			<!-- function elements, nothing else. -->
			<xsl:apply-templates select="//function" />
		</xsl:copy>
		<xsl:text>}</xsl:text>
	</xsl:template>
	<xsl:template match="function">
		<xsl:text>"</xsl:text>
		<xsl:value-of select="@name" />
		<xsl:text>":{</xsl:text>
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" />
		</xsl:copy>
		<xsl:text>}</xsl:text>
		<xsl:if test="not(position()=last())">
			<xsl:text>, </xsl:text>
		</xsl:if>
	</xsl:template>
	<xsl:template match="parameters">
		<xsl:text>"!type": "fn(</xsl:text>
		<xsl:copy>
			<xsl:apply-templates select="input-parameter" />
		</xsl:copy>
		<xsl:text>)</xsl:text>
		<xsl:copy>
			<xsl:apply-templates select="output-parameter" />
		</xsl:copy>
		<xsl:text>"</xsl:text>
	</xsl:template>
	<xsl:template match="input-parameter">
		<xsl:value-of select="@name" />
		<xsl:if test="@optional = 'true'">
			<xsl:text>?</xsl:text>
		</xsl:if>
		<xsl:text>: string</xsl:text>
		<xsl:if test="following-sibling::*[1][self::input-parameter]">
			<xsl:text>, </xsl:text>
		</xsl:if>
	</xsl:template>
	<xsl:template match="output-parameter">
		<xsl:text> -> string</xsl:text>
	</xsl:template>
	<xsl:template match="help-url">
		<xsl:text>"!url": "file://</xsl:text>
		<xsl:value-of select="." />
		<xsl:text>",</xsl:text>
	</xsl:template>
	<xsl:template match="function/description">
		<xsl:text>"!doc": "</xsl:text>
		<xsl:value-of select="replace(replace(., '\\', '\\\\'), '&quot;', '\\&quot;')" />
		<xsl:text>",</xsl:text>
	</xsl:template>
</xsl:stylesheet>
