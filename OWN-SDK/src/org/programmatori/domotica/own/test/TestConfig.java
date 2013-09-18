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
package org.programmatori.domotica.own.test;

import junit.framework.*;

import org.programmatori.domotica.own.sdk.config.AbstractConfig;

public class TestConfig extends TestCase {
	private final String HandWritePath = "/Users/moreno/git/GitHub_OWN/OWN-SDK";
	
	public TestConfig(String name) {
		super(name);
	}
	
	public void testHomeFolder() {
		String path = AbstractConfig.getHomeDirectory();
		
		assertEquals( "Wrong Path", HandWritePath, path);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite();
		suite.addTest(new TestConfig("testHomeFolder"));
		
		return suite;
	}

}
