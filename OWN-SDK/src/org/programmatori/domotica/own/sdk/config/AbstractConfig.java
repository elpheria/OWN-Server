/*
 * OWN Server is 
 * Copyright (C) 2010-2012 Moreno Cattaneo <moreno.cattaneo@gmail.com>
 * 
 * This file is part of OWN Server.
 * 
 * OWN Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 * 
 * OWN Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with OWN Server.  If not, see 
 * <http://www.gnu.org/licenses/>.
 */
package org.programmatori.domotica.own.sdk.config;

import java.io.File;
import java.util.*;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.programmatori.domotica.own.sdk.utils.LogUtility;

/**
 * Abstract configuration
 * @version 2.1 16/10/2010
 * @author Moreno Cattaneo (moreno.cattaneo@gmail.com)
 */
public abstract class AbstractConfig {
	private static Log log = LogFactory.getLog(AbstractConfig.class);
	public static final String DEFAULT_CONFIG_FOLDER = "conf";
	public static final String DEFAULT_CONFIG_PATH = "./" + DEFAULT_CONFIG_FOLDER;
	public static String HOME_FILE = "home.config";


	private String configPath;
	private boolean configLoaded;

	private XMLConfiguration config = null;

	protected AbstractConfig(String configPath) {
		this.configPath = configPath;
		configLoaded = false;

		loadConfig();
	}

	protected AbstractConfig() {
		this(null);
	}

	private void loadConfig() {
		try {
			config = new XMLConfiguration(getConfigPath() + "/config.xml");
			config.setAutoSave(true);
			log.info("Config File: " + config.getURL());

			configLoaded = true;
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
	}

	protected boolean isConfigLoaded() {
		return configLoaded;
	}

	public void setConfig(String configPath) {
		this.configPath = configPath;

		loadConfig();
	}

	protected String getString(String key) {
		return config.getString(key);
	}

	protected String getString(String key, String defaultVaule) {
		return config.getString(key, defaultVaule);
	}

	protected Boolean getBoolean(String key) {
		return config.getBoolean(key);
	}

	protected Boolean getBoolean(String key, boolean defaultVaule) {
		return config.getBoolean(key, defaultVaule);
	}

	protected int getInt(String key, int defaultValue) {
		return config.getInt(key, defaultValue);
	}

	protected void setParam(String key, String value) {
		config.addProperty(key, value);
	}

	protected void setParamWithNameSearch(String nodeToSearch, String nameSearched, String param, String value) {
		int pos = IndexOfAttributeName(nodeToSearch, nameSearched);

		if (pos > -1) {
			config.setProperty(nodeToSearch + "(" + pos + ")." + param, value);
			log.debug("Param: " + nodeToSearch + "(" + pos + ")." + param + " = " + value);
		}
	}

	protected String getParamWithNameSearch(String nodeToSearch, String nameSearched, String param) {
		String value = null;
		int pos = IndexOfAttributeName(nodeToSearch, nameSearched);

		if (pos > -1) {
			value = (String) config.getProperty(nodeToSearch + "(" + pos + ")." + param);
			log.debug("Param: " + nodeToSearch + "(" + pos + ")." + param + " = " + value);
		}

		return value;
	}

	protected int IndexOfAttributeName(String nodeToSearch, String nameSearched) {
		int idx = config.getMaxIndex(nodeToSearch);

		int pos = 0;
		while (!nameSearched.equals(config.getProperty(nodeToSearch + "(" + pos + ")[@name]")) && pos <= idx) {
			pos++;
		}

		if (nameSearched.equals(config.getProperty(nodeToSearch + "(" + pos + ")[@name]"))) {
			return pos;
		} else
			return -1;
	}

	public String getConfigPath() {
		String path = configPath;
		
		if (path == null) {
			try {
				String home = getHomeDirectory(HOME_FILE);
				path = home + "/" + DEFAULT_CONFIG_FOLDER;

			} catch (Exception e) {
				log.error(LogUtility.getErrorTrace(e));
				path = DEFAULT_CONFIG_PATH;
			}
		}
		return path;
	}

	protected Map<String, String> getMap(String nodeToSearch) {
		int idx = config.getMaxIndex(nodeToSearch);

		Map<String, String> ret = new HashMap<String, String>();

		int pos = 0;
		while (pos <= idx) {
			String key = (String) config.getProperty(nodeToSearch + "(" + pos + ")[@id]");
			String val = (String) config.getProperty(nodeToSearch + "(" + pos + ")");

			ret.put(key, val);

			pos++;
		}

		return ret;
	}

	protected void updateConfigFileVersion() {
		updateConfigFile(config);
	}

	protected abstract void updateConfigFile(XMLConfiguration config);

	/**
	 * Give the home of the project. <br>
	 * For return the home of the project need to have a file in the home
	 * <p>
	 *
	 * @return path of home directory
	 */
	public static String getHomeDirectory(String fileName) throws Exception {
		String home = "";
		String separator = File.separator;
		boolean first = true;

		try {
			Log log = LogFactory.getLog(AbstractConfig.class);

			// for solve bug in the jar use a real file instead a "."
			String filePath = null;

			// check presence of "filename", if it isn't raise an Exception
			filePath = ClassLoader.getSystemResource(fileName).toString();

			log.debug("Path of " + fileName + ": " + filePath);
			StringTokenizer st = new StringTokenizer(filePath, "/"); // URI Separator
			st.nextToken(); // don't calculate "file:"
			while (st.hasMoreTokens()) {
				String folder = st.nextToken();
				log.debug("Foledr: " + folder);

				// BUG Linux starting slash
				if (separator.equals("/") && first) {
					folder = separator + folder;
					first = false;
				}

				// BUG Eclipse put in the bin
				boolean bBin = !(folder.equals("bin") && (st.countTokens() < 2));

				// BUG jar can not support . then use a real file for find a path
				boolean bRealFile = !(folder.equals(fileName) && (st.countTokens() < 1));

				// BUG the home directory it cannot end with .jar
				boolean bJar = !(folder.endsWith(".jar!") && (st.countTokens() < 2));

				if (bBin && bRealFile && bJar) { // If i build under bin i don't insert in
					// home path
					if (home.length() > 0)
						home += "/";
					home = home + folder;
					log.debug("home: " + home);
				}
			}
		} catch (Exception e) {
			throw new Exception("File " + fileName + " must be present, please ensure it exists ");
		}

		return home;
	}

}