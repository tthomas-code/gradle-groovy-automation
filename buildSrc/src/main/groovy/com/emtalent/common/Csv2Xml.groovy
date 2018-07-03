/**
Csv2Xml.groovy 12/05/2010
*/
package com.emtalent.common

import java.io.InputStream
import java.io.OutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import org.gradle.api.*

class Csv2Xml extends AutodeployBase 
{
	/**
	Convert a CSV file to an XML file
	*/
	void convert(String source, String destination, Project project)
	{
		boolean justStarted=true
	
		def headers
		def values

		StringBuffer xml=new StringBuffer('<AUDIT>')

		new File(source).eachLine()
		{

			if(justStarted)
			{
				justStarted=false
				headers=it.tokenize(",")
			}
			else
			{
				values=it.tokenize(",")
				xml.append("<ENTRY>")
				for (def i=0;i<headers.size();i++)
				{
					xml.append("<KEY NAME=\"${headers.get(i)}\" VALUE=\"${values.get(i)}\"/>")
				}
				xml.append("</ENTRY>")
			}
		}
		xml.append("</AUDIT>")
		
		def stringWriter = new StringWriter() 
		def node = new XmlParser().parseText(xml.toString()); 
		new XmlNodePrinter(new PrintWriter(stringWriter)).print(node) 
		File dest = new File(destination)
		dest.write("<?xml-stylesheet type=\"text/xsl\" href=\"${project.AUTO_DEP_ATG_HOME}\\scripts\\xsl\\autodeploy-AUDIT.xsl\"?>")
		dest.append(stringWriter.toString())
		
	}
}