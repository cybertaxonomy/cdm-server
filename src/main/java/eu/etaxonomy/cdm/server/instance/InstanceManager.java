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

	boolean serverIsRunning = false;

	private final File datasourcesFile;
	private ListOrderedMap instances = new ListOrderedMap();

	private final boolean austostart = true;

	public InstanceManager(File configurationFile) {
		this.datasourcesFile = configurationFile;
	}

	/**
	 * this list of instances may contain removed
	 * instances.
	 * {@link #numOfConfiguredInstances()}
	 * @return the instances
	 */
	@SuppressWarnings("unchecked")
	public List<CdmInstance> getInstances() {
		return instances.valueList();
	}

	/**
	 * @return the number of existing instances, former instances which have been
	 * removed are not counted
	 */
	public int numOfConfiguredInstances(){
		int cnt=0;
		for(CdmInstance instance : getInstances()){
			if(instance.getStatus().equals(Status.removed)){
				continue;
			}
			cnt++;
		}
		return cnt;
	}

	/**
	 * loads and reloads the list of instances.
	 * After loading the configuration the required memory is checked
	 * <p>
	 * reload behavior:
	 * <ol>
	 * <li>newly added instances are created but are not started automatically</li>
	 * <li>removed instances are stopped, configuration and context are removed,
	 * state is set to Status.removed to indicate removal, removed instances can
	 * be re-added.</li>
	 * <li>the order of instances as defined in the config file is always retained
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
			if(currentInstances.containsKey(key)){
				CdmInstance existingInstance = (CdmInstance)currentInstances.get(key);
				// already removed instances will not be re-added if they have been stopped successfully
				if(existingInstance.getStatus().equals(Status.removed) && existingInstance.getWebAppContext().isStopped()){
					updatedInstances.put(key, existingInstance);
					if ( !(existingInstance).getConfiguration().equals(config)) {
						// instance has changed: stop it
						try {
							// TODO change problems into messages + severity (notice, error)
							existingInstance.getWebAppContext().stop();
							existingInstance.getProblems().add("Reloaded with modified configuration and thus stopped");
						} catch (Exception e) {
							existingInstance.getProblems().add("Error while stopping modified instance: " + e.getMessage());
							logger.error(e, e);
						}
					}
				}
			} else {
				updatedInstances.put(key, new CdmInstance(config));
			}
		}

		// find removed instances
		for(Object keyOfExisting : currentInstances.keyList()){
			if(!updatedInstances.containsKey(keyOfExisting)){
				CdmInstance removedInstance = (CdmInstance)currentInstances.get(keyOfExisting);
				updatedInstances.put(keyOfExisting, removedInstance);
				try {
					removedInstance.setStatus(Status.removed);
					removedInstance.getWebAppContext().stop();
					removedInstance.getProblems().add("Removed from configuration and thus stopped");
				} catch (Exception e) {
					removedInstance.getProblems().add("Error while stopping removed instance: " + e.getMessage());
					logger.error(e, e);
				}
			}
		}

		instances = updatedInstances;

        verifyMemoryRequirements();

        if(serverIsRunning) {
        	addInstancesToServer(false);
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
        	 addInstancesToServer(austostart);

    }

	private void addInstancesToServer(boolean austostart) {
		for (CdmInstance instance : getInstances()) {
			if (!instance.getStatus().equals(Status.removed)) {
				try {
					Bootloader.getBootloader().addCdmInstanceContext(instance);
				} catch (IOException e) {
					logger.error(e, e); // TODO better throw?
				}

				if (austostart) {
					try {
						instance.setStatus(Status.starting);
						instance.getWebAppContext().start();
						if (!instance.getStatus().equals(Status.error)) {
							instance.setStatus(Status.started);
						}
					} catch (Exception e) {
						logger.error("Could not start " + instance.getWebAppContext().getContextPath());
						instance.getProblems().add(e.getMessage());
						instance.setStatus(Status.error);
					}
				}
			}
		}
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

       verifyMemoryRequirement("PermGenSpace", PERM_GEN_SPACE_CDMSERVER, PERM_GEN_SPACE_PER_INSTANCE, JvmManager.getPermGenSpaceUsage().getMax());
       verifyMemoryRequirement("HeapSpace", HEAP_CDMSERVER, HEAP_PER_INSTANCE, JvmManager.getHeapMemoryUsage().getMax());

   }
    private void verifyMemoryRequirement(String memoryName, long requiredSpaceServer, long requiredSpacePerInstance, long availableSpace) {


        long recommendedMinimumSpace = recommendedMinimumSpace(requiredSpaceServer, requiredSpacePerInstance, null);

        if(recommendedMinimumSpace > availableSpace){

            String message = memoryName + " ("
                + (availableSpace / MB)
                + "MB) insufficient for "
                + numOfConfiguredInstances()
                + " instances. Increase " + memoryName + " to "
                + (recommendedMinimumSpace / MB)
                + "MB";
                ;
            logger.error(message + " => disabling some instances!!!");

            // disabling some instances
            int i=0;
            for(CdmInstance instance : getInstances()){
                i++;
                if(recommendedMinimumSpace(requiredSpaceServer, requiredSpacePerInstance, i)  > availableSpace){
                    instance.setStatus(Status.disabled);
                    instance.getProblems().add("Disabled due to: " + message);
                }
            }
        }
    }

    /**
     * @param requiredServerSpace
     * @param requiredSpacePerIntance
     * @param numOfInstances may be null, the total number of instances found in
     *  the current configuration is used in this case.
     * @return
     */
    public long recommendedMinimumSpace(long requiredServerSpace, long requiredSpacePerIntance, Integer numOfInstances) {
        if(numOfInstances == null){
            numOfInstances = numOfConfiguredInstances();
        }
        return (numOfInstances * requiredSpacePerIntance) + requiredServerSpace;
    }



}
