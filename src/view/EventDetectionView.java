package view;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.event.TabCloseEvent;

import esper.EsperNrg4CastHist;
import eventTypes.Alert;

@ManagedBean
@SessionScoped
public class EventDetectionView {

	EsperNrg4CastHist esper;
	private List<EPLPattern> patterns = new ArrayList<EPLPattern>();
	private String selectedPattern="";
	private List<Alert> detectedAlerts = new ArrayList<Alert>();
	private Alert selectedAlert;
private String measurementsText;
	
	//constants
//	private static final String host = "http://localhost";
//	private static final String port = ":1080";
	private static final String host = "http://mustang.ijs.si";
	private static final String port = ":9080";
	private static final String service = "/Nrg4Cast-CEP-Demo";
	private static String detectedAlertsLink = host + port + service + 
			"/ServletSubscriber?events=detected";
	private static String clearLink = host + port + service + 
			"/ServletSubscriber?events=clear";
	private static String measurementsLink = host + port + service + 
			"/DataServlet";
	private static final String USER_AGENT = "Mozilla/5.0";

	public List<EPLPattern> getPatterns() {
		return patterns;
	}



	public String getMeasurementsText() {
		return measurementsText;
	}



	public void setMeasurementsText(String measurementsText) {
		this.measurementsText = measurementsText;
	}



	public List<Alert> getDetectedAlerts() {
		return detectedAlerts;
	}



	public Alert getSelectedAlert() {
		return selectedAlert;
	}



	public void setSelectedAlert(Alert selectedAlert) {
		this.selectedAlert = selectedAlert;
	}



	public void setPatterns(List<EPLPattern> patterns) {
		this.patterns = patterns;
	}



	public String getSelectedPattern() {
		return selectedPattern;
	}

	public void setSelectedPattern(String selectedPattern) {
		this.selectedPattern = selectedPattern;
	}



	public void startButtonAction(ActionEvent actionEvent) {
		if(esper==null){
			esper = new EsperNrg4CastHist();
			addMessage("CEP Engine started!");
		} else {
			addMessage("CEP Engine already started!");
		}

	}

	@PostConstruct
	public void initPatterns() {
		//read filw
		String file = read("D:/server/TomcatNRG4Cast/conf/nrg4castApp/"
				+ "predifinedQueries.epl");
		String[] pats = file.split("\n");
		for(String p: pats){
			if(p!=null && p.length()>1){
				int beginIndex = p.indexOf("\"");
				int endIndex = p.indexOf("\"",beginIndex+1);
				String name = p.substring(beginIndex+1, endIndex);
				String epl = p.substring(endIndex+3, p.length()-1);
				EPLPattern pat = new EPLPattern();
				pat.setEpl(epl);
				pat.setName(name);
				pat.setRegistered(false);
				patterns.add(pat);
			}
		}

	}

	private String read(String fileName){
		StringBuilder sb = new StringBuilder();
		try {
			BufferedReader in= new BufferedReader(new FileReader(
					new File(fileName).getAbsoluteFile()));
			try {
				String s;
				while((s = in.readLine()) != null) {
					sb.append(s);
					sb.append("\n");
				}
			} finally {
				in.close();
			}
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
		return sb.toString();
	}



	public void stopButtonAction(ActionEvent actionEvent) {
		if(esper!=null){
			esper.shutdown();
			esper=null;
			addMessage("CEP Engine stopped!");
		} else {
			addMessage("CEP Engine not started!");
		}



	}

	public void startDataButtonAction(ActionEvent actionEvent){
		if(esper!=null){
			esper.addDataSources();		
			addMessage("data streaming started!");
		} else {
			addMessage("Start engine first!");
		}
	}

	public void clearButtonAction (ActionEvent actionEvent){

		try {

			sendGet(clearLink);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}
	
	public void onTabChange(TabChangeEvent event) {
		selectedPattern = event.getTab().getTitle();		
	}

	public void onTabClose(TabCloseEvent event) {
		selectedPattern = "";
	}

	public void registerButtonAction(ActionEvent actionEvent){
		if(esper !=null ){
			if(selectedPattern.length()>1){
				EPLPattern p = findSelectedPattern(selectedPattern);
				if(p!=null){
					addMessage(esper.addPattern(p.getEpl(), p.getName()));
					p.setRegistered(true);
				}else{
					addMessage("unexpected error, pattern not found by name");
				}
			}else{
				addMessage("select a pattern first");
			}			
		}	else {
			addMessage("Start engine first!");
		}
	}

	private EPLPattern findSelectedPattern(String name) {
		for (EPLPattern p: patterns){
			if(p.getName().equals(name)){
				return p;
			}
		}
		return null;
	}

	public void addMessage(String summary) {
		FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, 
				summary,  null);
		FacesContext.getCurrentInstance().addMessage(null, message);
	}
	
	// HTTP GET request
			private String sendGet(String url) throws Exception {
		 
				 
				URL obj = new URL(url);
				HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		 
				// optional default is GET
				con.setRequestMethod("GET");
		 
				//add request header
				con.setRequestProperty("User-Agent", USER_AGENT);
		 
				int responseCode = con.getResponseCode();
				System.out.println("\nSending 'GET' request to URL : " + url);
				System.out.println("Response Code : " + responseCode);
		 
				BufferedReader in = new BufferedReader(
				        new InputStreamReader(con.getInputStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();
		 
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();
		 
				//print result
				return response.toString();
		 
			}
		 
	public void updateDetectedAlerts(){

		try {
			URL url = new URL(detectedAlertsLink);
			HttpURLConnection connection = (HttpURLConnection)url
					.openConnection();

			connection.setRequestMethod("GET");
			connection.connect();			
			InputStream is =  connection.getInputStream();
			try {

				BufferedReader rd = new BufferedReader
						(new InputStreamReader(is));
				String text = readAll(rd);
				//System.out.println("json: "+text);
				JSONObject obj = (JSONObject) JSONValue.parse(text);
				detectedAlerts = parseJSONEvents(obj,"detected");
			} finally {
				is.close();
			}
			connection.disconnect();

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}				
	}
	
	public void updateMeasurementsText(){

		try {
			URL url = new URL(measurementsLink);
			HttpURLConnection connection = (HttpURLConnection)url
					.openConnection();

			connection.setRequestMethod("GET");
			connection.connect();			
			InputStream is =  connection.getInputStream();
			try {

				BufferedReader rd = new BufferedReader
						(new InputStreamReader(is));
				String text = readAll(rd);
				measurementsText = text;
			} finally {
				is.close();
			}
			connection.disconnect();

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}				
	}

	private List<Alert> parseJSONEvents(JSONObject obj, String eventType) {

		List<Alert> eList = new ArrayList<Alert>();
		boolean add=false;

		if(obj.containsKey("events")){
			JSONArray objArr= (JSONArray)obj.get("events");
			for(int i=0;i<objArr.size();i++){
				JSONObject o = (JSONObject) objArr.get(i);
				String time ="";
				Date timestamp = new Date(0);
				if(o.containsKey("timestamp")){
					time = o.get("timestamp").toString();
									
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					try {
						timestamp = sdf.parse(time);
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				
				String type = "";
				if(o.containsKey("type")){
					type = o.get("type").toString();
				}
				String sensorId = "";
				if(o.containsKey("sensorId")){
					sensorId = o.get("sensorId").toString();
				}
				double latitude=0.0;
				if(o.containsKey("latitude")){
					latitude =Double.parseDouble(o.get("latitude").toString());

				}
				double longitude=0.0;
				if(o.containsKey("longitude")){
					longitude = Double.parseDouble(o.get("longitude").toString());

				}
				String name="";
				if(o.containsKey("name")){
					name = o.get("name").toString();

				}
				String location="";
				if(o.containsKey("location")){
					location = o.get("location").toString();

				}
				String message="";
				if(o.containsKey("message")){
					message = o.get("message").toString();

				}
				String pilotId="";
				if(o.containsKey("pilotId")){
					pilotId = o.get("pilotId").toString();

				}
				String level="";
				if(o.containsKey("level")){
					level = o.get("level").toString();

				}
				String timeWindow="";
				if(o.containsKey("timeWindow")){
					timeWindow = o.get("timeWindow").toString();

				}
				boolean isLatLngSet = true;
				Alert a = new Alert(location, latitude, longitude, message,
						timestamp, name, pilotId, type, level, timeWindow, 
						isLatLngSet, sensorId);

				eList.add(a);
			}
			//	System.out.println(objArr.size() +"\t"+ eList.size());
		}
		return eList;
	}



	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

}
