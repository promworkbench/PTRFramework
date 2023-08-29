package org.processmining.ptrframework.utils;

import nl.tue.astar.AStarException;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.astar.petrinet.AbstractPetrinetReplayer;
import org.processmining.plugins.astar.petrinet.PetrinetReplayerWithILP;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.multietc.res.MultiETCResult;
import org.processmining.plugins.multietc.sett.MultiETCSettings;
import org.processmining.plugins.petrinet.replayer.algorithms.costbasedcomplete.CostBasedCompleteParam;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.stochasticawareconformancechecking.cli.FakeContext;

import java.util.HashMap;
import java.util.Map;

public final class AlignmentUtils {
    private AlignmentUtils() {
    }

    public static double getOneAlignPrecision(final AcceptingPetriNet apn, final XLog log) {
        PNRepResult alignments = getAlignment(apn.getNet(), log, apn.getInitialMarking(), apn.getFinalMarkings().toArray(new Marking[0])[0]);
        MultiETCSettings sett = new MultiETCSettings();
        sett.put(MultiETCSettings.REPRESENTATION, MultiETCSettings.Representation.ORDERED);
        AcceptingPetriNetMultiETCConformance etcPrecision = new AcceptingPetriNetMultiETCConformance();
        Object[] result = etcPrecision.checkMultiETCAlign1(apn, sett, alignments);
        return (double) ((MultiETCResult) result[0]).getAttribute(MultiETCResult.PRECISION);
    }


    public static PNRepResult getAlignment(final PetrinetGraph net, final XLog log, final Marking initialMarking, final Marking finalMarking) {
        Map<Transition, Integer> costMOS = constructMOSCostFunction(net);
        XEventClassifier eventClassifier = new XEventNameClassifier();
        Map<XEventClass, Integer> costMOT = constructMOTCostFunction(log, eventClassifier);
        TransEvClassMapping mapping = constructTransEvClassMapping(net, log, eventClassifier);

        AbstractPetrinetReplayer<?, ?> replayEngine = new PetrinetReplayerWithILP();

        CostBasedCompleteParam parameters = new CostBasedCompleteParam(costMOT, costMOS);
        parameters.setInitialMarking(initialMarking);
        parameters.setFinalMarkings(finalMarking);
        parameters.setGUIMode(false);
        parameters.setCreateConn(false);
        parameters.setNumThreads(Runtime.getRuntime().availableProcessors() - 1);
        parameters.setMaxNumOfStates(2000 * 1000);

        PNRepResult result = null;
        try {
            result = replayEngine.replayLog(new FakeContext(), net, log, mapping, parameters);

        } catch (AStarException e) {
            System.out.println(e);
        }

        return result;
    }

    public static Map<Transition, Integer> constructMOSCostFunction(final PetrinetGraph net) {
        Map<Transition, Integer> costMOS = new HashMap<>();

        for (Transition t : net.getTransitions())
            if (t.isInvisible()) costMOS.put(t, 0);
            else costMOS.put(t, 1);

        return costMOS;
    }

    private static Map<XEventClass, Integer> constructMOTCostFunction(final XLog log, XEventClassifier eventClassifier) {
        Map<XEventClass, Integer> costMOT = new HashMap<>();
        XLogInfo summary = XLogInfoFactory.createLogInfo(log, eventClassifier);

        for (XEventClass evClass : summary.getEventClasses().getClasses()) {
            costMOT.put(evClass, 1);
        }

        return costMOT;
    }

    private static TransEvClassMapping constructTransEvClassMapping(final PetrinetGraph net, final XLog log, final XEventClassifier eventClassifier) {
        TransEvClassMapping mapping = new TransEvClassMapping(eventClassifier, new XEventClass("DUMMY", 99999));

        XLogInfo summary = XLogInfoFactory.createLogInfo(log, eventClassifier);

        for (Transition t : net.getTransitions()) {
            for (XEventClass evClass : summary.getEventClasses().getClasses()) {
                String id = evClass.getId();

                if (t.getLabel().equals(id)) {
                    mapping.put(t, evClass);
                    break;
                }
            }

        }

        return mapping;
    }
}
