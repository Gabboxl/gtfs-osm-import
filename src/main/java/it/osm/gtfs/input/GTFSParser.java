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
package it.osm.gtfs.input;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import it.osm.gtfs.enums.OSMStopType;
import it.osm.gtfs.enums.RouteType;
import it.osm.gtfs.enums.WheelchairAccess;
import it.osm.gtfs.models.*;
import it.osm.gtfs.utils.GTFSImportSettings;
import it.osm.gtfs.utils.SharedCliOptions;
import org.fusesource.jansi.Ansi;
import org.jxmapviewer.viewer.GeoPosition;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

import static org.fusesource.jansi.Ansi.ansi;

public class GTFSParser {

    public static List<GTFSStop> readStops(String fName) throws IOException {
        List<GTFSStop> resultGtfsStopsList = new ArrayList<>();

        String thisLine;
        String[] elements;
        int stopIdKey = -1, stopNameKey = -1, stopCodeKey = -1, stopLatKey = -1, stopLonKey = -1, locationTypeKey = -1, parentStationKey = -1, wheelchairBoardingKey = -1;

        BufferedReader br = new BufferedReader(new FileReader(fName, StandardCharsets.UTF_8));
        boolean isFirstLine = true;
        Hashtable<String, Integer> keysIndex = new Hashtable<>();
        while ((thisLine = br.readLine()) != null) {
            if (isFirstLine) {
                isFirstLine = false;

                thisLine = thisLine.replace("\"", "");
                String[] keys = thisLine.split(",");

                for (int i = 0; i < keys.length; i++) {
                    switch (keys[i]) {
                        case "stop_id" -> stopIdKey = i;
                        case "stop_name" -> stopNameKey = i;
                        case "stop_lat" -> stopLatKey = i;
                        case "stop_lon" -> stopLonKey = i;
                        case "stop_code" -> stopCodeKey = i;
                        case "location_type" -> locationTypeKey = i;
                        case "parent_station" -> parentStationKey = i;
                        case "wheelchair_boarding" -> wheelchairBoardingKey = i;


                        // gtfs stop_url is mapped to source_ref tag in OSM
                        case "stop_url" -> keysIndex.put("source_ref", i);
                        default -> {
                            String t = "gtfs_" + keys[i];
                            keysIndex.put(t, i);
                        }
                    }
                }

                //GTFS Brescia: if code isn't present we use id as code
                if (stopCodeKey == -1)
                    stopCodeKey = stopIdKey;
            } else {
                elements = getElementsFromLine(thisLine, true);

                //GTFS Milano: code column present but empty (using id as code)
                String stopCode = elements[stopCodeKey];
                if (stopCode.length() == 0)
                    stopCode = elements[stopIdKey];
                if (stopCode.length() > 0) {
                    if (locationTypeKey >= 0 && parentStationKey >= 0 && "1".equals(elements[locationTypeKey])) {
                        //this is a station (group of multiple stops)
                        System.out.println(ansi().render("@|red GTFSParser: Skipped a station (group of multiple stops) with gtfs id: |@" + elements[stopIdKey]));
                    } else {


                        GTFSStop gtfsStop = new GTFSStop(elements[stopIdKey],
                                elements[stopCodeKey],
                                new GeoPosition(Double.parseDouble(elements[stopLatKey]), Double.parseDouble(elements[stopLonKey])),
                                elements[stopNameKey],
                                null, //TODO: we probably should find a way to get the real operator from GTFS for GTFS-type stops - no because the operator is set by us
                                null,
                                (wheelchairBoardingKey > -1) ? WheelchairAccess.getEnumByGtfsValue(Integer.parseInt(elements[wheelchairBoardingKey])) : null
                        );

                        OSMStopType stopType = GTFSImportSettings.getInstance().getPlugin().getStopType(gtfsStop);

                        if (!stopType.equals(OSMStopType.PHYSICAL_BUS_STOP) && SharedCliOptions.onlyBusStops) {
                            continue;
                        }

                        //todo: probably we should remove this plugin call and instead find a way to include the stop type directly in the constructor up there
                        gtfsStop.setStopType(stopType);


                        if (GTFSImportSettings.getInstance().getPlugin().isValidStop(gtfsStop)) {
                            resultGtfsStopsList.add(gtfsStop);
                        }
                    }
                } else {
                    System.out.println(ansi().render("@|red GTFSParser: Failed to parse stops.txt line: |@" + thisLine));
                }
            }
        }
        br.close();

        return resultGtfsStopsList;
    }

    public static List<Trip> readTrips(String gtfsTripsFilePath, Map<String, Route> routes, Map<String, TripStopsList> stopTimes) throws IOException {
        List<Trip> finalTripsList = new ArrayList<>();

        if (stopTimes.isEmpty()) {
            System.out.println(ansi().render("@|red No stop times provided! The trips list will be generated without a stop list! |@"));
        }

        String thisLine;
        String[] elements;
        int shape_id = -1, route_id = -1, trip_id = -1, trip_headsign = -1, wheelchair_accessible = -1;

        BufferedReader br = new BufferedReader(new FileReader(gtfsTripsFilePath, StandardCharsets.UTF_8));
        boolean isFirstLine = true;
        while ((thisLine = br.readLine()) != null) {
            if (isFirstLine) {
                isFirstLine = false;
                thisLine = thisLine.replace("\"", "");
                String[] keys = thisLine.split(",");
                for (int i = 0; i < keys.length; i++) {
                    switch (keys[i]) {
                        case "route_id" -> route_id = i;
                        case "trip_id" -> trip_id = i;
                        case "trip_headsign" -> trip_headsign = i;
                        case "shape_id" -> shape_id = i;
                        case "wheelchair_accessible" -> wheelchair_accessible = i;

                    }
                }
                //                    System.out.println(stopIdKey+","+stopNameKey+","+stopLatKey+","+stopLonKey);
            } else {
                elements = getElementsFromLine(thisLine, false);

                if (elements[shape_id].length() > 0) {
                    finalTripsList.add(new Trip(elements[trip_id],
                            routes.get(elements[route_id]),
                            elements[shape_id],
                            (trip_headsign > -1) ? elements[trip_headsign] : "",
                            stopTimes.get(elements[trip_id]),
                            (wheelchair_accessible > -1) ? WheelchairAccess.getEnumByGtfsValue(Integer.parseInt(elements[wheelchair_accessible])) : null
                    ));
                }
            }
        }
        br.close();

        return finalTripsList;
    }

    public static Map<String, Shape> readShapes(String fName) throws IOException {
        Map<String, Shape> result = new TreeMap<>();

        String thisLine;
        String[] elements;
        int shape_id = -1, shape_pt_lat = -1, shape_pt_lon = -1, shape_pt_sequence = -1;

        BufferedReader br = new BufferedReader(new FileReader(fName, StandardCharsets.UTF_8));
        boolean isFirstLine = true;
        while ((thisLine = br.readLine()) != null) {
            if (isFirstLine) {
                isFirstLine = false;
                thisLine = thisLine.replace("\"", "");
                String[] keys = thisLine.split(",");
                for (int i = 0; i < keys.length; i++) {
                    switch (keys[i]) {
                        case "shape_id" -> shape_id = i;
                        case "shape_pt_lat" -> shape_pt_lat = i;
                        case "shape_pt_lon" -> shape_pt_lon = i;
                        case "shape_pt_sequence" -> shape_pt_sequence = i;
                    }
                }
                //                    System.out.println(stopIdKey+","+stopNameKey+","+stopLatKey+","+stopLonKey);
            } else {
                elements = getElementsFromLine(thisLine, true);

                if (elements[shape_id].length() > 0) {
                    Shape s = result.get(elements[shape_id]);
                    if (s == null) {
                        s = new Shape(elements[shape_id]);
                        result.put(elements[shape_id], s);
                    }
                    s.pushPoint(Long.parseLong(elements[shape_pt_sequence]), Double.parseDouble(elements[shape_pt_lat]), Double.parseDouble(elements[shape_pt_lon]));
                }
            }
        }
        br.close();
        return result;
    }

    public static Map<String, Route> readRoutes(String fName) throws IOException {
        Map<String, Route> finalRouteIdRouteMap = new HashMap<>();

        String thisLine;
        String[] elements;
        int route_id = -1, agency_id = -1, route_short_name = -1, route_long_name = -1, route_type = -1, route_color = -1;

        BufferedReader br = new BufferedReader(new FileReader(fName, StandardCharsets.UTF_8));
        boolean isFirstLine = true;
        while ((thisLine = br.readLine()) != null) {
            if (isFirstLine) {
                isFirstLine = false;
                thisLine = thisLine.replace("\"", "");
                String[] keys = thisLine.split(",");
                for (int i = 0; i < keys.length; i++) {
                    switch (keys[i]) {
                        case "route_id" -> route_id = i;
                        case "agency_id" -> agency_id = i;
                        case "route_short_name" -> route_short_name = i;
                        case "route_long_name" -> route_long_name = i;
                        case "route_type" -> route_type = i;
                        case "route_color" -> route_color = i;
                    }
                }
            } else {
                elements = getElementsFromLine(thisLine, true);

                if (elements[route_id].length() > 0) {
                    finalRouteIdRouteMap.put(elements[route_id],
                            new Route(elements[route_id],
                                    elements[agency_id],
                                    elements[route_long_name],
                                    elements[route_short_name],
                                    RouteType.getEnumByGtfsValue(Integer.parseInt(elements[route_type])),
                                    elements[route_color])
                    );
                }
            }
        }
        br.close();

        return finalRouteIdRouteMap;
    }

    public static ReadStopTimesResult readStopTimes(String gtfsStopTimesFilePath, Map<String, OSMStop> gtfsIdOsmStopMap) throws IOException {
        Map<String, TripStopsList> tripIdStopListMap = new TreeMap<>();
        Set<String> missingStops = new HashSet<>();

        final ReadStopTimesResult readStopTimesResult;

        int count = 0;

        String thisLine;
        String[] thisLineElements;
        int trip_id = -1, stop_id = -1, stop_sequence = -1, arrival_time = -1;

        Path filePath = Paths.get(gtfsStopTimesFilePath);

        long numberOfLines = Files.lines(filePath).count();

        BufferedReader br = new BufferedReader(new FileReader(filePath.toFile(), StandardCharsets.UTF_8));
        boolean isFirstLine = true;
        while ((thisLine = br.readLine()) != null) {
            count++;

            if (count % 100000 == 0)
                System.out.println(ansi().fg(Ansi.Color.YELLOW).a("Stop times read so far: ").reset().a(count + "/" + numberOfLines));

            if (isFirstLine) {
                isFirstLine = false;

                thisLine = thisLine.replace("\"", "");
                String[] keys = thisLine.split(",");

                for (int i = 0; i < keys.length; i++) {
                    switch (keys[i]) {
                        case "trip_id" -> trip_id = i;
                        case "arrival_time" -> arrival_time = i;
                        case "stop_id" -> stop_id = i;
                        case "stop_sequence" -> stop_sequence = i;
                    }
                }

            } else {
                thisLineElements = getElementsFromLine(thisLine, true);

                if (thisLineElements[trip_id].length() > 0) {
                    TripStopsList tripStopsList = tripIdStopListMap.get(thisLineElements[trip_id]);

                    if (tripStopsList == null) {
                        tripStopsList = new TripStopsList(thisLineElements[trip_id]);
                        tripIdStopListMap.put(thisLineElements[trip_id], tripStopsList);
                    }

                    String thisLineGtfsID = thisLineElements[stop_id];

                    if (gtfsIdOsmStopMap.get(thisLineGtfsID) != null) {

                        tripStopsList.addStop(Long.parseLong(thisLineElements[stop_sequence]), gtfsIdOsmStopMap.get(thisLineGtfsID), thisLineElements[arrival_time]);
                    } else {
                        tripStopsList.invalidate();

                        if (!missingStops.contains(thisLineGtfsID)) {
                            missingStops.add(thisLineGtfsID);
                            System.out.println(ansi().render("@|red Warning: GTFS stop with gtfsId=" + thisLineGtfsID + " not found in OpenStreetMap data! The trip " + thisLineElements[trip_id] + " and maybe others won't be generated! |@"));
                        }
                    }
                }
            }
        }

        System.out.println(ansi().fg(Ansi.Color.GREEN).a("Stop times read completed.").reset());


        br.close();

        if (missingStops.size() > 0) {
            System.out.println(ansi().render("@|red \nError: Some GTFS stops weren't found in OSM data. |@"));
            System.out.println(ansi().render("@|red Make sure you uploaded the new GTFS stops data to OpenStreetMap before running this command! |@"));
            System.out.println(ansi().render("@|red Run the GTFSOSMImport \"stops\" command to create the new stops, upload the new stops to OSM, and then run this command again! |@"));
        }

        readStopTimesResult = new ReadStopTimesResult(tripIdStopListMap, missingStops);

        return readStopTimesResult;
    }


    public static GTFSFeedInfo readFeedInfo(String gtfsFeedInfoPath) throws IOException {

        GTFSFeedInfo gtfsFeedInfo;

        String thisLine;
        String[] elements;
        int feed_publisher_name = -1, feed_publisher_url = -1, feed_start_date = -1, feed_end_date = -1, feed_version = -1;

        BufferedReader br = new BufferedReader(new FileReader(gtfsFeedInfoPath, StandardCharsets.UTF_8));
        boolean isFirstLine = true;
        while ((thisLine = br.readLine()) != null) {
            if (isFirstLine) {
                isFirstLine = false;
                thisLine = thisLine.replace("\"", "");
                String[] keys = thisLine.split(",");
                for (int i = 0; i < keys.length; i++) {
                    switch (keys[i]) {
                        case "feed_publisher_name" -> feed_publisher_name = i;
                        case "feed_publisher_url" -> feed_publisher_url = i;
                        case "feed_start_date" -> feed_start_date = i;
                        case "feed_end_date" -> feed_end_date = i;
                        case "feed_version" -> feed_version = i;

                    }
                }
            } else {
                elements = getElementsFromLine(thisLine, true);

                gtfsFeedInfo = new GTFSFeedInfo(elements[feed_publisher_name],
                        elements[feed_publisher_url],
                        (feed_start_date > -1) ? elements[feed_start_date] : null,
                        (feed_end_date > -1) ? elements[feed_end_date] : null,
                        (feed_version > -1) ? elements[feed_version] : null
                );

                return gtfsFeedInfo;
            }
        }
        br.close();

        return null;
    }

    public static Multimap<Route, Trip> groupTrips(Map<String, Route> routes, List<Trip> trips) {
        Collections.sort(trips);
        Multimap<Route, Trip> result = ArrayListMultimap.create();

        for (Trip trip : trips) {
            Route route = routes.get(trip.getRoute().getId());

            TripStopsList tripStopsList = trip.getStopsList();

            if (tripStopsList.isValid()) {
                result.put(route, trip);
            }
        }

        return result;
    }

    /**
     * This code uses a regular expression to split the input string by commas that are not inside quotes.
     * It then iterates through the resulting array, removing the quotes from the elements that are enclosed in quotes, and adding them to the elements list.
     * If removeCommasFromValues is true, it also removes any commas from the elements before adding them to the list.
     *
     * @param thisLine
     * @param removeCommasFromValues
     * @return Split line in a vector
     */
    private static String[] getElementsFromLine(String thisLine, boolean removeCommasFromValues) {
        List<String> elements = new ArrayList<>();

        thisLine = thisLine.trim();

        Pattern pattern = Pattern.compile(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        String[] temp = pattern.split(thisLine);

        for (String element : temp) {
            if (element.startsWith("\"") && element.endsWith("\"")) {
                element = element.substring(1, element.length() - 1);
            }

            if (removeCommasFromValues) {
                element = element.replace(",", "");
            }

            elements.add(element);
        }

        return elements.toArray(new String[0]);
    }

}
