import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class test extends JPanel {

	private static final int CHUNK_SIZE = 64;
	private static final int blockSizeY = 5;
	private static final int blockSizeX = 5;
	private Complex[][] results;
	private boolean logModeEnabled;
	private int size = 50;

	/**
	 * @param args
	 * @throws LineUnavailableException
	 */
	public static void main(String[] args) {

		JFrame frame = new JFrame("Basic Shapes");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(new test());
		frame.setSize(350, 250);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	test() {
		final AudioFormat format = getFormat(); // Fill AudioFormat with the
												// wanted settings
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
		TargetDataLine line = null;
		try {
			line = (TargetDataLine) AudioSystem.getLine(info);
			line.open(format);
			line.start();
		} catch (LineUnavailableException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.exit(-1);
		}

		// In another thread I start:

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		// boolean running = true;

		byte[] buffer = new byte[10240];
		try {
			// while (running) {
			int count = line.read(buffer, 0, buffer.length);
			if (count > 0) {
				out.write(buffer, 0, count);
			}
			// }
			out.close();
		} catch (IOException e) {
			System.err.println("I/O problems: " + e);
			System.exit(-1);
		}
		// fft

		byte audio[] = out.toByteArray();

		final int totalSize = audio.length;

		int amountPossible = (int) Math.ceil((double) totalSize
				/ (double) CHUNK_SIZE);
		System.out.println("amountPossible = " + amountPossible);

		for (byte b : audio) {
			System.out.println(b);
		}
		// When turning into frequency domain we'll need complex numbers:
		results = new Complex[amountPossible][];

		// For all the chunks:
		for (int times = 0; times < amountPossible; times++) {
			Complex[] complex = new Complex[CHUNK_SIZE];
			for (int i = 0; i < CHUNK_SIZE; i++) {
				// Put the time domain data into a complex number with imaginary
				// part as 0:
				complex[i] = new Complex(audio[(times * CHUNK_SIZE) + i], 0);
			}
			// Perform FFT analysis on the chunk:
			results[times] = fft(complex);
			for (Complex c : results[times]) {
				System.out.println(c);
			}
		}
	}

	public void paint(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setColor(new Color(150, 150, 150));

		for (int i = 0; i < results.length; i++) {
			int freq = 1;
			for (int line = 1; line < size; line++) {
				// To get the magnitude of the sound at a given frequency slice
				// get the abs() from the complex number.
				// In this case I use Math.log to get a more managable number
				// (used for color)
				double magnitude = Math.log(results[i][freq].abs() + 1);

				// The more blue in the color the more intensity for a given
				// frequency point:
				g2d.setColor(new Color(0, (int) magnitude * 10,
						(int) magnitude * 20));
				// Fill:
				g2d.fillRect(i * blockSizeX, (size - line) * blockSizeY,
						blockSizeX, blockSizeY);

				// I used a improviced logarithmic scale and normal scale:
				if (logModeEnabled && (Math.log10(line) * Math.log10(line)) > 1) {
					freq += (int) (Math.log10(line) * Math.log10(line));
				} else {
					freq++;
				}
			}
		}
	}

	static Complex[] fft(Complex[] a) {
		return fft(a, false);
	}

	// http://e-maxx.ru/algo/fft_multiply
	static Complex[] fft(Complex[] a, boolean invert) {
		int n = (int) a.length;
		if (n == 1)
			return a;

		Complex[] a0 = new Complex[n / 2];
		Complex[] a1 = new Complex[n / 2];
		for (int i = 0, j = 0; i < n; i += 2, ++j) {
			a0[j] = a[i];
			a1[j] = a[i + 1];
		}
		Complex[] a2 = fft(a0, invert);
		Complex[] a3 = fft(a1, invert);
		Complex[] res = a;

		double ang = 2 * Math.PI / n * (invert ? -1 : 1);
		Complex w = new Complex(1);
		Complex wn = new Complex(Math.cos(ang), Math.sin(ang));
		for (int i = 0; i < n / 2; ++i) {
			res[i] = a2[i].plus(w.times(a3[i]));
			res[i + n / 2] = a2[i].minus(w.times(a3[i]));
			if (invert)
				res[i] = res[i].divides(Complex.TWO);
			res[i + n / 2] = res[i + n / 2].divides(Complex.TWO);
			w = w.times(wn);
		}
		return res;
	}

	private static AudioFormat getFormat() {
		float sampleRate = 44100;
		int sampleSizeInBits = 8;
		int channels = 1; // mono
		boolean signed = true;
		boolean bigEndian = true;
		return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed,
				bigEndian);
	}

}
