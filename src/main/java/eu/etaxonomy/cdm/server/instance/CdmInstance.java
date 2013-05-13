package eu.etaxonomy.cdm.server.instance;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.handler.ContextHandler.Context;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.component.LifeCycle.Listener;
import org.eclipse.jetty.webapp.WebAppContext;

import eu.etaxonomy.cdm.server.Bootloader;

public class CdmInstance implements Listener {

	private static final Logger logger = Logger.getLogger(InstanceManager.class);

	private WebAppContext webAppContext = null;

	private final Configuration configuration;

	private List<String> problems;
	private Status status = Status.uninitialized;

	public CdmInstance(Configuration configuration) {
		this.configuration = configuration;
	}

	public List<String> getProblems() {
		if (problems == null) {
			problems = new ArrayList<String>();
		}
		return problems;
	}

	public void setProblems(List<String> problems) {
		this.problems = problems;
	}

	public boolean hasProblems() {
		return getProblems().size() > 0;
	}

	/**
	 * @param status
	 *            the status to set
	 */
	public void setStatus(Status status) {
		this.status = status;
	}

	/**
	 * @return the status
	 */
	public Status getStatus() {
		return status;
	}

	/**
	 * @return the enabled
	 */
	public boolean isEnabled() {
		return !status.equals(Status.disabled);
	}

	/**
	 * @return the configuration
	 */
	public Configuration getConfiguration() {
		return configuration;
	}

	/**
	 * @return the webAppContext
	 */
	public WebAppContext getWebAppContext() {
		return webAppContext;
	}

	/**
	 * @return the webAppContext
	 */
	public void setWebAppContext(WebAppContext webAppContext) {
		this.webAppContext = webAppContext;
	}

	/**
	 * @param <T>
	 * @param webAppContext
	 * @param attributeName
	 * @param type
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <T> T getServletContextAttribute(WebAppContext webAppContext, String attributeName, Class<T> type) {

		Context servletContext = webAppContext.getServletContext();
		Object value = servletContext.getAttribute(attributeName);
		if (value != null && type.isAssignableFrom(value.getClass())) {

		}
		return (T) value;
	}

	@Override
	public void lifeCycleStopping(LifeCycle event) {
		logger.info("lifeCycleStopping");
	}

	@Override
	public void lifeCycleStopped(LifeCycle event) {
		logger.info("lifeCycleStopped");

	}

	@Override
	public void lifeCycleStarting(LifeCycle event) {
		logger.info("lifeCycleStarting");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void lifeCycleStarted(LifeCycle event) {
		logger.info("lifeCycleStarted");

		List<String> messages = getServletContextAttribute(webAppContext, Bootloader.ATTRIBUTE_ERROR_MESSAGES, List.class);
		String dataSourceName = getServletContextAttribute(webAppContext, Bootloader.ATTRIBUTE_DATASOURCE_NAME, String.class);

		if (messages != null && dataSourceName != null) {

			getProblems().addAll(messages);
			setStatus(Status.error);
			try {
				logger.warn("Stopping context '" + dataSourceName + "' due to errors reported in ServletContext");
				webAppContext.stop();
			} catch (Exception e) {
				logger.error(e);
			}
		}
	}

	@Override
	public void lifeCycleFailure(LifeCycle event, Throwable cause) {
		logger.error("lifeCycleFailure");
	}

}
