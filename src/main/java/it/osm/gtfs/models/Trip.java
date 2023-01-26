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


import it.osm.gtfs.enums.WheelchairAccess;

public class Trip implements Comparable<Trip> {
    private final Route route;
    private final String shapeId;
    private final String tripId;
    private final String tripHeadsign;
    private final TripStopsList tripStopsList;
    private final WheelchairAccess wheelchairAccess;

    public Trip(String tripId, Route route, String shapeId, String tripHeadsign, TripStopsList tripStopsList, WheelchairAccess wheelchairAccess) {
        super();
        this.route = route;
        this.shapeId = shapeId;
        this.tripId = tripId;
        this.tripHeadsign = tripHeadsign;
        this.tripStopsList = tripStopsList;
        this.wheelchairAccess = wheelchairAccess;
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

    public String getTripHeadsign() {
        return tripHeadsign;
    }
    public WheelchairAccess getWheelchairAccess() {
        return wheelchairAccess;
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
                ((other.tripStopsList == null && tripStopsList == null) ||
                        (other.tripStopsList != null && other.tripStopsList.equalsStops(tripStopsList))));
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
            if (a == 0 && tripStopsList != null && o.getStopsList() != null){
                if ((o.tripStopsList != null && o.tripStopsList.equalsStops(tripStopsList))){
                    return 0;
                }else{
                    return tripStopsList.getTripId().compareTo(o.getStopsList().getTripId());
                }
            }else{
                return a;
            }
        }else{
            return a;
        }
    }

    public TripStopsList getStopsList() {
        return tripStopsList;
    }
}
