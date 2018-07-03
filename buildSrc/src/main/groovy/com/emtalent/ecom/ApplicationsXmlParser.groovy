/**
ApplicationsXmlParser.groovy 10/02/2010

This groovy class provides methods for parsing and extracting data from the environment.xml file.
*/

package com.emtalent.atg

import org.gradle.api.*
import org.gradle.api.plugins.*
import groovy.util.slurpersupport.GPathResult
import groovy.util.slurpersupport.NodeChild

class ApplicationsXmlParser extends AtgBase	{

    // The environment XML File Object.  File to be read is in the gradle.properties file
    def xmlConfig
    
    public ApplicationsXmlParser(String applicationsXml)
	{
		xmlConfig = new XmlSlurper().parse(new File(applicationsXml))
	}

	/**
	get Application type (frontend or back)
	*/
	String getAppType(String appName)	
	{
		def app = xmlConfig.application.find{ it.@name.text().equals(appName) } 
		assert app.size() != 0, 'Error:  application name supplied is not valid:'+appName
		return(app.@type.text())		
	}
	
	/**
	get jbossInstanceTemplate name
	*/
	String getJbossInstanceTemplate(String appName)	
	{
		def app = xmlConfig.application.find{ it.@name.text().equals(appName) } 
		assert app.size() != 0, 'Error:  application name supplied is not valid:'+appName
		return(app.jbossInstance.@template.text())		
	}
	
	/**
	Get a list of apps for the specified appType
	**/
	List getAppsOfType(String appType)
	{
		return xmlConfig.application.findAll{it.@type.text().equals(appType)}.collect{it.@name.text()}
	}
	

}