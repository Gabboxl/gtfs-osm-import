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
package it.osm.gtfs.command;

import it.osm.gtfs.input.GTFSParser;
import it.osm.gtfs.models.BoundingBox;
import it.osm.gtfs.models.GTFSStop;
import it.osm.gtfs.utils.GTFSImportSettings;
import picocli.CommandLine;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "bbox", mixinStandardHelpOptions = true, description = "Get the Bounding Box of the GTFS File and api links")
public class GTFSGetBoundingBox implements Callable<Void> {

    @Override
    public Void call() throws IOException {
        List<GTFSStop> gtfs = GTFSParser.readStops(GTFSImportSettings.getInstance().getGTFSDataPath() + GTFSImportSettings.GTFS_STOP_FILE_NAME);
        BoundingBox bb = new BoundingBox(gtfs);

        System.out.println("GTFS " + bb);
        //Bus
        System.out.println("API link buses: " + GTFSImportSettings.OSM_OVERPASS_API_SERVER + "data=[bbox];node[highway=bus_stop];out meta;&bbox=" + bb.getAPIQuery());
        //Tram
        System.out.println("API link trams: " + GTFSImportSettings.OSM_OVERPASS_API_SERVER + "data=[bbox];node[railway=tram_stop];out meta;&bbox=" + bb.getAPIQuery());
        //Metro
        System.out.println("API link metro: " + GTFSImportSettings.OSM_OVERPASS_API_SERVER + "data=[bbox];node[railway=station];out meta;&bbox=" + bb.getAPIQuery());
        return null;
    }

}
