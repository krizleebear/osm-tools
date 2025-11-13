package net.leberfinger.overture;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Division {
	private final UUID id;
	private final JsonObject json;
	private final JsonObject properties;
	private final Set<UUID> hierarchyDivisionIDs;

	public Division(JsonObject divisionJSON) {
		this.json = divisionJSON;
		this.properties = divisionJSON.get("properties").getAsJsonObject();
		this.id = UUID.fromString(properties.get("id").getAsString());
		this.hierarchyDivisionIDs = extractRelatedIds(properties.get("hierarchies"));
	}

	private Set<UUID> extractRelatedIds(JsonElement hierarchies) {
		MutableSet<UUID> relatedIDs = Sets.mutable.empty();
		hierarchies.getAsJsonArray().forEach(hierarchy -> {
			hierarchy.getAsJsonArray().forEach(divisionJSON -> {
				String relatedID = divisionJSON.getAsJsonObject().get("division_id").getAsString();
				UUID relatedDivisionId = UUID.fromString(relatedID);
				relatedIDs.add(relatedDivisionId);
			});
		});
		return relatedIDs;
	}
	
	public UUID getID() {
		return this.id;
	}

	public JsonObject getProperties() {
		return properties;
	}

	public Set<UUID> getHierarchy() {
		return hierarchyDivisionIDs;
	}

	public void addLink(UUID missingLink) {
		hierarchyDivisionIDs.add(missingLink);
	}

	public boolean hasLinkTo(UUID otherID) {
		return hierarchyDivisionIDs.contains(otherID);
	}

	private Optional<String> getPropertyAsString(String key) {
		Optional<String> value = Optional.empty();
		JsonElement jsonElement = properties.get(key);
		if (jsonElement != null && !jsonElement.isJsonNull()) {
			value = Optional.ofNullable(jsonElement.getAsString());
		}
		return value;
	}

	public Optional<String> getName() {
		return getPropertyAsString("name");
	}

	public Optional<String> getDivisionClass() {
		return getPropertyAsString("class");
	}

	public Optional<String> getSubtype() {
		return getPropertyAsString("subtype");
	}

	@Override
	public String toString() {
		return "Division [id=" + id + ", getName()=" + getName() + ", getDivisionClass()=" + getDivisionClass()
				+ ", getSubtype()=" + getSubtype() + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Division other = (Division) obj;
		return Objects.equals(id, other.id);
	}

	public void write(Writer w) throws IOException {
		w.write(this.json.toString());
	}
}