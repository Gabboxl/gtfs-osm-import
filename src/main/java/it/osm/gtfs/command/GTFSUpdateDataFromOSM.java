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
import it.osm.gtfs.utils.DownloadUtils;
import it.osm.gtfs.utils.GTFSImportSetting;
import it.osm.gtfs.utils.OsmosisUtils;
import it.osm.gtfs.input.GTFSParser;
import it.osm.gtfs.model.BoundingBox;
import it.osm.gtfs.model.Relation;
import it.osm.gtfs.model.Stop;
import it.osm.gtfs.model.Stop.GTFSStop;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;

import javax.xml.parsers.ParserConfigurationException;

import org.openstreetmap.osmosis.core.pipeline.common.Pipeline;
import org.xml.sax.SAXException;
import picocli.CommandLine;

@CommandLine.Command(name = "update", description = "Generate/update osm data from api server")
public class GTFSUpdateDataFromOSM implements Callable<Void> {

    @CommandLine.Option(names = {"-r", "--relation"}, description = "Optional relation ID to generate/update single relation from api server")
    String relation;

    @Override
    public Void call() throws IOException, InterruptedException, ParserConfigurationException, SAXException {

        if (relation == null || relation.isBlank()) {

            File cachedirectory = new File(GTFSImportSetting.getInstance().getOSMCachePath());

            if (cachedirectory.mkdirs() || cachedirectory.isDirectory()) { //controllo che sia stata creata la directori o se esiste gia'
                updateBusStops();
                updateBaseRels();
                updateFullRels();
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
        List<GTFSStop> gtfs = GTFSParser.readBusStop(GTFSImportSetting.getInstance().getGTFSPath() + GTFSImportSetting.GTFS_STOP_FILE_NAME);
        BoundingBox bb = new BoundingBox(gtfs);

        String urlbus = GTFSImportSetting.OSM_OVERPASS_API_SERVER + "data=[bbox];node[highway=bus_stop];out meta;&bbox=" + bb.getAPIQuery();
        File filebus = new File(GTFSImportSetting.getInstance().getOSMCachePath() + "tmp_nbus.osm");
        urlbus = urlbus.replace(" ", "%20"); //fixo la richiesta sostituendo gli spazi con la codifica uri visto che la richeista è buggata con httpurlconnection e non va
        DownloadUtils.download(urlbus, filebus);

        Thread.sleep(1000L);

        String urlstop = GTFSImportSetting.OSM_OVERPASS_API_SERVER + "data=[bbox];node[public_transport=stop_position];out meta;&bbox=" + bb.getAPIQuery();
        File filestop = new File(GTFSImportSetting.getInstance().getOSMCachePath() + "tmp_nstop.osm");
        urlstop = urlstop.replace(" ", "%20");
        DownloadUtils.download(urlstop, filestop);

        Thread.sleep(1000L);

        String urltrm = GTFSImportSetting.OSM_OVERPASS_API_SERVER + "data=[bbox];node[railway=tram_stop];out meta;&bbox=" + bb.getAPIQuery();
        File filetrm = new File(GTFSImportSetting.getInstance().getOSMCachePath() + "tmp_ntram.osm");
        urltrm = urltrm.replace(" ", "%20");
        DownloadUtils.download(urltrm, filetrm);

        Thread.sleep(3000L);

        String urlmtr = GTFSImportSetting.OSM_OVERPASS_API_SERVER + "data=[bbox];node[railway=station];out meta;&bbox=" + bb.getAPIQuery();
        File filemtr = new File(GTFSImportSetting.getInstance().getOSMCachePath() + "tmp_nmetro.osm");
        urlmtr = urlmtr.replace(" ", "%20");
        DownloadUtils.download(urlmtr, filemtr);

        List<File> input = new ArrayList<File>();
        input.add(filebus);
        input.add(filestop);
        input.add(filetrm);
        input.add(filemtr);

        File fileout = new File(GTFSImportSetting.getInstance().getOSMPath() + GTFSImportSetting.OSM_STOP_FILE_NAME);
        OsmosisUtils.checkProcessOutput(OsmosisUtils.runOsmosisMerge(input, fileout));
    }

    private static void updateBaseRels() throws MalformedURLException, IOException{
        String urlrel = GTFSImportSetting.OSM_OVERPASS_API_SERVER + "data=relation[network=" + GTFSImportSetting.getInstance().getNetwork() +  "];out meta;";
        File filerel = new File(GTFSImportSetting.getInstance().getOSMCachePath() + "tmp_rels.osm");
        DownloadUtils.download(urlrel, filerel);
    }

    private static void updateFullRels() throws ParserConfigurationException, SAXException, IOException, InterruptedException{
        List<Stop> osmStops = OSMParser.readOSMStops(GTFSImportSetting.getInstance().getOSMPath() +  GTFSImportSetting.OSM_STOP_FILE_NAME);
        Map<String, Stop> osmstopsOsmID = OSMParser.applyOSMIndex(osmStops);

        List<Relation> osmRels = OSMParser.readOSMRelations(new File(GTFSImportSetting.getInstance().getOSMCachePath() +  "tmp_rels.osm"), osmstopsOsmID);

        Map<String, Integer> idWithVersion = new HashMap<String, Integer>();
        for (Relation r:osmRels){
            idWithVersion.put(r.getId(), r.getVersion());
        }

        updateFullRels(idWithVersion);
    }

    private static void updateFullRels(Map<String, Integer> idWithVersion) throws ParserConfigurationException, SAXException, IOException, InterruptedException{
        List<Stop> osmStops = OSMParser.readOSMStops(GTFSImportSetting.getInstance().getOSMPath() +  GTFSImportSetting.OSM_STOP_FILE_NAME);
        Map<String, Stop> osmstopsOsmID = OSMParser.applyOSMIndex(osmStops);

        List<File> sorted = new ArrayList<File>();

        // Default to all available rel, then override forced updates
        List<Relation> osmRels = OSMParser.readOSMRelations(new File(GTFSImportSetting.getInstance().getOSMCachePath() +  "tmp_rels.osm"), osmstopsOsmID);
        for (Relation r:osmRels){
            File filesorted = new File(GTFSImportSetting.getInstance().getOSMCachePath() + "tmp_s" + r.getId() + ".osm");
            if (filesorted.exists())
                sorted.add(filesorted);
        }

        Pipeline previousTask = null;
        for (String relationId:idWithVersion.keySet()){
            System.out.println("Processing relation " + relationId);
            File filesorted = new File(GTFSImportSetting.getInstance().getOSMCachePath() + "tmp_s" + relationId + ".osm");
            sorted.add(filesorted);

            boolean uptodate = false;
            try{
                if (filesorted.exists()){
                    List<Relation> relationInFile = OSMParser.readOSMRelations(filesorted, osmstopsOsmID);
                    if (relationInFile.size() > 0 && relationInFile.get(0).getVersion().equals(idWithVersion.get(relationId))) //si usa equals per le instanze Integer diverse come in questo caso per questo motivo: https://stackoverflow.com/a/4428779/9008381
                        uptodate = true;
                }
            }catch (Exception e) {
                System.err.println(e);
            }

            if (!uptodate){
                File filerelation = new File(GTFSImportSetting.getInstance().getOSMCachePath() + "tmp_r" + relationId + ".osm");
                String url = GTFSImportSetting.OSM_API_SERVER + "relation/" + relationId + "/full";
                DownloadUtils.download(url, filerelation);

                Pipeline current = OsmosisUtils.runOsmosisSort(filerelation, filesorted);
                OsmosisUtils.checkProcessOutput(previousTask);
                previousTask = current;
            }
        }
        OsmosisUtils.checkProcessOutput(previousTask);

        File filestops = new File(GTFSImportSetting.getInstance().getOSMPath() + GTFSImportSetting.OSM_STOP_FILE_NAME);
        File fileout = new File(GTFSImportSetting.getInstance().getOSMPath() + GTFSImportSetting.OSM_RELATIONS_FILE_NAME);
        sorted.add(filestops);

        OsmosisUtils.checkProcessOutput(OsmosisUtils.runOsmosisMerge(sorted, fileout));
    }
}
