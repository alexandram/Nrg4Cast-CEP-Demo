package esper;



import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;



import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;

import eventTypes.Alert;
import eventTypes.Measurement;
import eventTypes.Prediction;

public class EsperNrg4CastHist  {

	EPServiceProvider cep = null;
	Configuration cepConfig = null;
	static String configFile = "D:/Users/alexandram/nrg4cast/"
			+ "Esper-Services/WebContent/WEB-INF/conf/esper.nrg4castHist.cfg.xml";
	static String engineName = "nrg4castHist";
	List<Thread> datasources;

	//ServletContext context;


	public EsperNrg4CastHist() {	

		System.out.println("esper constructor");

		//initializations
		datasources = new ArrayList<Thread>();

		// The Configuration is meant only as an initialization-time object.
		cepConfig = new Configuration();
		// use File as a hack for fixing the issue with classpath 
		// run configuration, which seems that are not exported in .war
		// otherwise change configfile to the string name of the file
		// and pass it directly as parameter	
		File f = new File(configFile);
		cepConfig.configure(f);
		// for offline analysis disable the internal timer	
		// else comment the 2 lines below	
		cepConfig.getEngineDefaults().getThreading().
		setInternalTimerEnabled(false);

		addEventTypes(cepConfig);

		cep = EPServiceProviderManager.getProvider(
				engineName, cepConfig);		
		EPRuntime cepRT = cep.getEPRuntime();

		cepRT.sendEvent(new CurrentTimeEvent(0));
		//	this.addDataSources(cep.getEPRuntime());

		//tests
		EPAdministrator cepAdm = cep.getEPAdministrator();
	//	EPStatement cepStatement = cepAdm.createEPL("@Name(\"Count\") select count(*) from Measurement.win:time_batch(1 month)");
	//	EPStatement cepStatement2 = cepAdm.createEPL("@Name(\"Count2\") select count(*) from Alert");

	//	cepStatement.addListener(new CEPListener());
	//	cepStatement2.addListener(new CEPListener());



	}
	public String addPattern(String epl,String name) {
		EPAdministrator cepAdm = cep.getEPAdministrator();
		if(isValidQueryName(name)){
			try{
				EPStatement cepStatement = cepAdm.createEPL(epl);
				//cepStatement.addListener(new CEPListener());

			}catch (Exception e){
				return "Error: " + e.getMessage();
			}
			return "Pattern  "+ name +" added succesfully!";
		}else{
			return "Pattern already registered!";
		}



	}


	public void shutdown() {
		//stop data sources
		for(Thread t:datasources){
			System.out.println("going to interrupt " + t.getName());
			t.interrupt();
			if(t.isAlive()){
				System.out.println(t.getName() + " still alive");
			} if(t.isInterrupted()){
				System.out.println("interrupted");

			}

		}
		// destroy service provider
		cep.destroy();
		System.out.println("esper shutdown complete");
	}

	public void addDataSources(){
		//add data sources
		Thread tSync = new Thread(new DataStreamGenerator(cep.getEPRuntime()));
		tSync.setName("DataThread");
		tSync.start();
		datasources.add(tSync);

	}

	public void stopDataSources(String sourceName){
		for(Thread t:datasources){
			if(t.getName().equals(sourceName)){
				t.interrupt();
			}
		}
	}

	public void addEventTypes(Configuration config) {
		config.addEventType("Measurement", Measurement.class.getName());
		config.addEventType("Prediction", Prediction.class.getName());
		config.addEventType("Alert", Alert.class.getName());

	}

	public void addListeners() {
		// add listener for all queries added from saving modules
		EPAdministrator cepAdm = cep.getEPAdministrator();
		String[] names = cepAdm.getStatementNames();
		HashMap<String,String> returnMap = new HashMap<String,String>();
		for(String key:names){
			cepAdm.getStatement(key).addListener(new CEPListener());

		}	

	}




	public HashMap<String, String> getRegisteredQueries() {
		EPAdministrator cepAdm = cep.getEPAdministrator();
		String[] names = cepAdm.getStatementNames();
		HashMap<String,String> returnMap = new HashMap<String,String>();
		for(String key:names){
			returnMap.put(key, cepAdm.getStatement(key).getText());

		}
		return returnMap;
	}


	public boolean isValidQueryName(String name) {
		EPAdministrator cepAdm = cep.getEPAdministrator();
		String[] names = cepAdm.getStatementNames();
		if(Arrays.asList(names).contains(name)){
			return false;
		}

		return true;
	}


	public static class CEPListener implements UpdateListener {

		public void update(EventBean[] newData, EventBean[] oldData) {
			//log.info(newData[0].getUnderlying());
			//System.out.println(newData[0].getUnderlying());
			if(newData != null){
				for(EventBean e: newData){

					//	EventType t= e.getEventType();
					System.out.println(e.getUnderlying());

					//log.info(e.getUnderlying().toString());



					//	log.info(t.getName());
					//	log.info(t.getStartTimestampPropertyName());
					//	log.info(e.get(pNames[0]));
					//	log.info(t.getPropertyType(pNames[0]));
				}
			}


		}
	}

}
