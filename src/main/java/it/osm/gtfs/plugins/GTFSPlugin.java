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

import java.util.Collection;
import java.util.Set;

import it.osm.gtfs.models.Relation;
import it.osm.gtfs.models.Route;
import it.osm.gtfs.models.Stop;
import it.osm.gtfs.models.TripStopsList;
import it.osm.gtfs.models.Trip;

public interface GTFSPlugin {
    /**
     * Apply changes to the bus stop name before generating OSM Import file
     */
    String fixBusStopName(String stopName);
    String fixBusStopRef(String stopRef);

    String fixTripName(String name);

    /**
     * Allow to exclude some stops from importing
     */
    Boolean isValidStop(Stop s);

    boolean isValidRoute(Route route);

    /**
     * allow plugins to define custom rules to decide if a route is the same
     */
    boolean isRelationSameAs(Relation relation, TripStopsList s);

    boolean isValidTrip(Collection<Trip> allTrips, Set<Trip> uniqueTrips, Trip trip, TripStopsList s);
}
