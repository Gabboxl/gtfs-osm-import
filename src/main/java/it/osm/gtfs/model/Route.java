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

//TODO: consider converting classes like this one to java records
public class Route implements Comparable<Route> {
    private final String id;
    private final String agencyId;
    private final String shortName;
    private final String longName;
    private final String routeType;

    public Route(String id, String agencyId, String longName, String shortName, String routeType) {
        super();
        this.id = id;
        this.agencyId = agencyId;
        this.shortName = shortName;
        this.longName = longName;
        this.routeType = routeType;
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

    public String getRouteType() {
        return routeType;
    }


    @Override
    public boolean equals(Object other) {
        if (other instanceof Route){
            return ((Route)other).id.equals(id);
        }
        return false;
    }

    public int compareTo(Route route) {
        return id.compareTo(route.id);
    }

}
