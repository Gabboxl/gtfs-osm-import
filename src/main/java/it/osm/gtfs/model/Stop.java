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

import it.osm.gtfs.enums.WheelchairAccess;
import it.osm.gtfs.utils.GTFSImportSettings;

import java.util.ArrayList;
import java.util.List;

public abstract class Stop { //https://stackoverflow.com/a/42756744/9008381
    private String gtfsId;
    private String code;
    private Double lat;
    private Double lon;
    private String name;
    private String operator;
    private WheelchairAccess wheelchairAccessibility;
    private Boolean isTramStop; //TODO: should we move this variable to the OSMStop class?

    //private Boolean isMetroStop; //TODO: should we add this check only for GTFS stops or also for osm stops, or not at all?

    //TODO: maybe we should also create an arraylist for railwayStops matched with also?
    public List<Stop> stopsMatchedWith = new ArrayList<Stop>(); //TODO: this is actually pretty much boilerplate as we dont use it for anything, but it could be useful for multiple matches cases that are currently semi-supported but not handled in a GUI in the tool


    protected Stop(String gtfsId, String code, Double lat, Double lon, String name, String operator, WheelchairAccess wheelchairAccessibility) {
        super();
        this.gtfsId = gtfsId;
        this.code = code;
        this.lat = lat;
        this.lon = lon;
        this.name = name;
        this.operator = operator;
        this.wheelchairAccessibility = wheelchairAccessibility;
    }

    public String getGtfsId() {
        return gtfsId;
    }
    public String getCode() {
        return code;
    }
    public Double getLat() {
        return lat;
    }
    public Double getLon() {
        return lon;
    }
    public String getName() {
        return name;
    }
    public String getOperator() {
        return operator;
    }
    public WheelchairAccess getWheelchairAccessibility(){
        return wheelchairAccessibility;
    }
    public Boolean isTramStop(){
        return isTramStop;
    }


    public void setIsTramStop(Boolean isTramStop){
        this.isTramStop = isTramStop;
    }

    public void setGtfsId(String gtfsId) {
        this.gtfsId = gtfsId;
    }

    public void setCode(String code) {
        this.code = GTFSImportSettings.getInstance().getPlugin().fixBusStopRef(code);
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public void setWheelchairAccessibility(WheelchairAccess wheelchairAccessibility){
        this.wheelchairAccessibility = wheelchairAccessibility;
    }



    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((gtfsId == null) ? 0 : gtfsId.hashCode());
        return result;
    }

    //TODO: this method is never used, should we keep it? maybe yes because this function can be helpful
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Stop other = (Stop) obj;
        if (gtfsId == null) {
            return false;
        } else if (!gtfsId.equals(other.gtfsId))
            return false;
        return true;
    }

}
