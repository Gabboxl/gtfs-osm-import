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
import it.osm.gtfs.utils.OSMDistanceUtils;

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
    private Boolean isBusStopPosition = false; //TODO: should we move this variable to the OSMStop class too?

    //private Boolean isMetroStop; //TODO: should we add this check only for GTFS stops or also for osm stops, or not at all?
    public Stop stopMatchedWith;

    //maybe we should also create an arraylist for railwayStops matched with?
    public List<Stop> stopsMatchedWith = new ArrayList<Stop>(); //this is actually pretty much boilerplate as we dont use it for anything, but it could be useful for multiple matches cases that are currently unsupported in the tool


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
    public Boolean isBusStopPosition(){
        return isBusStopPosition;
    }


    public void setIsTramStop(Boolean isTramStop){
        this.isTramStop = isTramStop;
    }
    public void setIsBusStopPosition(Boolean isBusStopPosition){
        this.isBusStopPosition = isBusStopPosition;
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

    public boolean matches(Stop stop) {
        double distanceBetween = OSMDistanceUtils.distVincenty(getLat(), getLon(), stop.getLat(), stop.getLon());
        String debugData = "GTFS Stop data: [" + this + "] -> OSM Stop data: [" + stop +  "], exact distance between: " + distanceBetween + " m";

        if (stop.getCode() != null && stop.getCode().equals(getCode())){

            if (distanceBetween < 70 || (stop.getGtfsId() != null && getGtfsId() != null && stop.getGtfsId().equals(getGtfsId()))){
                //if the stops are less than 70m far away or are already linked with gtfsid TODO: or the revised key is already set to yes? maybe?
                return true;
            }else if (distanceBetween < 5000){
                System.err.println("Warning: Same ref tag with dist > 70 m (and less than 10km) / " + debugData);
            }

        }else if (distanceBetween < 70 && stop.getGtfsId() != null && getGtfsId() != null && stop.getGtfsId().equals(getGtfsId())){
            //if the stops have different ref tag code, same gtfs_id and are less than 70m far away
            System.err.println("Warning: Different ref tag matched but equal gtfs_id matched / " + debugData);
            return true;
        }

        return false;
    }

}
