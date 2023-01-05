package it.osm.gtfs.utils;

import picocli.CommandLine;

public class SharedCliOptions {
    @CommandLine.Option(names = {"-c", "--checkeverything"}, description = "Check stops with the operator tag value different than what is specified in the properties file")
    Boolean checkStopsOfAnyOperatorTagValue = false;
}
