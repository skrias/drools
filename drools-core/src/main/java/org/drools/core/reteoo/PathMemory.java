package org.drools.core.reteoo;

import org.drools.core.common.InternalAgenda;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.common.Memory;
import org.drools.core.common.NetworkNode;
import org.drools.core.phreak.RuleInstanceAgendaItem;
import org.drools.core.util.AbstractBaseLinkedListNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathMemory extends AbstractBaseLinkedListNode<Memory>
        implements
        Memory {
    protected static transient Logger log = LoggerFactory.getLogger(SegmentMemory.class);

    private long linkedSegmentMask;

    private long allLinkedMaskTest;

    private NetworkNode networkNode;

    private RuleInstanceAgendaItem agendaItem;

    private SegmentMemory[] segmentMemories;

    private SegmentMemory segmentMemory;

    public PathMemory(NetworkNode networkNode) {
        this.networkNode = networkNode;
    }

    public NetworkNode getNetworkNode() {
        return networkNode;
    }

    public RuleInstanceAgendaItem getAgendaItem() {
        return agendaItem;
    }

    public void setAgendaItem(RuleInstanceAgendaItem agendaItem) {
        this.agendaItem = agendaItem;
    }

    public void setlinkedSegmentMask(long mask) {
        linkedSegmentMask = mask;
    }

    public long getLinkedSegmentMask() {
        return linkedSegmentMask;
    }

    public long getAllLinkedMaskTest() {
        return allLinkedMaskTest;
    }

    public void setAllLinkedMaskTest(long allLinkedTestMask) {
        this.allLinkedMaskTest = allLinkedTestMask;
    }

    public void linkNodeWithoutRuleNotify(long mask) {
        linkedSegmentMask = linkedSegmentMask | mask;
    }
    
    public void linkSegment(long mask,
                            InternalWorkingMemory wm) {
        if ( log.isTraceEnabled() ) {
            if ( NodeTypeEnums.isTerminalNode(getNetworkNode()) ) {
                TerminalNode rtn = (TerminalNode) getNetworkNode();
                log.trace( "  LinkSegment smask={} rmask={} name={}", mask, linkedSegmentMask, rtn.getRule().getName()  );
            }  else {
                log.trace( "  LinkSegment smask={} rmask={} name={}", mask, "RiaNode" );
            }
        }
        linkedSegmentMask = linkedSegmentMask | mask;
        if ( isRuleLinked() ) {
            doLinkRule( wm );
        }
    }

    public void doLinkRule(InternalWorkingMemory wm) {
        TerminalNode rtn = (TerminalNode) getNetworkNode();
        if ( log.isTraceEnabled() ) {
            log.trace( "    LinkRule name={}", rtn.getRule().getName()  );
        }
        if ( agendaItem == null ) {
            int salience = rtn.getRule().getSalience().getValue( null,
                                                                 rtn.getRule(),
                                                                 wm );
            agendaItem = ((InternalAgenda) wm.getAgenda()).createRuleInstanceAgendaItem(salience, this, rtn);
        } else if ( !agendaItem.isActive() ) {
            ((InternalAgenda) wm.getAgenda()).addActivation( agendaItem );
        }
        agendaItem.getRuleExecutor().setDirty(true);
    }

    public void doUnlinkRule(InternalWorkingMemory wm) {
        TerminalNode rtn = (TerminalNode) getNetworkNode();
        if ( log.isTraceEnabled() ) {
            log.trace( "    UnlinkRule name={}", rtn.getRule().getName()  );
        }
        if ( agendaItem == null ) {
            int salience = rtn.getRule().getSalience().getValue( null,
                                                                 rtn.getRule(),
                                                                 wm );
            agendaItem = ((InternalAgenda) wm.getAgenda()).createRuleInstanceAgendaItem(salience, this, rtn);
        } else if ( !agendaItem.isActive() ) {
            ((InternalAgenda) wm.getAgenda()).addActivation( agendaItem );
        }
        agendaItem.getRuleExecutor().setDirty(true);
        //((InternalAgenda) wm.getAgenda()).removeActivation( agendaItem );
    }

    public void unlinkedSegment(long mask,
                                InternalWorkingMemory wm) {
        if ( log.isTraceEnabled() ) {
            log.trace( "  UnlinkSegment smask={} rmask={} name={}", mask, linkedSegmentMask, this  );
        }
        if ( isRuleLinked() ) {
            doUnlinkRule( wm );
        }
        linkedSegmentMask = linkedSegmentMask ^ mask;
    }

    public boolean isRuleLinked() {
        return (linkedSegmentMask & allLinkedMaskTest) == allLinkedMaskTest;
    }

    public short getNodeType() {
        return NodeTypeEnums.RuleTerminalNode;
    }

    public SegmentMemory[] getSegmentMemories() {
        return segmentMemories;
    }

    public void setSegmentMemories(SegmentMemory[] segmentMemories) {
        this.segmentMemories = segmentMemories;
    }

    public void setSegmentMemory(SegmentMemory sm) {
        this.segmentMemory = sm;
    }
    
    public SegmentMemory getSegmentMemory() {
        return this.segmentMemory;
    }

    public String toString() {
        return "[RuleMem " + ((TerminalNode)getNetworkNode()).getRule().getName() + "]";
    }

}
