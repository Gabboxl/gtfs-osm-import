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

    public boolean equalsStops(TripStopsList o) {
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
}
