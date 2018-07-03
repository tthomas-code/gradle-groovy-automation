/**
TemplateEngine.groovy  11/Feb/2010
*/

package com.emtalent.common

import org.gradle.api.*
import org.gradle.api.plugins.*
import groovy.util.XmlNodePrinter
import groovy.text.SimpleTemplateEngine
import groovy.util.Node;
import groovy.util.XmlParser;
import java.io.PrintWriter
import java.io.StringWriter

class TemplateEngine extends AutodeployBase	
	{

	/**
	This method creates a file based on a template and a set of bindings.
	
	param: binding - A set of key value pairs containing the replacements
	param: template - The  template.
	param: outputFile - The resulting combination of the binding and the template
	*/
	void makeTemplate(Map binding, File template, File outputFile) {

		def text=template.getText();
		
		binding.each
		{
			text=text.replaceAll('#'+it.key+'#', it.value)
		}
		outputFile.write(text)
	}
	
	
	/**
	Apply bindings to the the text of the supplied node object
	**/
	Node makeTemplate(Map binding, Node template)
	{
		StringWriter sWriter=new StringWriter()
		XmlParser parser=new XmlParser()
		
		new XmlNodePrinter(new PrintWriter(sWriter)).print(template)
		def text = sWriter.getBuffer().toString()
		binding.each
		{
			text=text.replaceAll('#'+it.key+'#', it.value)
		}
		return parser.parseText(text.toString());
	}

	
}

