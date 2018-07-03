/**
AtgGlobalConfigProcessor.groovy 19/03/2010

A class to process the ATG global configuration data. It simply does a find and replace activity
on the ATG configuration files using the values in atg-env-global-substitutions.xml
*/
package com.emtalent.atg

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.logging.Logging
import com.emtalent.ecom.EnvXmlParser
import com.emtalent.ecom.BaseConfigAdapterXMLParser
import com.emtalent.common.BeanManager
import groovy.util.slurpersupport.GPathResult

class AtgGlobalConfigProcessor extends AtgBase 
{
	/**
	Delegates the ATG generation task to private methods
	*/
	void process(String environment,String application,String server,Project project)
	{
	
		logger.info(Logging.QUIET, "processing global atg config : Start.")	
		logger.info(Logging.QUIET, "======================================")
		
		addFilesToAtgData(environment,application,server,project)
		findAndReplace(environment,application,server,project)

		logger.info(Logging.QUIET, "processing global atg config : End")
		logger.info(Logging.QUIET, "-----------------------------------")
		logger.info(Logging.QUIET, "\n")			
	}
	
	/**
	Substitute values in files using the data from atg-global-config-substitutions.xml
	*/
	private void findAndReplace(String environment,String application, String server, Project project)
	{
		logger.info(Logging.QUIET, "\nReplacing values in global config files for ${server}")
		logger.info(Logging.QUIET, "******************************************************")

		def globalConfigFolder = "${project.atg_config_target}/${environment}/${server}"
		
		/**
		Substitute values from atg-global-config-substitutions.xmlhshsmp
		*/
		def xmlConfig = new XmlSlurper().parse(new File("${project.AUTO_DEP_ATG_HOME}/config/atg-global-config-substitutions.xml"))
		def applicationNode = xmlConfig.application.find{ it.@name.text().equals(application) }
		def envXmlParser = new EnvXmlParser("${project.AUTO_DEP_ATG_HOME}/config/env/environment.${environment}.xml")
		
		doEnvironmentSubstitutions(environment,applicationNode,globalConfigFolder)
	}
	
	/**
	Use values specified in atg-global-config-substitutions.xml and amend ATG configuration files appropriately.
	*/
	private void doEnvironmentSubstitutions(String environment, GPathResult applicationNode, String globalConfigFolder)
	{
		applicationNode.file.each()
		{
			def fileNode=it
			File file=new File("${globalConfigFolder}/${fileNode.@path.text()}")
			
			if (!file.exists())
			{
				logger.info(Logging.QUIET, "WARNING: An expected SIT configuration file does not exist :" + file.getPath() )
			}
			else
			{
				logger.info(Logging.QUIET, "Processing file: " + file.getPath() )
				def newFileText=new StringBuffer()
				def originalText=file.getText()
				def fileText=createProperties(originalText,fileNode)
				fileText.eachLine
				{
					String line=it
					String newLine = replace(line,fileNode.property,environment)
					newFileText.append(newLine)
				}
				file.write(newFileText.toString())
				if(!newFileText.toString().equals(originalText))
				{
					logger.info(Logging.QUIET, "Amended file: " + file.getPath() )
				}
			}
		}	
	}
	
	/**
	if a line is a valid key value pair where the key is in the given properties list, prepare a new line
	using the key and an environment specific value provided in the properties list.
	*/
	private String replace(String line, GPathResult property, String environment)
	{
		if(line.startsWith('#')) return line + '\n'
		if(!line.contains('=')) return line + '\n'

		def tokens=line.trim().tokenize('=')
		
		def propertyInThisLine = property.find { it.@name.text().equals(tokens.get(0).trim())}
		if(propertyInThisLine.size()==0) return line + '\n'
		
		def valueForThisEnvironment = propertyInThisLine.env.find{it.@name.text().equals(environment)}
		if(valueForThisEnvironment.size()==0) return line + '\n'
		
		return propertyInThisLine.@name.text() + '=' + valueForThisEnvironment.text() + '\n'
	}

	
	/**
	Add files from atg-global-custom-config.xml to ATGData
	*/
	private void addFilesToAtgData(String environment,String application,String server,Project project)
	{
		def atgCustomConfigManager = BeanManager.getBean("atgCustomConfigManager")

		logger.info(Logging.QUIET, "\nAdding custom atg global config files for ${server}")
		logger.info(Logging.QUIET, "********************************************************") 

		atgCustomConfigManager.doGlobalCustomConfiguration(environment,application,server,project)
	}


	/**
	From the fileNode, find out the properies that may have to be created if they don't already exist in the file
	- the contents of which are in 'originalText'. Then search in the originalText for each such property. 
	If not found, add the property to returnText.
	*/
	private String createProperties(String originalText, GPathResult fileNode)
	{
		def propertiesToCreate = fileNode.property.findAll{it.@create.text().toUpperCase().equals('TRUE')}
		def returnText=new StringBuffer(originalText)
		
		propertiesToCreate.each
		{
			def property=it
			boolean found=false			
			returnText.toString().eachLine()
			{
				if(found) return //bad code , but groovy doesn't support continue and break in closures.
				
				def line=it
				if(line.startsWith('#')) return
				if(!line.contains('=')) return			
				def tokens=line.trim().tokenize('=')
				def propertyInThisLine = tokens.get(0).trim()
				if (property.@name.text().equals(propertyInThisLine))
				{
					found=true
				}
			}
			if(!found)
			{
				returnText.append("\n${property.@name.text()}=")
			}
		}
		return returnText		
	}	
}
