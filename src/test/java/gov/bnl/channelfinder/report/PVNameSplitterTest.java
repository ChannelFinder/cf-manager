package gov.bnl.channelfinder.report;

import org.junit.Test;

import java.util.Map;
import java.util.Optional;

import static gov.bnl.channelfinder.report.PVNameSplitter.*;
import static org.junit.Assert.assertTrue;

public class PVNameSplitterTest {

    String priSecTer = "PSy:PI-SSy:SI-TSy:TI";
    String priSecTer_noInstance = "PSy-SSy-TSy";
    String priSec = "PSy:PI-SSy:SI";
    String priSec_noInstance = "PSy-SSy";
    String pri = "PSy:PI";

    /**
     * Test parsing of "PSy:PI-SSy:SI-TSy:TI"
     */
    @Test
    public void testCompleteName(){
        String name = priSecTer;
        Map<String, Optional<String>> result = PVNameSplitter.process(name);
        assertTrue("Failed to find primary system  for : " + name, result.get(PRIMARY).get().equals("PSy"));
        assertTrue("Failed to find primary system instance ", result.get(PRIMARY_INST).get().equals("PI"));
        assertTrue("Failed to find secondary system  for : " + name, result.get(SECONDARY).get().equals("SSy"));
        assertTrue("Failed to find secondary system instance ", result.get(SECONDARY_INST).get().equals("SI"));
        assertTrue("Failed to find tertiary system  for : " + name, result.get(TERTIARY).get().equals("TSy"));
        assertTrue("Failed to find tertiary system instance ", result.get(TERTIARY_INST).get().equals("TI"));
    }

    /**
     * Test parsing of "PSy-SSy-TSy"
     */
    @Test
    public void testCompleteNameNoInstances(){
        String name = priSecTer_noInstance;
        Map<String, Optional<String>> result = PVNameSplitter.process(name);
        assertTrue("Failed to find primary system  for : " + name, result.get(PRIMARY).get().equals("PSy"));
        assertTrue("Failed to find primary system instance ", result.get(PRIMARY_INST).isEmpty());
        assertTrue("Failed to find secondary system  for : " + name, result.get(SECONDARY).get().equals("SSy"));
        assertTrue("Failed to find secondary system instance ", result.get(SECONDARY_INST).isEmpty());
        assertTrue("Failed to find tertiary system  for : " + name, result.get(TERTIARY).get().equals("TSy"));
        assertTrue("Failed to find tertiary system instance ", result.get(TERTIARY_INST).isEmpty());
    }

    /**
     * Test parsing of "PSy:PI-SSy:SI"
     */
    @Test
    public void testPrimarySecondaryName(){
        String name = priSec;
        Map<String, Optional<String>> result = PVNameSplitter.process(name);
        assertTrue("Failed to find primary system  for : " + name, result.get(PRIMARY).get().equals("PSy"));
        assertTrue("Failed to find primary system instance ", result.get(PRIMARY_INST).get().equals("PI"));
        assertTrue("Failed to find secondary system  for : " + name, result.get(SECONDARY).get().equals("SSy"));
        assertTrue("Failed to find secondary system instance ", result.get(SECONDARY_INST).get().equals("SI"));
        assertTrue("Failed to find tertiary system  for : " + name, result.get(TERTIARY).isEmpty());
        assertTrue("Failed to find tertiary system instance ", result.get(TERTIARY_INST).isEmpty());
    }

    /**
     * Test parsing of "PSy-SSy"
     */
    @Test
    public void testPrimarySecondaryNoInstantName(){
        String name = priSec_noInstance;
        Map<String, Optional<String>> result = PVNameSplitter.process(name);
        assertTrue("Failed to find primary system  for : " + name, result.get(PRIMARY).get().equals("PSy"));
        assertTrue("Failed to find primary system instance ", result.get(PRIMARY_INST).isEmpty());
        assertTrue("Failed to find secondary system  for : " + name, result.get(SECONDARY).get().equals("SSy"));
        assertTrue("Failed to find secondary system instance ", result.get(SECONDARY_INST).isEmpty());
        assertTrue("Failed to find tertiary system  for : " + name, result.get(TERTIARY).isEmpty());
        assertTrue("Failed to find tertiary system instance ", result.get(TERTIARY_INST).isEmpty());
    }


    /**
     * Test parsing of "PSy:PI"
     */
    @Test
    public void testPrimaryName(){
        String name = pri;
        Map<String, Optional<String>> result = PVNameSplitter.process(name);
        assertTrue("Failed to find primary system  for : " + name, result.get(PRIMARY).get().equals("PSy"));
        assertTrue("Failed to find primary system instance ", result.get(PRIMARY_INST).get().equals("PI"));
        assertTrue("Failed to find secondary system  for : " + name, result.get(SECONDARY).isEmpty());
        assertTrue("Failed to find secondary system instance ", result.get(SECONDARY_INST).isEmpty());
        assertTrue("Failed to find tertiary system  for : " + name, result.get(TERTIARY).isEmpty());
        assertTrue("Failed to find tertiary system instance ", result.get(TERTIARY_INST).isEmpty());
    }

}
