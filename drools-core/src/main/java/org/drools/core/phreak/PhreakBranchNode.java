package org.drools.core.phreak;

import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.common.LeftTupleSets;
import org.drools.core.reteoo.ConditionalBranchEvaluator;
import org.drools.core.reteoo.ConditionalBranchEvaluator.ConditionalExecution;
import org.drools.core.reteoo.ConditionalBranchNode;
import org.drools.core.reteoo.ConditionalBranchNode.ConditionalBranchMemory;
import org.drools.core.reteoo.LeftTuple;
import org.drools.core.reteoo.LeftTupleSink;
import org.drools.core.reteoo.RuleTerminalNode;
import org.drools.core.util.index.LeftTupleList;

/**
* Created with IntelliJ IDEA.
* User: mdproctor
* Date: 03/05/2013
* Time: 15:43
* To change this template use File | Settings | File Templates.
*/
public class PhreakBranchNode {
    public void doNode(ConditionalBranchNode branchNode,
                       ConditionalBranchMemory cbm,
                       LeftTupleSink sink,
                       InternalWorkingMemory wm,
                       LeftTupleSets srcLeftTuples,
                       LeftTupleSets trgLeftTuples,
                       LeftTupleSets stagedLeftTuples,
                       RuleExecutor executor) {

        if (srcLeftTuples.getDeleteFirst() != null) {
            doLeftDeletes(branchNode, cbm, wm, srcLeftTuples, trgLeftTuples, stagedLeftTuples, executor);
        }

        if (srcLeftTuples.getUpdateFirst() != null) {
            doLeftUpdates(branchNode, cbm, sink, wm, srcLeftTuples, trgLeftTuples, stagedLeftTuples, executor);
        }

        if (srcLeftTuples.getInsertFirst() != null) {
            doLeftInserts(branchNode, cbm, sink, wm, srcLeftTuples, trgLeftTuples, executor);
        }

        srcLeftTuples.resetAll();
    }

    public void doLeftInserts(ConditionalBranchNode branchNode,
                              ConditionalBranchMemory cbm,
                              LeftTupleSink sink,
                              InternalWorkingMemory wm,
                              LeftTupleSets srcLeftTuples,
                              LeftTupleSets trgLeftTuples,
                              RuleExecutor executor) {
        ConditionalBranchEvaluator branchEvaluator = branchNode.getBranchEvaluator();
        LeftTupleList tupleList = executor.getLeftTupleList();

        for (LeftTuple leftTuple = srcLeftTuples.getInsertFirst(); leftTuple != null; ) {
            LeftTuple next = leftTuple.getStagedNext();

            boolean breaking = false;
            ConditionalExecution conditionalExecution = branchEvaluator.evaluate(leftTuple, wm, cbm.context);

            boolean useLeftMemory = RuleNetworkEvaluator.useLeftMemory(branchNode, leftTuple);

            if (conditionalExecution != null) {
                RuleTerminalNode rtn = (RuleTerminalNode) conditionalExecution.getSink().getFirstLeftTupleSink();
                LeftTuple branchedLeftTuple = rtn.createLeftTuple(leftTuple,
                                                                  rtn,
                                                                  leftTuple.getPropagationContext(), useLeftMemory);

                leftTuple.setObject(branchedLeftTuple);

                //rtn.assertLeftTuple(branchedLeftTuple, leftTuple.getPropagationContext(), wm);
                tupleList.add(branchedLeftTuple);

                breaking = conditionalExecution.isBreaking();
            }

            if (!breaking) {
                trgLeftTuples.addInsert(sink.createLeftTuple(leftTuple,
                                                             sink,
                                                             leftTuple.getPropagationContext(), useLeftMemory));
            }

            leftTuple.clearStaged();
            leftTuple = next;
        }
    }

    public void doLeftUpdates(ConditionalBranchNode branchNode,
                              ConditionalBranchMemory cbm,
                              LeftTupleSink sink,
                              InternalWorkingMemory wm,
                              LeftTupleSets srcLeftTuples,
                              LeftTupleSets trgLeftTuples,
                              LeftTupleSets stagedLeftTuples,
                              RuleExecutor executor) {
        ConditionalBranchEvaluator branchEvaluator = branchNode.getBranchEvaluator();
        LeftTupleList tupleList = executor.getLeftTupleList();

        for (LeftTuple leftTuple = srcLeftTuples.getUpdateFirst(); leftTuple != null; ) {
            LeftTuple next = leftTuple.getStagedNext();

            LeftTuple rtnLeftTuple = (LeftTuple) leftTuple.getObject();
            LeftTuple mainLeftTuple = leftTuple.getFirstChild();

            RuleTerminalNode oldRtn = null;
            if (rtnLeftTuple != null) {
                oldRtn = (RuleTerminalNode) rtnLeftTuple.getSink();
            }

            ConditionalExecution conditionalExecution = branchEvaluator.evaluate(leftTuple, wm, cbm.context);

            RuleTerminalNode newRtn = null;
            boolean breaking = false;
            if (conditionalExecution != null) {
                newRtn = (RuleTerminalNode) conditionalExecution.getSink().getFirstLeftTupleSink();
                breaking = conditionalExecution.isBreaking();
            }

            // Handle conditional branches
            if (oldRtn != null) {
                if (newRtn == null) {
                    // old exits, new does not, so delete
                    if ( rtnLeftTuple.getMemory() != null ) {
                        tupleList.remove(rtnLeftTuple);
                    }
                    oldRtn.retractLeftTuple(rtnLeftTuple, rtnLeftTuple.getPropagationContext(), wm);

                } else if (newRtn == oldRtn) {
                    // old and new on same branch, so update
                    if ( rtnLeftTuple.getMemory() != null ) {
                        tupleList.remove(rtnLeftTuple); // must be removed before it can bereadded
                    }
                    tupleList.add(rtnLeftTuple);
                    //oldRtn.modifyLeftTuple(rtnLeftTuple, rtnLeftTuple.getPropagationContext(), wm);

                } else {
                    // old and new on different branches, delete one and insert the other
                    if ( rtnLeftTuple.getMemory() != null ) {
                        tupleList.remove(rtnLeftTuple);
                    }
                    oldRtn.retractLeftTuple(rtnLeftTuple, rtnLeftTuple.getPropagationContext(), wm);

                    rtnLeftTuple = newRtn.createLeftTuple(leftTuple,
                                                          newRtn,
                                                          leftTuple.getPropagationContext(), true);

                    leftTuple.setObject(rtnLeftTuple);
                    tupleList.add(rtnLeftTuple);
                    //newRtn.assertLeftTuple(rtnLeftTuple, rtnLeftTuple.getPropagationContext(), wm);
                }

            } else if (newRtn != null) {
                // old does not exist, new exists, so insert
                rtnLeftTuple = newRtn.createLeftTuple(leftTuple,
                                                      newRtn,
                                                      leftTuple.getPropagationContext(), true);

                leftTuple.setObject(rtnLeftTuple);
                //newRtn.assertLeftTuple(rtnLeftTuple, rtnLeftTuple.getPropagationContext(), wm);
                tupleList.add(rtnLeftTuple);
            }

            // Handle main branch
            if (mainLeftTuple != null) {
                switch (mainLeftTuple.getStagedType()) {
                    // handle clash with already staged entries
                    case LeftTuple.INSERT:
                        stagedLeftTuples.removeInsert(mainLeftTuple);
                        break;
                    case LeftTuple.UPDATE:
                        stagedLeftTuples.removeUpdate(mainLeftTuple);
                        break;
                }

                if (!breaking) {
                    // child exist, new one does, so update
                    trgLeftTuples.addUpdate(mainLeftTuple);
                } else {
                    // child exist, new one does not, so delete
                    trgLeftTuples.addDelete(mainLeftTuple);
                }
            } else if (!breaking) {
                // child didn't exist, new one does, so insert
                trgLeftTuples.addInsert(sink.createLeftTuple(leftTuple,
                                                             sink,
                                                             leftTuple.getPropagationContext(), true));
            }

            leftTuple.clearStaged();
            leftTuple = next;
        }
    }

    public void doLeftDeletes(ConditionalBranchNode branchNode,
                              ConditionalBranchMemory cbm,
                              InternalWorkingMemory wm,
                              LeftTupleSets srcLeftTuples,
                              LeftTupleSets trgLeftTuples,
                              LeftTupleSets stagedLeftTuples,
                              RuleExecutor executor) {
        LeftTupleList tupleList = executor.getLeftTupleList();

        for (LeftTuple leftTuple = srcLeftTuples.getDeleteFirst(); leftTuple != null; ) {
            LeftTuple next = leftTuple.getStagedNext();

            LeftTuple rtnLeftTuple = (LeftTuple) leftTuple.getObject();
            LeftTuple mainLeftTuple = leftTuple.getFirstChild();

            if (rtnLeftTuple != null) {
                RuleTerminalNode rtn = (RuleTerminalNode) rtnLeftTuple.getSink();
                if ( rtnLeftTuple.getMemory() != null ) {
                    tupleList.remove(rtnLeftTuple);
                }
                rtn.retractLeftTuple(rtnLeftTuple,
                                     rtnLeftTuple.getPropagationContext(),
                                     wm);
            }

            if (mainLeftTuple != null) {
                switch (mainLeftTuple.getStagedType()) {
                    // handle clash with already staged entries
                    case LeftTuple.INSERT:
                        stagedLeftTuples.removeInsert(mainLeftTuple);
                        break;
                    case LeftTuple.UPDATE:
                        stagedLeftTuples.removeUpdate(mainLeftTuple);
                        break;
                }
                trgLeftTuples.addDelete(mainLeftTuple);
            }

            leftTuple.clearStaged();
            leftTuple = next;
        }
    }
}
