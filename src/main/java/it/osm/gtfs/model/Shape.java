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
package it.osm.gtfs.model;

import java.util.Map;
import java.util.TreeMap;

public class Shape {

	private String id;
	private Map<Long, ShapePoint> points;
	
	public Shape(String id) {
		super();
		this.id = id;
		points = new TreeMap<Long, Shape.ShapePoint>();
	}

	public void pushPoint(Long seq, Double lat, Double lon){
		points.put(seq, new ShapePoint(seq, lat, lon));
	}

	public String getId() {
		return id;
	}
	
	public String getGPX(String desc){
		StringBuffer buffer = new StringBuffer();
		buffer.append("<?xml version=\"1.0\"?><gpx version=\"1.0\" creator=\"GTFS-import\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.topografix.com/GPX/1/0\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\">");
		for (Long p:points.keySet()){
			buffer.append("<wpt lat=\"");
			buffer.append(points.get(p).getLat());
			buffer.append("\" lon=\"");
			buffer.append(points.get(p).getLon());
			buffer.append("\"><name>");
			buffer.append(desc);
			buffer.append("</name><desc><![CDATA[");
			buffer.append(desc);
			buffer.append("]]></desc></wpt>");
		}
		buffer.append("</gpx>");
		return buffer.toString();
	}

	public class ShapePoint {
		private Long seq;
		private Double lat;
		private Double lon;
		
		public ShapePoint(Long seq, Double lat, Double lon) {
			super();
			this.seq = seq;
			this.lat = lat;
			this.lon = lon;
		}
		
		public Long getSeq() {
			return seq;
		}
		public Double getLat() {
			return lat;
		}
		public Double getLon() {
			return lon;
		}
	}
}

