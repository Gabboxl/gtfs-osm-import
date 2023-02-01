package it.osm.gtfs.utils;

import picocli.CommandLine;

@CommandLine.Command()
public class SharedCliOptions {
    //variables in the case of this tool should be static as multiple commands that use a shared variable are called consequently sometimes, and creating a new instance of this class everytime loses the value of these variables

    @CommandLine.Option(names = {"-c", "--checkeverything"}, description = "Check stops with the operator tag value different than what is specified in the properties file")
    public static boolean checkStopsOfAnyOperatorTagValue = false;

    @CommandLine.Option(names = {"--nometro"}, description = "Do not consider subway/metro stops")
    public static boolean skipMetroStops = false;
}
