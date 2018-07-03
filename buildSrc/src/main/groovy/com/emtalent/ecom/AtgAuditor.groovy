/**
AtgAuditor.groovy 11/05/2010

A class that helps ATG subproject in auditing build and deployment activities.
*/

package com.emtalent.atg

import com.emtalent.common.BeanManager
import java.util.Date
import org.gradle.api.*

class AtgAuditor extends AtgBase 
{
	/**
	Add context information regarding the current build to the auditMap object 
	and then use AuditManager to write them to a store
	*/
	void auditBuild(Project project)
	{
		def auditManager=BeanManager.getBean("auditManager")
		Map auditMap=BeanManager.getBean("auditMap")
		auditManager.setAuditLogLocation(project.AUTO_DEP_GOODS_OUT)

		project.atg_applications.tokenize(',').each()
		{
			auditMap.put("DATE",new Date().format('dd/MM/yyy HH:mm'))
			auditMap.put("ENVIRONMENT",project.atg_environments)
			auditMap.put("APPLICATION",it)
			auditMap.put("RFC",project.RFC)
			auditMap.put("autodeploy_VERSION",project.VERSION)
			auditMap.put("ATG_CONFIG_TFS_PATH",project.atgConfigLocation)
			auditMap.put("ATG_SIT_CONFIG_PATH",project.atgConfigForSIT)
			auditMap.put("RELEASE_OUTPUT","${project.NASPath}/${project.ReleaseID}")
			auditMap.put("RELEASE_INPUT",project.ATG_APPS_BUILD_HOME)
			auditMap.put("ACTION","BUILD")
			
			auditManager.write(auditMap)	
		}
		def converter=new com.emtalent.common.Csv2Xml()
		
		def source="${auditManager.getAuditLogLocation()}\\${auditManager.getAuditFilename()}"
		def destination="${auditManager.getAuditLogLocation()}\\${auditManager.getAuditFilename()}.XML"
		
		converter.convert(source,destination,project)		
	}
	
	/**
	Add context information regarding the currentdeployment to the auditMap object 
	and then use AuditManager to write them to a store
	*/	
	void auditDeploy(Project project)
	{
		def auditManager=BeanManager.getBean("auditManager")
		Map auditMap=BeanManager.getBean("auditMap")
		auditManager.setAuditLogLocation(project.AUTO_DEP_GOODS_OUT)
		
		project.ODServerGroups.tokenize(',').each()
		{

			auditMap.put("DATE",new Date().format('dd/MM/yyy HH:mm'))
			auditMap.put("ENVIRONMENT",project.ODEnv)
			auditMap.put("APPLICATION",it)
			auditMap.put("RFC",'-')			
			auditMap.put("autodeploy_VERSION",project.VERSION)
			auditMap.put("ATG_CONFIG_TFS_PATH",'-')
			auditMap.put("ATG_SIT_CONFIG_PATH",'-')								
			auditMap.put("RELEASE_OUTPUT","${project.NASPath}/${project.ODRelease}")
			auditMap.put("RELEASE_INPUT",'-')			
			auditMap.put("ACTION","DEPLOY")

			
			auditManager.write(auditMap)	
		}
		def converter=new com.emtalent.common.Csv2Xml()
		
		def source="${auditManager.getAuditLogLocation()}\\${auditManager.getAuditFilename()}"
		def destination="${auditManager.getAuditLogLocation()}\\${auditManager.getAuditFilename()}.XML"
		
		converter.convert(source,destination,project)
	}
	
	
	
}