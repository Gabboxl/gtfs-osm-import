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
import it.osm.gtfs.input.OSMParser;
import it.osm.gtfs.model.BoundingBox;
import it.osm.gtfs.model.Stop;
import it.osm.gtfs.model.Stop.GTFSStop;
import it.osm.gtfs.output.OSMBusImportGenerator;
import it.osm.gtfs.utils.GTFSImportSetting;
import it.osm.gtfs.utils.OSMDistanceUtils;
import it.osm.gtfs.utils.OSMXMLUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;


public class GTFSGenerateBusStopsImport {
	public static void run(boolean smallFileExport) throws IOException, ParserConfigurationException, SAXException, TransformerException {
		List<GTFSStop> gtfs = GTFSParser.readBusStop(GTFSImportSetting.getInstance().getGTFSPath() + GTFSImportSetting.GTFS_STOP_FILE_NAME);
		BoundingBox bb = new BoundingBox(gtfs);

		List<Stop> osms = OSMParser.readOSMStops(GTFSImportSetting.getInstance().getOSMPath() + GTFSImportSetting.OSM_STOP_FILE_NAME);

		for (GTFSStop gs:gtfs){
			for (Stop os:osms){
				if (gs.seams(os)){
					if (os.isStopPosition()){
						if (os.pairedWith != null){
							System.err.println("Mupliple paring found.");
							System.err.println(" OSM: " + os);
							System.err.println("GTFS: " + gs);
							System.err.println(" OSM: " + gs.pairedWithRailWay);
							System.err.println("GTFS: " + os.pairedWith);
							throw new IllegalArgumentException("Multiple paring found, this is currently unsupported.");
						}
						gs.pairedWithStopPositions.add(os);
						os.pairedWith = gs;
					}else if (os.isRailway()){
						if (gs.pairedWithRailWay != null || os.pairedWith != null){
							System.err.println("Mupliple paring found.");
							System.err.println(" OSM: " + os);
							System.err.println("GTFS: " + gs);
							System.err.println(" OSM: " + gs.pairedWithRailWay);
							System.err.println("GTFS: " + os.pairedWith);
							throw new IllegalArgumentException("Multiple paring found, this is currently unsupported.");
						}
						gs.pairedWithRailWay = os;
						os.pairedWith = gs;
					}else{
						if (gs.pairedWith != null || os.pairedWith != null){
							System.err.println("Mupliple paring found.");
							System.err.println(" OSM: " + os);
							System.err.println("GTFS: " + gs);
							System.err.println(" OSM: " + gs.pairedWith);
							System.err.println("GTFS: " + os.pairedWith);
							throw new IllegalArgumentException("Multiple paring found, this is currently unsupported.");
						}
						gs.pairedWith = os;
						os.pairedWith = gs;

					}
				}
			}
		}

		//Paired with gtfs_id (also checking stops no longer in GTFS)
		{
			//FIXME: check all tag present
			int paired_with_gtfs_id = 0;
			int osm_with_gtfs_id_not_in_gtfs = 0;
			int osm_with_different_gtfs_id = 0;
			OSMBusImportGenerator bufferNotInGTFS = new OSMBusImportGenerator(bb);
			OSMBusImportGenerator bufferDifferentGTFS = new OSMBusImportGenerator(bb);
			Map<Double, String> messages = new TreeMap<Double, String>();
			for (Stop os:osms){
				if (os.pairedWith != null && os.getGtfsId() != null){
					paired_with_gtfs_id++;
					Double dist = OSMDistanceUtils.distVincenty(os.getLat(), os.getLon(), os.pairedWith.getLat(), os.pairedWith.getLon());
					if (dist > 5){
						messages.put(dist, "Stop ref " + os.getCode() +
								" discance GTFS-OSM: " + OSMDistanceUtils.distVincenty(os.getLat(), os.getLon(), os.pairedWith.getLat(), os.pairedWith.getLon()) + " m");
					}
					if (!os.pairedWith.getGtfsId().equals(os.getGtfsId())){
						osm_with_different_gtfs_id++;
						Element n = (Element) os.originalXMLNode;
						OSMXMLUtils.addTagOrReplace(n, "gtfs_id", os.pairedWith.getGtfsId());
						bufferDifferentGTFS.appendNode(n);
						System.out.println("OSM Stop id " + os.getOSMId() +  " has gtfs_id: " + os.getGtfsId() + " but in GTFS has gtfs_id: " + os.pairedWith.getGtfsId());
					}
				}else if (os.getGtfsId() != null){
					osm_with_gtfs_id_not_in_gtfs++;
					System.out.println("OSM Stop id " + os.getOSMId() +  " has gtfs_id: " + os.getGtfsId() + " but is no longer in GTFS.");	
					Element n = (Element) os.originalXMLNode;
					bufferNotInGTFS.appendNode(n);
				}
			}
			for(String msg:messages.values())
				System.out.println(msg);

			if (osm_with_different_gtfs_id > 0){
				bufferDifferentGTFS.end();
				bufferDifferentGTFS.saveTo(new FileOutputStream(GTFSImportSetting.getInstance().getOutputPath() + GTFSImportSetting.OUTPUT_PAIRED_WITH_DIFFERENT_GTFS));
				System.out.println("OSM stops with different gtfs_id: " + osm_with_different_gtfs_id + " (created josm osm change file to review data: " + GTFSImportSetting.OUTPUT_PAIRED_WITH_DIFFERENT_GTFS + ")");
			}
			if (osm_with_gtfs_id_not_in_gtfs > 0){
				bufferNotInGTFS.end();
				bufferNotInGTFS.saveTo(new FileOutputStream(GTFSImportSetting.getInstance().getOutputPath() + GTFSImportSetting.OUTPUT_OSM_WITH_GTFSID_NOT_IN_GTFS));
				System.out.println("OSM stops with gtfs_id not found in GTFS: " + osm_with_gtfs_id_not_in_gtfs + " (created josm osm change file to review data: " + GTFSImportSetting.OUTPUT_OSM_WITH_GTFSID_NOT_IN_GTFS + ")");
			}
			System.out.println("Paired with gtfs_id: " + paired_with_gtfs_id);
		}

		//Paired without gtfs_id
		{
			//FIXME: check all tag present
			int paired_without_gtfs_id = 0;
			OSMBusImportGenerator buffer = new OSMBusImportGenerator(bb);
			for (Stop os:osms){
				if (os.pairedWith != null && os.getGtfsId() == null){
					Element n = (Element) os.originalXMLNode;
					OSMXMLUtils.addTag(n, "gtfs_id", os.pairedWith.getGtfsId());
					OSMXMLUtils.addTagIfNotExisting(n, "operator", GTFSImportSetting.getInstance().getOperator());
					OSMXMLUtils.addTagIfNotExisting(n, GTFSImportSetting.getInstance().getRevisitedKey(), "no");
					OSMXMLUtils.addTagIfNotExisting(n, "shelter", "unknown");
					OSMXMLUtils.addTagIfNotExisting(n, "bench", "unknown");
					OSMXMLUtils.addTagIfNotExisting(n, "tactile_paving", "unknown");
					OSMXMLUtils.addTagIfNotExisting(n, "name", GTFSImportSetting.getInstance().getPlugin().fixBusStopName(os.pairedWith.getName()));

					buffer.appendNode(n);

					paired_without_gtfs_id++;
				}
			}
			if (paired_without_gtfs_id > 0){
				buffer.end();
				buffer.saveTo(new FileOutputStream(GTFSImportSetting.getInstance().getOutputPath() + GTFSImportSetting.OUTPUT_PAIRED_WITHOUT_GTFS));
				System.out.println("Paired without gtfs_id: " + paired_without_gtfs_id + " (created josm osm change file to import data: " + GTFSImportSetting.OUTPUT_PAIRED_WITHOUT_GTFS + ")");
			}else{
				System.out.println("Paired without gtfs_id: " + paired_without_gtfs_id);
			}
		}

		//new in gtfs
		{
			int unpaired_in_gtfs = 0;
			int current_part = 0;
			OSMBusImportGenerator buffer = new OSMBusImportGenerator(bb);

			for (GTFSStop gs:gtfs){
				if (gs.pairedWith == null && gs.pairedWithRailWay == null && gs.pairedWithStopPositions.size() == 0){
					unpaired_in_gtfs++;
					buffer.appendNode(gs.getNewXMLNode(buffer));
					if (smallFileExport && unpaired_in_gtfs % 10 == 0){
						buffer.end();
						buffer.saveTo(new FileOutputStream(GTFSImportSetting.getInstance().getOutputPath() + GTFSImportSetting.OUTPUT_UNPAIRED_IN_GTFS + "."+ (current_part++) + ".osm"));
						buffer = new OSMBusImportGenerator(bb);
					}
				}
			}
			buffer.end();
			if (unpaired_in_gtfs > 0){
				buffer.saveTo(new FileOutputStream(GTFSImportSetting.getInstance().getOutputPath() + GTFSImportSetting.OUTPUT_UNPAIRED_IN_GTFS + "."+ (current_part++) + ".osm"));
				System.out.println("Unpaired in gtfs: " + unpaired_in_gtfs + " (created josm osm change file to import data: " + GTFSImportSetting.OUTPUT_UNPAIRED_IN_GTFS + "[.part].osm)");
			}else{
				System.out.println("Unpaired in gtfs: " + unpaired_in_gtfs);
			}
		}
	}
}
