package com.emtalent.common

import org.springframework.beans.factory.xml.XmlBeanFactory
import org.springframework.beans.factory.BeanFactory
import org.springframework.core.io.FileSystemResource


class BeanManager extends AutodeployBase
{

	private static BeanFactory beanFactory = new XmlBeanFactory(new FileSystemResource("beans.xml"));

	public static Object getBean(String beanName)
	{
		beanFactory.getBean(beanName)
	}

	public static BeanFactory getFactory()
	{
		return beanFactory
	}

}