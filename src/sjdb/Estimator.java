package sjdb;

public class Estimator implements PlanVisitor {
	public Estimator() {
		// empty constructor
	}

	/* 
	 * Create output relation on Scan operator
	 *
	 * Example implementation of visit method for Scan operators.
	 */
	public void visit(Scan op) {
		Relation input = op.getRelation();
		Relation output = new Relation(input.getTupleCount());

		for (Attribute attribute : input.getAttributes()) {
			output.addAttribute(new Attribute(attribute));
		}
		
		op.setOutput(output);
	}

	public void visit(Project op) {
	}
	
	public void visit(Select op) {
	}
	
	public void visit(Product op) {
	}
	
	public void visit(Join op) {
	}
}
