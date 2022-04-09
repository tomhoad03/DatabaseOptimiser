package sjdb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Optimiser {
    private final Catalogue catalogue;
    private boolean selectResolved = false; //
    private String currentSelect = ""; // to check if maximum optimisation has been reached
    private boolean projectAbove = false;

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
        Operator orderingPlan = orderingOptimise(plan);
        Operator selectPlan = selectOptimise(orderingPlan);
        Operator joinPlan = joinOptimise(selectPlan);
        return projectOptimise(joinPlan);
    }

    // Reorder subtrees to put the relation with the least tuples first
    public Operator orderingOptimise(Operator plan) {
        try {
            Product productPlan = (Product) plan;
            ArrayList<Scan> scans = new ArrayList<>();

            // Find all the relations in the canonical query
            while (isProduct(productPlan.getLeft())) {
                scans.add((Scan) productPlan.getRight());
                productPlan = (Product) productPlan.getLeft();
            }
            scans.add((Scan) productPlan.getLeft());
            scans.add((Scan) productPlan.getRight());

            // Sort the relations by their size
            scans.sort(Comparator.comparingInt(o -> o.getOutput().getAttributes().size()));
            Product leftProducts = new Product(scans.get(0), scans.get(1));

            for (int i = 2; i < scans.size(); i++) {
                leftProducts = new Product(leftProducts, scans.get(i));
            }

            // Return the canonical form with the relations reordered
            return leftProducts;
        } catch (Exception e1) {
            try {
                Project projectPlan = (Project) plan;
                return new Project(orderingOptimise(projectPlan.getInput()), projectPlan.getAttributes());
            } catch (Exception e2) {
                Select selectPlan = (Select) plan;
                return new Select(orderingOptimise(selectPlan.getInput()), selectPlan.getPredicate());
            }
        }
    }

    // Used to check if at the bottom of left-deep tree
    public boolean isProduct(Operator plan) {
        try {
            Product productPlan = (Product) plan;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Move select operators down
    public Operator selectOptimise(Operator plan) {
        Operator newPlan = plan;

        try {
            Select selectPlan = (Select) plan;
            Predicate selectPredicate = selectPlan.getPredicate();

            // Different method for moving down if attr=attr or attr=value
            if (selectPredicate.equalsValue()) {
                newPlan = moveSelectDown1(selectPlan.getInput(), selectPredicate);
            } else {
                newPlan = moveSelectDown2(selectPlan.getInput(), selectPredicate);
            }
            selectResolved = false;
        } catch (Exception ignored) { }

        try {
            Select selectPlan = (Select) newPlan;

            // Check if fully optimised
            if (currentSelect.equals(selectPlan.toString())) {
                return selectPlan;
            } else {
                currentSelect = selectPlan.toString();
                return selectOptimise(selectPlan);
            }
        } catch (Exception e1) {
            try {
                Project projectPlan = (Project) newPlan;
                return new Project(selectOptimise(projectPlan.getInput()), projectPlan.getAttributes());
            } catch (Exception e2) {
                return newPlan;
            }
        }
    }

    // when attr=value, move select down
    public Operator moveSelectDown1(Operator plan, Predicate predicate) {
        if (!selectResolved) {
            try {
                Scan scanPlan = (Scan) plan;

                // Place the select above relation that contains attr
                if (scanPlan.getOutput().getAttributes().contains(predicate.getLeftAttribute())) {
                    selectResolved = true;
                    return new Select(scanPlan, predicate);
                } else {
                    return plan;
                }
            } catch (Exception e1) {
                try {
                    Select selectPlan = (Select) plan;
                    return new Select(moveSelectDown1(selectPlan.getInput(), predicate), selectPlan.getPredicate());
                } catch (Exception e2) {
                    Product productPlan = (Product) plan;
                    return new Product(moveSelectDown1(productPlan.getLeft(), predicate), moveSelectDown1(productPlan.getRight(), predicate));
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
                Scan scanPlan = (Scan) plan;
                return scanPlan;
            } catch (Exception e1) {
                try {
                    Select selectPlan = (Select) plan;
                    return new Select(moveSelectDown2(selectPlan.getInput(), predicate), selectPlan.getPredicate());
                } catch (Exception e2) {
                    Product productPlan = (Product) plan;

                    // Place the select above the join that contains subtrees that each contains one of the attributes
                    Boolean leftLeft = isInSubtree(productPlan.getLeft(), predicate.getLeftAttribute());
                    Boolean rightRight = isInSubtree(productPlan.getRight(), predicate.getRightAttribute());

                    if ((leftLeft && rightRight) || (!leftLeft && !rightRight)) {
                        selectResolved = true;
                        return new Select(productPlan, predicate);
                    } else if (leftLeft) {
                        return new Product(moveSelectDown2(productPlan.getLeft(), predicate), productPlan.getRight());
                    } else {
                        return new Product(productPlan.getLeft(), moveSelectDown2(productPlan.getRight(), predicate));
                    }
                }
            }
        } else {
            return plan;
        }
    }

    // Is the attribute in a relation in this operators tree
    public Boolean isInSubtree(Operator plan, Attribute attribute) {
        try {
            Scan scanPlan = (Scan) plan;
            return scanPlan.getOutput().getAttributes().contains(attribute);
        } catch (Exception e) {
            if (plan.getInputs().size() > 1) {
                return isInSubtree(plan.getInputs().get(0), attribute) || isInSubtree(plan.getInputs().get(1), attribute);
            } else {
                return isInSubtree(plan.getInputs().get(0), attribute);
            }
        }
    }

    // Combine product and select to create join
    public Operator joinOptimise(Operator plan) {
        try {
            Scan scanPlan = (Scan) plan;
            return scanPlan;
        } catch (Exception e1) {
            try {
                Project projectPlan = (Project) plan;
                return new Project(joinOptimise(projectPlan.getInput()), projectPlan.getAttributes());
            } catch (Exception e2) {
                try {
                    Select selectPlan = (Select) plan;

                    // Combine an attr=attr select with the product below
                    try {
                        Product productPlan = (Product) selectPlan.getInput();
                        return new Join(joinOptimise(productPlan.getLeft()), joinOptimise(productPlan.getRight()), selectPlan.getPredicate());
                    } catch (Exception e) {
                        return new Select(joinOptimise(selectPlan.getInput()), selectPlan.getPredicate());
                    }
                } catch (Exception e3) {
                    Join joinPlan = (Join) plan;
                    return new Join(joinOptimise(joinPlan.getLeft()), joinOptimise(joinPlan.getRight()), joinPlan.getPredicate());
                }
            }
        }
    }

    // Move project operators down
    public Operator projectOptimise(Operator plan) {
        try {
            Scan scanPlan = (Scan) plan;
            return scanPlan;
        } catch (Exception e1) {
            try {
                Project projectPlan = (Project) plan;

                // Create projects for each
                for (Attribute projectAttribute : projectPlan.getAttributes()) {
                    projectAbove = true;
                    projectPlan = new Project(createProjects(projectPlan.getInput(), projectAttribute), projectPlan.getAttributes());
                    projectAbove = false;
                }

                return new Project(projectOptimise(projectPlan.getInput()), projectPlan.getAttributes());
            } catch (Exception e2) {
                try {
                    Select selectPlan = (Select) plan;
                    return new Select(projectOptimise(selectPlan.getInput()), selectPlan.getPredicate());
                } catch (Exception e3) {
                    try {
                        Product productPlan = (Product) plan;
                        return new Product(projectOptimise(productPlan.getLeft()), projectOptimise(productPlan.getRight()));
                    } catch (Exception e4) {
                        Join joinPlan = (Join) plan;
                        if (isInSubtree(joinPlan.getLeft(), joinPlan.getPredicate().getLeftAttribute())) {
                            return new Join(createProjects(projectOptimise(joinPlan.getLeft()), joinPlan.getPredicate().getLeftAttribute()), createProjects(projectOptimise(joinPlan.getRight()), joinPlan.getPredicate().getRightAttribute()), joinPlan.getPredicate());
                        } else {
                            return new Join(createProjects(projectOptimise(joinPlan.getLeft()), joinPlan.getPredicate().getRightAttribute()), createProjects(projectOptimise(joinPlan.getRight()), joinPlan.getPredicate().getLeftAttribute()), joinPlan.getPredicate());
                        }
                    }
                }
            }
        }
    }

    // Create a new project operator
    public Operator createProjects(Operator plan, Attribute attribute) {
        try {
            Scan scanPlan = (Scan) plan;

            // Add a projection above a relation
            if (scanPlan.getOutput().getAttributes().contains(attribute) && !projectAbove) {
                return new Project(scanPlan, new ArrayList<>(Collections.singleton(attribute)));
            } else {
                projectAbove = false;
                return scanPlan;
            }
        } catch (Exception e1) {
            try {
                Project projectPlan = (Project) plan;

                // Add an attribute to an existing projection
                if (!projectPlan.getAttributes().contains(attribute) && isInSubtree(projectPlan, attribute)) {
                    projectPlan.getAttributes().add(attribute);
                    projectAbove = true;
                } else projectAbove = isInSubtree(projectPlan, attribute);

                return new Project(createProjects(projectPlan.getInput(), attribute), projectPlan.getAttributes());
            } catch (Exception e2) {
                try {
                    Select selectPlan = (Select) plan;

                    // Add a projection above an attr=value selection
                    if (!projectAbove && isInSubtree(selectPlan, attribute)) {
                        return new Project(selectPlan, new ArrayList<>(Collections.singleton(attribute)));
                    } else {
                        projectAbove = false;
                        return selectPlan;
                    }
                } catch (Exception e3) {
                    Join joinPlan = (Join) plan;

                    // Add a projection above a join
                    if (!projectAbove && isInSubtree(joinPlan, attribute)) {
                        return new Project(new Join(createProjects(joinPlan.getLeft(), attribute), createProjects(joinPlan.getRight(), attribute), joinPlan.getPredicate()), new ArrayList<>(Collections.singleton(attribute)));
                    } else {
                        projectAbove = false;
                        return new Join(createProjects(joinPlan.getLeft(), attribute), createProjects(joinPlan.getRight(), attribute), joinPlan.getPredicate());
                    }
                }
            }
        }
    }
}
