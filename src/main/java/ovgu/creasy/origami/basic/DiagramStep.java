package ovgu.creasy.origami.basic;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A single step that transforms one crease pattern into a slightly
 * different one using some folding technique, equivalent to an edge in the
 * Step sequence graph
 */
public class DiagramStep {
    public DiagramStep(CreasePattern from, CreasePattern to) {
        this.from = from;
        this.to = to;
        this.sourcePatterns = new LinkedHashSet<>();
    }

    public DiagramStep(CreasePattern from, CreasePattern to, String sourcePattern) {
        this(from, to);
        if (sourcePattern != null && !sourcePattern.isBlank()) {
            this.sourcePatterns.add(sourcePattern);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiagramStep that = (DiagramStep) o;
        return Objects.equals(from, that.from)
                && Objects.equals(to, that.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    /**
     * The cp before the simplification step is applied (usually more complex than to)
     */
    public final CreasePattern from;
    /**
     * the cp after the simplification step is applied (usually less complex than from)
     */
    public final CreasePattern to;

    private final Set<String> sourcePatterns;

    public Set<String> getSourcePatterns() {
        return Collections.unmodifiableSet(sourcePatterns);
    }

    public String getSourcePatternsLabel() {
        if (sourcePatterns.isEmpty()) {
            return "unknown";
        }
        return String.join(", ", sourcePatterns);
    }

    public void mergeSourcesFrom(DiagramStep other) {
        if (other == null) {
            return;
        }
        this.sourcePatterns.addAll(other.sourcePatterns);
    }

    public void addSourcePattern(String sourcePattern) {
        if (sourcePattern == null || sourcePattern.isBlank()) {
            return;
        }
        this.sourcePatterns.add(sourcePattern);
    }
}
