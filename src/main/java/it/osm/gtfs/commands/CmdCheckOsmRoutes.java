package it.osm.gtfs.commands;

import it.osm.gtfs.input.OSMParser;
import it.osm.gtfs.models.OSMStop;
import it.osm.gtfs.models.ReadOSMRelationsResult;
import it.osm.gtfs.models.Relation;
import it.osm.gtfs.utils.GTFSImportSettings;
import it.osm.gtfs.utils.SharedCliOptions;
import it.osm.gtfs.utils.StopsUtils;
import org.fusesource.jansi.Ansi;
import org.xml.sax.SAXException;
import picocli.CommandLine;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.fusesource.jansi.Ansi.ansi;

@CommandLine.Command(name = "check", description = "Check and validate OSM relations")
public class CmdCheckOsmRoutes implements Callable<Void> {

    @CommandLine.Option(names = "--osmid", interactive = true)
    String osmId;
    @CommandLine.Mixin
    private SharedCliOptions sharedCliOptions;

    private static void check(Relation relation, Boolean debug) {
        if (relation.getStops().size() <= 1)
            throw new IllegalArgumentException("Relation " + relation.getId() + " has less than 2 stop.");

        /*
        OSMNode previous = null;
        for (OSMRelationWayMember m:relation.getWayMembers()){
            if (previous != null){
                if (m.backward == null){
                    if(previous.equals(m.way.nodes.get(m.way.nodes.size() - 1))){
                        previous = m.way.nodes.get(0);
                    }else if (previous.equals(m.way.nodes.get(0))){
                        previous = m.way.nodes.get(m.way.nodes.size() - 1);
                    }else{
                        throw new IllegalArgumentException("Relation " + relation.getId() + " has a gap. (Current way: " + m.way.getId() + ") " + JOSMUtils.getJOSMRemoteControlRelationLink(relation.getId()));
                    }
                }else if(!previous.equals(m.way.nodes.get((m.backward) ? m.way.nodes.size() - 1 : 0))){
                    throw new IllegalArgumentException("Relation " + relation.getId() + " has a gap. (Current way: " + m.way.getId() + ") " + JOSMUtils.getJOSMRemoteControlRelationLink(relation.getId()));
                }else{
                    previous = m.way.nodes.get((m.backward == null || !m.backward) ? m.way.nodes.size() - 1 : 0);
                }
            }else{
                previous = m.way.nodes.get((m.backward == null || !m.backward) ? m.way.nodes.size() - 1 : 0);
            }
        }
        */

        if (debug) {
            for (long f = 1; f <= relation.getStops().size(); f++) {
                OSMStop osmStop = relation.getStops().get(f); //not ideal casting here, need to review the StopsList class types
                System.out.println("Stop # " + f + "\t" + osmStop.getCode() + "\t" + osmStop.getOSMId() + "\t" + osmStop.getName());
            }
        }
    }

    @Override
    public Void call() throws ParserConfigurationException, IOException, SAXException {
        System.out.println(ansi().fg(Ansi.Color.YELLOW).a("Warning: this command is still in alpha stage and check only some aspects of the relations.").reset());
        System.out.println("Step 1/4 Reading OSM Stops");
        List<OSMStop> osmStops = OSMParser.readOSMStops(GTFSImportSettings.getInstance().getOsmStopsFilePath(), SharedCliOptions.checkStopsOfAnyOperatorTagValue);
        System.out.println("Step 2/4 Indexing OSM Stops");
        Map<String, OSMStop> osmstopsOsmID = StopsUtils.getOSMIdOSMStopMap(osmStops);
        System.out.println("Step 3/4 Reading OSM Relations");
        ReadOSMRelationsResult osmRels = OSMParser.readOSMRelations(new File(GTFSImportSettings.getInstance().getOsmRelationsFilePath()), osmstopsOsmID, SharedCliOptions.checkStopsOfAnyOperatorTagValue);

        System.out.println("Step 4/4 Checking relations");
        for (Relation validRelation : osmRels.getFinalValidRelations()) {
            try {
                if (osmId == null || osmId.length() == 0)
                    check(validRelation, false);
                else if (osmId.equals(validRelation.getId()))
                    check(validRelation, true);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        return null;
    }
}
