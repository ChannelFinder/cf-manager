package gov.bnl.channelfinder.report;

import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

public class PVNamesLengthProcessorTest {

    @Test
    public void checkLength(){
        String pvName_61 = "A_long_pv_name_consisting_more_than_sixty_characters_allowed_";
        String pvName_60 = "A_long_pv_name_consisting_just_the_sixty_characters_allowed_";
        Set<String> longNamedpvs = Set.of(pvName_60, pvName_61);
        PVNamesLengthProcessor processor = new PVNamesLengthProcessor();
        processor.setPVNames(longNamedpvs);

        String result = processor.call();
        Assert.assertTrue("Failed to find the correct number of violating pv names. ", result.contains("PV names violating the 60 char limit: 1"));
        Assert.assertTrue(result.contains(pvName_61));
        Assert.assertFalse(result.contains(pvName_60));
    }

}
