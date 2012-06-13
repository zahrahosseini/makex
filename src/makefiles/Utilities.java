package makefiles;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintWriter;


public abstract class Utilities {
	
	// constants
		public static final String HEAD = "head-";
		public static final String INIT = "init-";
		public static final String CORE = "core-";
		public static final String LIBS = "libs-";
		public static final String DRIVERS = "drivers-";
		public static final String NET = "net-";
		public static final String OBJ = "obj-";
		public static final String PLAT = "plat-";
		public static final String MACHINE = "machine-";
		public static final String ARCH = "arch-";
		public static final String IFEQ = "ifeq";
		public static final String IFNEQ = "ifneq";
		public static final String ENDIF = "endif";
		public static final String IFDEF = "ifdef";
		public static final String ELSE = "else";
		public static final String IFNDEF = "ifndef";
		public static enum KEYWORDS {

			HEAD, INIT, CORE, LIBS, DRIVERS, NET, OBJ, PLAT, MACHINE, ARCH, NONE
		};
		
		public static void writeArchNames() {
			PrintWriter writer = null;
			try {
				writer = new PrintWriter((new File("archNames.txt")));
				File archDir = new File("./arch/");
				FileFilter directoryFilter = new FileFilter() {

					public boolean accept(File pathname) {
						return pathname.isDirectory()
								&& !pathname.getName().startsWith(".");
					}
				};
				File[] directories = archDir.listFiles(directoryFilter);
				if (directories == null) {
					return;
				}
				for (File dir : directories) {
					String archName = dir.getName().trim();
					// System.out.println("archName: " + archName);
					writer.println(archName);
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			} finally {
				writer.close();
			}
		}
		
		public static String getIncludedFile(String line, String arch) {
			// include $(srctree)/arch/m68k/Makefile_mm
			String includedFile = line.trim().substring(7).trim();
			includedFile = replaceVariables(includedFile, arch);

			return includedFile;

		}
		
		public static KEYWORDS findKeyword(String line) {

			int index = -1;

			// look for the keywords, and make sure they are at the beginning of the
			// line
			if (line.startsWith(HEAD)) {
				index = HEAD.length();
				return KEYWORDS.HEAD;
			} else if ((index = line.indexOf(INIT)) != -1 && (index == 0)) {
				index = INIT.length();
				return KEYWORDS.INIT;
			} else if ((index = line.indexOf(CORE)) != -1 && (index == 0)) {
				index = CORE.length();
				return KEYWORDS.CORE;
			} else if ((index = line.indexOf(LIBS)) != -1 && (index == 0)) {
				index = LIBS.length();
				return KEYWORDS.LIBS;
			} else if ((index = line.indexOf(DRIVERS)) != -1 && (index == 0)) {
				index = DRIVERS.length();
				return KEYWORDS.DRIVERS;
			} else if ((index = line.indexOf(NET)) != -1 && (index == 0)) {
				index = NET.length();
				return KEYWORDS.NET;
			} else if ((index = line.indexOf(OBJ)) != -1 && (index == 0)) {
				index = OBJ.length();
				return KEYWORDS.OBJ;
			} else if ((index = line.indexOf(PLAT)) != -1 && (index == 0)) {
				index = PLAT.length();
				return KEYWORDS.PLAT;
			} else if ((index = line.indexOf(MACHINE)) != -1 && (index == 0)) {
				index = MACHINE.length();
				return KEYWORDS.MACHINE;
			} else if ((index = line.indexOf(ARCH)) != -1 && (index == 0)) {
				index = ARCH.length();
				return KEYWORDS.ARCH;
			}

			return KEYWORDS.NONE;
		}

		
	// parsing helpers
		public static boolean isConditional(String line) {
			line = line.trim();

			return (line.startsWith("ifdef") || line.startsWith("ifndef")
					|| line.startsWith("ifeq") || line.startsWith("ifneq") || line
						.startsWith("define"));
		}

		public static String checkToken(String token, KEYWORDS keyword) {

			if (keyword == KEYWORDS.PLAT) {
				token = "plat-" + token + "/";
			} else if (keyword == KEYWORDS.MACHINE) {
				token = "mach-" + token + "/";
			} else if (keyword == KEYWORDS.ARCH) {
				token = "arch-" + token + "/";
			}

			return token;
		}

		public static boolean fileExists(String entry, String path) {
			String fileName = entry.trim();

			File file = new File(path + fileName + ".c");

			if (file.exists()) {
				// System.out.println("Returning true");
				return true;
			} else {
				file = new File(path + fileName + ".S");

				if (file.exists()) {
					return true;
				}
			}

			return false;
		}

		public static String replaceVariables(String token, String arch) {
			String newToken = token.replaceAll("\\$\\(ARCH\\)", arch);
			newToken = newToken.replaceAll("\\$\\(ARCH_DIR\\)", "arch/" + arch);
			newToken = newToken.replaceAll("\\$\\(srctree\\)",
					(new File(".")).getPath());

			return newToken;
		}



}
