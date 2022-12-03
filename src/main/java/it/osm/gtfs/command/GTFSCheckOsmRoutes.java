package it.osm.gtfs.command;

import it.osm.gtfs.input.OSMParser;
import it.osm.gtfs.model.Relation;
import it.osm.gtfs.model.Relation.OSMNode;
import it.osm.gtfs.model.Relation.OSMRelationWayMember;
import it.osm.gtfs.model.Stop;
import it.osm.gtfs.utils.GTFSImportSettings;
import it.osm.gtfs.utils.JOSMUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.fusesource.jansi.Ansi;
import org.xml.sax.SAXException;
import picocli.CommandLine;

import javax.xml.parsers.ParserConfigurationException;

import static org.fusesource.jansi.Ansi.ansi;

@CommandLine.Command(name = "check", description = "Check and validate OSM relations")
public class GTFSCheckOsmRoutes implements Callable<Void> {

    @CommandLine.Option(names = "--osmid", interactive = true)
    String osmId;

    @Override
    public Void call() throws ParserConfigurationException, IOException, SAXException {
        System.out.println(ansi().fg(Ansi.Color.YELLOW).a("Warning: this command is still in alpha stage and check only some aspects of the relations.").reset());
        System.out.println("Step 1/4 Reading OSM Stops");
        List<Stop> osmStops = OSMParser.readOSMStops(GTFSImportSettings.getInstance().getOSMPath() +  GTFSImportSettings.OSM_STOP_FILE_NAME);
        System.out.println("Step 2/4 Indexing OSM Stops");
        Map<String, Stop> osmstopsOsmID = OSMParser.applyOSMIndex(osmStops);
        System.out.println("Step 3/4 Reading OSM Relations");
        List<Relation> osmRels = OSMParser.readOSMRelations(new File(GTFSImportSettings.getInstance().getOSMPath() +  GTFSImportSettings.OSM_RELATIONS_FILE_NAME), osmstopsOsmID);

        System.out.println("Step 4/4 Checking relations");
        for (Relation r:osmRels){
            try{
                if (osmId == null || osmId.length() == 0)
                    check(r, false);
                else if (osmId.equals(r.getId()))
                    check(r, true);
            }catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        return null;
    }

    private static void check(Relation relation, Boolean debug) {
        if (relation.getStops().size() <= 1)
            throw new IllegalArgumentException("Relation " + relation.getId() + " has less than 2 stop.");

        //FIXME: handling first segment without forward/backward as backward
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

        if (debug){
            for (long f = 1; f <= relation.getStops().size() ; f++){
                Stop osm = relation.getStops().get(f);
                System.out.println("Stop # " + f + "\t" + osm.getCode() + "\t" + osm.getOSMId() + "\t" + osm.getName());
            }
        }
    }
}
