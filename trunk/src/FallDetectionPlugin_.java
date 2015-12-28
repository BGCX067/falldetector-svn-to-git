import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import sun.jkernel.BackgroundDownloader;

import ij.plugin.ImageCalculator;
import ij.plugin.frame.*;
import ij.*;
import ij.process.*;
import ij.gui.*;

public class FallDetectionPlugin_ extends PlugInFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	Panel panel;
	static String title = new String("FALL DETECTION");
	int previousID;
	private JPanel loadPanel;
	private JPanel parameterPanel;
	private JPanel fallPanel;
	private GridBagLayout panelLayout;
	static Frame instance;
	protected boolean srodek = false;
	protected boolean odjecie = false;
	protected boolean binaryzacja = false;
	protected boolean erozja = false;
	private JTextField informacja;

	public FallDetectionPlugin_() {
		super(title);
		super.setResizable(false);
		if (IJ.versionLessThan("1.39t"))
			return;
		if (instance != null) {
			instance.toFront();
			return;
		}
		instance = this;
		addKeyListener(IJ.getInstance());

		instance.setLayout(new BorderLayout());
		setPanelLayout();
		loadPanel = new JPanel();
		instance.add(BorderLayout.WEST, loadPanel);

		parameterPanel = new JPanel();
		parameterPanel.setLayout(panelLayout);
		instance.add(BorderLayout.SOUTH, parameterPanel);

		fallPanel = new JPanel();
		instance.add(BorderLayout.CENTER, fallPanel);

		Border loweredetched = BorderFactory
				.createEtchedBorder(EtchedBorder.LOWERED);
		loadPanel.setBorder(BorderFactory.createTitledBorder(loweredetched,
				"Pliki"));
		parameterPanel.setBorder(BorderFactory.createTitledBorder(
				loweredetched, "Parametry"));
		fallPanel.setBorder(BorderFactory.createTitledBorder(loweredetched,
				"Wykrywanie"));
		
		loadPanel.add(addButton("Otwórz pliki"));

		fallPanel.add(addButton("Wykryj upadek"));	
		//informacja = new JTextField("Brak Upadku");
		//informacja.setEnabled(false);
		//informacja.setText("UPADEK");
		//informacja.setBackground(Color.RED);
		//IJ.beep();
		
		//fallPanel.add(informacja);
		
		// Comboboxy
		String[] erozjaString = { "Erozja IJ", "Erozja" };
		String[] binaryString = { "Binaryzacja IJ", "Binaryzacja" };
		String[] odjecieString = { "Minus", "Difference" };
		String[] srodekString = { "z polem", "z momentami" };

		JComboBox erozjaList = new JComboBox(erozjaString);
		JComboBox binaryList = new JComboBox(binaryString);
		JComboBox odjecieList = new JComboBox(odjecieString);
		JComboBox srodekList = new JComboBox(srodekString);

		erozjaList.setSelectedIndex(0);
		binaryList.setSelectedIndex(0);
		odjecieList.setSelectedIndex(1);
		srodekList.setSelectedIndex(1);

		erozjaList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox) e.getSource();
				if ((String) cb.getSelectedItem() == "Erozja") {
					erozja = true;
				}
				if ((String) cb.getSelectedItem() == "Erozja IJ") {
					erozja = false;
				}
			}
		});
		binaryList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox) e.getSource();
				if ((String) cb.getSelectedItem() == "Binaryzacja IJ") {
					binaryzacja = false;
				}
				if ((String) cb.getSelectedItem() == "Binaryzacja") {
					binaryzacja = true;
				}
			}
		});
		odjecieList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox) e.getSource();
				if ((String) cb.getSelectedItem() == "Minus") {
					odjecie = true;
				}
				if ((String) cb.getSelectedItem() == "Difference") {
					odjecie = false;
				}
			}
		});
		srodekList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox) e.getSource();
				if ((String) cb.getSelectedItem() == "z momentami") {
					srodek = false;
					System.out.println(" Co to jest" + e.getModifiers());
				}
				if ((String) cb.getSelectedItem() == "z polem") {
					srodek = true;
				}
			}
		});

		parameterPanel.add(addLabel("Odjêcie t³a: "), new GridBagConstraints(0,
				0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
				GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		parameterPanel.add(odjecieList, new GridBagConstraints(1, 0, 1, 1, 0.0,
				0.0, GridBagConstraints.FIRST_LINE_START,
				GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

		parameterPanel.add(addLabel("Binaryzacja: "), new GridBagConstraints(0,
				1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
				GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		parameterPanel.add(binaryList, new GridBagConstraints(1, 1, 1, 1, 0.0,
				0.0, GridBagConstraints.FIRST_LINE_START,
				GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

		parameterPanel.add(addLabel("Erozja: "), new GridBagConstraints(0, 2,
				1, 1, 0.0, 0.0, GridBagConstraints.EAST,
				GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		parameterPanel.add(erozjaList, new GridBagConstraints(1, 2, 1, 1, 0.0,
				0.0, GridBagConstraints.FIRST_LINE_START,
				GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

		parameterPanel.add(addLabel("Srodek cie¿koœci: "),
				new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
						GridBagConstraints.EAST, GridBagConstraints.NONE,
						new Insets(5, 5, 5, 5), 5, 5));
		parameterPanel.add(srodekList, new GridBagConstraints(1, 3, 1, 1, 0.0,
				0.0, GridBagConstraints.FIRST_LINE_START,
				GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

		pack();
		GUI.center(this);
		show();
	}

	private void setPanelLayout() {
		panelLayout = new GridBagLayout();
		panelLayout.rowWeights = new double[] { 0.1, 0.1, 0.1, 0.1, 0.1, 0.1,
				0.1, 0.1 };
		panelLayout.rowHeights = new int[] { 25, 25, 25, 25 };
		panelLayout.columnWeights = new double[] { 0.0, 0.0 };
		panelLayout.columnWidths = new int[] { 50, 50 };
	}

	private JLabel addLabel(String string) {
		JLabel l = new JLabel(string, JLabel.RIGHT);
		return l;
	}

	Button addButton(String label) {
		Button b = new Button(label);
		b.addActionListener(this);
		b.addKeyListener(IJ.getInstance());
		return b;
	}

	public void actionPerformed(ActionEvent e) {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null) {
			if (e.getActionCommand() == "Otwórz pliki") {
				SequenceLoader sq = new SequenceLoader();
				sq.run(null);
				return;
			} else {
				IJ.beep();
				IJ.showStatus("No image");
				IJ.showMessage("Najpierw otwórz pliki.");
				previousID = 0;
				return;
			}
		}
		if (!imp.lock()) {
			previousID = 0;
			return;
		}
		ImageProcessor ip = imp.getProcessor();
		int id = imp.getID();
		if (id != previousID)
			ip.snapshot();
		previousID = id;
		String label = e.getActionCommand();
		if (label == null)
			return;
		new CPOORunner(label, imp, ip, odjecie, binaryzacja, erozja, srodek);
	}

	public void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			instance = null;
		}
	}
}

class CPOORunner extends Thread {
	private String command;
	private ImagePlus imp;
	private ImageProcessor ip;
	private double xc;
	private double yc;
	private int[] srodek_x;
	private int[] srodek_y;
	private boolean upadek = false;
	private boolean backgroundMinus;
	private boolean binary;
	private boolean erode;
	private boolean centerOfGravity;
	private int prog_upadku = 10;

	CPOORunner(String command, ImagePlus imp, ImageProcessor ip,
			boolean odjecie, boolean binaryzacja, boolean erozja, boolean srodek) {
		super(command);
		this.command = command;
		this.imp = imp;
		this.ip = ip;
		backgroundMinus = odjecie;
		binary = binaryzacja;
		erode = erozja;
		centerOfGravity = srodek;
		setPriority(Math.max(getPriority() - 2, MIN_PRIORITY));
		start();
	}

	public void run() {
		try {
			runCommand(command, imp, ip);
		} catch (OutOfMemoryError e) {
			IJ.outOfMemory(command);
			if (imp != null)
				imp.unlock();
		} catch (Exception e) {
			CharArrayWriter caw = new CharArrayWriter();
			PrintWriter pw = new PrintWriter(caw);
			e.printStackTrace(pw);
			IJ.write(caw.toString());
			IJ.showStatus("");
			if (imp != null)
				imp.unlock();
		}
	}

	void runCommand(String command, ImagePlus imp, ImageProcessor ip) {
		IJ.showStatus(command + "...");
		if (command.equals("Otwórz pliki"))
			openImagesSequence(imp, ip);
		else if (command.equals("Wykryj upadek"))
			detectFall(imp, ip);
		imp.updateAndDraw();
		imp.unlock();
		IJ.showStatus("Czy wykryto " + upadek);
	}
	
	public boolean isUpadek(){
		return upadek;
	}
	
	private void detectFall(ImagePlus imp, ImageProcessor ip) {
		int size = imp.getStackSize();
		srodek_x = new int[size+1];
		srodek_y = new int[size+1];
		detectPersonShape(imp, ip);
		for (int i = 2; i <= size; i++) {
			imp.setSliceWithoutUpdate(i);
			if (centerOfGravity == false) {
				policz_cechy(imp, ip, i - 2);
			}
			if (centerOfGravity == true) {
				srodek_ciezkosci(imp, i - 2);
			}
		}
		for (int i = 2; i <= size; i++) {
			int r0, r1; // rozniczki I rzedu
			int r00; // rozniczki II rzedu
			boolean z00 = true; // kierunek ruchu, false -> upadek
			// predkosc
			r0 = (srodek_y[i - 2] - srodek_y[i - 1]);
			r1 = (srodek_y[i - 1] - srodek_y[i]);
			// przyspieszenie
			r00 = r0 - r1;
			if ((r0 < 0) && (r1 < 0))
				z00 = false;
			if ((Math.abs(r00) > prog_upadku) && (z00 == false)) {
				upadek = true;
			}
			System.out.print("Upadek faza " + i + upadek + "\n");
		}
		if (upadek == true)
			IJ.showMessage("WYKRYTO UPADEK.");
	}

	private void detectPersonShape(ImagePlus imp2, ImageProcessor ip2) {
		imp2.setSliceWithoutUpdate(1);
		ImageCalculator calculator = new ImageCalculator();
		int size = imp2.getStackSize();
		ImagePlus back = new ImagePlus("back", imp2.getImage());
		for (int i = 1; i <= size; i++) {
			imp2.setSlice(i);
			if (backgroundMinus == false)
				calculator.run("Difference", imp2, back);
			if (backgroundMinus == true)
				odjecie_tla(imp2, back);
			if (binary == false) {
				convertToBinary(imp.getProcessor());
				ip.filter(ImageProcessor.MEDIAN_FILTER);
				ip.dilate();
				ip.dilate();
				ip.invert();
			}
			if (binary == true)
				binaryzacja(imp, 35, 255);
			if (erode == false) {
				usun_grupy(imp2);
				usun_grupy(imp2);
				usun_krawedzie(imp2);
				ip.dilate();
				ip.dilate();
				ip.dilate();
			}

			if (erode == true) {
				erozja(imp2, 4);
				erozja(imp2, 4);
				erozja(imp2, 4);
				erozja(imp2, 4);
				erozja(imp2, 4);
				usun_grupy(imp2);
				usun_grupy(imp2);
				usun_krawedzie(imp2);
				dylatacja(imp2, 10);
			}
		}
	}

	public void openImagesSequence(ImagePlus imp, ImageProcessor ip) {
		SequenceLoader sq = new SequenceLoader();
		sq.run(null);
	}

	void convertToBinary(ImageProcessor ip) {
		ip.setAutoThreshold(ImageProcessor.ISODATA2,
				ImageProcessor.NO_LUT_UPDATE);
		double minThreshold = ip.getMinThreshold();
		double maxThreshold = ip.getMaxThreshold();
		int[] lut = new int[256];
		for (int j = 0; j < 256; j++) {
			if (j >= minThreshold && j <= maxThreshold)
				lut[j] = (byte) 255;
			else
				lut[j] = 0;
		}
		ip.applyTable(lut);
		ip.setThreshold(255, 255, ImageProcessor.NO_LUT_UPDATE);
		if (imp.isComposite()) {
			CompositeImage ci = (CompositeImage) imp;
			ci.setMode(CompositeImage.GRAYSCALE);
			ci.resetDisplayRanges();
			ci.updateAndDraw();
		}
		IJ.showStatus("");
	}

	double centralny(int p, int q, ImagePlus imp, ImageProcessor ip) {
		int width = imp.getWidth();
		int height = imp.getHeight();
		double result = 0;
		double spot = 0.0;

		for (int i = 1; i <= width; i++) {
			for (int j = 1; j <= height; j++) {
				if (ip.getPixelValue(i, j) < 0.5) {
					spot = 1.0;
				} else {
					spot = 0.0;
				}
				result += Math.pow(i, p) * Math.pow(j, q) * spot;
			}
		}
		return result;
	}

	public void policz_cechy(ImagePlus imp, ImageProcessor ip, int numer_zdjecia) {
		double m00 = centralny(0, 0, imp, ip);
		double m10 = centralny(1, 0, imp, ip);
		double m01 = centralny(0, 1, imp, ip);
		xc = m10 / m00;
		yc = m01 / m00;
		srodek_x[numer_zdjecia] = (int) xc;
		srodek_y[numer_zdjecia] = (int) yc;
	}

	void odjecie_tla(ImagePlus ob1, ImagePlus ob2) {
		ImageProcessor ip_ob1 = ob1.getProcessor();
		ImageProcessor ip_ob2 = ob2.getProcessor();

		int pix, x, y;
		for (y = 0; y < 480; y++) {
			for (x = 0; x < 640; x++) {
				pix = ip_ob1.get(x, y);
				if (pix >= ip_ob2.get(x, y))
					ip_ob1.set(x, y, pix - ip_ob2.get(x, y));
				else
					ip_ob1.set(x, y, ip_ob2.get(x, y) - pix);
			}
		}
	}

	void binaryzacja(ImagePlus ob1, int th1, int th2) {
		ImageProcessor ip_ob1 = ob1.getProcessor();
		int x, y;
		for (y = 0; y < 480; y++) {
			for (x = 0; x < 640; x++) {
				if ((ip_ob1.get(x, y) > th1) && (ip_ob1.get(x, y) < th2))
					ip_ob1.set(x, y, 0);
				else
					ip_ob1.set(x, y, 255);
			}
		}
	}

	void erozja(ImagePlus ob1, int ls) {
		ImageProcessor ip_ob1 = ob1.getProcessor();
		int pix = 0;
		int x, y, k, l, j = 0;
		int[][] tablica;
		tablica = new int[3][3];
		for (y = 1; y <= 478; y++) {
			for (x = 1; x <= 638; x++) {
				pix = ip_ob1.get(x, y);
				if (pix == 0) {
					tablica[0][0] = ip_ob1.get(x - 1, y - 1);
					tablica[0][1] = ip_ob1.get(x - 1, y);
					tablica[0][2] = ip_ob1.get(x - 1, y + 1);
					tablica[1][0] = ip_ob1.get(x, y - 1);
					tablica[1][1] = ip_ob1.get(x, y);
					tablica[1][2] = ip_ob1.get(x, y + 1);
					tablica[2][0] = ip_ob1.get(x + 1, y - 1);
					tablica[2][1] = ip_ob1.get(x + 1, y);
					tablica[2][2] = ip_ob1.get(x + 1, y + 1);
				}
				for (k = 0; k <= 2; k++) {
					for (l = 0; l <= 2; l++) {
						if ((tablica[k][l] == 255))
							j++;
					}
				}
				if (j > ls)
					ip_ob1.set(x, y, 255);
				j = 0;
			}
		}
	}

	void dylatacja(ImagePlus ob1, int glebokosc) {
		ImageProcessor ip_ob1 = ob1.getProcessor();
		int x = 0, y = 0, k, l, pix = 0;

		int[][] tablica;

		tablica = new int[640][480];

		for (x = 0; x < 640; x++) {
			for (y = 0; y < 480; y++) {
				tablica[x][y] = ip_ob1.get(x, y);
			}
		}

		for (y = glebokosc; y < (480 - glebokosc); y++) {
			for (x = glebokosc; x <= (640 - glebokosc); x++) {
				pix = ip_ob1.get(x, y);
				if (pix == 0) {
					for (k = -glebokosc; k <= glebokosc; k++) {
						for (l = -glebokosc; l <= glebokosc; l++) {
							tablica[x + k][y + l] = 0;
						}
					}
				}
			}
		}
		for (x = 0; x < 640; x++) {
			for (y = 0; y < 480; y++) {
				ip_ob1.set(x, y, tablica[x][y]);
			}
		}
	}

	void usun_grupy(ImagePlus ob1) {
		ImageProcessor ip_ob1 = ob1.getProcessor();
		int x, y;
		boolean gora = false, dol = false;
		int[] tablica;
		tablica = new int[5];

		for (y = 2; y < 477; y++) {
			for (x = 2; x < 638; x++) {
				if (ip_ob1.get(x, y) == 0) {
					// pionowo
					tablica[0] = ip_ob1.get(x, y - 2);
					tablica[1] = ip_ob1.get(x, y - 1);
					tablica[2] = ip_ob1.get(x, y);
					tablica[3] = ip_ob1.get(x, y + 1);
					tablica[4] = ip_ob1.get(x, y + 2);

					if ((tablica[0] == 255) || (tablica[1] == 255)) {
						gora = true;
					}

					if ((tablica[3] == 255) || (tablica[4] == 255)) {
						dol = true;
					}
					// poziomo
					tablica[0] = ip_ob1.get(x - 2, y);
					tablica[1] = ip_ob1.get(x - 1, y);
					tablica[2] = ip_ob1.get(x, y);
					tablica[3] = ip_ob1.get(x + 1, y);
					tablica[4] = ip_ob1.get(x + 2, y);

					if ((tablica[0] == 255) || (tablica[1] == 255)) {
						gora = true;
					}

					if ((tablica[3] == 255) || (tablica[4] == 255)) {
						dol = true;
					}

					if ((gora == true) && (dol == true)) {
						ip_ob1.set(x, y, 255);
						gora = false;
						dol = false;
					}
				}
			}
		}
	}

	void usun_grupy_1(ImagePlus ob1, int wym_x, int wym_y) {
		ImageProcessor ip_ob1 = ob1.getProcessor();
		int x = 0, y = 0, l = 0, k = 0, czarne = 0;

		int[][] tablica;
		tablica = new int[640][480];

		for (x = 0; x < 640; x++) {
			for (y = 0; y < 480; y++) {
				tablica[x][y] = ip_ob1.get(x, y);
			}
		}
		for (y = 0; y < (480 - wym_y); y++) {
			for (x = 0; x < (640 - wym_x); x++) {
				for (k = 0; k < wym_x; k = k + wym_x) {
					for (l = 0; l < wym_y; l++) {
						if (ip_ob1.get(x + k, y + l) == 0)
							czarne++;
					}
				}
				for (k = 0; k < wym_x; k++) {
					for (l = 0; l < wym_y; l = l + wym_y) {
						if (ip_ob1.get(x + k, y + l) == 0)
							czarne++;
					}
				}
				if (czarne == 0) {
					for (k = 0; k < wym_x; k++) {
						for (l = 0; l < wym_y; l++) {
							tablica[x][y] = 255;
						}
					}
				}
			}
		}

		for (x = 0; x < 640; x++) {
			for (y = 0; y < 480; y++) {
				ip_ob1.set(x, y, tablica[x][y]);
			}
		}
	}

	void usun_krawedzie(ImagePlus ob1) {
		ImageProcessor ip_ob1 = ob1.getProcessor();
		int x, y;
		for (y = 0; y < 480; y++) {
			for (x = 0; x < 640; x++) {
				if ((x <= 10) || (x >= 630))
					ip_ob1.set(x, y, 255);
				if ((y <= 10) || (y > 470))
					ip_ob1.set(x, y, 255);
			}
		}
	}

	void fall_detection(int prog_upadku) {
		int r0, r1, r2, r3; // rozniczki I rzedu
		int r00, r11, r22; // rozniczki II rzedu
		boolean z00 = true; // kierunek ruchu, false -> upadek
		boolean z11 = true;
		boolean z22 = true;
		// predkosc
		r0 = (srodek_y[1] - srodek_y[2]);
		r1 = (srodek_y[2] - srodek_y[3]);
		r2 = (srodek_y[3] - srodek_y[4]);
		r3 = (srodek_y[4] - srodek_y[5]);
		System.out.print("Predkosci\n");
		System.out.print(r0 + "\n");
		System.out.print(r1 + "\n");
		System.out.print(r2 + "\n");
		System.out.print(r3 + "\n");
		System.out.print("=========\n");
		// przyspieszenie
		r00 = r0 - r1;
		if ((r0 < 0) && (r1 < 0))
			z00 = false;
		System.out.print(r00 + "\n");
		System.out.print(z00 + "\n");
		r11 = r1 - r2;
		if ((r1 < 0) && (r2 < 0))
			z11 = false;
		System.out.print(r11 + "\n");
		System.out.print(z11 + "\n");
		r22 = r2 - r3;
		if ((r2 < 0) && (r1 < 3))
			z22 = false;
		System.out.print(r22 + "\n");
		System.out.print(z22 + "\n");
		if ((Math.abs(r00) > prog_upadku) && (z00 == false)) {
			upadek = true;
			System.out.print("Upadek faza 1 " + upadek + "\n");
		}
		;
		if ((Math.abs(r11) > prog_upadku) && (z11 == false)) {
			upadek = true;
			System.out.print("Upadek faza 2 " + upadek + "\n");
		}
		;
		if ((Math.abs(r22) > prog_upadku) && (z22 == false)) {
			upadek = true;
			System.out.print("Upadek faza 3 " + upadek + "\n");
		}
		;
	}

	void srodek_ciezkosci(ImagePlus ob1, int numer_zdjecia) {
		ImageProcessor ip_ob1 = ob1.getProcessor();
		int calka_x = 0;
		int calka_y = 0;
		int x, y, pole = 0, pix = 0;
		for (y = 0; y < 480; y++) {
			for (x = 0; x < 640; x++) {
				pix = ip_ob1.get(x, y);
				if (pix == 0) // piksel czarny
				{
					calka_x = calka_x + x;
					calka_y = calka_y + y;
					pole++;
				}
			}
		}
		srodek_x[numer_zdjecia] = (calka_x / pole);
		srodek_y[numer_zdjecia] = (calka_y / pole);
		System.out.print("X = ");
		System.out.print(srodek_x[numer_zdjecia]);
		System.out.print(" Y = ");
		System.out.print(srodek_y[numer_zdjecia]);
		System.out.print("\n");
	}
}
