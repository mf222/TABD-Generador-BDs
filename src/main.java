
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.neo4j.cypher.internal.compiler.v2_0.functions.Nodes;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.io.fs.FileUtils;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.client.MongoDatabase;

public class main {
    
	public static String nombres[] = {"Feña","Seba","Adrian","Jaime",
			"Vicho","Tony Stark","Hulk","Solid Snake",
			"Soprole","Lulu","Rodrigo","Belén",
			"Profe Juan"};
    
	public static String mascotas[][] ={
	        {"Carlos", "mamut", "13"},
	        {"Lili", "mariposa", "1"},
	        {"Poncho", "koala", "14"},
	        {"Verad", "dragon", " 6000"},
	        {"Cachupin", "perro", "4"},
	        {"Porqui", "cerdito", "30"},
	        {"Lora", "loro", "5"},
	        {"Samuel", "toro", "10"},
	        {"Rocko", "walabit", "20"},
	        {"Garfield", "gato", "7"}
	        };
    
    public static enum RelTypes implements RelationshipType
    {
        VECINO, MASCOTA 
    }
    
    static GraphDatabaseService graphDb;
    static Relationship relationship;
    
    static int tamaño_documento = 10;
    
     
  public static void main(String[] args) throws UnknownHostException {
  
	Random r = new Random();
	int idv = 0;
	List<Integer> ids = new ArrayList<Integer>();
    try {
    	
    

	int idm = tamaño_documento;
    
    //Datos de la conexion	
    MongoClient mongo = new MongoClient("localhost", 27017);
    DB db = mongo.getDB("DBVecinos");
    DBCollection collection = db.getCollection("person");   
    
    //Creamos la db de grafos
    graphDb = new GraphDatabaseFactory().newEmbeddedDatabase("grafo");
    registerShutdownHook(graphDb);
    List<Node> nodos = new ArrayList<Node>();
    Node firstNode;
    
    //Se crea la coleccion. documento es la query que ingresa a cada persona    
    BasicDBObject document ;
    for(int i = 0 ; i < tamaño_documento ; i++){
        
    	document = new BasicDBObject();
    	document.append("id", idv);
        document.append("nombre", nombres[r.nextInt(12)]); 
        document.append("edad", r.nextInt(50));
        document.append("vecinos", pickFriends(idv)); 
        
        idv++;
        int n_mascotas = (int) Math.round(Math.random());
        for(int j=0; j<n_mascotas;j++){
    	    String[] mascota = pickPet();
        	document.append("mascotas", new BasicDBObject("id",idm).append("nombre",mascota[0])
                                    .append("tipo", mascota[1])
                                    .append("edad", mascota[2])); 
        	idm++;
        }
        
        collection.insert(document);
 
    }
     
    //Ahora se comienza a pasar la BDDoc a BDGraph
    BasicDBObject exclude_include = new BasicDBObject();
    exclude_include.append("_id", 0);
    exclude_include.append("nombre", 0);
    exclude_include.append("edad", 0);
    DBCursor cursor = collection.find(new BasicDBObject(),exclude_include);
    
    
    
    
    
    try {
    	
    	try ( Transaction tx = graphDb.beginTx() )
    	{
    		//Se crean los nodos, cada uno mapea al de cada vecino
    		for(int m =0; m<tamaño_documento; m++){
     		   firstNode = graphDb.createNode();
     		   firstNode.setProperty("id", m);
     		   nodos.add(firstNode);
     	    }
    		
       while(cursor.hasNext()) {
    	   
    	   String rs = cursor.next().toString();
    	   
    	   //le quito los {} inicial y final
    	   String rs2 = rs.substring(1, rs.length()-1);
    	  
    	   //rs3[0] tiene la id, rs3[1] tiene las mascotas
    	   String[] rs3 = rs2.split("\\{"); 
    	   
    	   //los siguientes dos limpian rs3[0] para obtener la id y los vecinos
    	   String rs4 = rs3[0].replace("vecinos", "").replace("id", "").replace(":", "").replace("\"","");
    	   String[] rs5 = rs4.split(",");
    
    	   //Se obtiene la id, yay!
    	   String id = rs5[0].replace(" ", "");
    	   
    	   //Ahora se obtienen los vecinos y se almacenan en vecinillos
    	   List<String> vecinillos = new ArrayList<String>(); 
    	   for(int k = 1; k<rs5.length;k++){
    		   String[] vec = rs5[k].split("]");
    		   if(vec[0].replace("[", "").replace("]", "").replace(" ", "").replace("mascotas", "").length()!=0){ 
    			   vecinillos.add(vec[0].replace("[", "").replace("]", "").replace(" ", "").replace("mascotas", ""));
    			   }
    	   }
    	   
    	   
    	   //finalmente se obtienen la id de la mascota en id_m
    	   String id_m = "";
    	   if(rs3.length>1){
        	   String[] datm = rs3[1].split(",");
        	   id_m = datm[0].replace("\"id\"", "").replace(":", "").replace(" ", "");
    	   }
    		
    	   //Se relacionan los vecinos
    	   for(int k=0; k<vecinillos.size();k++){
    		   relationship = nodos.get(Integer.parseInt(id)).createRelationshipTo(nodos.get(Integer.parseInt(vecinillos.get(k))), RelTypes.VECINO );   	    
    	   }
    	   
    	   //Se relacionan las mascotas, si es que hay           
    	   if(!id_m.isEmpty()){
    		   nodos.add(graphDb.createNode());
    		   nodos.get(nodos.size()-1).setProperty("id", Integer.parseInt(id_m));
    		   relationship = nodos.get(Integer.parseInt(id)).createRelationshipTo(nodos.get(Integer.parseInt(id_m)), RelTypes.MASCOTA );
    	   }
           
       }
       
       tx.success();
    }
    } finally {
       cursor.close();
    }
    
    
  
    } catch (MongoException e) {
    e.printStackTrace();
    }
  
  }
  //----------------------------------------------------
  //These methods are just jused to build random data
  private static Object[] pickFriends(int id){
	  Random r = new Random();
	  int numberOfFriends = r.nextInt(tamaño_documento -1);
      List<Integer> friends = new ArrayList<Integer>();
      
      while(friends.size() < numberOfFriends){
    		int random = r.nextInt(tamaño_documento -1);
    	  	if(random!=id && !friends.contains(random)){
    	  		friends.add(random);
    	  	}
      }
      
      return  friends.toArray();
  }
  
  private static String[] pickPet(){
      int random = (int) (Math.random()*10);
      return mascotas[random];
  }
	    
  private static void registerShutdownHook( final GraphDatabaseService graphDb )
  {
      // Registers a shutdown hook for the Neo4j instance so that it
      // shuts down nicely when the VM exits (even if you "Ctrl-C" the
      // running application).
      Runtime.getRuntime().addShutdownHook( new Thread()
      {
          @Override
          public void run()
          {
              graphDb.shutdown();
          }
      } );
  }
	    
}
