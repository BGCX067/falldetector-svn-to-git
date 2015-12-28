import java.awt.*;
import java.io.*;
import java.awt.event.*;
import java.awt.image.ColorModel;
import ij.*;
import ij.io.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.*;
import ij.measure.Calibration;

/** Implements the image sequence open. */
public class SequenceLoader implements PlugIn {

	private static boolean sortFileNames = true;
	private static boolean virtualStack;
	private int n, start, increment;
	private String filter;
	private boolean isRegex;
	private FileInfo fi;
	private String info1;

	public void run(String arg) {
		OpenDialog od = new OpenDialog("Otwórz sekwencje obrazków...", arg);
		String directory = od.getDirectory();
		String name = od.getFileName();
		if (name==null)
			return;
		String[] list = (new File(directory)).list();
		if (list==null)
			return;
		String title = directory;
		if (title.endsWith(File.separator) || title.endsWith("/"))
			title = title.substring(0, title.length()-1);
		int index = title.lastIndexOf(File.separatorChar);
		if (index!=-1) title = title.substring(index + 1);
		if (title.endsWith(":"))
			title = title.substring(0, title.length()-1);
		
		IJ.register(SequenceLoader.class);
		list = trimFileList(list);
		if (list==null) return;
		if (IJ.debugMode) IJ.log("SequenceLoader: "+directory+" ("+list.length+" files)");
		int width=0,height=0,depth=0,bitDepth=0;
		ImageStack stack = null;
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		Calibration cal = null;
		boolean allSameCalibration = true;
		IJ.resetEscape();		
		try {
			for (int i=0; i<list.length; i++) {
				IJ.redirectErrorMessages();
				ImagePlus imp = (new Opener()).openImage(directory, list[i]);
				if (imp!=null) {
					width = imp.getWidth();
					height = imp.getHeight();
					bitDepth = imp.getBitDepth();
					fi = imp.getOriginalFileInfo();
					if (!showDialog(imp, list))
						return;
					break;
				}
			}
			if (width==0) {
				IJ.error("Sekwencja do zaimportowania", "TW folderze nie ma plików z rozszerzeniem TIFF,\n"
				+ "JPEG, BMP, DICOM, GIF, FITS or PGM files.");
				return;
			}

			if (filter!=null && (filter.equals("") || filter.equals("*")))
				filter = null;
			if (filter!=null) {
				int filteredImages = 0;
  				for (int i=0; i<list.length; i++) {
					if (isRegex&&list[i].matches(filter))
						filteredImages++;
					else if (list[i].indexOf(filter)>=0)
						filteredImages++;
 					else
 						list[i] = null;
 				}
  				if (filteredImages==0) {
  					if (isRegex)
  						IJ.error("Sekwencja do zaimportowania", "Brak plików zawieraj¹cych wyra¿enie regularne.");
  					else
   						IJ.error("Sekwencja do zaimportowania", "Zaden z "+list.length+" plików nie zawiera\n stringa '"+filter+"' w nazwie.");
 					return;
  				}
  				String[] list2 = new String[filteredImages];
  				int j = 0;
  				for (int i=0; i<list.length; i++) {
 					if (list[i]!=null)
 						list2[j++] = list[i];
 				}
  				list = list2;
  			}
			if (sortFileNames)
				list = sortFileList(list);

			if (n<1)
				n = list.length;
			if (start<1 || start>list.length)
				start = 1;
			if (start+n-1>list.length)
				n = list.length-start+1;
			int count = 0;
			int counter = 0;
			ImagePlus imp = null;
			for (int i=start-1; i<list.length; i++) {
				if ((counter++%increment)!=0)
					continue;
				Opener opener = new Opener();
				opener.setSilentMode(true);
				IJ.redirectErrorMessages();
				if (!virtualStack||stack==null)
					imp = opener.openImage(directory, list[i]);
				if (imp!=null && stack==null) {
					width = imp.getWidth();
					height = imp.getHeight();
					depth = imp.getStackSize();
					bitDepth = imp.getBitDepth();
					cal = imp.getCalibration();
					bitDepth = 8;
					ColorModel cm = imp.getProcessor().getColorModel();
					if (virtualStack) {
						stack = new VirtualStack(width, height, cm, directory);
						((VirtualStack)stack).setBitDepth(bitDepth);
					}
					else
						stack = new ImageStack(width, height, cm);
					info1 = (String)imp.getProperty("Info");
				}
				if (imp==null)
					continue;
				if (imp.getWidth()!=width || imp.getHeight()!=height) {
					IJ.log(list[i] + ": wrong size; "+width+"x"+height+" expected, "+imp.getWidth()+"x"+imp.getHeight()+" found");
					continue;
				}
				String label = imp.getTitle();
				if (depth==1) {
					String info = (String)imp.getProperty("Info");
					if (info!=null)
						label += "\n" + info;
				}
				if (imp.getCalibration().pixelWidth!=cal.pixelWidth)
					allSameCalibration = false;
				ImageStack inputStack = imp.getStack();
				for (int slice=1; slice<=inputStack.getSize(); slice++) {
					ImageProcessor ip = inputStack.getProcessor(slice);
					int bitDepth2 = imp.getBitDepth();
					if (!virtualStack) {
						ip = ip.convertToByte(true);
						bitDepth2 = 8;
						if (bitDepth2!=bitDepth) {
							IJ.log(list[i] + ": wrong bit depth; "+bitDepth+" expected, "+bitDepth2+" found");
							break;
						}
					}
					if (slice==1) count++;
					IJ.showStatus(count+"/"+n);
					IJ.showProgress(count, n);
					if (ip.getMin()<min) min = ip.getMin();
					if (ip.getMax()>max) max = ip.getMax();
					String label2 = label;
					if (depth>1) label2 = null;
					if (virtualStack) {
						if (slice==1) ((VirtualStack)stack).addSlice(list[i]);
					} else
						stack.addSlice(label2, ip);
				}
				if (count>=n)
					break;
				if (IJ.escapePressed())
					{IJ.beep(); break;}
				//System.gc();
			}
		} catch(OutOfMemoryError e) {
			IJ.outOfMemory("Sequence Loader");
			if (stack!=null) stack.trim();
		}
		if (stack!=null && stack.getSize()>0) {
			if (info1!=null && info1.lastIndexOf("7FE0,0010")>0)
				stack = (new DICOM_Sorter()).sort(stack);
			ImagePlus imp2 = new ImagePlus(title, stack);
			if (imp2.getType()==ImagePlus.GRAY16 || imp2.getType()==ImagePlus.GRAY32)
				imp2.getProcessor().setMinAndMax(min, max);
			if (fi==null)
				fi = new FileInfo();
			fi.fileFormat = FileInfo.UNKNOWN;
			fi.fileName = "";
			fi.directory = directory;
			imp2.setFileInfo(fi); // saves FileInfo of the first image
			if (allSameCalibration) {
				// use calibration from first image
				if (cal.pixelWidth!=1.0 && cal.pixelDepth==1.0)
					cal.pixelDepth = cal.pixelWidth;
				if (cal.pixelWidth<=0.0001 && cal.getUnit().equals("cm")) {
					cal.pixelWidth *= 10000.0;
					cal.pixelHeight *= 10000.0;
					cal.pixelDepth *= 10000.0;
					cal.setUnit("um");
				}
				imp2.setCalibration(cal);
			}
			if (imp2.getStackSize()==1 && info1!=null)
				imp2.setProperty("Info", info1);
			imp2.show();
		}
		IJ.showProgress(1.0);
	}
	
	boolean showDialog(ImagePlus imp, String[] list) {
		int fileCount = list.length;
		FolderOpenerDialog gd = new FolderOpenerDialog("Sequence Options", imp, list);
		gd.addNumericField("Liczba zdjêæ:", fileCount, 0);
		gd.addNumericField("Zdjêcie t³a:", 1, 0);
		gd.addNumericField("Co które zdjêcie:", 1, 0);
		gd.addStringField("Nazwa zaczyna siê:", "", 10);
		gd.showDialog();
		if (gd.wasCanceled())
			return false;
		n = (int)gd.getNextNumber();
		start = (int)gd.getNextNumber();
		increment = (int)gd.getNextNumber();
		if (increment<1)
			increment = 1;
		filter = gd.getNextString();
		return true;
	}

	/** Removes names that start with "." or end with ".db". ".txt", ".lut", "roi" or ".pty". */
	public String[] trimFileList(String[] rawlist) {
		int count = 0;
		for (int i=0; i< rawlist.length; i++) {
			String name = rawlist[i];
			if (name.startsWith(".")||name.equals("Thumbs.db")||name.endsWith(".txt")||name.endsWith(".lut")||name.endsWith(".roi")||name.endsWith(".pty"))
				rawlist[i] = null;
			else
				count++;
		}
		if (count==0) return null;
		String[] list = rawlist;
		if (count<rawlist.length) {
			list = new String[count];
			int index = 0;
			for (int i=0; i< rawlist.length; i++) {
				if (rawlist[i]!=null)
					list[index++] = rawlist[i];
			}
		}
		return list;
	}

	/** Sorts the file names into numeric order. */
	public String[] sortFileList(String[] list) {
		int listLength = list.length;
		boolean allSameLength = true;
		int len0 = list[0].length();
		for (int i=0; i<listLength; i++) {
			if (list[i].length()!=len0) {
				allSameLength = false;
				break;
			}
		}
		if (allSameLength)
			{ij.util.StringSorter.sort(list); return list;}
		int maxDigits = 15;		
		String[] list2 = null;	
		char ch;	
		for (int i=0; i<listLength; i++) {
			int len = list[i].length();
			String num = "";
			for (int j=0; j<len; j++) {
				ch = list[i].charAt(j);
				if (ch>=48&&ch<=57) num += ch;
			}
			if (list2==null) list2 = new String[listLength];
			if (num.length()==0) num = "aaaaaa";
			num = "000000000000000" + num; // prepend maxDigits leading zeroes
			num = num.substring(num.length()-maxDigits);
			list2[i] = num + list[i];
		}
		if (list2!=null) {
			ij.util.StringSorter.sort(list2);
			for (int i=0; i<listLength; i++)
				list2[i] = list2[i].substring(maxDigits);
			return list2;	
		} else {
			ij.util.StringSorter.sort(list);
			return list;   
		}	
	}

} // FolderOpener

class FolderOpenerDialog extends GenericDialog {
	ImagePlus imp;
	int fileCount;
 	boolean eightBits;
 	String[] list;
 	boolean isRegex;

	public FolderOpenerDialog(String title, ImagePlus imp, String[] list) {
		super(title);
		this.imp = imp;
		this.list = list;
		this.fileCount = list.length;
	}

	protected void setup() {
		setStackInfo();
	}
	
	public void textValueChanged(TextEvent e) {
 		setStackInfo();
	}

	void setStackInfo() {
		int width = imp.getWidth();
		int height = imp.getHeight();
		int depth = imp.getStackSize();
		int bytesPerPixel = 1;
 		int n = getNumber(numberField.elementAt(0));
		int start = getNumber(numberField.elementAt(1));
		int inc = getNumber(numberField.elementAt(2));
		
		if (n<1) n = fileCount;
		if (start<1 || start>fileCount) start = 1;
		if (start+n-1>fileCount)
			n = fileCount-start+1;
		if (inc<1) inc = 1;
 		TextField tf = (TextField)stringField.elementAt(0);
 		String filter = tf.getText();
		isRegex = true;
 		if (!filter.equals("") && !filter.equals("*")) {
 			int n2 = 0;
			for (int i=0; i<list.length; i++) {
				if (isRegex&&list[i].matches(filter))
					n2++;
				else if (list[i].indexOf(filter)>=0)
					n2++;
			}
			if (n2<n) n = n2;
 		}
		switch (imp.getType()) {
			case ImagePlus.GRAY16:
				bytesPerPixel=2;break;
			case ImagePlus.COLOR_RGB:
			case ImagePlus.GRAY32:
				bytesPerPixel=4; break;
		}
		if (eightBits)
			bytesPerPixel = 1;
		int n2 = ((fileCount-start+1)*depth)/inc;
		if (n2<0) n2 = 0;
		if (n2>n) n2 = n;
		double size = ((double)width*height*n2*bytesPerPixel)/(1024*1024);
	}

	public int getNumber(Object field) {
		TextField tf = (TextField)field;
		String theText = tf.getText();
		double value;
		Double d;
		try {d = new Double(theText);}
		catch (NumberFormatException e){
			d = null;
		}
		if (d!=null)
			return (int)d.doubleValue();
		else
			return 0;
      }

}