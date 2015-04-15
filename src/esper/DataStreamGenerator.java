package esper;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;

import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.time.CurrentTimeEvent;

import eventTypes.Measurement;

public class DataStreamGenerator extends Thread{
	static final Logger log = Logger.getLogger(DataStreamGenerator.class);
	private EPRuntime cepRT;
	
//	private static String host = "http://localhost";
//	private static String port = ":1080";
	private static String host = "http://mustang.ijs.si";
	private static String port = ":9080";
	private static String service = "/Nrg4Cast-CEP-Demo";
	private static String dataServletLink = host + port + service + 
			"/DataServlet";
	private final String USER_AGENT = "Mozilla/5.0";
	
	
	public DataStreamGenerator(EPRuntime cepRT){
		this.cepRT = cepRT;
	}

	@Override
	public void run() {
		//connect to database
		Connection dbCon = connectPsql("postgres", "pgAdmin3");
		// get measurements month by month starting with 2011-06-01
		// until 2015-03-1
		Calendar c = Calendar.getInstance();
		c.set(2011, 5, 1, 0, 0);
		Calendar end = Calendar.getInstance();
		end.set(2015, 3, 1, 0, 0);
		while(c.getTime().before(end.getTime())){
			ArrayList<Measurement> mes = getMeasurements(dbCon, c);
			//sendMeasurements to esper
			sendMeasurements(mes);
			//increment calendar with one month
			System.out.println(c.getTime().toString() + " has " +mes.size());
			c.add(Calendar.MONTH, 1);
			
		}
		
		
		try {
			dbCon.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	private void sendMeasurements(ArrayList<Measurement> mes) {
		
		if(mes==null){
			return;
		}
		Date last = new Date(0);
		if(mes.size()>0){
			last = mes.get(0).getTimestamp();
			//System.out.println(last.toString());
		}
		for(Measurement m: mes){			
			cepRT.sendEvent(m);
			
			if(last.before(m.getTimestamp())){
				cepRT.sendEvent(new CurrentTimeEvent(last.getTime()));
				last = m.getTimestamp(); 
				try {
					sendGet(dataServletLink+"?text="+URLEncoder.encode(m.toString(),"UTF-8"));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			//	System.out.println(last.toString() + " time event sent" );
			}
		}
		
	}

	private ArrayList<Measurement> getMeasurements(Connection con, Calendar time) {
		ArrayList<Measurement> result = new ArrayList<Measurement>();
		Calendar end = Calendar.getInstance();
		end.setTime(time.getTime());
		end.add(Calendar.MONTH, 1);
		
				
		String sql = "SELECT *FROM \"Measurements\"  WHERE "
				+ " \"nodeId\" like '%turin%' and "
				+ "timestamp>='"+time.getTime()+"' and "
				+ "timestamp<'"+end.getTime()+"'  "
				+ "ORDER BY timestamp ASC ";
	//	System.out.println(sql);
		try {
			Statement stmt = con.createStatement();
			stmt.execute(sql);
			ResultSet res = stmt.getResultSet();
			while(res.next()){
				
				/*"nodeId" text NOT NULL,
				  "nodeName" text,
				  "subjectId" text,
				  lat double precision,
				  lng double precision,
				  "sensorId" text NOT NULL,
				  value real,
				  "timestamp" timestamp without time zone NOT NULL,
				  "typeId" text,
				  "sensorType" text,
				  phenomenon text,
				  uom text,*/
				String nodeId = res.getString("nodeId");
				String nodeName = res.getString("nodeName");
				String subjectId= res.getString("subjectId");
				double lat = res.getDouble("lat");
				double lng = res.getDouble("lng");
				String sensorId = res.getString("sensorId");
				double value = res.getDouble("value");
				Date timestamp = res.getTimestamp("timestamp");
				String typeId = res.getString("typeId");
				String sensorType = res.getString("sensorType");
				String phenomenon = res.getString("phenomenon");
				String uom = res.getString("uom");
				Measurement m = new Measurement(nodeId, nodeName, subjectId, 
						lat, lng, sensorId, value, timestamp, 
						typeId, sensorType, phenomenon, uom);
				result.add(m);
			}
			res.close();
			stmt.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return result;
	}


	private Connection connectPsql(String user, String password){
		System.out.println("-------- PostgreSQL "
				+ "JDBC Connection Testing ------------");
 
		try {
 
			Class.forName("org.postgresql.Driver");
 
		} catch (ClassNotFoundException e) {
 
			System.out.println("Where is your PostgreSQL JDBC Driver? "
					+ "Include in your library path!");
			e.printStackTrace();
			return null;
 
		}
 
		System.out.println("PostgreSQL JDBC Driver Registered!");
 
		Connection connection = null;
 
		try {
 
			connection = DriverManager.getConnection(
					"jdbc:postgresql://127.0.0.1:5432/nrg4cast", user,
					password);
 
		} catch (SQLException e) {
 
			System.out.println("Connection Failed! Check output console");
			e.printStackTrace();
			return null;
 
		}
 
		if (connection != null) {
			System.out.println("connection successfull!");
			return connection;
		} else {
			System.out.println("Failed to make connection!");
			return null;
		}
	}

	private String sendGet(String url) throws Exception {
		 
		 
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
 
		// optional default is GET
		con.setRequestMethod("GET");
 
		//add request header
		con.setRequestProperty("User-Agent", USER_AGENT);
 
		int responseCode = con.getResponseCode();
	
 
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
}
