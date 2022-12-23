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

import it.osm.gtfs.input.GTFSParser;
import it.osm.gtfs.input.OSMParser;
import it.osm.gtfs.model.*;
import it.osm.gtfs.utils.GTFSImportSettings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import com.google.common.collect.Multimap;
import it.osm.gtfs.utils.StopsUtils;
import org.fusesource.jansi.Ansi;
import org.xml.sax.SAXException;
import picocli.CommandLine;

import javax.xml.parsers.ParserConfigurationException;

import static org.fusesource.jansi.Ansi.ansi;


@CommandLine.Command(name = "gpx", description = "Generate .gpx files for all GTFS trips (mostly for debug purposes)")
public class GTFSGenerateRoutesGPXs implements Callable<Void> {

    @CommandLine.Option(names = {"-w", "--waypoints"}, description = "Export GPXs as waypoints instead of shape/track")
    boolean asWaypoints;

    @Override
    public Void call() throws IOException, ParserConfigurationException, SAXException {
        Map<String, OSMStop> osmstops = StopsUtils.getGTFSIdOSMStopMap(OSMParser.readOSMStops(GTFSImportSettings.OSM_STOP_FILE_PATH));
        Map<String, Route> routes = GTFSParser.readRoutes(GTFSImportSettings.getInstance().getGTFSPath() + GTFSImportSettings.GTFS_ROUTES_FILE_NAME);
        Map<String, Shape> shapes = GTFSParser.readShapes(GTFSImportSettings.getInstance().getGTFSPath() + GTFSImportSettings.GTFS_SHAPES_FILE_NAME);
        Map<String, StopsList> stopTimes = GTFSParser.readStopTimes(GTFSImportSettings.getInstance().getGTFSPath() +  GTFSImportSettings.GTFS_STOP_TIME_FILE_NAME, osmstops);
        List<Trip> trips = GTFSParser.readTrips(GTFSImportSettings.getInstance().getGTFSPath() + GTFSImportSettings.GTFS_TRIPS_FILE_NAME,
                routes, new HashMap<String, StopsList>());

        //sorting set
        Multimap<String, Trip> grouppedTrips = GTFSParser.groupTrip(trips, routes, stopTimes);
        Set<String> keys = new TreeSet<String>(grouppedTrips.keySet());

        new File(GTFSImportSettings.getInstance().getOutputPath() + "gpx").mkdirs();

        int id = 10000;
        for (String k:keys){
            Collection<Trip> allTrips = grouppedTrips.get(k);
            Set<Trip> uniqueTrips = new HashSet<Trip>(allTrips);

            for (Trip trip:uniqueTrips){
                Route route = routes.get(trip.getRoute().getId());
                Shape shape = shapes.get(trip.getShapeID());

                FileOutputStream f = new FileOutputStream(GTFSImportSettings.getInstance().getOutputPath() + "/gpx/r" + id++ + " " + route.getShortName().replace("/", "B") + " " + trip.getName().replace("/", "_") + ".gpx");

                if(asWaypoints){
                    f.write(shape.getGPXasWaypoints(route.getShortName()).getBytes());
                }else{
                    f.write(shape.getGPXasShape(route.getShortName()).getBytes());
                }

                f.close();
            }
        }

        System.out.println(ansi().fg(Ansi.Color.GREEN).a("Done ").reset());
        return null;
    }
}
