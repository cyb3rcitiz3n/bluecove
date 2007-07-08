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
package net.sf.bluecove.obex;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;


/**
 * @author vlads
 *
 */
public class Main extends JFrame implements ActionListener {
	
	private static final long serialVersionUID = 1L;

	private static final int BLUETOOTH_DISCOVERY_STD_SEC = 11;
	
	private JLabel iconLabel;
	
	private String status;
	
	JProgressBar progressBar;
	
	private ImageIcon btIcon;
	
	private ImageIcon transferIcon;
	
	private ImageIcon searchIcon;
	
	private ImageIcon downloadIcon;
	
	private JComboBox cbDevices;
	
	private JButton btFindDevice;
	
	private JButton btSend;
	 
	private JButton btCancel;
	
	private BluetoothInquirer bluetoothInquirer;
	
	private Hashtable devices = new Hashtable(); 
	
	private String fileName;
	
	private byte[] data;
	
	protected Main() {
		super("BlueCove OBEX Push");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		Image btImage = Toolkit.getDefaultToolkit().getImage(Main.class.getResource("/icon.png"));
		btIcon = new ImageIcon(btImage);
		transferIcon = new ImageIcon((Toolkit.getDefaultToolkit().getImage(Main.class.getResource("/transfer.png"))));
		searchIcon = new ImageIcon((Toolkit.getDefaultToolkit().getImage(Main.class.getResource("/search.png"))));
		downloadIcon = new ImageIcon((Toolkit.getDefaultToolkit().getImage(Main.class.getResource("/download.png"))));
		
		this.setIconImage(btImage);
		
		JPanel contentPane = (JPanel)this.getContentPane();
		contentPane.setLayout(new BorderLayout(10, 10));
		contentPane.setBorder(new EmptyBorder(10,10,10,10));
		contentPane.setTransferHandler(new DropTransferHandler(this));
		
		JPanel progressPanel = new JPanel();
		progressPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		
		iconLabel = new JLabel();
		iconLabel.setIcon(btIcon);
		c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
		progressPanel.add(iconLabel, c);
		
		progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        
        c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = GridBagConstraints.REMAINDER;
        progressPanel.add(progressBar, c);
        
	    getContentPane().add(progressPanel, BorderLayout.NORTH);
	    
		JPanel optionsPanel = new JPanel();

		JLabel deviceLabel = new JLabel("Send to:");
		optionsPanel.add(deviceLabel);
		cbDevices = new JComboBox();
		cbDevices.addItem("{no device found}");
		cbDevices.setEnabled(false);
		optionsPanel.add(cbDevices);
		optionsPanel.add(btFindDevice = new JButton("Find"));
		btFindDevice.addActionListener(this);
		
	    getContentPane().add(optionsPanel, BorderLayout.CENTER);

	    JPanel actionPanel = new JPanel();
	    actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.LINE_AXIS));
	    actionPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
	    actionPanel.add(Box.createHorizontalGlue());
	    actionPanel.add(btSend = new JButton("Send"));
	    btSend.addActionListener(this);
	    actionPanel.add(Box.createRigidArea(new Dimension(10, 0)));
	    actionPanel.add(btCancel = new JButton("Cancel"));
	    btCancel.addActionListener(this);

    	contentPane.add(actionPanel, BorderLayout.SOUTH);
	    btSend.setEnabled(false);
	    String selected = Persistence.loadDevices(devices);
	    updateDevices(selected);
	}
	
	private static void createAndShowGUI(final String[] args) {
		final Main app = new Main();
		app.pack();
		app.center();
		app.setVisible(true);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (app.initializeBlueCove()) {
					if (args.length != 0) {
						app.downloadJar(args[0]);
					}
				}
			}
		});
	}
	
	public static void main(final String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI(args);
			}
		});
	}

	private void center() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		this.setLocation(((screenSize.width - this.getWidth()) / 2), ((screenSize.height - this.getHeight()) / 2));
	}
	
	protected void setStatus(final String message) {
		status = message;
		progressBar.setString(message);
	}
	
	void setProgressValue(int n) {
		progressBar.setValue(n);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				progressBar.setString(status);
			}
		});
	}
	
	protected void disabledBluetooth() {
		btFindDevice.setEnabled(false);
		cbDevices.setEnabled(false);
		setStatus("BlueCove not avalable");
		btSend.setEnabled(false);
		iconLabel.setIcon(new ImageIcon((Toolkit.getDefaultToolkit().getImage(Main.class.getResource("/bt-off.png")))));
	}
	
	protected boolean initializeBlueCove() {
		try {
			LocalDevice localDevice = LocalDevice.getLocalDevice();
			if ("000000000000".equals(localDevice.getBluetoothAddress())) {
				throw new Exception();
			}
			bluetoothInquirer = new BluetoothInquirer(this);
			setStatus("BlueCove Ready");
			return true;
		} catch (Throwable e) {
			debug(e);
			disabledBluetooth();
			return false;
		}
	}
	
	static void debug(Throwable e) {
		System.out.println(e.getMessage());
		e.printStackTrace();
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btFindDevice) {
			bluetoothDiscovery();
		} else if (e.getSource() == btCancel) {
			shutdown();
			System.exit(0);
		} else if (e.getSource() == btSend) {
			obexSend();
		}
		
	}


	private class DiscoveryTimerListener implements ActionListener {
		int seconds = 0;
		public void actionPerformed(ActionEvent e) {
			if (seconds < BLUETOOTH_DISCOVERY_STD_SEC) {
				seconds ++;
				setProgressValue(seconds);
			}
		}
	}
	
	private void addDevice(String btAddress, String name, String obexUrl) {
		String key = btAddress.toLowerCase(); 
		DeviceInfo di = (DeviceInfo)devices.get(key);
		if (di == null) {
			di = new DeviceInfo();
		}
		di.btAddress = btAddress;
		// Update name if one found
		if (di.name == null) {
			di.name = name;
		} else if (btAddress.equals(di.name)) {
			di.name = name;
		}
		di.obexUrl = obexUrl; 
		di.obexServiceFound = true;
		devices.put(key, di);
	}

	private void updateDevices(String selected) {
		cbDevices.removeAllItems();
		if (devices.size() == 0) {
			cbDevices.addItem("{no device found}");
			btSend.setEnabled(false);
			cbDevices.setEnabled(false);
		} else {
			for (Enumeration i = devices.keys(); i.hasMoreElements();) {
				String addr = (String) i.nextElement();
				DeviceInfo di = (DeviceInfo)devices.get(addr);
				cbDevices.addItem(di);
				if ((selected != null) && (selected.equals(di.btAddress))) {
					cbDevices.setSelectedItem(di); 
				}
			}
			cbDevices.setEnabled(true);
			btSend.setEnabled(true);
		}
	}

	private void bluetoothDiscovery() {
		final Timer timer = new Timer(1000, new DiscoveryTimerListener());
		progressBar.setMaximum(BLUETOOTH_DISCOVERY_STD_SEC);
		setProgressValue(0);
		Thread t = new Thread() {
			public void run() {
				if (bluetoothInquirer.startInquiry()) {
					iconLabel.setIcon(searchIcon);
					setStatus("Bluetooth discovery started");
					btFindDevice.setEnabled(false);
					timer.start();
					while (bluetoothInquirer.inquiring) {
						try {
							Thread.sleep(1000);
						} catch (Exception e) {
						}
					}
					timer.stop();
					//setStatus("Bluetooth discovery finished");
					
					setProgressValue(0);
					int idx = 0;
					progressBar.setMaximum(bluetoothInquirer.devices.size());
					for (Iterator iter = bluetoothInquirer.devices.iterator(); iter.hasNext();) {
						RemoteDevice dev = (RemoteDevice) iter.next();
						String obexUrl = bluetoothInquirer.findOBEX(dev.getBluetoothAddress());
						if (obexUrl != null){
							Logger.debug("found", dev.getBluetoothAddress());
							addDevice(dev.getBluetoothAddress(), BluetoothInquirer.getFriendlyName(dev), obexUrl);
						}
						idx ++;
						setProgressValue(idx);
					}
					setProgressValue(0);
					Persistence.storeDevices(devices, getSelectedDeviceAddress());
					updateDevices(null);
					btFindDevice.setEnabled(true);
					iconLabel.setIcon(btIcon);
				}
			}
		};
		t.start();
	}
	
	private String blueSoleilFindOBEX(String btAddress, String obexUrl) {
		if ("bluesoleil".equals(LocalDevice.getProperty("bluecove.stack"))) {
			RemoteDevice dev = new RemoteDeviceExt(btAddress);
			String foundObexUrl = bluetoothInquirer.findOBEX(dev.getBluetoothAddress());
			if (foundObexUrl != null){
				Logger.debug("found", btAddress);
				addDevice(dev.getBluetoothAddress(), BluetoothInquirer.getFriendlyName(dev), foundObexUrl);
			}
			return foundObexUrl;
		} 
		return obexUrl;
	}

	private DeviceInfo getSelectedDevice() {
		Object o = cbDevices.getSelectedItem();
		if ((o == null) || !(o instanceof DeviceInfo)) {
			return null;
		}
		return (DeviceInfo)o;
	}
	
	private String getSelectedDeviceAddress() {
		DeviceInfo d = getSelectedDevice();
		if (d == null) {
			return null;
		}
		return d.btAddress;
	}
	
	private void obexSend() {
		if (fileName == null) {
			setStatus("No file selected");
			return;
		}
		final DeviceInfo d = getSelectedDevice();
		if (d == null) {
			setStatus("No Device selected");
			return;
		}
		final ObexBluetoothClient o = new ObexBluetoothClient(this, fileName, data);
		Thread t = new Thread() {
			public void run() {
				btSend.setEnabled(false);
				iconLabel.setIcon(transferIcon);
				String obexUrl = d.obexUrl; 
				if (!d.obexServiceFound) {
					obexUrl = blueSoleilFindOBEX(d.btAddress, obexUrl);
				}
				o.obexPut(obexUrl);
				btSend.setEnabled(true);
				iconLabel.setIcon(btIcon);
				Persistence.storeDevices(devices, getSelectedDeviceAddress());
			}

		};
		t.start();
	}

	private static String simpleFileName(String filePath) {
		int idx = filePath.lastIndexOf('/');
		if (idx == -1) {
			idx = filePath.lastIndexOf('\\');
		}
		if (idx == -1) {
			return filePath;
		}
		return filePath.substring(idx + 1);
	}
	
	void downloadJar(final String filePath) {
		Thread t = new Thread() {
			public void run() {
				try {
					iconLabel.setIcon(downloadIcon);
					URL url = new URL(filePath);
					InputStream is = url.openConnection().getInputStream();  
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					byte[] buffer = new byte[0xFF];
					int i = is.read(buffer);
					int done = 0;
					while (i != -1) {
						bos.write(buffer, 0, i);
						done += i;
						//setProgressValue(done);
						i = is.read(buffer);
					}
					data = bos.toByteArray();
					fileName = simpleFileName(url.getFile());
					setStatus((data.length/1024) +"k " + fileName);
					iconLabel.setIcon(btIcon);
				} catch (Throwable e) {
					debug(e);
					setStatus("Download error" +  e.getMessage());
				}
			}
		};
		t.start();
		
	}

	private void shutdown() {
		if (bluetoothInquirer != null) {
			bluetoothInquirer.shutdown();
			bluetoothInquirer = null;
		}
	}
}
