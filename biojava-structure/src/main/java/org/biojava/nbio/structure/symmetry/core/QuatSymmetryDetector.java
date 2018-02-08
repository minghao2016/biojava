/*
 *                    BioJava development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the individual
 * authors.  These should be listed in @author doc comments.
 *
 * For more information on the BioJava project and its aims,
 * or to join the biojava-l mailing list, visit the home page
 * at:
 *
 *      http://www.biojava.org/
 *
 * Created on 2013-05-23
 *
 */
package org.biojava.nbio.structure.symmetry.core;

import org.biojava.nbio.structure.Calc;
import org.biojava.nbio.structure.Structure;
import org.biojava.nbio.structure.cluster.*;
import org.biojava.nbio.structure.contact.BoundingBox;
import org.biojava.nbio.structure.contact.Grid;
import org.jgrapht.graph.SimpleGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jgrapht.alg.clique.CliqueMinimalSeparatorDecomposition;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.Graph;

import javax.vecmath.Point3d;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Detects the symmetry (global, pseudo, internal and local) of protein
 * structures.
 * <p>
 * The {@link SubunitClustererParameters} determine the subunit definition and
 * clustering, while the {@link QuatSymmetryParameters} determine the calculated
 * symmetry results (point group and axes).
 *
 * @author Peter Rose
 * @author Aleix Lafita
 *
 */
public class QuatSymmetryDetector {

	private static final Logger logger = LoggerFactory
			.getLogger(QuatSymmetryDetector.class);

	private static final double CONTACT_GRAPH_DISTANCE_CUTOFF = 8;
	private static final int CONTACT_GRAPH_MIN_CONTACTS = 5;

	/** Prevent instantiation **/
	private QuatSymmetryDetector() {
	}

	/**
	 * Calculate GLOBAL symmetry results. This means that all {@link Subunit}
	 * are included in the symmetry.
	 *
	 * @param structure
	 *            protein chains will be extracted as {@link Subunit}
	 * @param symmParams
	 *            quaternary symmetry parameters
	 * @param clusterParams
	 *            subunit clustering parameters
	 * @return GLOBAL quaternary structure symmetry results
	 */
	public static QuatSymmetryResults calcGlobalSymmetry(Structure structure,
			QuatSymmetryParameters symmParams,
			SubunitClustererParameters clusterParams) {
		List<SubunitCluster> clusters = SubunitClusterer.cluster(structure,
				clusterParams);
		return calcGlobalSymmetry(clusters, symmParams);
	}

	/**
	 * Calculate GLOBAL symmetry results. This means that all {@link Subunit}
	 * are included in the symmetry.
	 *
	 * @param subunits
	 *            list of {@link Subunit}
	 * @param symmParams
	 *            quaternary symmetry parameters
	 * @param clusterParams
	 *            subunit clustering parameters
	 * @return GLOBAL quaternary structure symmetry results
	 */
	public static QuatSymmetryResults calcGlobalSymmetry(
			List<Subunit> subunits, QuatSymmetryParameters symmParams,
			SubunitClustererParameters clusterParams) {
		List<SubunitCluster> clusters = SubunitClusterer.cluster(subunits,
				clusterParams);
		return calcGlobalSymmetry(clusters, symmParams);
	}

	/**
	 * Calculate GLOBAL symmetry results. This means that all {@link Subunit}
	 * are included in the symmetry.
	 *
	 * @param clusters
	 *            list of {@link SubunitCluster}
	 * @param symmParams
	 *            quaternary symmetry parameters
	 * @return GLOBAL quaternary structure symmetry results
	 */
	public static QuatSymmetryResults calcGlobalSymmetry(
			List<SubunitCluster> clusters, QuatSymmetryParameters symmParams) {
		return calcQuatSymmetry(clusters, symmParams);
	}


	/**
	 * Returns a List of LOCAL symmetry results. This means that a subset of the
	 * {@link SubunitCluster} is left out of the symmetry calculation. Each
	 * element of the List is one possible LOCAL symmetry result.
	 * <p>
	 * Determine local symmetry if global structure is: (1) asymmetric, C1; (2)
	 * heteromeric (belongs to more than 1 subunit cluster); (3) more than 2
	 * subunits (heteromers with just 2 chains cannot have local symmetry)
	 *
	 * @param structure
	 *            protein chains will be extracted as {@link Subunit}
	 * @param symmParams
	 *            quaternary symmetry parameters
	 * @param clusterParams
	 *            subunit clustering parameters
	 * @return List of LOCAL quaternary structure symmetry results. Empty if
	 *         none.
	 */
	public static List<QuatSymmetryResults> calcLocalSymmetries(
			Structure structure, QuatSymmetryParameters symmParams,
			SubunitClustererParameters clusterParams) {

		List<SubunitCluster> clusters = SubunitClusterer.cluster(structure, clusterParams);
		return calcLocalSymmetries(clusters, symmParams);
	}

	/**
	 * Returns a List of LOCAL symmetry results. This means that a subset of the
	 * {@link SubunitCluster} is left out of the symmetry calculation. Each
	 * element of the List is one possible LOCAL symmetry result.
	 * <p>
	 * Determine local symmetry if global structure is: (1) asymmetric, C1; (2)
	 * heteromeric (belongs to more than 1 subunit cluster); (3) more than 2
	 * subunits (heteromers with just 2 chains cannot have local symmetry)
	 *
	 * @param subunits
	 *            list of {@link Subunit}
	 * @param symmParams
	 *            quaternary symmetry parameters
	 * @param clusterParams
	 *            subunit clustering parameters
	 * @return List of LOCAL quaternary structure symmetry results. Empty if
	 *         none.
	 */
	public static List<QuatSymmetryResults> calcLocalSymmetries(
			List<Subunit> subunits, QuatSymmetryParameters symmParams,
			SubunitClustererParameters clusterParams) {

		List<SubunitCluster> clusters = SubunitClusterer.cluster(subunits, clusterParams);
		return calcLocalSymmetries(clusters, symmParams);
	}

	/**
	 * Returns a List of LOCAL symmetry results. This means that a subset of the
	 * {@link SubunitCluster} is left out of the symmetry calculation. Each
	 * element of the List is one possible LOCAL symmetry result.
	 * <p>
	 * Determine local symmetry if global structure is: (1) asymmetric, C1; (2)
	 * heteromeric (belongs to more than 1 subunit cluster); (3) more than 2
	 * subunits (heteromers with just 2 chains cannot have local symmetry)
	 *
	 * @param clusters
	 *            list of {@link SubunitCluster}
	 * @param symmParams
	 *            quaternary symmetry parameters
	 * @return List of LOCAL quaternary structure symmetry results. Empty if
	 *         none.
	 */

	public static List<QuatSymmetryResults> calcLocalSymmetries(List<SubunitCluster> clusters, QuatSymmetryParameters symmParams) {

		symmParams.setLocalTimeStart(System.nanoTime());

		Set<Set<Integer>> knownResults = new HashSet<>();
		//more than one subunit per cluster required for symmetry
		List<SubunitCluster> nontrivialClusters =
				clusters.stream().
					filter(cluster -> (cluster.size()>1)).
					collect(Collectors.toList());

		QuatSymmetrySubunits allSubunits = new QuatSymmetrySubunits(nontrivialClusters);

		if (allSubunits.getSubunitCount() < 2)
			return new ArrayList<>();

		Graph<Integer, DefaultEdge> graph = initContactGraph(nontrivialClusters);

		List<Integer> allSubunitIds = new ArrayList<>(graph.vertexSet());
		Collections.sort(allSubunitIds);
		List<Integer> allSubunitClusterIds = allSubunits.getClusterIds();

		// since clusters are rearranged and trimmed, we need a reference to the original data
		// to maintain consistent IDs of clusters and subunits across all solutions
		Map<Integer, List<Integer>> clusterIdToSubunitIds =
				allSubunitIds.stream().
					collect(Collectors.
						groupingBy(allSubunitClusterIds::get, Collectors.toList()));

		List<QuatSymmetryResults> redundantSymmetries = new ArrayList<>();
		// first, find symmetries for single clusters and their groups
		// grouping is done based on symmetries found (i.e., no exhaustive permutation search is performed)
		if (clusters.size()>1) {
			List<QuatSymmetryResults> clusterSymmetries =
					calcLocalSymmetriesCluster(nontrivialClusters, clusterIdToSubunitIds,symmParams, knownResults);
			redundantSymmetries.addAll(clusterSymmetries);
		}
		//find symmetries for groups based on connectivity of subunits
		// disregarding initial clustering
		List<QuatSymmetryResults> graphSymmetries = calcLocalSymmetriesGraph(nontrivialClusters,
																			allSubunitClusterIds,
																			clusterIdToSubunitIds,
																			symmParams,
																			knownResults,
																			graph);

		redundantSymmetries.addAll(graphSymmetries);

		// find symmetries which are not superseded by any other symmetry
		// e.g., we have global stoichiometry of A3B3C,
		// the local symmetries found are C3 with stoichiometries A3, B3, A3B3;
		// then output only A3B3.

		List<QuatSymmetryResults> outputSymmetries =
				redundantSymmetries.stream().
					filter(a -> redundantSymmetries.stream().
						noneMatch(b -> a!=b && a.isSupersededBy(b))).
						collect(Collectors.toList());

		double time = (System.nanoTime() - symmParams.getLocalTimeStart()) / 1000000000;
		if (time > symmParams.getLocalTimeLimit()) {
			logger.warn("Exceeded time limit for local symmetry "
					+ "calculations. {} seconds elapsed. "
					+ "Local symmetry results may be incomplete.", time);
		}
		return outputSymmetries;
	}


	private static  Graph<Integer, DefaultEdge> initContactGraph(List<SubunitCluster> clusters){

		Graph<Integer, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);

		// extract Ca coords from every subunit of every cluster.
		// all subunit coords are used for contact evaluation,
		// not only the aligned equivalent residues
		List <Point3d[]> clusterSubunitCoords =
				clusters.stream().
					flatMap(c -> c.getSubunits().stream()).
						map(r -> Calc.atomsToPoints(r.getRepresentativeAtoms())).
						collect(Collectors.toList());

		for (int i = 0; i < clusterSubunitCoords.size(); i++) {
			graph.addVertex(i);
		}

		// pre-compute bounding boxes
		List<BoundingBox> boundingBoxes = new ArrayList<>();
		clusterSubunitCoords.forEach(c -> boundingBoxes.add(new BoundingBox(c)));

		for (int i = 0; i < clusterSubunitCoords.size() - 1; i++) {
			Point3d[] coords1 = clusterSubunitCoords.get(i);
			BoundingBox bb1 = boundingBoxes.get(i);

			for (int j = i + 1; j < clusterSubunitCoords.size(); j++) {
				Point3d[] coords2 = clusterSubunitCoords.get(j);
				BoundingBox bb2 = boundingBoxes.get(j);
				Grid grid = new Grid(CONTACT_GRAPH_DISTANCE_CUTOFF);
				grid.addCoords(coords1, bb1, coords2, bb2);

				if (grid.getIndicesContacts().size() >= CONTACT_GRAPH_MIN_CONTACTS) {
					graph.addEdge(i, j);
				}
			}
		}
		return graph;
	}

	private static List<QuatSymmetryResults> calcLocalSymmetriesCluster(List<SubunitCluster> nontrivialClusters,
	                                                                    Map<Integer, List<Integer>> clusterIdToSubunitIds,
	                                                                    QuatSymmetryParameters symmParams,
	                                                                    Set<Set<Integer>> knownResults) {

		List<QuatSymmetryResults> clusterSymmetries = new ArrayList<>();

		// find solutions for single clusters first
		for (int i=0;i<nontrivialClusters.size();i++) {
			SubunitCluster nontrivialCluster = nontrivialClusters.get(i);
			QuatSymmetryResults localResult =
					calcQuatSymmetry(Collections.singletonList(nontrivialCluster),symmParams);

			if(localResult!=null && !localResult.getSymmetry().equals("C1")) {
				localResult.setLocal(true);
				clusterSymmetries.add(localResult);
				Set<Integer> knownResult = new HashSet<>(clusterIdToSubunitIds.get(i));
				// since symmetry is found,
				// do not try graph decomposition of this set of subunits later
				knownResults.add(knownResult);
			}
		}

		// group clusters by symmetries found, in case they all share axes and have the same number of subunits
		Map<String, Map<Integer,List<QuatSymmetryResults>>> groupedSymmetries =
				clusterSymmetries.stream().
					collect(Collectors.
						groupingBy(QuatSymmetryResults::getSymmetry,Collectors.
							groupingBy(QuatSymmetryResults::getSubunitCount,Collectors.toList())));

		for (Map<Integer,List<QuatSymmetryResults>> symmetriesByGroup: groupedSymmetries.values()) {
			for (List<QuatSymmetryResults> symmetriesBySubunits: symmetriesByGroup.values()) {

				List<SubunitCluster> clustersGroup =
						symmetriesBySubunits.stream().
							map(r -> r.getSubunitClusters().get(0)).
							collect(Collectors.toList());

				if (clustersGroup.size() < 2) {
					continue;
				}
				//check if grouped clusters also have symmetry
				QuatSymmetryResults localResult = calcQuatSymmetry(clustersGroup,symmParams);

				if(localResult!=null && !localResult.getSymmetry().equals("C1")) {
					localResult.setLocal(true);
					clusterSymmetries.add(localResult);
					// find subunit ids in this cluster list
					Set<Integer> knownResult = new HashSet<>();
					for (SubunitCluster cluster: clustersGroup) {
						int i = nontrivialClusters.indexOf(cluster);
						knownResult.addAll(clusterIdToSubunitIds.get(i));
					}
					// since symmetry is found,
					// do not try graph decomposition of this set of subunits later
					knownResults.add(knownResult);
				}
			}
		}
		return clusterSymmetries;
	}


	private static List<QuatSymmetryResults> calcLocalSymmetriesGraph(final List<SubunitCluster> allClusters,
	                                                                  final List<Integer> allSubunitClusterIds,
	                                                                  final Map<Integer, List<Integer>> clusterIdToSubunitIds,
	                                                                  QuatSymmetryParameters symmParams,
	                                                                  Set<Set<Integer>> knownResults,
	                                                                  Graph<Integer, DefaultEdge> graph) {

		List<QuatSymmetryResults> localSymmetries = new ArrayList<>();

		// do not go any deeper into recursion if over the time limit
		double time = (System.nanoTime() - symmParams.getLocalTimeStart()) / 1000000000;
		if (time > symmParams.getLocalTimeLimit()) {
			return localSymmetries;
		}

		// extract components of a (sub-)graph
		CliqueMinimalSeparatorDecomposition<Integer, DefaultEdge> cmsd =
				new CliqueMinimalSeparatorDecomposition<>(graph);

		// only consider components with more than 1 vertex (subunit)
		Set<Set<Integer>> graphComponents =
				cmsd.getAtoms().stream().
					filter(component -> component.size()>1).
					collect(Collectors.toSet());

		//do not go into what has already been explored
		graphComponents.removeAll(knownResults);

		for (Set<Integer> graphComponent: graphComponents) {
			knownResults.add(graphComponent);

			List<Integer> usedSubunitIds = new ArrayList<>(graphComponent);
			Collections.sort(usedSubunitIds);
			// get clusters which contain only subunits in the current component
			List<SubunitCluster> localClusters =
					trimSubunitClusters(allClusters, allSubunitClusterIds, clusterIdToSubunitIds, usedSubunitIds);

			if (localClusters.size()==0) {
				continue;
			}

			//NB: usedSubunitIds might have changed when trimming clusters
			// if a subunit belongs to a cluster with no other subunits,
			// it is removed inside trimSubunitClusters
			Set<Integer> usedSubunitIdsSet = new HashSet<>(usedSubunitIds);
			if(!graphComponent.equals(usedSubunitIdsSet)) {
				if(knownResults.contains(usedSubunitIdsSet)) {
					continue;
				} else {
					knownResults.add(usedSubunitIdsSet);
				}
			}

			QuatSymmetryResults localResult = calcQuatSymmetry(localClusters,symmParams);
			if(localResult!=null && !localResult.getSymmetry().equals("C1")) {
				localResult.setLocal(true);
				localSymmetries.add(localResult);
				continue;
			}

			if (usedSubunitIds.size() < 3) {
				// cannot decompose this component any further
				continue;
			}

			for (Integer removeSubunitId: usedSubunitIds) {
				// try removing subunits one by one and decompose the sub-graph recursively
				Set<Integer> prunedGraphVertices = new HashSet<>(usedSubunitIds);
				prunedGraphVertices.remove(removeSubunitId);
				if (knownResults.contains(prunedGraphVertices)) {
					continue;
				}
				knownResults.add(prunedGraphVertices);

				Graph<Integer, DefaultEdge> subGraph = new AsSubgraph<>(graph,prunedGraphVertices);

				List<QuatSymmetryResults> localSubSymmetries = calcLocalSymmetriesGraph(allClusters,
																						allSubunitClusterIds,
																						clusterIdToSubunitIds,
																						symmParams,
																						knownResults,
																						subGraph);
				localSymmetries.addAll(localSubSymmetries);
			}

		}

		return localSymmetries;
	}

	private static List<SubunitCluster> trimSubunitClusters(List<SubunitCluster> allClusters,
	                                                        List<Integer> allSubunitClusterIds,
	                                                        Map<Integer, List<Integer>> clusterIdToSubunitIds,
	                                                        List<Integer> usedSubunitIds) {
		List<SubunitCluster> localClusters = new ArrayList<>();

		Set<Integer> usedClusterIds =
				usedSubunitIds.stream().
					map(allSubunitClusterIds::get).
					distinct().
					collect(Collectors.toSet());

		// for each used cluster, remove unused subunits
		for(Integer usedClusterId:usedClusterIds) {
			SubunitCluster originalCluster = allClusters.get(usedClusterId);
			List<Integer> allSubunitIdsInCluster = clusterIdToSubunitIds.get(usedClusterId);

			//subunit numbering is global for the entire graph
			// make it zero-based for the inside of a cluster
			int minSUValue = Collections.min(allSubunitIdsInCluster);
			List<Integer> usedSubunitIdsInCluster = new ArrayList<>(allSubunitIdsInCluster);
			usedSubunitIdsInCluster.retainAll(usedSubunitIds);

			List<Integer> subunitsToRetain =
					usedSubunitIdsInCluster.stream().
						map(i -> i-minSUValue).
						collect(Collectors.toList());

			if (subunitsToRetain.size()>1) {
				SubunitCluster filteredCluster = new SubunitCluster(originalCluster, subunitsToRetain);
				localClusters.add(filteredCluster);
			} else {
				// if the cluster ends up having only 1 subunit, remove it from further processing
				usedSubunitIds.removeAll(usedSubunitIdsInCluster);
			}
		}
		return localClusters;
	}


	private static QuatSymmetryResults calcQuatSymmetry(
			List<SubunitCluster> clusters, QuatSymmetryParameters parameters) {

		QuatSymmetrySubunits subunits = new QuatSymmetrySubunits(clusters);

		if (subunits.getSubunitCount() == 0)
			return null;

		RotationGroup rotationGroup = null;
		SymmetryPerceptionMethod method = null;
		if (subunits.getFolds().size() == 1) {
			// no symmetry possible, create empty ("C1") rotation group
			method = SymmetryPerceptionMethod.NO_ROTATION;
			rotationGroup = new RotationGroup();
			rotationGroup.setC1(subunits.getSubunitCount());
		} else if (subunits.getSubunitCount() == 2
				&& subunits.getFolds().contains(2)) {
			method = SymmetryPerceptionMethod.C2_ROTATION;
			QuatSymmetrySolver solver = new C2RotationSolver(subunits,
					parameters);
			rotationGroup = solver.getSymmetryOperations();
		} else {
			method = SymmetryPerceptionMethod.ROTATION;
			QuatSymmetrySolver solver = new RotationSolver(subunits, parameters);
			rotationGroup = solver.getSymmetryOperations();
		}

		QuatSymmetryResults results = new QuatSymmetryResults(clusters,
				rotationGroup, method);

		String symmetry = results.getSymmetry();

		// Check structures with Cn symmetry (n = 1, ...) for helical symmetry
		if (symmetry.startsWith("C")) {
			HelixSolver hc = new HelixSolver(subunits,
					rotationGroup.getOrder(), parameters);
			HelixLayers helixLayers = hc.getSymmetryOperations();

			if (helixLayers.size() > 0) {
				// if the complex has no symmetry (c1) or has Cn (n>=2) symmetry
				// and the
				// helical symmetry has a lower RMSD than the cyclic symmetry,
				// set helical symmetry
				// If the RMSD for helical and cyclic symmetry is similar, a
				// slight preference is
				// given to the helical symmetry by the helixRmsdThreshold
				// parameter.
				double cRmsd = rotationGroup.getScores().getRmsd();
				double hRmsd = helixLayers.getScores().getRmsd();
				// System.out.println("cRMSD: " + cRmsd + " hRMSD: " + hRmsd);
				double deltaRmsd = hRmsd - cRmsd;
				if (symmetry.equals("C1")
						|| (!symmetry.equals("C1") && deltaRmsd <= parameters
								.getHelixRmsdThreshold())) {
					method = SymmetryPerceptionMethod.ROTO_TRANSLATION;
					results = new QuatSymmetryResults(clusters, helixLayers,
							method);
				}
			}
		}

		return results;
	}
}
