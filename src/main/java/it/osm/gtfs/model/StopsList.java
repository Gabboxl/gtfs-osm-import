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
package it.osm.gtfs.model;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

//probably this is only a list of gtfsstops? ?
public class StopsList {
    private final String id;
    private Map<Long, OSMStop> stops;
    private Map<Long, String> stopsTime;
    private Boolean valid = true;

    public StopsList(String id) {
        super();
        this.id = id;
        stops = new TreeMap<Long, OSMStop>();
        stopsTime = new TreeMap<Long, String>();
    }

    public Boolean isValid() {
        return valid;
    }

    public void invalidate() {
        valid = false;
    }

    public String getId() {
        return id;
    }

    public void pushPoint(Long seq, OSMStop stop, String arrival_time){
        stops.put(seq, stop);
        stopsTime.put(seq, arrival_time);
    }

    public String getRelationAsStopList(Trip t, Route r){
        StringBuilder buffer = new StringBuilder();
        for (Stop s:stops.values()){
            buffer.append(s.getCode() + " " + s.getName()  + "\n");
        }
        return buffer.toString();
    }


    public Map<Long, OSMStop> getStops() {
        return stops;
    }

    public void setStops(Map<Long, OSMStop> s){
        stops = s;
    }

    public Map<Long, String> getStopsTime() {
        return stopsTime;
    }

    public void setStopsTime(Map<Long, String> s){
        stopsTime = s;
    }

    public boolean equalsStops(StopsList o) {
        if (stops.size() != o.stops.size())
            return false;
        for (Long key: o.stops.keySet()){
            Stop a = stops.get(key);
            Stop b = o.stops.get(key);
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
