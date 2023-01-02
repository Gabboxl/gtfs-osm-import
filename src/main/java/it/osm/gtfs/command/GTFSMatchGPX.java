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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.graphhopper.GraphHopper;
import com.graphhopper.GraphHopperConfig;
import com.graphhopper.ResponsePath;
import com.graphhopper.jackson.Gpx;
import com.graphhopper.matching.EdgeMatch;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.matching.Observation;
import com.graphhopper.routing.ev.OSMWayID;
import com.graphhopper.util.*;
import it.osm.gtfs.utils.GTFSImportSettings;
import it.osm.gtfs.utils.GTFSOSMWaysMatch;
import it.osm.gtfs.utils.GpxConversions;
import org.fusesource.jansi.Ansi;
import picocli.CommandLine;

import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import static org.fusesource.jansi.Ansi.ansi;

@CommandLine.Command(name = "match", description = "Match gpx files to OSM ways data to generate precise relations (this command will be removed soon)")
public class GTFSMatchGPX implements Callable<Void> {


    //@CommandLine.Option(names = {"-f", "--file"}, description = "export to file")
    //Boolean exportToFile;

    public Void call() throws IOException {

        //GTFSOSMWaysMatch(null);
        return null;
    }


}
