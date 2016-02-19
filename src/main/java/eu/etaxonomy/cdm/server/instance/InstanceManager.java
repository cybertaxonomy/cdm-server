/**
 * Copyright (C) 2013 EDIT
 * European Distributed Institute of Taxonomy
 * http://www.e-taxonomy.eu
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * See LICENSE.TXT at the top of this package for the full license terms.
 */
package eu.etaxonomy.cdm.server.instance;

import static eu.etaxonomy.cdm.server.AssumedMemoryRequirements.HEAP_CDMSERVER;
import static eu.etaxonomy.cdm.server.AssumedMemoryRequirements.HEAP_PER_INSTANCE;
import static eu.etaxonomy.cdm.server.AssumedMemoryRequirements.MB;
import static eu.etaxonomy.cdm.server.AssumedMemoryRequirements.PERM_GEN_SPACE_CDMSERVER;
import static eu.etaxonomy.cdm.server.AssumedMemoryRequirements.PERM_GEN_SPACE_PER_INSTANCE;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.log4j.Logger;
import org.eclipse.jetty.util.component.LifeCycle;

import eu.etaxonomy.cdm.server.Bootloader;
import eu.etaxonomy.cdm.server.JvmManager;

/**
 * Manager to load / reload list of instances the instance manager holds the
 * list of {@link CdmInstance}, the list can be updated, the instances are
 * identified by the bean id map<String,CdmInstance>
 *
 * The instance list is initially empty, all instances are usually started after
 * loading the list, see {@#loadDataSources()}
 *
 * @author a.kohlbecker
 * @date May 10, 2013
 */
public class InstanceManager implements LifeCycle.Listener {

    private static final Logger logger = Logger.getLogger(InstanceManager.class);

    private ListOrderedMap instances = new ListOrderedMap();

    private final StartupQueue queue = new StartupQueue();

    private final boolean austostart = true;

    boolean serverIsRunning = false;

    private File datasourcesFile;

    /**
     * @return the datasourcesFile
     */
    public File getDatasourcesFile() {
        return datasourcesFile;
    }

    /**
     * @param datasourcesFile
     *            the datasourcesFile to set
     */
    public void setDatasourcesFile(File datasourcesFile) {
        this.datasourcesFile = datasourcesFile;
    }

    public InstanceManager(File configurationFile) {
        this.datasourcesFile = configurationFile;
        queue.setParallelStartUps(JvmManager.availableProcessors());
    }

    /**
     * @return the {@link Bootloader} singelton instance
     */
    private Bootloader bootloader() {
        return Bootloader.getBootloader();
    }

    /**
     * this list of instances may contain removed instances.
     * {@link #numOfConfiguredInstances()}
     *
     * @return the instances
     */
    @SuppressWarnings("unchecked")
    public List<CdmInstance> getInstances() {
        return instances.valueList();
    }

    public CdmInstance getInstance(String instanceName) {
        return (CdmInstance) instances.get(instanceName);
    }

    /**
     * Starts the instance
     *
     * Rebinds the JndiDataSource and starts the given instance. The method
     * returns once the instance is fully started up.
     *
     * @param instance
     * @throws Exception
     */
    public void start(CdmInstance instance) {
        if (instance.getWebAppContext() != null) {
            // instance.unbindJndiDataSource();
            // instance.bindJndiDataSource();
            if (!instance.bindJndiDataSource()) {
                // a problem with the datasource occurred skip this webapp
                // cdmWebappContext = null;
                logger.error("a problem with the datasource occurred -> aboarding startup of /" + instance.getName());
                instance.setStatus(Status.error);
                // return cdmWebappContext;
            }
            if (logger.isDebugEnabled()) {
                logger.debug("starting " + instance.getName());
            }
            // instance.getWebAppContext().start();
            queue.add(instance);
        }
    }

    public void stop(CdmInstance instance) throws Exception {
        // TODO do we need to
        // bootloader().removeCdmInstanceContext(existingInstance); always?
        // see reLoadInstanceConfigurations()
        if (instance.getWebAppContext() != null) {
            instance.getWebAppContext().stop();
        }
        instance.unbindJndiDataSource();
        instance.getProblems().clear();
        // explicitly set status stopped here to clear up prior error states
        instance.setStatus(Status.stopped);
    }

    /**
     * @return the number of existing instances, former instances which have
     *         been removed are not counted
     */
    public int numOfConfiguredInstances() {
        int cnt = 0;
        for (CdmInstance instance : getInstances()) {
            if (instance.getStatus().equals(Status.removed)) {
                continue;
            }
            cnt++;
        }
        return cnt;
    }

    /**
     * loads and reloads the list of instances. After loading the configuration
     * the required memory is checked
     * <p>
     * reload behavior:
     * <ol>
     * <li>newly added instances are created but are not started automatically</li>
     * <li>removed instances are stopped, configuration and context are removed,
     * state is set to Status.removed to indicate removal, removed instances can
     * be re-added.</li>
     * <li>the order of instances as defined in the config file is always
     * retained
     * </ol>
     *
     */
    synchronized public void reLoadInstanceConfigurations() {

        ListOrderedMap currentInstances = instances;
        ListOrderedMap updatedInstances = new ListOrderedMap();

        List<Configuration> configList = DataSourcePropertyParser.parseDataSourceConfigs(datasourcesFile);
        logger.info("cdm server instance names loaded: " + configList.toString());

        for (Configuration config : configList) {
            String key = config.getInstanceName();
            if (currentInstances.containsKey(key)) {
                CdmInstance existingInstance = (CdmInstance) currentInstances.get(key);
                if (!(existingInstance.getStatus().equals(Status.removed) && existingInstance.getWebAppContext() == null)) {
                    // re-added instance if not already removed (removed
                    // instances will not be re-added if they have been stopped
                    // successfully)
                    updatedInstances.put(key, existingInstance);
                    if (!(existingInstance).getConfiguration().equals(config)) {
                        // instance has changed: stop it, clear error states,
                        // set new configuration
                        try {
                            // TODO change problems into messages + severity
                            // (notice, error)
                            stop(existingInstance);
                            bootloader().removeCdmInstanceContext(existingInstance);
                            existingInstance.setConfiguration(config);
                            existingInstance.getProblems().add("Reloaded with modified configuration");
                        } catch (Exception e) {
                            existingInstance.getProblems().add(
                                    "Error while stopping modified instance: " + e.getMessage());
                            logger.error(e, e);
                        }
                    }
                }
            } else {
                // create and add a new instance
                updatedInstances.put(key, new CdmInstance(config));
            }
        }

        // find removed instances
        for (Object keyOfExisting : currentInstances.keyList()) {
            if (!updatedInstances.containsKey(keyOfExisting)) {
                CdmInstance removedInstance = (CdmInstance) currentInstances.get(keyOfExisting);

                if (removedInstance.getStatus().equals(Status.removed)) {
                    // instance already is removed, forget it now
                    continue;
                }

                // remove the instance but remember it until next config reload
                updatedInstances.put(keyOfExisting, removedInstance);
                removedInstance.setStatus(Status.removed);
                removedInstance.getProblems().add("Removed from configuration and thus stopped");
                try {
                    bootloader().removeCdmInstanceContext(removedInstance);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }

            }
        }

        instances = updatedInstances;

        verifyMemoryRequirements();

        if (serverIsRunning) {
            addNewInstancesToServer(false);
        }
    }

    private void addNewInstancesToServer(boolean austostart) {
        for (CdmInstance instance : getInstances()) {
            if (instance.getStatus().equals(Status.uninitialized)) {
                try {
                    if (bootloader().addCdmInstanceContext(instance) != null && austostart) {
                        try {
                            start(instance);
                        } catch (Exception e) {
                            logger.error("Could not start " + instance.getWebAppContext().getContextPath(), e);
                            instance.getProblems().add(e.getMessage());
                            instance.setStatus(Status.error);
                        }
                    }
                } catch (IOException e) {
                    logger.error(e, e); // TODO better throw?
                }
            }
        }
    }

    @Override
    public void lifeCycleFailure(LifeCycle event, Throwable cause) {
        logger.error("Jetty LifeCycleFailure", cause);
    }

    @Override
    public void lifeCycleStarted(LifeCycle event) {
        serverIsRunning = true;
        logger.info("cdmserver has started, now adding CDM server contexts");
        addNewInstancesToServer(austostart);

    }

    @Override
    public void lifeCycleStarting(LifeCycle event) {
    }

    @Override
    public void lifeCycleStopped(LifeCycle event) {
        serverIsRunning = false;
    }

    @Override
    public void lifeCycleStopping(LifeCycle event) {
        serverIsRunning = false;
    }

    /**
    *
    */
    private void verifyMemoryRequirements() {

        verifyMemoryRequirement("HeapSpace", HEAP_CDMSERVER, HEAP_PER_INSTANCE, JvmManager.getHeapMemoryUsage()
                .getMax());
        if (JvmManager.getJvmVersion() == 7) {
            verifyMemoryRequirement("PermGenSpace", PERM_GEN_SPACE_CDMSERVER, PERM_GEN_SPACE_PER_INSTANCE, JvmManager
                    .getPermGenSpaceUsage().getMax());
        }

    }

    private void verifyMemoryRequirement(String memoryName, long requiredSpaceServer, long requiredSpacePerInstance,
            long availableSpace) {

        long recommendedMinimumSpace = recommendedMinimumSpace(requiredSpaceServer, requiredSpacePerInstance, null);

        if (recommendedMinimumSpace > availableSpace) {

            String message = memoryName + " (" + (availableSpace / MB) + "MB) insufficient for "
                    + numOfConfiguredInstances() + " instances. Increase " + memoryName + " to "
                    + (recommendedMinimumSpace / MB) + "MB";
            ;
            logger.error(message + " => disabling some instances!!!");

            // disabling some instances
            int i = 0;
            for (CdmInstance instance : getInstances()) {
                i++;
                if (recommendedMinimumSpace(requiredSpaceServer, requiredSpacePerInstance, i) > availableSpace) {
                    instance.setStatus(Status.disabled);
                    instance.getProblems().add("Disabled due to: " + message);
                }
            }
        }
    }

    /**
     * @param requiredServerSpace
     * @param requiredSpacePerIntance
     * @param numOfInstances
     *            may be null, the total number of instances found in the
     *            current configuration is used in this case.
     * @return
     */
    public long recommendedMinimumSpace(long requiredServerSpace, long requiredSpacePerIntance, Integer numOfInstances) {
        if (numOfInstances == null) {
            numOfInstances = numOfConfiguredInstances();
        }
        return (numOfInstances * requiredSpacePerIntance) + requiredServerSpace;
    }

}
