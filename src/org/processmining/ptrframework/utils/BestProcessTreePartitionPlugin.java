package org.processmining.ptrframework.utils;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.acceptingpetrinet.models.impl.AcceptingPetriNetImpl;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.plugins.InductiveMiner.mining.MiningParametersIM;
import org.processmining.plugins.InductiveMiner.plugins.IM;
import org.processmining.processtree.Edge;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.AbstractBlock;
import org.processmining.processtree.impl.AbstractTask;
import org.processmining.processtree.impl.ProcessTreeImpl;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class BestProcessTreePartitionPlugin {

    public ProcessTree discover(UIPluginContext context, XLog log) throws ProcessTree2Petrinet.InvalidProcessTreeException, ProcessTree2Petrinet.NotYetImplementedException {
        XFactory factory = new XFactoryNaiveImpl();
        XEventNameClassifier classifier = new XEventNameClassifier();

        XLogInfo info = XLogInfoImpl.create(log, classifier);

        XEventClasses eventClasses = info.getEventClasses();
        HashSet<String> classes = new HashSet<>();
        for (XEventClass aClass : eventClasses.getClasses()) {
            classes.add(aClass.getId());
        }


        PartitionSetCreator<String> partitionSetCreator = new PartitionSetCreator<>(classes);
        Set<Set<Set<String>>> allPartitions = partitionSetCreator.findAllPartitions();

        context.getProgress().setMaximum(allPartitions.size());
        UIPluginContext dummy = context.createChildContext("Dummy");

        ProcessTree lastTree = null;
        double lastPrecision = -1.0;

        for (Set<Set<String>> partitionSet : allPartitions) {
            LinkedList<ProcessTree> partitionTrees = new LinkedList<>();

            for (Set<String> partition : partitionSet) {
                XLog currLog = factory.createLog();
                boolean containsEmptyTrace = false;

                for (XTrace trace : log) {
                    XTrace currTrace = factory.createTrace();

                    for (XEvent event : trace) {
                        if (partition.contains(classifier.getClassIdentity(event))) {
                            currTrace.add(event);
                        }
                    }

                    if (!currTrace.isEmpty()) {
                        currLog.add(currTrace);
                    } else {
                        containsEmptyTrace = true;
                    }
                }

                ProcessTree tree = IM.mineProcessTree(dummy, currLog, new MiningParametersIM());
                if (containsEmptyTrace) {
                    AbstractBlock.Xor newRoot = new AbstractBlock.Xor("");
                    AbstractTask.Automatic skip = new AbstractTask.Automatic("");

                    tree.addNode(newRoot);
                    tree.addNode(skip);
                    tree.addEdge(newRoot.addChild(skip));
                    tree.addEdge(newRoot.addChild(tree.getRoot()));
                    tree.setRoot(newRoot);
                }

                partitionTrees.add(tree);
            }

            ProcessTree combinedTree = combineProcessTreesInParallel(partitionTrees);
            if (ProcessTreeUtils.isAllFlowerTree(combinedTree)) {
                if (lastTree == null) {
                    lastTree = combinedTree;
                }

                continue;
            }

            ProcessTree2Petrinet.PetrinetWithMarkings combinedPetriNet = ProcessTree2Petrinet.convert(combinedTree);

            double allAlignPrecision = AlignmentUtils.getOneAlignPrecision(new AcceptingPetriNetImpl(combinedPetriNet.petrinet, combinedPetriNet.initialMarking, combinedPetriNet.finalMarking), log);

            if (allAlignPrecision > lastPrecision) {
                lastTree = combinedTree;
                lastPrecision = allAlignPrecision;
            }

            context.getProgress().inc();
        }

        return lastTree;
    }

    public ProcessTree combineProcessTreesInParallel(Collection<ProcessTree> processTrees) {
        if (processTrees.size() > 1) {
            ProcessTree result = new ProcessTreeImpl();
            AbstractBlock.And root = new AbstractBlock.And("Flower combining root");
            result.addNode(root);
            result.setRoot(root);

            for (ProcessTree processTree : processTrees) {
                for (Node node : processTree.getNodes()) {
                    result.addNode(node);

                    for (Edge incomingEdge : node.getIncomingEdges()) {
                        result.addEdge(incomingEdge);
                    }
                }

                result.addEdge(root.addChild(processTree.getRoot()));
            }

            return result;
        } else {
            return processTrees.iterator().next();
        }
    }


}
