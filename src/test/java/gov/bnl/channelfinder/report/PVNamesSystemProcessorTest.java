package gov.bnl.channelfinder.report;

import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

public class PVNamesSystemProcessorTest {

    String pvWithNoSystem = "{IP:1}";
    String pvWithUnknownSystem = "INVALID:C001{IP:1}Vac-I";
    String pvWithKnownSystem = "SR:C001{IP:1}Vac-I";

    @Test
    public void checkInvalidDevice(){
        Set<String> pvNames = Set.of(pvWithNoSystem, pvWithUnknownSystem, pvWithKnownSystem);
        PVNamesSystemProcessor processor = new PVNamesSystemProcessor();
        processor.setPVNames(pvNames);

        String result = processor.call();
        System.out.println(result);
        Assert.assertTrue("Failed to find the correct number of violating pv names. ", result.contains("PV names with an unknown system specified : 2"));
        Assert.assertTrue(result.contains(pvWithUnknownSystem));
        Assert.assertTrue(result.contains(pvWithNoSystem));
    }

}
