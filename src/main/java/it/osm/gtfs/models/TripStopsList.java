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
    private Map<Long, OSMStop> seqOSMStopMap;
    private Map<Long, String> seqArrivalTimeMap;
    private Boolean valid = true;

    public TripStopsList(String tripId) {
        super(); //TODO: why is there a super() method call here?

        this.tripId = tripId;
        this.seqOSMStopMap = new TreeMap<>();
        this.seqArrivalTimeMap = new TreeMap<>();
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

    public void pushPoint(Long seq, OSMStop osmStop, String arrivalTime){
        seqOSMStopMap.put(seq, osmStop);
        seqArrivalTimeMap.put(seq, arrivalTime);
    }

    public String getRelationAsStopList(Trip t, Route r){
        StringBuilder buffer = new StringBuilder();
        for (Stop s: seqOSMStopMap.values()){
            buffer.append(s.getCode() + " " + s.getName()  + "\n");
        }
        return buffer.toString();
    }


    public Map<Long, OSMStop> getSeqOSMStopMap() {
        return seqOSMStopMap;
    }

    public void setSeqOSMStopMap(Map<Long, OSMStop> s){
        seqOSMStopMap = s;
    }

    public Map<Long, String> getSeqArrivalTimeMap() {
        return seqArrivalTimeMap;
    }

    public void setSeqArrivalTimeMap(Map<Long, String> s){
        seqArrivalTimeMap = s;
    }

    public boolean equalsStops(TripStopsList o) {
        if (seqOSMStopMap.size() != o.seqOSMStopMap.size())
            return false;
        for (Long key: o.seqOSMStopMap.keySet()){
            Stop a = seqOSMStopMap.get(key);
            Stop b = o.seqOSMStopMap.get(key);
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
