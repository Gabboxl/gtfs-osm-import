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
package it.osm.gtfs.commands;

import it.osm.gtfs.input.GTFSParser;
import it.osm.gtfs.models.BoundingBox;
import it.osm.gtfs.models.GTFSStop;
import it.osm.gtfs.utils.GTFSImportSettings;
import picocli.CommandLine;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "bbox", mixinStandardHelpOptions = true, description = "Get the Bounding Box of the GTFS File and api links")
public class CmdGetBoundingBox implements Callable<Void> {

    @Override
    public Void call() throws IOException {
        List<GTFSStop> gtfs = GTFSParser.readStops(GTFSImportSettings.getInstance().getGTFSDataPath() + GTFSImportSettings.GTFS_STOP_FILE_NAME);
        BoundingBox bb = new BoundingBox(gtfs);

        System.out.println("GTFS bounding box: " + bb);

        return null;
    }

}
