import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

public class SpriteAnimator extends Component {
	private static final long serialVersionUID = 2114886855236406900L;

	static final int SPRITESIZE = 896 * 32; // invariable lengths
	static final int PALETTESIZE = 0x78; // not simplified to understand the numbers
	static final int RASTERSIZE = 128 * 448 * 4;

	// used for parsing frame data
	static final String ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZαβ".toUpperCase(); // to uppercase to distinguish alpha/beta
	// format of snes 4bpp {row (r), bit plane (b)}
	// bit plane 0 indexed such that 1011 corresponds to 0123
	static final int BPPI[][] = {
			{0,0},{0,1},{1,0},{1,1},{2,0},{2,1},{3,0},{3,1},
			{4,0},{4,1},{5,0},{5,1},{6,0},{6,1},{7,0},{7,1},
			{0,2},{0,3},{1,2},{1,3},{2,2},{2,3},{3,2},{3,3},
			{4,2},{4,3},{5,2},{5,3},{6,2},{6,3},{7,2},{7,3}
	};

	/* taken and modified from
	 * http://alttp.mymm1.com/sprites/includes/animations.txt
	 * credit: mike trethewey
	 */
	static final String[] ANIMNAMES = {
		"stand", "standUp", "standDown", "walk", "walkUp", "walkDown", "bonk", "bonkUp", "bonkDown",
		"swim", "swimUp", "swimDown", "swimFlap", "treadingWater", "treadingWaterUp", "treadingWaterDown",
		"attack", "attackUp", "attackDown", "dashRelease", "dashReleaseUp", "dashReleaseDown",
		"spinAttack", "spinAttackLeft", "spinAttackUp", "spinAttackDown",
		"dashSpinup", "dashSpinupUp", "dashSpinupDown", "salute", "itemGet", "triforceGet",
		"readBook", "fall", "grab", "grabUp", "grabDown", "lift", "liftUp", "liftDown",
		"carry", "carryUp", "carryDown", "treePull", "throw", "throwUp", "throwDown",
		"push", "pushUp", "pushDown", "shovel", "boomerang", "boomerangUp", "boomerangDown",
		"rod", "rodUp", "rodDown", "powder", "powderUp", "powderDown", "cane", "caneUp", "caneDown",
		"bow", "bowUp", "bowDown", "bombos", "ether", "quake", "hookshot", "hookshotUp", "hookshotDown",
		"zap", "bunnyStand", "bunnyStandUp", "bunnyStandDown", "bunnyWalk", "bunnyWalkUp", "bunnyWalkDown",
		"walkDownstairs2F", "walkDownstairs1F", "walkUpstairs1F", "walkUpstairs2F",
		"deathSpin", "deathSplat", "poke", "pokeUp", "pokeDown", "tallGrass", "tallGrassUp", "tallGrassDown",
		"mapDungeon", "mapWorld", "sleep", "awake"
		};

	/* 
	 * format:
	 * <INDEX>{<XPOS>,<YPOS>}{<SPRITESIZE>}{<TRANSFORM>}
	 * : delimits sprites in the same frame
	 * ; delimits entire frames
	 * SPRITESIZE is a flag determining what part of the sprite to draw from
	 *		F  : Full 16x16
	 *		T  : Top 16x8
	 *		B  : Bottom 16x8
	 *		R  : Right 8x16
	 *		L  : Left 8x16
	 *		TR : Top-right 8x8 ; alias : turtle rock
	 *		TL : Top-left 8x8
	 *		BR : Bottom-right 8x8
	 *		BL : Bottom-left 8x8
	 * TRANSFORM is a flag determining how to flip the sprite
	 *		0  : No transform
	 *		U  : Mirror along X-axis
	 *		M  : Mirror along y-axis
	 *		UM : Mirror along both axes
	 */
	static final String[] ALLFRAMES = {
			/* Bunny walk Down */ "Z5{0,8}{F}{0}:α0{0,16}{F}{0} ; Z5{0,9}{F}{0}:Z7{0,16}{F}{0}"
	};

	/*
	 * GUI stuff
	 */
	private static final JComboBox<String> animOptions = new JComboBox<String>(ANIMNAMES);

	static final String[] MODES = {
			"Normal play",
			"Step-by-step",
			"All frames"
	};

	private static final JComboBox<String> modeOptions = new JComboBox<String>(MODES);
	/*
	 * Image controller
	 */
	private BufferedImage img = null; // sprite sheet
	private int anime; // animation id
	private int speed; // speed; 0 = normal; positive = faster; negative = slower
	private int mode; // animation mode
	private int frame;
	private int maxFrame;
	private boolean running;
	private Sprite[][] frames = null;
	private Timer tick;
	private static final int MAXSPEED = 5; // maximum speed magnitude
	// default initialization
	public SpriteAnimator() {
		anime = 0;
		speed = 0;
		mode = 0;
		frame = 0;
		maxFrame = 0;
		running = true;
		tick = new Timer(100, new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (isRunning())
					step();
			}
		});
	}

	/**
	 * Set image to animate.
	 * @param image
	 */
	public void setImage(BufferedImage image) {
		img = image;
	}

	/**
	 * Set animation ID.
	 * @param id
	 */
	public void setAnimation(int id) {
		if (img == null)
			return;
		anime = id;
		makeAnimationFrames();
		reset();
	}
	
	/**
	 * Get animation mode ID#.
	 */
	public int getMode() {
		return mode;
	}
	/**
	 * Set image mode and reset.
	 * <ul style="list-style:none">
	 * <li><b>0</b> - normal animation</li>
	 * <li><b>1</b> - step-by-step</li>
	 * <li><b>2</b> - all frames</li>
	 * </ul>
	 * @param m - mode 
	 */
	public void setMode(int m) {
		mode = m;
		reset();
	}
	
	/**
	 * Step forward 1 animation frame.
	 * Resets frame to 0 if we reach the end in modes that loop.
	 * Stops running if we reach the end of the animation in "All frames" mode.
	 * @return Frame # painted
	 */
	public int step() {
		int ret = frame;
		frame++;
		if (frame >= maxFrame) {
			frame = 0;
			if (mode == 2)
				setRunning(false);
		}
		repaint();
		return ret;
	}

	/**
	 * Reset based on mode.
	 */
	public void reset() {
		switch (mode) {
			case 0 :
				resetFrame();
				resetSpeed();
				setRunning(true);
				break;
			case 1 :
				resetFrame();
				setRunning(false);
				break;
			case 2 :
				resetFrame();
				resetSpeed();
				setRunning(true);
				break;
		}
		tick.start();
	}
	
	/**
	 * Reset speed to 0.
	 */
	public void resetSpeed() {
		speed = 0;
	}
	
	/**
	 * Resets frame to 0.
	 */
	public void resetFrame() {
		frame = 0;
	}
	
	/**
	 * Control self-animation permission.
	 */
	public void setRunning(boolean r) {
		running = r;
	}
	
	/**
	 * @return <b>true</b> if active.
	 */
	public boolean isRunning() {
		return running;
	}
	/**
	 * @return Timer object
	 */
	public Timer getTimer() {
		return tick;
	}
	/**
	 * Increments step speed by 1.
	 * @return <b>true</b> if speed reaches max.
	 */
	public boolean faster() {
		if (speed < MAXSPEED)
			speed++;
		return atMaxSpeed();
	}
	
	/**
	 * Decrements step speed by 1.
	 * @return <b>true</b> if speed reaches min.
	 */
	public boolean slower() {
		if (speed > (MAXSPEED * -1))
			speed--;
		return atMinSpeed();
	}
	/**
	 * Compares current step speed to maximum speed allowed.
	 */
	public boolean atMaxSpeed() {
		return speed == MAXSPEED;
	}
	/**
	 * Compares current step speed to minimum speed allowed.
	 */
	public boolean atMinSpeed() {
		return speed == (-1 * MAXSPEED);
	}

	/**
	 * Makes an array of {@link Sprite}s based on the frame data.
	 */
	public void makeAnimationFrames() {
		if (img == null)
			return;
		String f = ALLFRAMES[anime].toUpperCase().replace(" ", ""); // CAPS and remove all whitespace
		String[] eachFrame = f.split(";"); // split by frame
		maxFrame = eachFrame.length;
		frames = new Sprite[maxFrame][];
		// each frame
		for (int i = 0; i < maxFrame; i++) {
			String[] eachSprite = eachFrame[i].split(":");
			int spriteCount = eachSprite.length;
			// each sprite in frame
			frames[i] = new Sprite[spriteCount];
			for (int j = 0; j < spriteCount; j++) {
				// split into info sections
				String[] spriteSplit = eachSprite[j].split("[\\{\\}]{1,2}");
				char[] sprIndex = spriteSplit[0].toCharArray();
				String[] pos = spriteSplit[1].split(",");
				String sprSize = spriteSplit[2];
				String sprTrans = spriteSplit[3];
				// sprite position
				int xpos = Integer.parseInt(pos[0]);
				int ypos = Integer.parseInt(pos[1]);
				int drawY = ALPHA.indexOf(sprIndex[0]) * 16;
				System.out.println(sprIndex[0] + " " + sprIndex[0]);
				int drawX = Integer.parseInt((sprIndex[1] + "")) * 16;
				int drawYoffset, drawXoffset, width, height;
				
				// determine offset from initial position
				switch (sprSize) {
					case "F" :
						drawYoffset = 0;
						drawXoffset = 0;
						width = 16;
						height = 16;
						break;
					case "T" :
						drawYoffset = 0;
						drawXoffset = 0;
						width = 16;
						height = 8;
						break;
					case "B" :
						drawYoffset = 8;
						drawXoffset = 0;
						width = 16;
						height = 8;
						break;
					case "R" :
						drawYoffset = 0;
						drawXoffset = 8;
						width = 8;
						height = 16;
						break;
					case "L" :
						drawYoffset = 0;
						drawXoffset = 0;
						width = 8;
						height = 16;
						break;
					case "TR" :
						drawYoffset = 0;
						drawXoffset = 8;
						width = 8;
						height = 8;
						break;
					case "TL" :
						drawYoffset = 0;
						drawXoffset = 0;
						width = 8;
						height = 8;
						break;
					case "BR" :
						drawYoffset = 8;
						drawXoffset = 8;
						width = 8;
						height = 8;
						break;
					case "BL" :
						drawYoffset = 8;
						drawXoffset = 0;
						width = 8;
						height = 8;
						break;
					default :
						drawYoffset = 0;
						drawXoffset = 0;
						width = 16;
						height = 16;
						break;
				}
				drawX += drawXoffset;
				drawY += drawYoffset;
				BufferedImage spreet = img.getSubimage(drawX, drawY, width, height);
				frames[i][j] = new Sprite(spreet, xpos, ypos, j);
			}
		}
	}
	/**
	 * Draw every sprite
	 */
	public void paint(Graphics g) {
		if (frames==null || frames[frame] == null)
			return;
		System.out.println(frame + " " + maxFrame);
		Graphics2D g2 = (Graphics2D) g;
		for(Sprite s : frames[frame])
			s.draw(g2);
	}

	// error controller
	static final SpriteAnimator controller = new SpriteAnimator();
	
	public static void main(String[] args) throws IOException {
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
		final JFrame frame = new JFrame("Sprite animator");
		final Dimension d = new Dimension(600,282);
		final JTextField fileName = new JTextField("");
		final JButton fileNameBtn = new JButton("SPR file");
		final JButton loadBtn = new JButton("Load file");
		final JButton stepBtn = new JButton("Step");
		final JButton fasterBtn = new JButton("Speed+");
		final JButton slowerBtn = new JButton("Speed-");
		final JButton resetBtn = new JButton("Reset");
		final JPanel loadWrap = new JPanel(new BorderLayout());
		final JPanel btnWrap = new JPanel(new BorderLayout());
		final JPanel controls = new JPanel(new BorderLayout());
		final JPanel controls1 = new JPanel(new BorderLayout());
		final JPanel controls2 = new JPanel(new BorderLayout());
		controls1.add(animOptions,BorderLayout.NORTH);
		controls1.add(modeOptions,BorderLayout.SOUTH);
		
		controls2.add(stepBtn,BorderLayout.SOUTH);
		controls2.add(fasterBtn,BorderLayout.EAST);
		controls2.add(slowerBtn,BorderLayout.WEST);
		controls2.add(resetBtn,BorderLayout.CENTER);
		controls.add(controls1,BorderLayout.NORTH);
		controls.add(controls2,BorderLayout.SOUTH);

		final JPanel bottomStuff = new JPanel(new BorderLayout());
		stepBtn.setEnabled(false);

		final SpriteAnimator imageArea = new SpriteAnimator();
		final SpriteAnimator run = imageArea; // just a shorter name
		final Timer tock = run.getTimer();
		bottomStuff.add(imageArea,BorderLayout.CENTER);
		bottomStuff.add(controls,BorderLayout.EAST);
		
		btnWrap.add(fileNameBtn,BorderLayout.WEST);
		btnWrap.add(loadBtn,BorderLayout.EAST);
		loadWrap.add(btnWrap,BorderLayout.EAST);
		loadWrap.add(fileName,BorderLayout.CENTER);

		// Credits
		final JFrame aboutFrame = new JFrame("About");
		final JMenuItem peeps = new JMenuItem("About");
		final TextArea peepsList = new TextArea("", 0,0,TextArea.SCROLLBARS_VERTICAL_ONLY);
		peepsList.setEditable(false);
		peepsList.append("Written by fatmanspanda"); // hey, that's me
		peepsList.append("\n\nFrame resources:\n");
		peepsList.append("http://alttp.mymm1.com/sprites/includes/animations.txt\n");
		peepsList.append(join(new String[]{
				"\tMikeTrethewey", // it's mike
				"TWRoxas", // provided most valuable documentation
				}, ", "));// forced me to do this and falls in every category
		peepsList.append("\n\nCode contribution:\n");
		peepsList.append(join(new String[]{
				"Zarby89", // spr conversion
				}, ", "));
		peepsList.append("\n\nResources and development:\n");
		peepsList.append(join(new String[]{
				"Veetorp", // provided most valuable documentation
				"Zarby89", // various documentation and answers
				"Sosuke3" // various snes code answers
				}, ", "));
		// no one yet
		/*peepsList.append("\n\nTesting and feedback:\n");
		peepsList.append(join(new String[]{
				"",
				}, ", "));*/
		aboutFrame.add(peepsList);
		final JMenuBar menu = new JMenuBar();
		menu.add(peeps);
		peeps.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				aboutFrame.setVisible(true);
			}});
		aboutFrame.setSize(600,300);
		aboutFrame.setResizable(false);
		// end credits

		frame.add(bottomStuff, BorderLayout.CENTER);
		frame.add(loadWrap,BorderLayout.NORTH);
		frame.setSize(d);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		frame.setLocation(300,300);
		frame.setJMenuBar(menu);
		
		// file explorer
		final JFileChooser explorer = new JFileChooser();
		FileNameExtensionFilter sprFilter =
				new FileNameExtensionFilter("Sprite files", new String[] { "spr" });
		// can't clear text due to wonky code
		// have to set a blank file instead
		final File EEE = new File("");

		// load sprite file
		fileNameBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				explorer.setSelectedFile(EEE);
				explorer.setFileFilter(sprFilter);
				explorer.showOpenDialog(fileNameBtn);
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
		
		// turn sprite into png
		loadBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				byte[] sprite;
				try {
					sprite = readSprite(fileName.getText());
				} catch (IOException e1) {
					JOptionPane.showMessageDialog(frame,
							"Error reading sprite",
							"Oops",
							JOptionPane.WARNING_MESSAGE);
					return;
				}

				try {
					byte[][][] ebe = sprTo8x8(sprite);
					byte[][] palette = getPal(sprite);
					byte[] src = makeRaster(ebe,palette);
					
					run.setImage(makeSheet(src));
				} catch(Exception e) {
					JOptionPane.showMessageDialog(frame,
							"Error converting sprite",
							"Oops",
							JOptionPane.WARNING_MESSAGE);
					return;
				}
			}});
		
		// 
		animOptions.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				run.setAnimation(animOptions.getSelectedIndex());
			}});
		
		modeOptions.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				run.setMode(modeOptions.getSelectedIndex());
				int animMode = run.getMode();
				// button disabling
				switch(animMode) {
					case 0 :
						fasterBtn.setEnabled(true);
						slowerBtn.setEnabled(true);
						resetBtn.setEnabled(true);
						stepBtn.setEnabled(false);
						break;
					case 1 :
						fasterBtn.setEnabled(false);
						slowerBtn.setEnabled(false);
						resetBtn.setEnabled(true);
						stepBtn.setEnabled(true);
						break;
					case 2 :
						fasterBtn.setEnabled(false);
						slowerBtn.setEnabled(false);
						resetBtn.setEnabled(true);
						stepBtn.setEnabled(false);
						break;
				}
				run.reset();
			}});
		
		fasterBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				slowerBtn.setEnabled(true);
				if (run.faster())
					fasterBtn.setEnabled(false);
			}});
		
		slowerBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				fasterBtn.setEnabled(true);
				if (run.slower())
					slowerBtn.setEnabled(false);
			}});
		
		resetBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int animMode = run.getMode();
				run.repaint();
				run.reset();
				// button disabling
				switch (animMode) {
					case 0 :
						fasterBtn.setEnabled(true);
						slowerBtn.setEnabled(true);
						break;
					case 1 :
						// nothing
						break;
					case 2 :
						// nothing
						break;
				}
			}});
		
		stepBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				run.step();
			}});
		// turn on
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
	 * Splits a palette into RGB arrays.
	 * Only uses the first 16 colors.
	 * Automatically makes first index black.
	 */
	public static byte[][] getPal(byte[] sprite) {
		byte[][] ret = new byte[16][3];
		for (int i = 1; i < 16; i++) {
			short color = 0;
			int pos = SPRITESIZE + (i * 2) - 2;
			color = (short) unsignByte(sprite[pos+1]);
			color <<= 8;
			color |= (short) unsignByte(sprite[pos]);
			
			ret[i][0] = (byte) (((color >> 0) & 0x1F) << 3);
			ret[i][1] = (byte) (((color >> 5) & 0x1F) << 3);
			ret[i][2] = (byte) (((color >> 10) & 0x1F) << 3);

			System.out.println(
					((ret[i][0]+256)%256)
					+ " " +
					((ret[i][1]+256)%256)
					+ " " +
					((ret[i][2]+256)%256)
							);
		}

		// make black;
		// separate operation just in case I don't wanna change pal's values
		ret[0][0] = 0;
		ret[0][1] = 0;
		ret[0][2] = 0;

		return ret;
	}

	/**
	 * Turn index map in 8x8 format into an array of ABGR values
	 */
	public static byte[] makeRaster(byte[][][] ebe, byte[][] palette) {
		byte[] ret = new byte[RASTERSIZE];
		int largeCol = 0;
		int intRow = 0;
		int intCol = 0;
		int index = 0;
		byte[] color;
		// read image
		for (int i = 0; i < RASTERSIZE / 4; i++) {
			// get pixel color index
			byte coli = ebe[index][intRow][intCol];
			// get palette color
			color = palette[coli];
			// index 0 = trans
			if (coli == 0)
				ret[i*4] = 0;
			else
				ret[i*4] = (byte) 255;

			// BGR
			ret[i*4+1] = color[2];
			ret[i*4+2] = color[1];
			ret[i*4+3] = color[0];

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
		return ret;
	}

	/**
	 * Turns a 4 byte raster {A,B,G,R} into an integer array and sets the image.
	 * @param raster
	 * @return
	 */
	public static BufferedImage makeSheet(byte[] raster) {
		BufferedImage image = new BufferedImage(128, 448, BufferedImage.TYPE_4BYTE_ABGR_PRE);
		int[] rgb = new int[128 * 448];
		for (int i = 0, j = 0; i < rgb.length; i++) {
			int a = raster[j++] & 0xff;
			int b = raster[j++] & 0xff;
			int g = raster[j++] & 0xff;
			int r = raster[j++] & 0xff;
			rgb[i] = (a << 24) | (r << 16) | (g << 8) | b;
		}
		image.setRGB(0, 0, 128, 448, rgb, 0, 128);
		
		return image;
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
	
	/**
	 * Join array of strings together with a delimiter.
	 * @param s - array of strings
	 * @param c - delimiter
	 * @return A single <tt>String</tt>.
	 */
	public static String join(String[] s, String c) {
		String ret = "";
		for (int i = 0; i < s.length; i++) {
			ret += s[i];
			if (i != s.length-1)
				ret += c;
		}
		return ret;
	}

	/**
	 * 
	 * @param b
	 * @return
	 */
	public static int unsignByte(byte b) {
		int ret = (b + 256) % 256;
		return ret;
	}	
}

/**
 * Sprite class to handle drawing better
 * TODO: z field for when to draw and methods (above) for reordering based on z
 */
class Sprite {
	int x;
	int y;
	int z;
	BufferedImage img;
	public Sprite(BufferedImage image, int xpos, int ypos, int zindex) {
		img = image;
		x = xpos;
		y = ypos;
		z = zindex;
	}
	
	/**
	 * Attaches itself to a {@link Graphics2D} object and draws itself accordingly.
	 * @param g - Graphics2D object
	 */
	public void draw(Graphics2D g) {
		g.drawImage(img, x, y, null);
	}
}
