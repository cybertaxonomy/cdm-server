/**
 * Copyright (C) 2015 EDIT
 * European Distributed Institute of Taxonomy
 * http://www.e-taxonomy.eu
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * See LICENSE.TXT at the top of this package for the full license terms.
 */
package eu.etaxonomy.cdm.server.instance;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.component.LifeCycle.Listener;
import org.slf4j.MDC;

import eu.etaxonomy.cdm.server.logging.InstanceLogWrapper;

/**
 * @author a.kohlbecker
 * @date Jul 29, 2015
 */
public class StartupQueue extends LinkedList<CdmInstance> {

    private static final long serialVersionUID = -8173521573512154767L;

    private static final Logger logger = LogManager.getLogger();

    private Set<CdmInstance> instancesStartingUp = new HashSet<>();

    private int parallelStartUps = 1;

    public int getParallelStartUps() {
        return parallelStartUps;
    }
    public void setParallelStartUps(int parallelStartUps) {
        this.parallelStartUps = parallelStartUps;
    }

    @Override
    public boolean add(CdmInstance e) {
        boolean result = super.add(e);
        registerAt(e);
        return result;
    }

    @Override
    public void addFirst(CdmInstance e) {
        super.addFirst(e);
        registerAt(e);
    }

    @Override
    public void addLast(CdmInstance e) {
        super.addLast(e);
        registerAt(e);
    }

    protected void notifyInstanceStartedUp(CdmInstance instance) {
        logger.debug("received message that instance " + instance.getName() + " has started up.");
        instancesStartingUp.remove(instance);
        startNextInstances();
    }

    protected void notifyInstanceFailed(CdmInstance instance) {
        logger.debug("received message that instance " + instance.getName() + " has failed.");
        instancesStartingUp.remove(instance);
        startNextInstances();
    }

    private void startNextInstances() {
        logger.debug("startNextInstances()");
        while(instancesStartingUp.size() < parallelStartUps && !isEmpty()) {
            CdmInstance nextInstance = pop();
            instancesStartingUp.add(nextInstance);
            logger.debug("Starting instance " + nextInstance.getName() + " in new thread.");
            Thread t = new StartupThread(nextInstance);
            t.start();
        }
    }

    @SuppressWarnings("unused")
    private void registerAt(CdmInstance e) {
        new InstanceListener(e);
        startNextInstances();

    }

    class InstanceListener implements Listener {

        private CdmInstance instance;

        InstanceListener(CdmInstance instance) {
            this.instance = instance;
            instance.getWebAppContext().addLifeCycleListener(this);
        }

        @Override
        public void lifeCycleStarting(LifeCycle event) {
            // IGNORE
        }

        @Override
        public void lifeCycleStarted(LifeCycle event) {
            notifyInstanceStartedUp(instance);
            // release reference to the instance so
            // that the thread can be garbage collected
            instance.getWebAppContext().removeLifeCycleListener(this);
            instance = null;
        }

        @Override
        public void lifeCycleFailure(LifeCycle event, Throwable cause) {
            notifyInstanceFailed(instance);
            // release reference to the instance so
            // that the thread can be garbage collected
            instance.getWebAppContext().removeLifeCycleListener(this);
            instance = null;
        }

        @Override
        public void lifeCycleStopping(LifeCycle event) {
            // IGNORE
        }

        @Override
        public void lifeCycleStopped(LifeCycle event) {
            // IGNORE
        }
    }

    class StartupThread extends Thread{

        private final Logger logger = LogManager.getLogger();

        private CdmInstance instance;

        StartupThread(CdmInstance instance){
            this.instance = instance;
        }

        @Override
        public void run() {
            try {
                MDC.put(InstanceLogWrapper.CDM_INSTANCE, instance.getName());

                instance.getWebAppContext().setThrowUnavailableOnStartupException(true);
                instance.getWebAppContext().start();
                // release reference to the instance so
                // that the thread can be garbage collected
                instance = null;
            } catch(InterruptedException e) {
                try {
                    instance.getWebAppContext().stop();
                } catch (Exception e1) {
                    logger.error("Error on stopping instance", e1);
                    notifyInstanceFailed(instance);
                }
            } catch (Throwable e) {
                logger.error("Could not start " + instance.getWebAppContext().getContextPath(), e);
                instance.getProblems().add(e.getMessage());
                instance.setStatus(Status.error);
                notifyInstanceFailed(instance);
                try {
                    // try to stop
                    instance.getWebAppContext().stop();
                } catch (Exception e1) {
                    /* IGNORE */
                }
            } finally {
                MDC.clear();
            }
        }
    }
}