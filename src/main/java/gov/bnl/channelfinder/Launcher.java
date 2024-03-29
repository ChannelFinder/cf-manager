package gov.bnl.channelfinder;

import gov.bnl.channelfinder.report.GenerateReport;
import org.springframework.boot.SpringApplication;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

public class Launcher {

    private static final Logger logger = Logger.getLogger(Launcher.class.getName());
    private static Properties defaults = new Properties();
    static {

        String filename = "default.properties";
        try (InputStream input = Launcher.class.getClassLoader().getResourceAsStream(filename);) {
            if (input != null) {
                // load a properties file from class path, inside static method
                defaults.load(input);
            } else {
                logger.warning("Sorry, unable to find " + filename);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static Properties properties = new Properties(defaults);

    public static void main(final String[] original_args) {

        SpringApplication.run(Launcher.class, original_args);

        final List<String> args = new ArrayList<>(Arrays.asList(original_args));

//        final List<String> args = Arrays.asList(original_args);
        args.forEach(System.out::println);
        final Iterator<String> iter = args.iterator();
        try {
            while (iter.hasNext()) {
                final String cmd = iter.next();
                if (cmd.startsWith("-h")) {
                    help();
                    return;
                } else if (cmd.equals("-es_host")) {
                    if (!iter.hasNext())
                        throw new Exception("Missing -es_host hostname");
                    iter.remove();
                    properties.put("es_host", iter.next());
                    iter.remove();
                } else if (cmd.equals("-es_port")) {
                    if (!iter.hasNext())
                        throw new Exception("Missing -es_port port number");
                    iter.remove();
                    properties.put("es_port", iter.next());
                    iter.remove();
                } else if (cmd.equals("-name_query")) {
                    if (!iter.hasNext())
                        throw new Exception("Missing -name_query name query");
                    iter.remove();
                    properties.put("name_query", iter.next());
                    iter.remove();
                } else if (cmd.equals("-time_query")) {
                    if (!iter.hasNext())
                        throw new Exception("Missing -time_query time query");
                    iter.remove();
                    properties.put("time_query", iter.next());
                    iter.remove();
                } else if (cmd.equals("-exclude_inactive")) {
                    properties.put("exclude_inactive", true);
                    iter.remove();
                } else if (cmd.equals("-generate-report")) {
                    // generate report
                    GenerateReport.createReport(properties.getProperty("es_host"),
                            Integer.valueOf(properties.getProperty("es_port")),
                            properties.containsKey("exclude_inactive"),
                            properties.getProperty("name_query", null),
                            properties.getProperty("time_query", null));

                    System.exit(0);
                } else {
                    throw new Exception("Unknown option " + cmd);
                }
            }
        } catch (Exception ex) {
            help();
            System.out.println();
            ex.printStackTrace();
            return;
        }
    }

    private static void help() {
        System.out.println();
        System.out.println("Command-line arguments:");
        System.out.println();
        System.out.println("-generate-report          - Generate a report on the recsync properties");
        System.out.println("-es_host  localhost       - elastic server host");
        System.out.println("-es_port  9200            - elastic server port");
        System.out.println("-exclude_inactive         - exclude inactive pvs");
        System.out.println("-name_query               - include channels with names that match the given pattern");
        System.out.println("-time_query               - include channels with update time matching time pattern");
        System.out.println("-help                     - print this text");
        System.out.println();
    }
}
