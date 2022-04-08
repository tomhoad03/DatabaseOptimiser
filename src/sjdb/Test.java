package sjdb;

import java.io.*;
import java.util.ArrayList;
import sjdb.DatabaseException;

public class Test {
	private Catalogue catalogue;
	
	public Test() {
	}

	public static void main(String[] args) throws Exception {
		Catalogue catalogue = createCatalogue();
		Inspector inspector = new Inspector();
		Estimator estimator = new Estimator();

		Operator plan = query(catalogue);
		plan.accept(estimator);
		plan.accept(inspector);

		Optimiser optimiser = new Optimiser(catalogue);
		Operator planopt = optimiser.optimise(plan);
		planopt.accept(estimator);
		planopt.accept(inspector);
	}
	
	public static Catalogue createCatalogue() {
		Catalogue cat = new Catalogue();

		cat.createRelation("Employee", 20);
		cat.createAttribute("Employee", "ESSN", 90);
		cat.createAttribute("Employee", "BDATE", 60);
		cat.createAttribute("Employee", "LNAME", 30);

		cat.createRelation("Works_On", 15);
		cat.createAttribute("Works_On", "SSN", 100);
		cat.createAttribute("Works_On", "PNO", 50);

		cat.createRelation("Project", 10);
		cat.createAttribute("Project", "PNUMBER", 100);
		cat.createAttribute("Project", "PNAME", 50);
		
		return cat;
	}

	public static Operator query(Catalogue cat) throws Exception {
		Scan a = new Scan(cat.getRelation("Employee"));
		Scan b = new Scan(cat.getRelation("Works_On"));
		Scan c = new Scan(cat.getRelation("Project"));

		Product p1 = new Product(a, b);
		Product p2 = new Product(p1, c);
		
		Select s1 = new Select(p2, new Predicate(new Attribute("PNUMBER"), new Attribute("PNO")));
		Select s2 = new Select(s1, new Predicate(new Attribute("PNAME"), "Aquarius"));
		Select s3 = new Select(s2, new Predicate(new Attribute("ESSN"), new Attribute("SSN")));
		Select s4 = new Select(s3, new Predicate(new Attribute("BDATE"), "1957-12-31"));

		ArrayList<Attribute> atts = new ArrayList<>();
		atts.add(new Attribute("LNAME"));

		Project plan = new Project(s4, atts);

		return plan;
	}
	
}

