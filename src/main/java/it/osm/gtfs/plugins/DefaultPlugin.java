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

import it.osm.gtfs.model.Relation;
import it.osm.gtfs.model.Route;
import it.osm.gtfs.model.Stop;
import it.osm.gtfs.model.StopsList;
import it.osm.gtfs.model.Trip;

public class DefaultPlugin implements GTFSPlugin {

	@Override
	public String fixBusStopName(String busStopName) {
		return busStopName;
	}
	
	@Override
	public String fixBusStopRef(String busStopRef) {
		return busStopRef;
	}

	@Override
	public String fixTripName(String name) {
		return name;
	}

	@Override
	public Boolean isValidStop(Stop s) {
		return true;
	}

	@Override
	public boolean isValidRoute(Route route) {
		return true;
	}

	@Override
	public boolean isRelationSameAs(Relation relation, StopsList s) {
		return false;
	}

	@Override
	public boolean isValidTrip(Collection<Trip> allTrips, Set<Trip> uniqueTrips, Trip trip, StopsList stopList) {
		return true;
	}
}
