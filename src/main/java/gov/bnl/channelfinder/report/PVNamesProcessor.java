package gov.bnl.channelfinder.report;

import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Functionally code which process a set of pv names and returns a report.
 */
public interface PVNamesProcessor extends Callable<String> {

    public void setPVNames(Set<String> pvNames);
}
