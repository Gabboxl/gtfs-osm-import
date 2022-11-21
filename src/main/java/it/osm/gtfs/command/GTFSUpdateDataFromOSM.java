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

import javax.xml.parsers.ParserConfigurationException;

import org.openstreetmap.osmosis.core.pipeline.common.Pipeline;
import org.xml.sax.SAXException;

public class GTFSUpdateDataFromOSM {
	public static void run() throws IOException, InterruptedException, ParserConfigurationException, SAXException {
		new File(GTFSImportSetting.getInstance().getOSMCachePath()).mkdirs();
		updateBusStops();
		updateBaseRels();
		updateFullRels();
	}

	public static void run(String relation) throws ParserConfigurationException, SAXException, IOException, InterruptedException {
		StringTokenizer st = new StringTokenizer(relation, " ,\n\t");
		Map<String, Integer> idWithVersion = new HashMap<String, Integer>();
		while (st.hasMoreTokens()){
			idWithVersion.put(st.nextToken(), Integer.MAX_VALUE);
		}
		
		updateFullRels(idWithVersion);
	}

	private static void updateBusStops() throws IOException, InterruptedException{
		List<GTFSStop> gtfs = GTFSParser.readBusStop(GTFSImportSetting.getInstance().getGTFSPath() + GTFSImportSetting.GTFS_STOP_FILE_NAME);
		BoundingBox bb = new BoundingBox(gtfs);

		String urlbus = GTFSImportSetting.OSM_OVERPASS_XAPI_SERVER + "node" + bb.getXAPIQuery() + "[highway=bus_stop][@meta]";
		File filebus = new File(GTFSImportSetting.getInstance().getOSMCachePath() + "tmp_nbus.osm");
		DownloadUtils.downlod(urlbus, filebus);

		Thread.sleep(3000L);
		
		String urlstop = GTFSImportSetting.OSM_OVERPASS_XAPI_SERVER + "node" + bb.getXAPIQuery() + "[public_transport=stop_position][@meta]";
		File filestop = new File(GTFSImportSetting.getInstance().getOSMCachePath() + "tmp_nstop.osm");
		DownloadUtils.downlod(urlstop, filestop);

		Thread.sleep(20000L);
		
		String urltrm = GTFSImportSetting.OSM_OVERPASS_XAPI_SERVER + "node" + bb.getXAPIQuery() + "[railway=tram_stop][@meta]";
		File filetrm = new File(GTFSImportSetting.getInstance().getOSMCachePath() + "tmp_ntram.osm");
		DownloadUtils.downlod(urltrm, filetrm);

		Thread.sleep(15000L);
		
		String urlmtr = GTFSImportSetting.OSM_OVERPASS_XAPI_SERVER + "node" + bb.getXAPIQuery() + "[railway=station][@meta]";
		File filemtr = new File(GTFSImportSetting.getInstance().getOSMCachePath() + "tmp_nmetro.osm");
		DownloadUtils.downlod(urlmtr, filemtr);

		List<File> input = new ArrayList<File>();
		input.add(filebus);
		input.add(filestop);
		input.add(filetrm);
		input.add(filemtr);

		File fileout = new File(GTFSImportSetting.getInstance().getOSMPath() + GTFSImportSetting.OSM_STOP_FILE_NAME);
		OsmosisUtils.checkProcessOutput(OsmosisUtils.runOsmosisMerge(input, fileout));
	}

	private static void updateBaseRels() throws MalformedURLException, IOException{
		String urlrel = GTFSImportSetting.OSM_OVERPASS_XAPI_SERVER + "relation[network=" + GTFSImportSetting.getInstance().getNetwork() +  "][@meta]";
		File filerel = new File(GTFSImportSetting.getInstance().getOSMCachePath() + "tmp_rels.osm");
		DownloadUtils.downlod(urlrel, filerel);
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
					if (relationInFile.size() > 0 && relationInFile.get(0).getVersion() == idWithVersion.get(relationId))
						uptodate = true;
				}
			}catch (Exception e) {
			}
			
			if (!uptodate){
				File filerelation = new File(GTFSImportSetting.getInstance().getOSMCachePath() + "tmp_r" + relationId + ".osm");
				String url = GTFSImportSetting.OSM_API_SERVER + "relation/" + relationId + "/full";
				DownloadUtils.downlod(url, filerelation);
				
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
