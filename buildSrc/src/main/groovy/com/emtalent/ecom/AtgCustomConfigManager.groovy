/**
AtgCustomConfigManager.groovy 04/06/2010

There are a number of files that AUTO-DEP add to the ATG configuration data.
This class uses an XML definition (atg-custom-config.xml) to prepare and all such files.
*/

package com.emtalent.atg

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.logging.Logging
import groovy.util.slurpersupport.GPathResult

class AtgCustomConfigManager extends AtgBase 
{
	/**
	As part of preparing ATG configuration files, AUTO-DEP adds files to and removes from the 
	base SIT configuration supplied by the offshore supplier.
	
	This method does this task of custom configuration using the data supplied by atg-custom-config.xml
	*/
	public void doCustomConfiguration(environment,application,serverName,slot,slotType,project)
	{
		def customConfigXml = new XmlSlurper().parse(new File("${project.AUTO_DEP_ATG_HOME}/config/atg-custom-config.xml"))
		def applicationXml = customConfigXml.application.find{it.@name.text().equals(application)}
		
		//return if no custom configuration defined for an application.
		if(applicationXml.size()==0) return
		
		def envXml=applicationXml.env.find{it.@name.text().equals(environment)}
		
		//return if no custom configuration defined for an environment.
		if(envXml.size()==0) return

		def sourcePath="${project.AUTO_DEP_HOME}/${project.atgConfigLocation}"
		def destinationPath="${project.atg_config_target}/${environment}/${serverName}/${slot}"
		
		//First copy items that are defined for all types of slot
		def itemsToCopy = envXml.copy.item.findAll{it.@slotType.text().equals('*')}
		copyItems(itemsToCopy,sourcePath,destinationPath,project)
		
		//Then copy items that are defined for specific type of slots. This file will 
		//take priority if the same file has been defined for '*'
		itemsToCopy = envXml.copy.item.findAll{it.@slotType.text().contains("[${slotType}]")}
		copyItems(itemsToCopy,sourcePath,destinationPath,project)		

		def itemsToDelete = envXml.delete.item.findAll{it.@slotType.text().contains("[${slotType}]") || it.@slotType.text().equals('*')}		
		deleteItems(itemsToDelete,destinationPath,project)
	}
	

	/**
	As part of preparing ATG configuration files, AUTO-DEP adds files to
	D:\Deployments\ATG-Data
	These files are defined in atg-global-custom-config.xml.
	This method adds the files to the ATG Configurations that are being  prepared for deployment.
	*/
	public void doGlobalCustomConfiguration(environment,application,serverName,project)
	{
		def sourcePath="${project.AUTO_DEP_HOME}/${project.atgConfigLocation}"
		def destinationPath="${project.atg_config_target}/${environment}/${serverName}"
		
		// Delete if previously generated config exists.
		project.ant.delete(dir:"${destinationPath}/localconfig")
		logger.info(Logging.QUIET, "deleted ${destinationPath}/localconfig")	
		project.ant.mkdir(dir:"${destinationPath}/localconfig")
		logger.info(Logging.QUIET, "created ${destinationPath}/localconfig")		
						
		def customConfigXml = new XmlSlurper().parse(new File("${project.AUTO_DEP_ATG_HOME}/config/atg-global-custom-config.xml"))
		def applicationXml = customConfigXml.application.find{it.@name.text().equals(application)}
		
		//return if no custom configuration defined for an application.
		if(applicationXml.size()==0) return
		
		def envXml=applicationXml.env.find{it.@name.text().equals(environment)}
		
		//return if no custom configuration defined for an environment.
		if(envXml.size()==0) return
	
		copyItems(envXml.copy.item,sourcePath,destinationPath,project)	
	}
	
		
	/**
	Copy items in the list 'itemsToCopy' from sourcePath to destinationPath
	*/
	public void copyItems(itemsToCopy,sourcePath,destinationPath,project)
	{
		itemsToCopy.each()
		{
			
			def item=it
			def overwrite=true
			
			if (item.@overwrite.text().toUpperCase().equals("FALSE"))
			{
				overwrite=false
			}
			
			if(item.source.@type.text().equals("file"))
			{
				if(item.destination.@type.text().equals("file"))
				{
					project.ant.copy(file:"${sourcePath}/${item.source.text()}",toFile:"${destinationPath}/${item.destination.text()}",overwrite:overwrite)
				}
				else if (item.destination.@type.text().equals("folder"))
				{
					project.ant.copy(file:"${sourcePath}/${item.source.text()}",toDir:"${destinationPath}/${item.destination.text()}",overwrite:overwrite)
				}
			}
			else if (item.source.@type.text().equals("folder"))
			{
				if(!new File("${destinationPath}/${item.destination.text()}").exists())
				{
					project.ant.mkdir(dir:"${destinationPath}/${item.destination.text()}")
				}
				project.ant.copy(toDir:"${destinationPath}/${item.destination.text()}",overwrite:overwrite)
				{
					fileset(dir:"${sourcePath}/${item.source.text()}")
				}
			}
			logger.info(Logging.QUIET, "Copied ${sourcePath}/${item.source.text()} to ${destinationPath}/${item.destination.text()}")					
		}			
	}
	
	/**
	Delete items in the list 'itemsToDelete' from destinationPath
	*/
	public void deleteItems(itemsToDelete,destinationPath,project)
	{
		itemsToDelete.each()
		{
			def item=it
			if(item.@type.text().equals('file'))
			{
				project.ant.delete(file:"${destinationPath}/${item.text()}")
			}
			else if(item.@type.text().equals('folder'))
			{
				project.ant.delete(dir:"${destinationPath}/${item.text()}")
			}
			logger.info(Logging.QUIET, "Deleted ${item.@type.text()} ${destinationPath}/${item.text()}")	
		}	
	}
	
}