/**
IISConfigGenerator.groovy  10/03/2010

A groovy class to generate MODJK configuration files for IIS.
*/

package com.emtalent.atg

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.logging.Logging
import java.util.Properties
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.File
import java.util.Map
import com.emtalent.ecom.EnvXmlParser
import com.emtalent.common.BindingsParser
import com.emtalent.common.BeanManager

class IISConfigGenerator extends AtgBase	
{

private BindingsParser bindingsParser
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

/**
Generate all versions of worker.properties.minimal including those required for session draining.
*/

public void generateWorkerFiles(Project project)
{
		project.envToBuild.each() 
		{						
			def env=it
			def envXmlParser = new com.emtalent.ecom.EnvXmlParser("${project.AUTO_DEP_ATG_HOME}/config/env/environment.${it}.xml")
			
			def applicationsXmlParser = project.factory.getBean("applicationsXmlParser")
			
			//Prepare IIS Configuration for backend applications
			def applications = applicationsXmlParser.getAppsOfType('BACK')
			def source="${project.AUTO_DEP_ATG_HOME}/templates/iis/bws"
			def destination="${project.NASPath}/${project.ReleaseID}/${env}/IISConfiguration/bws"
			
			configureWorkers(envXmlParser,env, source , destination, 'BACK','*','ACT',project)
			configureWorkers(envXmlParser,env, source , destination, 'BACK','SITE-A','DIS',project)
			configureWorkers(envXmlParser,env, source , destination, 'BACK','SITE-A','STP',project)
			configureWorkers(envXmlParser,env, source , destination, 'BACK','SITE-B','DIS',project)
			configureWorkers(envXmlParser,env, source , destination, 'BACK','SITE-B','STP',project)			


			applications = applicationsXmlParser.getAppsOfType('FRONT')
			source="${project.AUTO_DEP_ATG_HOME}/templates/iis/fws"
			destination="${project.NASPath}/${project.ReleaseID}/${env}/IISConfiguration/fws"
			
			configureWorkers(envXmlParser,env, source , destination, 'FRONT','*','ACT',project)		
			configureWorkers(envXmlParser,env, source , destination, 'FRONT','SITE-A','DIS',project)			
			configureWorkers(envXmlParser,env, source , destination, 'FRONT','SITE-A','STP',project)
			configureWorkers(envXmlParser,env, source , destination, 'FRONT','SITE-B','DIS',project)
			configureWorkers(envXmlParser,env, source , destination, 'FRONT','SITE-B','STP',project)
		}
}

/**
Configure the properties file specified by the 'propertiesFile' and write 
it to 'destination'
*/
void configureWorkers(EnvXmlParser envXmlParser, String environment, String source, String destination, String appType, String site, String workerStatus, Project project)
{
	File template=new File("${source}/workers.properties.minimal.template");
	new File("${destination}").mkdirs()
	File output= new File("${destination}/workers.properties.minimal.${site.equals('*')?'ALL':site}.${workerStatus}")
	output.write(template.getText())

	String workers="jkstatus"
	String workerBalancers=""
	String workerType=""
	String stickySessions=""


	String[] appsForEnv = envXmlParser.getAppsForEnvironment(environment)
	appsForEnv.each
	{
		def serverGroup=it
		if (!applicationsXmlParser.getAppType(serverGroup).equals(appType))
			{ return }
		
		String delimiter=""
		def serverGroupBalancers="worker.${serverGroup}.balance_workers="

		workers="${workers},${serverGroup}"
		workerType="${workerType}\nworker.${serverGroup}.type=lb"
		stickySessions="${stickySessions}\nworker.${serverGroup}.sticky_session=true"

		def serverNames=envXmlParser.getServerNames(environment,serverGroup)
		
		
		serverNames.each
			{
				def serverName=it
				def shorterServerName=serverName.minus(project.serverNamePartForRemoval)
				def slots=envXmlParser.getSlotsForServer(environment, serverGroup, serverName)
				slots.each
				{
					def slot=it
					
					//Do this only for loadBearing servers.
					if(!envXmlParser.isSlotLoadBearing(environment,serverGroup,serverName,slot)) return
					
					def slotType = envXmlParser.getSlotType(environment,serverGroup,serverName,slot)					
					Map jbossSlotBindings=bindingsParser.getJbossBindings(new File('platforms/atg/config/jboss-bindings.xml'),serverGroup,environment,slotType,it)
					
					output.append("\n###########################################################")
					output.append("\n# Worker configuration for ${serverName}_${slot}")
					output.append("\n###########################################################")					
					output.append("\nworker.${shorterServerName}_${slot}.port=${jbossSlotBindings.get("emtalent.jboss.webdeployer.server.ajp.connector.port")}")
					output.append("\nworker.${shorterServerName}_${slot}.host=${serverName}")
					output.append("\nworker.${shorterServerName}_${slot}.type=ajp13")
					output.append("\nworker.${shorterServerName}_${slot}.ping_mode=A")
					output.append("\nworker.${shorterServerName}_${slot}.socket_connect_timeout=600000")
					output.append("\nworker.${shorterServerName}_${slot}.socket_keepalive=true")
					output.append("\nworker.${shorterServerName}_${slot}.connection_pool_timeout=900")
					output.append("\nworker.${shorterServerName}_${slot}.connection_pool_size=250")
					output.append("\nworker.${shorterServerName}_${slot}.connection_ping_interval=100")

					def partition = envXmlParser.getPartitionName(environment,serverGroup,serverName,slot)
					output.append("\nworker.${shorterServerName}_${slot}.domain=${partition}")

					output.append("\nworker.${shorterServerName}_${slot}.max_packet_size=10240")
					output.append("\nworker.${shorterServerName}_${slot}.recovery_options=7")
					
					def serverSite=envXmlParser.getServerSite(environment,serverGroup,serverName)
					
					if(serverSite.equals(site) || site.equals('*'))
					{
						output.append("\nworker.${shorterServerName}_${slot}.activation=${workerStatus}")
					}
					else
					{
						output.append("\nworker.${shorterServerName}_${slot}.activation=ACT")
					}

					output.append("\n\n")
					serverGroupBalancers="${serverGroupBalancers}${delimiter}${shorterServerName}_${slot}"
					delimiter=","
				}
			}

		workerBalancers="${workerBalancers}\n${serverGroupBalancers}"
	}
	output.append("\n")
	output.append("\n###########################################################")
	output.append("\n# Worker Balancing")
	output.append("\n###########################################################")					
	output.append("\n${workerBalancers}")
	output.append("\n\n")

	output.append("\n###########################################################")
	output.append("\n# Workers type")
	output.append("\n###########################################################")		
	output.append("${workerType}")	
	output.append("\n\n")

	output.append("\n###########################################################")
	output.append("\n# Workers sticky sessions")
	output.append("\n###########################################################")		
	output.append("${stickySessions}")
	output.append("\n\n")
		
	output.append("\n###########################################################")
	output.append("\n# Workers list")
	output.append("\n###########################################################")		
	output.append("\nworker.list=${workers}")
	output.append("\n\n")	

	logger.info(Logging.QUIET, "Created file ${output.getPath()} \n")	
	
}


}


