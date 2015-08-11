package se.sics.p2ptoolbox.election.api.rules;

import se.sics.p2ptoolbox.election.api.LCPeerView;
import se.sics.p2ptoolbox.election.api.LEContainer;

import java.util.Collection;
import java.util.Comparator;

/**
 * Rule set governing the functioning of the cohorts in the
 * system.
 *
 * Created by babbar on 2015-08-11.
 */
public abstract class CohortsRuleSet extends RuleSet{

    public CohortsRuleSet(Comparator<LEContainer> comparator) {
        super(comparator);
    }

    /**
     * In case the leader tries to assert self as a leader, the cohorts can reject the promise using the
     * method. The full semantics actually set by the application.
     *
     * @param leaderContainer descriptor of leader
     * @param selfContainer self descriptor
     * @param viewSample sample
     * @return valid / not valid
     */
    public abstract boolean validate( LEContainer leaderContainer, LEContainer selfContainer, Collection<LEContainer> viewSample);
}
