package com.citytechinc.aem.groovy.console.extension.impl

import com.citytechinc.aem.groovy.console.api.ScriptMetaClassExtensionProvider
import com.citytechinc.aem.groovy.console.builders.ResourceBuilder
import com.citytechinc.aem.groovy.console.helpers.Groover
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.Service
import org.apache.sling.api.SlingHttpServletRequest
import org.apache.sling.api.resource.Resource
import org.apache.sling.api.resource.ResourceNotFoundException

@Service
@Component(immediate = true)
class GrooverScriptMetaClassExtensionProvider implements ScriptMetaClassExtensionProvider {

	private static final Map<String, ?> GROOVY_CONSOLE_LINK_PROPERTY_MAP = ["jcr:mixinTypes"    : "sling:Redirect",
																			"jcr:description"   : "Create and use Groovy Scripts",
																			"jcr:title"         : "Groovy Console",
																			"sling:resourceType": "sling:redirect",
																			"sling:target"      : "/etc/groovyconsole"] as Map<String, ?>

	Closure getScriptMetaClass(SlingHttpServletRequest request) {
		def closure = {
			delegate.getGroover = {
				delegate.groover = delegate.groover ?: new Groover(binding)
				delegate.groover
			}

			delegate.setDryRun = { boolean dryRun ->
				delegate.getGroover().dryRun = dryRun
			}

			delegate.isDryRun = {
				delegate.getGroover().dryRun
			}

			delegate.setSilent = { boolean silent ->
				delegate.getGroover().silent = silent
			}

			delegate.isSilent = {
				delegate.getGroover().silent
			}

			delegate.setActivate = { boolean activate ->
				delegate.getGroover().activate = activate
			}

			delegate.isActivate = {
				delegate.getGroover().activate
			}

			delegate.process = { selectorArg, Closure<Boolean> closure ->
				delegate.getGroover().process(selectorArg, closure)
			}

			delegate.setResourceProperties = { selectorArg, Map<String, ?> propertyMap, boolean updateOnly ->
				delegate.getGroover().setResourceProperties(selectorArg, propertyMap, updateOnly)
			}

			delegate.setResourceProperties = { selectorArg, Map<String, ?> propertyMap ->
				delegate.getGroover().setResourceProperties(selectorArg, propertyMap, false)
			}

			delegate.updateResourceProperties = { selectorArg, Map<String, ?> propertyMap ->
				delegate.getGroover().setResourceProperties(selectorArg, propertyMap, true)
			}

			delegate.removeExistingProperties = { selectorArg, List<String> propertyNames ->
				delegate.getGroover().removeExistingProperties(selectorArg, propertyNames)
			}

			delegate.removeExistingProperties = { selectorArg, Map<String, ?> propertyMap ->
				delegate.getGroover().removeExistingProperties(selectorArg, propertyMap)
			}

			delegate.removeResources = { selectorArg ->
				delegate.getGroover().removeResources selectorArg
			}

			delegate.removeResource = { selectorArg ->
				delegate.getGroover().removeResource selectorArg
			}

			delegate.safeActivate = { Resource resource ->
				delegate.getGroover().activate resource
			}

			delegate.getResourceBuilder = {
				delegate.getGroover()
				new ResourceBuilder(delegate.resourceResolver, delegate.resourceResolver.getResource('/'))
			}

			delegate.getResourceBuilder = { Resource resource ->
				delegate.getGroover()
				new ResourceBuilder(delegate.resourceResolver, resource)
			}

			delegate.getResourceBuilder = { String path ->
				delegate.getGroover()
				def resource = delegate.resourceResolver.getResource(path)
				if (resource) {
					new ResourceBuilder(delegate.resourceResolver, resource)
				} else {
					throw new ResourceNotFoundException("No resource found under path ${path}")
				}
			}

			delegate.addGroovyConsoleLink = {
				delegate.getResourceBuilder("/libs/cq/core/content/welcome/features").groovyconsole GROOVY_CONSOLE_LINK_PROPERTY_MAP
				delegate.resourceResolver.commit()
			}

			delegate.removeGroovyConsoleLink = {
				delegate.setDryRun false
				delegate.removeResource("/libs/cq/core/content/welcome/features/groovyconsole")
				delegate.resourceResolver.commit()
				delegate.setDryRun true
			}

		}

		closure
	}
}
