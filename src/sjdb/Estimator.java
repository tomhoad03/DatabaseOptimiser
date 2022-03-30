package sjdb;

public class Estimator implements PlanVisitor {
	public Estimator() {}

	public void visit(Scan op) {
		Relation input = op.getRelation();
		Relation output = new Relation(input.getTupleCount());

		for (Attribute attribute : input.getAttributes()) {
			output.addAttribute(new Attribute(attribute));
		}
		
		op.setOutput(output);
	}

	public void visit(Project op) {
		Relation input = op.getInput().getOutput();
		Relation output = new Relation(input.getTupleCount());

		for (Attribute attribute : input.getAttributes()) {
			if (op.getAttributes().contains(attribute)) {
				output.addAttribute(new Attribute(attribute));
			}
		}

		op.setOutput(output);
	}
	
	public void visit(Select op) {
		Relation input = op.getInput().getOutput();
		Relation output;

		if (op.getPredicate().equalsValue()) { // attr=val
			Attribute predicateAttribute = input.getAttribute(op.getPredicate().getLeftAttribute());
			int predicateAttributeCount = predicateAttribute.getValueCount();
			output = new Relation(input.getTupleCount() - predicateAttributeCount);

			for (Attribute attribute : input.getAttributes()) {
				if (attribute.equals(predicateAttribute)) {
					output.addAttribute(new Attribute(attribute.getName(), 1)); // set number of distinct values to 1
				} else {
					output.addAttribute(new Attribute(attribute));
				}
			}
		} else { // attr=attr
			Attribute leftPredicateAttribute = input.getAttribute(op.getPredicate().getLeftAttribute());
			Attribute rightPredicateAttribute = input.getAttribute(op.getPredicate().getRightAttribute());
		}

		op.setOutput(output);
	}
	
	public void visit(Product op) {

	}
	
	public void visit(Join op) {

	}
}
