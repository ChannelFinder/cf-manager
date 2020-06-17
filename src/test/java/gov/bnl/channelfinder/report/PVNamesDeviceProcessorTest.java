package gov.bnl.channelfinder.report;

import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

public class PVNamesDeviceProcessorTest {

    String pvWithNoDevice = "SR:C001Vac-I";
    String pvWithUnknownDevice = "SR:C001{test:device}Vac-I";
    String pvWithKnownDevice = "SR:C001{IP:2}Vac-I";
    @Test
    public void checkNoDevice(){
        Set<String> pvNames = Set.of(pvWithNoDevice, pvWithUnknownDevice, pvWithKnownDevice);
        PVNamesDeviceProcessor processor = new PVNamesDeviceProcessor();
        processor.setPVNames(pvNames);

        String result = processor.call();
        Assert.assertTrue("Failed to find the correct number of violating pv names. ", result.contains("PV names with no device specified : 1"));
        Assert.assertTrue(result.contains(pvWithNoDevice));
    }

    @Test
    public void checkInvalidDevice(){
        Set<String> pvNames = Set.of(pvWithNoDevice, pvWithUnknownDevice, pvWithKnownDevice);
        PVNamesDeviceProcessor processor = new PVNamesDeviceProcessor();
        processor.setPVNames(pvNames);

        String result = processor.call();
        Assert.assertTrue("Failed to find the correct number of violating pv names. ", result.contains("PV names with an unknown device specified : 1"));
        Assert.assertTrue(result.contains(pvWithNoDevice));
    }

}
