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

public class DefaultPlugin implements GTFSPlugin {

    @Override
    public String fixBusStopName(GTFSStop gtfsStop) {
        return gtfsStop.getName();
    }

    @Override
    public String fixBusStopRef(String busStopRef) {
        return busStopRef;
    }

    @Override
    public String fixTripHeadsignName(String name) {
        return name;
    }

    @Override
    public String fixGtfsVersionDate(String gtfsVersionDate) {
        return gtfsVersionDate;
    }

    @Override
    public Boolean isValidStop(GTFSStop gtfsStop) {
        return true;
    }

    @Override
    public boolean isValidRoute(Route route) {
        return true;
    }

    @Override
    public OSMStopType getStopType(GTFSStop gtfsStop) throws IllegalStateException {
        return OSMStopType.PHYSICAL_BUS_STOP;
    }

    @Override
    public boolean isRelationSameAs(Relation relation, TripStopsList s) {
        return false;
    }

    @Override
    public boolean isValidTrip(Collection<Trip> allTrips, Set<Trip> uniqueTrips, Trip trip, TripStopsList stopList) {
        return true;
    }
}
