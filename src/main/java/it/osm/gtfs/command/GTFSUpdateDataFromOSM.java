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

import it.osm.gtfs.input.OSMParser;
import it.osm.gtfs.model.*;
import it.osm.gtfs.utils.DownloadUtils;
import it.osm.gtfs.utils.GTFSImportSettings;
import it.osm.gtfs.utils.OsmosisUtils;
import it.osm.gtfs.input.GTFSParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;

import javax.xml.parsers.ParserConfigurationException;

import it.osm.gtfs.utils.StopsUtils;
import org.fusesource.jansi.Ansi;
import org.openstreetmap.osmosis.core.pipeline.common.Pipeline;
import org.xml.sax.SAXException;
import picocli.CommandLine;

import static org.fusesource.jansi.Ansi.ansi;

@CommandLine.Command(name = "update", description = "Generate/update data from OpenStreetMap")
public class GTFSUpdateDataFromOSM implements Callable<Void> {

    @CommandLine.Option(names = {"-r", "--relation"}, description = "Optional relation ID to generate/update single relation from api server")
    String relation;

    @Override
    public Void call() throws IOException, InterruptedException, ParserConfigurationException, SAXException {

        if (relation == null || relation.isBlank()) {

            File cachedirectory = new File(GTFSImportSettings.getInstance().getCachePath());
            File osmdatadirectory = new File(GTFSImportSettings.getInstance().getOsmDataPath());

            if ((cachedirectory.mkdirs() || cachedirectory.isDirectory()) && (osmdatadirectory.mkdirs() || osmdatadirectory.isDirectory())) { //controllo che sia stata creata la directori o se esiste gia'
                updateBusStops();
                updateBaseRels();
                updateFullRels();

                System.out.println(ansi().fg(Ansi.Color.GREEN).a("Data update complete. You can now generate the bus stops import.").reset());
            } else {
                System.err.println("Error during the creation of the cache directory for gtfs-osm-import.");
            }
        }else {
            StringTokenizer st = new StringTokenizer(relation, " ,\n\t");
            Map<String, Integer> idWithVersion = new HashMap<String, Integer>();
            while (st.hasMoreTokens()){
                idWithVersion.put(st.nextToken(), Integer.MAX_VALUE);
            }

            updateFullRels(idWithVersion);
        }
        return null;
    }

    private static void updateBusStops() throws IOException, InterruptedException{
        List<GTFSStop> gtfs = GTFSParser.readStops(GTFSImportSettings.getInstance().getGTFSPath() + GTFSImportSettings.GTFS_STOP_FILE_NAME);
        BoundingBox bb = new BoundingBox(gtfs);

        String urlbus = GTFSImportSettings.OSM_OVERPASS_API_SERVER + "data=[bbox];node[highway=bus_stop];out meta;&bbox=" + bb.getAPIQuery();
        File filebus = new File(GTFSImportSettings.getInstance().getCachePath() + "tmp_nbus.osm");
        urlbus = urlbus.replace(" ", "%20"); //we substitute spaced with the uri code as httpurlconnection doesn't do that automatically, and it makes the request fail
        DownloadUtils.download(urlbus, filebus, false);

        Thread.sleep(1000L);

        String urlstop = GTFSImportSettings.OSM_OVERPASS_API_SERVER + "data=[bbox];node[public_transport=stop_position];out meta;&bbox=" + bb.getAPIQuery();
        File filestop = new File(GTFSImportSettings.getInstance().getCachePath() + "tmp_nstop.osm");
        urlstop = urlstop.replace(" ", "%20");
        DownloadUtils.download(urlstop, filestop, false);

        Thread.sleep(1000L);

        String urltrm = GTFSImportSettings.OSM_OVERPASS_API_SERVER + "data=[bbox];node[railway=tram_stop];out meta;&bbox=" + bb.getAPIQuery();
        File filetrm = new File(GTFSImportSettings.getInstance().getCachePath() + "tmp_ntram.osm");
        urltrm = urltrm.replace(" ", "%20");
        DownloadUtils.download(urltrm, filetrm, false);

        Thread.sleep(2000L);

        String urlmtr = GTFSImportSettings.OSM_OVERPASS_API_SERVER + "data=[bbox];node[railway=station];out meta;&bbox=" + bb.getAPIQuery();
        File filemtr = new File(GTFSImportSettings.getInstance().getCachePath() + "tmp_nmetro.osm");
        urlmtr = urlmtr.replace(" ", "%20");
        DownloadUtils.download(urlmtr, filemtr, false);

        List<File> input = new ArrayList<File>();
        input.add(filebus);
        input.add(filestop);
        input.add(filetrm);
        input.add(filemtr);

        File fileout = new File(GTFSImportSettings.OSM_STOPS_FILE_PATH);
        OsmosisUtils.checkProcessOutput(OsmosisUtils.runOsmosisMerge(input, fileout));
    }

    private static void updateBaseRels() throws IOException{
        String urlrel = GTFSImportSettings.OSM_OVERPASS_API_SERVER + "data=relation[network=" + GTFSImportSettings.getInstance().getNetwork() +  "];out meta;";
        File filerel = new File(GTFSImportSettings.getInstance().getCachePath() + "tmp_rels.osm");
        DownloadUtils.download(urlrel, filerel, false);
    }

    private static void updateFullRels() throws ParserConfigurationException, SAXException, IOException, InterruptedException{

        //TODO: should we really read stops of ANY operator?
        List<OSMStop> osmStops = OSMParser.readOSMStops(GTFSImportSettings.OSM_STOPS_FILE_PATH, true);
        Map<String, OSMStop> osmstopsOsmID = StopsUtils.getOSMIdOSMStopMap(osmStops);

        List<Relation> osmRels = OSMParser.readOSMRelations(new File(GTFSImportSettings.getInstance().getCachePath() +  "tmp_rels.osm"), osmstopsOsmID);

        Map<String, Integer> idWithVersion = new HashMap<String, Integer>();
        for (Relation r:osmRels){
            idWithVersion.put(r.getId(), r.getVersion());
        }

        updateFullRels(idWithVersion);
    }

    private static void updateFullRels(Map<String, Integer> idWithVersion) throws ParserConfigurationException, SAXException, IOException, InterruptedException{
        List<OSMStop> osmStops = OSMParser.readOSMStops(GTFSImportSettings.OSM_STOPS_FILE_PATH, true);
        Map<String, OSMStop> osmstopsOsmID = StopsUtils.getOSMIdOSMStopMap(osmStops);

        List<File> sorted = new ArrayList<File>();

        // Default to all available rel, then override forced updates
        List<Relation> osmRels = OSMParser.readOSMRelations(new File(GTFSImportSettings.getInstance().getCachePath() +  "tmp_rels.osm"), osmstopsOsmID);
        for (Relation r:osmRels){
            File filesorted = new File(GTFSImportSettings.getInstance().getCachePath() + "tmp_s" + r.getId() + ".osm");
            if (filesorted.exists())
                sorted.add(filesorted);
        }

        Pipeline previousTask = null;
        for (String relationId:idWithVersion.keySet()){
            System.out.println("Processing relation " + relationId);
            File filesorted = new File(GTFSImportSettings.getInstance().getCachePath() + "tmp_s" + relationId + ".osm");
            sorted.add(filesorted);

            boolean uptodate = false;
            try{
                if (filesorted.exists()){
                    List<Relation> relationInFile = OSMParser.readOSMRelations(filesorted, osmstopsOsmID);
                    if (relationInFile.size() > 0 && relationInFile.get(0).getVersion().equals(idWithVersion.get(relationId))) //si usa equals per le instanze Integer diverse come in questo caso per questo motivo: https://stackoverflow.com/a/4428779/9008381
                        uptodate = true;
                }
            }catch (Exception e) {
                e.printStackTrace();
            }

            if (!uptodate){
                File filerelation = new File(GTFSImportSettings.getInstance().getCachePath() + "tmp_r" + relationId + ".osm");
                String url = GTFSImportSettings.OSM_API_SERVER + "relation/" + relationId + "/full";
                DownloadUtils.download(url, filerelation, false);

                Pipeline current = OsmosisUtils.runOsmosisSort(filerelation, filesorted);
                OsmosisUtils.checkProcessOutput(previousTask);
                previousTask = current;
            }
        }
        OsmosisUtils.checkProcessOutput(previousTask);

        File filestops = new File(GTFSImportSettings.OSM_STOPS_FILE_PATH);
        File fileout = new File(GTFSImportSettings.OSM_RELATIONS_FILE_PATH);
        sorted.add(filestops);

        OsmosisUtils.checkProcessOutput(OsmosisUtils.runOsmosisMerge(sorted, fileout));
    }
}
