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
import it.osm.gtfs.utils.GTFSImportSettings;
import it.osm.gtfs.utils.SharedCliOptions;
import it.osm.gtfs.utils.StopsUtils;
import org.xml.sax.SAXException;
import picocli.CommandLine;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "reldiff", description = "Analyze the diff between osm relations and gtfs trips")
public class CmdGenerateRoutesDiff implements Callable<Void> {

    @CommandLine.Mixin
    private SharedCliOptions sharedCliOptions;

    @Override
    public Void call() throws ParserConfigurationException, IOException, SAXException {
        List<OSMStop> osmStops = OSMParser.readOSMStops(GTFSImportSettings.getInstance().getOsmStopsFilePath(), SharedCliOptions.checkStopsOfAnyOperatorTagValue);
        Map<String, OSMStop> osmstopsGTFSId = StopsUtils.getGTFSIdOSMStopMap(osmStops);
        Map<String, OSMStop> osmstopsOsmID = StopsUtils.getOSMIdOSMStopMap(osmStops);
        ReadOSMRelationsResult osmRels = OSMParser.readOSMRelations(new File(GTFSImportSettings.getInstance().getOsmRelationsFilePath()), osmstopsOsmID, SharedCliOptions.checkStopsOfAnyOperatorTagValue);

        Map<String, Route> routes = GTFSParser.readRoutes(GTFSImportSettings.getInstance().getGTFSDataPath() + GTFSImportSettings.GTFS_ROUTES_FILE_NAME);
        ReadStopTimesResult readStopTimesResult = GTFSParser.readStopTimes(GTFSImportSettings.getInstance().getGTFSDataPath() + GTFSImportSettings.GTFS_STOP_TIMES_FILE_NAME, osmstopsGTFSId);
        List<Trip> trips = GTFSParser.readTrips(GTFSImportSettings.getInstance().getGTFSDataPath() + GTFSImportSettings.GTFS_TRIPS_FILE_NAME,
                routes, readStopTimesResult.getTripIdStopListMap());

        //looking from mapping gtfs trip into existing osm relations
        Set<Relation> osmRelationNotFoundInGTFS = new HashSet<>(osmRels.getFinalValidRelations());
        Set<Relation> osmRelationFoundInGTFS = new HashSet<>();
        List<Trip> tripsNotFoundInOSM = new LinkedList<>();

        Multimap<Route, Trip> groupedTrips = GTFSParser.groupTrips(routes, trips);
        Set<Route> routeSet = new TreeSet<>(groupedTrips.keySet());
        Map<Relation, Affinity> affinities = new HashMap<>();

        for (Route route : routeSet) {
            Collection<Trip> allTrips = groupedTrips.get(route);
            Set<Trip> uniqueTrips = new HashSet<>(allTrips);

            for (Trip trip : uniqueTrips) {
                TripStopsList s = readStopTimesResult.getTripIdStopListMap().get(trip.getTripId());
                if (GTFSImportSettings.getInstance().getPlugin().isValidTrip(allTrips, uniqueTrips, trip, s)) {
                    if (GTFSImportSettings.getInstance().getPlugin().isValidRoute(route)) {
                        Relation found = null;
                        for (Relation validRelation : osmRels.getFinalValidRelations()) {
                            if (validRelation.equalsStops(s) || GTFSImportSettings.getInstance().getPlugin().isRelationSameAs(validRelation, s)) {
                                if (found != null) {
                                    osmRelationNotFoundInGTFS.remove(found);
                                    osmRelationFoundInGTFS.add(found);
                                }
                                found = validRelation;
                            }
                            int affinity = validRelation.getStopsAffinity(s);
                            Affinity oldAff = affinities.get(validRelation);
                            if (oldAff == null) {
                                oldAff = new Affinity();
                                oldAff.trip = trip;
                                oldAff.affinity = affinity;
                                affinities.put(validRelation, oldAff);
                            } else if (oldAff.affinity < affinity) {
                                oldAff.trip = trip;
                                oldAff.affinity = affinity;
                            }
                        }
                        if (found != null) {
                            osmRelationNotFoundInGTFS.remove(found);
                            osmRelationFoundInGTFS.add(found);
                        } else {
                            tripsNotFoundInOSM.add(trip);
                            System.err.println("Warning: tripid: " + trip.getTripId() + " (" + trip.getTripHeadsign() + ") not found in OSM, details below.");
                            System.err.println("Details: shapeid: " + trip.getShapeId() + " shortname: " + route.getShortName() + " longname:" + route.getLongName());
                        }
                    } else {
                        System.err.println("Warning: tripid: " + trip.getTripId() + " skipped (invalidated route by plugin).");
                    }
                } else {
                    System.err.println("Warning: tripid: " + trip.getTripId() + " skipped (invalidated trip by plugin).");
                }
            }
        }

        System.out.println("---");

        for (Relation relation : osmRelationFoundInGTFS) {
            System.out.println("Relation " + relation.getId() + " (" + relation.getName() + ") matched in GTFS ");
        }

        System.out.println("---");

        for (Trip trip : tripsNotFoundInOSM) {
            System.out.println("Trip " + trip.getTripId() + " (" + routes.get(trip.getRoute().getId()).getShortName() + " - " + trip.getTripHeadsign() + ") not found in OSM ");
            TripStopsList stopGTFS = readStopTimesResult.getTripIdStopListMap().get(trip.getTripId());
            System.out.println("Progressivo \tGTFS\tOSM");

            for (long f = 1; f <= stopGTFS.getStopSequenceOSMStopMap().size(); f++) {
                Stop gtfs = stopGTFS.getStopSequenceOSMStopMap().get(f);
                System.out.println("Stop # " + f + "\t" + ((gtfs != null) ? gtfs.getCode() : "-") + "\t" + "-" + "*");
            }
        }

        System.out.println("---");

        for (Relation relation : osmRelationNotFoundInGTFS) {
            System.out.println("---");

            Affinity affinityGTFS = affinities.get(relation);
            System.out.println("Relation " + relation.getId() + " (" + relation.getName() + ") NOT matched in GTFS ");
            System.out.println("Best match (" + affinityGTFS.affinity + "): id: " + affinityGTFS.trip.getTripId() + " " + routes.get(affinityGTFS.trip.getRoute().getId()).getShortName() + " " + affinityGTFS.trip.getTripHeadsign());
            TripStopsList stopGTFS = readStopTimesResult.getTripIdStopListMap().get(affinityGTFS.trip.getTripId());

            long max = Math.max(stopGTFS.getStopSequenceOSMStopMap().size(), relation.getStops().size());

            System.out.println("Progressivo \tGTFS\tOSM");

            for (long f = 1; f <= max; f++) {
                Stop gtfs = stopGTFS.getStopSequenceOSMStopMap().get(f);
                Stop osm = relation.getStops().get(f);
                try {
                    System.out.println("Stop # " + f + "\t" + ((gtfs != null) ? gtfs.getCode() : "-") + "\t" + ((osm != null) ? osm.getCode() : "-") + ((gtfs != null) && (osm != null) && gtfs.getCode().equals(osm.getCode()) ? "" : "*") + "\t" + ((osm != null) ? osm.getName() : "-"));
                } catch (Exception e) {
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

    private static class Affinity {
        public Trip trip;
        public int affinity;
    }
}
