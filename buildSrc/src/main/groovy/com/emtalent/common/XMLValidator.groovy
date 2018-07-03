package com.emtalent.common

import com.emtalent.ecom.AtgBase
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

class XMLValidator extends AutodeployBase {

    Boolean validate(String xsdFile, String xmlFile)
    {
      def xsd = new File(xsdFile)
      def xml = new File(xmlFile)	    
     
      def factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
      def schema = factory.newSchema(xsd)
      def validator = schema.newValidator()
      validator.validate(new StreamSource(new FileReader(xml)))	
      return true
    }
    
}