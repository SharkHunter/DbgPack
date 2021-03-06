package com.sharkhunter.dbgpack;

import java.awt.Window;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
//import java.awt.event.ItemEvent;
//import java.awt.event.ItemListener;
import java.awt.Insets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.SwingUtilities;
import javax.swing.plaf.metal.MetalIconFactory;

import net.pms.PMS;
import net.pms.external.ExternalListener;
import net.pms.external.ExternalFactory;
import net.pms.configuration.PmsConfiguration;
import net.pms.logging.LoggingConfigFileLoader;

import net.pms.external.dbgpack;

public class DbgPack_plugin implements ExternalListener, ActionListener /*, ItemListener*/ {

	private boolean init;
	private LinkedHashMap<File, JCheckBox> items;
	private String debug_log, dbg_zip;

	public DbgPack_plugin() {
		init = true;
		items = new LinkedHashMap<File, JCheckBox>();
		debug_log = LoggingConfigFileLoader.getLogFilePaths().get("debug.log");
		dbg_zip = debug_log.replace("debug.log", "pms_dbg.zip");
	}

	public void shutdown() {
	}

	public String name() {
		return "View and Zip Logs";
	}

	//@Override
	public JComponent config() {
		if (init) {
			poll();
			init = false;
		}
		JPanel top = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(0, 5, 0, 5);
		c.ipadx = 5;
		c.gridx = 0;
		c.gridy = 0;
		for (Map.Entry<File, JCheckBox> item : items.entrySet()) {
			File file = item.getKey();
			boolean exists = file.exists();
			JCheckBox box = item.getValue();
			if (box == null) {
				box = new JCheckBox(file.getName(), exists);
				item.setValue(box);
			}
			if (!exists) {
				box.setSelected(false);
				box.setEnabled(false);
			}
			c.weightx = 1.0;
			top.add(box, c);
			JButton open = exists ?
				new JButton(MetalIconFactory.getTreeLeafIcon()) : new JButton("+");
			open.setActionCommand(file.getAbsolutePath());
			open.setToolTipText((exists ? "" : "Create ") + file.getAbsolutePath());
			open.addActionListener(this);
			c.gridx++;
			c.weightx = 0.0;
			top.add(open, c);
			c.gridx--;
			c.gridy++;
		}
		c.weightx = 2.0;
		JButton debugPack = new JButton("Zip selected files");
		debugPack.setActionCommand("pack");
		debugPack.addActionListener(this);
		top.add(debugPack, c);
		JButton open = new JButton(MetalIconFactory.getTreeFolderIcon());
		open.setActionCommand("showzip");
		open.setToolTipText("Open zip location");
		open.addActionListener(this);
		c.gridx++;
		c.weightx = 0.0;
		top.add(open, c);
		return top;
	}

	private void poll() {
		// call the client callbacks
		for(ExternalListener listener:ExternalFactory.getExternalListeners()) {
			Object obj = null;
			if(listener instanceof net.pms.external.dbgpack) {
				obj = ((net.pms.external.dbgpack)listener).dbgpack_cb();
			}
			// backward compatibility - to be removed
			else if(listener instanceof com.sharkhunter.dbgpack.dbgpack) {
				obj = ((com.sharkhunter.dbgpack.dbgpack)listener).dbgpack_cb();
			}
			else continue;

			PMS.debug("found client " + listener.name());
			if(obj instanceof String) {
				add(((String)obj).split(","));
			} else if(obj instanceof String[]) {
				add((String[])obj);
			}
		}
		PmsConfiguration configuration = PMS.getConfiguration();
		// check dbgpack property in PMS.conf
		PMS.debug("checking dbgpack property in PMS.conf");
		String f = (String)configuration.getCustomProperty("dbgpack");
		if(f != null) {
			add(f.split(","));
		}
		// add core items with debug.log last (LinkedHashMap preserves insertion order)
		String profileDirectory = configuration.getProfileDirectory();
		add(new File(debug_log.replace("debug.log", "pmsencoder.log")));
		add(new File(profileDirectory, "WEB.conf"));
		add(new File(configuration.getProfilePath()));
		add(new File(debug_log));
	}

	private void add(String[] files) {
		for(String file:files) {
			PMS.debug("adding " + file);
			try {
				items.put(new File(file).getCanonicalFile(), null);
			} catch (IOException e) {}
		}
	}

	private void add(File file) {
		PMS.debug("adding " + file.getAbsolutePath());
		try {
			items.put(file.getCanonicalFile(), null);
		} catch (IOException e) {}
	}

	private void writeToZip(ZipOutputStream out, File f) throws Exception {
		byte[] buf = new byte[1024];
		int len;
		if(!f.exists()) {
			PMS.debug("DbgPack file "+f.getAbsolutePath()+" does not exist - ignoring");
			return;
		}
		FileInputStream in = new FileInputStream(f);
		out.putNextEntry(new ZipEntry(f.getName()));
		while ((len = in.read(buf)) > 0)
			out.write(buf, 0, len);
		out.closeEntry();
		in.close();
	}

	private boolean saveDialog() {
		JFileChooser fc = new JFileChooser() {
			public void approveSelection() {
				File f = getSelectedFile();
				if (!f.isDirectory()) {
					if (f.exists() && JOptionPane.showConfirmDialog(null, "Overwrite existing file?", "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
						return;
					}
					super.approveSelection();
				}
			}
		};
		fc.setFileFilter(
			new FileFilter () {
				public boolean accept(File f) {
					String s = f.getName();
					return f.isDirectory() || (s.endsWith(".zip") || s.endsWith(".ZIP"));
				}
				public String getDescription() {
					return "*.zip";
				}
			}
		);
		fc.setSelectedFile(new File(dbg_zip));
		if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
			dbg_zip = fc.getSelectedFile().getPath();
			return true;
		}
		return false;
	}

	private void packDbg() {
		if (! saveDialog()) {
			return;
		}
		try {
			ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(dbg_zip));
			for (Map.Entry<File, JCheckBox> item : items.entrySet()) {
				if (item.getValue().isSelected()) {
					File file = item.getKey();
					PMS.debug("packing " + file.getAbsolutePath());
						writeToZip(zos, file);
				}
			}
			zos.close();
		} catch (Exception e) {
			PMS.debug("error packing zip file "+e);
		}
	}

//	@Override
//	public void itemStateChanged(ItemEvent e) {
//	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String str = e.getActionCommand();
		if (str.equals("pack")) {
			packDbg();
		} else {
			// open
			try {
				File file = str.equals("showzip") ? new File(dbg_zip).getParentFile() : new File(str);
				boolean exists = file.isFile() && file.exists();
				if (!exists) {
					file.createNewFile();
				}
				java.awt.Desktop.getDesktop().open(file);
				if (!exists) {
					reload((JComponent)e.getSource());
				}
			} catch (IOException e1) {
				PMS.debug(String.format("Failed to open '%s' in default desktop application %s", str, e1));
			}
		}
	}

	private void reload(JComponent c) {
		// rebuild and restart
		PMS.debug("reloading.");
		init = true;
		((Window)c.getTopLevelAncestor()).dispose();
		JOptionPane.showOptionDialog((JFrame) (SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame())),
			config(), "Options", JOptionPane.CLOSED_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
	}
}
