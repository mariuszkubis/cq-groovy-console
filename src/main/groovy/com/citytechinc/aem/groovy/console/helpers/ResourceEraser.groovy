package com.citytechinc.aem.groovy.console.helpers

import org.apache.sling.api.resource.Resource
import org.apache.sling.api.resource.ResourceResolver
import org.apache.sling.query.SlingQuery

import javax.jcr.RepositoryException
import javax.jcr.query.Query

class ResourceEraser {

	ResourceResolver resourceResolver

	Closure println

	ResourceEraser(ResourceResolver resourceResolver) {
		this.resourceResolver = resourceResolver
		this.println = Groover.THREAD_LOCAL_LOG.get()
	}

	Set<Resource> removeResources(String query) {
		resourceResolver.findResources(query, Query.JCR_SQL2).collect { removeResource it } as Set<Resource>
	}

	Set<Resource> removeResources(SlingQuery slingQuery) {
		slingQuery.asList().collect { removeResource it } as Set<Resource>
	}

	Set<Resource> removeResources(List<String> paths) {
		paths.collect {removeResource it} as Set<Resource>
	}

	Set<Resource> removeResources(Iterator<Resource> iterator) {
		iterator.collect { removeResource it } as Set<Resource>
	}

	Resource removeResource(String path) {
		removeResource resourceResolver.getResource(path)
	}

	Resource removeResource(Resource resource) {
		if(!resource){
			return null
		}
		def parent = resource.getParent()
		println "Removing ${resource?.getPath()}"
		try {
			resourceResolver.delete(resource)
			parent
		} catch(RepositoryException e) {
			println "Error: ${e.getMessage()}"
		}
		parent
	}
}
