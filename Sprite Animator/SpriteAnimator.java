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
import javax.swing.JLabel;
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
	 *		E  : Empty frame 0x0
	 * TRANSFORM is a flag determining how to flip the sprite
	 *		0  : No transform
	 *		U  : Mirror along X-axis
	 *		M  : Mirror along y-axis
	 *		UM : Mirror along both axes
	 */
	static final String[] ALLFRAMES = {
			// stand A0:B0
			"A0{-1,8}{F}{0}:B0{0,16}{F}{0}", 
			// standUp A2:C1
			"A2{0,8}{F}{0}:C1{0,16}{F}{0}",
			// standDown A1:B3
			"A1{0,8}{F}{0}:B3{0,16}{F}{0}", // 
			// walk  A0:B0,A0:B1,K3:B2,K4:Q7,A0:S4,A0:R6,K3:R7,K4:S3
			"A0{-1,8}{F}{0}:B0{0,16}{F}{0};" +
					"A0{-1,8}{F}{0}:B1{0,16}{F}{0};" +
					"K3{-1,8}{F}{0}:B2{0,16}{F}{0};" +
					"K4{-1,8}{F}{0}:Q7{0,16}{F}{0};" +
					"A0{-1,8}{F}{0}:S4{0,16}{F}{0};" +
					"A0{-1,8}{F}{0}:R6{0,16}{F}{0};" +
					"K3{-1,8}{F}{0}:R7{0,16}{F}{0};" +
					"K4{-1,8}{F}{0}:S3{0,16}{F}{0};",
			// walkUp - A2:B6,A2:C0,A2:S7,A2:T3,A2:T7,A2:T4,A2:T5,A2:T6
			"A2{0,0}{F}{0}:B6{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:C0{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:S7{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:T3{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:T7{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:T4{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:T5{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:T6{0,16}{F}{0}",
			// walkDown - A1:B4,A1:B5,A1:S5,A1:S6,A1:B4-M,A1:B5-M,A1:S5-M,A1:S6-M
			"A1{0,0}{F}{0}:B4{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:B5{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:S5{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:S6{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:B4{0,16}{F}{M};" +
				"A1{0,0}{F}{0}:B5{0,16}{F}{M};" +
				"A1{0,0}{F}{0}:S5{0,16}{F}{M};" +
				"A1{0,0}{F}{0}:S6{0,16}{F}{M}",
			// bonk - F3
			"F3{0,0}{F}{0}:G3{0,16}{T}{0}",
			// bonkUp - F4
			"F4{0,0}{F}{0}:G4{0,16}{T}{0}",
			// bonkDown - F2
			"F2{0,0}{F}{0}:G2{0,16}{T}{0}",
			// swim - H5:I7,H6:J0,H5:I7,H7:J1
			"H5{0,0}{F}{0}:I7{0,16}{F}{0};" +
				"H6{0,0}{F}{0}:J0{0,16}{F}{0};" +
				"H5{0,0}{F}{0}:I7{0,16}{F}{0};" +
				"H7{0,0}{F}{0}:J1{0,16}{F}{0}",
			// swimUp - A2:I5,E4:I6
			"A2{0,0}{F}{0}:I5{0,16}{F}{0};" +
				"E4{0,0}{F}{0}:I6{0,16}{F}{0}",
			// swimDown - I3:J3,I4:J4
			"I3{0,0}{F}{0}:J3{0,16}{F}{0};" +
				"I4{0,0}{F}{0}:J4{0,16}{F}{0}",
			// swimFlap - P0,J5
			"P0{0,0}{F}{0};" +
				"J5{0,0}{F}{0}",
			// treadingWater - A0:L0,A0:L0-M
			"A0{0,0}{F}{0}:L0{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:L0{0,16}{F}{M}",
			// treadingWaterUp - A2:J2,A2:J2-M
			"A2{0,0}{F}{0}:J2{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:J2{0,16}{F}{M}",
			// treadingWaterDown - A1:J2,A1:J2-M
			"A1{0,0}{F}{0}:J2{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:J2{0,16}{F}{M}",
			// attack - A0:C2,A0:C3,A0:C4,A0:α7,ZZ100:Z6,A0:C4,A0:C5
			"A0{0,0}{F}{0}:C2{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:C3{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:C4{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:α7{0,16}{F}{0};" +
				"ZZ100:Z6{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:C4{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:C5{0,16}{F}{0}",
			// attackUp - F1,A2:D1,A2:D2,A2:β1,A2:D2,A2:L4
			"F1{0,0}{F}{0};" +
				"A2{0,0}{F}{0}:D1{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:D2{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:β1{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:D2{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:L4{0,16}{F}{0}",
			// attackDown - F0,A1:C6,A4:D0,A4:β0,A4:D0,A3:L3
			"F0{0,0}{F}{0};" +
				"A1{0,0}{F}{0}:C6{0,16}{F}{0};" +
				"A4{0,0}{F}{0}:D0{0,16}{F}{0};" +
				"A4{0,0}{F}{0}:β0{0,16}{F}{0};" +
				"A4{0,0}{F}{0}:D0{0,16}{F}{0};" +
				"A3{0,0}{F}{0}:L3{0,16}{F}{0}",
			// dashRelease - A0:M6,K3:V1,A0:M6,K4:M7
			"A0{0,0}{F}{0}:M6{0,16}{F}{0};" +
				"K3{0,0}{F}{0}:V1{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:M6{0,16}{F}{0};" +
				"K4{0,0}{F}{0}:M7{0,16}{F}{0}",
			// dashReleaseUp - A2:M3,A2:M4,A2:M5,A2:M3,A2:M4-M,A2:M5-M,A2:M3-M
			"A2{0,0}{F}{0}:M3{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:M4{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:M5{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:M3{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:M4{0,16}{F}{M};" +
				"A2{0,0}{F}{0}:M5{0,16}{F}{M};" +
				"A2{0,0}{F}{0}:M3{0,16}{F}{M}",
			// dashReleaseDown - A1:M0,A1:M1,A1:M2,A1:M0,A1:β2,A1:β3,A1:M0
			"A1{0,0}{F}{0}:M0{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:M1{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:M2{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:M0{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:β2{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:β3{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:M0{0,16}{F}{0}",
			// spinAttack - A0:I0,A0:P1,A0-M:I0,A0:B0,A1:P3,A0-M:B0-M,A2:P2,A0:I0
			"A0{0,0}{F}{0}:I0{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:P1{0,16}{F}{0};" +
				"A0{0,0}{F}{M}:I0{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:B0{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:P3{0,16}{F}{0};" +
				"A0{0,0}{F}{M}:B0{0,16}{F}{M};" +
				"A2{0,0}{F}{0}:P2{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:I0{0,16}{F}{0}",
			// spinAttackLeft - A0-M:I1,A1:P3,A0:B0,A2:P2,A0-M:B0-M,A0-M:I1,A0-M:I2,A0-M:I1
			"A0{0,0}{F}{M}:I1{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:P3{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:B0{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:P2{0,16}{F}{0};" +
				"A0{0,0}{F}{M}:B0{0,16}{F}{M};" +
				"A0{0,0}{F}{M}:I1{0,16}{F}{0};" +
				"A0{0,0}{F}{M}:I2{0,16}{F}{0};" +
				"A0{0,0}{F}{M}:I1{0,16}{F}{0}",
			// spinAttackUp - A2:D1-M,F1-M,A2:D1-M,A2:P2,A0:B0,A1:P3,A0-M:B0-M,A2:D1-M
			"A2{0,0}{F}{0}:D1{0,16}{F}{M};" +
				"F1{0,0}{F}{M};" +
				"A2{0,0}{F}{0}:D1{0,16}{F}{M};" +
				"A2{0,0}{F}{0}:P2{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:B0{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:P3{0,16}{F}{0};" +
				"A0{0,0}{F}{M}:B0{0,16}{F}{M};" +
				"A2{0,0}{F}{0}:D1{0,16}{F}{M}",
			// spinAttackDown - A1:C6,F0,A1:C6,A1:P3,A0-M:B0-M,A2:P2,A0:B0,A1:C6
			"A1{0,0}{F}{0}:C6{0,16}{F}{0};" +
				"F0{0,0}{F}{0};" +
				"A1{0,0}{F}{0}:C6{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:P3{0,16}{F}{0};" +
				"A0{0,0}{F}{M}:B0{0,16}{F}{M};" +
				"A2{0,0}{F}{0}:P2{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:B0{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:C6{0,16}{F}{0}",
			// dashSpinup - A0:B0,A0:B1,K3:B2,K4:Q7,A0:S4,A0:R6,K3:R7,K4:S3,A0:B0,A0:B1,K3:B2,K4:Q7,A0:S4,A0:R6,K3:R7,K4:S3,A0:B0,A0:B1,K3:B2,K4:Q7,A0:S4,A0:R6,K3:R7,K4:S3
			"A0{0,0}{F}{0}:B0{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:B1{0,16}{F}{0};" +
				"K3{0,0}{F}{0}:B2{0,16}{F}{0};" +
				"K4{0,0}{F}{0}:Q7{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:S4{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:R6{0,16}{F}{0};" +
				"K3{0,0}{F}{0}:R7{0,16}{F}{0};" +
				"K4{0,0}{F}{0}:S3{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:B0{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:B1{0,16}{F}{0};" +
				"K3{0,0}{F}{0}:B2{0,16}{F}{0};" +
				"K4{0,0}{F}{0}:Q7{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:S4{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:R6{0,16}{F}{0};" +
				"K3{0,0}{F}{0}:R7{0,16}{F}{0};" +
				"K4{0,0}{F}{0}:S3{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:B0{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:B1{0,16}{F}{0};" +
				"K3{0,0}{F}{0}:B2{0,16}{F}{0};" +
				"K4{0,0}{F}{0}:Q7{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:S4{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:R6{0,16}{F}{0};" +
				"K3{0,0}{F}{0}:R7{0,16}{F}{0};" +
				"K4{0,0}{F}{0}:S3{0,16}{F}{0}",
			// dashSpinupUp - A2:C1,A2:B6,A2:C0,A2:S7,A2:T3,A2:T7,A2:T4,A2:T5,A2:T6,A2:B6,A2:C0,A2:S7,A2:T3,A2:T7,A2:T4,A2:T5,A2:T6,A2:B6,A2:C0,A2:S7,A2:T3,A2:T7,A2:T4,A2:T5
			"A2{0,0}{F}{0}:C1{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:B6{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:C0{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:S7{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:T3{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:T7{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:T4{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:T5{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:T6{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:B6{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:C0{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:S7{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:T3{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:T7{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:T4{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:T5{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:T6{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:B6{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:C0{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:S7{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:T3{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:T7{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:T4{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:T5{0,16}{F}{0}",
			// dashSpinupDown - A1:B3,A1:B4-M,A1:B5,A1:S5,A1:S6,A1:B4-M,A1:B5-M,A1:S5-M,A1:S6-M,A1:B4-M,A1:B5,A1:S5,A1:S6,A1:B4-M,A1:B5-M,A1:S5-M,A1:S6-M,A1:B4-M,A1:B5,A1:S5,A1:S6,A1:B4-M,A1:B5-M,A1:S5-M,A1:S6-M
			"A1{0,0}{F}{0}:B3{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:B4{0,16}{F}{M};" +
				"A1{0,0}{F}{0}:B5{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:S5{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:S6{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:B4{0,16}{F}{M};" +
				"A1{0,0}{F}{0}:B5{0,16}{F}{M};" +
				"A1{0,0}{F}{0}:S5{0,16}{F}{M};" +
				"A1{0,0}{F}{0}:S6{0,16}{F}{M};" +
				"A1{0,0}{F}{0}:B4{0,16}{F}{M};" +
				"A1{0,0}{F}{0}:B5{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:S5{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:S6{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:B4{0,16}{F}{M};" +
				"A1{0,0}{F}{0}:B5{0,16}{F}{M};" +
				"A1{0,0}{F}{0}:S5{0,16}{F}{M};" +
				"A1{0,0}{F}{0}:S6{0,16}{F}{M};" +
				"A1{0,0}{F}{0}:B4{0,16}{F}{M};" +
				"A1{0,0}{F}{0}:B5{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:S5{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:S6{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:B4{0,16}{F}{M};" +
				"A1{0,0}{F}{0}:B5{0,16}{F}{M};" +
				"A1{0,0}{F}{0}:S5{0,16}{F}{M};" +
				"A1{0,0}{F}{0}:S6{0,16}{F}{M}",
			// salute - A3:P4-M
			"A3{0,0}{F}{0}:P4{0,16}{F}{M}",
			// itemGet - L1:L2
			"L1{0,0}{F}{0}:L2{0,16}{F}{0}",
			// triforceGet - Z2:β4
			"Z2{0,0}{F}{0}:β4{0,16}{F}{0}",
			// readBook - K5:K6,R1,A5:Q1,A5:Q0,S1
			"K5{0,0}{F}{0}:K6{0,16}{F}{0};" +
				"R1{0,0}{F}{0};" +
				"A5{0,0}{F}{0}:Q1{0,16}{F}{0};" +
				"A5{0,0}{F}{0}:Q0{0,16}{F}{0};" +
				"S1{0,0}{F}{0}",
			// fall - G0,E5,E6,H4-T,H4-B,G4-B
			"G0{0,0}{F}{0};" +
				"E5{0,0}{F}{0};" +
				"E6{0,0}{F}{0};" +
				"H4-T;" +
				"H4-B;" +
				"G4-B",
			// grab - A0:X2,Z3:Z4
			"A0{0,0}{F}{0}:X2{0,16}{F}{0};" +
				"Z3{0,0}{F}{0}:Z4{0,16}{F}{0}",
			// grabUp - Z0:V5,Y6
			"Z0{0,0}{F}{0}:V5{0,16}{F}{0};" +
				"Y6{0,0}{F}{0}",
			// grabDown - E3:X5,U0:P7
			"E3{0,0}{F}{0}:X5{0,16}{F}{0};" +
				"U0{0,0}{F}{0}:P7{0,16}{F}{0}",
			// lift - E2:U5,U1:U6,L6:O2
			"E2{0,0}{F}{0}:U5{0,16}{F}{0};" +
				"U1{0,0}{F}{0}:U6{0,16}{F}{0};" +
				"L6{0,0}{F}{0}:O2{0,16}{F}{0}",
			// liftUp - U2:U7,A2:V0,L7:O5
			"U2{0,0}{F}{0}:U7{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:V0{0,16}{F}{0};" +
				"L7{0,0}{F}{0}:O5{0,16}{F}{0}",
			// liftDown - U0:U4,U0:U3,L5:N7
			"U0{0,0}{F}{0}:U4{0,16}{F}{0};" +
				"U0{0,0}{F}{0}:U3{0,16}{F}{0};" +
				"L5{0,0}{F}{0}:N7{0,16}{F}{0}",
			// carry - L6:O2,L6:O3,L6:O4
			"L6{0,0}{F}{0}:O2{0,16}{F}{0};" +
				"L6{0,0}{F}{0}:O3{0,16}{F}{0};" +
				"L6{0,0}{F}{0}:O4{0,16}{F}{0}",
			// carryUp - L7:O5,L7:O6,L7:O7
			"L7{0,0}{F}{0}:O5{0,16}{F}{0};" +
				"L7{0,0}{F}{0}:O6{0,16}{F}{0};" +
				"L7{0,0}{F}{0}:O7{0,16}{F}{0}",
			// carryDown - L5:N7,L5:O0,L5:O1
			"L5{0,0}{F}{0}:N7{0,16}{F}{0};" +
				"L5{0,0}{F}{0}:O0{0,16}{F}{0};" +
				"L5{0,0}{F}{0}:O1{0,16}{F}{0}",
			// treePull - P7-UM:E7,N0:A1-UM,K2:K0,K1,F4,N0:A1-UM,K2:K0,K1
			"P7{0,0}{F}{UM}:E7{0,16}{F}{0};" +
				"N0{0,0}{F}{0}:A1{0,16}{F}{UM};" +
				"K2{0,0}{F}{0}:K0{0,16}{F}{0};" +
				"K1{0,0}{F}{0};" +
				"F4{0,0}{F}{0};" +
				"N0{0,0}{F}{0}:A1{0,16}{F}{UM};" +
				"K2{0,0}{F}{0}:K0{0,16}{F}{0};" +
				"K1{0,0}{F}{0}",
			// throw - A0:M6,A0:B0
			"A0{0,0}{F}{0}:M6{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:B0{0,16}{F}{0}",
			// throwUp - A2:M3,A2:C1
			"A2{0,0}{F}{0}:M3{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:C1{0,16}{F}{0}",
			// throwDown - A1:M0,A1:B3
			"A1{0,0}{F}{0}:M0{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:B3{0,16}{F}{0}",
			// push - U1:X2,U1:X3,U1:X4,U1:X2,U1:X3,U1:X2,U1:X3,U1:X4
			"U1{0,0}{F}{0}:X2{0,16}{F}{0};" +
				"U1{0,0}{F}{0}:X3{0,16}{F}{0};" +
				"U1{0,0}{F}{0}:X4{0,16}{F}{0};" +
				"U1{0,0}{F}{0}:X2{0,16}{F}{0};" +
				"U1{0,0}{F}{0}:X3{0,16}{F}{0};" +
				"U1{0,0}{F}{0}:X2{0,16}{F}{0};" +
				"U1{0,0}{F}{0}:X3{0,16}{F}{0};" +
				"U1{0,0}{F}{0}:X4{0,16}{F}{0}",
			// pushUp - U2:M3,U2:M4,U2:M5,U2:M3,U2:M4-M,U2:M3,U2:M4,U2:M5
			"U2{0,0}{F}{0}:M3{0,16}{F}{0};" +
				"U2{0,0}{F}{0}:M4{0,16}{F}{0};" +
				"U2{0,0}{F}{0}:M5{0,16}{F}{0};" +
				"U2{0,0}{F}{0}:M3{0,16}{F}{0};" +
				"U2{0,0}{F}{0}:M4{0,16}{F}{M};" +
				"U2{0,0}{F}{0}:M3{0,16}{F}{0};" +
				"U2{0,0}{F}{0}:M4{0,16}{F}{0};" +
				"U2{0,0}{F}{0}:M5{0,16}{F}{0}",
			// pushDown - U0:X5,U0:X6,U0:X7,U0:X5,U0:X6-M,U0:X5,U0:X6,U0:X7
			"U0{0,0}{F}{0}:X5{0,16}{F}{0};" +
				"U0{0,0}{F}{0}:X6{0,16}{F}{0};" +
				"U0{0,0}{F}{0}:X7{0,16}{F}{0};" +
				"U0{0,0}{F}{0}:X5{0,16}{F}{0};" +
				"U0{0,0}{F}{0}:X6{0,16}{F}{M};" +
				"U0{0,0}{F}{0}:X5{0,16}{F}{0};" +
				"U0{0,0}{F}{0}:X6{0,16}{F}{0};" +
				"U0{0,0}{F}{0}:X7{0,16}{F}{0}",
			// shovel - B7:D7,A0:F5,A0:C7
			"B7{0,0}{F}{0}:D7{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:F5{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:C7{0,16}{F}{0}",
			// boomerang - S2,A0:C4,A0:B0
			"S2{0,0}{F}{0};" +
				"A0{0,0}{F}{0}:C4{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:B0{0,16}{F}{0}",
			// boomerangUp - R2,A2:Q6,A2:C1
			"R2{0,0}{F}{0};" +
				"A2{0,0}{F}{0}:Q6{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:C1{0,16}{F}{0}",
			// boomerangDown - A1:Q5,A1:D0,A1:B3
			"A1{0,0}{F}{0}:Q5{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:D0{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:B3{0,16}{F}{0}",
			// rod - G2-R,A0:C4,A0:N4
			"G2-R;" +
				"A0{0,0}{F}{0}:C4{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:N4{0,16}{F}{0}",
			// rodUp - G3-R,A2:D2,A2:N5
			"G3-R;" +
				"A2{0,0}{F}{0}:D2{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:N5{0,16}{F}{0}",
			// rodDown - G1-R,A1:N6,A1:D0
			"G1-R;" +
				"A1{0,0}{F}{0}:N6{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:D0{0,16}{F}{0}",
			// powder - A0:C2,A0:C3,A0:C4,A0:C5
			"A0{0,0}{F}{0}:C2{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:C3{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:C4{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:C5{0,16}{F}{0}",
			// powderUp - F1,A2:D1,A2:D2,A2:L4
			"F1{0,0}{F}{0};" +
				"A2{0,0}{F}{0}:D1{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:D2{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:L4{0,16}{F}{0}",
			// powderDown - F0,A1:C6,A3:D0,A3:L3
			"F0{0,0}{F}{0};" +
				"A1{0,0}{F}{0}:C6{0,16}{F}{0};" +
				"A3{0,0}{F}{0}:D0{0,16}{F}{0};" +
				"A3{0,0}{F}{0}:L3{0,16}{F}{0}",
			// cane - A0:I2-M,L1:O2,A0:C4
			"A0{0,0}{F}{0}:I2{0,16}{F}{M};" +
				"L1{0,0}{F}{0}:O2{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:C4{0,16}{F}{0}",
			// caneUp - F1-M,A2:P5-M,A2:D2
			"F1{0,0}{F}{M};" +
				"A2{0,0}{F}{0}:P5{0,16}{F}{M};" +
				"A2{0,0}{F}{0}:D2{0,16}{F}{0}",
			// caneDown - F0-M,A1:P4-M,A1:D0
			"F0{0,0}{F}{M};" +
				"A1{0,0}{F}{0}:P4{0,16}{F}{M};" +
				"A1{0,0}{F}{0}:D0{0,16}{F}{0}",
			// bow - A0:M6,A0:C4,A0:P6
			"A0{0,0}{F}{0}:M6{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:C4{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:P6{0,16}{F}{0}",
			// bowUp - A2:C1,A2:M4,A2:P5
			"A2{0,0}{F}{0}:C1{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:M4{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:P5{0,16}{F}{0}",
			// bowDown - A1:B3,A1:B4,A1:B4
			"A1{0,0}{F}{0}:B3{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:B4{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:B4{0,16}{F}{0}",
			// bombos - A1:M0,A0-M:M6-M,A2:M3,A0:M6,A1:M0,A0-M:M6-M,A2:M3,A0:M6,A1:P3,A1:P4-M,A1:P3,A1:L3,A1:D0
			"A1{0,0}{F}{0}:M0{0,16}{F}{0};" +
				"A0{0,0}{F}{M}:M6{0,16}{F}{M};" +
				"A2{0,0}{F}{0}:M3{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:M6{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:M0{0,16}{F}{0};" +
				"A0{0,0}{F}{M}:M6{0,16}{F}{M};" +
				"A2{0,0}{F}{0}:M3{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:M6{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:P3{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:P4{0,16}{F}{M};" +
				"A1{0,0}{F}{0}:P3{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:L3{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:D0{0,16}{F}{0}",
			// ether - A1:M0,A0-M:M6-M,A2:M3,A0:M6,A1:M0,A0-M:M6-M,A2:M3,A0:M6,A1:P3,A7:P4-M
			"A1{0,0}{F}{0}:M0{0,16}{F}{0};" +
				"A0{0,0}{F}{M}:M6{0,16}{F}{M};" +
				"A2{0,0}{F}{0}:M3{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:M6{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:M0{0,16}{F}{0};" +
				"A0{0,0}{F}{M}:M6{0,16}{F}{M};" +
				"A2{0,0}{F}{0}:M3{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:M6{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:P3{0,16}{F}{0};" +
				"A7{0,0}{F}{0}:P4{0,16}{F}{M}",
			// quake - A1:M0,A0-M:M6-M,A2:M3,A0:M6,A1:M0,A0-M:M6-M,A2:M3,A0:M6,A1:P3,A1:P4-M,L5:N7,A1:Q1
			"A1{0,0}{F}{0}:M0{0,16}{F}{0};" +
				"A0{0,0}{F}{M}:M6{0,16}{F}{M};" +
				"A2{0,0}{F}{0}:M3{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:M6{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:M0{0,16}{F}{0};" +
				"A0{0,0}{F}{M}:M6{0,16}{F}{M};" +
				"A2{0,0}{F}{0}:M3{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:M6{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:P3{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:P4{0,16}{F}{M};" +
				"L5{0,0}{F}{0}:N7{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:Q1{0,16}{F}{0}",
			// hookshot - A0:C4
			"A0{0,0}{F}{0}:C4{0,16}{F}{0}",
			// hookshotUp - A2:D2
			"A2{0,0}{F}{0}:D2{0,16}{F}{0}",
			// hookshotDown - A1:D0
			"A1{0,0}{F}{0}:D0{0,16}{F}{0}",
			// zap - R0,S0
			"R0{0,0}{F}{0};" +
				"S0{0,0}{F}{0}",
			// bunnyStand - α4:α5
			"α4{0,0}{F}{0}:α5{0,16}{F}{0}",
			// bunnyStandUp - α1:α2
			"α1{0,0}{F}{0}:α2{0,16}{F}{0}",
			// bunnyStandDown - Z5:α0
			"Z5{0,0}{F}{0}:α0{0,16}{F}{0}",
			// bunnyWalk - α4:α5,α4:α6
			"α4{0,0}{F}{0}:α5{0,16}{F}{0};" +
				"α4{0,0}{F}{0}:α6{0,16}{F}{0}",
			// bunnyWalkUp - α1:α2,α1:α3
			"α1{0,0}{F}{0}:α2{0,16}{F}{0};" +
				"α1{0,0}{F}{0}:α3{0,16}{F}{0}",
			// bunnyWalkDown - Z5:α0,Z5:Z7
			"Z5{0,8}{F}{0}:α0{0,16}{F}{0}l" +
				"Z5{0,9}{F}{0}:Z7{0,16}{F}{0}",
			// walkDownstairs2F - A2:C1,A2:V5,A2:V6,A2:C1,A2:D4,A2:M5-M,A2:C1,A2:V5,X1-M:Y4-M,X1-M:Y5-M,X1-M:Y3-M,X1-M:Y4-M,X1-M:Y5-M,X1-M:Y3-M,X1-M:Y4-M,X1-M:Y5-M,X1-M:Y3-M,A0-M:B0-M,A0-M:V1-M,A0-M:V2-M,A0-M:B0-M,A0-M:V1-M,A0-M:V2-M,A0-M:B0-M,A0-M:V1-M,A0-M:V2-M,A0-M:B0-M,A0-M:V1-M,A0-M:V2-M,A0-M:B0-M
			"A2{0,0}{F}{0}:C1{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:V5{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:V6{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:C1{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:D4{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:M5{0,16}{F}{M};" +
				"A2{0,0}{F}{0}:C1{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:V5{0,16}{F}{0};" +
				"X1{0,0}{F}{M}:Y4{0,16}{F}{M};" +
				"X1{0,0}{F}{M}:Y5{0,16}{F}{M};" +
				"X1{0,0}{F}{M}:Y3{0,16}{F}{M};" +
				"X1{0,0}{F}{M}:Y4{0,16}{F}{M};" +
				"X1{0,0}{F}{M}:Y5{0,16}{F}{M};" +
				"X1{0,0}{F}{M}:Y3{0,16}{F}{M};" +
				"X1{0,0}{F}{M}:Y4{0,16}{F}{M};" +
				"X1{0,0}{F}{M}:Y5{0,16}{F}{M};" +
				"X1{0,0}{F}{M}:Y3{0,16}{F}{M};" +
				"A0{0,0}{F}{M}:B0{0,16}{F}{M};" +
				"A0{0,0}{F}{M}:V1{0,16}{F}{M};" +
				"A0{0,0}{F}{M}:V2{0,16}{F}{M};" +
				"A0{0,0}{F}{M}:B0{0,16}{F}{M};" +
				"A0{0,0}{F}{M}:V1{0,16}{F}{M};" +
				"A0{0,0}{F}{M}:V2{0,16}{F}{M};" +
				"A0{0,0}{F}{M}:B0{0,16}{F}{M};" +
				"A0{0,0}{F}{M}:V1{0,16}{F}{M};" +
				"A0{0,0}{F}{M}:V2{0,16}{F}{M};" +
				"A0{0,0}{F}{M}:B0{0,16}{F}{M};" +
				"A0{0,0}{F}{M}:V1{0,16}{F}{M};" +
				"A0{0,0}{F}{M}:V2{0,16}{F}{M};" +
				"A0{0,0}{F}{M}:B0{0,16}{F}{M}",
			// walkDownstairs1F - A0-M:V2-M,A0-M:B0-M,A0-M:V1-M,B7-M:Y1-M,B7-M:Y2-M,B7-M:Y0-M,B7-M:Y1-M,B7-M:Y2-M,B7-M:Y0-M,B7-M:Y1-M,A1:V3,A1:V4,A1:B3,A1:V3-M,A1:V4-M,A1:B3,A1:V3,A1:V4,A1:B3,A1:V3-M,A1:S6,A1:B3
			"A0{0,0}{F}{M}:V2{0,16}{F}{M};" +
				"A0{0,0}{F}{M}:B0{0,16}{F}{M};" +
				"A0{0,0}{F}{M}:V1{0,16}{F}{M};" +
				"B7{0,0}{F}{M}:Y1{0,16}{F}{M};" +
				"B7{0,0}{F}{M}:Y2{0,16}{F}{M};" +
				"B7{0,0}{F}{M}:Y0{0,16}{F}{M};" +
				"B7{0,0}{F}{M}:Y1{0,16}{F}{M};" +
				"B7{0,0}{F}{M}:Y2{0,16}{F}{M};" +
				"B7{0,0}{F}{M}:Y0{0,16}{F}{M};" +
				"B7{0,0}{F}{M}:Y1{0,16}{F}{M};" +
				"A1{0,0}{F}{0}:V3{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:V4{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:B3{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:V3{0,16}{F}{M};" +
				"A1{0,0}{F}{0}:V4{0,16}{F}{M};" +
				"A1{0,0}{F}{0}:B3{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:V3{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:V4{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:B3{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:V3{0,16}{F}{M};" +
				"A1{0,0}{F}{0}:S6{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:B3{0,16}{F}{0}",
			// walkUpstairs1F - A2:V5,A2:V6,A2:C1,X1:Y3,X1:Y4,X1:Y5,X1:Y3,X1:Y4,X1:Y5,X1:Y3,X1:Y4,A0:V2,A0:B0,A0:V2,A0:B0,A0:V2,A0:B0,A0:V2,A0:B0
			"A2{0,0}{F}{0}:V5{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:V6{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:C1{0,16}{F}{0};" +
				"X1{0,0}{F}{0}:Y3{0,16}{F}{0};" +
				"X1{0,0}{F}{0}:Y4{0,16}{F}{0};" +
				"X1{0,0}{F}{0}:Y5{0,16}{F}{0};" +
				"X1{0,0}{F}{0}:Y3{0,16}{F}{0};" +
				"X1{0,0}{F}{0}:Y4{0,16}{F}{0};" +
				"X1{0,0}{F}{0}:Y5{0,16}{F}{0};" +
				"X1{0,0}{F}{0}:Y3{0,16}{F}{0};" +
				"X1{0,0}{F}{0}:Y4{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:V2{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:B0{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:V2{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:B0{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:V2{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:B0{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:V2{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:B0{0,16}{F}{0}",
			// walkUpstairs2F - A0:B0,A0:V1,B7:Y1,B7:Y2,B7:Y0,B7:Y1,B7:Y2,B7:Y0,B7:Y1,B7:Y2,A1:B3,A1:V3-M,A1:V4-M,A1:B3,A1:V3,A1:V4,A1:B3
			"A0{0,0}{F}{0}:B0{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:V1{0,16}{F}{0};" +
				"B7{0,0}{F}{0}:Y1{0,16}{F}{0};" +
				"B7{0,0}{F}{0}:Y2{0,16}{F}{0};" +
				"B7{0,0}{F}{0}:Y0{0,16}{F}{0};" +
				"B7{0,0}{F}{0}:Y1{0,16}{F}{0};" +
				"B7{0,0}{F}{0}:Y2{0,16}{F}{0};" +
				"B7{0,0}{F}{0}:Y0{0,16}{F}{0};" +
				"B7{0,0}{F}{0}:Y1{0,16}{F}{0};" +
				"B7{0,0}{F}{0}:Y2{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:B3{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:V3{0,16}{F}{M};" +
				"A1{0,0}{F}{0}:V4{0,16}{F}{M};" +
				"A1{0,0}{F}{0}:B3{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:V3{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:V4{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:B3{0,16}{F}{0}",
			// deathSpin - A1:B3,A0-M:B0-M,A2:C1,A0:B0,A1:B3,A0-M:B0-M,A2:C1,A0:B0,A1:B3,A0-M:B0-M,A2:C1,A0:B0,E2:J6,J7
			"A1{0,0}{F}{0}:B3{0,16}{F}{0};" +
				"A0{0,0}{F}{M}:B0{0,16}{F}{M};" +
				"A2{0,0}{F}{0}:C1{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:B0{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:B3{0,16}{F}{0};" +
				"A0{0,0}{F}{M}:B0{0,16}{F}{M};" +
				"A2{0,0}{F}{0}:C1{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:B0{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:B3{0,16}{F}{0};" +
				"A0{0,0}{F}{M}:B0{0,16}{F}{M};" +
				"A2{0,0}{F}{0}:C1{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:B0{0,16}{F}{0};" +
				"E2{0,0}{F}{0}:J6{0,16}{F}{0};" +
				"J7{0,0}{F}{0}",
			// deathSplat - E2:J6,J7
			"E2{0,0}{F}{0}:J6{0,16}{F}{0};" +
				"J7{0,0}{F}{0}",
			// poke - A0:N3,A0:N2,A0:F6
			"A0{0,0}{F}{0}:N3{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:N2{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:F6{0,16}{F}{0}",
			// pokeUp - E4:D2,E4:F7,E4:L4
			"E4{0,0}{F}{0}:D2{0,16}{F}{0};" +
				"E4{0,0}{F}{0}:F7{0,16}{F}{0};" +
				"E4{0,0}{F}{0}:L4{0,16}{F}{0}",
			// pokeDown - A1:N1,E3:G7
			"A1{0,0}{F}{0}:N1{0,16}{F}{0};" +
				"E3{0,0}{F}{0}:G7{0,16}{F}{0}",
			// tallGrass - A0:B0,A0:V1,A0:V2
			"A0{0,0}{F}{0}:B0{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:V1{0,16}{F}{0};" +
				"A0{0,0}{F}{0}:V2{0,16}{F}{0}",
			// tallGrassUp - A2:V5,A2:V6,A2:C1,A2:D4,A2:M5-M
			"A2{0,0}{F}{0}:V5{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:V6{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:C1{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:D4{0,16}{F}{0};" +
				"A2{0,0}{F}{0}:M5{0,16}{F}{M}",
			// tallGrassDown - A1:B3,A1:V3,A1:V4
			"A1{0,0}{F}{0}:B3{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:V3{0,16}{F}{0};" +
				"A1{0,0}{F}{0}:V4{0,16}{F}{0}",
			// mapDungeon - K7
			"K7{0,0}{F}{0};K7{0,0}{E}{0}",
			// mapWorld - Y7
			"Y7{0,0}{F}{0};Y7{0,0}{E}{0}",
			// sleep - A6:D3
			"A6{0,0}{F}{0}:D3{0,16}{F}{0}",
			// awake - E3:D3
			"E3{0,0}{F}{0}:D3{0,16}{F}{0}"
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
	private int baseSpeed = 100; // base speed in milliseconds
	private int mode; // animation mode
	private int frame;
	private int maxFrame;
	private boolean running;
	private Sprite[][] frames = null;
	private Timer tick;
	private static final int MAXSPEED = 3; // maximum speed magnitude
	// default initialization
	public SpriteAnimator() {
		anime = 0;
		speed = 0;
		mode = 0;
		frame = 0;
		maxFrame = 0;
		running = true;
		tick = new Timer(baseSpeed, new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (isRunning())
					step();
			}
		});
	}

	public void stop() {
		running = false;
		tick.stop();
	}
	
	public int getFrame() {
		return frame;
	}
	
	public int maxFrame() {
		return maxFrame();
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
	public void step() {
		frame++;
		if (frame >= maxFrame) {
			frame = 0;
			if (mode == 2)
				setRunning(false);
		}
		repaint();
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
		adjustTimer();
	}
	
	/**
	 * Resets frame to 0.
	 */
	public void resetFrame() {
		frame = 0;
		repaint();
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
		adjustTimer();
		return atMaxSpeed();
	}
	
	/**
	 * Decrements step speed by 1.
	 * @return <b>true</b> if speed reaches min.
	 */
	public boolean slower() {
		if (speed > (MAXSPEED * -1))
			speed--;
		adjustTimer();
		return atMinSpeed();
	}
	
	/**
	 * Adjusts timer based on speed
	 */
	public void adjustTimer() {
		double speedMS = baseSpeed * Math.pow(1.5, speed * -1);
		tick.setDelay((int) speedMS);
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

	// @link Sprite - lol get it?
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
				BufferedImage spreet;
				if (sprSize.equals("E"))
					spreet = new BufferedImage(16, 16, BufferedImage.TYPE_4BYTE_ABGR_PRE);
				else
					spreet = img.getSubimage(drawX, drawY, width, height);
				
				// put it in backwards to preserve draw order
				frames[i][spriteCount-1-j] = new Sprite(spreet, xpos, ypos, j);
			}
		}
	}
	/**
	 * Draw every sprite
	 */
	public void paint(Graphics g) {
		if (frames==null || frames[frame] == null)
			return;
		Graphics2D g2 = (Graphics2D) g;
		g2.scale(3.0, 3.0);
		for(Sprite s : frames[frame])
			if (s!=null)
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
		final JButton loadBtn = new JButton("Load file");
		final JButton stepBtn = new JButton("Step");
		final JButton fasterBtn = new JButton("Speed+");
		final JButton slowerBtn = new JButton("Speed-");
		final JButton resetBtn = new JButton("Reset");
		final JPanel loadWrap = new JPanel(new BorderLayout());
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

		final JPanel bottomStuffWrap = new JPanel(new BorderLayout());
		final JPanel bottomStuff = new JPanel(new BorderLayout());
		stepBtn.setEnabled(false);

		final SpriteAnimator imageArea = new SpriteAnimator();
		final SpriteAnimator run = imageArea; // just a shorter name

		bottomStuffWrap.add(imageArea,BorderLayout.CENTER);
		bottomStuff.add(controls,BorderLayout.EAST);
		
		final JPanel frameCounter = new JPanel(new BorderLayout());
		final JLabel frameWord = new JLabel("Frame:");
		final JLabel frameCur = new JLabel("0");
		frameCur.setVerticalAlignment(JLabel.EAST);
		frameCounter.add(frameWord,BorderLayout.WEST);
		frameCounter.add(frameCur,BorderLayout.EAST);
		bottomStuff.add(frameCounter,BorderLayout.SOUTH);
		bottomStuffWrap.add(bottomStuff,BorderLayout.EAST);
		loadWrap.add(loadBtn,BorderLayout.EAST);
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
		aboutFrame.setLocation(150,150);
		aboutFrame.setResizable(false);
		// end credits

		frame.add(bottomStuffWrap, BorderLayout.CENTER);
		frame.add(loadWrap,BorderLayout.NORTH);
		frame.setSize(d);
		frame.setMinimumSize(d);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.setLocation(300,300);
		frame.setJMenuBar(menu);
		
		// file explorer
		final JFileChooser explorer = new JFileChooser();
		FileNameExtensionFilter sprFilter =
				new FileNameExtensionFilter("Sprite files", new String[] { "spr" });
		// can't clear text due to wonky code
		// have to set a blank file instead
		final File EEE = new File("");

		Timer tock = run.getTimer();
		tock.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				frameCur.setText("" + run.getFrame());
			}
		});
		// load sprite file
		loadBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				explorer.setSelectedFile(EEE);
				explorer.setFileFilter(sprFilter);
				int option = explorer.showOpenDialog(loadBtn);
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
					else
						return;
				}
				explorer.removeChoosableFileFilter(sprFilter);

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
		g.drawImage(img, x + 10, y + 10, null);
	}
}
