package sjdb;

public class Optimiser {
    private final Catalogue catalogue;
    private boolean selectResolved = false;

    public Optimiser(Catalogue catalogue) {
        this.catalogue = catalogue;
    }

    /*
    https://secure.ecs.soton.ac.uk/notes/comp3211/2021/gk/Query_Processing_20_21.pdf

    1. Start with canonical form.
    2. Move select operators down.
    3. Reorder subtrees to put most restrictive select first.
    4. Combine product and select to create join.
    5. Move project operators down.
     */

    public Operator optimise(Operator plan) {
        if (plan.toString().startsWith("SELECT")) {
            Select selectPlan = (Select) plan;
            Predicate selectPredicate = selectPlan.getPredicate();

            // pushed down to just above the relation that contains attr
            if (selectPredicate.equalsValue()) {
                System.out.println("TEST1");
            // pushed down to the product above the subtree containing the relations that contain attr1 and attr2
            } else {
                System.out.println("TEST2");
                moveSelectDown(selectPlan.getInput(), selectPredicate.getLeftAttribute(), selectPredicate.getRightAttribute());
            }

            optimise(selectPlan.getInput());
        } else {
            System.out.println("ELSE");
        }

        return plan;
    }

    public Operator moveSelectDown(Operator plan, Attribute leftAttribute, Attribute rightAttribute) {
        if (!selectResolved) {
            try {
                Product productOp = (Product) plan;

                // Check if left and right subtrees contain attributes, otherwise find next product
                Boolean leftLeft = leftAttrLeft(productOp.getLeft(), leftAttribute);
                Boolean rightRight = rightAttrRight(productOp.getRight(), rightAttribute);

                if (leftLeft && rightRight) {
                    selectResolved = true;
                    return plan;
                } else if (leftLeft) {
                    return new Product(moveSelectDown(productOp.getLeft(), leftAttribute, rightAttribute), productOp.getRight());
                } else {
                    return new Product(productOp.getLeft(), moveSelectDown(productOp.getRight(), leftAttribute, rightAttribute));
                }
            } catch (Exception e1) {
                try {
                    Join joinOp = (Join) plan;
                    return new Join(moveSelectDown(joinOp.getLeft(), leftAttribute, rightAttribute), moveSelectDown(joinOp.getRight(), leftAttribute, rightAttribute), joinOp.getPredicate());
                } catch (Exception e2) {
                    try {
                        Select selectOp = (Select) plan;
                        return new Select(moveSelectDown(selectOp.getInput(), leftAttribute, rightAttribute), selectOp.getPredicate());
                    } catch (Exception e3) {
                        try {
                            Project projectOp = (Project) plan;
                            return new Project(moveSelectDown(projectOp.getInput(), leftAttribute, rightAttribute), projectOp.getAttributes());
                        } catch (Exception e4) {
                            return plan;
                        }
                    }
                }
            }
        } else {
            return plan;
        }
    }

    public Boolean leftAttrLeft(Operator leftPlan, Attribute leftAttribute) {
        try {
            Scan scanOp = (Scan) leftPlan;
            return scanOp.getOutput().getAttributes().contains(leftAttribute);
        } catch (Exception e) {
            if (leftPlan.getInputs().size() > 1) {
                return leftAttrLeft(leftPlan.getInputs().get(0), leftAttribute) || leftAttrLeft(leftPlan.getInputs().get(1), leftAttribute);
            } else {
                return leftAttrLeft(leftPlan.getInputs().get(0), leftAttribute);
            }
        }
    }

    public Boolean rightAttrRight(Operator rightPlan, Attribute rightAttribute) {
        try {
            Scan scanOp = (Scan) rightPlan;
            return scanOp.getOutput().getAttributes().contains(rightAttribute);
        } catch (Exception e) {
            if (rightPlan.getInputs().size() > 1) {
                return rightAttrRight(rightPlan.getInputs().get(0), rightAttribute) || leftAttrLeft(rightPlan.getInputs().get(1), rightAttribute);
            } else {
                return rightAttrRight(rightPlan.getInputs().get(0), rightAttribute);
            }
        }
    }
}
