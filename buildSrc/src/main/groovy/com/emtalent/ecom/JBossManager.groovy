
package com.emtalent.atg

import org.gradle.api.*
import org.gradle.api.plugins.*
import com.emtalent.ecom.EnvXmlParser
import org.gradle.api.logging.Logging
import com.emtalent.common.BindingsParser
import com.emtalent.ecom.ApplicationsXmlParser
import com.emtalent.common.TemplateEngine
import com.emtalent.common.BeanManager
import groovy.util.slurpersupport.GPathResult
import groovy.util.slurpersupport.NodeChild

/*
This Plugin Extends emtalentPlugin which allows us to define emtalent specific 
functionality for all our plugins.

Such as access to the gradle logger -see below
*/

class JBossManager extends AtgBase	{

	private BindingsParser bindingsParser
	private TemplateEngine templateEngine
	private ApplicationsXmlParser applicationsXmlParser
	
	public BindingsParser getBindingsParser()
	{
		return bindingsParser
	}

	public void setBindingsParser(BindingsParser parser)
	{
		bindingsParser = parser
	}
	
	public ApplicationsXmlParser getApplicationsXmlParser()
	{
		return applicationsXmlParser
	}

	public void setApplicationsXmlParser(ApplicationsXmlParser parser)
	{
		applicationsXmlParser = parser
	}	
	
	public TemplateEngine getTemplateEngine()
	{
		return templateEngine
	}

	public void setTemplateEngine(TemplateEngine engine)
	{
		templateEngine = engine
	}
	
	
	/**
	Copy the default installation of JBoss to the release folders.
	*/
	void copyJbossToRelease(String environment, String application, String server, Project project) 
	{
		logger.info(Logging.QUIET, "\nCopying default JBoss to ${application}.${server}")

		def sourcePath = "${project.JBossInstallLocation}/jboss-as"
		def destinationPath = "${project.NASPath}/${project.ReleaseID}/${environment}/${application}/${server}/Jboss/jboss-as"

		project.ant.copy(todir: destinationPath) 
		{
			 fileset(dir: sourcePath)
		}
		logger.info(Logging.QUIET, "\nCopied files from: ${sourcePath} to: ${destinationPath}")		
	}
	

	/**
	Create slots as defined in the environments.xml. These slots are created under the 
	"environment" and "servergroup" passed in as parameters.
	*/
	void createSlot(String environment, String serverGroup, String server, String slot,Project project) 
	{
		logger.info(Logging.QUIET, "Creating slot ${serverGroup}.${server}.${slot}")
		
		def envXmlParser = new EnvXmlParser("${project.AUTO_DEP_ATG_HOME}/config/env/environment.${environment}.xml")
		def serverFolder = "${project.NASPath}/${project.ReleaseID}/${environment}/${serverGroup}/${server}/Jboss/jboss-as/server"
		def slotFolder = "${serverFolder}/${slot}"

		project.ant.mkdir(dir:slotFolder)
		project.ant.copy(todir:slotFolder) 
		{
		    def jbossInstanceTemplate = envXmlParser.getJbossInstanceTemplate(serverGroup,server,slot)
		    def fromDir="${serverFolder}/${jbossInstanceTemplate}"
			fileset(dir:fromDir)
			logger.info(Logging.QUIET, "\nCopied files from: ${fromDir} to: ${slotFolder}")			
		}
		

	}
	
	/**
	There are some jboss files that should be copied to the jboss server above the slot folders
	This method reads a list of such files from jboss-custom-config.xml file and copy them 
	across to the correct locations.
	**/
	public void copyJBossConfigToServer(String environment,String serverGroup,String serverName,Project project)
	{
			def releaseFolder = "${project.NASPath}/${project.ReleaseID}/"
			def jbossCustomConfig = new XmlSlurper().parse(new File("${project.AUTO_DEP_ATG_HOME}/config/jboss-custom-config.xml"))

			def filesToCopy = jbossCustomConfig.copyToServer.item.findAll{
			(it.@application.text().equals('*') | it.@application.text().contains("[${serverGroup}]")) &
			(it.@env.text().equals('*') | it.@env.text().equals(environment))
			}
			
			filesToCopy.each
			{
				def item=it
				def sourceFile = new File("${project.AUTO_DEP_ATG_HOME}/templates/${project.jboss_templates_version}/${item.source.text()}")
				def destinationFile=new File("${releaseFolder}${environment}/${serverGroup}/${serverName}/${item.destination.text()}")
				assert sourceFile.exists(), "An expected file {sourceFile} does not exist."
				project.ant.copy(file:sourceFile,toFile:destinationFile,overwrite:true)
				logger.info(Logging.QUIET, "Copied file ${sourceFile} to ${destinationFile}")
			}
	}


	/** 
	Some files need to be deleted from the default jboss server. This methods reads a list of such files from
	jboss-custom-config.xml file and deletes them from the appropriate locations.
	*/
	public void deleteFromSlot(String environment,String serverGroup,String serverName,String slot,Project project)
	{
		def releaseFolder = "${project.NASPath}/${project.ReleaseID}/"
		def jbossCustomConfig = new XmlSlurper().parse(new File("${project.AUTO_DEP_ATG_HOME}/config/jboss-custom-config.xml"))

		def itemsToDelete = jbossCustomConfig.deleteFromSlot.item.findAll{
			(it.@application.text().equals('*') | it.@application.text().contains("[${serverGroup}]")) &
			(it.@env.text().equals('*') | it.@env.text().equals(environment))
			}

		itemsToDelete.each
		{
			def item=it
			def destinationFile=new File("${releaseFolder}${environment}/${serverGroup}/${serverName}/Jboss/jboss-as/server/${slot}/${item.text()}")
			if(item.@type.text().equals('file'))
			{
				project.ant.delete(file:destinationFile)
			}
			else if (item.@type.text().equals('folder'))
			{
				project.ant.delete(dir:destinationFile)			
			}
			logger.info(Logging.QUIET, "Deleted " + destinationFile)
		}
	} 
	
	/**
	Copy ATG Configuration files to ATG Data Folder under the correct slots.
	**/
	public void copyAtgConfig(String environment,String serverGroup,String serverName,String slot,Project project)
	{
		def releaseFolder = "${project.NASPath}/${project.ReleaseID}/"
		def sourceFolder = "${project.atg_config_target}/${environment}/${serverName}/${slot}/localconfig"
		def destinationFolder = "${releaseFolder}/${environment}/${serverGroup}/${serverName}/ATG-Data/servers/${slot}/localconfig"

		assert new File(sourceFolder).exists(), 'An expected folder' + sourceFolder + ' does not exist'
		project.ant.copy(toDir:destinationFolder, overwrite:true)
		{
			fileset(dir:sourceFolder)
		}
		logger.info(Logging.QUIET, "Copied folder " + sourceFolder)
		logger.info(Logging.QUIET, "to " + destinationFolder)
	}
	
	/**
	Copy ATG Global Configuration files to ATG Data folder under the correct servers.
	**/
	public void copyAtgGlobalConfig(String environment,String serverGroup,String serverName,Project project)
	{
		def releaseFolder = "${project.NASPath}/${project.ReleaseID}/"
		def sourceFolder = "${project.atg_config_target}/${environment}/${serverName}/localconfig"
		def destinationFolder = "${releaseFolder}/${environment}/${serverGroup}/${serverName}/ATG-Data/localconfig"

		assert new File(sourceFolder).exists(), 'An expected folder' + sourceFolder + ' does not exist'
		project.ant.copy(toDir:destinationFolder, overwrite:true)
		{
			fileset(dir:sourceFolder)
		}
		logger.info(Logging.QUIET, "Copied folder " + sourceFolder)
		logger.info(Logging.QUIET, "to " + destinationFolder)
	}
		
	/**
	Copy jboss configurations files (such as jdbc drivers and login-conf.xml) to each slot.
	*/
	public void copyJBossConfigToSlot(String environment,String serverGroup,String serverName,String slot,Project project)
	{
		def releaseFolder = "${project.NASPath}/${project.ReleaseID}/"
		def sourceFolder = "${project.atg_config_target}/${environment}/${serverName}/${slot}/jboss-server"
		def destinationFolder = "${releaseFolder}/${environment}/${serverGroup}/${serverName}/Jboss/jboss-as/server/${slot}"
		assert new File(sourceFolder).exists(), 'An expected folder' + sourceFolder + ' does not exist'
		project.ant.copy(toDir:destinationFolder, overwrite:true)
		{
			fileset(dir:sourceFolder)
		}
		logger.info(Logging.QUIET, "Copied folder " + sourceFolder)
		logger.info(Logging.QUIET, "to " + destinationFolder)
	}
	
	/**
	Apply jboss bindings to jboss templates and copy them across to the correct locations.
	*/
	public void configureJBoss(String environment,String serverGroup,String serverName,String slot,Project project)				
	{
		def releaseFolder = "${project.NASPath}/${project.ReleaseID}/"
		def envXmlParser = new EnvXmlParser("${project.AUTO_DEP_ATG_HOME}/config/env/environment.${environment}.xml")
		def slotType = envXmlParser.getSlotType(environment, serverGroup,serverName,slot)
		Map bindings=bindingsParser.getJbossBindings(new File(project.AUTO_DEP_ATG_HOME + "/config/jboss-bindings.xml"),serverGroup,environment,slotType,slot)
		
		bindings.put("emtalent.jboss.webcluster.sar.jbossservice.buddyreplication.numBuddies","${envXmlParser.getBuddyPoolSize(serverGroup).intValue()-1}")
		bindings.put("emtalent.jboss.webcluster.sar.jbossservice.buddyreplication.buddyPoolName",envXmlParser.getJbossDomainNameForSlot(environment,serverGroup,serverName,slot))
		bindings.put("emtalent.jboss.webdeployer.server.engine.jvmroute", 'jvmRoute=\"' + serverName.minus(project.serverNamePartForRemoval) + '_' + slot + '\"')								
		
	    def jbossTemplateForApp = envXmlParser.getJbossInstanceTemplate(serverGroup,serverName,slot)
		def jbossCustomConfig = new XmlSlurper().parse(new File("${project.AUTO_DEP_ATG_HOME}/config/jboss-custom-config.xml"))
		
		def templatesToCopy = jbossCustomConfig.copyToSlot.item.findAll{
			(it.@application.text().equals('*') | it.@application.text().contains("[${serverGroup}]")) &
			(it.@env.text().equals('*') | it.@env.text().equals(environment)) &
			(it.@jbossTemplateForApp.text().equals('*') | it.@jbossTemplateForApp.text().equals(jbossTemplateForApp))			
			}
		copyJBossTemplates(templatesToCopy,bindings,environment,serverGroup,serverName,slot,project)
	}
	
	/**
	Apply bindings to the templates available in the list  'templatesToCopy' and copy them to the correct 
	locations under the jboss folder.
	**/
	private void copyJBossTemplates(templatesToCopy,bindings,environment,serverGroup,serverName,slot,project)
	{
		templatesToCopy.each()
		{
			def item=it
			def sourceFile = new File("${project.AUTO_DEP_ATG_HOME}/templates/${project.jboss_templates_version}/${item.source.text()}")
			assert sourceFile.exists(), "An expected file ${sourceFile} does not exist."
			def destinationFile=new File("${project.NASPath}/${project.ReleaseID}/${environment}/${serverGroup}/${serverName}/Jboss/jboss-as/server/${slot}/${item.destination.text()}")
			templateEngine.makeTemplate(bindings,sourceFile,destinationFile)
			logger.info(Logging.QUIET, "Applied bindings to ${destinationFile} using template ${sourceFile}")
		}
	}
		
	/**
	Copy build inputs to the server folder.
	*/
	public void copyGoodsInToServer(String environment,String serverGroup,String serverName, Project project)
	{
		def releaseFolder = "${project.NASPath}/${project.ReleaseID}/"
		def applicationsXml = new XmlSlurper().parse(new File("${project.AUTO_DEP_ATG_HOME}/config/applications.xml"))
		def applicationXml = applicationsXml.application.find{it.@name.text().equals(serverGroup)}
		def sourcePath="${project.ATG_APPS_BUILD_HOME}"
		def destinationPath="${releaseFolder}/${environment}/${serverGroup}/${serverName}"
		def items=applicationXml.buildinput.item.findAll{it.destination.@receiver.text().equals("server")}

		copyGoodsIn(items,sourcePath,destinationPath,project)		
	}
	
	/**
	Copy build inputs to the slot folder.
	*/
	public void copyGoodsInToSlot(String environment,String serverGroup,String serverName,String slot,Project project)
	{
		def releaseFolder = "${project.NASPath}/${project.ReleaseID}/"
		def applicationsXml = new XmlSlurper().parse(new File("${project.AUTO_DEP_ATG_HOME}/config/applications.xml"))
		def applicationXml = applicationsXml.application.find{it.@name.text().equals(serverGroup)}
		def sourcePath="${project.ATG_APPS_BUILD_HOME}"
		def destinationPath="${releaseFolder}${environment}/${serverGroup}/${serverName}/Jboss/jboss-as/server/${slot}"
		def items=applicationXml.buildinput.item.findAll{it.destination.@receiver.text().equals("slot")}
		
		copyGoodsIn(items,sourcePath,destinationPath,project)		
	}
		
	/**
	Copy build input artefacts to the release folders.
	*/
	public void copyGoodsIn(GPathResult items, String sourcePath, String destinationPath, Project project)
	{	
		items.each()
		{
			def item=it
		
			if(item.source.@type.text().equals("file"))
			{
				if(item.destination.@type.text().equals("file"))
				{
					project.ant.copy(file:"${sourcePath}/${item.source.text()}",toFile:"${destinationPath}/${item.destination.text()}",overwrite:true)
				}
				else if (item.destination.@type.text().equals("folder"))
				{
					project.ant.copy(file:"${sourcePath}/${item.source.text()}",toDir:"${destinationPath}/${item.destination.text()}",overwrite:true)
				}		
			}
			else if (item.source.@type.text().equals("folder"))
			{
				project.ant.copy(toDir:"${destinationPath}/${item.destination.text()}",overwrite:true)
				{
					fileset(dir:"${sourcePath}/${item.source.text()}")
				}
			}
			logger.info(Logging.QUIET, "Copied ${sourcePath}/${item.source.text()} to ${destinationPath}/${item.destination.text()}")					
		}				
	}
		
	//Add server side scripts for extracting temp archives.
	public void addScriptsToExtractArchives(String environment,String serverGroup,String serverName,String slot,Project project)
	{
		def releaseFolder = "${project.NASPath}/${project.ReleaseID}/"	
		def applicationsXml = new XmlSlurper().parse(new File("${project.AUTO_DEP_ATG_HOME}/config/applications.xml"))
		def applicationXml = applicationsXml.application.find{it.@name.text().equals(serverGroup)}
		def batchFileFolder="${releaseFolder}${environment}/${serverGroup}/${serverName}/scripts/batch"
		def batchFile="${batchFileFolder}/deliverCompressedEar.bat"

		applicationXml.buildinput.item.each()
		{
			def item=it
			def destinationPath

			if (item.@extractAtTarget.text().equals("true"))
			{
				def targetPathArray = item.source.text().split('/')
				def targetFilename = targetPathArray[targetPathArray.length-1]
				def targetPath = item.destination.text()

				// Generate a postDeploy batch script based on applications.xml
				def bindings=[:]
				bindings.put("compressed.ear.file.name",targetFilename)
				bindings.put("compressed.ear.file.path",targetPath)
				bindings.put("emtalent.ecom.jboss.java_home","${project.java_home}".replace('/','\\\\'))				
				project.ant.mkdir(dir:batchFileFolder)
				templateEngine.makeTemplate(bindings,new File(project.AUTO_DEP_ATG_HOME + "/templates/batch/deliverCompressedEar.bat.template"),new File(batchFile))							
			}	
		}				
	}
}
