package de.foxysoft.idm.devtools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class Tern4IDM {
	private static class ShowMessageOnlyException extends Exception {
		static final long serialVersionUID = 1L;

		public ShowMessageOnlyException(String msg) {
			super(msg);
		}

	}

	private static final String EXE_NAME = "t4i";

	private static boolean g_verbose;

	private static void trc(String msg) {
		if (g_verbose) {
			System.err.println(msg);
		}
	}

	private static String getMandatoryParam(CommandLine line, char p)
			throws Exception {
		String v = line.getOptionValue(p);
		if (v == null || v.equals("")) {
			throw new ShowMessageOnlyException("Missing mandatory parameter -"
					+ p + "; try " + EXE_NAME + " --help");
		}
		return v;
	}// getMandatoryParam

	/**
	 * Create this application's private directory if it doesn't exist yet. 
	 * @return
	 * @throws Exception
	 */
	private static File createAppDir() throws Exception {
		String home = System.getProperty("user.home");
		File appDir = new File(home, "." + EXE_NAME);
		if (!appDir.exists()) {
			boolean result = appDir.mkdir();
			if (!result) {
				throw new Exception("Failed to create "
						+ appDir.getCanonicalPath());
			}
		} else {
			if (!appDir.isDirectory()) {
				throw new Exception("Can't create directory "
						+ appDir.getCanonicalPath()
						+ " because there's a file with the same name");
			}
		}
		return appDir;

	}

	/**
	 * Download file from URL into destination directory
	 * @param fromUrl
	 * @param toDir
	 * @return
	 * @throws Exception
	 */
	private static File downloadFile(String fromUrl, File toDir)
			throws Exception {
		final String M = "downloadFile: ";
		trc(M + "Entering fromUrl=" + fromUrl);

		URL oUrl = new URL(fromUrl);
		trc(M + "oUrl=" + oUrl);
		String filename = new File(oUrl.getPath()).getName();
		trc(M+"filename="+filename);

		ReadableByteChannel rbc = null;
		FileOutputStream fos = null;
		File outputFile = new File(toDir, filename);

		try {
			rbc = Channels.newChannel(oUrl.openStream());

			fos = new FileOutputStream(outputFile);
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		} finally {
			if (fos != null) {
				try {
					fos.flush();
				} catch (Exception e) {
				}
				try {
					fos.close();
				} catch (Exception e) {
				}
			}// if (fos != null) {
			if (rbc != null) {
				try {
					rbc.close();
				} catch (Exception e) {
				}
			}// if (rbc != null) {
		}// finally
		return outputFile;
	}

	/**
	 * Extract ZIP file into destination directory
	 * @param zipFile
	 * @param toDir
	 * @throws Exception
	 */
	private static void unzipFile(File zipFile, File toDir) throws Exception {

		final String M = "unzipFile ";
		trc(M + "Enterig jar=" + zipFile + ", toDir=" + toDir);

		byte[] buffer = new byte[1024];
		FileOutputStream fos = null;
		ZipInputStream zis = null;

		try {

			zis = new ZipInputStream(new FileInputStream(zipFile));
			ZipEntry ze = null;
			File outParent = null;

			while ((ze = zis.getNextEntry()) != null) {
				if (ze.isDirectory()) {
					continue;
				}
				String outFileName = ze.getName();
				File outFile = new File(toDir + File.separator + outFileName);

				trc(M + "Processing " + outFile.getCanonicalPath());

				outParent = new File(outFile.getParent());
				if (!outParent.exists()) {
					boolean result = outParent.mkdirs();
					if (!result) {
						throw new Exception("Failed to create directory "
								+ outParent.getCanonicalPath());
					}
				}

				try {
					fos = new FileOutputStream(outFile);

					int len;
					while ((len = zis.read(buffer)) != -1) {
						fos.write(buffer, 0, len);
					}// while
				} finally {
					if (fos != null) {
						try {
							fos.flush();
						} catch (Exception e) {
						}
						try {
							fos.close();
						} catch (Exception e) {
						}
					}
				}

				zis.closeEntry();
			}// while ((ze = zis.getNextEntry()) != null) {
		}// try
		finally {
			if (zis != null) {
				try {
					zis.close();
				} catch (Exception e) {
				}
			}
		}// finally
	}

	/**
	 * Perform actual business logic
	 * @param line
	 * @throws Exception
	 */
	private static void doWork(CommandLine line) throws Exception {
		File appDir = createAppDir();
		String url = getMandatoryParam(line, 'u');
		if(!url.endsWith("/")) {
			url+="/";
		}
		url+="artifacts.jar";
		
		File jar = downloadFile(url, appDir);
		unzipFile(jar, appDir);
	}

	/**
	 * Parse command line and show help, if required.
	 * Otherwise call doWork() to perform actual business logic.
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		final String M = "main: ";

		// create the command line parser
		CommandLineParser parser = new DefaultParser();

		// create the Options
		Options options = new Options();

		options.addOption(Option //
				.builder("u") // short name
				.longOpt("url") // long name
				.required(false) //
				.desc("Eclipse Repository URL") // description
				.hasArg() //
				.argName("REPO_URL") //
				.build());

		options.addOption(Option //
				.builder("d") // short name
				.longOpt("dir") // long name
				.required(false) //
				.desc("Directory to store typelib") // description
				.hasArg() //
				.argName("DIR") //
				.build());

		options.addOption(Option //
				.builder("v") // short name
				.longOpt("verbose") // long name
				.required(false) //
				.desc("Output trace messages to STDERR") // description
				.build());

		options.addOption(Option //
				.builder("h") // short name
				.longOpt("help") // long name
				.required(false) //
				.desc("Display this help") // description
				.build());

		CommandLine line = null;

		try {

			// parse the command line arguments
			line = parser.parse(options, args);

			if (!line.hasOption('h')) {

				g_verbose = line.hasOption('v');
				trc(M + "Entering args=" + args);
				trc(M + "CLASSPATH=" + System.getProperty("java.class.path"));

				doWork(line);

			}// if(!line.hasOption('h'))
			else {
				HelpFormatter formatter = new HelpFormatter();
				formatter.setLeftPadding(4);
				String br = formatter.getNewLine();
				char[] alp = new char[formatter.getLeftPadding()];
				Arrays.fill(alp, ' ');
				String lp = new String(alp);
				String header = "Generate Tern typelib from SAP(R) IDM DevStudio help"
						+ br //
						+ br //
						+ "OPTIONS:" //
						+ br //
				;

				String footer = br //
						+ "EXAMPLES:"
						+ br //
						+ EXE_NAME
						+ " -u https://tools.hana.ondemand.com/neon" //
						+ br
						+ lp
						+ "Download help file from SAP(R) Eclipse Neon repository"
						+ br //
						+ lp
						+ "and generate Tern typelib for SAP(R) IDM into current directory"
						+ br //
						+ br //
				;

				// Output options in the order they have been declared
				formatter.setOptionComparator(null);

				formatter.printHelp(EXE_NAME, header, options, footer, true);
			}// else
		} catch (org.apache.commons.cli.MissingArgumentException mae) {
			System.err.println(mae.getMessage() + "; try " + EXE_NAME
					+ " --help");
		} catch (ShowMessageOnlyException smoe) {
			System.err.println(smoe.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}// main

}// Tern4IDM
