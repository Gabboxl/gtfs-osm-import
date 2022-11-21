package it.osm.gtfs.model;

import java.util.ArrayList;
import java.util.List;

public class Relation extends StopsList{
	private String name;
	private Integer version;
	private String ref;
	private String from;
	private String to;
	private RelationType type;
	private List<OSMRelationWayMember> wayMembers = new ArrayList<Relation.OSMRelationWayMember>();

	public Relation(String id) {
		super(id);
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public RelationType getType() {
		return type;
	}

	public void setType(RelationType type) {
		this.type = type;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public List<OSMRelationWayMember> getWayMembers() {
		return wayMembers;
	}

	public void setWayMembers(List<OSMRelationWayMember> wayMembers) {
		this.wayMembers = wayMembers;
	}

	public static enum RelationType{
		SUBWAY(0), TRAM(1), BUS(2), TRAIN(3), LIGHT_RAIL(4); 

		private int dbId;
		private RelationType(int dbId){
			this.dbId = dbId;
		}

		public static RelationType parse(String nodeValue) {
			if (nodeValue != null){
				if (nodeValue.equalsIgnoreCase("bus"))
					return BUS;
				if (nodeValue.equalsIgnoreCase("tram"))
					return TRAM;
				if (nodeValue.equalsIgnoreCase("subway"))
					return SUBWAY;
				if (nodeValue.equalsIgnoreCase("train"))
					return TRAIN;
				if (nodeValue.equalsIgnoreCase("light_rail"))
					return LIGHT_RAIL;
			}
			throw new IllegalArgumentException("unsupported relation type: " + nodeValue);
		}

		public int dbId() {
			return dbId;
		}

	}

	public static class OSMRelationWayMember{
		public OSMWay way;
		public Boolean backward;
	}

	public static class OSMWay {
		private long id;
		public List<OSMNode> nodes = new ArrayList<Relation.OSMNode>();
		public boolean oneway = false;

		public OSMWay(long id){
			this.id = id;
		}

		public long getId() {
			return id;
		}
	}

	public static class OSMNode {
		private Double lat;
		private Double lon;

		public OSMNode(Double lat, Double lon) {
			super();
			this.lat = lat;
			this.lon = lon;
		}

		public Double getLat() {
			return lat;
		}

		public Double getLon() {
			return lon;
		}
	}
}