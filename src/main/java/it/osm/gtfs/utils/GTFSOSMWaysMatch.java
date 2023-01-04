package it.osm.gtfs.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.graphhopper.GraphHopper;
import com.graphhopper.GraphHopperConfig;
import com.graphhopper.jackson.Gpx;
import com.graphhopper.matching.EdgeMatch;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.matching.Observation;
import com.graphhopper.routing.ev.OSMWayID;
import com.graphhopper.util.*;
import org.fusesource.jansi.Ansi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.fusesource.jansi.Ansi.ansi;

public class GTFSOSMWaysMatch {
    private StopWatch importSW;
    private StopWatch matchSW;
    private XmlMapper xmlMapper;
    private MapMatching mapMatching;
    private GraphHopper hopper;
    private Translation tr;
    private boolean withRoute;

    public ArrayList<Integer> runMatch(String xmlGPXData, boolean deletePreviousCacheData) throws IOException {

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory()); // jackson databind
        GraphHopperConfig graphHopperConfiguration = objectMapper.readValue(GTFSOSMWaysMatch.class.getResourceAsStream("/graphhopper-config.yml"), GraphHopperConfig.class);

        hopper = new GraphHopper().init(graphHopperConfiguration);
        hopper.setGraphHopperLocation(GTFSImportSettings.getInstance().getCachePath() + "graph-cache/");

        if (deletePreviousCacheData) {
            hopper.clean();
        }

        //we programmatically set the OSM ways data file as using the yml doesn't work for custom paths on different machines
        hopper.setOSMFile("C:/Users/Gabriele/Desktop/osm/gtfsimport/osmdata/overpassways.osm");

        //programmatically set additional values to be assigned to every graphhopper's edge for a later use (in our case we need the osm way ids ofr every graphhopper's edge)
        hopper.setEncodedValuesString("osm_way_id"); //ricorda, se vuoi cambiare/aggiungere questi encoded values, devi rigenerare tutti i graph cancellando da cache dei graph in modo che vengano aggiunti i nuovi valori scelti a ogni edge

        hopper.importOrLoad();

        PMap hints = new PMap();
        String profile_graphhopper = "car"; //TODO: maybe remove this as it is already specified in the yml file?
        hints.putObject("profile", profile_graphhopper);
        mapMatching = MapMatching.fromGraphHopper(hopper, hints);
        mapMatching.setTransitionProbabilityBeta(2.0);
        mapMatching.setMeasurementErrorSigma(40);

        importSW = new StopWatch();
        matchSW = new StopWatch();

        String instructions_locale = ""; //TODO: maybe remove this as it is already specified in the yml file? or not
        tr = new TranslationMap().doImport().getWithFallBack(Helper.getLocale(instructions_locale));
        withRoute = !instructions_locale.isEmpty();
        xmlMapper = new XmlMapper();

        //si matchaa
        ArrayList<Integer> matchWayIDs = matchGPX(xmlGPXData);

        System.out.println(ansi().fg(Ansi.Color.GREEN).a("GPS import took: ").reset().a(importSW.getSeconds() + " s").fg(Ansi.Color.GREEN).a(", match took: ").reset().a(matchSW.getSeconds() + " s"));

        return matchWayIDs;
    }

    private ArrayList<Integer> matchGPX(String xmlGpxData){
        try {
            importSW.start();

            Gpx gpx = xmlMapper.readValue(xmlGpxData, Gpx.class);

            if (gpx.trk == null) {
                throw new IllegalArgumentException("No tracks found in GPX data. Are you using waypoints or routes instead?");
            }

            if (gpx.trk.size() > 1) {
                throw new IllegalArgumentException("GPX data with multiple tracks not supported yet.");
            }

            List<Observation> measurements = GpxConversions.getEntries(gpx.trk.get(0));
            importSW.stop();

            matchSW.start();
            MatchResult matchResult = mapMatching.match(measurements);
            matchSW.stop();

            System.out.println("\tMatches:\t" + matchResult.getEdgeMatches().size() + ", GPS entries:" + measurements.size());
            System.out.println("\tGPX length:\t" + (float) matchResult.getGpxEntriesLength() + " vs " + (float) matchResult.getMatchLength());


            //prendo gli id delle vie per ogni edge virtuale creato da graphhopper e li metto in un array
            ArrayList<Integer> osmWaysIds = new ArrayList<>();

            var listaEdgeMatches = matchResult.getEdgeMatches();

            for (EdgeMatch edgeMatch : listaEdgeMatches) {
                var edgeState = edgeMatch.getEdgeState();

                try {
                    //we get the osm way id associated to this Graphhopper's edge, which is an encoded value
                    var osmWayEncodedValue = hopper.getEncodingManager().getIntEncodedValue(OSMWayID.KEY);
                    Integer osmWayID = edgeState.get(osmWayEncodedValue);

                    if (!osmWaysIds.contains(osmWayID)) { //controllo che l'id non sia gia' presente nell'array
                        osmWaysIds.add(osmWayID);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return osmWaysIds;


            /*
            ResponsePath responsePath = new PathMerger(matchResult.getGraph(), matchResult.getWeighting()).
                    doWork(PointList.EMPTY, Collections.singletonList(matchResult.getMergedPath()), hopper.getEncodingManager(), tr);
            if (responsePath.hasErrors()) {
                System.err.println("Problem with gpx data/file " + xmlGpxData + ", " + responsePath.getErrors());
                return null;
            }


            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
                long time = gpx.trk.get(0).getStartTime()
                        .map(Date::getTime)
                        .orElse(System.currentTimeMillis());
                writer.append(GpxConversions.createGPX(responsePath.getInstructions(), gpx.trk.get(0).name != null ? gpx.trk.get(0).name : "", time, hopper.hasElevation(), withRoute, true, false, Constants.VERSION, tr));
            } */


        } catch (Exception e) {
            importSW.stop();
            matchSW.stop();
            System.err.println("Problem with data " + xmlGpxData);

            e.printStackTrace(System.err);
        }

        return null;
    }
}
