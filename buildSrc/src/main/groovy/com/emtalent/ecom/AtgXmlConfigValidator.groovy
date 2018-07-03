/**
AtgXmlConfigValidator.groovy 19/03/2010

	There are a number of .xml and .wdl files that are part of ATG Configuration files. These files 
	contain environment specific values that should be replaced with the values appropriate to a given environment.
	To facilitate this, these files have been converted to templates and are stored in TFS as part of autodeploy code.
	
	Storing these atg config files as templates in TFS require us to ensure that any changes to the original files 
	by the development team are properly merged into the templates kept in TFS. The methods below compares the templates and 
	the original files and alerts the user if the templates need updating.
*/

package com.emtalent.atg

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.logging.Logging
import com.emtalent.ecom.EnvXmlParser
import com.emtalent.ecom.BaseConfigAdapterXMLParser
import com.emtalent.common.BeanManager
import groovy.util.slurpersupport.GPathResult

class AtgXmlConfigValidator extends AtgBase 
{
	/**
	There are a number of .xml and .wdl files that are part of ATG Configuration files. These files 
	contain environment specific values that should be replaced with the values appropriate to a given environment.
	To facilitate this, these files have been converted to templates and are stored in TFS as part of autodeploy code.
	
	Storing these atg config files as templates in TFS require us to ensure that any changes to the original files 
	by the development team are properly merged into the templates kept in TFS. The method below compares the templates and 
	the original files and alerts the user if the templates need updating.
	*/
	private boolean validate(String environment,String application, String server, String slot, Project project)
	{
		boolean valid=true
		def baseConfigAdapterXMLParser = new BaseConfigAdapterXMLParser("${project.AUTO_DEP_ATG_HOME}/config/${project.baseConfigAdapter}")
		def baseConfigSlot = baseConfigAdapterXMLParser.getSlotForApp(application)
		def baseConfigServer = baseConfigAdapterXMLParser.getServerForApp(application)		
		def bindingsParser = BeanManager.getBean("bindingsParser")
		def envNavigator=BeanManager.getBean("environmentNavigator")
		def envXmlParser = new EnvXmlParser("${project.AUTO_DEP_ATG_HOME}/config/env/environment.${environment}.xml")
	    def slotType = envXmlParser.getSlotType(environment, application,server,slot)	

		Map envBindings = bindingsParser.getBaseConfigAtgBindings(new File("${project.AUTO_DEP_ATG_HOME}/config/atg-env-bindings.xml"),"${project.baseConfigEnv}",environment)

		def templatesFolder="${project.AUTO_DEP_HOME}/${project.atgConfigLocation}/ATG-Data/${application}/${environment}/${baseConfigSlot}/localconfig"
		templatesFolder=templatesFolder.replace('/','\\')
		def baseConfigFolder="${project.AUTO_DEP_GOODS_IN}/${project.atgConfigForSIT}/${baseConfigServer}/${baseConfigSlot}/localconfig"
		
		//When the validation fails, this folder will be used to write the differences.
		def diffFolder = "${project.atg_config_target}/${environment}/DIFF/${application}"
		
		new File(templatesFolder).eachFileRecurse
		{
			//Derive the name of the corresponding file in SIT configuration
			def relativeFilePath=it.getPath().replace(templatesFolder,'')
			def baseConfigFile="${baseConfigFolder}${relativeFilePath}"

			if (! new File(baseConfigFile).exists())
			{
				logger.info(Logging.QUIET, "\n\n=========================================================================" )
				logger.info(Logging.QUIET, "AtgConfig validation failed:Expected file or folder ${baseConfigFile} does not exist." )
				valid=false
				return
			}
			
			//Only files need comparing.
			if (!it.isFile()) return
						
			//Replace values in the template file.
			def templateText=it.getText()		
			envBindings.each
			{
				templateText=templateText.replace("#${it.key}#",envNavigator.applySlotContext(it.value,environment,application,server,slot,project))
			}
			

			def baseConfigFileText=new File(baseConfigFile).getText()
			
			if (! (templateText.trim().tokenize() == baseConfigFileText.trim().tokenize()))
			{
				logger.info(Logging.QUIET, "\n\n=========================================================================" )
				
				//Write differences to files for later review.
				File autodeployDiffFile = new File("${diffFolder}/${relativeFilePath}.autodeploy")
				File baseConfigDiffFile = new File("${diffFolder}/${relativeFilePath}.BASE")
				File autodeployDiffFileNorm = new File("${diffFolder}/${relativeFilePath}.normalised.autodeploy")
				File baseConfigDiffFileNorm = new File("${diffFolder}/${relativeFilePath}.normalised.BASE")
				
				project.ant.mkdir(dir:autodeployDiffFile.getParent())
				autodeployDiffFile.write(templateText)
				baseConfigDiffFile.write(baseConfigFileText)
				autodeployDiffFileNorm.write(templateText.tokenize().toString())
				baseConfigDiffFileNorm.write(baseConfigFileText.tokenize().toString())				

				logger.info(Logging.QUIET, "AtgConfig validation failed:The files ${it.getPath()} and ${baseConfigFile} are different."
										+"\nThe differences can be viewed at ${diffFolder}" )				
				valid=false
				return
			}
		}
		return valid
	}
}
