
package com.emtalent.atg

import org.gradle.api.*
import org.gradle.api.plugins.*
import com.emtalent.ecom.EnvXmlParser
import org.gradle.api.logging.Logging
import com.emtalent.common.BindingsParser
import com.emtalent.common.TemplateEngine
import com.emtalent.common.BeanManager

class JONManager extends AtgBase	{

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
	Copy JON to release folder.
	*/
	void copyJONToRelease(String environment, String application, String server, Project project)
	{
		def rhqFolder = "${project.NASPath}/${project.ReleaseID}/${environment}/rhq-agent/${application}/${server}"
		def sourceFolder = "${project.AUTO_DEP_HOME}/${project.ecom.jonSource}"
		
		project.ant.copy(todir: rhqFolder) 
		{
			 fileset(dir: sourceFolder)
		}		
	}

	/**
	Apply environment specific changes to JON Agent by copying environment specific files to
	the relevant release folders
	*/
	void configureJON(String environment, String serverGroup, String server,Project project) 
	{
		// This is the release folder under which slots are created.	
		def releaseFolder = "${project.NASPath}/${project.ReleaseID}"
		
		logger.info(Logging.QUIET, "\nConfiguring JON Agent for " + server)
		logger.info(Logging.QUIET, "*********************************************************")
					
		def jonCustomConfig = new XmlSlurper().parse(new File("${project.AUTO_DEP_ATG_HOME}/config/jon-custom-config.xml"))
		def environmentNode = jonCustomConfig.copy.environment.find{ it.@name.text().equals(environment) }
		
		environmentNode.item.each
		{
			def item=it
			def sourceFile = new File("${project.AUTO_DEP_ATG_HOME}/templates/jon/${item.source.text()}")
			def destinationFile = new File("${releaseFolder}/${environment}/rhq-agent/${serverGroup}/${server}/${item.destination.text()}")
			assert sourceFile.exists(), "An expected file {sourceFile} does not exist."
			project.ant.copy(file:sourceFile,toFile:destinationFile,overwrite:true)
			logger.info(Logging.QUIET, "Copied file ${sourceFile} to ${destinationFile}")
		}					
	}
}