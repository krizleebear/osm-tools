package net.leberfinger.osm;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.IOUtils;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import crosby.binary.osmosis.OsmosisReader;

/**
 * Export AdminCentres of administrative boundary relations from a given PBF
 * file.
 * </p>
 * Only relations with admin_level information are considered.
 */
public class AdminCentreExtract implements Sink {

	private long entityCount = 0;
	private long adminRelations = 0;
	private long adminRelationsWithAdminCentre = 0;
	
	private  static final String[] TSV_HEADERS = new String[] { "admin_id", "admin_level", "admin_name",
			"admincentre_type", "admincentre_id" };

	private static final Path OUTPUT_TSV = Paths.get("admincentres.tsv");
	private static final Path OUTPUT_JSON = Paths.get("admincentres.geojsonseq");

	private PrintWriter writer = null;

	@Override
	public void initialize(Map<String, Object> metaData) {
		try {
			writer = newJsonPrinter();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private PrintWriter newJsonPrinter() throws IOException {
		return new PrintWriter(Files.newBufferedWriter(OUTPUT_JSON));
	}

	@Override
	public void complete() {
		System.out.println("adminRelations: " + adminRelations);
		System.out.println("adminRelationsWithAdminCentre: " + adminRelationsWithAdminCentre);

		if (writer != null) {
			writer.flush();
		}
	}

	private CSVPrinter newTSVPrinter() throws IOException {
		Writer out = Files.newBufferedWriter(OUTPUT_TSV);
		return new CSVPrinter(out, CSVFormat.TDF.withHeader(TSV_HEADERS));
	}

	@Override
	public void close() {
		IOUtils.closeQuietly(writer);
	}

	@Override
	public void process(EntityContainer entityContainer) {
		entityCount++;
		if (entityCount % 1_000_000 == 0) {
			System.out.print(".");
		}

		if (entityContainer instanceof RelationContainer) {
			Relation relation = ((RelationContainer) entityContainer).getEntity();
			Optional<AdminWithCentre> adminWithCentre = tryExtractAdminCentre(relation);
			adminWithCentre.ifPresent(a -> a.printToJSONStream(writer));
		}
	}

	private Optional<AdminWithCentre> tryExtractAdminCentre(Relation relation) {
		Optional<Tag> adminLevelContainer = getTag(relation, "admin_level");
		if (adminLevelContainer.isPresent()) {
			adminRelations++;
			Relation adminRelation = relation; // renamed for better legibility

			Optional<RelationMember> adminCentreContainer = getAdminCentreMember(adminRelation);
			if (adminCentreContainer.isPresent()) {
				adminRelationsWithAdminCentre++;

				long adminID = adminRelation.getId();
				String adminLevel = getValueOrEmptyString(adminLevelContainer);
				String adminName = getValueOrEmptyString(adminRelation, "name");

				RelationMember adminCentre = adminCentreContainer.get();
				EntityType adminCentreType = adminCentre.getMemberType();
				long adminCentreID = adminCentre.getMemberId();
				AdminWithCentre adminWithCentre = new AdminWithCentre(adminID, adminLevel, adminName, adminCentreType,
						adminCentreID);

				return Optional.of(adminWithCentre);
			}
		}
		return Optional.empty();
	}

	private String getValueOrEmptyString(Entity entity, String tagName) {
		return getValueOrEmptyString(getTag(entity, tagName));
	}

	public static String getValueOrEmptyString(Optional<Tag> tag) {
		if (tag.isPresent()) {
			return tag.get().getValue();
		}
		return "";
	}

	public static boolean hasAdminLevel(Entity entity) {
		return hasTag(entity, "admin_level");
	}

	/**
	 * If available: Get the member of the relation that has the role "admin_center"
	 * or "label".
	 * 
	 * @param relation
	 * @return
	 */
	public static Optional<RelationMember> getAdminCentreMember(Relation relation) {
		List<RelationMember> relationMembers = relation.getMembers();

		Optional<RelationMember> label = Optional.empty();
		for (RelationMember member : relationMembers) {
			final String memberRole = member.getMemberRole();
			if ("admin_centre".equals(memberRole)) {
				return Optional.of(member);
			} else if ("label".equals(memberRole)) {
				label = Optional.of(member);
			}
		}
		return label;
	}

	private static Optional<Tag> getTag(Entity entity, final String tagName) {
		Collection<Tag> tags = entity.getTags();
		for (Tag myTag : tags) {
			if (tagName.equals(myTag.getKey())) {
				return Optional.of(myTag);
			}
		}
		return Optional.empty();
	}

	private static boolean hasTag(Entity entity, final String tagName) {
		Collection<Tag> tags = entity.getTags();
		for (Tag myTag : tags) {
			if (tagName.equals(myTag.getKey())) {
				return true;
			}
		}
		return false;
	}

	private static class AdminWithCentre {

		private final long adminID;
		private final String adminLevel;
		private final String adminName;
		private final String adminCentreType;
		private final long adminCentreID;

		public AdminWithCentre(long adminID, String adminLevel, String adminName, EntityType adminCentreType,
				long adminCentreID) {
			this.adminID = adminID;
			this.adminLevel = adminLevel;
			this.adminName = adminName;
			this.adminCentreType = adminCentreType.toString().toLowerCase();
			this.adminCentreID = adminCentreID;
		}

		public void printTo(CSVPrinter printer) {
			try {
				printer.printRecord(adminID, adminLevel, adminName, adminCentreType, adminCentreID);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public void printToJSONStream(PrintWriter pw) {
			pw.println(toJSON());
		}

		/**
		 * <pre>
		 {
		"type": "Feature",
		"@id": 12345,
		"@type": "node",
		"properties": {},
		"geometry": null
		}
		 * </pre>
		 * 
		 * @return
		 */
		public String toJSON() {
			JsonObject j = new JsonObject();
			j.addProperty("type", "Feature");
			j.add("geometry", JsonNull.INSTANCE);
			JsonObject properties = new JsonObject();
			j.add("properties", properties);

			properties.addProperty("@id", adminID);
			properties.addProperty("@type", "relation");
			properties.addProperty("admincentre_type", adminCentreType);
			properties.addProperty("admincentre_id", adminCentreID);

			return j.toString();
		}
	}

	public void extract(Path pbfFile) throws IOException {
		try (InputStream inputStream = Files.newInputStream(pbfFile)) {
			AdminCentreExtract detector = new AdminCentreExtract();

			OsmosisReader reader = new OsmosisReader(inputStream);
			reader.setSink(detector);
			reader.run();
		}
	}
}
