package com.emtalent.common

import java.io.InputStream
import java.io.OutputStream
import java.io.FileInputStream
import java.io.FileOutputStream

class FileTools extends AutodeployBase 
{
	
	/**
	Copy the contents of source folder to destination
	*/
	void copyFolder(String source,String destination)
	{
		new File(source).eachFile()
		{
			if(it.isDirectory())
			{
				new File("${destination}/${it.getName()}").mkdir()
				copyFolder("${source}/${it.getName()}","${destination}/${it.getName()}")
			}
			else
			{
				copyFile(new File("${source}/${it.getName()}"),new File("${destination}/${it.getName()}"))
			}
		
		}
	}
	
	/**
	Copy a file to another.
	*/
	void copyFile(File src, File dst) throws IOException 
	{
		InputStream inStream = new FileInputStream(src)
		OutputStream outStream = new FileOutputStream(dst)

		byte[] buf = new byte[1024]
		int len
		while ((len = inStream.read(buf)) > 0) 
			{
			    outStream.write(buf, 0, len)
			}
		inStream.close()
		outStream.close()
    	}
	
}