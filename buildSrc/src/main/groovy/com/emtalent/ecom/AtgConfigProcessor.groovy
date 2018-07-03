/**
ATGConfigProcessor.groovy 19/03/2010

A class to process the ATG configuration data. It simply does a find and replace activity
on the ATG configuration files using the values in atg-env-substitutions.xml
*/
package com.emtalent.atg

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.logging.Logging
import com.emtalent.ecom.EnvXmlParser
import com.emtalent.ecom.BaseConfigAdapterXMLParser
import com.emtalent.common.BeanManager
import groovy.util.slurpersupport.GPathResult

class AtgConfigProcessor extends AtgBase 
{
	/**
	Delegates the ATG generation task to private methods
	*/
	void process(String environment,String application,String server,String slot,Project project)
	{
	
		logger.info(Logging.QUIET, "processATGConfig : Start.")	
		logger.info(Logging.QUIET, "============================================================")
		
		if ("${project.validateAtgXmlConfig}".toUpperCase().equals("TRUE"))
		{
			validateAtgXmlConfig(environment,application,server,slot,project)
		}
		
		// Delete if previously generated config exists.
		project.ant.delete(dir:"${project.atg_config_target}/${environment}/${server}/${slot}")
		
		copySITConfigToSlot(environment,application,server,slot,project)
		copyTemplatesToSlot(environment,application,server,slot,project) 		
		addFilesToSlot(environment,application,server,slot,project)
		findAndReplace(environment,application,server,slot,project)
		applyServerBindings(environment,application,server,slot,project)

		/** 
		Please note that apply bindings should be done after findAndReplace()
		This is because of what we do with the localDirectory property in
		OutreachWebAppFileSystem.properties within applyBindings method.
		*/
		applyBindings(environment,application,server,slot,project)
		
		/**
		There are a few files that can only be processed programmatically.
		*/
		applyExceptions(environment,application,server,slot,project)
		
		logger.info(Logging.QUIET, "processATGConfig : End.")
		logger.info(Logging.QUIET, "------------------------------------------------------------")
		logger.info(Logging.QUIET, "\n")			
	}
	
	/**
	Generate configuration data for each server and slot for the specified application.
	*/
	private void copySITConfigToSlot(String environment,String application, String server, String slot,Project project)
	{
		logger.info(Logging.QUIET, "\nCreating atg config folder for ${server}/${slot}")
		logger.info(Logging.QUIET, "**************************************************")
				
		def baseConfigAdapterXMLParser = new BaseConfigAdapterXMLParser("${project.AUTO_DEP_ATG_HOME}/config/${project.baseConfigAdapter}")
		def baseConfigSlot = baseConfigAdapterXMLParser.getSlotForApp(application)
		def baseConfigServer = baseConfigAdapterXMLParser.getServerForApp(application)
		def sourceFolder = "${project.AUTO_DEP_GOODS_IN}/${project.atgConfigForSIT}/${baseConfigServer}/${baseConfigSlot}/localconfig"
		def configFolder = "${project.atg_config_target}/${environment}/${server}/${slot}/localconfig"
		
		project.ant.delete(dir:configFolder)
		project.ant.mkdir(dir:configFolder)
		
		project.ant.copy(todir:configFolder,overwrite:true) 
		{
			fileset(dir:sourceFolder) 			
		}
		
		logger.info(Logging.QUIET, "Copied files from: ${sourceFolder} to ${configFolder}" )
	}	
	
	/**
	Copy ATG Config Templates. 
	Please note that this action will overwrite some of the ATG configuration files supplied by Infosys.
	*/
	private void copyTemplatesToSlot(String environment, String application, String server, String slot, Project project)
	{
		def baseConfigAdapterXMLParser = new BaseConfigAdapterXMLParser("${project.AUTO_DEP_ATG_HOME}/config/${project.baseConfigAdapter}")
		def baseConfigSlot = baseConfigAdapterXMLParser.getSlotForApp(application)
		def sourceFolder = "${project.AUTO_DEP_HOME}/${project.atgConfigLocation}/ATG-Data/${application}/${environment}/${baseConfigSlot}"
		def slotFolder = "${project.atg_config_target}/${environment}/${server}/${slot}"
				
		logger.info(Logging.QUIET, "\nCopying atg config templates to ${slotFolder}")
		logger.info(Logging.QUIET, "**********************************************") 

		project.ant.copy(todir:slotFolder,overwrite:true) 
		{
			fileset(dir:sourceFolder) 			
		}
		logger.info(Logging.QUIET, "Copied files from: ${sourceFolder} to ${slotFolder}" )
	}
	
	
	/**
	Substitute values in files using the data from atg-env-substitutions.xml
	*/
	private void findAndReplace(String environment,String application, String server, String slot, Project project)
	{
		logger.info(Logging.QUIET, "\nReplacing values in config files for ${server}/${slot}")
		logger.info(Logging.QUIET, "******************************************************")

		def slotFolder = "${project.atg_config_target}/${environment}/${server}/${slot}"
		
		/**
		Substitute values from atg-env-substitutions.xml
		*/
		def xmlConfig = new XmlSlurper().parse(new File("${project.AUTO_DEP_ATG_HOME}/config/atg-env-${application}-substitutions.xml"))
		def applicationNode = xmlConfig.application.find{ it.@name.text().equals(application) }

		def envXmlParser = new EnvXmlParser("${project.AUTO_DEP_ATG_HOME}/config/env/environment.${environment}.xml")
		def slotType = envXmlParser.getSlotType(environment, application,server,slot)
		
		doEnvironmentSubstitutions(environment,application,server,slot,applicationNode,slotFolder,slotType,project)
		
		/**
		Substitute values from atg-slot-substitutions.xml
		*/
		xmlConfig = new XmlSlurper().parse(new File("${project.AUTO_DEP_ATG_HOME}/config/atg-slot-substitutions.xml"))
		
		applicationNode = xmlConfig.application.find{ it.@name.text().equals('*') }
		doSlotSubstitutions(slot,slotType,applicationNode,slotFolder,environment,application,server,project)
		
		applicationNode = xmlConfig.application.find{ it.@name.text().equals(application) }
		doSlotSubstitutions(slot,slotType,applicationNode,slotFolder,environment,application,server,project)	
	}
	
	/**
	Use values specified in atg-slot-substitutions.xml and amend ATG configuration files appropriately.
	*/
	private void doSlotSubstitutions(String slot, String slotType, GPathResult applicationNode, String slotFolder, String environment, String application, String server, Project project)
	{
		applicationNode.file.each()
		{
			def fileNode=it
			File file=new File("${slotFolder}/${fileNode.@path.text()}")
			
			if (!file.exists())
			{
				logger.info(Logging.QUIET, "WARNING: An expected SIT configuration file does not exist :" + file.getPath() )
			}
			else
			{
				logger.info(Logging.QUIET, "Processing file: " + file.getPath() )
				def newFileText=new StringBuffer()
				def originalText=file.getText()
				file.eachLine
				{
					String line=it
					String newLine = replaceSlotValue(line,fileNode.property,slot,slotType,environment,application,server,project)
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
	Use values specified in atg-env-substitutions.xml and amend ATG configuration files appropriately.
	*/
	private void doEnvironmentSubstitutions(String environment, String application, String server, String slot,GPathResult applicationNode, String slotFolder,String slotType, Project project)
	{
		applicationNode.file.each()
		{
			def fileNode=it
			File file=new File("${slotFolder}/${fileNode.@path.text()}")
			
			if (!file.exists())
			{
				logger.info(Logging.QUIET, "WARNING: An expected SIT configuration file does not exist :" + file.getPath() )
			}
			else
			{
				logger.info(Logging.QUIET, "Processing file: " + file.getPath() )
				def newFileText=new StringBuffer()
				def originalText=file.getText()
				def fileText=createProperties(originalText,fileNode,environment,slotType)
				fileText.eachLine
				{
					String line=it
					String newLine = replace(line,fileNode.property,environment,application,server,slot,slotType,project)
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
	private String replace(String line, GPathResult property, String environment,String application, String server, String slot, String slotType, Project project)
	{
		if(line.startsWith('#')) return line + '\n'
		if(!line.contains('=')) return line + '\n'

		def tokens=line.trim().tokenize('=')
		
		def propertyInThisLine = property.find { it.@name.text().equals(tokens.get(0).trim())}
		if(propertyInThisLine.size()==0) return line + '\n'

		/**
		//Commented out.
		if (tokens.size()>2)
		{
			logger.error(Logging.QUIET, "Invalid configuration line found in ATG Config files ==>" + line )
			assert false
		}		
		*/
		
		def valueForThisEnvironment = propertyInThisLine.env.find{it.@name.text().equals(environment) & it.@slotType.text().equals(slotType)}
		if(valueForThisEnvironment.size()==0)
		{
			valueForThisEnvironment = propertyInThisLine.env.find{it.@name.text().equals(environment)& it.@slotType.text().equals('')}
		}
		if(valueForThisEnvironment.size()==0) return line + '\n'
		
		def envNavigator=BeanManager.getBean("environmentNavigator")
		return propertyInThisLine.@name.text() + '=' + envNavigator.applySlotContext(valueForThisEnvironment.text(),environment,application,server,slot,project) + '\n'
	}

	/**
	if a line is a valid key value pair where the key is in the given properties list, prepare a new line
	using the key and an slot specific value provided in the properties list.
	*/
	private String replaceSlotValue(String line, GPathResult property, String slot, String slotType, String environment,String application,String server, Project project)
	{
		if(line.startsWith('#')) return line + '\n'
		if(!line.contains('=')) return line + '\n'

		def tokens=line.trim().tokenize('=')
		
		def propertyInThisLine = property.find { it.@name.text().equals(tokens.get(0).trim()) & it.@env.text().equals(environment)}
		if(propertyInThisLine.size()==0)
		{
			propertyInThisLine = property.find { it.@name.text().equals(tokens.get(0).trim()) & it.@env.text().equals('*')}
		}
		if(propertyInThisLine.size()==0) return line + '\n'

		if (tokens.size()>2)
		{
			logger.error(Logging.QUIET, "Invalid configuration line found in ATG Config files ==>" + line )
			assert false
		}		

		//slotType takes priority over slot number
		def valueForThisSlot = propertyInThisLine.slot.find{it.@type.text().equals(slotType)}
		if(valueForThisSlot.size()==0)
		{
			valueForThisSlot = propertyInThisLine.slot.find{it.@name.text().equals(slot)}
		}
		
		if(valueForThisSlot.size()==0) return line + '\n'
		
		def envNavigator=BeanManager.getBean("environmentNavigator")		
				
		return propertyInThisLine.@name.text() + '=' + envNavigator.applySlotContext(valueForThisSlot.text(),environment,application,server,slot,project) + '\n'
	}	
	
	
	/**
	Substitute values in files using the data from atg-env-bindings.xml
	*/
	private void applyBindings(String environment,String application, String server, String slot, Project project)
	{
		def baseConfigAdapterXMLParser = new BaseConfigAdapterXMLParser("${project.AUTO_DEP_ATG_HOME}/config/${project.baseConfigAdapter}")
		def envXmlParser = new EnvXmlParser("${project.AUTO_DEP_ATG_HOME}/config/env/environment.${environment}.xml")
		def bindingsParser = BeanManager.getBean("bindingsParser")
		def envNavigator=BeanManager.getBean("environmentNavigator")		
	    def slotType = envXmlParser.getSlotType(environment, application,server,slot)				
		Map envBindings = bindingsParser.getAtgBindings(new File("${project.AUTO_DEP_ATG_HOME}/config/atg-env-bindings.xml"),environment,slotType)
		def baseConfigSlot = baseConfigAdapterXMLParser.getSlotForApp(application)
		
		logger.info(Logging.QUIET, "\nApplying bindings for ${server}/${slot}")
		logger.info(Logging.QUIET, "*********************************************")

		def templatesFolder = new File("${project.AUTO_DEP_HOME}/${project.atgConfigLocation}/ATG-Data/${application}/${environment}/${baseConfigSlot}/localconfig")	
		def configFolder = "${project.atg_config_target}/${environment}/${server}/${slot}/localconfig"
	
		templatesFolder.eachFileRecurse
		{
			if (!it.isFile()) return			
			def relativePath = it.getPath().minus(templatesFolder.getPath())	
			def configFile=new File("${configFolder}${relativePath}")
			def text=configFile.getText()
			envBindings.each
			{
				text=text.replaceAll("#${it.key}#",envNavigator.applySlotContext(it.value,environment,application,server,slot,project))
			}
			configFile.write(text)					
			logger.info(Logging.QUIET, "\nApplied atg bindings to : ${configFile.getPath()}")
		}	
	}

	/**
	There are a number files that cannot yet be processed using the configuration files.
	They need to be processed programatically.
	*/
	private void applyExceptions(String environment,String application, String server, String slot, Project project)
	{
		def envXmlParser = new EnvXmlParser("${project.AUTO_DEP_ATG_HOME}/config/env/environment.${environment}.xml")
	    def slotType = envXmlParser.getSlotType(environment, application,server,slot)				
		
		logger.info(Logging.QUIET, "\nApplying exceptions for ${server}/${slot}")
		logger.info(Logging.QUIET, "*********************************************")
		def configFolder = "${project.atg_config_target}/${environment}/${server}/${slot}/localconfig"
		
		//Do custom configuration for /atg/dynamo/Initial.properties
		if (!slotType.startsWith('LM'))
		{
		    def initialFile=new File("${configFolder}/atg/dynamo/Initial.properties")
		    if(initialFile.exists())
		    {
				def text=initialFile.getText()
				text=text.replaceAll('/atg/dynamo/service/ServerLockManager,',"")
				text=text.replaceAll('/atg/dynamo/service/ServerLockManager',"")
				initialFile.write(text)
			}
		} 
		
		if(slotType.toUpperCase().equals('DEPLOYMENTSERVER'))
		{
		    def configFile=new File("${configFolder}/atg/epub/Configuration.properties")
		    if(configFile.exists())
		    {
				def text=configFile.getText()
				text = text + prepareAtgEpubConfiguration(environment,application,server,slot,project)
				configFile.write(text)			
			}
			
		    def serverNameFile=new File("${configFolder}/atg/dynamo/service/ServerName.properties")
		    if(serverNameFile.exists())
		    {
				def text=serverNameFile.getText()
				text = text + prepareDynamoServiceServerName(environment,application,server,slot,project)			
				serverNameFile.write(text)
			}
		}	
	}

		
	/**
	Add additional files to the generated ATG configuration.
	*/
	private void addFilesToSlot(String environment,String application,String server,String slot,Project project)
	{
		def atgCustomConfigManager = BeanManager.getBean("atgCustomConfigManager")
		def envXmlParser = new EnvXmlParser("${project.AUTO_DEP_ATG_HOME}/config/env/environment.${environment}.xml")

		logger.info(Logging.QUIET, "\nAdding custom atg  config files for ${server}/${slot}")
		logger.info(Logging.QUIET, "********************************************************") 
		
		def slotType = envXmlParser.getSlotType(environment, application,server,slot)

		def licenceTarget = "${project.atg_config_target}/${environment}/${server}/${slot}/localconfig"
		project.ant.delete{
			fileset(dir:licenceTarget)
			{
				include(name:"*License.properties")
			}			
		}
		atgCustomConfigManager.doCustomConfiguration(environment,application,server,slot,slotType,project)
	}

	/**
	A method to prepare values for atg\\epub\\Configuration.properties.
	*/
	private String prepareAtgEpubConfiguration(String environment,String application, String server, String slot, Project project)
	{
		def envXmlParser = new EnvXmlParser("${project.AUTO_DEP_ATG_HOME}/config/env/environment.${environment}.xml")
		def bindingsParser = BeanManager.getBean("bindingsParser")
		def serverNames = envXmlParser.getServerNames(environment, application)	

		def remoteHostsLabel="\nremoteHosts=\\"
		def remoteRMIPortsLabel="\nremoteRMIPorts=\\"
		def remotePortsLabel="\nremotePorts=\\"
		
		def remoteHosts = ""
		def remoteRMIPorts = ""
		def remotePorts = ""
		
		def delimiter="\n"
		
		serverNames.each
		{
			def thisServer=it
			def slots = envXmlParser.getSlotsForServer(environment, application, thisServer)

			slots.each
			{
				def thisSlot=it
			    def slotType = envXmlParser.getSlotType(environment, application,thisServer,thisSlot)
				def slotNumber=bindingsParser.getJbossBindings(new File("${project.AUTO_DEP_ATG_HOME}/config/jboss-bindings.xml"),application,environment,slotType,thisSlot).get("emtalent.jboss.slot.number")
			    if (thisServer.equals(server) && thisSlot.equals(slot)) return
			    if (!slotType.toUpperCase().equals('DEPLOYMENTSERVER') & !slotType.toUpperCase().equals('PS')) return
			    
			    remoteHosts = "${remoteHostsLabel}${remoteHosts}${delimiter}${thisServer}"
			    remoteRMIPorts = "${remoteRMIPortsLabel}${remoteRMIPorts}${delimiter}10${slotNumber}60"
			    remotePorts = "${remotePortsLabel}${remotePorts}${delimiter}10${slotNumber}15"
				
				remoteHostsLabel=remoteRMIPortsLabel=remotePortsLabel=""
				delimiter=",\\\n"			    
			}
		}
	return remoteHosts + "\n" +  remoteRMIPorts + "\n" + remotePorts
	}
	
	/**
	A method to prepare values for atg\\dynamo\\service\\ServerName.properties.
	*/
	private String prepareDynamoServiceServerName(String environment,String application, String server, String slot, Project project)
	{
		def bindingsParser = BeanManager.getBean("bindingsParser")
		def envXmlParser = new EnvXmlParser("${project.AUTO_DEP_ATG_HOME}/config/env/environment.${environment}.xml")
		def slotType = envXmlParser.getSlotType(environment, application,server,slot)				
		def slotNumber=bindingsParser.getJbossBindings(new File("${project.AUTO_DEP_ATG_HOME}/config/jboss-bindings.xml"),application,environment,slotType,slot).get("emtalent.jboss.slot.number")
		def serverName="\nserverName=${server}:10${slotNumber}50"
		def drpPort="\ndrpPort=10${slotNumber}50"
		return serverName + drpPort
	}

	/**
	There are a number of .xml and .wdl files that are part of ATG Configuration files. These files 
	contain environment specific values that should be replaced with the values appropriate to a given environment.
	To facilitate this, these files have been converted to templates and are stored in TFS as part of autodeploy code.
	
	Storing these atg config files as templates in TFS require us to ensure that any changes to the original files 
	by the development team are properly merged into the templates kept in TFS. The method below compares the templates and 
	the original files and alerts the user if the templates need updating.
	*/
	private void validateAtgXmlConfig(String environment,String application, String server, String slot, Project project)
	{
		AtgXmlConfigValidator validator = new AtgXmlConfigValidator()
		assert validator.validate(environment,application,server,slot,project), "The build has failed for the following reason. The .xml and .wdl files that are" +
		" part of ATG Config Templates in autodeploy require updating because the corresponding " +
		" files supplied with SIT Configuration have differences. " +
		" The exact name of the file(s) that requires update are printed in the logs above."
	}
	
	
	/**
	From the fileNode, find out the properies that may have to be created if they don't already exist in the file
	- the contents of which are in 'originalText'. Then search in the originalText for each such property. 
	If not found, add the property to returnText.
	*/
	private String createProperties(String originalText, GPathResult fileNode, String environment, String slotType)
	{
		def propertiesToCreate = fileNode.property.findAll{it.@create.text().toUpperCase().equals('TRUE')}
		def returnText=new StringBuffer(originalText)
		
		propertiesToCreate.each
		{
			def property=it
			boolean found=false
			
			/**
			Even if a property is marked as create='TRUE', it should only be created if there is a need for this property
			in 'this' environment and 'this' slotType. Therefore check for the existence of an <env> element for 'this' environment and
			this 'slotType'.
			*/
			def envValues = property.env.findAll { it.@name.text().equals(environment) & (it.@slotType.text().trim().equals('')||it.@slotType.text().trim().equals(slotType)) }
			if (envValues.size()==0) return

						
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
	
	/**
	There are some files that contains values that should change specific to the server on which those files are deployed. This method deals with such files.
	*/
	private void applyServerBindings(String environment,String application, String server, String slot, Project project)
	{
		logger.info(Logging.QUIET, "\nReplacing server specific values in config files for ${server}/${slot}")
		logger.info(Logging.QUIET, "******************************************************")

		def slotFolder = "${project.atg_config_target}/${environment}/${server}/${slot}/jboss-server"
		
		/**
		Substitute values from server-bindings.xml
		*/
		def xmlConfig = new XmlSlurper().parse(new File("${project.AUTO_DEP_ATG_HOME}/config/server-bindings.xml"))
		xmlConfig.file.each
		{
			File file=new File("${slotFolder}/${it.@name.text()}")
			if(file.exists())
			{
				logger.info(Logging.QUIET, "Apply server specific values to ${file.getPath()}")
				
				def fileText=file.getText()
				//def serverNode=it.server.find{it.@name.text().equals(server)}
				
				def slotNode=it.server.find{it.@name.text().equals(server)}.slot.findAll{it.@name.text().equals(slot) || it.@name.text().equals('*')}
				
				slotNode.each
				{	
					it.key.each
					{
						fileText=fileText.replaceAll("#${it.@name.text()}#",it.@value.text())
					}	
					file.write(fileText)
				}
				
			}
		}
	}		
}
