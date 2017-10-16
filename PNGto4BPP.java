import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.DataBufferByte;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
 
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
public class PNGto4BPP {
 
 
	// to spit out errors
	public PNGto4BPP() {}
	static final PNGto4BPP controller = new PNGto4BPP();
	 
	//static final FileFilter imageFilter = FileFilter.;
	public static void main(String[] args) {
		//try to set Nimbus
		try {
			NimbusLookAndFeel lookAndFeel = new NimbusLookAndFeel();
			UIManager.setLookAndFeel(lookAndFeel);
		} catch (UnsupportedLookAndFeelException e) {
			// try to set System default
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (UnsupportedLookAndFeelException
					| ClassNotFoundException
					|InstantiationException
					| IllegalAccessException e2) {
					// nothing 
			} //end System
		} // end Nimbus
		final JFrame frame = new JFrame("PNG to SNES 4BPP");
		final Dimension d = new Dimension(600,382);
		final Dimension d2 = new Dimension(9900,882);
		final JTextField imageName = new JTextField("");
		final JTextField palName = new JTextField("");
		final JTextField fileName = new JTextField("");
 
		// buttons
		final JButton imageBtn = new JButton("PNG file");
		final JButton palBtn = new JButton("Pal file");
		final JButton fileNameBtn = new JButton("File name");
		final JButton runBtn = new JButton("Convert");
 
		// file explorer
		final JFileChooser explorer = new JFileChooser();
		explorer.setSize(d2);
		// set filters
		FileNameExtensionFilter imgFilter =
				new FileNameExtensionFilter("PNG files", "png");
		FileNameExtensionFilter palFilter =
				new FileNameExtensionFilter("Palette files", "gpl", "pal", "txt");
		FileNameExtensionFilter sprFilter =
				new FileNameExtensionFilter("Sprite files", "spr");
	 
		explorer.setAcceptAllFileFilterUsed(false);
		 
		String[] palChoices = {"Read an ASCII (.GPL/.PAL)","Binary (YY .PAL)","Extract from the last block of PNG"};
		final JComboBox<String> palOptions = new JComboBox<String>(palChoices);
		final JPanel frame2 = new JPanel(new BorderLayout());
		final JPanel imgPalWrapper = new JPanel(new BorderLayout());
		final JPanel imgNWrapper = new JPanel(new BorderLayout());
		final JPanel palNWrapper = new JPanel(new BorderLayout());
		final JPanel palBtnWrapper = new JPanel(new BorderLayout());
		final JPanel fileNWrapper = new JPanel(new BorderLayout());
		final JPanel allWrapper = new JPanel(new BorderLayout());
 
		imageName.setEditable(false);
		palName.setEditable(false);
		
		// add image button and field
		imgNWrapper.add(imageName,BorderLayout.CENTER);
		imgNWrapper.add(imageBtn,BorderLayout.EAST);
		imgPalWrapper.add(imgNWrapper,BorderLayout.NORTH);
 
		// add palette button and field
		palNWrapper.add(palName,BorderLayout.CENTER);
		palBtnWrapper.add(palBtn,BorderLayout.WEST);
		palBtnWrapper.add(palOptions,BorderLayout.EAST);
		palNWrapper.add(palBtnWrapper,BorderLayout.EAST);
		imgPalWrapper.add(palNWrapper,BorderLayout.SOUTH);
 
		// add new file button and field
		fileNWrapper.add(fileName,BorderLayout.CENTER);
		fileNWrapper.add(fileNameBtn,BorderLayout.EAST);
 
		// add run button
		fileNWrapper.add(runBtn,BorderLayout.SOUTH);
		allWrapper.add(imgPalWrapper,BorderLayout.NORTH);
		allWrapper.add(fileNWrapper,BorderLayout.SOUTH);

		// add wrappers
		frame2.add(allWrapper,BorderLayout.NORTH);
		frame.add(frame2);
		frame.setSize(d);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		frame.setLocation(200,200);
		frame.setVisible(true);
 
		// image button
		imageBtn.addActionListener(new ActionListener() {
 
			public void actionPerformed(ActionEvent arg0) {
				explorer.setFileFilter(imgFilter);
				explorer.showOpenDialog(imageBtn);
				String n = "";
				try {
					n = explorer.getSelectedFile().getPath();
 
				} catch (NullPointerException e) {
 
				} finally {
					imageName.setText(n);
				}
				explorer.removeChoosableFileFilter(imgFilter);
			}});
		
		// palette button
		palBtn.addActionListener(new ActionListener() {
 
			public void actionPerformed(ActionEvent arg0) {
				explorer.setFileFilter(palFilter);
				explorer.showOpenDialog(palBtn);
				String n = "";
				try {
					n = explorer.getSelectedFile().getPath();
 
				} catch (NullPointerException e) {
 
				} finally {
					palName.setText(n);
				}
				explorer.removeChoosableFileFilter(palFilter);
			}});
 
		// file name button
		fileNameBtn.addActionListener(new ActionListener() {
 
			public void actionPerformed(ActionEvent arg0) {
				explorer.setFileFilter(sprFilter);
				explorer.showOpenDialog(fileNameBtn);
				String n = "";
				try {
					n = explorer.getSelectedFile().getPath();
 
				} catch (NullPointerException e) {
 
				} finally {
					fileName.setText(n);
				}
				explorer.removeChoosableFileFilter(sprFilter);
			}});
		
		// run button
		runBtn.addActionListener(new ActionListener() {
 
			public void actionPerformed(ActionEvent arg0) {
				byte[] rando = new byte[16];
				for (int i = 0; i < rando.length; i++) {
					rando[i] = -1;
				}
				BufferedImage img;
				BufferedImage imgRead;
				byte[] pixels;
				File imageFile = new File(imageName.getText());
				BufferedReader br;
				int[] palette = null;
				byte[] palData = null;
				byte[][][] eightbyeight;
				// image file
				try {
					imgRead = ImageIO.read(imageFile);
				} catch (FileNotFoundException e) {
					JOptionPane.showMessageDialog(frame,
							"Image file not found",
							"Oops",
							JOptionPane.WARNING_MESSAGE);
					return;
				} catch (IOException e) {
					JOptionPane.showMessageDialog(frame,
							"Error reading image",
							"Oops",
							JOptionPane.WARNING_MESSAGE);
					return;
				}
 
				// convert to RGB colorspace
				try {
					img = convertToABGR(imgRead);
				} catch (FailedConvertException e) {
					JOptionPane.showMessageDialog(frame,
							"Error converting colorspace",
							"Oops",
							JOptionPane.WARNING_MESSAGE);
					return;
				}				
				
				// image raster
				try {
					pixels = getImageRaster(img);
				} catch (BadDimensionsException e) {
					JOptionPane.showMessageDialog(frame,
							"Image dimensions must be 128x448",
							"Oops",
							JOptionPane.WARNING_MESSAGE);
					return;
				}
				 
				// see which palette method we're using
				int palChoice = palOptions.getSelectedIndex();
				 
				// explicit ASCII palette
				if (palChoice == 0) {
					// palette file
					try {
						br = getPaletteFile(palName.getText());
					} catch (FileNotFoundException e) {
						JOptionPane.showMessageDialog(frame,
								"Palette file not found",
								"Oops",
								JOptionPane.WARNING_MESSAGE);
						return;
					}
					String palExt = palName.getText().substring(palName.getText().length()-4);
					// palette parsing
					try {
						if (palExt.toLowerCase() == "txt")
							palette = getPaletteColorsFromPaintNET(br);
						else
							palette = getPaletteColorsFromFile(br);
						palData = palDataFromArray(palette);
					} catch (NumberFormatException|IOException e) {
						JOptionPane.showMessageDialog(frame,
								"Error reading palette",
								"Oops",
								JOptionPane.WARNING_MESSAGE);
						return;
					} catch (ShortPaletteException e) {
						JOptionPane.showMessageDialog(frame,
								"Unable to find 16 colors",
								"Oops",
								JOptionPane.WARNING_MESSAGE);
						return;
					}
				}
				 
				if (palChoice == 1) {
					JOptionPane.showMessageDialog(frame,
							"Binary .PAL file reading not available yet\nWatch this space :thinking:",
							"Oops",
							JOptionPane.WARNING_MESSAGE);
					return;
				}
				if (palChoice == 2) {
					palette = palExtract(pixels);
					palData = palDataFromArray(palette);
				}
				// save location
				String loc = fileName.getText();
				boolean bamboozled = false;
				boolean[] bamboozarino = new boolean[16];
				if (loc.toLowerCase().matches("bamboozle:\\s*[0-9a-f]+")) {
					bamboozled = true;
					loc = loc.replace("bamboozle:","");
					String HEX = "0123456789ABCDEF";
					for (int i = 0; i < HEX.length(); i++) {
						char a = HEX.charAt(i);
						if (loc.indexOf(a) != -1)
							bamboozarino[i] = true;
					}
					
					for (int i = 0; i < bamboozarino.length; i++) {
						if (bamboozarino[i])
							rando[i] = (byte) i;
					}
					loc = "";
				}
				
				// default name
				if (loc.equals("")) {
					loc = imageName.getText();
					loc = loc.substring(0,loc.length()-4);
					loc += " (" + (bamboozled ? "bamboozled" : "exported") + ").spr";
				}
				
				// only allow sprite files
				String locExt = loc.substring(loc.lastIndexOf(".") + 1);
				if (!locExt.toLowerCase().equals("spr")) {
					JOptionPane.showMessageDialog(frame,
							"Sprites must be an .spr file",
							"Oops",
							JOptionPane.WARNING_MESSAGE);
					return;
				}
					
				// make the file
				try {
					new File(loc);
				} catch (NullPointerException e) {
					JOptionPane.showMessageDialog(frame,
							"Invalid file name",
							"Oops",
							JOptionPane.WARNING_MESSAGE);
				}
 
				eightbyeight = get8x8(pixels, palette);
				
				// write data to SPR file
				byte[] SNESdata = exportPNG(eightbyeight, palData, rando);
				try {
					writeSPR(SNESdata, loc);
				} catch (IOException e) {
					JOptionPane.showMessageDialog(frame,
							"Error writing sprite",
							"Oops",
							JOptionPane.WARNING_MESSAGE);
					return;
				}
				JOptionPane.showMessageDialog(frame,
						"Sprite file successfully written to " + (new File(loc).getName()),
						"Oops",
						JOptionPane.PLAIN_MESSAGE);
			}});
	}
 
	/**
	 * Finds the palette file (as a .gpl or .pal) from <tt>palPath<tt>
	 * @param palPath - full file path of the palette
	 * @throws FileNotFoundException
	 */
	public static BufferedReader getPaletteFile(String palPath)
			throws FileNotFoundException {
		FileReader pal = new FileReader(palPath);
		BufferedReader ret = new BufferedReader(pal);
		return ret;
	}
 
	/**
	 * Converts to RGB colorspace
	 */
	@SuppressWarnings("unused")
	public static BufferedImage convertToABGR(BufferedImage img) throws FailedConvertException {
		BufferedImage ret = null;
		
		ret = new BufferedImage(img.getWidth(),img.getHeight(),BufferedImage.TYPE_4BYTE_ABGR);
		ColorConvertOp rgb = new ColorConvertOp(null);
		rgb.filter(img,ret);
		
		if (ret == null)
			throw controller.new FailedConvertException();
		return ret;
	}
	
	/**
	 * Parses the palette file passed and 
	 * @param pal
	 * @return The palette as an array of integers of the format RRRGGGBBB
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	public static int[] getPaletteColorsFromFile(BufferedReader pal)
			throws NumberFormatException, IOException, ShortPaletteException {
		int[] palret = new int[64];
		String line;
 
		// read palette
		int pali = 0;
		while ( (line = pal.readLine()) != null) {
			// look with 3 numbers
			String[] line2 = (line.trim()).split("\\D+");
			int colori = 0;
			int[] colorArray = new int[3];
			if (line2.length >= 3) {
				for (String s : line2) {
					int curCol = -1;
					try {
						curCol = Integer.parseInt(s);
					} catch(NumberFormatException e) {
						// nothing
					} finally {
						colorArray[colori] = curCol;
						colori++;
					}
					if (colori == 3)
						break;
				}
				// read RGB bytes as ints
				int r = colorArray[0];
				int g = colorArray[1];
				int b = colorArray[2];
				palret[pali] = (r * 1000000) + (g * 1000) + b; // add to palette as RRRGGGBBB
				pali++; // increment palette index
			}
			if (pali == 64)
				break;
		}
		// short palettes throw an error
		if (pali < 16 )
			throw controller.new ShortPaletteException();
		// truncate long palettes
		int[] newret = new int[64];
		pali = 16 * (pali / 16);
		if (pali > 64)
			pali = 64;
		for (int i = 0; i < pali; i++)
			newret[i] = palret[i];
		if (pali < 64)
			for (int i = pali; i < 64; i++)
				newret[i] = palret[i%16];
		return newret;
	}
	
	/**
	 * Reads a text file for palette
	 * Has to be different since Paint.NET uses HEX in its palette files
	 */
	public static int[] getPaletteColorsFromPaintNET(BufferedReader pal)
			throws NumberFormatException, IOException, ShortPaletteException {
		int[] palret = new int[64];
		String line;
		 
		// read palette
		int pali = 0;
		while ( (line = pal.readLine()) != null) {
			if (line.matches("[0-9A-F] {8}")) {
				char[] line2 = line.toCharArray();
				// read RGB bytes as ints
				int r = Integer.parseInt( ("" + line2[2] + line2[3]), 16);
				int g = Integer.parseInt( ("" + line2[4] + line2[5]), 16);
				int b = Integer.parseInt( ("" + line2[6] + line2[7]), 16);
				palret[pali] = (r * 1000000) + (g * 1000) + b; // add to palette as RRRGGGBBB
				pali++; // increment palette index
			}
		if (pali == 64)
			break;
		}
		 
		// Paint.NET forces 96 colors, but put this here just in case
		if (pali < 16 )
			throw controller.new ShortPaletteException();
		// truncate long palettes
		int[] newret = new int[64];
		pali = 16 * (pali / 16);
		if (pali > 64)
			pali = 64;
		for (int i = 0; i < pali; i++)
			newret[i] = palret[i];
		if (pali < 64)
			for (int i = pali; i < 64; i++)
				newret[i] = palret[i%16];
		return palret;
	}
	
	/**
	 * Get the full image raster
	 * @param img
	 * @return
	 * @throws BadDimensionsException
	 */
	public static byte[] getImageRaster(BufferedImage img) throws BadDimensionsException {
		int w = img.getWidth();
		int h = img.getHeight();
		if (w != 128 || h != 448) {
			throw controller.new BadDimensionsException();
		}
		byte[] pixels = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
		return pixels;
	}
 
	/**
	 * writes the .spr file based on the bytemap and file location
	 */
	public static void writeSPR(byte[] map, String loc) throws IOException {
		// create a file at directory
		new File(loc);
 
		FileOutputStream fileOuputStream = new FileOutputStream(loc);
		try { 
			fileOuputStream.write(map);
		} finally {
			fileOuputStream.close();
		}
	}
 
	/**
	 * Create binary palette data for appending at the end of the sprite
	 * Also fills in empty mail rows with the green mail colors
	 * @param pal
	 * @return
	 */
	public static byte[] palDataFromArray(int[] pal) {
		// create palette data as 5:5:5
		ByteBuffer palRet = ByteBuffer.allocate(0x80);
 
		for (int i = 0; i < 16; i++) {
			for (int t = 0; t < 4; t++) {
				int r = pal[i+16*t] / 1000000;
				int g = (pal[i+16*t] % 1000000) / 1000;
				int b = pal[i+16*t] % 1000;
				short s = (short) ((( b / 8) << 10) | ((( g / 8) << 5) | ((( r / 8) << 0))));
				// put color into every mail palette
				palRet.putShort(30*t+i*2,Short.reverseBytes(s));
			}
		}
		// end palette
		return palRet.array();
	}
	
	/**
	 * Method for extracting palette coors from the last 8x8 square of the file
	 * @param pixels
	 * @return
	 */
	public static int[] palExtract(byte[] pixels) {
		int[] palret = new int[64];
		int pali = 0;
		int startAt = (128 * 448 - 8) - (128 * 7);
		int endAt = startAt + (8 * 128);
		for (int i = startAt; i < endAt; i+= 128) {
			for (int j = 0; j < 8; j++) {
				int k = i + j;
				int b = (pixels[k*4+1]+256)%256;
				int g = (pixels[k*4+2]+256)%256;
				int r = (pixels[k*4+3]+256)%256;
				palret[pali] = (1000000 * r) + (1000 * g) + b;
				pali++;
			}
		}
		return palret;
	}
 
	/**
	 * Turn the image into an array of 8x8 blocks
	 * Assumes ABGR color space
	 * @param pixels
	 * @param pal
	 * @return
	 */
	public static byte[][][] get8x8(byte[] pixels, int[] pal) {
		int dis = pixels.length/4;
		int largeCol = 0;
		int intRow = 0;
		int intCol = 0;
		int index = 0;
 
		// all 8x8 squares, read left to right, top to bottom
		byte[][][] eightbyeight = new byte[896][8][8];
 
		// read image
		for (int i = 0; i < dis; i++) {
			// get each color and get rid of sign
			// colors are stored as {A,B,G,R,A,B,G,R...}
			int b = (pixels[i*4+1]+256)%256;
			int g = (pixels[i*4+2]+256)%256;
			int r = (pixels[i*4+3]+256)%256;
 
			// convert to 9 digits
			int rgb = (1000000 * r) + (1000 * g) + b;
 
			// find palette index of current pixel
			for (int s = 0; s < pal.length; s++) {
				   if (pal[s] == rgb) {
					eightbyeight[index][intRow][intCol] = (byte) (s % 16); // mod 16 in case it reads another mail
					break;
				}
			}
 
			// count up square by square
			// at 8, reset the "Interior column" which we use to locate the pixel in 8x8
			// increments the "Large column", which is the index of the 8x8 sprite on the sheet
			// at 16, reset the index and move to the next row
			// (so we can wrap around back to our old 8x8)
			// after 8 rows, undo the index reset, and move on to the next super row
			intCol++;
			if (intCol == 8) {
				index++;
				largeCol++;
				intCol = 0;
				if (largeCol == 16) {
					index -= 16;
					largeCol = 0;
					intRow++;
					if (intRow == 8) {
						index += 16;
						intRow = 0;
					}
				}
			}
		}
		return eightbyeight;
	}
	
	/**
	 * 
	 * @param pixels byte array of pixels as indeices
	 * @param pal int array of palette
	 * @param rando byte array of palettes to 
	 * @return new byte array in SNES4BPP format
	 */
	public static byte[] exportPNG(byte[][][] eightbyeight, byte[] palData, byte[] rando) {
		// why is this here
		// randomize desired indices
		for (int i = 0; i < eightbyeight.length; i++) {
			for (int j = 0; j < eightbyeight[0].length; j++) {
				for (int k = 0; k < eightbyeight[0][0].length; k++) {
					for (byte a : rando) {
						if (eightbyeight[i][j][k] == a) {
							eightbyeight[i][j][k] = (byte) (Math.random() * 16);
						}
					}
				}
			}
		}
 
		// format of snes 4bpp {row (r), bit plane (b)}
		// bit plane 0 indexed such that 1011 corresponds to 0123
		int bppi[][] = {
				{0,0},{0,1},{1,0},{1,1},{2,0},{2,1},{3,0},{3,1},
				{4,0},{4,1},{5,0},{5,1},{6,0},{6,1},{7,0},{7,1},
				{0,2},{0,3},{1,2},{1,3},{2,2},{2,3},{3,2},{3,3},
				{4,2},{4,3},{5,2},{5,3},{6,2},{6,3},{7,2},{7,3}
		};
 
		boolean[][][] fourbpp = new boolean[896][32][8];
 
		for (int i = 0; i < fourbpp.length; i++) {
			// each byte, as per bppi
			for (int j = 0; j < fourbpp[0].length; j++) {
				for (int k = 0; k < 8; k++) {
					// get row r's bth bit plane, based on index j of bppi
					int row = bppi[j][0];
					int plane = bppi[j][1];
					int byteX = eightbyeight[i][row][k];
					// AND the bits with 1000, 0100, 0010, 0001 to get bit in that location
					boolean bitB = ( byteX & (1 << plane) ) > 0;
					fourbpp[i][j][k] = bitB;
				}
			}
		}
 
		// byte map
		// includes the size of the sheet (896*32) + palette data (0x78)
		byte[] bytemap = new byte[896*32+0x78];
 
		int k = 0;
		for (int i = 0; i < fourbpp.length; i++) {
			for (int j = 0; j < fourbpp[0].length; j++) {
				byte next = 0;
				// turn true false into byte
				for (boolean a : fourbpp[i][j]) {
					next <<= 1;
					next |= (a ? 1 : 0);
				}
				bytemap[k] = next;
				k++;
			}
		}
		// end 4BPP
 
		// add palette data, starting at end of sheet
		int i = 896*32-2;
		for (byte b : palData) {
			if (i == bytemap.length)
				break;
			bytemap[i] = b;
			i++;
		}
		return bytemap;
	}
	 
	// errors
	
	// Return image is null
	public class FailedConvertException extends Exception {
		private static final long serialVersionUID = 1L;
		public FailedConvertException(String message) {
			super(message);
		}
 
		public FailedConvertException() {}
	}
	
	// Palette has <16 colors
	public class ShortPaletteException extends Exception {
		private static final long serialVersionUID = 1L;
		public ShortPaletteException(String message) {
			super(message);
		}
 
		public ShortPaletteException() {}
	}
 
	// Image is wrong dimensions
	public class BadDimensionsException extends Exception {
		private static final long serialVersionUID = 1L;
		public BadDimensionsException(String message) {
			super(message);
		}
 
		public BadDimensionsException() {}
	}
}