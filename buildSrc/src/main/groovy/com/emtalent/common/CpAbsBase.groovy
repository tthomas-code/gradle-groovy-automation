package com.emtalent.common
 
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

abstract class AutodeployBase {
 
  Logger getLogger() 
  { 
  	Logging.getLogger(this.class) 
  }

}
