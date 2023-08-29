package org.processmining.ptrframework.utils;

import org.deckfour.xes.extension.std.XExtendedEvent;
import org.deckfour.xes.model.XTrace;

import java.util.List;
import java.util.stream.Collectors;

public class TraceUtils {
    public static List<String> traceToStringList(XTrace trace) {
        return trace.stream().map(event -> new XExtendedEvent(event).getName()).collect(Collectors.toList());
    }
}
