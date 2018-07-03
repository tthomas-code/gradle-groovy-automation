/**
BaseConfigAdapterXml.groovy 10/02/2010

This groovy class provides methods for parsing and extracting data from the baseConfigAdapter xml file.
*/

package com.emtalent.atg

import org.gradle.api.*
import org.gradle.api.plugins.*
import groovy.util.slurpersupport.GPathResult
import groovy.util.slurpersupport.NodeChild


class BaseConfigAdapterXMLParser extends AtgBase	{

    def xmlConfig
    
    /**
    Constructor
    */
    public BaseConfigAdapterXMLParser(String baseConfigAdapterXml)
	{
		xmlConfig = new XmlSlurper().parse(new File(baseConfigAdapterXml))
	}
	
	/**
	return the server attribute for the specified application
	*/
	public String getServerForApp(String appName)
	{
		def application = xmlConfig.Applications.Application.find{ it.@name.text().equals(appName) }
		// Check that a valid application was found in the xml
		assert application.size() != 0, 'Error: application supplied is not valid:'+appName
		return application.@server.text()
	}
	
	/**
	return the slot attribute for the specified application
	*/
	public String getSlotForApp(String appName)
	{
		def application = xmlConfig.Applications.Application.find{ it.@name.text().equals(appName) }
		// Check that a valid application was found in the xml
		assert application.size() != 0, 'Error: application supplied is not valid:'+appName
		return application.@slot.text()	
	}
	
	/**
	return the datasource attribute for the specified application
	*/
	public String getDataSourceForApp(String appName)
	{
		def application = xmlConfig.Applications.Application.find{ it.@name.text().equals(appName) }
		// Check that a valid application was found in the xml
		assert application.size() != 0, 'Error: application supplied is not valid:'+appName
		return application.@dataSource.text()	
	}
	
	/**
	return a list of substitutions
	*/
	public Map getSubstitutions()
	{
		Map subMap = [:]
		xmlConfig.Substitutions.Substitution.each
		{
			subMap.put(it.@find.text(),it.@replacewith.text())
		}
		return subMap
	}
}