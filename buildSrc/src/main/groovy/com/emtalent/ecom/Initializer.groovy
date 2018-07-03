/**
Initializer.groovy  18/02/2010

A plugin that initialises the CB-ABS build
*/

package com.emtalent.atg

import org.gradle.api.Project
import org.gradle.api.plugins.ProjectPluginsContainer
import org.gradle.api.Plugin
import org.gradle.api.logging.Logging
import org.springframework.beans.factory.xml.XmlBeanFactory
import org.springframework.beans.factory.BeanFactory
import org.springframework.core.io.FileSystemResource
import com.emtalent.common.BeanManager

class Initializer extends AtgBase implements Plugin	
{
	void use(Project project, ProjectPluginsContainer projectPluginsHandler) 
	{ 
		def config = new ConfigSlurper().parse(new File(project.AUTO_DEP_ATG_HOME + "/atg.properties").toURL())
		project.setProperty('atg',config)

		project.setProperty('factory',BeanManager.getFactory())		
	}	
	
	/**
	Do necessary pre-build checks prior to starting the build process
	*/
	Boolean preBuildVerification(Project project)
	{
		//Verify that AUTO-DEP home folder exists.
		assert new File(project.AUTO_DEP_HOME).exists(), "Required folder ${project.AUTO_DEP_HOME} does not exist."
		
		//Verify that AUTO-DEP-ATG-HOME folder exists.
		assert new File(project.AUTO_DEP_ATG_HOME).exists(), "Required folder ${project.AUTO_DEP_ATG_HOME} does not exist."

		//Check for environment XML and XSD
		assert new File(project.ecom.environmentXsd).exists(), "Required file ${project.ecom.environmentXsd} does not exist." 

		//Check that an RFC number has been passed in as a project property
		assert project.property('RFC') != null
		
		project.envToBuild.each()
		{	
			def environment=it
			//Verify that the environment xml file exists.
			assert new File("${project.AUTO_DEP_ATG_HOME}/config/env/environment.${it}.xml").exists(), "Required file ${project.AUTO_DEP_ATG_HOME}/config/env/environment.${it}.xml does not exist."  		
			def envXmlParser = new EnvXmlParser("${project.AUTO_DEP_ATG_HOME}/config/env/environment.${environment}.xml")
			def applicationsXmlParser = BeanManager.getBean("applicationsXmlParser")
			
			project.appsToBuild.each()
			{
				def serverGroup=it
				def serverNames = envXmlParser.getServerNames(environment, serverGroup)	

				// This is the release folder under which slots are created.	
				def releaseFolder = "${project.NASPath}/${project.ReleaseID}/"

				serverNames.each
				{
					def serverName=it

					//Retreive a list of slots from the environment.xml file.
					def slots = envXmlParser.getSlotsForServer(environment, serverGroup, it)

					slots.each
					{
						//Copy build inputs described in applications.xml
						def applicationsXml = new XmlSlurper().parse(new File("${project.AUTO_DEP_ATG_HOME}/config/applications.xml"))
						def applicationXml = applicationsXml.application.find{it.@name.text().equals(serverGroup)}
						def sourcePath="${project.ATG_APPS_BUILD_HOME}"
						applicationXml.buildinput.item.each()
						{
							assert new File("${sourcePath}/${it.source.text()}").exists(), "An expected file or folder ${sourcePath}/${it.source.text()} does not exist"
						}						
					}
				}
			}			
		}
		
		//Verify that the storage folder for release deliverables exist.
		assert new File(project.NASPath).exists(), "Required folder ${project.NASPath} does not exist."
		
		//Verify that the JBoss Appserver Source files exist.
		def jbossSource = project.AUTO_DEP_HOME + '/' + project.jbossAppserverSource
		assert new File(jbossSource).exists(), "Required installer ${jbossSource} does not exist."
	
		logger.info(Logging.QUIET, "Pre-build verifications completed successfully.")
		return true;
	}

	/**
	Do necessary pre-deployment checks prior to starting the deployment process
	*/
	Boolean preDeployVerification(Project project)
	{
		//Checks go here.
		logger.info(Logging.QUIET, "Pre-deploy verifications completed successfully.")
		return true;
	}

}

