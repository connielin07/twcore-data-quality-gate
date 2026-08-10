package com.twlab.qualitygate.validation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;

final class BundleReferenceIndex {

	private BundleReferenceIndex() {
	}

	static boolean isExternalReference(String reference) {
		return reference != null && (reference.startsWith("http://") || reference.startsWith("https://"));
	}

	static String idOrUnknown(Resource resource) {
		String id = resource.getIdElement().getIdPart();
		return id == null || id.isBlank() ? "UNKNOWN" : id;
	}

	static Set<String> referencesTo(Bundle bundle, Class<? extends Resource> resourceType, String resourceName) {
		Set<String> references = new HashSet<>();
		for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
			Resource resource = entry.getResource();
			if (resourceType.isInstance(resource)) {
				addLogicalReference(references, resource, resourceName);
				addFullUrlReference(references, entry);
			}
		}
		return references;
	}

	static Map<String, Observation> observationsByReference(Bundle bundle) {
		Map<String, Observation> observationsByReference = new HashMap<>();
		for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
			Resource resource = entry.getResource();
			if (resource instanceof Observation observation) {
				String id = observation.getIdElement().getIdPart();
				if (id != null && !id.isBlank()) {
					observationsByReference.put("Observation/" + id, observation);
				}
				String fullUrl = entry.getFullUrl();
				if (fullUrl != null && !fullUrl.isBlank()) {
					observationsByReference.put(fullUrl, observation);
				}
			}
		}
		return observationsByReference;
	}

	static Map<String, String> patientReferenceAliases(Bundle bundle) {
		Map<String, String> patientReferenceAliases = new HashMap<>();
		for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
			Resource resource = entry.getResource();
			if (resource instanceof Patient) {
				String id = resource.getIdElement().getIdPart();
				String canonical = id == null || id.isBlank() ? entry.getFullUrl() : "Patient/" + id;
				if (canonical == null || canonical.isBlank()) {
					continue;
				}
				patientReferenceAliases.put(canonical, canonical);
				String fullUrl = entry.getFullUrl();
				if (fullUrl != null && !fullUrl.isBlank()) {
					patientReferenceAliases.put(fullUrl, canonical);
				}
			}
		}
		return patientReferenceAliases;
	}

	private static void addLogicalReference(Set<String> references, Resource resource, String resourceName) {
		String id = resource.getIdElement().getIdPart();
		if (id != null && !id.isBlank()) {
			references.add(resourceName + "/" + id);
		}
	}

	private static void addFullUrlReference(Set<String> references, Bundle.BundleEntryComponent entry) {
		String fullUrl = entry.getFullUrl();
		if (fullUrl != null && !fullUrl.isBlank()) {
			references.add(fullUrl);
		}
	}
}
