/**
EnvironmentNavigator.groovy 11/05/2010

A class that provides utility methods for easily navigating through an evironment definition.
*/
package com.emtalent.atg

import java.util.List
import org.gradle.api.*
import org.gradle.api.plugins.*
import com.emtalent.common.BeanManager

class EnvironmentNavigator extends AtgBase 
{
	/**
	Invoke the closure for every slot in every server 
	in every serverGroup and every environment defined in gradle.properties
	*/
	void eachSlot(Project project, Closure c)
	{
		project.envToBuild.each() 
		{				
			def env = it
			project.appsToBuild.each()
			{			
				def app = it
				def envXmlParser = new EnvXmlParser("${project.AUTO_DEP_ATG_HOME}/config/env/environment.${env}.xml")
				def servers = envXmlParser.getServerNames(env, app)
				servers.each
				{
					def server = it
					def slots = envXmlParser.getSlotsForServer(env, app, server)
					slots.each
					{
						def slot=it
						c.call(env, app, server, slot, project)
					}
				}				
			}
		}			
	}
	
	/**
	Invoke the closure for every slot in every server 
	in every serverGroup and every environment defined in gradle.properties
	*/
	void eachServer(Project project, Closure c)
	{
		project.envToBuild.each() 
		{				
			def env = it
			project.appsToBuild.each()
			{			
				def app = it
				def envXmlParser = new EnvXmlParser("${project.AUTO_DEP_ATG_HOME}/config/env/environment.${env}.xml")
				def servers = envXmlParser.getServerNames(env, app)
				servers.each
				{
					def server = it
					c.call(env, app, server, project)
				}				
			}
		}			
	}
	
	/**
	Invoke the closure for the first server in the specified serverGroup.
	for every environment defined in gradle.properties
	*/
	void firstServerInApp(String application,Project project, Closure c)
	{
		project.envToBuild.each() 
		{				
			def env = it
			project.appsToBuild.each()
			{			
				def app = it
				if (app != application) return
				
				def envXmlParser = new EnvXmlParser("${project.AUTO_DEP_ATG_HOME}/config/env/environment.${env}.xml")
				def servers = envXmlParser.getServerNames(env, app)
				c.call(env, app, servers[0], project)			
			}
		}			
	}
	
	/**
	Some values provide in xml configuration files can have place holders for common context informations
	such as environment,server,application,slot and slotNumber. This method will replace such placeholders
	the appropriate values.
	*/
	String applySlotContext(String text, String environment,String application,String server,String slot,Project project)
	{
		def bindingsParser = BeanManager.getBean("bindingsParser")		
		def envXmlParser = new EnvXmlParser("${project.AUTO_DEP_ATG_HOME}/config/env/environment.${environment}.xml")
		def slotType = envXmlParser.getSlotType(environment, application,server,slot)						
		def slotNumber=bindingsParser.getJbossBindings(new File("${project.AUTO_DEP_ATG_HOME}/config/jboss-bindings.xml"),application,environment,slotType,slot).get("emtalent.jboss.slot.number")
		text=text.replaceAll("#environment#",environment)
		text=text.replaceAll("#application#",application)
		text=text.replaceAll("#server#",server)
		text=text.replaceAll("#slot#",slot)
		text=text.replaceAll("#slotNumber#",slotNumber)
		return text			
	}		

}