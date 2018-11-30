package gov.bnl.channelfinder.migration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.MultivaluedHashMap;

import gov.bnl.channelfinder.XmlChannel;

public class Migrate {

    public static void migrate(String[] args) {
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<String, String>();
        map.put("~name", Arrays.asList("*"));
        map.put("~end", Arrays.asList("10000"));
        try {
            List<XmlChannel> result = new ArrayList<XmlChannel>();
            for (int start = 0; start < 1964846; start = start + 10000) {
                map.put("~start", Arrays.asList(String.valueOf(start)));
                System.out.println(String.valueOf(start));
                result = FindChannelsQuery.findChannelsByMultiMatch(map);
                result.forEach((c) -> {
//                    System.out.println(XmlChannel.toLog(c));
                });
            }

        } catch (CFException e) {
            e.printStackTrace();
        }
    }

}
