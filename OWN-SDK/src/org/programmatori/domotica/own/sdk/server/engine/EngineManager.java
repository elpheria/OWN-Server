/*
 * OWN Server is 
 * Copyright (C) 2010-2013 Moreno Cattaneo <moreno.cattaneo@gmail.com>
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
package org.programmatori.domotica.own.sdk.server.engine;

import org.programmatori.domotica.own.sdk.msg.SCSMsg;

/**
 * This interface is for connect the class that need to talk with the bus with it.
 * Is the interface for plugin and engine of the EnginemanagerImp that is in Server
 * 
 * @author Moreno Cattaneo (moreno.cattaneo@gmail.com)
 * @version 0.0.1, 22/02/2013
 * @since OWNServer 0.5.0
 */
public interface EngineManager {

	void sendCommand(SCSMsg msg, Sender client);

	void addMonitor(Monitor monitor);
	void removeMonitor(Monitor monitor);
}