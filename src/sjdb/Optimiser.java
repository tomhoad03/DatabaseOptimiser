package sjdb;

public class Optimiser {
    private final Catalogue catalogue;
    private boolean selectResolved = false; //
    private String currentSelect = ""; // to check if maximum optimisation has been reached

    public Optimiser(Catalogue catalogue) {
        this.catalogue = catalogue;
    }

    /*
    https://secure.ecs.soton.ac.uk/notes/comp3211/2021/gk/Query_Processing_20_21.pdf

    1. Start with canonical form.
    2. Move select operators down.
        a. Pushed down to just above the relation that contains attr when attr=val.
        b. Pushed down to the product above the subtree containing the relations that contain attr1 and attr2 when attr=attr.
    3. Reorder subtrees to put most restrictive select first.
    4. Combine product and select to create join.
    5. Move project operators down.
     */

    public Operator optimise(Operator plan) {
        Operator selectPlan = selectOptimise(plan);
        Operator restrictivePlan = restrictiveOptimise(selectPlan);
        Operator joinPlan = joinOptimise(restrictivePlan);
        return projectOptimise(joinPlan);
    }

    // Move select operators down
    public Operator selectOptimise(Operator plan) {
        Operator newPlan = plan;

        try {
            Select selectPlan = (Select) plan;
            Predicate selectPredicate = selectPlan.getPredicate();

            if (selectPredicate.equalsValue()) {
                newPlan = moveSelectDown1(selectPlan.getInput(), selectPredicate);
            } else {
                newPlan = moveSelectDown2(selectPlan.getInput(), selectPredicate);
            }
            selectResolved = false;
        } catch (Exception ignored) { }

        try {
            Select selectPlan = (Select) newPlan;

            if (currentSelect.equals(selectPlan.toString())) {
                return selectPlan;
            } else {
                currentSelect = selectPlan.toString();
                return selectOptimise(selectPlan);
            }
        } catch (Exception e1) {
            try {
                Scan scanPlan = (Scan) newPlan;
                return scanPlan;
            } catch (Exception e2) {
                try {
                    Join joinPlan = (Join) newPlan;
                    return new Join(selectOptimise(joinPlan.getLeft()), selectOptimise(joinPlan.getRight()), joinPlan.getPredicate());
                } catch (Exception e3) {
                    try {
                        Project projectPlan = (Project) newPlan;
                        return new Project(selectOptimise(projectPlan.getInput()), projectPlan.getAttributes());
                    } catch (Exception e4) {
                        Product productPlan = (Product) newPlan;
                        return new Product(selectOptimise(productPlan.getLeft()), selectOptimise(productPlan.getRight()));
                    }
                }
            }
        }
    }

    // when attr=value, move select down
    public Operator moveSelectDown1(Operator plan, Predicate predicate) {
        if (!selectResolved) {
            try {
                Scan scanOp = (Scan) plan;

                if (scanOp.getOutput().getAttributes().contains(predicate.getLeftAttribute())) {
                    selectResolved = true;
                    return new Select(scanOp, predicate);
                } else {
                    return plan;
                }
            } catch (Exception e1) {
                try {
                    Join joinOp = (Join) plan;
                    return new Join(moveSelectDown1(joinOp.getLeft(), predicate), moveSelectDown1(joinOp.getRight(), predicate), joinOp.getPredicate());
                } catch (Exception e2) {
                    try {
                        Select selectOp = (Select) plan;
                        return new Select(moveSelectDown1(selectOp.getInput(), predicate), selectOp.getPredicate());
                    } catch (Exception e3) {
                        try {
                            Project projectOp = (Project) plan;
                            return new Project(moveSelectDown1(projectOp.getInput(), predicate), projectOp.getAttributes());
                        } catch (Exception e4) {
                            Product productOp = (Product) plan;
                            return new Product(moveSelectDown1(productOp.getLeft(), predicate), moveSelectDown1(productOp.getRight(), predicate));
                        }
                    }
                }
            }
        } else {
            return plan;
        }
    }

    // when attr=attr, move select down
    public Operator moveSelectDown2(Operator plan, Predicate predicate) {
        if (!selectResolved) {
            try {
                Product productOp = (Product) plan;

                // Check if left and right subtrees contain attributes, otherwise find next product
                Boolean leftLeft = isInSubtree(productOp.getLeft(), predicate.getLeftAttribute());
                Boolean rightRight = isInSubtree(productOp.getRight(), predicate.getRightAttribute());

                if ((leftLeft && rightRight) || (!leftLeft && !rightRight)) {
                    selectResolved = true;
                    return new Select(productOp, predicate);
                } else if (leftLeft) {
                    return new Product(moveSelectDown2(productOp.getLeft(), predicate), productOp.getRight());
                } else {
                    return new Product(productOp.getLeft(), moveSelectDown2(productOp.getRight(), predicate));
                }
            } catch (Exception e1) {
                try {
                    Scan scanOp = (Scan) plan;
                    return scanOp;
                } catch (Exception e2) {
                    try {
                        Join joinOp = (Join) plan;
                        return new Join(moveSelectDown2(joinOp.getLeft(), predicate), moveSelectDown2(joinOp.getRight(), predicate), joinOp.getPredicate());
                    } catch (Exception e3) {
                        try {
                            Select selectOp = (Select) plan;
                            return new Select(moveSelectDown2(selectOp.getInput(), predicate), selectOp.getPredicate());
                        } catch (Exception e4) {
                            Project projectOp = (Project) plan;
                            return new Project(moveSelectDown2(projectOp.getInput(), predicate), projectOp.getAttributes());
                        }
                    }
                }
            }
        } else {
            return plan;
        }
    }

    public Boolean isInSubtree(Operator plan, Attribute attribute) {
        try {
            Scan scanOp = (Scan) plan;
            return scanOp.getOutput().getAttributes().contains(attribute);
        } catch (Exception e) {
            if (plan.getInputs().size() > 1) {
                return isInSubtree(plan.getInputs().get(0), attribute) || isInSubtree(plan.getInputs().get(1), attribute);
            } else {
                return isInSubtree(plan.getInputs().get(0), attribute);
            }
        }
    }

    // Reorder subtrees to put most restrictive select first
    public Operator restrictiveOptimise(Operator plan) {
        return plan;
    }

    // Combine product and select to create join
    public Operator joinOptimise(Operator plan) {
        try {
            Select selectPlan = (Select) plan;

            try {
                Product productPlan = (Product) selectPlan.getInput();
                return new Join(joinOptimise(productPlan.getLeft()), joinOptimise(productPlan.getRight()), selectPlan.getPredicate());
            } catch (Exception e) {
                return new Select(joinOptimise(selectPlan.getInput()), selectPlan.getPredicate());
            }
        } catch (Exception e1) {
            try {
                Scan scanPlan = (Scan) plan;
                return scanPlan;
            } catch (Exception e2) {
                try {
                    Join joinPlan = (Join) plan;
                    return new Join(joinOptimise(joinPlan.getLeft()), joinOptimise(joinPlan.getRight()), joinPlan.getPredicate());
                } catch (Exception e3) {
                    try {
                        Project projectPlan = (Project) plan;
                        return new Project(joinOptimise(projectPlan.getInput()), projectPlan.getAttributes());
                    } catch (Exception e4) {
                        Product productPlan = (Product) plan;
                        return new Product(joinOptimise(productPlan.getLeft()), joinOptimise(productPlan.getRight()));
                    }
                }
            }
        }
    }

    // Move project operators down
    public Operator projectOptimise(Operator plan) {
        return plan;
    }
}
