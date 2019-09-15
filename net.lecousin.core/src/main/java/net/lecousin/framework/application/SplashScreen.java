package net.lecousin.framework.application;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.RepaintManager;
import javax.swing.border.LineBorder;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.event.AsyncEvent;
import net.lecousin.framework.progress.WorkProgress;

/**
 * Splash screen that may be used while loading an application.
 * It starts a new thread so the loading process can continue while the window is initializing,
 * and avoid slowing down the loading process.
 */
public class SplashScreen implements WorkProgress {
	
	/** Constructor.
	 * @param devMode if true the screen is automatically closed after 20 seconds if the application is still loading.
	 * 				This may be useful when debugging because else the splash screen remains in front of other windows.
	 */
	public SplashScreen(boolean devMode) {
		thread = new Thread(new Init(), "Splash Screen");
		this.devMode = devMode;
		thread.start();
	}
	
	private static final String FONT_FAMILY_NAME = "Arial";
	
	private Thread thread;
	private boolean devMode;
	JWindow win;
	private JPanel logoContainer;
	private JPanel titleContainer;
	private JPanel bottom;
	private JLabel progressText;
	private JLabel progressSubText;
	private JProgressBar progressBar;
	private ImageIcon poweredBy = null;
	private boolean ready = false;

	private JLabel logo = null;
	private JLabel title = null;
	private boolean isCustomLogo = false;
	
	private String text = "";
	private String subText = "";
	private long amount = 10000;
	private long worked = 0;
	private SynchronizationPoint<Exception> synch = new SynchronizationPoint<>();
	private AsyncEvent event = null;
	
	/** Close this window. */
	public void close() {
		JWindow w;
		synchronized (this) {
			if (!synch.isUnblocked()) synch.unblock();
			if (win == null) return;
			w = win;
			win = null;
		}
		if (event != null)
			event.fire();
		w.setVisible(false);
		w.removeAll();
		w.dispose();
		RepaintManager.setCurrentManager(null);
		logoContainer = null;
		titleContainer = null;
		bottom = null;
		progressText = null;
		progressSubText = null;
		progressBar = null;
		poweredBy = null;
		logo = null;
		title = null;
		text = null;
		subText = null;
		event = null;
	}
	
	private class Init implements Runnable {
	
		@Override
		@SuppressWarnings("squid:AssignmentInSubExpressionCheck") // it makes the code clearer
		public void run() {
			synchronized (this) {
				win = new JWindow();
				win.setAlwaysOnTop(true);
				win.getRootPane().setBorder(new LineBorder(Color.BLACK));
				win.getRootPane().getContentPane().setLayout(new BoxLayout(win.getRootPane().getContentPane(), BoxLayout.Y_AXIS));
				win.getRootPane().getContentPane().add(logoContainer = new JPanel());
				FlowLayout fl = new FlowLayout();
				fl.setVgap(0);
				fl.setHgap(0);
				logoContainer.setLayout(fl);
				win.getRootPane().getContentPane().add(titleContainer = new JPanel());
				fl = new FlowLayout();
				fl.setVgap(0);
				fl.setHgap(0);
				titleContainer.setLayout(fl);
				win.getRootPane().getContentPane().add(bottom = new JPanel());
				bottom.setMinimumSize(new Dimension(400, 40));
				bottom.setPreferredSize(new Dimension(400, 40));
				bottom.setMaximumSize(new Dimension(400, 40));
				GridBagLayout gb = new GridBagLayout();
				gb.columnWeights = new double[] { 1,0 };
				gb.rowWeights = new double[] { 1,0 };
				bottom.setLayout(gb);
				bottom.setBackground(Color.WHITE);
				progressText = new JLabel("");
				progressText.setFont(new Font(FONT_FAMILY_NAME, 0, 10));
				GridBagConstraints grid = new GridBagConstraints();
				grid.gridx = 0;
				grid.gridy = 0;
				grid.anchor = GridBagConstraints.SOUTHEAST;
				grid.fill = GridBagConstraints.HORIZONTAL;
				bottom.add(progressText, grid);
				progressSubText = new JLabel("Loading lecousin.net framework");
				progressSubText.setFont(new Font(FONT_FAMILY_NAME, 0, 10));
				grid = new GridBagConstraints();
				grid.gridx = 0;
				grid.gridy = 1;
				grid.anchor = GridBagConstraints.SOUTHEAST;
				grid.fill = GridBagConstraints.HORIZONTAL;
				bottom.add(progressSubText, grid);
				progressBar = new JProgressBar();
				grid = new GridBagConstraints();
				grid.gridx = 0;
				grid.gridy = 2;
				grid.anchor = GridBagConstraints.SOUTHEAST;
				grid.fill = GridBagConstraints.HORIZONTAL;
				bottom.add(progressBar, grid);
			}
			if (logoContainer.getComponentCount() == 0 || isCustomLogo)
				loadPoweredBy();
			synchronized (this) {
				ready = true;
				this.notifyAll();
			}
			update();
			if (devMode) {
				// in development mode, hide it after maximum of 20 seconds to do not block debug screen
				for (int i = 0; i < 20 && win != null && (LCCore.get() == null || !LCCore.get().isStopping()); ++i) {
					try { Thread.sleep(1000); }
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
				}
				close();
			}
			thread = null;
		}
		
		private void loadPoweredBy() {
			int pos = 0;
			int size = 4363;
			byte[] buffer = new byte[size];
			try (InputStream in = getClass().getClassLoader().getResourceAsStream("net/lecousin/framework/core/logo_110x40.png")) {
				do {
					int nb = in.read(buffer, pos, size - pos);
					if (nb <= 0) break;
					pos += nb;
				} while (pos < size);
			} catch (IOException e) {
				// ignore, no logo
			}
			if (pos == size) {
				synchronized (this) {
					if (win != null)
						poweredBy = new ImageIcon(buffer);
				}
			}
		}
		
	}
	
	public boolean isReady() { return ready; }
	
	@SuppressWarnings("squid:S3776") // complexity
	private void update() {
		if (!ready) return;
		if (win == null) return;
		if (logo != null && logoContainer.getComponentCount() == 0) {
			logoContainer.setBackground(Color.WHITE);
			logo.setBackground(Color.WHITE);
			logoContainer.add(logo);
		}
		if (logo != null && isCustomLogo && bottom.getComponentCount() == 2) {
			JPanel panel = new JPanel();
			FlowLayout layout = new FlowLayout();
			layout.setVgap(0);
			layout.setVgap(0);
			panel.setLayout(layout);
			panel.setBackground(Color.WHITE);
			GridBagConstraints grid = new GridBagConstraints();
			grid.gridx = 1;
			grid.gridy = 0;
			grid.gridheight = 3;
			panel.setMinimumSize(new Dimension(110, 40));
			panel.setMaximumSize(new Dimension(110, 40));
			panel.setPreferredSize(new Dimension(110, 40));
			JLabel label = new JLabel(poweredBy);
			panel.add(label);
			bottom.add(panel, grid);
		}
		if (title != null) {
			if (isCustomLogo) {
				if (titleContainer.getComponentCount() != 0)
					titleContainer.removeAll();
			} else {
				if (titleContainer.getComponentCount() == 0)
					titleContainer.add(title);
				FlowLayout layout = new FlowLayout();
				layout.setVgap(0);
				layout.setVgap(0);
				titleContainer.setLayout(layout);
				titleContainer.setBackground(Color.WHITE);
				title.setForeground(new Color(0,0,0x60));
				title.setFont(new Font(FONT_FAMILY_NAME, Font.BOLD, 12));
			}
		}
		int width = 400;
		int height = 0;
		if (logoContainer.getComponentCount() > 0 && logo != null)
			height += logo.getPreferredSize().height;
		if (titleContainer.getComponentCount() > 0)
			height += titleContainer.getPreferredSize().height;
		height += 40;
		height += 2; // borders
		win.setSize(width, height);
		if (!win.isVisible())
			win.setVisible(true);
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		win.setLocation(screen.width / 2 - width / 2, screen.height / 2 - height / 2);
	}
	
	/** Change the logo. */
	public synchronized void setLogo(ImageIcon logo, boolean isCustom) {
		if (win == null) return;
		this.logo = new JLabel(logo);
		Dimension dim = new Dimension(logo.getIconWidth(), logo.getIconHeight());
		if (dim.width > 400) dim.width = 400;
		if (dim.height > 300) dim.height = 300;
		this.logo.setMaximumSize(dim);
		this.logo.setMinimumSize(dim);
		this.logo.setPreferredSize(dim);
		this.isCustomLogo = isCustom;
		update();
	}

	/** Load the default logo. */
	public void loadDefaultLogo() {
		int pos = 0;
		int size = 18296;
		byte[] buffer = new byte[size];
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("net/lecousin/framework/core/logo_200x200.png")) {
			do {
				int nb = in.read(buffer, pos, size - pos);
				if (nb <= 0) break;
				pos += nb;
			} while (pos < size);
		} catch (IOException e) {
			// ignore, no logo
		}
		if (pos == size)
			setLogo(new ImageIcon(buffer), false);
	}
	
	/** Show application name. */
	public synchronized void setApplicationName(String name) {
		if (win == null) return;
		title = new JLabel("Starting " + name + "...");
		update();
	}
	
	/** End of initial startup. */
	public synchronized void endInit() {
		if (win == null) return;
		progressText.setText("Starting framework...");
		progressBar.setMinimum(0);
		progressBar.setMaximum(10050);
		progressBar.setValue(50);
		bottom.invalidate();
	}
	
	@Override
	public synchronized void setAmount(long work) {
		if (win == null) return;
		amount = work;
		updateProgress();
	}
	
	@Override
	@SuppressWarnings("squid:S2886") // no need for synchronization
	public long getAmount() {
		return amount;
	}
	
	@Override
	public synchronized void setPosition(long position) {
		if (win == null) return;
		worked = position;
		updateProgress();
	}
	
	@Override
	@SuppressWarnings("squid:S2886") // no need for synchronization
	public long getPosition() {
		return worked;
	}
	
	@Override
	public long getRemainingWork() {
		return amount - worked;
	}
	
	@Override
	public synchronized void progress(long amountDone) {
		if (win == null) return;
		worked += amountDone;
		updateProgress();
	}
	
	@Override
	public synchronized void setText(String text) {
		if (win == null) return;
		this.text = text;
		updateProgress();
	}
	
	@Override
	@SuppressWarnings("squid:S2886") // no need for synchronization
	public String getText() {
		return text;
	}
	
	@Override
	public synchronized void setSubText(String text) {
		if (win == null) return;
		subText = text;
		updateProgress();
	}
	
	@Override
	@SuppressWarnings("squid:S2886") // no need for synchronization
	public String getSubText() {
		return subText;
	}
	
	@Override
	public void done() {
		close(); // close window and unblock synch
	}
	
	@Override
	public void error(Exception error) {
		synch.error(error);
	}
	
	@Override
	public void cancel(CancelException reason) {
		synch.cancel(reason);
	}
	
	@Override
	public ISynchronizationPoint<Exception> getSynch() {
		return synch;
	}
	
	@Override
	public void listen(Runnable onchange) {
		synchronized (this) {
			if (event == null) event = new AsyncEvent();
			event.addListener(onchange);
		}
	}
	
	@Override
	public void unlisten(Runnable onchange) {
		synchronized (this) {
			if (event == null) return;
			event.removeListener(onchange);
		}
	}
	
	private boolean eventInterrupted;
	
	@Override
	public void interruptEvents() {
		eventInterrupted = true;
	}
	
	@Override
	public void resumeEvents(boolean trigger) {
		eventInterrupted = false;
		if (trigger)
			updateProgress();
	}
	
	private void updateProgress() {
		if (subText.length() == 0) {
			progressText.setText("");
			progressSubText.setText(text);
		} else {
			progressText.setText(text);
			progressSubText.setText(subText);
		}
		progressBar.setValue((int)(50 + (10000 * worked / amount)));
		bottom.invalidate();
		if (!eventInterrupted)
			synchronized (this) {
				if (event != null) event.fire();
			}
	}
}
