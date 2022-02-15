/**
 * Copyright (C) 2009 EDIT
 * European Distributed Institute of Taxonomy
 * http://www.e-taxonomy.eu
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * See LICENSE.TXT at the top of this package for the full license terms.
 */
package eu.etaxonomy.cdm.server.instance;

import org.apache.log4j.Logger;

/**
 * @author a.kohlbecker
 * @date May 10, 2013
 */
public class Configuration {

	private static final Logger logger = Logger.getLogger(Configuration.class);

	private String instanceName;
	private String password;
	private String username;
	private String dataSourceUrl;
	private String driverClass;

	public String getInstanceName() {
		return instanceName;
	}

	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
	}

	public String getJdbcJndiName() {
		return "jdbc/" + instanceName;
	}

	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}

	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}

	public String getDataSourceUrl() {
		return dataSourceUrl;
	}
	public void setDataSourceUrl(String url) {
		this.dataSourceUrl = url;
	}

	public String getDriverClass() {
		return driverClass;
	}
	public void setDriverClass(String driverClass) {
		this.driverClass = driverClass;
	}

	@Override
	public String toString() {
		return instanceName + " : " + username + "@" + dataSourceUrl;

	}

	@Override
	public boolean equals(Object obj){
		if(obj == null || !(obj instanceof Configuration)){
			return false;
		}
		Configuration other = (Configuration)obj;
		return this.dataSourceUrl.equals(other.dataSourceUrl)
				&& this.driverClass.equals(other.driverClass)
				&& this.instanceName.equals(other.instanceName)
				&& this.password.equals(other.password)
				&& this.username.equals(other.username);
	}
}
