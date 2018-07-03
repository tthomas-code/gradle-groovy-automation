package com.emtalent.atg

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.logging.Logging
import com.emtalent.ecom.EnvXmlParser
import com.emtalent.common.BeanManager
import com.emtalent.ecom.ApplicationsXmlParser
import com.emtalent.common.TemplateEngine

class ReleaseManager extends AtgBase {

	private TemplateEngine templateEngine

	public TemplateEngine getTemplateEngine()
	{
		return templateEngine
	}

	public void setTemplateEngine(TemplateEngine engine)
	{
		templateEngine = engine
	}	
	
	/**
	Generate a unique release number. This will be used to name the release folder.
	*/
	String generateReleaseId(Project project)
	{
		project.ant.buildnumber (file : "mybuild.number")
		def today= new Date().format('ddMM')
        def releaseNumber = 'R_' + project.ant.properties."build.number" + '_' + today

		File file = new File("latestBuildLocation.txt")
		file.write(releaseNumber)

		return releaseNumber
	}

    /** 
    Validate a Release Number for deployment
    */
    String validateReleaseId(Project project, String releaseNumber)
    {
      if (releaseNumber.equals("latest"))
      {
        File file = new File("latestBuildLocation.txt")
        def latestReleaseNumber
        file.withReader { line->latestReleaseNumber = line.readLine().trim()}
        return latestReleaseNumber
      } 
      return releaseNumber
    }
	
	/**
	Create a root folder for each server on the release path
	*/
	void createServerRootFolder(String environment, String serverGroup, String server, Project project) 
	{
		def rootFolder = "${project.NASPath}/${project.ReleaseID}/${environment}/${serverGroup}/${server}"
		project.ant.mkdir(dir:rootFolder)
		logger.info(Logging.QUIET, "Folder created: ${rootFolder}" )
	}	


	
	/**
	Create a temporary archieve as defined in applications.xml. The file is copied under the 
	"environment" and "servergroup" passed in as parameters.
	
	param: environment - The environment under which the archieve is to be copied.
	param: serverGroup - The serverGroup under which the archieve is to be copied.
	*/
	void createTempArchivesForRelease(String environment, String serverGroup, Project project) 
	{
		//Create build inputs described in applications.xml
		def applicationsXml = new XmlSlurper().parse(new File("${project.AUTO_DEP_ATG_HOME}/config/applications.xml"))
		def applicationXml = applicationsXml.application.find{it.@name.text().equals(serverGroup)}
		def sourcePath="${project.ATG_APPS_BUILD_HOME}/${serverGroup}/server/slot"
		def destinationPath="${project.ATG_APPS_BUILD_HOME}/${serverGroup}/server/slot"
		 
		applicationXml.buildinput.item.each()
		{
			def item=it
			if(item.source.@type.text().equals("explodedEar"))
			{
				if(item.destination.@type.text().equals("tempArchive"))
				{
				    def targetPathArray = item.destination.text().split('/')
					def targetFilename = targetPathArray[targetPathArray.length-1]
					def targetPath = item.destination.text().replaceFirst('/' + targetFilename, '')
					
					project.ant.mkdir(dir:sourcePath + "/" + targetPath)
					project.ant.jar(basedir:"${sourcePath}/${item.source.text()}",destfile:"${destinationPath}/${item.destination.text()}")
					logger.info(Logging.QUIET, "Jar'ed up ${sourcePath}/${item.source.text()} to ${destinationPath}/${item.destination.text()}")
				}
			}							
		}	
	}

	/**
	Copy a temporary archieve as defined in applications.xml. The file is copied under the 
	"environment" and "servergroup" passed in as parameters.
	
	param: environment - The environment under which the archieve is to be copied.
	param: serverGroup - The serverGroup under which the archieve is to be copied.
	*/
	void copyTempArchiveToRelease(String environment, String serverGroup, Project project) 
	{
		def envXmlParser = new EnvXmlParser("${project.AUTO_DEP_ATG_HOME}/config/env/environment.${environment}.xml")
		
		//Retreive a list server names defined for the specified server group in the 
		//environment.xml file.
		
		def serverNames = envXmlParser.getServerNames(environment, serverGroup)	

		// This is the release folder under which slots are created.	
		def releaseFolder = "${project.NASPath}/${project.ReleaseID}/"

		serverNames.each
		{
			def serverName=it

			logger.info(Logging.QUIET, "\nCopying tempory archieve to " + serverName)
			logger.info(Logging.QUIET, "**********************************")

			//Copy build inputs described in applications.xml
			def applicationsXml = new XmlSlurper().parse(new File("${project.AUTO_DEP_ATG_HOME}/config/applications.xml"))
			def applicationXml = applicationsXml.application.find{it.@name.text().equals(serverGroup)}
			def sourcePath="${project.ATG_APPS_BUILD_HOME}/${serverGroup}/server/slot"
			def destinationPath="${releaseFolder}${environment}/${serverGroup}/${serverName}" 
							
			def postDeployBatchFolder = destinationPath + '/scripts/batch/'
			def postDeployBatchFile = postDeployBatchFolder + 'deliverCompressedEar.bat' 
			 
			applicationXml.buildinput.item.each()
			{
				def item=it
				if(item.source.@type.text().equals("explodedEar"))
				{
					if(item.destination.@type.text().equals("tempArchive"))
					{
						def targetPathArray = item.destination.text().split('/')
						def targetFilename = targetPathArray[targetPathArray.length-1]
						def targetPath = item.destination.text().replaceFirst('/' + targetFilename, '')
						
					    // Generate a postDeploy batch script based on applications.xml
					    def bindings=[:]
					    bindings.put("compressed.ear.file.name",targetFilename)
					    bindings.put("compressed.ear.file.path",targetPath)
						project.ant.mkdir(dir:postDeployBatchFolder)
						templateEngine.makeTemplate(bindings,new File(project.AUTO_DEP_ATG_HOME + "/templates/batch/deliverCompressedEar.bat.template"),new File(postDeployBatchFile))							
					
					    // This is counter intuative - however the orignal source has been zipped up and stored at sourcePath/destination.text()
						project.ant.copy(file:"${sourcePath}/${item.destination.text()}",toFile:"${destinationPath}/${item.destination.text()}",overwrite:true)
					}
				}
				logger.info(Logging.QUIET, "Copied ${sourcePath}/${item.destination.text()} to ${destinationPath}/${item.destination.text()}")					
			}
		}
    }	
	
	/**
	Delete releases that older than the number days indicated by project property AGE.
	*/
	public void deleteReleases(Project project)
		{
			logger.info(Logging.QUIET, "deleteReleases : Start.")
			logger.info(Logging.QUIET, "============================================================")
			logger.info(Logging.QUIET, "Deleting all releases before ${new Date().minus(new Integer(project.AGE).intValue())}")

			new File(project.NASPath).eachDir()
			{
				if(new Date().minus(new Date(it.lastModified())) > new Integer(project.AGE).intValue()&& it.getName().startsWith('R'))
				{
					logger.info(Logging.QUIET, "Deleting ${it.getPath()}")
					project.ant.delete(dir:it.getPath())
				}
			}
			logger.info(Logging.QUIET, "deleteReleases : End.")
			logger.info(Logging.QUIET, "------------------------------------------------------------")
			logger.info(Logging.QUIET, "\n")
		}		
}
