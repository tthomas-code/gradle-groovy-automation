/**
AuditManager.groovy

A class that manages the writing of audit maps (which are contectual information regarding
current build or deployment)
*/

package com.emtalent.common

import org.gradle.api.logging.Logging

class AuditManager extends AutodeployBase
{
	private String auditHeaders
	private String auditFilename
	private String auditLogLocation

	public void setAuditHeaders(String auditHeaders)
		{
			this.auditHeaders=auditHeaders
		}

	public String getAuditHeaders()
		{
			return auditHeaders
		}

	public void setAuditFilename(String auditFilename)
		{
			this.auditFilename=auditFilename
		}

	public String getAuditFilename()
		{
			return auditFilename
		}
		
	public void setAuditLogLocation(String auditLogLocation)
		{
			this.auditLogLocation=auditLogLocation
		}

	public String getAuditLogLocation()
		{
			return auditLogLocation
		}		

	public AuditManager(String auditHeaders, String auditFilename)
		{
			this.auditHeaders=auditHeaders
			this.auditFilename=auditFilename
		}

	/**
	Write an audit map to a CSV file as supplied by the auditFilename
	The beans.xml file can be used to configure the filename as well as the header names used within the file.
	*/
	public void write(Map auditMap)
		{
			File auditFile=new File("${auditLogLocation}\\${auditFilename}")
			
			if(!auditFile.exists())
			{
				String headerLine=''
				auditHeaders.tokenize(',').each()
				{
					headerLine+=it+','	
				}
				
				auditFile.write(headerLine)
			}
			
			String auditLine=''
			
			auditHeaders.tokenize(',').each()
			{
				def value=auditMap.get(it)
				
				if(value == null)
				{value = ''}
				
				auditLine = auditLine + value + ','	
			}
			
			auditFile.append("\n${auditLine}")
		}
}
