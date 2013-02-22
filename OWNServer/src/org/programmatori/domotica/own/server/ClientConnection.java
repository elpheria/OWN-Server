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
package org.programmatori.domotica.own.server;

import java.io.*;
import java.net.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.programmatori.domotica.own.sdk.config.Config;
import org.programmatori.domotica.own.sdk.msg.MessageFormatException;
import org.programmatori.domotica.own.sdk.msg.SCSMsg;
import org.programmatori.domotica.own.sdk.server.engine.*;
import org.programmatori.domotica.own.sdk.utils.LogUtility;
import org.programmatori.domotica.own.server.engine.*;

/**
 * Manager for a single client Connection.
 *
 * @author Moreno Cattaneo (moreno.cattaneo@gmail.com)
 * @since OWNServer v0.1.0
 * @version 0.6 29/04/2012
 */
public class ClientConnection implements Runnable, Monitor, Sender {
	private static final Log log = LogFactory.getLog(ClientConnection.class);

	private static final int STATUS_DISCONNECTED = -1;
	private static final int STATUS_START = 0;
	private static final int STATUS_PASSWORD = 1;
	private static final int STATUS_CONNECTED = 2;

	private TcpIpServer server = null;
	private Socket clientSocket = null;
	private long id = 0;

	private PrintWriter socketOut = null;
	private BufferedReader socketIn = null;

	private EngineManager engine;
	private int mode;
	private int status;

	public ClientConnection(Socket clientSocket, TcpIpServer server, EngineManager engine) {
		log.trace("Client Start");
		this.server = server;
		this.clientSocket = clientSocket;
		this.engine = engine;
		mode = OpenWebNetProtocol.MODE_COMMAND;

		id = GeneratorID.get();
		log.debug("Generate ID: " + id);

		status = STATUS_START;

		try {
			socketOut = new PrintWriter(clientSocket.getOutputStream(), true);
			socketIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			// in = new InputStreamReader(clientSocket.getInputStream());

		} catch (IOException e) {
			log.error(LogUtility.getErrorTrace(e));
		}
	}

	@Override
	public void run() {
		int timeout = Config.getInstance().getWelcomeTimeout();
		try {
			clientSocket.setSoTimeout(timeout);
		} catch (SocketException e1) {
			log.error(LogUtility.getErrorTrace(e1));
		}

		// Welcome
		log.debug("Welcome msg: " + OpenWebNetProtocol.MSG_WELCOME.toString());
		socketOut.print(OpenWebNetProtocol.MSG_WELCOME.toString());
		socketOut.flush();
		logSignal(OpenWebNetProtocol.MSG_WELCOME, true);

		try {
			String inputLine = "";
			int intch = 0;

			while (clientSocket.isConnected() && !Config.getInstance().isExit()) {
				if (!inputLine.endsWith("##")) {
					intch = socketIn.read();

					// If arrive -1 it mean the connection is close
					if (intch == -1) {
						if (mode != OpenWebNetProtocol.MODE_COMMAND) engine.removeMonitor(this);
						server.remove(this);
						log.trace("Client End");
						return;
					}

					inputLine += (char) intch;
				} else {
					log.debug(getId() + " RX MSG: " + inputLine);


					SCSMsg msgSCS = new SCSMsg(inputLine);
					logSignal(msgSCS, false);

					switch (status) {
					case STATUS_START:
						processStart(msgSCS);
						break;

					case STATUS_PASSWORD:
						//TODO: Implement PASSWORD case
						status = STATUS_CONNECTED;
						break;

					case STATUS_CONNECTED:
						if (mode == OpenWebNetProtocol.MODE_MONITOR) {
							throw new Exception("Monitor can only receive message");
						} else {
							SCSMsg msg = new SCSMsg(inputLine);
							engine.sendCommand(msg, this);
						}
						break;
					default:
						log.error("Unknow Status");
						break;
					}

					inputLine = "";
				}
			}

		} catch (IOException e) {
			log.error(LogUtility.getErrorTrace(e));
		} catch (MessageFormatException e) {
			log.error(LogUtility.getErrorTrace(e));
		} catch (Exception e) {
			log.error(LogUtility.getErrorTrace(e));
		}

		try {
			if (mode != OpenWebNetProtocol.MODE_COMMAND) engine.removeMonitor(this);
			server.remove(this);
			clientSocket.close();
		} catch (IOException e) {
			// stub
		}
		log.trace("Client End");
	}

	private void processStart(SCSMsg msgSCS) {
		SCSMsg response = SCSMsg.MSG_ACK;

		if (msgSCS.equals(OpenWebNetProtocol.MSG_MODE_COMMAND)) {
			mode = OpenWebNetProtocol.MODE_COMMAND;
			log.info(getId() + " Mode: Command");
		} else if (msgSCS.equals(OpenWebNetProtocol.MSG_MODE_MONITOR)) {
			mode = OpenWebNetProtocol.MODE_MONITOR;
			
			//Bug.ID: 3 Monitor don't have time-out
			try {
				clientSocket.setSoTimeout(0);
			} catch (SocketException e) {
				log.error(LogUtility.getErrorTrace(e));
			}
			
			log.info(getId() + " Mode: Monitor");
			engine.addMonitor(this);
		} else if (msgSCS.equals(OpenWebNetProtocol.MSG_MODE_TEST)) {
			mode = OpenWebNetProtocol.MODE_TEST;
			
			// Mixed mode i disable timeout
			try {
				clientSocket.setSoTimeout(0);
			} catch (SocketException e) {
				log.error(LogUtility.getErrorTrace(e));
			}
			
			log.info(getId() + " Mode: Test");
			engine.addMonitor(this);
		} else {
			response = SCSMsg.MSG_NACK;
			status = STATUS_DISCONNECTED;
			Config.getInstance().setExit(true);
		}

		// Check Next Status
		if (response.equals(SCSMsg.MSG_ACK)) {
			if (checkValidIP(clientSocket.getInetAddress())) {
				status = STATUS_CONNECTED;
			} else {
				response = createPwAsk();
				status = STATUS_PASSWORD;
			}
		}

		socketOut.print(response.toString());
		socketOut.flush();
		log.debug(getId() + " TX MSG: " + response.toString());
		logSignal(response, true);
	}

	private SCSMsg createPwAsk() {
		//FIXME: Add algorithm for password
		return null;
	}

	private boolean checkValidIP(InetAddress ip) {
		//FIXME: Now i accept anyone
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ClientConnection other = (ClientConnection) obj;
		if (id != other.id)
			return false;
		return true;
	}

//	@Deprecated
//	public void SCSValueChanged(SCSEvent e) {
//		log.debug(getId() + " TX MSG: " + e.getMessage().toString());
//
//		logSignal(e.getMessage(), true);
//		socketOut.print(e.getMessage().toString());
//		socketOut.flush();
//	}

	public void setMode(int mode) {
		this.mode = mode;
	}

	@Override
	public long getId() {
		return id;
	}

	public void logSignal(SCSMsg msg, boolean isSend) {
		Log log = LogFactory.getLog("org.programmatori.domotica.own.message");

		String direction = (isSend? "TX MSG:" : "RX MSG:");

		log.info(getId() + "-" + direction + msg.toString());
	}

	@Override
	public void reciveMsg(SCSMsg msg) {
		log.debug(getId() + " TX MSG: " + msg.toString());

		logSignal(msg, true);
		socketOut.print(msg.toString());
		socketOut.flush();
	}

}
