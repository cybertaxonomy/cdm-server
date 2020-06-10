/**
 * Copyright (C) 2013 EDIT
 * European Distributed Institute of Taxonomy
 * http://www.e-taxonomy.eu
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * See LICENSE.TXT at the top of this package for the full license terms.
 */
package eu.etaxonomy.cdm.server.instance;

/**
 * The Attributes defined in this calls are 1:1 copies of attributes which are
 * primarily defined in
 * <code>eu.etaxonomy.cdm.remote.config.*</code>
 *
 * TODO avoid duplication of code by directly referring to the original classes
 * but without fully depending on cdmlib-remote, since this would mean to increase the
 * size of the cdm-server a lot.
 *
 * @author a.kohlbecker
 * @date May 13, 2013
 *
 */
public class SharedAttributes {

    /**
     * same as
     * <code>eu.etaxonomy.cdm.remote.config.DataSourceConfigurer#ATTRIBUTE_JDBC_JNDI_NAME</code>
     */
    public static final String ATTRIBUTE_JDBC_JNDI_NAME = "cdm.jdbcJndiName";

    /**
     * same as
     * <code>eu.etaxonomy.cdm.remote.config.DataSourceConfigurer#ATTRIBUTE_DATASOURCE_NAME</code>
     */
    public static final String ATTRIBUTE_DATASOURCE_NAME = "cdm.datasource";


    /**
     * same as
     * <code>eu.etaxonomy.cdm.remote.config.AbstractWebApplicationConfigurer#ATTRIBUTE_ERROR_MESSAGES</code>
     */
    public static final String ATTRIBUTE_ERROR_MESSAGES = "cdm.errorMessages";

    /**
     * Force a schema update when the cdm-webapp instance is starting up
     * same as <code>eu.etaxonomy.cdm.remote.config.DataSourceConfigurer.ATTRIBUTE_FORCE_SCHEMA_UPDATE</code>
     */
    public static final String ATTRIBUTE_FORCE_SCHEMA_UPDATE = "cdm.forceSchemaUpdate";

}
