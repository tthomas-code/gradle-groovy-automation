/**
FireDaemonManager.groovy  18/Feb/2010

This groovy class provides methods required for creation of the following FireDaemon XML Service definitions

1) FireDaemon Service Definitions for each JBoss instance on every target server.
2) FireDaemon Service Definition for a JON Instance on each target server.
3) FireDaemon Service Definition for a search service a Knowledge server.
*/
package com.emtalent.atg

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.logging.Logging
import com.emtalent.common.BindingsParser
import com.emtalent.common.TemplateEngine
import com.emtalent.common.BeanManager
import com.emtalent.ecom.EnvXmlParser

class FireDaemonManager extends AtgBase	{

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
	Add a fireDaemon service definition to the releaseFolder
	so that a JBoss service can be started on the given server.
	*/
	public void addJBossServiceDefinition(String environment, String serverGroup, String serverName, String slot, Project project)
	{
			logger.info(Logging.QUIET, "\nConfigure FireDaemon for " + serverName + '/' + slot)
			logger.info(Logging.QUIET, "********************************")
			
			def releaseFolder = "${project.NASPath}/${project.ReleaseID}/"
			def fireDaemonFolder = releaseFolder + environment + '/' + serverGroup + '/' + serverName + '/FireDaemon/'
			if(!new File(fireDaemonFolder).exists())
			{
				project.ant.mkdir(dir:fireDaemonFolder)				
			}
			
			def envXmlParser = new EnvXmlParser("${project.AUTO_DEP_ATG_HOME}/config/env/environment.${environment}.xml")
			Map bindings = bindingsParser.getBindings(new File(project.AUTO_DEP_ATG_HOME + "/config/firedaemonconfig.xml"),slot)
			
			bindings.put("emtalent.firedaemon.service.name", 'ATG_' + slot)
			bindings.put("emtalent.firedaemon.service.display.name", 'ATG_' + slot)
			bindings.put("emtalent.firedaemon.service.description", 'JBoss ATG ' + slot + ' instance')
			bindings.put("emtalent.firedaemon.jboss.server.name", slot)
			bindings.put("emtalent.firedaemon.ecom.server.name", slot)
			bindings.put("emtalent.firedaemon.service.java_home", project.java_home)
			
			// Get the JBoss patition name for the Server Group
			def partition = envXmlParser.getPartitionName(environment,serverGroup,serverName,slot)
			bindings.put("emtalent.firedaemon.jboss.partition.name", partition)
			
			def java_opts = envXmlParser.getJavaOptsForSlot(environment,serverGroup,serverName,slot)
			bindings.put("emtalent.firedaemon.java.opts", java_opts)
			
			//Retrieve service user account and password
			def xmlConfig = new XmlSlurper().parse(new File(project.AUTO_DEP_ATG_HOME + "/config/firedaemon-service-accounts.xml"))
			def envAppGPath = xmlConfig.env.find{ it.@name.text().equals(environment) &&  it.@application.text().equals(serverGroup)}
			
			if(envAppGPath.size()==0)
			{
				envAppGPath = xmlConfig.env.find{ it.@name.text().equals(environment) &&  it.@application.text().equals('*')}
			}
						
			bindings.put("emtalent.firedaemon.service.useraccount.name", envAppGPath.@user.text())
			bindings.put("emtalent.firedaemon.service.useraccount.password", envAppGPath.@password.text())
			
			//Get the slot number the given slot
			def slotType = envXmlParser.getSlotType(environment,serverGroup,serverName,slot)				
			
			Map jbossSlotBindings=bindingsParser.getJbossBindings(new File('platforms/atg/config/jboss-bindings.xml'),serverGroup,environment,slotType,slot)
			def slotNumber=jbossSlotBindings.get("emtalent.jboss.slot.number")
			
			//Calculate prelaunch delay for the slot.
			def preLaunchDelay = Math.max(0,((Integer.parseInt(slotNumber)-1) * Integer.parseInt(project.jbossStartupInterval) * 1000))
			bindings.put("emtalent.firedaemon.service.prelaunchdelay", "${preLaunchDelay}")
			
			def fireDaemonServiceFile = fireDaemonFolder + 'fireDaemon_' + slot + '.xml'
			
			templateEngine.makeTemplate(bindings,new File(project.AUTO_DEP_ATG_HOME + "/templates/firedaemon/fireDaemonService.xml.template"),new File(fireDaemonServiceFile))		
			logger.info(Logging.QUIET, "Added fireDaemon service " + fireDaemonServiceFile)
	}

	
	/**
	Add a fireDaemon service definition to the releaseFolder
	so that a JON service can be started on the given server.
	*/
	private void addJONServiceDefinition(String environment,String serverGroup,String serverName,Project project)
	{
			def fireDaemonFolder = "${project.NASPath}/${project.ReleaseID}/${environment}/rhq-agent/${serverGroup}/${serverName}/FireDaemon"

			if(!new File(fireDaemonFolder).exists())
			{
				project.ant.mkdir(dir:fireDaemonFolder)				
			}
						
			def bindings=[:]

			//Retrieve service user account and password
			def xmlConfig = new XmlSlurper().parse(new File(project.AUTO_DEP_ATG_HOME + "/config/firedaemon-service-accounts.xml"))
			def envGPath = xmlConfig.env.find{ it.@name.text().equals(environment) }
			bindings.put("emtalent.firedaemon.service.useraccount.name", envGPath.@user.text())
			bindings.put("emtalent.firedaemon.service.useraccount.password", envGPath.@password.text())
						
			bindings.put("emtalent.firedaemon.service.jon_home", project.jon_home)
			bindings.put("emtalent.firedaemon.service.jon.log", project.jon_home + "/logs/console.log")
			bindings.put("emtalent.firedaemon.service.jon.firedaemon.log", project.jon_home + "/logs/firedaemon.log")
			bindings.put("emtalent.firedaemon.service.java_home", project.java_home)
			bindings.put("emtalent.firedaemon.service.jboss_home", project.jboss_home)
			
			def fireDaemonServiceFile = "${fireDaemonFolder}/fireDaemon_jonAgent.xml"
			
			templateEngine.makeTemplate(bindings,new File(project.AUTO_DEP_ATG_HOME + "/templates/firedaemon/fireDaemonServiceJON.xml.template"),new File(fireDaemonServiceFile))		
			logger.info(Logging.QUIET, "Added firedaemon service " + fireDaemonServiceFile)			
	}
	
	/**
	Add a fireDaemon service definition to the releaseFolder
	so that a search service can be started on the given server.
	*/
	private void addSearchServiceDefinition(String environment,String serverGroup,String serverName,Project project)
	{
			def releaseFolder = "${project.NASPath}/${project.ReleaseID}/"
			def fireDaemonFolder = releaseFolder + environment + '/' + serverGroup + '/' + serverName + '/FireDaemon/'
			if(!new File(fireDaemonFolder).exists())
			{
				project.ant.mkdir(dir:fireDaemonFolder)				
			}
						
			def bindings=[:]
			def fireDaemonServiceFile = fireDaemonFolder + 'fireDaemon_search.xml'
			
			templateEngine.makeTemplate(bindings,new File(project.AUTO_DEP_ATG_HOME + "/templates/firedaemon/fireDaemonServiceSearch.xml.template"),new File(fireDaemonServiceFile))		
			logger.info(Logging.QUIET, "Added firedaemon service " + fireDaemonServiceFile)		
	}				
}

