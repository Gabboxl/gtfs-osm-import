package it.osm.gtfs.utils;

public class JOSMUtils {
	public static String getJOSMRemoteControlRelationLink(String osmID){
		return "http://localhost:8111/import?url=http://api.openstreetmap.org/api/0.6/relation/" + osmID + "/full";
	}
}
