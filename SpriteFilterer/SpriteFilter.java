import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

public class SpriteFilter {
	// to spit out errors
	public SpriteFilter() {}
	static final SpriteFilter controller = new SpriteFilter();

	static final int SPRITESIZE = 896 * 32; // invariable lengths
	static final int PALETTESIZE = 0x78; // not simplified to understand the numbers

	static final String HEX = "0123456789ABCDEF"; // HEX values

	// format of snes 4bpp {row (r), bit plane (b)}
	// bit plane 0 indexed such that 1011 corresponds to 0123
	static final int BPPI[][] = {
			{0,0},{0,1},{1,0},{1,1},{2,0},{2,1},{3,0},{3,1},
			{4,0},{4,1},{5,0},{5,1},{6,0},{6,1},{7,0},{7,1},
			{0,2},{0,3},{1,2},{1,3},{2,2},{2,3},{3,2},{3,3},
			{4,2},{4,3},{5,2},{5,3},{6,2},{6,3},{7,2},{7,3}
	};
	static final String[][] FILTERS = {
			{ "Static",
				"Randomizes pixels of specific indices.",
				"Flag accepts HEX values (0-F) of which indices to randomize; defaults to 1-F.\n" +
				"Prefix with '-' to inverse selection."},
			{ "Index swap",
				"Swaps pixel indices to the other end of the palette, ignoring transparent colors"
					+ "; e.g. 0x1 with 0xF, 0x2 with 0xE, etc.",
				null },
			{ "Line shift",
				"Shifts even rows to the right and odd rows to the left by 1 pixel.",
				null },
			{ "Palette shift",
				"Shifts all pixels a specific number of palette indices to the right.",
				"Flag accepts an integer number (decimal) of spaces to shift each index; defaults to 5"},
			{ "Row swap",
				"Swaps even rows with odd rows.",
				null },
			{ "Column swap",
				"Swaps even columns with odd columns.",
				null },
			{ "Buzz swap",
				"Swaps both even and odd rows and columns, simultaneously.",
				null },
			{ "X-Squish",
				"Squishes sprite horizontally.",
				null },
			{ "Y-Squish",
				"Squishes sprite vertically.",
				null },
			};
	public static void main(String[] args) throws IOException {
		// have to have this up here or nimbus overrides everything
		// stupid nimbus
		final JTextPane flagTextInfo = new JTextPane();
		flagTextInfo.setEditable(false);
		flagTextInfo.setHighlighter(null);
		flagTextInfo.setBackground(new Color(214,217,223,255));

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
					| InstantiationException
					| IllegalAccessException e2) {
					// do nothing
			} //end System
		} // end Nimbus

		final JFrame frame = new JFrame("Sprite filtering");
		final Dimension d = new Dimension(600,382);
		final JTextField fileName = new JTextField("");
		final JTextField flags = new JTextField();
		final JButton fileNameBtn = new JButton("SPR file");
		final JButton goBtn = new JButton("Apply");
		final JLabel optlbl = new JLabel("   Flag and filter   ");

		String[] filterNames = new String[FILTERS.length];
		for (int i = 0; i < filterNames.length; i++)
			filterNames[i] = FILTERS[i][0];
		FileNameExtensionFilter sprFilter =
				new FileNameExtensionFilter("Sprite files", new String[] { "spr" });
		final JComboBox<String> options = new JComboBox<String>(filterNames);
		final JPanel frame2 = new JPanel(new BorderLayout());
		final JPanel imgWrap = new JPanel(new BorderLayout());
		final JPanel filtWrap = new JPanel(new BorderLayout());
		final JPanel goWrap = new JPanel(new BorderLayout());
		final JPanel goBtnWrap = new JPanel(new BorderLayout());
		final JPanel bothWrap = new JPanel(new BorderLayout());

		imgWrap.add(fileName,BorderLayout.CENTER);
		imgWrap.add(fileNameBtn,BorderLayout.EAST);
		goWrap.add(flags,BorderLayout.NORTH);
		goWrap.add(options,BorderLayout.EAST);
		goWrap.add(optlbl,BorderLayout.WEST);
		goWrap.add(flagTextInfo,BorderLayout.SOUTH);
		goBtnWrap.add(goBtn, BorderLayout.NORTH);
		frame2.add(flagTextInfo,BorderLayout.CENTER);
		filtWrap.add(goBtnWrap,BorderLayout.CENTER);
		filtWrap.add(goWrap,BorderLayout.WEST);
		bothWrap.add(imgWrap,BorderLayout.NORTH);
		bothWrap.add(filtWrap,BorderLayout.SOUTH);
		frame2.add(bothWrap,BorderLayout.NORTH);
		frame.add(frame2);
		frame.setSize(d);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		frame.setLocation(200,200);

		// file explorer
		final JFileChooser explorer = new JFileChooser();
		// can't clear text due to wonky code
		// have to set a blank file instead
		final File EEE = new File("");
		options.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int option = options.getSelectedIndex();
				String filterText = FILTERS[option][1];
				String flagText = FILTERS[option][2];
				if (flagText == null)
					flagText = "No flag options available for this filter.";
				flagTextInfo.setText(filterText + "\n" + flagText);
			}});
		fileNameBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				explorer.setSelectedFile(EEE);
				explorer.setFileFilter(sprFilter);
				int option = explorer.showOpenDialog(fileNameBtn);
				if (option == JFileChooser.CANCEL_OPTION)
					return;
				String n = "";
				try {
					n = explorer.getSelectedFile().getPath();
				} catch (NullPointerException e) {
					// do nothing
				} finally {
					if (testFileType(n,"spr"))
						fileName.setText(n);
				}
				explorer.removeChoosableFileFilter(sprFilter);
			}});
		goBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String fileN = fileName.getText();
				byte[] curSprite = null;
				try {
					curSprite = readSprite(fileN);
				} catch (IOException e) {
					JOptionPane.showMessageDialog(frame,
							"Error reading sprite",
							"Oops",
							JOptionPane.WARNING_MESSAGE);
					return;
				}

				int filterToken = options.getSelectedIndex();
				byte[][][] eightXeight = sprTo8x8(curSprite);
				eightXeight = filter(eightXeight,filterToken, flags.getText());
				byte[] palette = getPalette(curSprite);

				byte[] fullMap = exportPNG(eightXeight,palette);
				String exportedName = fileN.substring(0,fileN.lastIndexOf('.')) +
						" (" + FILTERS[filterToken][0].toLowerCase() + ").spr";
				try {
					writeSPR(fullMap,exportedName);
				} catch (IOException e) {
					JOptionPane.showMessageDialog(frame,
							"Error writing sprite",
							"Oops",
							JOptionPane.WARNING_MESSAGE);
					return;
				}
				JOptionPane.showMessageDialog(frame,
						"Sprite successfully filtered and written to:\n" + exportedName,
						"YAY",
						JOptionPane.PLAIN_MESSAGE);
			}});

		// random crap to fire an event to update text
		options.getActionListeners()[0].actionPerformed(
				new ActionEvent(options, ActionEvent.ACTION_PERFORMED,"",0,0));
		frame.setVisible(true);
	}

	/**
	 * Reads a sprite file
	 * @throws IOException
	 */
	public static byte[] readSprite(String path) throws IOException {
		File file = new File(path);
		byte[] ret = new byte[(int) file.length()];
		FileInputStream s;
		try {
			s = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			throw e;
		}
		try {
			s.read(ret);
			s.close();
		} catch (IOException e) {
			throw e;
		}

		return ret;
	}
	/**
	 * Takes a sprite and turns it into 896 blocks of 8x8 pixels
	 * @param sprite
	 */
	public static byte[][][] sprTo8x8(byte[] sprite) {
		byte[][][] ret = new byte[896][8][8];

		// current block we're working on, each sized 32
		// start at -1 since we're incrementing at 0mod32
		int b = -1;
		// locate where in interlacing map we're reading from
		int g;
		for (int i = 0; i < SPRITESIZE; i++) {
			// find interlacing index
			g = i%32;
			// increment at 0th index
			if (g == 0)
				b++;
			// row to look at
			int r = BPPI[g][0];
			// bit plane of byte
			int p = BPPI[g][1];

			// byte to unravel
			byte q = sprite[i];

			// run through the byte
			for (int c = 0; c < 8; c++) {
				// AND with 1 shifted to the correct plane
				boolean bitOn = (q & (1 << (7-c))) != 0;
				// if true, OR with that plane in index map
				if (bitOn)
					ret[b][r][c] |= (1 << (p));
			}
		}
		return ret;
	}

	/**
	 * Read palette from last set of data
	 */
	public static byte[] getPalette(byte[] sprite) {
		byte[] pal = new byte[PALETTESIZE];
		int offset = SPRITESIZE;
		for (int i = 0; i < PALETTESIZE; i++)
			pal[i] = sprite[offset+i];
		return pal;
	}

	/*
	 * 
	 * 
	 * Start of filters
	 * 
	 * 
	 * 
	 */
	/**
	 * Apply a filter based on a token.
	 * @param img - image map to screw up
	 * @param c - filter token
	 * @param f - flag
	 */
	public static byte[][][] filter(byte[][][] img, int c, String f) {
		byte[][][] ret = img.clone();
		switch(c) {
			case 0 :
				ret = staticFilter(ret, f);
				break;
			case 1 :
				ret = swapFilter(ret);
				break;
			case 2 :
				ret = lineShiftFilter(ret);
				break;
			case 3 :
				ret = palShiftFilter(ret, f);
				break;
			case 4 :
				ret = rowSwapFilter(ret);
				break;
			case 5 :
				ret = columnSwapFilter(ret);
				break;
			case 6 :
				ret = buzzSwapFilter(ret);
				break;
			case 7 :
				ret = squishFilter(ret);
				break;
			case 8 :
				ret = squashFilter(ret);
				break;
		}

		return ret;
	}

	/**
	 * Randomizes all desired pixels.
	 * @param img
	 */
	public static byte[][][] staticFilter(byte[][][] img, String f) {
		// default
		if (f.equals("")) {
			f = "-0";
		}
		// check if we're inversed
		boolean inversed = false;
		try {
			inversed = f.charAt(0) == '-';
		} catch (Exception e) {
			// do nothing
		}
		// clear all non HEX values
		f = f.toUpperCase();
		f = f.replaceAll("[^0-9A-F]", "");

		// default to all but trans pixel
		// find which hex numbers exist in flags
		boolean[] randomize = new boolean[16];
		for (int i = 0; i < HEX.length(); i++)
			if (f.indexOf(HEX.charAt(i)) != -1)
				randomize[i] = true;
		if (inversed)
			for (int i = 0; i < randomize.length; i++)
				randomize[i] = !randomize[i];

		for (int i = 0; i < img.length; i++)
			for (int j = 0; j < img[0].length; j++)
				for (int k = 0; k < img[0][0].length; k++) {
					if (randomize[img[i][j][k]] == true)
						img[i][j][k] = (byte) (Math.random() * 16);
				}
		return img;
	}

	/**
	 * Swaps indices with the other end; e.g. 0x1 swapped with 0xF, 0x2 swapped with 0xE, etc.
	 * Ignores trans pixels
	 */
	public static byte[][][] swapFilter(byte[][][] img) {
		for (int i = 0; i < img.length; i++)
			for (int j = 0; j < img[0].length; j++)
				for (int k = 0; k < img[0][0].length; k++) {
					if (img[i][j][k] != 0)
						img[i][j][k] = (byte) (16 - img[i][j][k]);
				}
		return img;
	}

	/**
	 * Shifts rows by 1 to the left or right, alternating
	 */
	public static byte[][][] lineShiftFilter(byte[][][] img) {
		for (int i = 0; i < img.length; i++)
			for (int j = 0; j < img[0].length; j++)
				for (int k = 0; k < img[0][0].length; k++) {
					if (j % 2 == 0) {
						if (k != 7)
							img[i][j][7-k] = img[i][j][6-k];
						else
							img[i][j][7-k] = 0;
					} else {
						if (k != 7)
							img[i][j][k] = img[i][j][k+1];
						else
							img[i][j][k] = 0;
					}
				}
		return img;
	}

	/**
	 * Shifts all non transparent indices an integer value to the right, wrapping around;
	 * defaults to 5.
	 * @param img
	 * @param f
	 */
	public static byte[][][] palShiftFilter(byte[][][] img, String f) {
		int wrap = 5;
		try {
			wrap = Integer.parseInt(f, 10);
		} catch (NumberFormatException e) {
			// do nothing
		}
		for (int i = 0; i < img.length; i++)
			for (int j = 0; j < img[0].length; j++)
				for (int k = 0; k < img[0][0].length; k++) {
					if (img[i][j][k] != 0)
						img[i][j][k] = (byte) ((img[i][j][k] + wrap) % 16);
				}
		return img;
	}

	/**
	 * Swap even and odd rows
	 * @param img
	 */
	public static byte[][][] rowSwapFilter(byte[][][] img) {
		byte[][] copy = new byte[8][8];
		for (int i = 0; i < img.length; i++) {
			// copy array, .clone() is stupid
			for (int i2 = 0; i2 < copy.length; i2++) {
				for (int j2 = 0; j2 < copy.length; j2++)
					copy[i2][j2] = img[i][i2][j2];
			}

			for (int j = 0; j < img[0].length; j++) {
				int dir = (j%2) == 0 ? 1 : -1;
				for (int k = 0; k < img[0][0].length; k++) {
					img[i][j][k] = copy[j+dir][k];
				}
			}
		}
		return img;
	}

	/**
	 * Swap even and odd columns
	 * @param img
	 */
	public static byte[][][] columnSwapFilter(byte[][][] img) {
		byte[][] copy = new byte[8][8];
		for (int i = 0; i < img.length; i++) {
			// copy array, .clone() is stupid
			for (int i2 = 0; i2 < copy.length; i2++) {
				for (int j2 = 0; j2 < copy.length; j2++)
					copy[i2][j2] = img[i][i2][j2];
			}

			for (int j = 0; j < img[0].length; j++) {
				for (int k = 0; k < img[0][0].length; k++) {
					int dir = (k%2) == 0 ? 1 : -1;
					img[i][j][k] = copy[j][k+dir];
				}
			}
		}
		return img;
	}

	/**
	 * Swap even and odd rows and columns
	 * @param img
	 */
	public static byte[][][] buzzSwapFilter(byte[][][] img) {
		byte[][] copy = new byte[8][8];
		for (int i = 0; i < img.length; i++) {
			// copy array, .clone() is stupid
			for (int i2 = 0; i2 < copy.length; i2++) {
				for (int j2 = 0; j2 < copy.length; j2++)
					copy[i2][j2] = img[i][i2][j2];
			}

			for (int j = 0; j < img[0].length; j++) {
				int dir =  (j%2) == 0 ? 1 : -1;
				for (int k = 0; k < img[0][0].length; k++) {
					int dir2 = (k%2) == 0 ? 1 : -1;
					img[i][j][k] = copy[j+dir][k+dir2];
				}
			}
		}
		return img;
	}

	/**
	 * Squish horizontally
	 * @param img
	 */
	public static byte[][][] squishFilter(byte[][][] img) {
		for (int i = 0; i < img.length; i++) {
			for (int j = 0; j < img[0].length; j++)
				for (int k = 0; k < img[0][0].length; k++) {
					int dir = (k%2) == 0 ? 1 : -1;
					img[i][j][k] = img[i][j][k+dir];
				}
		}
		return img;
	}

	/**
	 * Squish vertically
	 * @param img
	 */
	public static byte[][][] squashFilter(byte[][][] img) {
		for (int i = 0; i < img.length; i++) {
			for (int j = 0; j < img[0].length; j++) {
				int dir = (j%2) == 0 ? 1 : -1;
				for (int k = 0; k < img[0][0].length; k++) {
					img[i][j][k] = img[i][j+dir][k];
				}
			}
		}
		return img;
	}

	/*
	 * 
	 * 
	 * End of filters
	 * 
	 * 
	 * 
	 */

	/**
	 * Converts an index map into a proper 4BPP (SNES) byte map.
	 * @param eightbyeight - color index map
	 * @param pal - palette
	 * @param rando - palette indices to randomize
	 * @return new byte array in SNES4BPP format
	 */
	public static byte[] exportPNG(byte[][][] eightbyeight, byte[] palData) {
		// bit map
		boolean[][][] fourbpp = new boolean[896][32][8];

		for (int i = 0; i < fourbpp.length; i++) {
			// each byte, as per bppi
			for (int j = 0; j < fourbpp[0].length; j++) {
				for (int k = 0; k < 8; k++) {
					// get row r's bth bit plane, based on index j of bppi
					int row = BPPI[j][0];
					int plane = BPPI[j][1];
					int byteX = eightbyeight[i][row][k];
					// AND the bits with 1000, 0100, 0010, 0001 to get bit in that location
					boolean bitB = ( byteX & (1 << plane) ) > 0;
					fourbpp[i][j][k] = bitB;
				}
			}
		}

		// byte map
		// includes the size of the sheet (896*32) + palette data (0x78)
		byte[] bytemap = new byte[SPRITESIZE+PALETTESIZE];

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
		int i = SPRITESIZE;
		for (byte b : palData) {
			if (i == bytemap.length)
				break;
			bytemap[i] = b;
			i++;
		}
		return bytemap;
	}

	/**
	 * Writes the image to an <tt>.spr</tt> file.
	 * @param map - SNES 4BPP file, including 5:5:5
	 * @param loc - File path of exported sprite
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
	/*
	 * GUI related functions
	 */
	/**
	 * gives file extension name from a string
	 * @param s - test case
	 * @return extension type
	 */
	public static String getFileType(String s) {
		String ret = s.substring(s.lastIndexOf(".") + 1);
		return ret;
	}

	/**
	 * Test a file against multiple extensions.
	 * The way <b>getFileType</b> works should allow
	 * both full paths and lone file types to work.
	 * 
	 * @param s - file name or extension
	 * @param type - list of all extensions to test against
	 * @return <tt>true</tt> if any extension is matched
	 */
	public static boolean testFileType(String s, String[] type) {
		boolean ret = false;
		String filesType = getFileType(s);
		for (String t : type) {
			if (filesType.equalsIgnoreCase(t)) {
				ret = true;
				break;
			}
		}
		return ret;
	}

	/**
	 * Test a file against a single extension.
	 * 
	 * @param s - file name or extension
	 * @param type - extension
	 * @return <tt>true</tt> if extension is matched
	 */
	public static boolean testFileType(String s, String type) {
		return testFileType(s, new String[] { type });
	}
}