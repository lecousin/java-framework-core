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
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.event.AsyncEvent;
import net.lecousin.framework.event.SimpleListenableContainer;
import net.lecousin.framework.progress.WorkProgress;
import net.lecousin.framework.util.ThreadUtil;

/**
 * Splash screen that may be used while loading an application.
 * It starts a new thread so the loading process can continue while the window is initializing,
 * and avoid slowing down the loading process.
 */
public class SplashScreen extends SimpleListenableContainer<AsyncEvent> implements WorkProgress {
	
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
	private Async<Exception> synch = new Async<>();
	
	@Override
	protected AsyncEvent createEvent() {
		return new AsyncEvent();
	}
	
	/** Close this window. */
	public void close() {
		JWindow w;
		synchronized (this) {
			if (!synch.isDone()) synch.unblock();
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
			synchronized (SplashScreen.this) {
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
			synchronized (SplashScreen.this) {
				ready = true;
				SplashScreen.this.notifyAll();
			}
			update();
			if (devMode) {
				// in development mode, hide it after maximum of 20 seconds to do not block debug screen
				for (int i = 0; i < 20 && win != null && (LCCore.get() == null || !LCCore.get().isStopping()); ++i)
					if (!ThreadUtil.sleep(1000))
						break;
				close();
			}
			thread = null;
		}
		
		private void loadPoweredBy() {
			ImageIcon image = loadImage("logo_110x40.png", 4363);
			if (image != null) {
				synchronized (SplashScreen.this) {
					if (win != null)
						poweredBy = image;
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
	
	private ImageIcon loadImage(String filename, int size) {
		int pos = 0;
		byte[] buffer = new byte[size];
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("net/lecousin/framework/core/" + filename)) {
			do {
				int nb = in.read(buffer, pos, size - pos);
				if (nb <= 0) break;
				pos += nb;
			} while (pos < size);
		} catch (IOException e) {
			// ignore, no logo
		}
		if (pos == size)
			return new ImageIcon(buffer);
		return null;
	}

	/** Load the default logo. */
	public void loadDefaultLogo() {
		ImageIcon image = loadImage("logo_200x200.png", 18296);
		if (image != null)
			setLogo(image, false);
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
	public IAsync<Exception> getSynch() {
		return synch;
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
		if (subText.isEmpty()) {
			progressText.setText("");
			progressSubText.setText(text);
		} else {
			progressText.setText(text);
			progressSubText.setText(subText);
		}
		progressBar.setValue((int)(50 + (10000L * worked / amount)));
		bottom.invalidate();
		if (!eventInterrupted)
			synchronized (this) {
				if (event != null) event.fire();
			}
	}
}
