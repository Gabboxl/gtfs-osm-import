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
package it.osm.gtfs.plugins;

import it.osm.gtfs.enums.OSMStopType;
import it.osm.gtfs.models.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static org.fusesource.jansi.Ansi.ansi;

public class GTTTurinPlugin implements GTFSPlugin {

    public String fixBusStopRef(String busStopRef){
        if (busStopRef.startsWith("0"))
            return busStopRef.substring(1);
        return busStopRef;
    }

    public String fixBusStopName(GTFSStop gtfsStop){
        String stopName = gtfsStop.getName();

        String fixedStopName = stopName.replace('"', '\'');

        if(gtfsStop.getStopType().equals(OSMStopType.PHYSICAL_BUS_STOP)) {
            fixedStopName = fixedStopName.replaceAll("Fermata [\\d]* - ", "")
                    .replaceAll("FERMATA [\\d]* - ", "")
                    .replaceAll("Fermata ST[\\d]* - ", "")
                    .replaceAll("Fermata S00[\\d]* - ", "");

        } else if (gtfsStop.getStopType().equals(OSMStopType.PHYSICAL_SUBWAY_STOP)) {
            fixedStopName = fixedStopName.replaceAll("Metro ", "")
                    .replaceAll("METRO ", "");

        } else if (gtfsStop.getStopType().equals(OSMStopType.PHYSICAL_TRAIN_STATION)) {
            fixedStopName = fixedStopName.replaceAll("Stazione ", "")
                    .replaceAll("STAZIONE ", "");
        }



        try {
            if (Character.isUpperCase(fixedStopName.charAt(1))) {
                return camelCase(fixedStopName).trim();
            }
        }catch (Exception e){
            System.err.println("stopname: " + fixedStopName + " " + e); //sarebbe meglio e.printStacktrace(); al posto di un println
        }
        return fixedStopName;
    }

    @Override
    public String fixTripHeadsignName(String name) {
        return camelCase(name).trim();
    }

    private static String camelCase(String string) {
        String[] words = string.split("\\s");
        StringBuilder buffer = new StringBuilder();
        for (String s : words) {
            buffer.append(capitalize(s) + " ");
        }
        return buffer.toString();
    }

    private static String capitalize(String string) {
        if (string.length() == 0) return string;
        return string.substring(0, 1).toUpperCase() + string.substring(1).toLowerCase();
    }

    @Override
    public String fixGtfsVersionDate(String gtfsVersionDate) {

        final DateTimeFormatter OLD_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
        final DateTimeFormatter NEW_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDate date = LocalDate.parse(gtfsVersionDate, OLD_FORMATTER);

        return date.format(NEW_FORMATTER);
    }

    @Override
    public Boolean isValidStop(GTFSStop gtfsStop) {
        if (gtfsStop.getCode().trim().length() == 0){
            gtfsStop.setCode(gtfsStop.getGtfsId());
        }else{
            gtfsStop.setCode(fixBusStopRef(gtfsStop.getCode()));
        }

        //TODO: codice da revisionare per ref non numerici
		/*try{
			Integer.parseInt(gs.getCode());
		}catch(Exception e){
			System.err.println("Warning not numeric ref: " + gs.getCode() + " " + gs.getName() + " " + gs.getGtfsId());
		}*/
        return true;
    }

    @Override
    public boolean isValidRoute(Route route) {
        return !"GTT_E".equals(route.getAgencyId());
    }

    @Override
    public boolean isRelationSameAs(Relation relation, TripStopsList s) {
        //Allow missing last stop (bug in gtfs)
        if (relation.getStops().size() == s.getStopSequenceOSMStopMap().size() + 1){
            for (Long key: s.getStopSequenceOSMStopMap().keySet())
                if (!relation.getStops().get(key).equals(s.getStopSequenceOSMStopMap().get(key)))
                    return false;
            System.out.println(ansi().render("@|red GTTPlugin: Matched relation " + relation.getId() + " with gtfs bug |@" ));
            return true;
        }else{
            return false;
        }
    }

    @Override
    public boolean isValidTrip(Collection<Trip> allTrips, Set<Trip> uniqueTrips, Trip trip, TripStopsList s) {
        int frequency = Collections.frequency(allTrips, trip);

        if (s.getStopSequenceArrivalTimeMap().get(1L) == null)
            return false;
        else if (frequency <= 1){
            System.out.println(ansi().render("@|red GTTPlugin: Ignoring trip " + trip.getTripId() + " found only one, may not be a valid route |@"));
            return false;
        }else if (frequency <= 4 && (s.getStopSequenceArrivalTimeMap().get(1L).startsWith("04") || s.getStopSequenceArrivalTimeMap().get(1L).startsWith("05") || s.getStopSequenceArrivalTimeMap().get(1L).startsWith("06"))){
            System.out.println(ansi().render("@|red GTTPlugin: Ignoring trip " + trip.getTripId() + " found only four times in early morning, may be a warmup route |@"));
            return false;
        }
        return true;
    }

    //todo: is there any way to determine whether a gtfs stop is a tram or bus stop?
    @Override
    public OSMStopType getStopType(GTFSStop gtfsStop) {
        String stopName = gtfsStop.getName().toLowerCase();

        if (stopName.startsWith("fermata")) {
            return OSMStopType.PHYSICAL_BUS_STOP;
        } else if (stopName.startsWith("metro")) {
            return OSMStopType.PHYSICAL_SUBWAY_STOP;
        } else if (stopName.startsWith("stazione")) {
            return OSMStopType.PHYSICAL_TRAIN_STATION;
        }

        throw new IllegalStateException("GTTPlugin: Couldn't determine the GTFS stop type: " + gtfsStop.getName());
    }
}
