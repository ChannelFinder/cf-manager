package gov.bnl.channelfinder.report;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static gov.bnl.channelfinder.report.PVNameSplitter.PRIMARY;

/**
 * Validate the primary system part of the pv name.
 */
public class PVNamesSystemProcessor implements PVNamesProcessor {

    private Set<String> pvNames;

    private static File file = new File(PVNamesSystemProcessor.class.getResource("system_names").getFile());
    private static Set<String> systemNames = new HashSet<>();

    static {
        try (BufferedReader reader = new BufferedReader(new FileReader(file));){
            String line;
            while ((line = reader.readLine()) != null) {
                systemNames.add(line.split("\\s+")[0]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String call() {

        // List of pv's with system names
        Set<String> pvsWithUnknownSystem = new HashSet<>();
        Set<String> unkownSystems = new HashSet<>();

        pvNames.stream().forEach(pvName -> {
            int index = pvName.contains("{") ? pvName.indexOf("{") : pvName.length();
            Optional<String> primary = PVNameSplitter.process(pvName.substring(0, index)).get(PRIMARY);
            if (primary.isEmpty() || !systemNames.contains(primary.get())) {
                primary.ifPresent(unkownSystems::add);
                pvsWithUnknownSystem.add(pvName);
            }
        });


        StringBuilder sb = new StringBuilder();


        sb.append("Unknown system specified : " + unkownSystems.size());
        sb.append(System.lineSeparator());
        sb.append(unkownSystems.stream().collect(Collectors.joining(" ")));
        sb.append(System.lineSeparator());

        sb.append("PV names with an unknown system specified : " + pvsWithUnknownSystem.size());
        sb.append(System.lineSeparator());
        sb.append(pvsWithUnknownSystem.stream().collect(Collectors.joining(" ")));
        sb.append(System.lineSeparator());

        return sb.toString();
    }

    @Override
    public void setPVNames(Set<String> pvNames) {
        this.pvNames = pvNames;
    }
}
