package com.citytechinc.aem.groovy.console.decorators

import com.citytechinc.aem.groovy.console.helpers.Groover
import org.apache.sling.api.resource.ModifiableValueMap
import org.apache.sling.api.resource.Resource
import org.apache.sling.api.resource.ValueMap

class ValueMapDecorator implements ValueMap {

	ValueMap valueMap

	Closure println

	def ValueMapDecorator(ValueMap valueMap) {
		this.valueMap = valueMap
		this.println = Groover.THREAD_LOCAL_LOG.get()
	}

	def ValueMapDecorator(Resource resource) {
		this(resource.adaptTo(ModifiableValueMap.class))
	}

	def remove(key) {
		valueMap?.remove(key)
	}

	boolean containsValue(value) {
		valueMap?.containsValue(value)
	}

	void clear() {
		println "Resource values cleared"
		valueMap?.clear()
	}

	Set<?> keySet() {
		valueMap?.keySet()
	}

	int size() {
		valueMap?.size()
	}

	def get(key) {
		valueMap?.get(key)
	}

	def <T> T get(String string, Class<T> value) {
		valueMap?.get(string, value)
	}

	def <T> T get(String string, T defaultValue) {
		valueMap?.get(string, defaultValue)
	}

	Set<Map.Entry<String, ?>> entrySet() {
		valueMap?.entrySet()
	}

	void putAll(Map<? extends String, ?> map) {
		println "Adding property map: $map"
		valueMap?.putAll(map)
	}

	def put(String key, value) {
		println "Adding property: $key: $value"
		valueMap?.put(key, value)
	}

	boolean containsKey(key) {
		valueMap?.containsKey(key)
	}

	Collection<?> values() {
		valueMap?.values()
	}

	boolean isEmpty() {
		valueMap?.isEmpty()
	}
}