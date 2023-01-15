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
package it.osm.gtfs.models;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class TripStopsList {
    private final String tripId;
    private Map<Long, OSMStop> stopSequenceOSMStopMap;
    private Map<Long, String> stopSequenceArrivalTimeMap;
    private Boolean valid = true;

    public TripStopsList(String tripId) {
        super();
        this.tripId = tripId;
        this.stopSequenceOSMStopMap = new TreeMap<>();
        this.stopSequenceArrivalTimeMap = new TreeMap<>();
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

    public void addStop(Long stopSequence, OSMStop osmStop, String arrivalTime){
        stopSequenceOSMStopMap.put(stopSequence, osmStop);
        stopSequenceArrivalTimeMap.put(stopSequence, arrivalTime);
    }

    public String getRelationAsStopList(Trip t, Route r){
        StringBuilder buffer = new StringBuilder();
        for (Stop s: stopSequenceOSMStopMap.values()){
            buffer.append(s.getCode() + " " + s.getName()  + "\n");
        }
        return buffer.toString();
    }


    public Map<Long, OSMStop> getStopSequenceOSMStopMap() {
        return stopSequenceOSMStopMap;
    }

    public void setStopSequenceOSMStopMap(Map<Long, OSMStop> s){
        stopSequenceOSMStopMap = s;
    }

    public Map<Long, String> getStopSequenceArrivalTimeMap() {
        return stopSequenceArrivalTimeMap;
    }

    public void setStopSequenceArrivalTimeMap(Map<Long, String> s){
        stopSequenceArrivalTimeMap = s;
    }

    public boolean equalsStops(TripStopsList o) {
        if (stopSequenceOSMStopMap.size() != o.stopSequenceOSMStopMap.size())
            return false;
        for (Long key: o.stopSequenceOSMStopMap.keySet()){
            Stop a = stopSequenceOSMStopMap.get(key);
            Stop b = o.stopSequenceOSMStopMap.get(key);
            if (a == null || !a.equals(b))
                return false;
        }
        return true;
    }



    private static <T, E> T getKeysByValue(Map<T, E> map, E value) {
        for (Entry<T, E> entry : map.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
