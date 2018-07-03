/**
BindingsParser.groovy 10/02/2010

This groovy class provides methods for parsing an XML file that holds binding values for various configurations such as 
JBOSS and OPENDEPLOY.
*/

package com.emtalent.common

import org.gradle.api.*
import org.gradle.api.plugins.*
import groovy.util.slurpersupport.GPathResult

class BindingsParser extends AutodeployBase	{

	Map getBindings(File bindingsXML, String configName)
	{
		def parsedXML = new XmlSlurper().parse(bindingsXML)
		
		if(parsedXML.@type.text().equals('1'))
		{
			return getType1Bindings(parsedXML, configName)
		}
		else if (parsedXML.@type.text().equals('2'))
		{
			return getType2Bindings(parsedXML, configName)
		}
	}

	/**
	Return a Map of bindings defined in the specified xml file for the specified config element.
	*/
	
	private Map getType1Bindings(GPathResult parsedXML, String configName) 
	{
		def config = parsedXML.config.find{ it.@name.text().equals(configName) } 
		
		assert config.size() != 0, 'Error: configName supplied is not valid'
		
		def bindingsMap=[:]
		
		config.key.each
		{
			bindingsMap.put(it.@name.text(),it.text())	
		}
	
		return(bindingsMap)					
	}
	
	private Map getType2Bindings(GPathResult parsedXML, String configName) 
	{
		def bindingsMap=[:]
		
		parsedXML.key.each
		{
			def value = it.value.find{ it.@for.text().equals(configName) }
			bindingsMap.put(it.@name.text(),value.text())
		}

		return(bindingsMap)					
	}

	Map getAtgBindings(File bindingsXML, String env, String slotType) 
	{
		
		def bindingsMap=[:]
		
		def parsedXML = new XmlSlurper().parse(bindingsXML)

		parsedXML.key.each
		{
			def value = it.value.find{ it.@for.text().equals(env) && it.@slotType.text().contains("[${slotType}]") }			
			if(value.size()==0)
			{
				value = it.value.find{ it.@for.text().equals(env) && it.@slotType.text().equals('*') }
			}
			bindingsMap.put(it.@name.text(),value.text())
		}
		return(bindingsMap)					
	}


	Map getBaseConfigAtgBindings(File bindingsXML, String baseEnv, String targetEnv)
	{
		def bindingsMap=[:]
		
		def parsedXML = new XmlSlurper().parse(bindingsXML)
		parsedXML.key.each
		{
			def baseConfig = it.baseconfig.find{it.@baseEnv.text().equals(baseEnv)}
			assert baseConfig.size() == 1, "Error: Invalid configuration in atg-env-bindings.xml for key ${it.@name.text()}. Please review."
			
			def value = baseConfig.value.find{it.@targetEnv.text().equals(targetEnv)}
			if(value.size()==0)
			{
				value = baseConfig.value.find{it.@targetEnv.text().equals('*')}
			}
			bindingsMap.put(it.@name.text(),value.text())
		}
		return(bindingsMap)					
	}


	Map getJbossBindings(File bindingsXML, String app, String env, String slotType, String slot) 
	{
		
		def bindingsMap=[:]	
		def parsedXML = new XmlSlurper().parse(bindingsXML)
		parsedXML.key.each
		{
			def application = it.application.find {it.@name.text().contains("[${app}]")}
			if(application.size()==0)
			{
				application = it.application.find {it.@name.text().equals('*')}
			}
			
			def environment = application.env.find {it.@name.text().contains("[${env}]")}
			if(environment.size()==0)
			{
				environment = application.env.find {it.@name.text().equals('*')}
			}			

			def value = environment.value.find{ it.@slotType.text().contains("[${slotType}]") && it.@slot.text().equals(slot) }
			
			if(value.size()==0)
			{			
				value = environment.value.find{ it.@slotType.text().equals('*') && it.@slot.text().equals(slot) }			
			}
			if(value.size()==0)
			{
				value = environment.value.find{ it.@slotType.text().contains("[${slotType}]") && it.@slot.text().equals('*') }
			}
			if(value.size()==0)
			{
				value = environment.value.find{ it.@slotType.text().equals('*') && it.@slot.text().equals('*') }
			}			
			bindingsMap.put(it.@name.text(),value.text())
		}
		return(bindingsMap)					
	}
}
