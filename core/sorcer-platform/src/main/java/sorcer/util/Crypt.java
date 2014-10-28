/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sorcer.util;

import java.util.Date;

/**
 * This is an implementation of the crypt() algorithm of DES encryption.
 * 
 * Code is ported from c to java by Rob Vermeulen (6-14-96). C-code was written
 * by David Ljung (3-25-94).
 * 
 * Tables were taken form simple_crypt.c by Michael Glad (glad@daimi.aau.dk)
 * (Copyright 1996 under GNU Public License)
 * 
 * The crypt() function is used for attribute encryption in metaweb.
 */
public class Crypt {
	static int sub[] = new int[48];

	// TABLES
	/* Expansion table (32 to 48) */
	int E_p[] = { 32, 1, 2, 3, 4, 5, 4, 5, 6, 7, 8, 9, 8, 9, 10, 11, 12, 13,
			12, 13, 14, 15, 16, 17, 16, 17, 18, 19, 20, 21, 20, 21, 22, 23, 24,
			25, 24, 25, 26, 27, 28, 29, 28, 29, 30, 31, 32, 1 };

	/* Permutation Choice 1 for subkey generation (64/56 to 56) */
	int PC1_p[] = { 57, 49, 41, 33, 25, 17, 9, 1, 58, 50, 42, 34, 26, 18, 10,
			2, 59, 51, 43, 35, 27, 19, 11, 3, 60, 52, 44, 36, 63, 55, 47, 39,
			31, 23, 15, 7, 62, 54, 46, 38, 30, 22, 14, 6, 61, 53, 45, 37, 29,
			21, 13, 5, 28, 20, 12, 4 };

	/* Permutation Choice 2 for subkey generation (56 to 48) */
	int PC2_p[] = { 14, 17, 11, 24, 1, 5, 3, 28, 15, 6, 21, 10, 23, 19, 12, 4,
			26, 8, 16, 7, 27, 20, 13, 2, 41, 52, 31, 37, 47, 55, 30, 40, 51,
			45, 33, 48, 44, 49, 39, 56, 34, 53, 46, 42, 50, 36, 29, 32 };

	/* Number of rotations for the iteration of key scheduling */
	/* The concept of a table here doesn't fit our behavioral model */
	/* This will be logic in our final design */
	int keyrots[] = { 1, 1, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 1 };

	/*
	 * Selection blocks There are 8 sblocks, each of which is referenced by a 2
	 * bit value which picks the row, and a 4 bit value which picks the column
	 * This number is then the 4 bit output for that select block
	 */
	int sblocks[][][] = {
			{ { 14, 4, 13, 1, 2, 15, 11, 8, 3, 10, 6, 12, 5, 9, 0, 7 },
					{ 0, 15, 7, 4, 14, 2, 13, 1, 10, 6, 12, 11, 9, 5, 3, 8 },
					{ 4, 1, 14, 8, 13, 6, 2, 11, 15, 12, 9, 7, 3, 10, 5, 0 },
					{ 15, 12, 8, 2, 4, 9, 1, 7, 5, 11, 3, 14, 10, 0, 6, 13 } },

			{ { 15, 1, 8, 14, 6, 11, 3, 4, 9, 7, 2, 13, 12, 0, 5, 10 },
					{ 3, 13, 4, 7, 15, 2, 8, 14, 12, 0, 1, 10, 6, 9, 11, 5 },
					{ 0, 14, 7, 11, 10, 4, 13, 1, 5, 8, 12, 6, 9, 3, 2, 15 },
					{ 13, 8, 10, 1, 3, 15, 4, 2, 11, 6, 7, 12, 0, 5, 14, 9 } },

			{ { 10, 0, 9, 14, 6, 3, 15, 5, 1, 13, 12, 7, 11, 4, 2, 8 },
					{ 13, 7, 0, 9, 3, 4, 6, 10, 2, 8, 5, 14, 12, 11, 15, 1 },
					{ 13, 6, 4, 9, 8, 15, 3, 0, 11, 1, 2, 12, 5, 10, 14, 7 },
					{ 1, 10, 13, 0, 6, 9, 8, 7, 4, 15, 14, 3, 11, 5, 2, 12 } },

			{ { 7, 13, 14, 3, 0, 6, 9, 10, 1, 2, 8, 5, 11, 12, 4, 15 },
					{ 13, 8, 11, 5, 6, 15, 0, 3, 4, 7, 2, 12, 1, 10, 14, 9 },
					{ 10, 6, 9, 0, 12, 11, 7, 13, 15, 1, 3, 14, 5, 2, 8, 4 },
					{ 3, 15, 0, 6, 10, 1, 13, 8, 9, 4, 5, 11, 12, 7, 2, 14 } },

			{ { 2, 12, 4, 1, 7, 10, 11, 6, 8, 5, 3, 15, 13, 0, 14, 9 },
					{ 14, 11, 2, 12, 4, 7, 13, 1, 5, 0, 15, 10, 3, 9, 8, 6 },
					{ 4, 2, 1, 11, 10, 13, 7, 8, 15, 9, 12, 5, 6, 3, 0, 14 },
					{ 11, 8, 12, 7, 1, 14, 2, 13, 6, 15, 0, 9, 10, 4, 5, 3 } },

			{ { 12, 1, 10, 15, 9, 2, 6, 8, 0, 13, 3, 4, 14, 7, 5, 11 },
					{ 10, 15, 4, 2, 7, 12, 9, 5, 6, 1, 13, 14, 0, 11, 3, 8 },
					{ 9, 14, 15, 5, 2, 8, 12, 3, 7, 0, 4, 10, 1, 13, 11, 6 },
					{ 4, 3, 2, 12, 9, 5, 15, 10, 11, 14, 1, 7, 6, 0, 8, 13 } },

			{ { 4, 11, 2, 14, 15, 0, 8, 13, 3, 12, 9, 7, 5, 10, 6, 1 },
					{ 13, 0, 11, 7, 4, 9, 1, 10, 14, 3, 5, 12, 2, 15, 8, 6 },
					{ 1, 4, 11, 13, 12, 3, 7, 14, 10, 15, 6, 8, 0, 5, 9, 2 },
					{ 6, 11, 13, 8, 1, 4, 10, 7, 9, 5, 0, 15, 14, 2, 3, 12 } },

			{ { 13, 2, 8, 4, 6, 15, 11, 1, 10, 9, 3, 14, 5, 0, 12, 7 },
					{ 1, 15, 13, 8, 10, 3, 7, 4, 12, 5, 6, 11, 0, 14, 9, 2 },
					{ 7, 11, 4, 1, 9, 12, 14, 2, 0, 6, 10, 13, 15, 3, 5, 8 },
					{ 2, 1, 14, 7, 4, 10, 8, 13, 15, 12, 9, 0, 3, 5, 6, 11 } } };

	/* Permutation P for after sblocks */
	int P_p[] = { 16, 7, 20, 21, 29, 12, 28, 17, 1, 15, 23, 26, 5, 18, 31, 10,
			2, 8, 24, 14, 32, 27, 3, 9, 19, 13, 30, 6, 22, 11, 4, 25 };

	/*
	 * Inverse permutation of IP for end Temporary - the true behavior will be
	 * implemented in a shift out register (Look at the pattern obvious in an
	 * 8x8 layout)
	 */
	int IPinv_p[] = { 40, 8, 48, 16, 56, 24, 64, 32, 39, 7, 47, 15, 55, 23, 63,
			31, 38, 6, 46, 14, 54, 22, 62, 30, 37, 5, 45, 13, 53, 21, 61, 29,
			36, 4, 44, 12, 52, 20, 60, 28, 35, 3, 43, 11, 51, 19, 59, 27, 34,
			2, 42, 10, 50, 18, 58, 26, 33, 1, 41, 9, 49, 17, 57, 25 };

	// CODE
	private void pr_bits(int[] s, int amt) {
		int i, j, x;
		amt /= 8;
		for (i = 0; i < amt; i++) {
			x = 0;
			for (j = 0; j < 8; j++) {
				x |= s[i * 8 + j] << (7 - j);
			}
			Integer integ = new Integer(x);
			System.out.print(integ.toString(x, 16) + " ");
		}
		System.out.println();
	};

	private void permute(int[] by, int amt, int[] in, int[] out) {
		for (amt--; amt >= 0; amt--)
			out[amt] = in[by[amt] - 1];
	};

	private void do_sblocks(int[] in, int[] out) {
		int i, j, val;
		for (i = 0; i < 8; i++) {
			val = sblocks[i][in[i * 6] << 1 | in[i * 6 + 5]][in[i * 6 + 1] << 3
					| in[i * 6 + 2] << 2 | in[i * 6 + 3] << 1 | in[i * 6 + 4]];
			out[i * 4] = val >> 3 & 1;
			out[i * 4 + 1] = val >> 2 & 1;
			out[i * 4 + 2] = val >> 1 & 1;
			out[i * 4 + 3] = val & 1;
		}

		// Debug information
		// System.out.println("Before SB: ");
		// pr_bits(in,48);
		// System.out.println("After SB : ");
		// pr_bits(out,32);
	};

	private int ascii_to_bin(char c) {
		if (c >= 'a')
			return (c - 59);
		else if (c >= 'A')
			return (c - 53);
		else
			return (c - '.');
	};

	private char bin_to_ascii(int c) {
		if (c >= 38)
			return ((char) (c - 38 + 'a'));
		else if (c >= 12)
			return ((char) (c - 12 + 'A'));
		else
			return ((char) (c + '.'));
	};

	private void load_salt(int[] saltmask, char[] salt) {
		int tot, i;

		// Debug information
		// System.out.println("*"+ascii_to_bin(salt[0])+", "+ascii_to_bin(salt[1])+", "+(int)salt[0]+", "+(int)salt[1]+"Hatjie");

		tot = ascii_to_bin(salt[0]) | (ascii_to_bin(salt[1]) << 6);
		for (i = 0; i < 12; i++)
			saltmask[i] = tot >> i & 1;
	};

	private void do_salt(int[] bits, int[] saltmask) {
		int i, t;

		// Debug information
		// print_bits(saltmask,12);
		for (i = 0; i < 12; i++) {
			if (saltmask[i] != 0) {
				t = bits[i];
				bits[i] = bits[24 + i];
				bits[24 + i] = t;
			}
		}
	};

	private void load_key(int[] ikey, char[] password) {
		int i, j;
		int tmp[] = new int[64];

		for (i = 0; i < 8; i++) {
			for (j = 0; j < 8; j++) {
				tmp[i * 8 + j] = (password[i] >> (6 - j)) & 1;
			}
		}
		permute(PC1_p, 56, tmp, ikey);

		// Debug information
		// System.out.print("Password bits: ");
		// pr_bits(tmp,64);
		// System.out.print("Loaded (after PC 1): ");
		// pr_bits(ikey,56);

	};

	private void subkey(int[] ikey, int iter) {
		int rots, i, tmp0l, tmp1l, tmp0r, tmp1r;

		rots = keyrots[iter];

		tmp0l = ikey[0];
		tmp1l = ikey[1];
		tmp0r = ikey[28];
		tmp1r = ikey[29];
		for (i = 0; i < 28 - rots; i++) {
			ikey[i] = ikey[i + rots];
			ikey[28 + i] = ikey[28 + i + rots];
		}
		if (rots == 2) {
			ikey[26] = tmp0l;
			ikey[27] = tmp1l;
			ikey[54] = tmp0r;
			ikey[55] = tmp1r;
		} else {
			ikey[27] = tmp0l;
			ikey[55] = tmp0r;
		}

		permute(PC2_p, 48, ikey, sub);
	};

	private void xor(int[] src1, int[] src2, int index, int num) {
		int i;
		for (i = 0; i < num; i++) {
			src1[i] = (src1[i] ^ src2[index + i]) & 1;
		}
	};

	private void print_bits(int[] s, int amt) {

		int i;
		for (i = 0; i < amt; i++)
			System.out.print("" + s[i]);
		System.out.println("");
	};

	private void do_f(int[] in, int index, int[] out, int iter, int[] ikey,
			int[] saltmask) {
		int i, tmp48[] = new int[48], tmp32[] = new int[32], skey[];

		for (i = 0; i < 32; i++) {
			tmp32[i] = in[i + index];
		}
		permute(E_p, 48, tmp32, tmp48);
		// Debug information
		// System.out.print("Expansion: "); pr_bits(tmp48,48);
		do_salt(tmp48, saltmask);

		// Debug information
		// System.out.println("TMP48 --> ");
		// pr_bits(tmp48,48);
		subkey(ikey, iter);
		// System.out.println("SUBKEY --> ");
		// print_bits(sub,48);
		skey = sub;
		xor(tmp48, skey, 0, 48); // Goed
		do_sblocks(tmp48, tmp32);
		permute(P_p, 32, tmp32, out);
	};

	private char[] mycrypt(char[] password, char[] salt) {
		int bits[] = new int[64];
		int outl[] = new int[32];
		int outr[] = new int[32];
		int done[] = new int[66]; // In c-code array-size was 64 !?!
		int i, j, dess, iters;
		int ikey[] = new int[56];
		int saltmask[] = new int[12];
		char[] answer = new char[14];

		for (i = 0; i < 64; i++) {
			bits[i] = 0;
		}
		load_key(ikey, password);
		load_salt(saltmask, salt);

		// Debug information
		// System.out.print("Key : ");
		// pr_bits(ikey,56);
		// System.out.print("Salt: ");
		// pr_bits(saltmask,12);

		for (dess = 0; dess < 25; dess++) {
			for (iters = 0; iters < 16; iters += 2) { // Debug information
				// System.out.print("Mycrypt "+iters+" : ");
				// pr_bits(bits,64);
				do_f(bits, 32, outl, iters, ikey, saltmask);
				xor(outl, bits, 0, 32);
				do_f(outl, 0, outr, iters + 1, ikey, saltmask);
				xor(outr, bits, 32, 32);

				if (iters != 14) {
					for (i = 0; i < 32; i++) {
						bits[i] = outl[i];
						bits[i + 32] = outr[i];
					}
				} else {
					for (i = 0; i < 32; i++) {
						bits[i] = outr[i];
						bits[i + 32] = outl[i];
					}
				}
				// Debug information
				// if(iters==14)
				// { System.out.print("End :    ");
				// pr_bits(bits,64);
				// }

			}
		}
		permute(IPinv_p, 64, bits, done);

		// Debug information
		// System.out.print("OUT BITS: ");
		// pr_bits(done,64);

		answer[0] = (char) salt[0];
		answer[1] = (char) salt[1];
		for (i = 0; i < 11; i++) {
			char c = 0;
			for (j = 0; j < 6; j++) {
				c |= done[6 * i + j] << (5 - j);
			}
			answer[i + 2] = bin_to_ascii(c);
		}
		// anwser[i+2]='\0';
		return answer;
	};

	/**
	 * This method decodes the given password Encryption is based on the one way
	 * DES encryption
	 * 
	 * @return The encrypted password
	 * @param passwd
	 *            is the password to encrypt
	 * @param s
	 *            is salt
	 */

	public String crypt(String passwd, String s) {
		char[] password = new char[9];
		char[] salt = new char[3];
		String uitkomst;

		passwd.getChars(0, passwd.length() >= 8 ? 8 : passwd.length(),
				password, 0);
		s.getChars(0, 2, salt, 0);

		uitkomst = new String().valueOf(mycrypt(password, salt));
		return (uitkomst.substring(0, 13));
	}

	/**
	 * main method allows you to call crypt function from command line usage :
	 * java Crypt <password> <salt>
	 */
	public static void main(String argv[]) {
		Crypt c = new Crypt();
		String pw = null;
		String sa = null;
		try {
			pw = argv[0];
			sa = argv[1];
		} catch (Exception e) {
			System.err.println("wrong parameter");
			return;
		}
		Date d1 = new Date();
		long t1 = d1.getTime();
		System.out.println("Encrypting " + pw + " with salt " + sa);
		System.out.println("Encrypted password " + c.crypt(pw, sa));
		Date d2 = new Date();
		long t2 = d2.getTime();
		System.out.println("Encryption took " + (t2 - t1) + " milliseconds = "
				+ (t2 - t1) / 1000 + " seconds");
	}

}
