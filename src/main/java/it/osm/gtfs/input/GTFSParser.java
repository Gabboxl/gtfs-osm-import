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
package it.osm.gtfs.input;

import it.osm.gtfs.enums.GTFSWheelchairAccess;
import it.osm.gtfs.model.*;
import it.osm.gtfs.utils.GTFSImportSettings;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.fusesource.jansi.Ansi;

import static org.fusesource.jansi.Ansi.ansi;

public class GTFSParser {

    //TODO: rename to plural readBusStops
    public static List<GTFSStop> readBusStop(String fName) throws IOException{
        List<GTFSStop> result = new ArrayList<GTFSStop>();

        String thisLine;
        String [] elements;
        int stopIdKey=-1, stopNameKey=-1, stopCodeKey=-1, stopLatKey=-1, stopLonKey=-1, locationTypeKey=-1, parentStationKey=-1, wheelchairBoardingKey = -1;

        BufferedReader br = new BufferedReader(new FileReader(fName));
        boolean isFirstLine = true;
        Hashtable<String, Integer> keysIndex = new Hashtable<String, Integer>();
        while ((thisLine = br.readLine()) != null) {
            if (isFirstLine) {
                isFirstLine = false;
                thisLine = thisLine.replace("\"", "");
                String[] keys = thisLine.split(",");
                for(int i=0; i<keys.length; i++){
                    if(keys[i].equals("stop_id")) stopIdKey = i;
                    else if(keys[i].equals("stop_name")) stopNameKey = i;
                    else if(keys[i].equals("stop_lat")) stopLatKey = i;
                    else if(keys[i].equals("stop_lon")) stopLonKey = i;
                    else if(keys[i].equals("stop_code")) stopCodeKey = i;
                    else if(keys[i].equals("location_type")) locationTypeKey = i;
                    else if(keys[i].equals("parent_station")) parentStationKey = i;
                    else if(keys[i].equals("wheelchair_boarding")) wheelchairBoardingKey = i;

                        // gtfs stop_url is mapped to source_ref tag in OSM
                    else if(keys[i].equals("stop_url")){
                        keysIndex.put("source_ref", i);
                    }
                    else {
                        String t = "gtfs_"+keys[i];
                        keysIndex.put(t, i);
                    }
                }
                //GTFS Brescia: if code isn't present we use id as code
                if (stopCodeKey == -1)
                    stopCodeKey = stopIdKey;
            }
            else {
                thisLine = thisLine.trim();

                if(thisLine.contains("\"")) {
                    String[] temp = thisLine.split("\"");
                    for(int x=0; x<temp.length; x++){
                        if(x%2==1) temp[x] = temp[x].replace(",", "");
                    }
                    thisLine = "";
                    for(int x=0; x<temp.length; x++){
                        thisLine = thisLine + temp[x];
                    }
                }
                elements = thisLine.split(",");

                //GTFS Milano: code column present but empty (using id as code)
                String stopCode = elements[stopCodeKey];
                if (stopCode.length() == 0)
                    stopCode = elements[stopIdKey];
                if (stopCode.length() > 0){
                    if (locationTypeKey >= 0 && parentStationKey >= 0 && "1".equals(elements[locationTypeKey])){
                        //this is a station (group of multiple stops)
                        System.err.println("GTFSParser: skipped station (group of multiple stops): " + elements[stopIdKey]);
                    }else{
                        GTFSStop gs = new GTFSStop(elements[stopIdKey],
                                elements[stopCodeKey],
                                Double.valueOf(elements[stopLatKey]),
                                Double.valueOf(elements[stopLonKey]),
                                elements[stopNameKey],
                                null, //we probably should find a way to get the real operator from GTFS for GTFS-type stops
                                GTFSWheelchairAccess.values()[Integer.parseInt(elements[wheelchairBoardingKey])]); //this is not ideal as we are using the value as index of the enums but it works (we should create a lookup method with a for cycle)
                        if (GTFSImportSettings.getInstance().getPlugin().isValidStop(gs)){
                            result.add(gs);
                        }
                    }
                }else{
                    System.err.println("GTFSParser: Failed to parse stops.txt line: " + thisLine);
                }
            }
        }
        br.close();
        return result;
    }

    public static List<Trip> readTrips(String fName, Map<String, Route> routes, Map<String, StopsList> stopTimes) throws IOException{
        List<Trip> result = new ArrayList<Trip>();

        String thisLine;
        String [] elements;
        int shape_id=-1, route_id=-1, trip_id=-1, trip_headsign=-1;

        BufferedReader br = new BufferedReader(new FileReader(fName));
        boolean isFirstLine = true;
        while ((thisLine = br.readLine()) != null) {
            if (isFirstLine) {
                isFirstLine = false;
                thisLine = thisLine.replace("\"", "");
                String[] keys = thisLine.split(",");
                for(int i=0; i<keys.length; i++){
                    if(keys[i].equals("route_id")) route_id = i;
                    else if(keys[i].equals("trip_headsign")) trip_headsign = i;
                    else if(keys[i].equals("shape_id")) shape_id = i;
                    else if(keys[i].equals("trip_id")) trip_id = i;
                }
                //                    System.out.println(stopIdKey+","+stopNameKey+","+stopLatKey+","+stopLonKey);
            }
            else {
                thisLine = thisLine.trim();

                if(thisLine.contains("\"")) {
                    String[] temp = thisLine.split("\"");
                    for(int x=0; x<temp.length; x++){
                        if(x%2==1) temp[x] = temp[x].replace(",", "");
                    }
                    thisLine = "";
                    for(int x=0; x<temp.length; x++){
                        thisLine = thisLine + temp[x];
                    }
                }
                elements = thisLine.split(",");

                if (elements[shape_id].length() > 0){
                    result.add(new Trip(elements[trip_id], routes.get(elements[route_id]), elements[shape_id],
                            (trip_headsign > -1) ? elements[trip_headsign] : "", stopTimes.get(elements[trip_id])));
                }
            }
        }
        br.close();
        return result;
    }

    public static Map<String, Shape> readShapes(String fName) throws IOException{
        Map<String, Shape> result = new TreeMap<String, Shape>();

        String thisLine;
        String [] elements;
        int shape_id=-1, shape_pt_lat=-1, shape_pt_lon=-1, shape_pt_sequence=-1;

        BufferedReader br = new BufferedReader(new FileReader(fName));
        boolean isFirstLine = true;
        while ((thisLine = br.readLine()) != null) {
            if (isFirstLine) {
                isFirstLine = false;
                thisLine = thisLine.replace("\"", "");
                String[] keys = thisLine.split(",");
                for(int i=0; i<keys.length; i++){
                    if(keys[i].equals("shape_id")) shape_id = i;
                    else if(keys[i].equals("shape_pt_lat")) shape_pt_lat = i;
                    else if(keys[i].equals("shape_pt_lon")) shape_pt_lon = i;
                    else if(keys[i].equals("shape_pt_sequence")) shape_pt_sequence = i;
                }
                //                    System.out.println(stopIdKey+","+stopNameKey+","+stopLatKey+","+stopLonKey);
            }
            else {
                thisLine = thisLine.trim();

                if(thisLine.contains("\"")) {
                    String[] temp = thisLine.split("\"");
                    for(int x=0; x<temp.length; x++){
                        if(x%2==1) temp[x] = temp[x].replace(",", "");
                    }
                    thisLine = "";
                    for(int x=0; x<temp.length; x++){
                        thisLine = thisLine + temp[x];
                    }
                }
                elements = thisLine.split(",");

                if (elements[shape_id].length() > 0){
                    Shape s = result.get(elements[shape_id]);
                    if (s == null){
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

    public static Map<String, Route> readRoutes(String fName) throws IOException{
        Map<String, Route> result = new HashMap<String, Route>();

        String thisLine;
        String [] elements;
        int route_id=-1, route_short_name=-1, route_long_name=-1, agency_id = -1;

        BufferedReader br = new BufferedReader(new FileReader(fName));
        boolean isFirstLine = true;
        while ((thisLine = br.readLine()) != null) {
            if (isFirstLine) {
                isFirstLine = false;
                thisLine = thisLine.replace("\"", "");
                String[] keys = thisLine.split(",");
                for(int i=0; i<keys.length; i++){
                    if(keys[i].equals("route_id")) route_id = i;
                    else if(keys[i].equals("route_short_name")) route_short_name = i;
                    else if(keys[i].equals("route_long_name")) route_long_name = i;
                    else if(keys[i].equals("agency_id")) agency_id = i;
                }
            }
            else {
                thisLine = thisLine.trim();

                if(thisLine.contains("\"")) {
                    String[] temp = thisLine.split("\"");
                    for(int x=0; x<temp.length; x++){
                        if(x%2==1) temp[x] = temp[x].replace(",", "");
                    }
                    thisLine = "";
                    for(int x=0; x<temp.length; x++){
                        thisLine = thisLine + temp[x];
                    }
                }
                elements = thisLine.split(",");

                if (elements[route_id].length() > 0){
                    result.put(elements[route_id], new Route(elements[route_id],elements[route_short_name],elements[route_long_name], elements[agency_id]));
                }
            }
        }
        br.close();
        return result;
    }

    public static Map<String, StopsList> readStopTimes(String fName, Map<String, OSMStop> osmstops) throws IOException{
        Map<String, StopsList> result = new TreeMap<String, StopsList>();
        Set<String> missingStops = new HashSet<String>();
        int count = 0;

        String thisLine;
        String [] elements;
        int trip_id=-1, stop_id=-1, stop_sequence=-1, arrival_time = -1;

        BufferedReader br = new BufferedReader(new FileReader(fName));
        boolean isFirstLine = true;
        while ((thisLine = br.readLine()) != null) {
            count ++;
            if (count % 100000 == 0)
                System.out.println(ansi().fg(Ansi.Color.YELLOW).a("Stop Times Readed so far: ").reset().a(count));

            if (isFirstLine) {
                isFirstLine = false;
                thisLine = thisLine.replace("\"", "");
                String[] keys = thisLine.split(",");
                for(int i=0; i<keys.length; i++){
                    if(keys[i].equals("trip_id")) trip_id = i;
                    else if(keys[i].equals("arrival_time")) arrival_time = i;
                    else if(keys[i].equals("stop_id")) stop_id = i;
                    else if(keys[i].equals("stop_sequence")) stop_sequence = i;
                }
            } else {
                thisLine = thisLine.trim();

                if(thisLine.contains("\"")) {
                    String[] temp = thisLine.split("\"");
                    for(int x=0; x<temp.length; x++){
                        if(x%2==1) temp[x] = temp[x].replace(",", "");
                    }
                    thisLine = "";
                    for(int x=0; x<temp.length; x++){
                        thisLine = thisLine + temp[x];
                    }
                }
                elements = thisLine.split(",");

                if (elements[trip_id].length() > 0){
                    StopsList s = result.get(elements[trip_id]);
                    if (s == null){
                        s = new StopsList(elements[trip_id]);
                        result.put(elements[trip_id], s);
                    }
                    String gtfsID = elements[stop_id];
                    if (osmstops.get(gtfsID) != null){
                        s.pushPoint(Long.parseLong(elements[stop_sequence]), osmstops.get(gtfsID), elements[arrival_time]);
                    }else{
                        s.invalidate();
                        if (!missingStops.contains(gtfsID)){
                            missingStops.add(gtfsID);
                            System.err.println("Warning: No stop found with gtfs_id = " + gtfsID + ". This Trip " + elements[trip_id] + " and maybe others won't be generated!");
                        }
                    }
                }
            }
        }
        br.close();
        if (missingStops.size() > 0)
            System.err.println("Warning: Some stops weren't found, not all trip have been generated.");
        return result;
    }

    public static Multimap<String, Trip> groupTrip(List<Trip> trips, Map<String, Route> routes, Map<String, StopsList> stopTimes){
        Collections.sort(trips);
        Multimap<String, Trip> result = ArrayListMultimap.create();
        for (Trip t:trips){
            Route r = routes.get(t.getRoute().getId());
            StopsList s = stopTimes.get(t.getTripID());

            if (s.isValid()){
                result.put(r.getShortName(), t);
            }
        }
        return result;
    }
}
