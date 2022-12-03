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
import it.osm.gtfs.utils.GTFSImportSetting;
import it.osm.gtfs.model.Relation;
import it.osm.gtfs.model.Route;
import it.osm.gtfs.model.Stop;
import it.osm.gtfs.model.StopsList;
import it.osm.gtfs.model.Trip;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.google.common.collect.Multimap;
import picocli.CommandLine;

@CommandLine.Command(name = "reldiff", description = "Analyze the diff between osm relations and gtfs trips")
public class GTFSGenerateRoutesDiff implements Callable<Void> {

	@Override
	public Void call() throws ParserConfigurationException, IOException, SAXException {
		List<Stop> osmStops = OSMParser.readOSMStops(GTFSImportSetting.getInstance().getOSMPath() +  GTFSImportSetting.OSM_STOP_FILE_NAME);
		Map<String, Stop> osmstopsGTFSId = OSMParser.applyGTFSIndex(osmStops);
		Map<String, Stop> osmstopsOsmID = OSMParser.applyOSMIndex(osmStops);
		List<Relation> osmRels = OSMParser.readOSMRelations(new File(GTFSImportSetting.getInstance().getOSMPath() +  GTFSImportSetting.OSM_RELATIONS_FILE_NAME), osmstopsOsmID);

		Map<String, Route> routes = GTFSParser.readRoutes(GTFSImportSetting.getInstance().getGTFSPath() +  GTFSImportSetting.GTFS_ROUTES_FILE_NAME);
		Map<String, StopsList> stopTimes = GTFSParser.readStopTimes(GTFSImportSetting.getInstance().getGTFSPath() +  GTFSImportSetting.GTFS_STOP_TIME_FILE_NAME, osmstopsGTFSId);
		List<Trip> trips = GTFSParser.readTrips(GTFSImportSetting.getInstance().getGTFSPath() +  GTFSImportSetting.GTFS_TRIPS_FILE_NAME,
				routes, stopTimes);

		//looking from mapping gtfs trip into existing osm relations
		Set<Relation> osmRelationNotFoundInGTFS = new HashSet<Relation>(osmRels);
		Set<Relation> osmRelationFoundInGTFS = new HashSet<Relation>();
		List<Trip> tripsNotFoundInOSM = new LinkedList<Trip>();

		Multimap<String, Trip> grouppedTrips = GTFSParser.groupTrip(trips, routes, stopTimes);
		Set<String> keys = new TreeSet<String>(grouppedTrips.keySet());
		Map<Relation, Affinity> affinities = new HashMap<Relation, GTFSGenerateRoutesDiff.Affinity>();

		for (String k:keys){
			Collection<Trip> allTrips = grouppedTrips.get(k);
			Set<Trip> uniqueTrips = new HashSet<Trip>(allTrips);

			for (Trip trip:uniqueTrips){
				Route route = routes.get(trip.getRoute().getId());
				StopsList s = stopTimes.get(trip.getTripID());
				if (GTFSImportSetting.getInstance().getPlugin().isValidTrip(allTrips, uniqueTrips, trip, s)){
					if (GTFSImportSetting.getInstance().getPlugin().isValidRoute(route)){
						Relation found = null;
						for (Relation relation: osmRels){
							if (relation.equalsStops(s) || GTFSImportSetting.getInstance().getPlugin().isRelationSameAs(relation, s)){
								if (found != null){
									osmRelationNotFoundInGTFS.remove(found);
									osmRelationFoundInGTFS.add(found);
								}
								found = relation;
							}
							int affinity = relation.getStopsAffinity(s);
							Affinity oldAff = affinities.get(relation);
							if (oldAff == null){
								oldAff = new Affinity();
								oldAff.trip = trip;
								oldAff.affinity = affinity;
								affinities.put(relation, oldAff);
							}else if (oldAff.affinity < affinity){
								oldAff.trip = trip;
								oldAff.affinity = affinity;
							}
						}
						if (found != null){
							osmRelationNotFoundInGTFS.remove(found);
							osmRelationFoundInGTFS.add(found);
						}else{
							tripsNotFoundInOSM.add(trip);
							System.err.println("Warning: tripid: " + trip.getTripID() + " (" + trip.getName() + ") not found in OSM, details below." );
							System.err.println("Details: shapeid: " + trip.getShapeID() + " shortname: " + route.getShortName() + " longname:" + route.getLongName());
						}
					}else{
						System.err.println("Warning: tripid: " + trip.getTripID() + " skipped (invalidated route by plugin)." );
					}
				}else{
					System.err.println("Warning: tripid: " + trip.getTripID() + " skipped (invalidated trip by plugin)." );
				}
			}
		}

		System.out.println("---");
		for (Relation r:osmRelationFoundInGTFS){
			System.out.println("Relation " + r.getId() + " (" + r.getName() + ") matched in GTFS ");
		}
		System.out.println("---");
		for (Trip t:tripsNotFoundInOSM){
			System.out.println("Trip " + t.getTripID() + " (" + routes.get(t.getRoute().getId()).getShortName() + " - " + t.getName() + ") not found in OSM ");
			StopsList stopGTFS = stopTimes.get(t.getTripID());
			System.out.println("Progressivo \tGTFS\tOSM");
			for (long f = 1; f <= stopGTFS.getStops().size() ; f++){
				Stop gtfs= stopGTFS.getStops().get(f);
				System.out.println("Stop # " + f + "\t" + ((gtfs != null) ? gtfs.getCode() : "-") + "\t" + "-" + "*");
			}
		}
		System.out.println("---");
		for (Relation r : osmRelationNotFoundInGTFS){
			System.out.println("---");
			Affinity affinityGTFS = affinities.get(r);
			System.out.println("Relation " + r.getId() + " (" + r.getName() + ") NOT matched in GTFS ");
			System.out.println("Best match (" + affinityGTFS.affinity + "): id: " + affinityGTFS.trip.getTripID() + " " + routes.get(affinityGTFS.trip.getRoute().getId()).getShortName() + " " + affinityGTFS.trip.getName());
			StopsList stopGTFS = stopTimes.get(affinityGTFS.trip.getTripID());
			long max = Math.max(stopGTFS.getStops().size(), r.getStops().size());
			System.out.println("Progressivo \tGTFS\tOSM");
			for (long f = 1; f <= max ; f++){
				Stop gtfs= stopGTFS.getStops().get(f);
				Stop osm = r.getStops().get(f);
				try{
					System.out.println("Stop # " + f + "\t" + ((gtfs != null) ? gtfs.getCode() : "-") + "\t" + ((osm != null) ? osm.getCode() : "-") + ((gtfs != null) && (osm != null) &&  gtfs.getCode().equals(osm.getCode()) ? "" : "*") + "\t" + ((osm != null) ? osm.getName() : "-"));
				}catch (Exception e) {
					System.out.println("Stop # " + f + "\t-\r-");
				}
			}
		}
		System.out.println("---");
		System.out.println("Relation in OSM matched in GTFS: " + osmRelationFoundInGTFS.size());
		System.out.println("Relation in OSM not matched in GTFS: " + osmRelationNotFoundInGTFS.size());
		System.out.println("Trips in GTFS not matched in OSM: " + tripsNotFoundInOSM.size());
		System.out.println("---");
		return null;
	}

	private static class Affinity{
		public Trip trip;
		public int affinity;
	}
}
