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

import it.osm.gtfs.utils.GTFSImportSettings;
import it.osm.gtfs.utils.GTFSOSMWaysMatch;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Callable;

import static org.fusesource.jansi.Ansi.ansi;

@CommandLine.Command(name = "match", description = "Match gpx files to OSM ways data to generate precise relations (this command will be removed soon)")
public class GTFSMatchGPXFile implements Callable<Void> {
    //TODO: make this command match both folders or individual files given the path in input


    //@CommandLine.Option(names = {"-f", "--file"}, description = "export to file")
    //Boolean exportToFile;

    public Void call() throws IOException {


        File directoryPath = new File(GTFSImportSettings.getInstance().getOutputPath() + "/gpx");
        //List of all files and directories
        File[] gpx_files_list = directoryPath.listFiles();



        assert gpx_files_list != null; //mi assicuro che la cartella dei file gpx sia piena almeno, altrimenti genera un'eccezione
        for (File gpxFile : gpx_files_list) {

            System.out.println(gpxFile);

            String outFile = gpxFile.getAbsolutePath() + ".res.gpx";


           // new GTFSOSMWaysMatch().initMatch(Files.readString(gpxFile.toPath()), false);

            System.out.println("\tExport results to:" + outFile);
        }

        //GTFSOSMWaysMatch(null);
        return null;
    }


}
