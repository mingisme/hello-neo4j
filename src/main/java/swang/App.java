package swang;


import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.session.SessionFactory;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) throws SQLException {
//         embeddedTest();
       driver4();
    }

    private static void ogm() {
        //failed
        Configuration conf = new Configuration.Builder().uri("http://neo4j:123456@localhost:7474").build();
        SessionFactory factory = new SessionFactory(conf, "swang");
        org.neo4j.ogm.session.Session session = factory.openSession();
        Car tesla = new Car("tesla");
        Company baeldung = new Company("baeldung");

        baeldung.setCar(tesla);
        session.save(baeldung);
    }

    private static void jdbc() throws SQLException {
        //failed
        Connection con = DriverManager.getConnection(
                "jdbc:neo4j:bolt://localhost/?user=neo4j,password=123456");
        Statement stmt = con.createStatement();
        stmt.execute("CREATE (baeldung:Company {name:\"Baeldung\"}) "
                + "-[:owns]-> (tesla:Car {make: 'tesla', model: 'modelX'})"
                + "RETURN baeldung, tesla");

        ResultSet rs = stmt.executeQuery(
                "MATCH (company:Company)-[:owns]-> (car:Car)" +
                        "WHERE car.make='tesla' and car.model='modelX'" +
                        "RETURN company.name");

        while (rs.next()) {
            rs.getString("company.name");
        }
    }


    private static void driver4() {
        /* java driver 4.0.0 */
        Driver driver = GraphDatabase.driver(
                "neo4j://localhost:7687", AuthTokens.basic("neo4j", "123456"));

        Session session = driver.session();

        session.run("CREATE (baeldung:Company {name:\"Baeldung\"}) " +
                "-[:owns]-> (tesla:Car {make: 'tesla', model: 'modelX'})" +
                "RETURN baeldung, tesla");

        session.close();
        driver.close();
    }

    private static void embeddedTest() {
        /* embedded neo4j*/
        GraphDatabaseFactory graphDbFactory = new GraphDatabaseFactory();
        GraphDatabaseService graphDb = graphDbFactory.newEmbeddedDatabase(
                new File("data/cars"));

        graphDb.beginTx();


        Node car = graphDb.createNode(Label.label("Car"));
        car.setProperty("make", "tesla");
        car.setProperty("model", "model3");

        Node owner = graphDb.createNode(Label.label("Person"));
        owner.setProperty("firstName", "baeldung");
        owner.setProperty("lastName", "baeldung");

        owner.createRelationshipTo(car, RelationshipType.withName("owner"));

        Result result = graphDb.execute(
                "MATCH (c:Car) <-[owner]- (p:Person) " +
                        "WHERE c.make = 'tesla'" +
                        "RETURN p.firstName, p.lastName");

        System.out.println(result.resultAsString());

        /* create node */
        result = graphDb.execute(
                "CREATE (baeldung:Company {name:\"Baeldung\"}) " +
                        "-[:owns]-> (tesla:Car {make: 'tesla', model: 'modelX'})" +
                        "RETURN baeldung, tesla");

        System.out.println(result.resultAsString());

        /* update node */
        result = graphDb.execute("MATCH (car:Car)" +
                "WHERE car.make='tesla'" +
                " SET car.milage=120" +
                " SET car :Car:Electro" +
                " SET car.model=NULL" +
                " RETURN car");

        System.out.println(result.resultAsString());

        /* delete node */
        graphDb.execute("MATCH (company:Company)" +
                " WHERE company.name='Baeldung'" +
                " DELETE company");

        result = graphDb.execute("MATCH (company:Company)"
                + " WHERE company.name='Baeldung'"
                + " RETURN company");

        System.out.println(result.resultAsString());

        /* parameter binding */
        Map<String, Object> params = new HashMap<>();
        params.put("name", "baeldung");
        params.put("make", "tesla");
        params.put("model", "modelS");

        result = graphDb.execute("CREATE (baeldung:Company {name:$name}) " +
                "-[:owns]-> (tesla:Car {make: $make, model: $model})" +
                "RETURN baeldung, tesla", params);

        System.out.println(result.resultAsString());
    }
}
