package it.osm.gtfs.utils;

import picocli.CommandLine;

@CommandLine.Command(synopsisHeading      = "%nUsage: ",
        descriptionHeading   = "Description: ",
        parameterListHeading = "%nParameters:%n",
        optionListHeading    = "%nOptions:%n",
        commandListHeading   = "%nCommands:%n")
public class SharedCliOptions {
    @CommandLine.Option(names = {"-c", "--checkeverything"}, description = "Check stops with the operator tag value different than what is specified in the properties file")
    Boolean checkStopsOfAnyOperatorTagValue = false;
}
