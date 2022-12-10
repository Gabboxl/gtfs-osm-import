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

import it.osm.gtfs.model.Relation;
import it.osm.gtfs.model.Route;
import it.osm.gtfs.model.Stop;
import it.osm.gtfs.model.StopsList;
import it.osm.gtfs.model.Trip;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class GTTTurinPlugin implements GTFSPlugin {

    public String fixBusStopRef(String busStopRef){
        if (busStopRef.startsWith("0"))
            return busStopRef.substring(1);
        return busStopRef;
    }

    public String fixBusStopName(String busStopName){
        busStopName = busStopName.replace('"', '\'')
                .replaceAll("Fermata [\\d]* - ", "").replaceAll("FERMATA [\\d]* - ", "")
                .replaceAll("Fermata ST[\\d]* - ", "").replaceAll("Fermata S00[\\d]* - ", "");

        try {
            if (Character.isUpperCase(busStopName.charAt(1))) {
                return camelCase(busStopName).trim();
            }
        }catch (Exception e){
            System.err.println("stopname: " + busStopName + " " + e); //sarebbe meglio e.printStacktrace(); al posto di un println
        }
        return busStopName;
    }

    @Override
    public String fixTripName(String name) {
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
    public Boolean isValidStop(Stop gs) {
        if (gs.getCode().trim().length() == 0){
            gs.setCode(gs.getGtfsId());
        }else{
            gs.setCode(fixBusStopRef(gs.getCode()));
        }
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
    public boolean isRelationSameAs(Relation relation, StopsList s) {
        //Allow missing last stop (bug in gtfs)
        if (relation.getStops().size() == s.getStops().size() + 1){
            for (Long key: s.getStops().keySet())
                if (!relation.getStops().get(key).equals(s.getStops().get(key)))
                    return false;
            System.err.println("GTTPlugin: Matched relation with gtfs bug " + relation.getId());
            return true;
        }else{
            return false;
        }
    }

    @Override
    public boolean isValidTrip(Collection<Trip> allTrips, Set<Trip> uniqueTrips, Trip trip, StopsList s) {
        int frequency = Collections.frequency(allTrips, trip);

        if (s.getStopsTime().get(1L) == null)
            return false;
        else if (frequency <= 1){
            System.err.println("GTTPlugin: Ignoring trip " + trip.getTripID() + " found only one, may not be a valid route");
            return false;
        }else if (frequency <= 4 && (s.getStopsTime().get(1L).startsWith("04") || s.getStopsTime().get(1L).startsWith("05") || s.getStopsTime().get(1L).startsWith("06"))){
            System.err.println("GTTPlugin: Ignoring trip " + trip.getTripID() + " found only four times in early morning, may be a warmup route");
            return false;
        }
        return true;
    }
}
