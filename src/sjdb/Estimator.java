package sjdb;

public class Estimator implements PlanVisitor {
	public Estimator() {}

	// scan - feed name relation into query plan
	public void visit(Scan op) {
		Relation input = op.getRelation();
		Relation output = new Relation(input.getTupleCount());

		for (Attribute attribute : input.getAttributes()) {
			output.addAttribute(new Attribute(attribute));
		}
		
		op.setOutput(output);
	}

	// projection - select certain attributes of a relation
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

	// selection - select certain tuples of a relation, based on a predicate
	public void visit(Select op) {
		Relation input = op.getInput().getOutput();
		Relation output;

		if (op.getPredicate().equalsValue()) { // attr=val
			Attribute predicateAttribute = input.getAttribute(op.getPredicate().getLeftAttribute());

			int predicateAttributeCount = predicateAttribute.getValueCount();
			output = new Relation(input.getTupleCount() / predicateAttributeCount);

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

			int distinctValuesCount = Math.min(leftPredicateAttribute.getValueCount(), rightPredicateAttribute.getValueCount());
			int predicateAttributeCount = Math.max(leftPredicateAttribute.getValueCount(), rightPredicateAttribute.getValueCount());
			output = new Relation(input.getTupleCount() / predicateAttributeCount);

			for (Attribute attribute : input.getAttributes()) {
				if (attribute.equals(leftPredicateAttribute) || attribute.equals(rightPredicateAttribute)) {
					output.addAttribute(new Attribute(attribute.getName(), distinctValuesCount)); // set number of distinct values to min(V(R, A), V(R, B))
				} else {
					output.addAttribute(new Attribute(attribute));
				}
			}
		}

		op.setOutput(output);
	}

	// product - cartesian product of inputs
	public void visit(Product op) {
		Relation leftInput = op.getLeft().getOutput();
		Relation rightInput = op.getRight().getOutput();

		Relation output = new Relation(leftInput.getTupleCount() * rightInput.getTupleCount());

		for (Attribute attribute : leftInput.getAttributes()) {
			output.addAttribute(new Attribute(attribute));
		}

		for (Attribute attribute : rightInput.getAttributes()) {
			output.addAttribute(new Attribute(attribute));
		}

		op.setOutput(output);
	}

	// join - join inputs using a predicate (DOES NOT WORK ATM)
	public void visit(Join op) {
		Relation leftInput = op.getLeft().getOutput();
		Relation rightInput = op.getRight().getOutput();

		Attribute leftPredicateAttribute = leftInput.getAttribute(op.getPredicate().getLeftAttribute());
		Attribute rightPredicateAttribute = rightInput.getAttribute(op.getPredicate().getRightAttribute());

		int distinctValuesCount = Math.min(leftPredicateAttribute.getValueCount(), rightPredicateAttribute.getValueCount());
		int predicateAttributeCount = Math.max(leftPredicateAttribute.getValueCount(), rightPredicateAttribute.getValueCount());
		Relation output = new Relation((leftInput.getTupleCount() * rightInput.getTupleCount()) / predicateAttributeCount);

		leftInput.getAttributes().addAll(rightInput.getAttributes());

		for (Attribute attribute : leftInput.getAttributes()) {
			if (attribute.equals(leftPredicateAttribute) || attribute.equals(rightPredicateAttribute)) {
				output.addAttribute(new Attribute(attribute.getName(), distinctValuesCount)); // set number of distinct values to min(V(R, A), V(S, B))
			} else {
				output.addAttribute(new Attribute(attribute));
			}
		}

		for (Attribute attribute : rightInput.getAttributes()) {
			if (attribute.equals(leftPredicateAttribute) || attribute.equals(rightPredicateAttribute)) {
				output.addAttribute(new Attribute(attribute.getName(), distinctValuesCount)); // set number of distinct values to min(V(R, A), V(S, B))
			} else {
				output.addAttribute(new Attribute(attribute));
			}
		}

		op.setOutput(output);
	}
}
