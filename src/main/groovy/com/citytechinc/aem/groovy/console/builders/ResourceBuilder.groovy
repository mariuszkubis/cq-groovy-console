package com.citytechinc.aem.groovy.console.builders

import com.citytechinc.aem.groovy.console.helpers.Groover
import org.apache.sling.api.resource.Resource
import org.apache.sling.api.resource.ResourceResolver

import javax.jcr.nodetype.NodeType

//TODO the builder needs to commit changes automatically depending on the dry run flag in Groover
class ResourceBuilder extends BuilderSupport {

	private final ResourceResolver resourceResolver

	private final Resource rootResource

	private Resource currentResource

	private Closure println

	ResourceBuilder(ResourceResolver resourceResolver) {
		this(resourceResolver, "/")
	}

	ResourceBuilder(ResourceResolver resourceResolver, Resource rootResource) {
		this.resourceResolver = resourceResolver
		this.rootResource = rootResource
		this.currentResource = this.rootResource
		this.println = Groover.THREAD_LOCAL_LOG.get()
	}

	void setParent(Object parent, Object child) {
		//empty implementation of an abstract method; sling resources have parent-child relations assigned on creation
	}

	def createNode(name) {
		createNode(name, NodeType.NT_UNSTRUCTURED)
	}

	def createNode(name, Map attributes) {
		createNode(name, attributes, NodeType.NT_UNSTRUCTURED)
	}

	//TODO add a check for primaryNodeType and a logging for situation where
	def createNode(name, Object primaryNodeTypeName) {
		def childExists = currentResource.getChild(name)
		if (childExists) {
			currentResource = childExists
			println "Resource ${currentResource.path} already exists"
		} else {
			currentResource = currentResource.getOrAddChild(name, primaryNodeTypeName)
			println "Created resource: ${currentResource.path}"
		}
		currentResource
	}

	def createNode(name, Map attributes, Object primaryNodeTypeName) {
		createNode(name, primaryNodeTypeName)
		setAttributes(attributes)
		currentResource
	}

	def setAttributes(Map attributes) {
		currentResource.set(attributes)
		println "Set attributes: ${attributes}"
	}

	void nodeCompleted(parent, node) {
		currentResource = currentResource.getParent()
	}

	void setAttributesToCurrentResource(Map attributes) {
		//TODO this should add the appropriate cq:Modified and cq:ModifiedBy properties to the current resource where applicable
	}
}
