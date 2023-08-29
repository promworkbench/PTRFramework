package org.processmining.ptrframework.plugins;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginCategory;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.plugins.InductiveMiner.plugins.IMProcessTree;
import org.processmining.processtree.Block;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;
import org.processmining.ptrframework.algorithms.treereplay.ProcessTreeForReplayPlugin;
import org.processmining.ptrframework.utils.BestProcessTreePartitionPlugin;
import org.processmining.ptrframework.utils.ProcessTreeModifier;
import org.processmining.ptrframework.utils.ProcessTreeUtils;

import java.util.ArrayList;

@Plugin(name = "Process Tree Projection & Replacement Framework (PtR framework)", parameterLabels = {"Process Tree", "Log"}, returnLabels = {"Process Tree"}, returnTypes = {ProcessTree.class}, categories = {PluginCategory.Discovery, PluginCategory.Enhancement}, keywords = {"Inductive Miner", "ProjectionMiner", "IM", "Precision Improvement"}, help = "Applies the inductive miner to flower structures within the process tree.")
public class InductiveProjectionMiner {
    ArrayList<Node> replacementCandidates;

    @PluginVariant(requiredParameterLabels = {0, 1})
    @UITopiaVariant(affiliation = "PADS", author = "Christian Rennert", email = "rennert@pads.rwth-aachen.de")
    public ProcessTree discover(UIPluginContext context, ProcessTree inputTree, XLog log) throws ProcessTree2Petrinet.InvalidProcessTreeException, ProcessTree2Petrinet.NotYetImplementedException {
        replacementCandidates = new ArrayList<>();

        return discoverProjective(context, inputTree, log);
    }

    public ProcessTree discoverProjective(UIPluginContext context, ProcessTree inputTree, XLog log) throws ProcessTree2Petrinet.InvalidProcessTreeException, ProcessTree2Petrinet.NotYetImplementedException {
        ProcessTree copyTree = inputTree;

        ProcessTreeModifier treeModifier = new ProcessTreeModifier();
        copyTree = treeModifier.apply(copyTree);
        findCandidatesForReplacement(copyTree);

        ProcessTreeForReplayPlugin plugin = new ProcessTreeForReplayPlugin();
        plugin.wipeLastSettings();
        plugin.computeAndStoreAlignmentPerNode(copyTree, log);

        for (Node replacementCandidate : replacementCandidates) {
            Block flower = (Block) replacementCandidate;
            XLog logOfNode = plugin.getLogOfNode(flower);

            ProcessTree substitutingTree = IMProcessTree.mineProcessTree(logOfNode);
            if (ProcessTreeUtils.isAllFlowerTree(substitutingTree)) {
                BestProcessTreePartitionPlugin bestProcessTreePartitionPlugin = new BestProcessTreePartitionPlugin();
                substitutingTree = bestProcessTreePartitionPlugin.discover(context, logOfNode);
            }

            copyTree = ProcessTreeUtils.replaceSubProcessTreeBySubProcessTree(copyTree, flower, substitutingTree);
        }

        return copyTree;
    }

    private void findCandidatesForReplacement(ProcessTree tree) {
        tree.getNodes().stream().filter(node -> node.getName().equals(ProcessTreeModifier.getIdentifierName())).forEach(replacementCandidates::add);
    }

}
