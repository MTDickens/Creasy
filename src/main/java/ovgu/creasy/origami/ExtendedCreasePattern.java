package ovgu.creasy.origami;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import oripa.domain.creasepattern.CreasePatternInterface;
import oripa.domain.fold.foldability.FoldabilityChecker;
import oripa.domain.fold.halfedge.OrigamiModel;
import oripa.domain.fold.halfedge.OrigamiModelFactory;
import ovgu.creasy.geom.Vertex;
import ovgu.creasy.origami.basic.Crease;
import ovgu.creasy.origami.basic.CreasePattern;
import ovgu.creasy.origami.basic.DiagramStep;
import ovgu.creasy.origami.oripa.OripaTypeConverter;

/**
 * Extended Crease Pattern, adds information about Reflection Creases to a
 * crease Pattern and is used for the step generation Explained in chapter 4.2
 * of the paper (pages 28 - 36)
 */
public class ExtendedCreasePattern {
    private static final boolean DEBUG_STEPS = Boolean.getBoolean("creasy.debug.steps");

    private Set<Vertex> vertices;
    private Set<ExtendedCrease> creases;
    private Map<Vertex, List<ExtendedCrease>> connections;
    private List<DiagramStep> possibleSteps;
    private CreasePattern cp;

    /**
     * @param vertices is the set of extended vertices
     * @param creases is the set of directed edges of the extended graph.
     * @param connections is a set of ordered, circular lists of edges parting
     * from each vertex in vertices
     */
    public ExtendedCreasePattern(Set<Vertex> vertices,
            Set<ExtendedCrease> creases,
            Map<Vertex, List<ExtendedCrease>> connections,
            CreasePattern cp) {
        this.vertices = vertices;
        this.creases = creases;
        this.connections = connections;
        this.cp = cp;
    }

    public CreasePattern toCreasePattern() {
        return this.cp;
    }

    public Set<Vertex> getVertices() {
        return Collections.unmodifiableSet(vertices);
    }

    public Set<ExtendedCrease> getCreases() {
        return Collections.unmodifiableSet(creases);
    }

    public Map<Vertex, List<ExtendedCrease>> getAdjacencyLists() {
        return Collections.unmodifiableMap(connections);
    }

    @Override
    public String toString() {
        return "ExtendedCreasePattern{"
                + "xC=" + creases
                + '}';
    }

    public static List<ExtendedCreasePattern> createECPs(CreasePattern cp, boolean randomized) {
        List<ExtendedCreasePattern> ECPs;
        if (randomized) {
            ECPs = new ExtendedCreasePatternFactory().createRandomizedEcps(cp, 10);
        } else {
            ECPs = new ArrayList<>();
            ECPs.add(new ExtendedCreasePatternFactory().createExtendedCreasePattern(cp));
        }
        return ECPs;
    }

    public static List<DiagramStep> getSteps(List<ExtendedCreasePattern> eCPs) {
        Map<DiagramStep, DiagramStep> merged = new LinkedHashMap<>();
        for (ExtendedCreasePattern ecp : eCPs) {
            for (DiagramStep step : ecp.possibleSteps()) {
                DiagramStep existing = merged.get(step);
                if (existing == null) {
                    merged.put(step, step);
                } else {
                    existing.mergeSourcesFrom(step);
                }
            }
        }
        return merged.values().stream().toList();
    }

    public List<DiagramStep> possibleSteps() {
        if (this.possibleSteps == null) {
            this.possibleSteps = calculatePossibleSteps();
        }
        return possibleSteps;
    }

    private List<DiagramStep> calculatePossibleSteps() {
        List<DiagramStep> steps = new ArrayList<>();
        HashSet<List<ExtendedReflectionPath>> removableCreases = new HashSet<>();
        // System.out.println("finding simple folds");
        for (Vertex vertex : vertices) {

            removableCreases.addAll(findSimpleFolds(vertex).stream().map(Collections::singletonList)
                    .collect(Collectors.toList()));
        }
        Map<CreasePattern, Set<String>> candidates = new LinkedHashMap<>();
        for (List<ExtendedReflectionPath> removablePathList : removableCreases) {
            CreasePattern newcp = this.cp.copy();
            removablePathList.forEach(p -> p.getCreases().forEach(newcp::removeCrease));
            newcp.removeAllLinearPoints();
            candidates.computeIfAbsent(newcp, _k -> new LinkedHashSet<>()).add("simple fold");
        }

        for (SimplificationPattern pattern : KnownPatterns.allPatterns) {
            List<SimplificationPattern.Match> matches = pattern.matches(this);
            for (SimplificationPattern.Match match : matches) {
                CreasePattern simplified = pattern.simplify(this, match);
                String label = pattern.getName();
                if (match.isInverted()) {
                    label += " (inverted)";
                }
                candidates.computeIfAbsent(simplified, _k -> new LinkedHashSet<>()).add(label);
            }
        }

        FoldabilityChecker foldabilityChecker = new FoldabilityChecker();

        if (DEBUG_STEPS) {
            System.out.println("[creasy.debug.steps] ECP candidates=" + candidates.size()
                    + " (simpleFoldCandidates=" + removableCreases.size()
                    + ", patternMatches="
                    + (candidates.size() - removableCreases.size())
                    + "), inputCpCreases=" + this.cp.getCreases().size()
                    + ", inputCpPoints=" + this.cp.getPoints().size());
            int i = 0;
            for (Map.Entry<CreasePattern, Set<String>> entry : candidates.entrySet()) {
                i++;
                CreasePattern candidate = entry.getKey();
                System.out.println("[creasy.debug.steps] candidate#" + i
                        + " sources=" + String.join(", ", entry.getValue())
                        + " creases=" + candidate.getCreases().size()
                        + " points=" + candidate.getPoints().size());
            }
        }

        candidates.forEach((cp, sourcePatterns) -> {
            CreasePattern cp2 = new CreasePattern();
            cp.getCreases().forEach(cp2::addCrease);
            cp2.removeAllLinearPoints();
            CreasePatternInterface cpOripa = OripaTypeConverter.convertToOripaCp(cp2);
            OrigamiModel model = new OrigamiModelFactory().createOrigamiModel(cpOripa, cpOripa.getPaperSize());
            if (foldabilityChecker.testLocalFlatFoldability(model)) {
                DiagramStep step = new DiagramStep(this.toCreasePattern(), cp2);
                for (String source : sourcePatterns) {
                    step.addSourcePattern(source);
                }
                steps.add(step);
                if (DEBUG_STEPS) {
                    System.out.println("[creasy.debug.steps] foldable=true sources=" + step.getSourcePatternsLabel()
                            + " outCreases=" + cp2.getCreases().size()
                            + " outPoints=" + cp2.getPoints().size());
                }
            } else if (DEBUG_STEPS) {
                System.out.println("[creasy.debug.steps] foldable=false sources=" + String.join(", ", sourcePatterns)
                        + " outCreases=" + cp2.getCreases().size()
                        + " outPoints=" + cp2.getPoints().size());
            }
        });
        if (DEBUG_STEPS) {
            System.out.println("[creasy.debug.steps] possibleSteps=" + steps.size());
        }
        return steps;
    }

    private Set<ExtendedReflectionPath> findSimpleFolds(Vertex vertex) {
        List<ExtendedCrease> outgoing = this.connections.get(vertex);
        return outgoing.stream()
                .filter(crease -> crease.getType() != Crease.Type.EDGE)
                .filter(ExtendedCrease::isComplete)
                .map(ExtendedCrease::getExtendedReflectionPath).collect(Collectors.toSet());
    }
}
