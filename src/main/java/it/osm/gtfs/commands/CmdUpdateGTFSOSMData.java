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
import it.osm.gtfs.input.OSMParser;
import it.osm.gtfs.models.*;
import it.osm.gtfs.utils.*;
import org.apache.commons.httpclient.util.URIUtil;
import org.fusesource.jansi.Ansi;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import picocli.CommandLine;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.fusesource.jansi.Ansi.ansi;

@CommandLine.Command(name = "update", mixinStandardHelpOptions = true, description = "Generate/update data from OpenStreetMap")
public class CmdUpdateGTFSOSMData implements Callable<Void> {

    @Override
    public Void call() throws IOException, InterruptedException, ParserConfigurationException, SAXException, TransformerException {

            File cachedirectory = new File(GTFSImportSettings.getInstance().getCachePath());
            File osmdatadirectory = new File(GTFSImportSettings.getInstance().getOsmDataPath());

            cachedirectory.mkdirs();
            osmdatadirectory.mkdirs();

            updateGTFSData();
            updateBusStops();
            updateFullRels();

            System.out.println(ansi().fg(Ansi.Color.GREEN).a("GTFS and OSM data update completed.").reset());

        return null;
    }

    private static void updateGTFSData() {
        System.out.println(ansi().fg(Ansi.Color.YELLOW).a("Downloading and extracting GTFS data from " + GTFSImportSettings.getInstance().getGTFSZipUrl() + " ...").reset());
        DownloadUtils.downloadZip(GTFSImportSettings.getInstance().getGTFSZipUrl(), GTFSImportSettings.getInstance().getGTFSDataPath());
    }

    private static void updateBusStops() throws IOException, InterruptedException {

        List<GTFSStop> gtfsStops = GTFSParser.readStops(GTFSImportSettings.getInstance().getGTFSDataPath() + GTFSImportSettings.GTFS_STOP_FILE_NAME);
        BoundingBox bb = new BoundingBox(gtfsStops);

        String queryBusStopsUrl = "data=[bbox];node[highway=bus_stop];out meta;&bbox=" + bb.getAPIQuery();
        File busFileTemp = new File(GTFSImportSettings.getInstance().getCachePath() + "tmp_busstops.osm");
        URIUtil.encodeQuery(queryBusStopsUrl);

        String busStopsUrl = GTFSImportSettings.OSM_OVERPASS_API_SERVER + URIUtil.encodeQuery(queryBusStopsUrl);

        DownloadUtils.download(busStopsUrl, busFileTemp, false);

        Thread.sleep(1000L);

        String queryStopPositionsUrl = "data=[bbox];node[public_transport=stop_position];out meta;&bbox=" + bb.getAPIQuery();
        File stopPositionsFileTemp = new File(GTFSImportSettings.getInstance().getCachePath() + "tmp_stoppositions.osm");
        String stopPositionsUrl = GTFSImportSettings.OSM_OVERPASS_API_SERVER + URIUtil.encodeQuery(queryStopPositionsUrl);
        DownloadUtils.download(stopPositionsUrl, stopPositionsFileTemp, false);

        Thread.sleep(1000L);

        String queryTramStopsUrl = "data=[bbox];node[railway=tram_stop];out meta;&bbox=" + bb.getAPIQuery();
        File tramFileTemp = new File(GTFSImportSettings.getInstance().getCachePath() + "tmp_tramstops.osm");
        String tramStopsUrl = GTFSImportSettings.OSM_OVERPASS_API_SERVER + URIUtil.encodeQuery(queryTramStopsUrl);;
        DownloadUtils.download(tramStopsUrl, tramFileTemp, false);

        Thread.sleep(2000L);

        String queryMetroTrainStationsUrl = "data=[bbox];node[railway=station];out meta;&bbox=" + bb.getAPIQuery();
        File metroFileTemp = new File(GTFSImportSettings.getInstance().getCachePath() + "tmp_metrostops.osm");
        String metroTrainStationsUrl = GTFSImportSettings.OSM_OVERPASS_API_SERVER + URIUtil.encodeQuery(queryMetroTrainStationsUrl);
        DownloadUtils.download(metroTrainStationsUrl, metroFileTemp, false);

        String queryUrlstat = "data=[bbox];node[public_transport=station];out meta;&bbox=" + bb.getAPIQuery();
        File stationsFileTemp = new File(GTFSImportSettings.getInstance().getCachePath() + "tmp_stationstops.osm");
        String urlstat = GTFSImportSettings.OSM_OVERPASS_API_SERVER + URIUtil.encodeQuery(queryUrlstat);
        DownloadUtils.download(urlstat, stationsFileTemp, false);

        List<File> tempFileList = new ArrayList<>();
        tempFileList.add(busFileTemp);
        tempFileList.add(stopPositionsFileTemp);
        tempFileList.add(tramFileTemp);
        tempFileList.add(metroFileTemp);
        tempFileList.add(stationsFileTemp);

        File finalMergedFileOut = new File(GTFSImportSettings.getInstance().getOsmStopsFilePath());

        OsmosisUtils.checkProcessOutput(OsmosisUtils.runOsmosisMerge(tempFileList, finalMergedFileOut));
    }

    //todo: we should cleanup the cache relations files before every update i think
    private static void updateFullRels() throws ParserConfigurationException, SAXException, IOException, TransformerException {

        //we download the relations data
        String queryRel = "data=(relation[network=" + GTFSImportSettings.getInstance().getNetwork() +  "];>;);out meta;";
        String urlrel = GTFSImportSettings.OSM_OVERPASS_API_SERVER + URIUtil.encodeQuery(queryRel);;

        File uncheckedRelsFile = new File(GTFSImportSettings.getInstance().getCachePath() + "tmp_unchecked_rels.osm");
        DownloadUtils.download(urlrel, uncheckedRelsFile, false);



        //TODO: should we really read stops of ANY operator?
        List<OSMStop> osmStops = OSMParser.readOSMStops(GTFSImportSettings.getInstance().getOsmStopsFilePath(), true);
        Map<String, OSMStop> osmIdOSMStopMap = StopsUtils.getOSMIdOSMStopMap(osmStops);

        // Default to all available rel, then override forced updates
        ReadOSMRelationsResult readRelsResult = OSMParser.readOSMRelations(new File(GTFSImportSettings.getInstance().getCachePath() +  "tmp_unchecked_rels.osm"), osmIdOSMStopMap, SharedCliOptions.checkStopsOfAnyOperatorTagValue);

        
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(uncheckedRelsFile);
        doc.getDocumentElement().normalize();

        NodeList relationElementList = doc.getElementsByTagName("relation");

        for(Relation failedRelation : readRelsResult.getFailedRelations()) {

            for (int s = 0; s < relationElementList.getLength(); s++) {
                Node node = relationElementList.item(s);

                if (node.getAttributes().getNamedItem("id").getNodeValue().equals(failedRelation.getId())) {
                    doc.getDocumentElement().removeChild(node);
                }
            }

        }

        String tmpCheckedRelsPath = GTFSImportSettings.getInstance().getCachePath() + "tmp_checked_rels.osm";

        OutputStream stream = new FileOutputStream(tmpCheckedRelsPath);
        Source source = new DOMSource(doc);
        Result result = new StreamResult(stream);
        Transformer xformer = TransformerFactory.newInstance().newTransformer();
        xformer.transform(source, result);

        //we close the file stream
        stream.close();


        File checkedRelsFile = new File(tmpCheckedRelsPath);
        File filteredRelsFile = new File(GTFSImportSettings.getInstance().getCachePath() + "tmp_filteredchecked_rels.osm");
        OsmosisUtils.checkProcessOutput(OsmosisUtils.runOsmosisUnusedWaysNodes(checkedRelsFile, filteredRelsFile));


        File stopsFile = new File(GTFSImportSettings.getInstance().getOsmStopsFilePath());

        List<File> sortedfilestest = new ArrayList<>();
        sortedfilestest.add(filteredRelsFile);
        sortedfilestest.add(stopsFile);

        //final relations file merge
        File testout = new File(GTFSImportSettings.getInstance().getOsmRelationsFilePath());
        OsmosisUtils.checkProcessOutput(OsmosisUtils.runOsmosisMerge(sortedfilestest, testout));

    }
}
