package eu.etaxonomy.cdm.server.instance;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.eclipse.jetty.plus.jndi.Resource;
import org.eclipse.jetty.server.handler.ContextHandler.Context;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.component.LifeCycle.Listener;
import org.eclipse.jetty.webapp.WebAppContext;


public class CdmInstance implements Listener {

    private static final Logger logger = Logger.getLogger(InstanceManager.class);

    private WebAppContext webAppContext = null;

    private Configuration configuration;

    private List<String> problems;
    private Status status = Status.uninitialized;

    private Resource jndiDataSource;

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

    public boolean onError() {
        return status.equals(Status.error);
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
     * @return true if status is not {@link Status#disabled}
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
     * @return the configuration
     */
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
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
        if(!getStatus().equals(Status.removed)){
            // never override Status.removed !!!
            setStatus(Status.stopping);
        }
    }

    @Override
    public void lifeCycleStopped(LifeCycle event) {
        logger.info("lifeCycleStopped");
        if(!getStatus().equals(Status.removed)){
            // never override Status.removed !!!
            setStatus(Status.stopped);
        }

    }

    @Override
    public void lifeCycleStarting(LifeCycle event) {
        logger.info("lifeCycleStarting");
        setStatus(Status.starting);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void lifeCycleStarted(LifeCycle event) {
        logger.info("lifeCycleStarted");

        List<String> messages = getServletContextAttribute(webAppContext, SharedAttributes.ATTRIBUTE_ERROR_MESSAGES, List.class);
        String dataSourceName = getServletContextAttribute(webAppContext, SharedAttributes.ATTRIBUTE_DATASOURCE_NAME, String.class);

        if (messages != null && dataSourceName != null) {
            // Problems with instance
            Status errorStatus = Status.error;
            for(String message : messages){
                if(message.startsWith("Incompatible version")){
                    errorStatus = Status.incompatible_version;
                    break;
                }
            }
            setStatus(errorStatus);

            getProblems().addAll(messages);

            try {
                logger.warn("Stopping context '" + dataSourceName + "' due to errors reported in ServletContext");
                webAppContext.stop();
                setStatus(errorStatus);
            } catch (Exception e) {
                logger.error(e);
            }
        } else {
            // Instance is OK
            setStatus(Status.started);
        }
    }

    @Override
    public void lifeCycleFailure(LifeCycle event, Throwable cause) {
        logger.error("lifeCycleFailure");
        if(!getStatus().equals(Status.removed)){
            // never override Status.removed !!!
            setStatus(Status.error);
        }
        getProblems().add(cause.getMessage());
    }

    public void releaseWebAppContext() {
        webAppContext = null;
    }

    public boolean bindJndiDataSource() {
        try {
            Class<DataSource> dsCass = (Class<DataSource>) Thread.currentThread().getContextClassLoader().loadClass("com.mchange.v2.c3p0.ComboPooledDataSource");
            DataSource datasource = dsCass.newInstance();
            dsCass.getMethod("setDriverClass", new Class[] {String.class}).invoke(datasource, new Object[] {configuration.getDriverClass()});
            dsCass.getMethod("setJdbcUrl", new Class[] {String.class}).invoke(datasource, new Object[] {configuration.getDataSourceUrl()});
            dsCass.getMethod("setUser", new Class[] {String.class}).invoke(datasource, new Object[] {configuration.getUsername()});
            dsCass.getMethod("setPassword", new Class[] {String.class}).invoke(datasource, new Object[] {configuration.getPassword()});

            Connection connection = null;
            String sqlerror = null;
            try {
                connection = datasource.getConnection();
                connection.close();
            } catch (SQLException e) {
                sqlerror = e.getMessage() + "["+ e.getSQLState() + "]";
                getProblems().add(sqlerror);
                setStatus(Status.error);
                if(connection !=  null){
                    try {connection.close();} catch (SQLException e1) { /* IGNORE */ }
                }
                logger.error(configuration.toString() + " has problem : "+ sqlerror );
            }

            if(!onError()){
                logger.info("binding jndi datasource at " + configuration.getJdbcJndiName() + " with " + configuration.getUsername() +"@"+ configuration.getDataSourceUrl());
                jndiDataSource = new Resource(configuration.getJdbcJndiName(), datasource);
                return true;
            }

        } catch (IllegalArgumentException e) {
            logger.error(e);
            e.printStackTrace();
        } catch (SecurityException e) {
            logger.error(e);
        } catch (ClassNotFoundException e) {
            logger.error(e);
        } catch (InstantiationException e) {
            logger.error(e);
        } catch (IllegalAccessException e) {
            logger.error(e);
        } catch (InvocationTargetException e) {
            logger.error(e);
        } catch (NoSuchMethodException e) {
            logger.error(e);
        } catch (NamingException e) {
            logger.error(e);
        }
        return false;
    }

    public void unbindJndiDataSource() {
        if(jndiDataSource != null){
            jndiDataSource.release();
            jndiDataSource = null;
        }
    }

}
