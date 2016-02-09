package com.citytechinc.aem.groovy.console.helpers

import com.citytechinc.aem.groovy.console.decorators.ValueMapDecorator
import org.apache.sling.api.resource.Resource
import org.apache.sling.api.resource.ResourceResolver
import org.apache.sling.query.SlingQuery

import javax.jcr.query.Query

class PropertyWriter {

	ResourceResolver resourceResolver

	Closure println

	public PropertyWriter(ResourceResolver resourceResolver) {
		this.resourceResolver = resourceResolver
		this.println = Groover.THREAD_LOCAL_LOG.get()
	}

	List<Resource> setResourceProperties(String query, Map<String, ?> valueMap, boolean updateOnly) {
		resourceResolver.findResources(query, Query.JCR_SQL2)
				.findAll { setProperties(it, valueMap, updateOnly) } as List<Resource>
	}

	List<Resource> setResourceProperties(SlingQuery slingQuery, Map<String, ?> valueMap, boolean updateOnly) {
		slingQuery.asList()
				.findAll { setProperties(it, valueMap, updateOnly) } as List<Resource>
	}

	List<Resource> setResourceProperties(List<String> paths, Map<String, ?> valueMap, boolean updateOnly) {
		paths.collect {
			resourceResolver.getResource(it) ?: handleNoResourceFound(it)
		}.findAll {
			it && setProperties(it, valueMap, updateOnly)
		} as List<Resource>
	}

	List<Resource> setResourceProperties(Iterator<Resource> iterator, Map<String, ?> valueMap, boolean updateOnly) {
		iterator.toList()
				.findAll { setProperties(it, valueMap, updateOnly) } as List<Resource>
	}

	List<Resource> removeExistingProperties(String query, Collection<String> propertyNames) {
		resourceResolver.findResources(query, Query.JCR_SQL2)
				.findAll { removeProperties(it, propertyNames) } as List<Resource>
	}

	List<Resource> removeExistingProperties(SlingQuery slingQuery, Collection<String> propertyNames) {
		slingQuery.asList()
				.findAll { removeProperties(it, propertyNames) } as List<Resource>
	}

	List<Resource> removeExistingProperties(List<String> paths, Collection<String> propertyNames) {
		paths.collect {
			resourceResolver.getResource(it) ?: handleNoResourceFound(it)
		}.findAll {
			it && removeProperties(it, propertyNames)
		} as List<Resource>
	}

	List<Resource> removeExistingProperties(Iterator<Resource> iterator, Collection<String> propertyNames) {
		iterator.toList()
				.findAll { removeProperties(it, propertyNames) } as List<Resource>
	}

	List<Resource> removeExistingProperties(String query, Map<String, ?> valueMap) {
		resourceResolver.findResources(query, Query.JCR_SQL2)
				.findAll { removePropertiesIfSet(it, valueMap) } as List<Resource>
	}

	List<Resource> removeExistingProperties(SlingQuery slingQuery, Map<String, ?> valueMap) {
		slingQuery.asList()
				.findAll { removePropertiesIfSet(it, valueMap) } as List<Resource>
	}

	List<Resource> removeExistingProperties(List<String> paths, Map<String, ?> valueMap) {
		paths.collect {
			resourceResolver.getResource(it) ?: handleNoResourceFound(it)
		}.findAll {
			it && removePropertiesIfSet(it, valueMap)
		} as List<Resource>
	}

	List<Resource> removeExistingProperties(Iterator<Resource> iterator, Map<String, ?> valueMap) {
		iterator.toList()
				.findAll { removePropertiesIfSet(it, valueMap) } as List<Resource>
	}

	private handleNoResourceFound(res) {
		println "Cannot resolve path: ${res}"
		return
	}

	private boolean setProperties(Resource resource, Map<String, ?> valueMap, Boolean updateOnly) {
		if (!resource) {
			return false
		}
		ValueMapDecorator modifiableMap = new ValueMapDecorator(resource)
		println "Setting properties for ${resource.path}"

		boolean updated

		valueMap?.entrySet().each { entry ->
			if (!updateOnly || (updateOnly && modifiableMap.containsKey(entry.key))) {
				modifiableMap.put(entry.key, entry.value)
				updated = true
			}
		}
		updated
	}

	private boolean removeProperties(Resource resource, Collection<String> propertyNames) {
		if (!resource) {
			return false
		}
		ValueMapDecorator modifiableMap = new ValueMapDecorator(resource)
		println "Removing properties from ${resource.path}"

		boolean result

		propertyNames?.each { propertyName ->
			boolean removed = modifiableMap.remove(propertyName)
			result = result || removed
		}
		result
	}

	private boolean removePropertiesIfSet(Resource resource, Map<String, ?> valueMap) {
		if (!resource) {
			return false
		}
		ValueMapDecorator modifiableMap = new ValueMapDecorator(resource)
		println "Removing properties from ${resource.path}"

		boolean updated

		valueMap?.entrySet().each { entry ->
			if (modifiableMap.containsKey(entry.key) && modifiableMap.get(entry.key) == entry.value) {
				modifiableMap.remove(entry.key)
				updated = true
			}
		}
		updated
	}
}
