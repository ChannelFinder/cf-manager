package gov.bnl.channelfinder.report;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static gov.bnl.channelfinder.report.PVNameSplitter.PRIMARY;

/**
 * Validate the device part of the pv name.
 * Check that the pv name has a device section included.
 * Check that the primary device is part of the approved device_names
 */
public class PVNamesDeviceProcessor implements PVNamesProcessor {

    private Set<String> pvNames;

    private static final Pattern devicePattern = Pattern.compile(".*\\{(.*)\\}.*");

    private static File file = new File(PVNamesDeviceProcessor.class.getResource("device_names").getFile());
    private static Set<String> deviceNames = new HashSet<>();

    static {
        try (BufferedReader reader = new BufferedReader(new FileReader(file));){
            String line;
            while ((line = reader.readLine()) != null) {
                deviceNames.add(line.split("\\s+")[0]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String call() {

        // List of pv's with no device specified.
        Set<String> missingDevices = new HashSet<>();
        // List of pv's with device names
        Set<String> unknownDevices = new HashSet<>();

        pvNames.stream().forEach( pvName -> {
            Matcher matcher = devicePattern.matcher(pvName);
            if(matcher.matches()) {
                Optional<String> primary = PVNameSplitter.process(matcher.group(1)).get(PRIMARY);
                if(primary.isEmpty() || !deviceNames.contains(primary.get())){
                    unknownDevices.add(pvName);
                }
            } else {
                missingDevices.add(pvName);
            }
        });


        StringBuilder sb = new StringBuilder();
        sb.append("PV names with no device specified : " + missingDevices.size());
        sb.append(System.lineSeparator());
        sb.append(missingDevices.stream().collect(Collectors.joining(" ")));
        sb.append(System.lineSeparator());

        sb.append("PV names with an unknown device specified : " + unknownDevices.size());
        sb.append(System.lineSeparator());
        sb.append(unknownDevices.stream().collect(Collectors.joining(" ")));
        sb.append(System.lineSeparator());

        return sb.toString();
    }

    @Override
    public void setPVNames(Set<String> pvNames) {
        this.pvNames = pvNames;
    }
}
