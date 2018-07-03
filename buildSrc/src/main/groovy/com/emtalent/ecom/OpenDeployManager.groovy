/**
OpenDeployMananger.groovy  11/Feb/2010
This plugin provides methods for deploying builds to servers, servergroups or environments.
*/

package com.emtalent.atg

import org.gradle.api.*
import org.gradle.api.plugins.*
import java.lang.Runtime;
import java.lang.Process;
import org.gradle.api.logging.Logging
import com.emtalent.common.TemplateEngine

class OpenDeployManager extends AtgBase	
{
	private TemplateEngine templateEngine

	public TemplateEngine getTemplateEngine()
	{
		return templateEngine
	}

	public void setTemplateEngine(TemplateEngine engine)
	{
		templateEngine = engine
	}	

	//========================================================================
	// Start the open deploy task using the ODServer and ODRelease parameters 
	// passed in on the command line.
	//========================================================================
	void deploy(Project project) 
	{
		// Check if the necessary parameters are available.
		Boolean success=executeIwodcmdStart("AUTO-DEP/${project.ODEnv}.atg_deploy_atgdata.xml",project)
		assert success, "Opendeploy command failed."
				
		success=executeIwodcmdStart("AUTO-DEP/${project.ODEnv}.atg_deploy_adapters.xml",project)
		assert success, "Opendeploy command failed." 
		
		success=executeIwodcmdStart("AUTO-DEP/${project.ODEnv}.atg_deploy_firedaemon.xml",project)
		assert success, "Opendeploy command failed." 
		
		success=executeIwodcmdStart("AUTO-DEP/${project.ODEnv}.atg_deploy_batch.xml",project)
		assert success, "Opendeploy command failed." 

		success = executeIwodcmdStart("AUTO-DEP/${project.ODEnv}.atg_deploy_jboss.xml",project)
		assert success, "Opendeploy command failed."
	}	
	
	//========================================================================
	// Start the open deploy 'deployJboss' task using the ODServer and ODRelease parameters 
	// passed in on the command line.
	//========================================================================
	void deployJbossBase(Project project) 
	{
		//Deploy stop scripts and stop all the instances running on the servers.
		Boolean success=executeIwodcmdStart("AUTO-DEP/${project.ODEnv}.atg_deploy_jbossbase_batch.xml",project)
		assert success, "Opendeploy command failed."
		
		//Deploy jboss base to the servers.
		success=executeIwodcmdStart("AUTO-DEP/${project.ODEnv}.atg_deploy_jbossbase.xml",project)
		assert success, "Opendeploy command failed."
	}
	
	//========================================================================
	// Start the open deploy 'deployJon' task using the ODServer and ODRelease parameters 
 	// passed in on the command line.
	//========================================================================
	void deployJon(Project project) 
	{
		// Check if the necessary parameters are available.
		Boolean success=executeIwodcmdStart("AUTO-DEP/${project.ODEnv}.jon_deploy_batch.xml",project)
		assert success, "Opendeploy command failed."
		
		success = executeIwodcmdStart("AUTO-DEP/${project.ODEnv}.jon_deploy.xml",project)
		assert success, "Opendeploy command failed."
	}
		
	//========================================================================
	// Start the open deploy 'deployApps' task using the ODServer and ODRelease parameters 
	// passed in on the command line.
	//========================================================================
	void deployApps(Project project) 
	{
		Boolean success=executeIwodcmdStart("AUTO-DEP/${project.ODEnv}.atg_deploy_firedaemon.xml",project)
		assert success, "Opendeploy command failed."
				
		success=executeIwodcmdStart("AUTO-DEP/${project.ODEnv}.atg_deploy_batch.xml",project)
		assert success, "Opendeploy command failed." 
		
		success=executeIwodcmdStart("AUTO-DEP/${project.ODEnv}.atg_deploy_atgdata.xml",project)
		assert success, "Opendeploy command failed."

		success=executeIwodcmdStart("AUTO-DEP/${project.ODEnv}.atg_deploy_atgdata_global_config.xml",project)
		assert success, "Opendeploy command failed."		

		success=executeIwodcmdStart("AUTO-DEP/${project.ODEnv}.atg_deploy_temparchive.xml",project)
		assert success, "Opendeploy command failed."
				
		success=executeIwodcmdStart("AUTO-DEP/${project.ODEnv}.atg_deploy_apps.xml",project)
		assert success, "Opendeploy command failed."
		
		success=executeIwodcmdStart("AUTO-DEP/${project.ODEnv}.atg_deploy_adapters.xml",project)
		assert success, "Opendeploy command failed." 		
		
	}	
		
	//========================================================================
	// Deploy previously prepared ATG Configuration to a number of clusters.
	//========================================================================
	void deployConfig(Project project) 
	{
		Boolean success=executeIwodcmdStart("AUTO-DEP/${project.ODEnv}.atg_deploy_firedaemon.xml",project)
		assert success, "Opendeploy command failed."
				
		success=executeIwodcmdStart("AUTO-DEP/${project.ODEnv}.atg_deploy_batch.xml",project)
		assert success, "Opendeploy command failed." 
		
		success=executeIwodcmdStart("AUTO-DEP/${project.ODEnv}.atg_deploy_atgdata.xml",project)
		assert success, "Opendeploy command failed."

		success=executeIwodcmdStart("AUTO-DEP/${project.ODEnv}.atg_deploy_atgdata_global_config.xml",project)
		assert success, "Opendeploy command failed."		
		
		success=executeIwodcmdStart("AUTO-DEP/${project.ODEnv}.atg_deploy_adapters.xml",project)
		assert success, "Opendeploy command failed." 		
		
	}	
	
	
	/**
	Private method to execute an opendeploy command.
	*/
	private Boolean executeIwodcmdStart(String odXmlFile, Project project)
	{
		def command_command="iwodcmd start"
		def command_param_xml=odXmlFile

		def command=command_command + ' ' +command_param_xml
	
		//Check if the opendeploy configuration file exist.
		assert new File(project.open_deploy_home + "/conf/" + command_param_xml).exists(), 'Expected file ' +  command_param_xml + ' does not exisit.'
		
		logger.info(Logging.QUIET, "Executing command " + command)

		def process=command.execute()
		
		process.waitFor()
		
		//Capture OpenDeploy output and write to command window
		println "stderr: ${process.err.text}"
		println "stdout: ${process.in.text}" 
				
		//Check return code. 
		assert process.exitValue()==0, 'The opendeploy task exited with errors.'
		
		return true
	}
	
	
	
	/**
	A method to configure the OpenDeploy XML definition file.
	*/
	void configureODXml(File templateFile, File outputFile, Map serversMap, Project project)
	{
		//Parse Template
		Node xmlConfig=new XmlParser().parse(templateFile);
		
		//Read the template elements
		Node replicationFarmTemplate=xmlConfig.replicationFarmSet.replicationFarm[0]
		Node definitionTemplate=xmlConfig.definition[0]
		Node execDeploymentTemplate=xmlConfig.deployment.execDeploymentTask[0]

		serversMap.each
		{
			
			def server = it.key
			def serverGroup = it.value
			
			// Prepare Bindings for each server.
			def path="${project.NASPath}/${project.ODRelease}/${project.ODEnv}/${serverGroup}/${server}"
			def target="${project.open_deploy_target}"
			
			Map bindings=["server":server,"path":path,"target":target]
		
			//Add a nodeRef element for the server.
			Node newReplicationFarm=templateEngine.makeTemplate(bindings,replicationFarmTemplate)
			xmlConfig.replicationFarmSet[0].append(newReplicationFarm)
			
			logger.info(Logging.QUIET, "Parsing " + outputFile.getName())

			if (outputFile.getName().contains("atg_deploy_atgdata.xml") | outputFile.getName().contains("atg_deploy_apps.xml")) {
			    
				def releasedEnvXmlParser = new com.emtalent.ecom.EnvXmlParser("${project.NASPath}/${project.ODRelease}/buildInfo/environment.${project.ODEnv}.xml")
				releasedEnvXmlParser.getSlotsForServer(project.ODEnv,serverGroup,server).each
				{
					bindings.put("slot",it)
					
					//Add a definition element for the server.
					Node definition=templateEngine.makeTemplate(bindings,definitionTemplate)
					xmlConfig.append(definition)
					
					//Add a execDeploymentTask element for the server.
					Node execDeployment=templateEngine.makeTemplate(bindings,execDeploymentTemplate)
					xmlConfig.deployment[0].append(execDeployment)
				}
			} else {
				//Add a definition element for the server.
				Node definition=templateEngine.makeTemplate(bindings,definitionTemplate)
				xmlConfig.append(definition)
			
				Node execDeployment=templateEngine.makeTemplate(bindings,execDeploymentTemplate)
				xmlConfig.deployment[0].append(execDeployment)			
			}
		}
		
		//Remove templates from the output xml.
		xmlConfig.replicationFarmSet[0].remove(replicationFarmTemplate)		
		xmlConfig.remove(definitionTemplate)
		xmlConfig.deployment[0].remove(execDeploymentTemplate)
		
		//Write resulting file.
		project.ant.mkdir(dir:outputFile.getParent())
		new XmlNodePrinter(new PrintWriter(outputFile)).print(xmlConfig)
	}

	/**
	A method to configure the OpenDeploy XML definition file.
	*/
	void configureODXmlForJon(File templateFile, File outputFile, Map serversMap, Project project)
	{
		//Parse Template
		Node xmlConfig=new XmlParser().parse(templateFile);
		
		//Read the template elements
		Node replicationFarmTemplate=xmlConfig.replicationFarmSet.replicationFarm[0]
		Node definitionTemplate=xmlConfig.definition[0]
		Node execDeploymentTemplate=xmlConfig.deployment.execDeploymentTask[0]

		serversMap.each
		{
			
			def server = it.key
			def serverGroup = it.value
			
			// Prepare Bindings for each server.
			def path="${project.NASPath}/${project.ODRelease}/${project.ODEnv}/rhq-agent/${serverGroup}/${server}"
			def target="${project.open_deploy_target}"
			
			Map bindings=["server":server,"path":path,"target":target]
		
			//Add a nodeRef element for the server.
			Node newReplicationFarm=templateEngine.makeTemplate(bindings,replicationFarmTemplate)
			xmlConfig.replicationFarmSet[0].append(newReplicationFarm)
			
			logger.info(Logging.QUIET, "Parsing " + outputFile.getName())

			//Add a definition element for the server.
			Node definition=templateEngine.makeTemplate(bindings,definitionTemplate)
			xmlConfig.append(definition)
			
			Node execDeployment=templateEngine.makeTemplate(bindings,execDeploymentTemplate)
			xmlConfig.deployment[0].append(execDeployment)			
		}
		
		//Remove templates from the output xml.
		xmlConfig.replicationFarmSet[0].remove(replicationFarmTemplate)		
		xmlConfig.remove(definitionTemplate)
		xmlConfig.deployment[0].remove(execDeploymentTemplate)
		
		//Write resulting file.
		project.ant.mkdir(dir:outputFile.getParent())
		new XmlNodePrinter(new PrintWriter(outputFile)).print(xmlConfig)
	}


}

