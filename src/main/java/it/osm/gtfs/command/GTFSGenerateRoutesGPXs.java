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
import it.osm.gtfs.model.Route;
import it.osm.gtfs.model.Shape;
import it.osm.gtfs.model.Stop;
import it.osm.gtfs.model.StopsList;
import it.osm.gtfs.model.Trip;
import it.osm.gtfs.utils.GTFSImportSetting;

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

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.google.common.collect.Multimap;

public class GTFSGenerateRoutesGPXs {
	public static void run() throws IOException, ParserConfigurationException, SAXException {
		Map<String, Stop> osmstops = OSMParser.applyGTFSIndex(OSMParser.readOSMStops(GTFSImportSetting.getInstance().getOSMPath() +  GTFSImportSetting.OSM_STOP_FILE_NAME));
		Map<String, Route> routes = GTFSParser.readRoutes(GTFSImportSetting.getInstance().getGTFSPath() + GTFSImportSetting.GTFS_ROUTES_FILE_NAME);
		Map<String, Shape> shapes = GTFSParser.readShapes(GTFSImportSetting.getInstance().getGTFSPath() + GTFSImportSetting.GTFS_SHAPES_FILE_NAME);
		Map<String, StopsList> stopTimes = GTFSParser.readStopTimes(GTFSImportSetting.getInstance().getGTFSPath() +  GTFSImportSetting.GTFS_STOP_TIME_FILE_NAME, osmstops);
		List<Trip> trips = GTFSParser.readTrips(GTFSImportSetting.getInstance().getGTFSPath() + GTFSImportSetting.GTFS_TRIPS_FILE_NAME,
				routes, new HashMap<String, StopsList>());
		
		//sorting set
		Multimap<String, Trip> grouppedTrips = GTFSParser.groupTrip(trips, routes, stopTimes);
		Set<String> keys = new TreeSet<String>(grouppedTrips.keySet());

		new File(GTFSImportSetting.getInstance().getOutputPath() + "gpx").mkdirs();
		
		int id = 10000;
		for (String k:keys){
			Collection<Trip> allTrips = grouppedTrips.get(k);
			Set<Trip> uniqueTrips = new HashSet<Trip>(allTrips);

			for (Trip trip:uniqueTrips){
				Route r = routes.get(trip.getRoute().getId());
				Shape s = shapes.get(trip.getShapeID());

				FileOutputStream f = new FileOutputStream(GTFSImportSetting.getInstance().getOutputPath() + "/gpx/r" + id++ + " " + r.getShortName().replace("/", "B") + " " + trip.getName().replace("/", "_") + ".gpx");
				f.write(s.getGPX(r.getShortName()).getBytes());
				f.close();
			}
		}
	}
}
