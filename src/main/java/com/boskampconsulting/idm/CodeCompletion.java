package com.boskampconsulting.idm;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.w3c.dom.Document;

public class CodeCompletion {
	private static class ShowMessageOnlyException extends Exception {
		static final long serialVersionUID = 1L;

		public ShowMessageOnlyException(String msg) {
			super(msg);
		}

	}

	private static final String EXE_NAME = "idmcc";

	private static boolean g_verbose;

	private static boolean g_force;

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
	 * 
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

	private static class WorkDirs {
		File root;
		File jars;
		File html;
		File tern;
		File snippets;
	}

	private static WorkDirs createWorkDirs(File parent, String idmVersion)
			throws Exception {
		WorkDirs result = new WorkDirs();
		result.root = createDirectory(parent, idmVersion);
		result.jars = createDirectory(result.root, "jars");
		result.html = createDirectory(result.root, "html");
		result.tern = createDirectory(result.root, "tern");
		result.snippets = createDirectory(result.root, "snippets");
		return result;
	}

	private static File createDirectory(File parent, String name)
			throws Exception {
		boolean result;
		File dir;
		dir = new File(parent, name);
		if (!dir.exists()) {
			result = dir.mkdir();
			if (!result) {
				throw new Exception("Failed to create "
						+ dir.getCanonicalPath());
			}
		}
		return dir;
	}

	/**
	 * Download file from URL into destination directory
	 * 
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
		trc(M + "filename=" + filename);

		ReadableByteChannel rbc = null;
		FileOutputStream fos = null;
		File outputFile = new File(toDir, filename);
		if (g_force || !outputFile.exists()) {
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
		}
		return outputFile;
	}

	/**
	 * Extract ZIP file into destination directory
	 * 
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
				if (!g_force && outFile.exists()) {
					continue;
				}

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
	 * Transform xmlFile using XSL stylesheet styleSheetName and return result
	 * as string
	 * 
	 * @param xmlFile
	 * @param styleSheetName
	 * @return
	 * @throws Exception
	 */
	private static String xslTransform(File xmlFile, String styleSheetName,
			Map<String, String> xslParams) throws Exception {
		final String M = "xslTransform: ";
		trc(M + "Entering xmlFile=" + xmlFile + ", styleSheetName="
				+ styleSheetName);
		// Use Saxon to support XSLT 2.0
		TransformerFactory tf = new net.sf.saxon.TransformerFactoryImpl();
		InputStream styleStream = CodeCompletion.class
				.getResourceAsStream(styleSheetName);
		trc(M + "styleStream=" + styleStream);
		if (styleStream == null) {
			throw new Exception("XSL stylesheet " + styleSheetName
					+ " not found");
		}
		Source s = new StreamSource(styleStream);
		Transformer t = tf.newTransformer(s);
		if (xslParams != null) {
			Iterator<Map.Entry<String, String>> iter = xslParams.entrySet()
					.iterator();
			while (iter.hasNext()) {
				Map.Entry<String, String> next = iter.next();
				trc(M + "Setting parameter " + next.getKey() + "="
						+ next.getValue());
				t.setParameter(next.getKey(), next.getValue());
			}
		}// if(xslParams != null)
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(xmlFile);
		Source xmlSource = new DOMSource(doc);

		// Must set system ID to avoid
		// XTDE1162: Relative URI passed to document() function
		xmlSource.setSystemId(xmlFile.getCanonicalPath());
		trc(M + "xmlSource.getSystemId()=" + xmlSource.getSystemId());

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		StreamResult outputTarget = new StreamResult(baos);
		t.transform(xmlSource, outputTarget);
		String result = baos.toString(doc.getInputEncoding());
		return result;
	}

	private static String xslTransform(File xmlFile, String styleSheetName)
			throws Exception {
		return xslTransform(xmlFile, styleSheetName, null);
	}

	/**
	 * Write string to file without any implicit charset conversion, as would be
	 * done by java.io.PrintStream
	 * 
	 * @param content
	 * @param charsetName
	 * @param parent
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	private static File writeFile(String content, String charsetName,
			File parent, String filename) throws Exception {
		FileOutputStream fos = null;
		File outputFile = null;
		try {
			outputFile = new File(parent, filename);
			byte[] contentBytes = content.getBytes(charsetName);
			fos = new FileOutputStream(outputFile);
			fos.write(contentBytes, 0, contentBytes.length);
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
		}// finally
		return outputFile;
	}

	/**
	 * Perform actual business logic
	 * 
	 * @param line
	 * @throws Exception
	 */
	private static void doWork(CommandLine line) throws Exception {
		final String M = "doWork: ";
		trc(M + "Entering line=" + line);
		File appDir = createAppDir();
		String baseUrl = getMandatoryParam(line, 'u');

		if (!baseUrl.endsWith("/")) {
			baseUrl += "/";
		}

		trc(M + "baseUrl=" + baseUrl);

		String artifactsJarUrl = baseUrl + "artifacts.jar";
		File artifactsJarFile = downloadFile(artifactsJarUrl, appDir);
		unzipFile(artifactsJarFile, appDir);

		String idmVersion = xslTransform(new File(appDir, "artifacts.xml"),
				"fi_artifacts.xsl");
		trc(M + "idmVersion=" + idmVersion);

		WorkDirs w = createWorkDirs(appDir, idmVersion);

		String helpJarUrl = baseUrl + "plugins/com.sap.idm.dev-ui-help_"
				+ idmVersion + ".jar";
		File helpJarFile = downloadFile(helpJarUrl, w.jars);
		unzipFile(helpJarFile, w.html);

		File tocFile = new File(w.html, "toc.xml");

		String helpXmlContent = xslTransform(tocFile, "fi_help_to_xml.xsl");

		File helpXmlFile = writeFile(helpXmlContent, "UTF-8", w.root,
				"idm_internal_functions_" + idmVersion + ".xml");

		String jsonContent = xslTransform(helpXmlFile, "fi_xml_to_json.xsl");
		File jsonFile = writeFile(jsonContent, "UTF-8", w.tern, "idm_ternlib_"
				+ idmVersion + ".json");
		System.out.println("Generated " + jsonFile.getCanonicalPath());

		Map<String, String> xslParams = new HashMap<String, String>();
		xslParams.put("iv_output_dir", w.snippets.getCanonicalPath());
		xslTransform(helpXmlFile, "fi_xml_to_snippets.xsl", xslParams);
		System.out.println("Generated snippets for YASnippet into "
				+ w.snippets.getCanonicalPath());
	}

	/**
	 * Parse command line and show help, if required. Otherwise call doWork() to
	 * perform actual business logic.
	 * 
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
				.builder("f") // short name
				.longOpt("force") // long name
				.required(false) //
				.desc("Force overwrite existing files") // description
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

				g_force = line.hasOption('f');
				doWork(line);

			}// if(!line.hasOption('h'))
			else {
				HelpFormatter formatter = new HelpFormatter();
				formatter.setLeftPadding(4);
				String br = formatter.getNewLine();
				char[] alp = new char[formatter.getLeftPadding()];
				Arrays.fill(alp, ' ');
				String lp = new String(alp);
				String header = "Generate code completion metadata for SAP(R) Identity Management"
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

}// CodeCompletion
