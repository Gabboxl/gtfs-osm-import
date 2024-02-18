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
package it.osm.gtfs.plugins;

import it.osm.gtfs.enums.OSMStopType;
import it.osm.gtfs.models.*;

import java.util.Collection;
import java.util.Set;

public interface GTFSPlugin {
    /**
     * Manipulation of the bus stop name / other GTFS data before generating OSM Import file
     */
    String fixBusStopName(GTFSStop gtfsStop);

    /**
     * Manipulation of the stop's ref code
     */
    String fixBusStopRef(String stopRef);

    /**
     * Manipulation of the trip's headsign name
     */
    String fixTripHeadsignName(String name);

    /**
     * Manipulation of the GTFS version date
     */
    String fixGtfsVersionDate(String gtfsVersionDate);

    /**
     * Custom logic to consider valid or exclude specific stops from the import
     */
    Boolean isValidStop(GTFSStop gtfsStop);

    /**
     * Custom logic to consider valid or exclude specific routes from the import
     */
    boolean isValidRoute(Route route);

    /**
     * Custom logic to define the type of GTFS stop (bus, tram, subway, etc.)
     */
    OSMStopType getStopType(GTFSStop gtfsStop) throws IllegalStateException;

    /**
     * Custom logic to decide if a GTFS route is the same of an OSM relation
     */
    boolean isRelationSameAs(Relation relation, TripStopsList s);

    /**
     * Custom logic to consider valid or exclude specific trips from the import
     */

    boolean isValidTrip(Collection<Trip> allTrips, Set<Trip> uniqueTrips, Trip trip, TripStopsList s);
}
