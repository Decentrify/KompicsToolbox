package se.sics.p2ptoolbox.election.api.rules;

import se.sics.p2ptoolbox.election.api.LCPeerView;
import se.sics.p2ptoolbox.election.api.LEContainer;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import java.util.Collection;
import java.util.Comparator;

/**
 * Rule set for the leader election component.
 *
 * Created by babbar on 2015-08-11.
 */
public abstract class LCRuleSet extends RuleSet{

    public LCRuleSet(Comparator<LEContainer> comparator) {
        super(comparator);
    }

    /**
     * Invoked by the node which is trying to become the leader. The initiating node
     * executes the condition as defined by the application and in case the outcome is empty
     * gives up the attempt to become the leader.
     *
     * @param selfAddress self address
     * @param viewSample view
     * @return cohorts
     */
    public abstract Collection<DecoratedAddress> initiateLeadership (DecoratedAddress selfAddress, Collection<LEContainer> viewSample);


    /**
     * Once a node becomes the leader, it will periodically execute the leadership extension protocol
     * in which the node will initiate the extension protocol based on the outcome of the invocation.
     *
     * @param selfAddress self address
     * @param viewSample view sample
     * @return cohorts
     */
    public abstract Collection<DecoratedAddress> continueLeadership(DecoratedAddress selfAddress, Collection<LEContainer> viewSample);


    /**
     * Under special situations, the application can force the node to give up leadership.
     * The condition is decided by the application.
     *
     * @param oldView old view
     * @param newView new self view
     * @return true -> back off
     */
    public abstract boolean terminateLeadership(LCPeerView oldView, LCPeerView newView);


}
