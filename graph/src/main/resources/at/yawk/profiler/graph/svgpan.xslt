<?xml version="1.0"?>

<xsl:stylesheet version="1.0"
                xmlns="http://www.w3.org/2000/svg"
                xmlns:svg="http://www.w3.org/2000/svg"
                xmlns:xlink="http://www.w3.org/1999/xlink"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:param name="svgpan-uri"/>

    <xsl:output method="xml" indent="yes"/>

    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="/svg:svg/svg:g">
        <script xlink:href="{$svgpan-uri}"/>
        <g id="viewport">
            <xsl:copy>
                <xsl:apply-templates select="node()|@*"/>
            </xsl:copy>
        </g>
    </xsl:template>

    <xsl:template match="/svg:svg/@viewBox">
    </xsl:template>
</xsl:stylesheet>
