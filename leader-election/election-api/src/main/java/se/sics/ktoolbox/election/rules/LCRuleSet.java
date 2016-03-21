package se.sics.ktoolbox.election.rules;

import se.sics.ktoolbox.election.util.LCPeerView;
import se.sics.ktoolbox.election.util.LEContainer;
import java.util.Collection;
import java.util.Comparator;
import se.sics.ktoolbox.util.network.KAddress;

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
     * @param selfContainer self address
     * @param viewSample view
     * @return cohorts
     */
    public abstract Collection<KAddress> initiateLeadership (LEContainer selfContainer, Collection<LEContainer> viewSample, int leaderGroupSize);


    /**
     * Once a node becomes the leader, it will periodically execute the leadership extension protocol
     * in which the node will initiate the extension protocol based on the outcome of the invocation.
     *
     * @param selfContainer self address
     * @param viewSample view sample
     * @return cohorts
     */
    public abstract Collection<KAddress> continueLeadership(LEContainer selfContainer, Collection<LEContainer> viewSample, int leaderGroupSize);


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
