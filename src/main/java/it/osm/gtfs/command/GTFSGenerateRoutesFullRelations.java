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
package it.osm.gtfs.command;

import com.google.common.collect.Multimap;
import it.osm.gtfs.input.GTFSParser;
import it.osm.gtfs.input.OSMParser;
import it.osm.gtfs.model.*;
import it.osm.gtfs.output.OSMRelationImportGenerator;
import it.osm.gtfs.utils.GTFSImportSettings;
import org.xml.sax.SAXException;
import picocli.CommandLine;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;


@CommandLine.Command(name = "fullrels", description = "Generate full releations including ways and stops")
public class GTFSGenerateRoutesFullRelations implements Callable<Void> {

    @Override
    public Void call() throws IOException, ParserConfigurationException, SAXException {
        Map<String, Stop> osmstops = OSMParser.applyGTFSIndex(OSMParser.readOSMStops(GTFSImportSettings.getInstance().getOSMPath() +  GTFSImportSettings.OSM_STOP_FILE_NAME));
        Map<String, Route> routes = GTFSParser.readRoutes(GTFSImportSettings.getInstance().getGTFSPath() +  GTFSImportSettings.GTFS_ROUTES_FILE_NAME);
        Map<String, Shape> shapes = GTFSParser.readShapes(GTFSImportSettings.getInstance().getGTFSPath() + GTFSImportSettings.GTFS_SHAPES_FILE_NAME);
        Map<String, StopsList> stopTimes = GTFSParser.readStopTimes(GTFSImportSettings.getInstance().getGTFSPath() +  GTFSImportSettings.GTFS_STOP_TIME_FILE_NAME, osmstops);
        List<Trip> trips = GTFSParser.readTrips(GTFSImportSettings.getInstance().getGTFSPath() +  GTFSImportSettings.GTFS_TRIPS_FILE_NAME,
                routes, stopTimes);
        BoundingBox bb = new BoundingBox(osmstops.values());

        //sorting set
        Multimap<String, Trip> grouppedTrips = GTFSParser.groupTrip(trips, routes, stopTimes);
        Set<String> keys = new TreeSet<String>(grouppedTrips.keySet());

        new File(GTFSImportSettings.getInstance().getOutputPath() + "fullrelations").mkdirs();

        int id = 10000;
        for (String k:keys){
            Collection<Trip> allTrips = grouppedTrips.get(k);
            Set<Trip> uniqueTrips = new HashSet<Trip>(allTrips);

            for (Trip trip:uniqueTrips){
                int count = Collections.frequency(allTrips, trip);

                Route route = routes.get(trip.getRoute().getId());
                StopsList stops = stopTimes.get(trip.getTripID());
                Shape shape = shapes.get(trip.getShapeID());

                String xmlGPXShape = shape.getGPXasShape(route.getShortName());


                List<Integer> osmWayIds = new GTFSMatchGPX().runMatch(xmlGPXShape);

                FileOutputStream f = new FileOutputStream(GTFSImportSettings.getInstance().getOutputPath() + "fullrelations/r" + id + " " + route.getShortName().replace("/", "B") + " " + trip.getName().replace("/", "_") + "_" + count + ".osm");
                f.write(OSMRelationImportGenerator.getRelation(bb, stops, osmWayIds, trip, route).getBytes());
                f.close();
                f = new FileOutputStream(GTFSImportSettings.getInstance().getOutputPath() + "fullrelations/r" + id++ + " " + route.getShortName().replace("/", "B") + " " + trip.getName().replace("/", "_") + "_" + count + ".txt");
                f.write(stops.getRelationAsStopList(trip, route).getBytes());
                f.close();
            }
        }
        return null;
    }
}
