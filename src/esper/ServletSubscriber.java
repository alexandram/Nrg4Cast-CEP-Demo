package esper;


import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Servlet implementation class ServletSubscriber
 */
@WebServlet("/ServletSubscriber")
public class ServletSubscriber extends HttpServlet {
	private static final long serialVersionUID = 1L;
	List<JSONObject> detectedEvents;
	


	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ServletSubscriber() {
		super();
		detectedEvents = new ArrayList<JSONObject>();
		
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	@SuppressWarnings("unchecked")
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		Enumeration<String> parameters = request.getParameterNames();
		JSONObject responseObjects = new JSONObject();
		if(!parameters.hasMoreElements()){ 
			//
			//empty get means return all events
			//
			HttpSession session = request.getSession();
			if(session.isNew()){
				session.setAttribute("eventsList", new ArrayList<JSONObject>());
			} else {
				JSONArray objarr = new JSONArray();
				for(JSONObject o:(ArrayList<JSONObject>)session.
						getAttribute("eventsList")){
					objarr.add(o);
				}
				responseObjects.put("events", objarr);
			}

		} else { 
			JSONObject obj = new JSONObject();
			
			// check if events parameter exists and 
			// send all events in this case
			//
			if(request.getParameterMap().containsKey("events")){
				JSONArray objarr = new JSONArray();
				String eventTypes = request.getParameter("events");
				if(eventTypes.equals("detected")){
					for(JSONObject o:detectedEvents){
						objarr.add(o);
					}
					
				} else if (eventTypes.equals("clear")){
					detectedEvents.clear();
					
				}  
				responseObjects.put("events", objarr);
			} else {
				//
				// else parse all parameters and add event to lists
				// checking for valid sessions at the same time
				//
				
				String stream="";
				while(parameters.hasMoreElements()){
					String param = parameters.nextElement();
					obj.put(param, request.getParameter(param));
					if(request.getParameterMap().containsKey("stream")){
						stream = request.getParameter("stream");
					}
				}
				responseObjects.put("event", obj);


				if(stream.equals("Alert")){
					if(detectedEvents.size()>10){
						detectedEvents.remove(0);
					}
					detectedEvents.add(obj);
				} 
				
			}
		}


		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.println(responseObjects);
		out.flush();
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
