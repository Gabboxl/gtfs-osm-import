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

import it.osm.gtfs.enums.RouteType;

public class GTFSFeedInfo implements Comparable<GTFSFeedInfo> {
    private final String publisherName;
    private final String publisherUrl;
    private final String startDate;
    private final String endDate;
    private final String version;
    //private final String contactEmail;

    //private final List<Trip> trips;

    public GTFSFeedInfo(String publisherName, String publisherUrl, String startDate, String endDate, String version) {
        super();
        this.publisherName = publisherName;
        this.publisherUrl = publisherUrl;
        this.startDate = startDate;
        this.endDate = endDate;
        this.version = version;
    }


    @Override
    public boolean equals(Object other) {
        if (other instanceof GTFSFeedInfo){
            return ((GTFSFeedInfo)other).version.equals(version);
        }
        return false;
    }

    public int compareTo(GTFSFeedInfo route) {
        return version.compareTo(route.version);
    }

    public String getPublisherName() {
        return publisherName;
    }

    public String getPublisherUrl() {
        return publisherUrl;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public String getVersion() {
        return version;
    }
}
