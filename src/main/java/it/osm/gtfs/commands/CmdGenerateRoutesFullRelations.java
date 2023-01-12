/**
 Licensed under the GNU General Public License version 3
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.gnu.org/licenses/gpl-3.0.html

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 **/
package it.osm.gtfs.commands;

import com.google.common.collect.Multimap;
import it.osm.gtfs.input.GTFSParser;
import it.osm.gtfs.input.OSMParser;
import it.osm.gtfs.models.*;
import it.osm.gtfs.output.OSMRelationImportGenerator;
import it.osm.gtfs.utils.*;
import org.fusesource.jansi.Ansi;
import org.xml.sax.SAXException;
import picocli.CommandLine;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;

import static org.fusesource.jansi.Ansi.ansi;


@CommandLine.Command(name = "fullrels", mixinStandardHelpOptions = true, description = "Generate full relations including ways and stops (very long!)")
public class CmdGenerateRoutesFullRelations implements Callable<Void> {

    @CommandLine.Mixin
    private SharedCliOptions sharedCliOptions;

    @CommandLine.Option(names = {"-n", "--nowaymatching"}, description = "Generate stops-only relations (skips OSM ways matching)")
    Boolean noOsmWayMatching = false;

    @CommandLine.Option(names = {"-s", "--skipupdate"}, description = "Skip download of updated OSM ways, OSM data and GTFS data")
    Boolean skipDataUpdate = false;


    @Override
    public Void call() throws IOException, ParserConfigurationException, SAXException, InterruptedException {

        Map<String, OSMStop> gtfsIdOsmStopMap = StopsUtils.getGTFSIdOSMStopMap(OSMParser.readOSMStops(GTFSImportSettings.OSM_STOPS_FILE_PATH, SharedCliOptions.checkStopsOfAnyOperatorTagValue));
        BoundingBox bb = new BoundingBox(gtfsIdOsmStopMap.values());


        Map<String, Route> routes = GTFSParser.readRoutes(GTFSImportSettings.getInstance().getGTFSDataPath() +  GTFSImportSettings.GTFS_ROUTES_FILE_NAME);
        Map<String, Shape> shapes = GTFSParser.readShapes(GTFSImportSettings.getInstance().getGTFSDataPath() + GTFSImportSettings.GTFS_SHAPES_FILE_NAME);

        Map<String, TripStopsList> stopTimes = GTFSParser.readStopTimes(GTFSImportSettings.getInstance().getGTFSDataPath() +  GTFSImportSettings.GTFS_STOP_TIMES_FILE_NAME,
                gtfsIdOsmStopMap);

        List<Trip> trips = GTFSParser.readTrips(GTFSImportSettings.getInstance().getGTFSDataPath() +  GTFSImportSettings.GTFS_TRIPS_FILE_NAME,
                routes, stopTimes);

        //sorting set
        Multimap<String, Trip> groupedTrips = GTFSParser.groupTrip(trips, routes, stopTimes);
        Set<String> keys = new TreeSet<>(groupedTrips.keySet());





        //download of updated OSM ways in the GTFS bounding box
        if(!skipDataUpdate) {
            //update osm and gtfs data
            new CmdUpdateGTFSOSMData().call();

            String urlhighways = GTFSImportSettings.OSM_OVERPASS_API_SERVER + "data=[bbox];(way[\"highway\"~\"motorway|trunk|primary|tertiary|secondary|unclassified|motorway_link|trunk_link|primary_link|track|path|residential|service|secondary_link|tertiary_link|bus_guideway|road|busway\"];>;);out body;&bbox=" + bb.getAPIQuery();
            File fileOverpassHighways = new File(GTFSImportSettings.OSM_OVERPASS_WAYS_FILE_PATH);
            urlhighways = urlhighways.replace(" ", "%20"); //we substitute spaced with the uri code as httpurlconnection doesn't do that automatically, and it makes the request fail
            DownloadUtils.download(urlhighways, fileOverpassHighways, true);
        }

        GTFSOSMWaysMatch osmmatchinstance = new GTFSOSMWaysMatch().initMatch(!skipDataUpdate);


        new File(GTFSImportSettings.getInstance().getOutputPath() + "fullrelations").mkdirs();

        int id = 10000;
        for (String k:keys){
            Collection<Trip> allTrips = groupedTrips.get(k);
            Set<Trip> uniqueTrips = new HashSet<>(allTrips);

            for (Trip trip : uniqueTrips){

                int count = Collections.frequency(allTrips, trip);

                Route route = routes.get(trip.getRoute().getId());
                TripStopsList stops = stopTimes.get(trip.getTripId());
                List<Integer> osmWayIds = null;

                if(!noOsmWayMatching) {
                    System.out.println(ansi().fg(Ansi.Color.YELLOW).a("\nCreating full way-matched relation for trip " + trip.getName() + " tripID=" + trip.getTripId() +  " ...").reset());

                    Shape shape = shapes.get(trip.getShapeId());

                    String xmlGPXShape = shape.getGPXasSegment(route.getShortName());

                    //TODO: need to check if the way matches are ordered well
                    osmWayIds = osmmatchinstance.matchGPX(xmlGPXShape);
                }else {
                    System.out.println(ansi().fg(Ansi.Color.YELLOW).a("Creating stops-only relation " + trip.getName() + " tripID=" + trip.getTripId() +  " ...").reset());
                }

                FileOutputStream f = new FileOutputStream(GTFSImportSettings.getInstance().getOutputPath() + "fullrelations/r" + id + " " + route.getShortName().replace("/", "B") + " " + trip.getName().replace("/", "_") + "_" + count + ".osm");
                f.write(OSMRelationImportGenerator.getRelation(bb, stops, osmWayIds, trip, route).getBytes());
                f.close();
                f = new FileOutputStream(GTFSImportSettings.getInstance().getOutputPath() + "fullrelations/r" + id++ + " " + route.getShortName().replace("/", "B") + " " + trip.getName().replace("/", "_") + "_" + count + ".txt");
                f.write(stops.getRelationAsStopList(trip, route).getBytes());
                f.close();
            }

        }

        System.out.println(ansi().fg(Ansi.Color.GREEN).a("\nRelations generation completed!").reset());

        if(!noOsmWayMatching) {
            System.out.println(ansi().fg(Ansi.Color.YELLOW).a("\nBe aware that the IDs of OSM's ways can change anytime!").reset());
            System.out.println(ansi().fg(Ansi.Color.YELLOW).a("This means you can encounter problems if you upload these relations to OSM later as matched OSM ways could be changed/removed and a new match would be required.").reset());
        }

        return null;
    }
}
