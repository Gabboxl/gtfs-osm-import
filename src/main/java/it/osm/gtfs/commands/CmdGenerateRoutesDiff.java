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

import it.osm.gtfs.input.GTFSParser;
import it.osm.gtfs.input.OSMParser;
import it.osm.gtfs.models.*;
import it.osm.gtfs.utils.GTFSImportSettings;

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

import it.osm.gtfs.utils.SharedCliOptions;
import it.osm.gtfs.utils.StopsUtils;
import org.xml.sax.SAXException;

import com.google.common.collect.Multimap;
import picocli.CommandLine;

@CommandLine.Command(name = "reldiff", description = "Analyze the diff between osm relations and gtfs trips")
public class CmdGenerateRoutesDiff implements Callable<Void> {

    @CommandLine.Mixin
    private SharedCliOptions sharedCliOptions;

    @Override
    public Void call() throws ParserConfigurationException, IOException, SAXException {
        List<OSMStop> osmStops = OSMParser.readOSMStops(GTFSImportSettings.OSM_STOPS_FILE_PATH, SharedCliOptions.checkStopsOfAnyOperatorTagValue);
        Map<String, OSMStop> osmstopsGTFSId = StopsUtils.getGTFSIdOSMStopMap(osmStops);
        Map<String, OSMStop> osmstopsOsmID = StopsUtils.getOSMIdOSMStopMap(osmStops);
        List<Relation> osmRels = OSMParser.readOSMRelations(new File(GTFSImportSettings.OSM_RELATIONS_FILE_PATH), osmstopsOsmID);

        Map<String, Route> routes = GTFSParser.readRoutes(GTFSImportSettings.getInstance().getGTFSDataPath() +  GTFSImportSettings.GTFS_ROUTES_FILE_NAME);
        Map<String, TripStopsList> stopTimes = GTFSParser.readStopTimes(GTFSImportSettings.getInstance().getGTFSDataPath() +  GTFSImportSettings.GTFS_STOP_TIME_FILE_NAME, osmstopsGTFSId);
        List<Trip> trips = GTFSParser.readTrips(GTFSImportSettings.getInstance().getGTFSDataPath() +  GTFSImportSettings.GTFS_TRIPS_FILE_NAME,
                routes, stopTimes);

        //looking from mapping gtfs trip into existing osm relations
        Set<Relation> osmRelationNotFoundInGTFS = new HashSet<>(osmRels);
        Set<Relation> osmRelationFoundInGTFS = new HashSet<>();
        List<Trip> tripsNotFoundInOSM = new LinkedList<>();

        Multimap<String, Trip> groupedTrips = GTFSParser.groupTrip(trips, routes, stopTimes);
        Set<String> keys = new TreeSet<>(groupedTrips.keySet());
        Map<Relation, Affinity> affinities = new HashMap<>();

        for (String k : keys){
            Collection<Trip> allTrips = groupedTrips.get(k);
            Set<Trip> uniqueTrips = new HashSet<>(allTrips);

            for (Trip trip : uniqueTrips){
                Route route = routes.get(trip.getRoute().getId());
                TripStopsList s = stopTimes.get(trip.getTripId());
                if (GTFSImportSettings.getInstance().getPlugin().isValidTrip(allTrips, uniqueTrips, trip, s)){
                    if (GTFSImportSettings.getInstance().getPlugin().isValidRoute(route)){
                        Relation found = null;
                        for (Relation relation: osmRels){
                            if (relation.equalsStops(s) || GTFSImportSettings.getInstance().getPlugin().isRelationSameAs(relation, s)){
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
                            System.err.println("Warning: tripid: " + trip.getTripId() + " (" + trip.getName() + ") not found in OSM, details below." );
                            System.err.println("Details: shapeid: " + trip.getShapeId() + " shortname: " + route.getShortName() + " longname:" + route.getLongName());
                        }
                    }else{
                        System.err.println("Warning: tripid: " + trip.getTripId() + " skipped (invalidated route by plugin)." );
                    }
                }else{
                    System.err.println("Warning: tripid: " + trip.getTripId() + " skipped (invalidated trip by plugin)." );
                }
            }
        }

        System.out.println("---");

        for (Relation relation : osmRelationFoundInGTFS){
            System.out.println("Relation " + relation.getId() + " (" + relation.getName() + ") matched in GTFS ");
        }

        System.out.println("---");

        for (Trip trip : tripsNotFoundInOSM){
            System.out.println("Trip " + trip.getTripId() + " (" + routes.get(trip.getRoute().getId()).getShortName() + " - " + trip.getName() + ") not found in OSM ");
            TripStopsList stopGTFS = stopTimes.get(trip.getTripId());
            System.out.println("Progressivo \tGTFS\tOSM");

            for (long f = 1; f <= stopGTFS.getSeqOSMStopMap().size() ; f++){
                Stop gtfs= stopGTFS.getSeqOSMStopMap().get(f);
                System.out.println("Stop # " + f + "\t" + ((gtfs != null) ? gtfs.getCode() : "-") + "\t" + "-" + "*");
            }
        }

        System.out.println("---");

        for (Relation relation : osmRelationNotFoundInGTFS){
            System.out.println("---");

            Affinity affinityGTFS = affinities.get(relation);
            System.out.println("Relation " + relation.getId() + " (" + relation.getName() + ") NOT matched in GTFS ");
            System.out.println("Best match (" + affinityGTFS.affinity + "): id: " + affinityGTFS.trip.getTripId() + " " + routes.get(affinityGTFS.trip.getRoute().getId()).getShortName() + " " + affinityGTFS.trip.getName());
            TripStopsList stopGTFS = stopTimes.get(affinityGTFS.trip.getTripId());

            long max = Math.max(stopGTFS.getSeqOSMStopMap().size(), relation.getStops().size());

            System.out.println("Progressivo \tGTFS\tOSM");

            for (long f = 1; f <= max ; f++){
                Stop gtfs= stopGTFS.getSeqOSMStopMap().get(f);
                Stop osm = relation.getStops().get(f);
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
