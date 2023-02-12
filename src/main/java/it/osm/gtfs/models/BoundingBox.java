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

import it.osm.gtfs.output.IElementCreator;
import org.w3c.dom.Element;

import java.util.Collection;

public class BoundingBox {
    private Double minLat;
    private Double minLon;
    private Double maxLat;
    private Double maxLon;

    public BoundingBox(Collection<? extends Stop> stops){
        Stop first = stops.iterator().next();
        minLat = first.getGeoPosition().getLatitude();
        minLon = first.getGeoPosition().getLongitude();
        maxLat = first.getGeoPosition().getLatitude();
        maxLon = first.getGeoPosition().getLongitude();

        for (Stop s:stops){
            minLat = Math.min(minLat, s.getGeoPosition().getLatitude());
            minLon = Math.min(minLon, s.getGeoPosition().getLongitude());
            maxLat = Math.max(maxLat, s.getGeoPosition().getLatitude());
            maxLon = Math.max(maxLon, s.getGeoPosition().getLongitude());
        }
        //expand the bbox to fit point near the border
        minLat -= 0.01;
        minLon -= 0.01;
        maxLat += 0.01;
        maxLon += 0.01;
    }

    @Override
    public String toString() {
        return "[minLat=" + minLat + ", minLon=" + minLon
                + ", maxLat=" + maxLat + ", maxLon=" + maxLon + "]";
    }

    public String getAPIQuery(){
        //return "[bbox=" + minLon +"," + minLat + "," + maxLon +"," + maxLat + "]"; //questo era per la vecchia XAPI
        return minLon +"," + minLat + "," + maxLon +"," + maxLat; //questo è per la nuova api Overpass QL
    }

    public String getXMLTag() {
        return "<bounds minlat='" + minLat + "' minlon='" + minLon + "' maxlat='" + maxLat + "' maxlon='" + maxLon + "' origin='GTFSOSMImport' />";
    }

    public Element getXMLTag(IElementCreator document) {
        Element e = document.createElement("bounds");
        e.setAttribute("minlat", minLat.toString());
        e.setAttribute("minlon", minLon.toString());
        e.setAttribute("maxlat", maxLat.toString());
        e.setAttribute("maxlon", maxLon.toString());
        e.setAttribute("origin", "GTFSOSMImport");
        return e;
    }

}
