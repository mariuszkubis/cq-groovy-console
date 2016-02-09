package com.citytechinc.aem.groovy.console.helpers

import com.citytechinc.aem.groovy.console.decorators.ValueMapDecorator
import org.apache.sling.api.resource.Resource
import org.apache.sling.api.resource.ResourceResolver
import org.apache.sling.query.SlingQuery

import javax.jcr.query.Query

class ClosureProcessor {

	ResourceResolver resourceResolver

	Closure println

	ClosureProcessor(ResourceResolver resourceResolver) {
		this.resourceResolver = resourceResolver
		this.println = Groover.THREAD_LOCAL_LOG.get()
	}

	List<Resource> process(String query, Closure<Boolean> c) {
		resourceResolver.findResources(query, Query.JCR_SQL2)
				.findAll {processResource(it, c)} as List<Resource>
	}

	List<Resource> process(SlingQuery slingQuery, Closure<Boolean> c) {
		slingQuery?.asList()
				.findAll {processResource(it, c)} as List<Resource>
	}

	List<Resource> process(List<String> paths, Closure<Boolean> c) {
		paths?.collect {
			resourceResolver.getResource(it)?: handleNoResourceFound(it)
		}.findAll {
			it && processResource(it, c)
		} as List<Resource>
	}

	List<Resource> process(Iterator<Resource> iterator, Closure<Boolean> c) {
		iterator?.toList()
				.findAll {processResource(it, c)} as List<Resource>
	}

	private handleNoResourceFound(res) {
		println "Cannot resolve path: ${res}"
		return
	}

	private boolean processResource(Resource resource, Closure<Boolean> c) {
		if (!resource) {
			return false
		}
		ValueMapDecorator modifiableMap = new ValueMapDecorator(resource)
		println "Processing ${resource.path}"
		c?.call(resource, modifiableMap)
	}
}
