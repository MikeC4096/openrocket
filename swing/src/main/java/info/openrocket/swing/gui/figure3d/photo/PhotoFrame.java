package info.openrocket.swing.gui.figure3d.photo;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.EventObject;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import info.openrocket.core.database.Databases;
import info.openrocket.core.document.OpenRocketDocument;
import info.openrocket.core.file.GeneralRocketLoader;
import info.openrocket.core.file.RocketLoadException;
import info.openrocket.core.gui.util.SimpleFileFilter;
import info.openrocket.core.l10n.Translator;
import info.openrocket.core.logging.Markers;
import info.openrocket.core.plugin.PluginModule;
import info.openrocket.core.startup.Application;
import info.openrocket.core.util.StateChangeListener;
import info.openrocket.swing.file.photo.PhotoStudioGetter;
import info.openrocket.swing.file.photo.PhotoStudioSetter;
import info.openrocket.swing.gui.main.SwingExceptionHandler;
import info.openrocket.swing.gui.util.FileHelper;
import info.openrocket.swing.gui.util.GUIUtil;
import info.openrocket.swing.gui.util.Icons;
import info.openrocket.swing.gui.util.SwingPreferences;
import info.openrocket.swing.gui.widgets.SaveFileChooser;
import info.openrocket.swing.logging.LoggingSystemSetup;
import info.openrocket.swing.startup.GuiModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

@SuppressWarnings("serial")
public class PhotoFrame extends JFrame {
	private static final Logger log = LoggerFactory.getLogger(PhotoFrame.class);
	private final int SHORTCUT_KEY = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
	private final Translator trans = Application.getTranslator();

	private final PhotoPanel photoPanel;
	private final JDialog settings;

	public PhotoFrame(OpenRocketDocument document, Window parent) {
		this(false, document);
		setTitle(trans.get("PhotoFrame.title") + " - " + document.getRocket().getName());

		// Close this window when the parent is closed
		parent.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				dispose();
			}
		});
	}

	public PhotoFrame(boolean app, OpenRocketDocument document) {
		PhotoSettings p = new PhotoStudioGetter(document.getPhotoSettings()).getPhotoSettings();

		// Send the new PhotoSetting to the core module
		p.addChangeListener(new StateChangeListener() {
			@Override
			public void stateChanged(EventObject e) {
				Map<String, String> par = PhotoStudioSetter.getPhotoSettings(p);
				document.setPhotoSettings(par);
			}
		});

		this.setMinimumSize(new Dimension(160, 150));
		this.setSize(1024, 768);
		photoPanel = new PhotoPanel(document, p);
		photoPanel.setDoc(document);
		setJMenuBar(getMenu(app));
		setContentPane(photoPanel);

		if (!app)
			setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				closeAction();
			}
		});

		
		GUIUtil.rememberWindowSize(this);
		this.setLocationByPlatform(true);
		GUIUtil.rememberWindowPosition(this);
		GUIUtil.setWindowIcons(this);

		settings = new JDialog(this, trans.get("PhotoSettingsConfig.title")) {
			{
				setContentPane(new PhotoSettingsConfig(p, document));
				setPreferredSize(new Dimension(600, 500));
				pack();
				this.setLocationByPlatform(true);
				GUIUtil.rememberWindowSize(this);
				GUIUtil.rememberWindowPosition(this);
				setVisible(true);
			}
		};
	}

	private JMenuBar getMenu(final boolean showOpen) {
		JMenuBar menubar = new JMenuBar();
		JMenu menu;
		JMenuItem item;

		// // File

		menu = new JMenu(trans.get("main.menu.file"));
		menu.setMnemonic(KeyEvent.VK_F);
		// // File-handling related tasks
		menu.getAccessibleContext().setAccessibleDescription(trans.get("main.menu.file.desc"));
		menubar.add(menu);

		if (showOpen) {
			item = new JMenuItem(trans.get("main.menu.file.open"), KeyEvent.VK_O);
			item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, SHORTCUT_KEY));
			// // Open a rocket design
			item.getAccessibleContext().setAccessibleDescription(trans.get("BasicFrame.item.Openrocketdesign"));
			item.setIcon(Icons.FILE_OPEN);
			item.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					log.info(Markers.USER_MARKER, "Open... selected");

					JFileChooser chooser = new JFileChooser();

					chooser.addChoosableFileFilter(FileHelper.ALL_DESIGNS_FILTER);
					chooser.addChoosableFileFilter(FileHelper.OPENROCKET_DESIGN_FILTER);
					chooser.addChoosableFileFilter(FileHelper.ROCKSIM_DESIGN_FILTER);
					chooser.addChoosableFileFilter(FileHelper.RASAERO_DESIGN_FILTER);
					chooser.setFileFilter(FileHelper.ALL_DESIGNS_FILTER);

					chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

					chooser.setCurrentDirectory(((SwingPreferences) Application.getPreferences()).getDefaultDirectory());
					int option = chooser.showOpenDialog(PhotoFrame.this);
					if (option == JFileChooser.APPROVE_OPTION) {
						File file = chooser.getSelectedFile();
						log.debug("Opening File " + file.getAbsolutePath());
						((SwingPreferences) Application.getPreferences()).setDefaultDirectory(chooser
								.getCurrentDirectory());
						GeneralRocketLoader grl = new GeneralRocketLoader(file);
						try {
							OpenRocketDocument doc = grl.load();
							photoPanel.setDoc(doc);
						} catch (RocketLoadException e1) {
							e1.printStackTrace();
						}
					}
				}
			});
			menu.add(item);
		}

		item = new JMenuItem(trans.get("PhotoFrame.menu.file.save"), KeyEvent.VK_S);
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, SHORTCUT_KEY));
		// // Open a rocket design
		item.getAccessibleContext().setAccessibleDescription(trans.get("PhotoFrame.menu.file.save"));
		item.setIcon(Icons.FILE_OPEN);
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				log.info(Markers.USER_MARKER, "Save... selected");
				photoPanel.addImageCallback(new PhotoPanel.ImageCallback() {
					@Override
					public void performAction(final BufferedImage image) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								log.info("Got image {} to save...", image);

								final FileFilter png = new SimpleFileFilter(trans.get("PhotoFrame.fileFilter.png"),
										".png");

								final JFileChooser chooser = new SaveFileChooser();

								chooser.addChoosableFileFilter(png);
								chooser.setFileFilter(png);
								chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

								chooser.setCurrentDirectory(((SwingPreferences) Application.getPreferences())
										.getDefaultDirectory());
								final int option = chooser.showSaveDialog(PhotoFrame.this);

								if (option != JFileChooser.APPROVE_OPTION) {
									log.info(Markers.USER_MARKER, "User decided not to save, option=" + option);
									return;
								}

								final File file = FileHelper.forceExtension(chooser.getSelectedFile(), "png");
								if (file == null) {
									log.info(Markers.USER_MARKER, "User did not select a file");
									return;
								}

								((SwingPreferences) Application.getPreferences()).setDefaultDirectory(chooser
										.getCurrentDirectory());
								log.info(Markers.USER_MARKER, "User chose to save image as {}", file);

								if (FileHelper.confirmWrite(file, PhotoFrame.this)) {
									try {
										ImageIO.write(image, "png", file);
									} catch (IOException e) {
										throw new Error(e);
									}
								}
							}
						});
					}
				});
			}
		});
		menu.add(item);

		// // Edit
		menu = new JMenu(trans.get("main.menu.edit"));
		menu.setMnemonic(KeyEvent.VK_E);
		// // Rocket editing
		menu.getAccessibleContext().setAccessibleDescription(trans.get("PhotoFrame.menu.edit.unk"));
		menubar.add(menu);

		Action action = new AbstractAction(trans.get("PhotoFrame.menu.edit.copy")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				photoPanel.addImageCallback(new PhotoPanel.ImageCallback() {
					@Override
					public void performAction(final BufferedImage image) {
						Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new Transferable() {
							@Override
							public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException,
									IOException {
								if (flavor.equals(DataFlavor.imageFlavor) && image != null) {
									return image;
								} else {
									throw new UnsupportedFlavorException(flavor);
								}
							}

							@Override
							public DataFlavor[] getTransferDataFlavors() {
								DataFlavor[] flavors = new DataFlavor[1];
								flavors[0] = DataFlavor.imageFlavor;
								return flavors;
							}

							@Override
							public boolean isDataFlavorSupported(DataFlavor flavor) {
								DataFlavor[] flavors = getTransferDataFlavors();
								for (int i = 0; i < flavors.length; i++) {
									if (flavor.equals(flavors[i])) {
										return true;
									}
								}

								return false;
							}
						}, null);
					}
				});
			}
		};
		item = new JMenuItem(action);
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, SHORTCUT_KEY));
		item.setMnemonic(KeyEvent.VK_C);
		item.getAccessibleContext().setAccessibleDescription(trans.get("PhotoFrame.menu.edit.copy.desc"));
		menu.add(item);

		menu.add(new JMenuItem(new AbstractAction(trans.get("PhotoSettingsConfig.title")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				settings.setVisible(true);
			}
		}));

		// Window
		menu = new JMenu(trans.get("PhotoFrame.menu.window"));
		menubar.add(menu);
		JMenu sizeMenu = new JMenu(trans.get("PhotoFrame.menu.window.size"));
		menu.add(sizeMenu);

		sizeMenu.add(new JMenuItem(new SizeAction(320, 240, "QVGA")));
		sizeMenu.add(new JMenuItem(new SizeAction(640, 480, "VGA")));
		sizeMenu.add(new JMenuItem(new SizeAction(1024, 768, "XGA")));

		sizeMenu.addSeparator();

		final String s = trans.get("PhotoFrame.menu.window.size.portrait");
		sizeMenu.add(new JMenuItem(new SizeAction(240, 320, s.replace("{0}", "QVGA"))));
		sizeMenu.add(new JMenuItem(new SizeAction(480, 640, s.replace("{0}", "VGA"))));
		sizeMenu.add(new JMenuItem(new SizeAction(768, 1024, s.replace("{0}", "XGA"))));

		sizeMenu.addSeparator();

		sizeMenu.add(new JMenuItem(new SizeAction(854, 480, "420p")));
		sizeMenu.add(new JMenuItem(new SizeAction(1280, 720, "720p")));
		sizeMenu.add(new JMenuItem(new SizeAction(1920, 1080, "1080p")));

		return menubar;
	}

	private class SizeAction extends AbstractAction {
		private final int w, h;

		SizeAction(final int w, final int h, final String n) {
			super(w + " x " + h + " (" + n + ")");
			this.w = w;
			this.h = h;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			photoPanel.setPreferredSize(new Dimension(w, h));
			PhotoFrame.this.pack();
		}

	}

	private boolean closeAction() {
		photoPanel.clearDoc();
		return true;
	}
	
	public static void main(String args[]) throws Exception {

		LoggingSystemSetup.setupLoggingAppender();
		LoggingSystemSetup.addConsoleAppender();

		// Setup the uncaught exception handler
		log.info("Registering exception handler");
		SwingExceptionHandler exceptionHandler = new SwingExceptionHandler();
		Application.setExceptionHandler(exceptionHandler);
		exceptionHandler.registerExceptionHandler();

		// Load motors etc.
		log.info("Loading databases");

		GuiModule guiModule = new GuiModule();
		Module pluginModule = new PluginModule();
		Injector injector = Guice.createInjector(guiModule, pluginModule);
		Application.setInjector(injector);

		guiModule.startLoader();

		// Set the look-and-feel
		log.info("Setting LAF");
		GUIUtil.applyLAF();

		// Load defaults
		((SwingPreferences) Application.getPreferences()).loadDefaultUnits();

		Databases.fakeMethod();

		GeneralRocketLoader grl = new GeneralRocketLoader(new File(
				"resources/datafiles/examples/A simple model rocket.ork"));
		OpenRocketDocument doc = grl.load();

		PhotoFrame pa = new PhotoFrame(true, doc);
		pa.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pa.setTitle("OpenRocket - Photo Studio Alpha");
		pa.setVisible(true);

		pa.photoPanel.setDoc(doc);
	}

}
