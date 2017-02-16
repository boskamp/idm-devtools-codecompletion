package de.foxysoft.idm.devtools;

import java.util.Arrays;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class Tern4IDM {
    private static boolean g_verbose;

    private static final String EXE_NAME = "t4i";

    private static void trc(String msg) {
        if (g_verbose) {
            System.err.println(msg);
        }
    }

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

                //TODO: implement business logic here

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
                    + EXE_NAME +" -u https://tools.hana.ondemand.com/neon" //
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

                formatter.printHelp(EXE_NAME, header, options, footer,
                                    true);
            }// else
        } catch (org.apache.commons.cli.MissingArgumentException mae) {
            System.err.println(mae.getMessage() + "; try "+EXE_NAME+" --help");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }//main
}//Tern4IDM
