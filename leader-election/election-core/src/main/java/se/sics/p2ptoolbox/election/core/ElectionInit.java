package se.sics.p2ptoolbox.election.core;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Init;
import se.sics.p2ptoolbox.election.api.LCPeerView;
import se.sics.p2ptoolbox.election.api.rules.CohortsRuleSet;
import se.sics.p2ptoolbox.election.api.rules.LCRuleSet;
import se.sics.p2ptoolbox.election.core.util.LeaderFilter;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Comparator;

/**
 * Base Init class for the core classes involved in leader election.
 * Created by babbar on 2015-04-04.
 */
public class ElectionInit<T extends ComponentDefinition> extends  Init <T> {

    public final DecoratedAddress selfAddress;
    public final LCPeerView initialView;
    public final long seed;
    public final ElectionConfig electionConfig;
    public final PublicKey publicKey;
    public final PrivateKey privateKey;
    public final Comparator<LCPeerView> comparator;
    public final LeaderFilter filter;
    public final LCRuleSet lcRuleSet;
    public final CohortsRuleSet cohortsRuleSet;

    public ElectionInit(DecoratedAddress selfAddress,
                        LCPeerView initialView,
                        long seed,
                        ElectionConfig electionConfig,
                        PublicKey publicKey,
                        PrivateKey privateKey,
                        Comparator<LCPeerView> comparator,
                        LeaderFilter filter, LCRuleSet lcRuleSet, CohortsRuleSet cohortsRuleSet){

        this.selfAddress = selfAddress;
        this.seed = seed;
        this.initialView = initialView;
        this.electionConfig = electionConfig;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.comparator = comparator;
        this.filter = filter;
        this.lcRuleSet = lcRuleSet;
        this.cohortsRuleSet = cohortsRuleSet;
    }

}
