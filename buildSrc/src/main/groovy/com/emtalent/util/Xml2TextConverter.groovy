/**
XmlDuplicateFinder.groovy 12/05/2010
*/
package com.emtalent.util

import com.emtalent.common.AutodeployBase
import java.io.InputStream
import java.io.OutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import org.gradle.api.*
import org.gradle.api.plugins.*
import groovy.util.slurpersupport.GPathResult
import groovy.util.slurpersupport.NodeChild

/**
it is a nice little piece of code that demonstrates how easy life is with groovy.
*/

class Xml2TextConverter extends AutodeployBase 
{
	void convert(String xmlFileName)
	{
		def xmlConfig
		xmlConfig = new XmlSlurper().parse(new File(xmlFileName))

		Map uniqueMap=[:]
		xmlConfig.node.each
		{
			int count=1
			println "${it.@name}^${it.@id}^${it.@parent}"
		}
	}
}