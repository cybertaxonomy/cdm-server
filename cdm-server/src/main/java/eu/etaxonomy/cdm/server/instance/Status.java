package eu.etaxonomy.cdm.server.instance;

import org.eclipse.jetty.util.component.LifeCycle;

public enum Status{
    /**
     * New instances are uninitialized
     */
    uninitialized,
    /**
     * Instance is being configured and added to the server
     */
    initializing,
    /**
     * Instance is starting up, see {@link LifeCycle#isStarting()}
     */
    starting,
    /**
     * Instance is started up, see {@link LifeCycle#isStarted()}
     */
    started,
    /**
     * Instance is stopped, see {@link LifeCycle#isStopped()}
     */
    stopped,
    /**
     * Instance is stopping, see {@link LifeCycle#isStopping()}
     */
    stopping,
    /**
     * An error occurred either during initialization, starting up or while running.
     */
    error,
    /**
     * Instance has been disabled due to memory limitations
     */
    disabled,
    /**
     * The instance has been removed from the configuration file.
     */
    removed,
    /**
     * The version of the instance database is not compatible.
     * It is potentially updateable
     */
    incompatible_version
}