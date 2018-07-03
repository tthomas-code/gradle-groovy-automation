
package com.emtalent.atg

import org.gradle.api.*
import org.gradle.api.plugins.*
import com.emtalent.ecom.EnvXmlParser
import org.gradle.api.logging.Logging
import com.emtalent.common.BindingsParser
import com.emtalent.common.TemplateEngine
import com.emtalent.common.BeanManager

class BatchManager extends AtgBase	{

	private BindingsParser bindingsParser
	private TemplateEngine templateEngine
	
	public BindingsParser getBindingsParser()
	{
		return bindingsParser
	}

	public void setBindingsParser(BindingsParser parser)
	{
		bindingsParser = parser
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
	Add batch scripts to release folder.
	*/
	void addBatchScriptsToRelease(String environment, String serverGroup, String server, Project project)
	{	
		def releaseFolder = "${project.NASPath}/${project.ReleaseID}/"

		logger.info(Logging.QUIET, "\nAdding Scripts Folder for ${server}")
		logger.info(Logging.QUIET, "*************************************")
					
	    def target = "${releaseFolder}/${environment}/${serverGroup}/${server}/scripts"
		def source = "${project.AUTO_DEP_ATG_HOME}/scripts"
		project.ant.copy(todir: target) {
			fileset(dir: source)
		}
		
		Map bindings=[:]
		bindings.put("emtalent.firedaemon.service.java_home","${project.java_home}".replace('/','\\\\'))
		bindings.put("emtalent.firedaemon.home",project.firedaemon_home)
		
		def batchScripts="postDeploy.bat,preDeploy.bat,jbossBasePredeploy.bat"
		batchScripts.tokenize(',').each
		{
			def sourceFile = new File("${project.AUTO_DEP_ATG_HOME}/templates/batch/${it}.template")
			def destinationFile=new File("${releaseFolder}${environment}/${serverGroup}/${server}/scripts/batch/${it}")
			templateEngine.makeTemplate(bindings,sourceFile,destinationFile)
			logger.info(Logging.QUIET, "Applied bindings to ${destinationFile} using template ${sourceFile}")		
		}
	}	

	/**
	Add batch scripts to release folder.
	*/
	void addBatchScriptsToJonRelease(String environment, String serverGroup, String server, Project project)
	{	
		def releaseFolder = "${project.NASPath}/${project.ReleaseID}/"

		logger.info(Logging.QUIET, "\nAdding Scripts jon Folder for ${server}")
		logger.info(Logging.QUIET, "*************************************")
					
	    def target = "${releaseFolder}/${environment}/rhq-agent/${serverGroup}/${server}/scripts"
		project.ant.mkdir(dir:target)
		
		Map bindings=[:]
		bindings.put("emtalent.firedaemon.home",project.firedaemon_home)
		
		def sourceFile = new File("${project.AUTO_DEP_ATG_HOME}/templates/batch/jonPostDeploy.bat.template")
		def destinationFile=new File("${target}/jonPostDeploy.bat")
		templateEngine.makeTemplate(bindings,sourceFile,destinationFile)
		logger.info(Logging.QUIET, "Applied bindings to ${destinationFile} using template ${sourceFile}")		

		sourceFile = new File("${project.AUTO_DEP_ATG_HOME}/templates/batch/jonPreDeploy.bat.template")
		destinationFile=new File("${target}/jonPreDeploy.bat")
		templateEngine.makeTemplate(bindings,sourceFile,destinationFile)
		logger.info(Logging.QUIET, "Applied bindings to ${destinationFile} using template ${sourceFile}")		
						
	}
	
	/**
	Add PruneLog scripts to the release folder.
	*/
	void addPruneLogsScript(String environment, String serverGroup, String server, Project project) 
	{
		def envXmlParser = new EnvXmlParser("${project.AUTO_DEP_ATG_HOME}/config/env/environment.${environment}.xml")

		// This is the release folder under which slots are created.	
		def releaseFolder = "${project.NASPath}/${project.ReleaseID}/"
		
		logger.info(Logging.QUIET, "\nAdding Prune logs script for " + server)
		logger.info(Logging.QUIET, "*********************************************************")
		
		def commands = ""
		def slots = envXmlParser.getSlotsForServer(environment, serverGroup, server)

		slots.each
		{
			def slot=it		
			def daysToRetain = envXmlParser.getlogFileRetentionDaysForSlot(environment, serverGroup, server, slot)
			commands = commands + "cscript /nologo D:/Deployments/scripts/pruneLogs.vbs  D:/Deployments/Jboss/jboss-as/server/${slot}/log ${daysToRetain} >> D:/Deployments/pruneLogs.log\n"
		}
			
		Map bindings=[:]
		bindings.put("prunelogs.cmd",commands)
		
		def sourceFile = new File("${project.AUTO_DEP_ATG_HOME}/templates/batch/pruneLogs.bat.tempate")
		def destinationFile=new File("${releaseFolder}${environment}/${serverGroup}/${server}/scripts/batch/pruneLogs.bat")
		templateEngine.makeTemplate(bindings,sourceFile,destinationFile)
		logger.info(Logging.QUIET, "Applied bindings to ${destinationFile} using template ${sourceFile}")	
	}
}
