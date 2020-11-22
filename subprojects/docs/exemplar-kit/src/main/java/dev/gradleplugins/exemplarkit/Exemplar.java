package dev.gradleplugins.exemplarkit;

import java.io.File;
import java.util.*;

import static java.util.Objects.requireNonNull;

public final class Exemplar {
	private final Sample sample;
	private final List<Step> steps;

	private Exemplar(Sample sample, List<Step> steps) {
		this.sample = sample;
		this.steps = steps;
	}

	public Sample getSample() {
		return sample;
	}

	public List<Step> getSteps() {
		return Collections.unmodifiableList(steps);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private Sample sample = Sample.empty();
		private final List<Step> steps = new ArrayList<>();

		public Builder fromArchive(File archiveFile) {
			this.sample = Sample.fromArchive(archiveFile);
			return this;
		}

		public Builder fromDirectory(File sampleDirectory) {
			this.sample = Sample.fromDirectory(sampleDirectory);
			return this;
		}

		public Builder step(Step step) {
			steps.add(requireNonNull(step, "Step cannot be null."));
			return this;
		}

		public Builder step(Step.Builder stepBuilder) {
			steps.add(requireNonNull(stepBuilder, "Step builder cannot be null.").build());
			return this;
		}

		public Exemplar build() {
			return new Exemplar(sample, steps);
		}
	}
}
