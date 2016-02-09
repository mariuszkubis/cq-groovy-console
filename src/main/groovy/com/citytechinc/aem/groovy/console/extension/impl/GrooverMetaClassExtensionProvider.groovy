package com.citytechinc.aem.groovy.console.extension.impl

import com.citytechinc.aem.groovy.console.builders.ResourceBuilder
import com.citytechinc.aem.groovy.console.decorators.ValueMapDecorator
import com.citytechinc.aem.groovy.extension.api.MetaClassExtensionProvider
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.Service
import org.apache.sling.api.resource.Resource

@Service
@Component(immediate = true)
class GrooverMetaClassExtensionProvider implements MetaClassExtensionProvider{

	private static final Closure RESOURCE_METACLASS = {
		delegate."retrievedValueMap" = null
		delegate."builder" = null

		getValueMap {
			delegate.retrievedValueMap = delegate.retrievedValueMap ?: new ValueMapDecorator(delegate)
			delegate.retrievedValueMap
		}

		iterator { delegate.getChildren() }

		recurse { Closure closure ->
			closure(delegate)
			delegate.getChildren().each { resource ->
				resource.recurse(closure)
			}
		}

		get { String propertyName ->
			delegate.getValueMap().get(propertyName)
		}

		set { String propertyName, value ->
			delegate.getValueMap().put(propertyName, value)
		}

		set { Map<String, ?> propertyMap ->
			delegate.getValueMap().putAll(propertyMap)
		}

		update { String propertyName, value ->
			def valueMap = delegate.getValueMap()
			valueMap.containsKey(propertyName) ? valueMap.put(propertyName, value) : false
		}

		getBuilder {
			new ResourceBuilder(delegate.resourceResolver, delegate)
		}

		getOrAddChild { String childName ->
			delegate.getChild(childName)?:delegate.getResourceResolver().create(delegate, childName, [:] as Map<String, ?>)
		}

		getOrAddChild { String childName, String primaryNodeTypeName ->
			delegate.getChild(childName)?:delegate.getResourceResolver().create(delegate, childName, ["jcr:primaryType" : primaryNodeTypeName] as Map<String, ?>)
		}

		remove {
			delegate.getResourceResolver().delete(delegate)
		}

		removeChild { String childName ->
			delegate.getResourceResolver().delete(delegate.getChild(childName))
		}

		hasProperty { String propertyName ->
			delegate.getValueMap().containsKey(propertyName)
		}
	}

	Map<Class, Closure> getMetaClasses(){
		[(Resource) : RESOURCE_METACLASS]
	}
}
