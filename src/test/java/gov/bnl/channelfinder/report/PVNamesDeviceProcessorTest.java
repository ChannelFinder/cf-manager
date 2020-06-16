package gov.bnl.channelfinder.report;

import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

public class PVNamesDeviceProcessorTest {

    @Test
    public void checkNoDevice(){
        String pvWithNoDevice = "SR:C001Vac-I";
        String pvWithADevice = "SR:C001{test:device}Vac-I";
        Set<String> pvNames = Set.of(pvWithNoDevice, pvWithADevice);
        PVNamesDeviceProcessor processor = new PVNamesDeviceProcessor();
        processor.setPVNames(pvNames);

        String result = processor.call();
        Assert.assertTrue("Failed to find the correct number of violating pv names. ", result.contains("PV names with no device specified : 1"));
        Assert.assertTrue(result.contains(pvWithNoDevice));
    }

    @Test
    public void checkInvalidDevice(){
        String pvWithNoDevice = "SR:C001Vac-I";
        String pvWithADevice = "SR:C001{test:device}Vac-I";
        Set<String> pvNames = Set.of(pvWithNoDevice, pvWithADevice);
        PVNamesDeviceProcessor processor = new PVNamesDeviceProcessor();
        processor.setPVNames(pvNames);

        String result = processor.call();
        Assert.assertTrue("Failed to find the correct number of violating pv names. ", result.contains("PV names with no device specified : 1"));
        Assert.assertTrue(result.contains(pvWithNoDevice));
    }

}
