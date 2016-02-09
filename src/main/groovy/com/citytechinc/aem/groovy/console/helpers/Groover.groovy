package com.citytechinc.aem.groovy.console.helpers

import com.day.cq.replication.ReplicationActionType
import com.day.cq.replication.ReplicationStatus
import com.day.cq.replication.Replicator
import com.day.cq.wcm.api.NameConstants
import com.day.cq.wcm.api.Page
import com.day.cq.wcm.api.PageManager
import org.apache.sling.api.resource.ModifiableValueMap
import org.apache.sling.api.resource.Resource
import org.apache.sling.api.resource.ResourceResolver
import org.osgi.framework.BundleContext

import javax.jcr.Session

class Groover {

	private static final String MSG_DRY_RUN = "DRY_RUN: Changes reverted"

	private static final String MSG_LIVE_RUN = "LIVE_RUN: Changes saved"

	private static final String MSG_ACTIVATED = "Changes activated"

	private static final String MSG_NOT_ACTIVATED = "Changes not activated"

	private static final List<String> REQUIRED_BINDINGS = ["resourceResolver", "out", "session", "pageManager", "bundleContext"]

	public static ThreadLocal<Closure> THREAD_LOCAL_LOG = new ThreadLocal<Closure>()

	private Closure println

	private ResourceResolver resourceResolver

	private String userID

	private PageManager pageManager

	private Session session

	private PrintStream out

	private ClosureProcessor closureProcessor

	private PropertyWriter propertyWriter

	private ResourceEraser resourceEraser

	private BundleContext bundleContext

	private Replicator replicator

	private Boolean dryRun = true

	private Boolean activate = false

	private silent = false

	/**
	 * Constructor for Groover helper class
	 * @param binding Binding containing at least these values:
	 * <ul>
	 * <li>ResourceResolver resourceResolver</li>
	 * <li>PrintStream out</li>
	 * <li>Session session </li>
	 * <li>PageManager pageManager</li>
	 * <li>BundleContext bundleContext</li>
	 * </ul>
	 */
	def Groover(Binding binding) {
		def variables = binding.getVariables()
		REQUIRED_BINDINGS.each {
			this[it] = variables[it]
		}
		THREAD_LOCAL_LOG.set({param -> if (!silent) out.println param})
		println = THREAD_LOCAL_LOG.get()
		userID = "groover(${resourceResolver.getUserID()})".toString()
		closureProcessor = new ClosureProcessor(resourceResolver)
		propertyWriter = new PropertyWriter(resourceResolver)
		resourceEraser = new ResourceEraser(resourceResolver)
		replicator = bundleContext.getService(bundleContext.getServiceReference(Replicator.class))
	}

	/**
	 * Runs the closure for every matched resource.
	 * The resource selector can be one of the following:
	 * <ul>
	 * <li>String query - a JCR_SQL2 query string</li>
	 * <li>SlingQuery slingQuery - a SlingQuery object</li>
	 * <li>List<String> paths - a list of absolute paths to resources</li>
	 * <li>Iterator<Resource> iterator - an iterator over a collection of resources</li>
	 * </ul>
	 * @param selectorArg The resource selector
	 * @param closure Closure that takes the resource and it's own valueMap as arguments
	 * @return List of modified resources
	 */
	List<Resource> process(selectorArg, Closure<Boolean> closure) {
		List<Resource> resources = closureProcessor.process(selectorArg, closure)
		finishProcessing(resources)
	}

	/**
	 * Applies the map of properties to every matched resource. If updateOnly is true, then this method won't create new properties.
	 * The resource selector can be one of the following:
	 * <ul>
	 * <li>String query - a JCR_SQL2 query string</li>
	 * <li>SlingQuery slingQuery - a SlingQuery object</li>
	 * <li>List<String> paths - a list of absolute paths to resources</li>
	 * <li>Iterator<Resource> iterator - an iterator over a collection of resources</li>
	 * </ul>
	 * @param selectorArg The resource selector
	 * @param propertyMap Map of properties to be applied to every matched resource
	 * @param updateOnly A flag guarding whether new properties will be created by this method
	 * @return List of modified resources
	 */
	List<Resource> setResourceProperties(selectorArg, Map<String, ?> propertyMap, boolean updateOnly) {
		List<Resource> resources = propertyWriter.setResourceProperties(selectorArg, propertyMap, updateOnly)
		finishProcessing(resources)
	}

	/**
	 * Removes properties from matched resources if their values match those provided within a map.
	 * The resource selector can be one of the following:
	 * <ul>
	 * <li>String query - a JCR_SQL2 query string</li>
	 * <li>SlingQuery slingQuery - a SlingQuery object</li>
	 * <li>List<String> paths - a list of absolute paths to resources</li>
	 * <li>Iterator<Resource> iterator - an iterator over a collection of resources</li>
	 * </ul>
	 * @param selectorArg The resource selector
	 * @param propertyMap A "property_name":"property_value" map used to check whether a property should be removed from resource
	 * @return List of modified resources
	 */
	List<Resource> removeExistingProperties(selectorArg, Collection<String> propertyNames) {
		List<Resource> resources = propertyWriter.removeExistingProperties(selectorArg, propertyNames)
		finishProcessing(resources)
	}

	/**
	 * Removes properties from matched resources if their values match those provided within a map.
	 * The resource selector can be one of the following:
	 * <ul>
	 * <li>String query - a JCR_SQL2 query string</li>
	 * <li>SlingQuery slingQuery - a SlingQuery object</li>
	 * <li>List<String> paths - a list of absolute paths to resources</li>
	 * <li>Iterator<Resource> iterator - an iterator over a collection of resources</li>
	 * </ul>
	 * @param selectorArg The resource selector
	 * @param propertyMap A "property_name":"property_value" map used to check whether a property should be removed from resource
	 * @return List of modified resources
	 */
	List<Resource> removeExistingProperties(selectorArg, Map<String, ?> propertyMap) {
		List<Resource> resources = propertyWriter.removeExistingProperties(selectorArg, propertyMap)
		finishProcessing resources
	}

	/**
	 * Removes the matched resources.
	 * The resource selector can be one of the following:
	 * <ul>
	 * <li>String query - a JCR_SQL2 query string</li>
	 * <li>SlingQuery slingQuery - a SlingQuery object</li>
	 * <li>List<String> paths - a list of absolute paths to resources</li>
	 * <li>Iterator<Resource> iterator - an iterator over a collection of resources</li>
	 * </ul>
	 * @param selectorArg The resource selector
	 * @return List of modified resources
	 */
	List<Resource> removeResources(selectorArg) {
		List<Resource> parents = resourceEraser.removeResources(selectorArg) as List<Resource>
		finishProcessing parents
	}

	/**
	 * Removes the resource.
	 * @param resource Resource to be deleted, provided as either Resource object or target resource's path as String
	 * @return Parent of the removed resource
	 */
	Resource removeResource(resource) {
		Resource parent = resourceEraser.removeResource(resource)
		finishProcessing([parent] as List<Resource>)[0]
	}

	/**
	 * Activates the page containing the given resource (so long as there haven't been any author-made changes on that page since last activation)
	 * @param resource
	 * @return Activated resource or null if activation couldn't be completed
	 */
	Resource activate(Resource resource) {
		if (!resource) {
			println  "Cannot activate a null resource"
			return
		}
		ModifiableValueMap valueMap = resource.adaptTo ModifiableValueMap.class
		Calendar lastModified = valueMap.get(NameConstants.PN_PAGE_LAST_MOD, Calendar.class)

		Page page = pageManager.getContainingPage(resource)
		ReplicationStatus status = page?.adaptTo(ReplicationStatus.class)
		Calendar lastPublished = status?.getLastPublished()

		if (lastModified && lastPublished && status?.isActivated()) {
			if (lastModified.compareTo(lastPublished) <= 0) {
				replicator.replicate(session, ReplicationActionType.ACTIVATE, page.getPath())
				println "Page ${page.getPath()} activated"
				return resource
			} else {
				println "Resource ${page.getPath()} won't be activated: last aem modification date is later than publication date"
				return
			}
		}
	}

	private void updateModificationStatus(Resource resource, Calendar now) {
		if (!resource){
			return
		}
		if (resource.get("jcr:primaryType") == "cq:PageContent") {
			resource.set(["cq:lastModified":now,"cq:lastModifiedBy":userID])
		} else {
			if (resource.hasProperty("jcr:lastModified") || resource.hasProperty("jcr:lastModifiedBy")){
				resource.set(["jcr:lastModified":now,"jcr:lastModifiedBy":userID])
			}
			def parent = resource.getParent()
			if (parent) {
				updateModificationStatus(parent, now)
			}
		}
	}

	private List<Resource> finishProcessing(List<Resource> resourceList) {
		if (!resourceList) {
			return
		}
		if (dryRun) {
			resourceResolver.revert()
			println MSG_DRY_RUN
		} else {
			def now = new GregorianCalendar()
			resourceList.each {updateModificationStatus it, now}
			resourceResolver.commit()
			println MSG_LIVE_RUN
			if (activate) {
				resourceList.each {activate it}
				println MSG_ACTIVATED
			} else {
				println MSG_NOT_ACTIVATED
			}
		}
		resourceList
	}
}
