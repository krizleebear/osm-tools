package net.leberfinger.overture;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.collections.impl.factory.Maps;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DivisionIndex {

	private final Map<UUID, Division> divisions = Maps.mutable.empty();

	public void importGeoJSONStream(Reader r) {
		try (BufferedReader br = new BufferedReader(r)) {
			JsonParser parser = new JsonParser();
			String line = null;

			while ((line = br.readLine()) != null) {
				JsonObject json = parser.parse(line).getAsJsonObject();
				
				Division division = new Division(json);
				divisions.put(division.getID(), division);
			}

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	public void exportGeoJSONStream(Writer w) {
		divisions.forEach((id, division) -> {
			try {
				division.write(w);
				w.write("\n");
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}

	/**
	 * Identifies divisions with missing bidirectional links in their hierarchical
	 * relationships.
	 *
	 * @return a map where each key is a UUID that needs to be replaced (the wrong
	 *         ID) and each value is the Division that should be used instead (the
	 *         correct division)
	 */
	public Map<UUID, Division> findMissingLinks() {
		Map<UUID, Division> fixedLinks = Maps.mutable.empty();
		
		divisions.forEach((currentID, currentDivision) -> {
			Set<UUID> relatedIDs = currentDivision.getHierarchy();
			String currentName = currentDivision.getName().orElse("");
			String divisionClass = currentDivision.getDivisionClass().orElse("");

			// we're only interested in locality/hamlet/city divisions with the same name
			if ("".equals(divisionClass) || "".equals(currentName)) {
				return;
			}

			relatedIDs.forEach(relatedID -> {
				Division relatedDivision = divisions.get(relatedID);
				if (relatedDivision != null) {

					// find the related division with the same name as the current division
					String relatedName = relatedDivision.getName().orElse("");
					if (relatedName.equals(currentName)) {
						if (!relatedDivision.hasLinkTo(currentID)) {
							// we found a missing back link between the current division and the related
							// division. This must be fixed so that division_area will finally be linked to the correct division information
							fixedLinks.put(relatedID, currentDivision);
							JsonObject relatedProps = relatedDivision.getProperties();
							relatedProps.addProperty("originalID", relatedID.toString());
						}
					}
				}
			});
		});
		
		return fixedLinks;
	}
	
	/**
	 * Repairs missing bidirectional links between related divisions that share the
	 * same name.
	 * <p>
	 * This method iterates through all divisions and checks their hierarchical
	 * relationships. When it finds a division that references another division with
	 * the same name, but the referenced division doesn't have a back-link to the
	 * original division, it creates a fixed link by replacing the referenced
	 * division with the current division. This ensures that division_area entities
	 * are properly linked to their corresponding division information.
	 * <p>
	 * The method also adds an "originalID" property to the properties of divisions
	 * that have been replaced, preserving the original UUID for reference.
	 * <p>
	 * Only divisions with non-empty names and division classes are processed.
	 */
	public void repairMissingLinks() {
		Map<UUID, Division> fixedLinks = findMissingLinks();
		
		fixedLinks.forEach((id, fixedDivision) -> {
			divisions.put(id, fixedDivision);
		});
	}

	public Division get(String divisionID) {
		return divisions.get(UUID.fromString(divisionID));
	}
}
