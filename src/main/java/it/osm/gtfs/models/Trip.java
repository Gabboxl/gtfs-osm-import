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


public class Trip implements Comparable<Trip> {
    private final Route route;
    private final String shapeId;
    private final String tripId;
    private final String name;
    private final StopsList stopList;

    public Trip(String tripId, Route route, String shapeId, String name, StopsList stopList) {
        super();
        this.route = route;
        this.shapeId = shapeId;
        this.tripId = tripId;
        this.name = name;
        this.stopList = stopList;
    }

    public String getTripId() {
        return tripId;
    }

    @Deprecated
    public String getRouteID() {
        return route.getId();
    }

    public Route getRoute() {
        return route;
    }

    public String getShapeId() {
        return shapeId;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Trip)) {
            return false;
        }

        Trip other = (Trip) obj;
        return (other.route.equals(route) && other.shapeId.equals(shapeId) &&
                ((other.stopList == null && stopList == null) ||
                        (other.stopList != null && other.stopList.equalsStops(stopList))));
    }

    @Override
    public int hashCode() {
        return route.getId().hashCode() + shapeId.hashCode();
    }

    @Override
    public int compareTo(Trip o) {
        int a = route.compareTo(o.route);
        if (a == 0){
            a = shapeId.compareTo(o.shapeId);
            if (a == 0 && stopList != null && o.getStopTime() != null){
                if ((o.stopList != null && o.stopList.equalsStops(stopList))){
                    return 0;
                }else{
                    return stopList.getTripId().compareTo(o.getStopTime().getTripId());
                }
            }else{
                return a;
            }
        }else{
            return a;
        }
    }

    public StopsList getStopTime() {
        return stopList;
    }
}
