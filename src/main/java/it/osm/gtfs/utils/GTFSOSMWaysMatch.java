package it.osm.gtfs.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.graphhopper.GraphHopper;
import com.graphhopper.GraphHopperConfig;
import com.graphhopper.ResponsePath;
import com.graphhopper.jackson.Gpx;
import com.graphhopper.matching.EdgeMatch;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.matching.Observation;
import com.graphhopper.routing.ev.OSMWayID;
import com.graphhopper.util.*;
import it.osm.gtfs.command.GTFSMatchGPX;
import org.fusesource.jansi.Ansi;

import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.fusesource.jansi.Ansi.ansi;

public class GTFSOSMWaysMatch {
    private String instructions_locale = "";
    private String profile_graphhopper = "car";
    private StopWatch importSW;
    private StopWatch matchSW;
    private XmlMapper xmlMapper;
    private MapMatching mapMatching;
    private GraphHopper hopper;
    private Translation tr;
    private boolean withRoute;
    private ArrayList<Integer> matchWayIDs = null;

    public ArrayList<Integer> runMatch(@Nullable String xmlGPXString) throws IOException {

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory()); // jackson databind
        GraphHopperConfig graphHopperConfiguration = objectMapper.readValue(GTFSMatchGPX.class.getResourceAsStream("/graphhopper-config.yml"), GraphHopperConfig.class);

        hopper = new GraphHopper().init(graphHopperConfiguration);
        hopper.importOrLoad();
        //hopper.setEncodedValuesString("osm_way_id"); x tag personalizzati senza specificarli in graphopper.yml

        PMap hints = new PMap();
        hints.putObject("profile", profile_graphhopper);
        mapMatching = MapMatching.fromGraphHopper(hopper, hints);
        mapMatching.setTransitionProbabilityBeta(2.0);
        mapMatching.setMeasurementErrorSigma(40);

        importSW = new StopWatch();
        matchSW = new StopWatch();

        tr = new TranslationMap().doImport().getWithFallBack(Helper.getLocale(instructions_locale));
        withRoute = !instructions_locale.isEmpty();
        xmlMapper = new XmlMapper();


        if(xmlGPXString == null || xmlGPXString.isBlank()) {
            File directoryPath = new File(GTFSImportSettings.getInstance().getOutputPath() + "/gpx");
            //List of all files and directories
            File[] gpx_files_list = directoryPath.listFiles();

            assert gpx_files_list != null; //mi assicuro che la cartella dei file gpx sia piena almeno, altrimenti genera un'eccezione
            for (File gpxFile : gpx_files_list) {

                System.out.println(gpxFile);

                String outFile = gpxFile.getAbsolutePath() + ".res.gpx";


                matchGPX(Files.readString(gpxFile.toPath()), outFile);

                System.out.println("\tExport results to:" + outFile);
            }
        } else {
            matchWayIDs = matchGPX(xmlGPXString, null);
        }

        System.out.println(ansi().fg(Ansi.Color.GREEN).a("GPS import took: ").reset().a(importSW.getSeconds() + " s").fg(Ansi.Color.GREEN).a(", match took: ").reset().a(matchSW.getSeconds() + " s"));

        return matchWayIDs;
    }

    private @Nullable ArrayList<Integer> matchGPX(String xmlData, @Nullable String outputFile){
        try {
            importSW.start();
            Gpx gpx = xmlMapper.readValue(xmlData, Gpx.class);
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
            ArrayList<Integer> osmWaysIds = new ArrayList<Integer>();

            var listaEdgeMatches = matchResult.getEdgeMatches();

            for (EdgeMatch edgeMatch : listaEdgeMatches) {
                var edgeState = edgeMatch.getEdgeState();

                try {
                    var osmWayEncodedValue = hopper.getEncodingManager().getIntEncodedValue(OSMWayID.KEY);
                    Integer osmWayID = edgeState.get(osmWayEncodedValue);

                    if (!osmWaysIds.contains(osmWayID)) { //controllo che l'id non sia gia' presente nell'array
                        osmWaysIds.add(osmWayID);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (outputFile == null) {
                return osmWaysIds;
            }



            ResponsePath responsePath = new PathMerger(matchResult.getGraph(), matchResult.getWeighting()).
                    doWork(PointList.EMPTY, Collections.singletonList(matchResult.getMergedPath()), hopper.getEncodingManager(), tr);
            if (responsePath.hasErrors()) {
                System.err.println("Problem with gpx data/file " + xmlData + ", " + responsePath.getErrors());
                return null;
            }


            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
                long time = gpx.trk.get(0).getStartTime()
                        .map(Date::getTime)
                        .orElse(System.currentTimeMillis());
                writer.append(GpxConversions.createGPX(responsePath.getInstructions(), gpx.trk.get(0).name != null ? gpx.trk.get(0).name : "", time, hopper.hasElevation(), withRoute, true, false, Constants.VERSION, tr));
            }


        } catch(Exception ex){
            importSW.stop();
            matchSW.stop();
            System.err.println("Problem with data/file " + xmlData);
            ex.printStackTrace(System.err);
        }

        return null;
    }
}
