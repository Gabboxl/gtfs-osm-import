/**
 * Licensed under the GNU General Public License version 3
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/gpl-3.0.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package it.osm.gtfs.commands;

import com.google.common.collect.Multimap;
import it.osm.gtfs.input.GTFSParser;
import it.osm.gtfs.input.OSMParser;
import it.osm.gtfs.models.*;
import it.osm.gtfs.output.OSMRelationImportGenerator;
import it.osm.gtfs.utils.*;
import org.apache.commons.httpclient.util.URIUtil;
import org.fusesource.jansi.Ansi;
import org.xml.sax.SAXException;
import picocli.CommandLine;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;

import static org.fusesource.jansi.Ansi.ansi;


@CommandLine.Command(name = "fullrels", mixinStandardHelpOptions = true, description = "Generate full relations including ways and stops (very long!)")
public class CmdGenerateRoutesFullRelations implements Callable<Void> {

    @CommandLine.Option(names = {"-n", "--nowaymatching"}, description = "Generate stops-only relations (skips OSM ways matching)")
    Boolean noOsmWayMatching = false;
    @CommandLine.Option(names = {"-s"}, description = "Skip update of OSM stops and GTFS stops data")
    Boolean skipDataUpdate = false;
    @CommandLine.Option(names = {"-sw"}, description = "Skip download of updated OSM ways")
    Boolean skipWaysUpdate = false;
    @CommandLine.Mixin
    private SharedCliOptions sharedCliOptions;

    @Override
    public Void call() throws IOException, ParserConfigurationException, SAXException, InterruptedException, TransformerException {

        if (!skipDataUpdate) {
            //update osm and gtfs data
            new CmdUpdateGTFSOSMData().call();
        }

        GTFSFeedInfo gtfsFeedInfo = GTFSParser.readFeedInfo(GTFSImportSettings.getInstance().getGTFSDataPath() + GTFSImportSettings.GTFS_FEED_INFO_FILE_NAME);

        Map<String, OSMStop> gtfsIdOsmStopMap = StopsUtils.getGTFSIdOSMStopMap(OSMParser.readOSMStops(GTFSImportSettings.getInstance().getOsmStopsFilePath(), SharedCliOptions.checkStopsOfAnyOperatorTagValue));

        if (gtfsIdOsmStopMap.values().isEmpty()) {

            System.out.println(ansi().render("@|red \n The relations generation will not continue as there are no OSM stops with a GTFS id on OpenStreetMap!" +
                    "\n Please run the \"stops\" command and upload the new stops to OSM first! |@"));

            return null;
        }

        BoundingBox boundingBox = new BoundingBox(gtfsIdOsmStopMap.values());

        Map<String, Route> routes = GTFSParser.readRoutes(GTFSImportSettings.getInstance().getGTFSDataPath() + GTFSImportSettings.GTFS_ROUTES_FILE_NAME);
        Map<String, Shape> shapes = GTFSParser.readShapes(GTFSImportSettings.getInstance().getGTFSDataPath() + GTFSImportSettings.GTFS_SHAPES_FILE_NAME);

        ReadStopTimesResult readStopTimesResult = GTFSParser.readStopTimes(GTFSImportSettings.getInstance().getGTFSDataPath() + GTFSImportSettings.GTFS_STOP_TIMES_FILE_NAME,
                gtfsIdOsmStopMap);

        List<Trip> trips = GTFSParser.readTrips(GTFSImportSettings.getInstance().getGTFSDataPath() + GTFSImportSettings.GTFS_TRIPS_FILE_NAME,
                routes, readStopTimesResult.getTripIdStopListMap());


        //sorting set
        Multimap<Route, Trip> groupedTrips = GTFSParser.groupTrips(routes, trips);

        //this is usually the same as the routes variable, but making a list of actually used routes from the trips
        //is more accurate, as some routes may not have any trips
        Set<Route> finalRoutesSet = new TreeSet<>(groupedTrips.keySet());


        if (!readStopTimesResult.getMissingStops().isEmpty()) {

            System.out.println(ansi().render("@|red The relations generation will not continue as there are some GTFS stops that are missing from OSM. |@"));

            return null;
        }


        if (!skipWaysUpdate) {
            //download of updated OSM ways in the GTFS bounding box
            String queryHighways =  "?data=[bbox];(way[\"highway\"~\"motorway|trunk|primary|tertiary|secondary|unclassified|motorway_link|trunk_link|primary_link|track|path|residential|service|secondary_link|tertiary_link|bus_guideway|road|busway\"];>;);out body;&bbox=" + boundingBox.getAPIQuery();
            File fileOverpassHighways = new File(GTFSImportSettings.getInstance().getOsmOverpassWaysFilePath());

            String urlhighways = GTFSImportSettings.getInstance().getOverpassApiServer() + URIUtil.encodeQuery(queryHighways);
            DownloadUtils.download(urlhighways, fileOverpassHighways, true);
        }

        GTFSOSMWaysMatch osmmatchinstance = new GTFSOSMWaysMatch().initMatch(!skipWaysUpdate);


        //create file paths
        new File(GTFSImportSettings.getInstance().getFullRelsOutputPath()).mkdirs();

        //list of the files to be merged into one
        List<File> relationsFileList = new ArrayList<>();


        int tempid = 10000;

        for (Route route : finalRoutesSet) { //for every route
            Collection<Trip> allTrips = groupedTrips.get(route);
            Set<Trip> uniqueTrips = new HashSet<>(allTrips); //uses the equals method of the Trip class to check if the trips are the same


            List<Integer> newRelationsIds = new ArrayList<>();

            for (Trip trip : uniqueTrips) { //for every trip

                int count = Collections.frequency(allTrips, trip); //number of trips with the same headsign present in the gtfs trips file

                List<Integer> osmWayIds = null;

                if (!noOsmWayMatching) {
                    System.out.println(ansi().fg(Ansi.Color.YELLOW).a("\nCreating full way-matched relation for trip " + trip.getTripHeadsign() + " tripId = " + trip.getTripId() + " ...").reset());

                    Shape shape = shapes.get(trip.getShapeId());

                    String xmlGPXShape = shape.getGPXasSegment(route.getShortName());

                    //TODO: need to check if the way matches are ordered well
                    osmWayIds = osmmatchinstance.matchGPX(xmlGPXShape);

                } else {
                    System.out.println(ansi().fg(Ansi.Color.YELLOW).a("Creating stops-only relation " + trip.getTripHeadsign() + " tripId=" + trip.getTripId() + " ...").reset());
                }

                String fixedTripHeadsignFileName = trip.getTripHeadsign().replace("/", "_").replace(",", "");
                String fixedRouteShortNameFileName = route.getShortName().replace("/", "B");


                File relationOutputFile = new File(GTFSImportSettings.getInstance().getFullRelsOutputPath() + "r" + tempid + " " + fixedRouteShortNameFileName + " " + fixedTripHeadsignFileName + "_" + count + ".osm");

                FileOutputStream fileOutputStream = new FileOutputStream(relationOutputFile);
                OutputStreamWriter out = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);

                out.write(OSMRelationImportGenerator.createSingleTripRelation(boundingBox, osmWayIds, trip, route, gtfsFeedInfo, tempid));
                out.close();


                //we add the file to the merge list
                relationsFileList.add(relationOutputFile);

                //printa il file txt delle fermate con i nomi di esse
                //f = new FileOutputStream(GTFSImportSettings.getInstance().getFullRelsOutputPath() + "r" + tempid + " " + fixedRouteShortNameFileName + " " + fixedTripHeadsignFileName + "_" + count + ".txt");
                //f.write(tripStopsList.getStopsListTextFile().getBytes());
                //f.close();

                newRelationsIds.add(tempid);

                tempid++;
            }



            String fixedRouteShortNameFileName = route.getShortName().replace("/", "B");
            //master relation creation
            File routeMasterOutputFile = new File(GTFSImportSettings.getInstance().getFullRelsOutputPath() + "routemasterfiles/" + fixedRouteShortNameFileName +".osm");
            routeMasterOutputFile.getParentFile().mkdirs(); //we create the required parent folder and not a folder with the filename

            FileOutputStream fileOutputStream = new FileOutputStream(routeMasterOutputFile);
            OutputStreamWriter out = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);

            out.write(OSMRelationImportGenerator.createMasterRouteTripsRelation(route, newRelationsIds, boundingBox, tempid));
            out.close();


            //we add the file to the merge list
            relationsFileList.add(routeMasterOutputFile);

            tempid++;

        }


        //we merge all the files together
        File mergedRelationsFile = new File(GTFSImportSettings.getInstance().getOutputPath() + "gtfs_import_mergedFullRelations.osm");
        OsmosisUtils.checkProcessOutput(OsmosisUtils.runOsmosisMerge(relationsFileList, mergedRelationsFile));


        System.out.println(ansi().fg(Ansi.Color.GREEN).a("\nRelations generation completed!").reset());

        if (!noOsmWayMatching) {
            System.out.println(ansi().fg(Ansi.Color.YELLOW).a("\nBe aware that the IDs of OSM's ways can change anytime!").reset());
            System.out.println(ansi().fg(Ansi.Color.YELLOW).a("This means you can encounter problems if you upload these relations later, as those matched ways can be changed/removed and a new match would be required.").reset());
        }

        return null;
    }
}
