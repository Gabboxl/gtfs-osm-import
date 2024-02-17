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
package it.osm.gtfs.models;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class TripStopsList {
    private final String tripId;
    private final TreeMap<Long, OSMStop> stopSequenceOSMStopMap;
    private final TreeMap<Long, String> stopSequenceArrivalTimeMap;
    private Boolean valid = true;

    public TripStopsList(String tripId) {
        super();
        this.tripId = tripId;
        this.stopSequenceOSMStopMap = new TreeMap<>();
        this.stopSequenceArrivalTimeMap = new TreeMap<>();
    }

    private static <T, E> T getKeysByValue(Map<T, E> map, E value) {
        for (Entry<T, E> entry : map.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Boolean isValid() {
        return valid;
    }

    public void invalidate() {
        valid = false;
    }

    public String getTripId() {
        return tripId;
    }

    public void addStop(Long stopSequence, OSMStop osmStop, String arrivalTime) {
        stopSequenceOSMStopMap.put(stopSequence, osmStop);
        stopSequenceArrivalTimeMap.put(stopSequence, arrivalTime);
    }

    public String getStopsListTextFile() {
        StringBuilder buffer = new StringBuilder();
        for (Stop stop : stopSequenceOSMStopMap.values()) {
            buffer.append(stop.getCode() + " " + stop.getName() + "\n");
        }
        return buffer.toString();
    }

    public TreeMap<Long, OSMStop> getStopSequenceOSMStopMap() {
        return stopSequenceOSMStopMap;
    }

    public TreeMap<Long, String> getStopSequenceArrivalTimeMap() {
        return stopSequenceArrivalTimeMap;
    }

    //this method checks if the list contains the same stops as the parameter list by comparing the GTFS stop sequence in the stop_times.txt file
    public boolean equalsStops(TripStopsList o) {
        //we check if the number of contained stops is the same
        if (stopSequenceOSMStopMap.size() != o.stopSequenceOSMStopMap.size())
            return false;

        for (Long key : o.stopSequenceOSMStopMap.keySet()) {
            Stop a = stopSequenceOSMStopMap.get(key);
            Stop b = o.stopSequenceOSMStopMap.get(key);
            if (a == null || !a.equals(b))
                return false;
        }
        return true;
    }

    //this method checks if the list contains the same stops as the parameter list by iterating the tripstoplist and comparing each stop to this class tripstoplist
    public boolean equalsStopsNoSequenceCode(TripStopsList o) {
        //we check if the number of contained stops is the same
        if (stopSequenceOSMStopMap.size() != o.stopSequenceOSMStopMap.size())
            return false;

        var set1 = stopSequenceOSMStopMap.values().toArray();
        var set2 = o.stopSequenceOSMStopMap.values().toArray();

        //check if every stop in set1 is present in set2 at the same position of set1
        for (int i = 0; i < set1.length; i++) {
            Stop a = (Stop) set1[i];
            Stop b = (Stop) set2[i];
            if (a == null || !a.equals(b))
                return false;
        }


        return true;
    }

    //instead of equalsStops, this method checks if the list contains the same stops as the parameter list without checking the GTFS stop sequence in the stop_times.txt file
    public boolean equalsContainedStops(TripStopsList list) {
        //we check if the number of contained stops is the same
        if (stopSequenceOSMStopMap.size() != list.stopSequenceOSMStopMap.size())
            return false;

        for (Long key : list.stopSequenceOSMStopMap.keySet()) {
            Stop a = list.stopSequenceOSMStopMap.get(key);
            if (!stopSequenceOSMStopMap.containsValue(a))
                return false;
        }
        return true;
    }
}
