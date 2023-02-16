package it.osm.gtfs.utils;

import picocli.CommandLine;

public class VersionProvider implements CommandLine.IVersionProvider {
    @Override
    public String[] getVersion() {
        return new String[]{getClass().getPackage().getImplementationVersion()};
    }
}
