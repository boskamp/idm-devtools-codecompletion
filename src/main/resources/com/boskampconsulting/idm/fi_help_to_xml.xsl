<?xml version="1.0" encoding="UTF-8"?>
<!--
  Copyright 2017 Lambert Boskamp
  
  Licensed under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License.  You may obtain a copy
  of the License at
  
    http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
  License for the specific language governing permissions and limitations under
  the License.
-->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema">
  <xsl:output method="xml" indent="yes"/>

  <xsl:template match="/">
    <xsl:element name="functions">
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="topic[@label='Internal Functions']/topic">
    <xsl:call-template name="generate_js_code">
      <xsl:with-param
          name="iv_xhtml_content"
          select="document(@href)"
          as="document-node()"/>
    </xsl:call-template>

    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template
      name="generate_js_code"
      xpath-default-namespace="http://www.w3.org/1999/xhtml">
    <xsl:param name="iv_xhtml_content"/>

    <xsl:variable
        name="lv_function">

      <!-- Must use separator="" in XSLT 2.0 to get the concatenated -->
      <!-- string value of ALL nodes matched by select expression.   -->
      <xsl:value-of
          select="$iv_xhtml_content/html/head/title"
          separator=""/>
    </xsl:variable>
    <xsl:variable
        name="lv_description">
      <xsl:value-of
          select="$iv_xhtml_content//tr[1]/td[2]"
          separator=""/>
    </xsl:variable>
    <xsl:variable
        name="lv_syntax">
      <xsl:value-of
          select="$iv_xhtml_content//tr[2]/td[2]"
          separator=""/>
    </xsl:variable>

    <xsl:variable name="lv_out_param">
      <xsl:if test="contains($lv_syntax, '=')">

        <xsl:variable name="lv_syntax_before_equals">
          <xsl:value-of select="normalize-space(
                                substring-before($lv_syntax, '=')
                                )"/>
        </xsl:variable>

        <xsl:choose>
          <xsl:when test="starts-with(
                          $lv_syntax_before_equals
                          , 'Java: ')">

            <xsl:value-of
                select="normalize-space(
                        substring-after(
                        $lv_syntax_before_equals
                        , 'Java: '))"/>
          </xsl:when>

          <xsl:otherwise>
            <xsl:value-of select="normalize-space(
                                  $lv_syntax_before_equals)"/>
          </xsl:otherwise>
        </xsl:choose>

      </xsl:if> <!-- test="contains($lv_syntax, '=')"-->
    </xsl:variable>

    <xsl:element name="function">
      <xsl:attribute name="name">
        <xsl:value-of select="normalize-space($lv_function)"/>
      </xsl:attribute>

      <xsl:element name="help-url">
        <xsl:value-of select="document-uri($iv_xhtml_content)"/>
      </xsl:element>

      <xsl:element name="description">
        <xsl:value-of select="normalize-space($lv_description)"/>
      </xsl:element>

      <xsl:element name="parameters">
        <xsl:for-each
            select="$iv_xhtml_content//span[@class='keyword parmname']">

          <xsl:variable name="lv_param" select="normalize-space(.)"/>

          <xsl:variable
              name="lv_param_description">
            <xsl:value-of
                select="./ancestor::td[1]/following-sibling::td[1]"
                separator=""/>
          </xsl:variable>

          <xsl:if test="upper-case($lv_param)
                        !=
                        upper-case($lv_out_param)">

            <xsl:element name="input-parameter">
              <xsl:attribute name="name">
                <xsl:value-of select="$lv_param"/>
              </xsl:attribute>

              <xsl:attribute name="optional">
                <xsl:value-of
                    select="contains(
                            upper-case($lv_param_description)
                            , 'OPTIONAL.')"/>

              </xsl:attribute>

              <xsl:element name="description">
                <xsl:value-of
                    select="normalize-space($lv_param_description)"/>
              </xsl:element>
            </xsl:element>

          </xsl:if>

        </xsl:for-each>

        <xsl:if test="string-length($lv_out_param) &gt; 0">

          <xsl:element name="output-parameter">
            <xsl:attribute name="name">
              <xsl:value-of select="$lv_out_param"/>
            </xsl:attribute>
            <xsl:element name="description">
              <!-- <xsl:value-of -->
              <!--     select="normalize-space($lv_param_description)"/> -->
            </xsl:element>
          </xsl:element>

        </xsl:if>
      </xsl:element><!-- parameters -->

    </xsl:element><!--function -->

  </xsl:template>

</xsl:stylesheet>
