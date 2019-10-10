package net.leberfinger.osm.nominatim;

import java.io.IOException;
import java.util.Optional;

public interface IAdminResolver {

	public Optional<AdminPlace> resolve(double lat, double lon) throws IOException;
	public String getStatistics();
	
}
