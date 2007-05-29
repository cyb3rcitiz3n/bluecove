/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *  @version $Id$
 */
package com.intel.bluetooth;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.UUID;

public class SearchServicesThread extends Thread {
	
	private static int transIDGenerator = 0;
	
	private BluetoothStack stack;
	
	private int transID;
	
	private int[] attrSet; 
	
	UUID[] uuidSet; 
	
	RemoteDevice device;

	private DiscoveryListener listener;

	private BluetoothStateException startException;
	
	private boolean started = false;
	
	private boolean terminated = false;
	
	private SearchServicesThread(BluetoothStack stack, int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener) {
		super("SearchServicesThread");
		this.stack = stack;
		this.transID = (++transIDGenerator);
		this.attrSet = attrSet;
		this.listener = listener;
		this.uuidSet = uuidSet;
		this.device = device;
	}

	/**
	 * Start DeviceInquiry and wait for startException or deviceInquiryStartedCallback
	 */
	public static int startSearchServices(BluetoothStack stack, int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {
		SearchServicesThread t = (new SearchServicesThread(stack, attrSet, uuidSet, device, listener));
		//In case the BTStack hangs, exit JVM anyway
		t.setDaemon(true);
		synchronized (t) {
			t.start();
			while (!t.started && !t.terminated) {
				try {
					t.wait();
				} catch (InterruptedException e) {
					return 0;
				}
				if (t.startException != null) {
					throw t.startException;
				}
			}
		}
		if (t.started) {
			return t.getTransID();
		} else {
			return 0;
		}
	}
	
	public void run() {
		int respCode = DiscoveryListener.SERVICE_SEARCH_ERROR;
		try {
			respCode = stack.runSearchServices(this, attrSet, uuidSet, device, listener);
		} catch (BluetoothStateException e) {
			startException = e;
			return;
		} finally {
			terminated = true;
			synchronized (this) {
				notifyAll();
			}
			DebugLog.debug("runSearchServices ends");
		}
		if (started) {
			listener.serviceSearchCompleted(getTransID(), respCode);
		}
	}
	
	public void searchServicesStartedCallback() {
		DebugLog.debug("searchServicesStartedCallback");
		started = true;
		synchronized (this) {
			notifyAll();
		}
	}

	public int getTransID() {
		return this.transID;
	}

}
