/**
DuplicateFinder.groovy 12/05/2010
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

These few lines of code does quite a bit.. groovy..!
It
	a) Parses an XML file
	b) Identify all nodes that have duplicate ID attributes
	c) Calculate the count of duplicates
	d) and print them out..!

*/

class XmlDuplicateFinder extends AutodeployBase
{
	void findDuplicates(String xmlFileName)
	{
		def xmlConfig
		xmlConfig = new XmlSlurper().parse(new File(xmlFileName))

		Map uniqueMap=[:]
		xmlConfig.node.each
		{
			int count=1
			if(uniqueMap.get("${it.@id}") != null)
			{
				count = new Integer(uniqueMap.get("${it.@id}")).intValue() + 1
			}
			uniqueMap.put("${it.@id}",count)
		}

		uniqueMap.each{
		if(it.value > 1) println "${it.key} appears ${it.value} times"
		}
	}
}