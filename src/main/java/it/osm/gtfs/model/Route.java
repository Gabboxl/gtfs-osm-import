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

import java.util.HashSet;
import java.util.Set;

public class Route implements Comparable<Route> {
	private String id;
	private String shortName;
	private String longName;
	private String agencyId;
	private Set<String> shapesIDs;
	
	public Route(String id, String shortName, String longName, String agencyId) {
		super();
		this.id = id;
		this.shortName = shortName;
		this.longName = longName;
		this.agencyId = agencyId;
		shapesIDs = new HashSet<String>();
	}

	public String getId() {
		return id;
	}

	public String getShortName() {
		return shortName;
	}

	public String getLongName() {
		return longName;
	}
	
	public String getAgencyId() {
		return agencyId;
	}

	public Set<String> getShapesIDs() {
		return shapesIDs;
	}
	
	public void putShape(String id){
		shapesIDs.add(id);
	}

	@Override
	public boolean equals(Object other) {
		if (other != null && other instanceof Route){
			return ((Route)other).id.equals(id);
		}
		return false;
	}

	public int compareTo(Route route) {
		return id.compareTo(route.id);
	}
	
	
}
