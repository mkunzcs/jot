package org.openolly.reporting;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableGraph;
import org.apache.commons.text.StringEscapeUtils;

// ...

public class RouteGraph {

	private static MutableGraph<Event> graph = GraphBuilder.directed().allowsSelfLoops(true).build();
 
	public static void update(Trace t) {
		try {
			List<Event> events = t.getEventsForRule( "route" );
			if ( events.isEmpty() ) {
				return;
			}
			Event route = events.get(0);
			if ( route.getCapture().endsWith(".js") || route.getCapture().endsWith(".css") 
				|| route.getCapture().endsWith( ".jpg") || route.getCapture().endsWith( ".png" )
				|| route.getCapture().endsWith( ".gif" ) ) {
				return;
			}
			route = findExistingRoute( route );
			graph.addNode( route );

			Event tail = route;
			for ( Event event : t.getEvents() ) {
				if ( !event.getRule().equals( "route" ) ) {
					event = findExistingEventInRoute( route, event );
					graph.putEdge( tail, event );
					tail = event;
					if ( event.getRule().equals( "forward" ) ) {
						route = event;
					}
				}
			}
			dump( graph );
		} catch( Exception e ) {
			e.printStackTrace();
		}
	}

	// Return existing node for this route event if it's already in graph
	public static Event findExistingRoute( Event add ) {
		for ( Event e : graph.nodes() ) {
			if ( e.getRule().equals( add.getRule() ) && e.getCapture().equals( add.getCapture() ) ) {
				return e;
			}
		}
		return add;
	}

	// Return existing event if it's already in the subgraph for this route
	public static Event findExistingEventInRoute( Event route, Event add ) {
		Set<Event> t = Graphs.reachableNodes(graph, route);
		for ( Event e : t ) {
			if ( e.getRule().equals( add.getRule() ) && e.getCapture().equals( add.getCapture() ) ) {
				return e;
			}
		}
		return add;
	}

	// FIXME: HTML Encode specials
	private static String style = "shape=box, style=\"rounded,filled\",fillcolor=blue,fontcolor=white,fontname=Helvetica,fontsize=10";
	public static void dump( Graph<Event> g ) {
		System.err.println( "====" );
		System.err.println( "digraph {" );
		// System.err.println( "\trankdir=LR" );
		if ( !graph.nodes().isEmpty() ) {
			System.err.println( "\t{ node ["+style+"]");
			for ( Event e : graph.nodes() ) {
				System.err.println( "\t\t" + e.getHash() + " [label=<<TABLE border=\"0\">"+
				"<TR><TD>"+StringEscapeUtils.escapeHtml4( e.getRule() )+"</TD></TR>"+
				"<TR><TD>"+StringEscapeUtils.escapeHtml4( e.getCapture() )+"</TD></TR>"+
				"<TR><TD>"+StringEscapeUtils.escapeHtml4( e.getCaller(2) )+"</TD></TR>"+
				"</TABLE>>];" );
			}
			System.err.println( "\t}" );
		}
		if ( !graph.edges().isEmpty() ) {
			System.err.println( "\t{ edge [style=dashed]");
			for ( EndpointPair<Event> edge : graph.edges() ) {
				System.err.println( "\t\t" + edge.source().getHash() + " -> " + edge.target().getHash() );
			}
			System.err.println( "\t}" );
		}
		System.err.println( "}" );
		System.err.println( "====" );
	}
    
}
